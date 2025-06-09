package com.p2pnetwork.message.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.p2pnetwork.routing.RoutingEntry;
import lombok.*;

import java.util.Map;
import java.util.Set;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class PromotionContent {
    private RoutingEntry[] superNodes;
    private RoutingEntry[] redundancies;

    @JsonDeserialize(contentAs = java.util.HashSet.class)
    private Map<String, Set<RoutingEntry>> metadataMap;
}