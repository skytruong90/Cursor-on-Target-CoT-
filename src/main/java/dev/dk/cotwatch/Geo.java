package dev.dk.cotwatch;

import java.util.List;

public class Geo {

    // Haversine distance in kilometers
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0088; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R*c;
    }

    // Ray casting point-in-polygon
    public static boolean pointInPolygon(double lat, double lon, List<double[]> poly) {
        boolean inside = false;
        for (int i=0, j=poly.size()-1; i<poly.size(); j=i++) {
            double[] pi = poly.get(i);
            double[] pj = poly.get(j);
            double xi = pi[0], yi = pi[1];
            double xj = pj[0], yj = pj[1];

            boolean intersect = ((yi > lon) != (yj > lon)) &&
                    (lat < (xj - xi) * (lon - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}

