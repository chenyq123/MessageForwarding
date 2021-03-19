package net.ccc.library.clink.impl.async;

import net.ccc.library.clink.core.Frame;
import net.ccc.library.clink.core.IoArgs;
import net.ccc.library.clink.core.SendPacket;
import net.ccc.library.clink.core.ds.BytePriorityNode;
import net.ccc.library.clink.frames.*;

import java.io.Closeable;
import java.io.IOException;

public class AsyncPacketReader implements Closeable {
    private final PacketProvider provider;
    private volatile IoArgs args = new IoArgs();

    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    synchronized void cancel(SendPacket packet) {
        if (nodeSize == 0) {
            return;
        }
        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            break;
                        }
                    }
                    CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                    appendNewFrame(cancelSendFrame);

                    provider.completePacket(packet, false);
                    break;
                }
            }
        }
    }

    private void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            node = removeNode.next;
        } else {
            before.next = removeNode.next;
        }
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }
        SendPacket packet = provider.takePacket();
        if (packet != null) {
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }

    boolean requestSendHeartbeatFrame() {
        synchronized (this) {
            for (BytePriorityNode<Frame> x = node; x != null; x = x.next) {
                Frame frame = x.item;
                if (frame.getBodyType() == Frame.TYPE_COMMAND_HEARTBEAT) {
                    return false;
                }
            }
        }
        appendNewFrame(new HeartbeatSendFrame());
        return true;
    }

    @Override
    public synchronized void close() {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completePacket(packet, false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (node != null) {
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    private synchronized Frame getCurrentFrame() {
        if (node == null) {
            return null;
        }
        return node.item;
    }

    private short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }
        try {
            if (currentFrame.handle(args)) {
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    provider.completePacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }
                popCurrentFrame();
            }
            return args;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }


    interface PacketProvider {
        SendPacket takePacket();

        void completePacket(SendPacket packet, boolean isSucceed);
    }
}
