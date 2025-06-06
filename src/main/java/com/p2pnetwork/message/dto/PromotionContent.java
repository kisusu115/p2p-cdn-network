package com.p2pnetwork.message.dto;

import com.p2pnetwork.routing.RoutingEntry;
import lombok.*;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class PromotionContent {
    private RoutingEntry[] superNodes;
    private RoutingEntry[] redundancies;
}