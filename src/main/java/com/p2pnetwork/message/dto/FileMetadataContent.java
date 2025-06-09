package com.p2pnetwork.message.dto;

import com.p2pnetwork.routing.RoutingEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class FileMetadataContent {
    boolean found;
    String fileHash;
    RoutingEntry targetEntry;
}
