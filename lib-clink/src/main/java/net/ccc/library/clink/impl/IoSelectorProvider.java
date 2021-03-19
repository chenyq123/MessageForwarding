package net.ccc.library.clink.impl;

import net.ccc.library.clink.core.IoProvider;
import net.ccc.library.clink.util.CloseUtils;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();
        inputHandlePool = Executors.newFixedThreadPool(4,
                new NameableThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4,
                new NameableThreadFactory("IoProvider-Output-Thread-"));
        startRead();
        startWrite();
    }

    private void startWrite() {
        Thread thread = new SelectThread("Clink IoSelectorProvider ReadSelector Thread",
                isClosed, inRegInput, readSelector, inputCallbackMap, inputHandlePool, SelectionKey.OP_READ);
        thread.start();
    }

    private void startRead() {
        Thread thread = new SelectThread("Clink IoSelectorProvider WriteSelector Thread",
                isClosed, inRegOutput, writeSelector, outputCallbackMap, outputHandlePool, SelectionKey.OP_WRITE);
        thread.start();
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandlerProviderCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ,
                inRegInput, inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandlerProviderCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE,
                inRegOutput, outputCallbackMap, callback) != null;
    }

    @Override
    public void unregisterInput(SocketChannel channel) {
        unregisterSelection(channel, readSelector, inputCallbackMap, inRegInput);
    }

    @Override
    public void unregisterOutput(SocketChannel channel) {
        unregisterSelection(channel, writeSelector, outputCallbackMap, inRegOutput);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdownNow();
            outputHandlePool.shutdownNow();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            CloseUtils.close(readSelector, writeSelector);
        }
    }

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector,
                                                  int registerOps, AtomicBoolean locker,
                                                  HashMap<SelectionKey, Runnable> map,
                                                  Runnable runnable) {
        synchronized (locker) {
            locker.set(true);
            try {
                selector.wakeup();
                SelectionKey key = null;
                if (channel.isRegistered()) {
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }
                if (key == null) {
                    key = channel.register(selector, registerOps);
                    map.put(key, runnable);
                }
                return key;
            } catch (ClosedChannelException
                    | CancelledKeyException
                    | ClosedSelectorException e) {
                return null;
            } finally {
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void unregisterSelection(SocketChannel channel, Selector selector,
                                            Map<SelectionKey, Runnable> map, AtomicBoolean locker) {
        synchronized (locker) {
            locker.set(true);
            selector.wakeup();
            try {
                if (channel.isRegistered()) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                        map.remove(key);
                    }
                }
            } finally {
                locker.set(false);
                try {
                    locker.notifyAll();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handleSelection(SelectionKey key, int keyOps,
                                        HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool, AtomicBoolean locker) {
        synchronized (locker) {
            try {
                key.interestOps(key.readyOps() & ~keyOps);
            } catch (CancelledKeyException e) {
                return;
            }
        }
        Runnable runnable = null;
        try {
            runnable = map.get(key);
        } catch (Exception ignored) {
        }
        if (runnable != null && !pool.isShutdown()) {
            pool.execute(runnable);
        }
    }



    static class SelectThread extends Thread {
        private final AtomicBoolean isClosed;
        private final AtomicBoolean locker;
        private final Selector selector;
        private final HashMap<SelectionKey, Runnable> callMap;
        private final ExecutorService pool;
        private final int keyOps;


        SelectThread(String name,
                     AtomicBoolean isClosed,
                     AtomicBoolean locker,
                     Selector selector,
                     HashMap<SelectionKey, Runnable> callMap,
                     ExecutorService pool,
                     int keyOps) {
            super(name);
            this.isClosed = isClosed;
            this.locker = locker;
            this.selector = selector;
            this.callMap = callMap;
            this.pool = pool;
            this.keyOps = keyOps;
            this.setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            super.run();
            AtomicBoolean locker = this.locker;
            AtomicBoolean isClosed = this.isClosed;
            Selector selector = this.selector;
            HashMap<SelectionKey, Runnable> callMap = this.callMap;
            ExecutorService pool = this.pool;
            int keyOps = this.keyOps;

            while (!isClosed.get()) {
                try {
                    if (selector.select() == 0) {
                        waitSelection(locker);
                        continue;
                    } else if (locker.get()) {
                        waitSelection(locker);
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isValid()) {
                            handleSelection(selectionKey, keyOps, callMap, pool, locker);
                        }
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClosedSelectorException ignored) {
                    break;
                }
            }
        }
    }
}
