package com.p2pnetwork.node;

public enum NodeRole {
    PEER,
    SUPERNODE,
    BOOTSTRAP;

    public boolean isAtLeast(NodeRole other) {
        return this.ordinal() >= other.ordinal();
    }
}
