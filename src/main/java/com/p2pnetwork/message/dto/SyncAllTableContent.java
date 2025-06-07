package com.p2pnetwork.message.dto;

import com.p2pnetwork.routing.RoutingEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class SyncAllTableContent {
    private RoutingEntry[] superNodes;
    private RoutingEntry[] redundancies;

    private RoutingEntry superNodeEntry;
    private RoutingEntry redundancyEntry;
    private RoutingEntry[] routingTable;
}
