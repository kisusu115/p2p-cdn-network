package com.p2pnetwork.util;

import java.security.MessageDigest;

public class NodeIdGenerator {
    public static String generateNodeId(double lat, double lon, String ip, int port) throws Exception {
        String geohash = GeoHashUtils.encode(lat, lon, 5); // 5자리 Geohash
        String ipPortHash = generateIpPortHash(ip, port);
        return geohash + "_" + ipPortHash;
    }

    private static String generateIpPortHash(String ip, int port) throws Exception {
        String input = ip + ":" + port;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%02x", hashBytes[i]));
        }
        return sb.substring(0, 8);
    }
}
