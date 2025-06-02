package com.p2pnetwork.routing;

import com.p2pnetwork.node.NodeRole;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class RoutingEntry {
    private String nodeId;
    private String ip;
    private int port;
    private NodeRole role;
}