package com.p2pnetwork.node;

public enum NodeRole {
    PEER,
    REDUNDANCY,
    SUPERNODE,
    BOOTSTRAP;

    public boolean isAtLeast(NodeRole other) {
        return this.ordinal() >= other.ordinal();
    }
}
