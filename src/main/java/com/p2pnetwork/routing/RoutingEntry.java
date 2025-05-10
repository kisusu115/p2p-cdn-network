package com.p2pnetwork.routing;

import com.p2pnetwork.node.NodeRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoutingEntry implements Serializable {
    private String nodeId;
    private String ip;
    private int port;
    private NodeRole role;

    public NodeRole setNodeRole(NodeRole role) {
        this.role=role;
        return role;
    }
}
