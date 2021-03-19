package net.ccc.library.clink.core.ds;

public class BytePriorityNode<Item> {
    public byte priority;
    public Item item;
    public BytePriorityNode<Item> next;

    public BytePriorityNode(Item item) {
        this.item = item;
    }

    public void appendWithPriority(BytePriorityNode<Item> node) {
        if (next == null) {
            next = node;
        } else {
            BytePriorityNode<Item> after = this.next;
            if (after.priority < node.priority) {
                this.next = node;
                node.next = after;
            } else {
                after.appendWithPriority(node);
            }
        }
    }
}
