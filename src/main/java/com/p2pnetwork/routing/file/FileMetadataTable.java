package com.p2pnetwork.routing.file;

import com.p2pnetwork.routing.RoutingEntry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileMetadataTable {
    private static final FileMetadataTable INSTANCE = new FileMetadataTable();
    private final Map<String, Set<RoutingEntry>> metadataMap = new ConcurrentHashMap<>();

    public static FileMetadataTable getInstance() {
        return INSTANCE;
    }

    public synchronized void addFileMetadata(String filehash, RoutingEntry entry) {
        Set<RoutingEntry> routingEntrySetSet = metadataMap.get(filehash);

        if(routingEntrySetSet == null) {
            routingEntrySetSet = new HashSet<>();
            routingEntrySetSet.add(entry);
            metadataMap.put(filehash, routingEntrySetSet);
            return;
        }

        routingEntrySetSet.add(entry);
    }

    public synchronized RoutingEntry getAnyFileEntry(String filehash) {
        Set<RoutingEntry> entries = metadataMap.get(filehash);
        if (entries == null || entries.isEmpty()) return null;
        return entries.iterator().next();
    }

    public void printTable() {
        System.out.println("==== [FileMetadataTable] ====");
        metadataMap.forEach((fileKey, entries) -> {
            System.out.println("  " + fileKey + " : " + entries.size() + " node(s)");
            entries.forEach(entry -> System.out.println("    - " + entry.getNodeId()));
        });
        System.out.println("=============================");
    }
}