package com.p2pnetwork.util;

import ch.hsr.geohash.GeoHash;

public class GeoHashUtils {
    public static String encode(double lat, double lon, int precision) {
        return GeoHash.withCharacterPrecision(lat, lon, precision).toBase32();
    }
}
