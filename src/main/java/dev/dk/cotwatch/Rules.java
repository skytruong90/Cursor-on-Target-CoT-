package dev.dk.cotwatch;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Rules {

    public record Circle(double lat, double lon, double km) {}
    public record Config(List<double[]> polygon, Circle circle, Double speedKts) {}

    private final Config cfg;

    private Rules(Config cfg) { this.cfg = cfg; }

    public static Rules fromCli(String polyFence, String radFence, Double speedKts) {
        List<double[]> poly = null;
        if (polyFence != null && polyFence.startsWith("polygon:")) {
            poly = new ArrayList<>();
            String pts = polyFence.substring("polygon:".length());
            for (String s : pts.split(";")) {
                String[] parts = s.split(",");
                poly.add(new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())});
            }
        }
        Circle circle = null;
        if (radFence != null && radFence.startsWith("center:")) {
            String[] parts = radFence.split(";");
            String[] ll = parts[0].substring("center:".length()).split(",");
            double lat = Double.parseDouble(ll[0].trim());
            double lon = Double.parseDouble(ll[1].trim());
            double km = 5.0;
            for (String p : parts) {
                if (p.startsWith("km:")) km = Double.parseDouble(p.substring(3).trim());
            }
            circle = new Circle(lat, lon, km);
        }
        return new Rules(new Config(poly, circle, speedKts));
    }

    // Speed cache per UID to derive speed if not present
    public static class SpeedCache {
        private static class Sample { Instant t; double lat, lon; }
        private final Map<String, Sample> last = new HashMap<>();
        public Double deriveKts(CotEvent ev) {
            if (ev.uid == null) return null;
            Instant now = ev.time != null ? ev.time : Instant.now();
            Sample prev = last.get(ev.uid);
            Sample cur = new Sample(); cur.t = now; cur.lat = ev.lat; cur.lon = ev.lon;
            last.put(ev.uid, cur);
            if (prev == null) return null;
            double km = Geo.haversineKm(prev.lat, prev.lon, ev.lat, ev.lon);
            double hours = Duration.between(prev.t, now).toMillis() / 3600000.0;
            if (hours <= 0) return null;
            double kts = km / 1.852 / hours; // km/h -> knots
            return kts;
        }
    }

    public record Result(boolean alert, String reason, Double speedKts) {
        public boolean alert() { return alert; }
        public String toJson(CotEvent ev) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uid", ev.uid);
            m.put("type", ev.type);
            m.put("lat", ev.lat);
            m.put("lon", ev.lon);
            m.put("time", ev.time != null ? ev.time.toString() : null);
            m.put("speedKts", speedKts);
            m.put("reason", reason);
            try { return new ObjectMapper().writeValueAsString(m); } catch (Exception e) { return "{}"; }
        }
    }

    public Result evaluate(CotEvent ev, SpeedCache cache) {
        Double vKts = ev.speedKts != null ? ev.speedKts : cache.deriveKts(ev);
        boolean geoHit = false;
        String reason = null;

        if (cfg.polygon() != null && !cfg.polygon().isEmpty()) {
            boolean inside = Geo.pointInPolygon(ev.lat, ev.lon, cfg.polygon());
            if (!inside) { geoHit = true; reason = "outside-polygon"; }
        }
        if (cfg.circle() != null) {
            double distKm = Geo.haversineKm(ev.lat, ev.lon, cfg.circle().lat(), cfg.circle().lon());
            if (distKm > cfg.circle().km()) { geoHit = true; reason = "outside-radius"; }
        }
        boolean speedHit = false;
        if (cfg.speedKts() != null && vKts != null && vKts > cfg.speedKts()) {
            speedHit = true;
            reason = reason == null ? "speed>" + cfg.speedKts() : reason + "+speed";
        }
        boolean alert = geoHit || speedHit;
        return new Result(alert, alert ? reason : "ok", vKts);
    }
}

