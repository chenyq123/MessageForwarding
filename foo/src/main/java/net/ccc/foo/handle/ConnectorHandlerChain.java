package net.ccc.foo.handle;

public abstract class ConnectorHandlerChain<Model> {
    private volatile ConnectorHandlerChain<Model> next;

    public ConnectorHandlerChain<Model> appendLast(ConnectorHandlerChain<Model> newChain) {
        if (newChain == this || this.getClass().equals(newChain.getClass())) {
            return this;
        }

        synchronized (this) {
            if (next == null) {
                next = newChain;
                return newChain;
            }
            return next.appendLast(newChain);
        }
    }

    public synchronized boolean remove(Class<? extends ConnectorHandlerChain<Model>> clx) {
        if (this.getClass().equals(clx)) {
            return false;
        }
        synchronized (this) {
            if (next == null) {
                return false;
            } else if(next.getClass().equals(clx)){
                next = next.next;
                return true;
            } else {
                return next.remove(clx);
            }
        }
    }

    synchronized boolean handle(ConnectorHandler handler, Model model) {
        ConnectorHandlerChain<Model> next = this.next;
        if (consume(handler, model)) {
            return true;
        }

        boolean consumed = next != null && next.handle(handler, model);
        if (consumed) {
            return true;
        }

        return consumeAgain(handler, model);
    }

    protected abstract boolean consume(ConnectorHandler handler, Model model);
    protected  boolean consumeAgain(ConnectorHandler handler, Model model) {
        return false;
    }
}
