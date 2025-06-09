package com.p2pnetwork.message.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.p2pnetwork.routing.RoutingEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class SyncAllTableContent {
    private RoutingEntry[] superNodes;
    private RoutingEntry[] redundancies;

    private RoutingEntry superNodeEntry;
    private RoutingEntry redundancyEntry;
    private RoutingEntry[] routingTable;

    @JsonDeserialize(contentAs = java.util.HashSet.class)
    private Map<String, Set<RoutingEntry>> metadataMap;
}
