package dev.dk.cotwatch;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name="cot-watch", mixinStandardHelpOptions = true,
        description="CoT geofence & speed alerting with KML/JSON outputs.")
public class App implements Runnable {

    public static void main(String[] args) {
        int exit = new CommandLine(new App())
                .addSubcommand(new Udp())
                .addSubcommand(new Files())
                .execute(args);
        System.exit(exit);
    }

    @Override public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name="udp", description="Listen for CoT over UDP.")
    static class Udp implements Callable<Integer> {
        @Option(names="--port", defaultValue="6969") int port;
        @Option(names="--bind", defaultValue="0.0.0.0") String bind;
        @Option(names="--mcast", description="Optional multicast group (e.g. 239.2.3.1)")
        String multicast;

        @Option(names="--out", defaultValue="output") Path outDir;
        @Option(names="--geofence", description="polygon:lat,lon;lat,lon;...")
        String polyFence;
        @Option(names="--radius", description="center:lat,lon;km:X")
        String radFence;
        @Option(names="--speed-kts", description="Speed alert threshold (knots)")
        Double speedKts;

        @Override public Integer call() throws Exception {
            Files.createDirectories(outDir);
            Rules rules = Rules.fromCli(polyFence, radFence, speedKts);
            KmlWriter kml = new KmlWriter(outDir.resolve("alerts.kml"));
            Path jsonl = outDir.resolve("alerts.jsonl");

            DatagramSocket socket;
            if (multicast != null && !multicast.isBlank()) {
                var group = InetAddress.getByName(multicast);
                var mcast = new MulticastSocket(port);
                mcast.joinGroup(new InetSocketAddress(group, port), NetworkInterface.getByInetAddress(InetAddress.getByName(bind)));
                socket = mcast;
                System.out.printf("[*] Listening multicast %s:%d%n", multicast, port);
            } else {
                socket = new DatagramSocket(new InetSocketAddress(bind, port));
                System.out.printf("[*] Listening UDP %s:%d%n", bind, port);
            }

            byte[] buf = new byte[65507];
            var speedCache = new Rules.SpeedCache();

            while (true) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                String xml = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8);
                try {
                    CotEvent ev = CotParser.parse(xml);
                    Rules.Result res = rules.evaluate(ev, speedCache);
                    if (res.alert()) {
                        String msg = String.format("ALERT uid=%s type=%s lat=%.6f lon=%.6f spdKts=%.1f cause=%s",
                                ev.uid, ev.type, ev.lat, ev.lon, res.speedKts(), res.reason());
                        System.out.println(msg);

                        kml.addPlacemark(ev, res);
                        Files.writeString(jsonl, res.toJson(ev) + System.lineSeparator(),
                                StandardCharsets.UTF_8,
                                Files.exists(jsonl) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
                    }
                } catch (Exception e) {
                    System.err.println("[!] Parse error: " + e.getMessage());
                }
            }
        }
    }

    @Command(name="files", description="Process CoT XML files in a folder.")
    static class Files implements Callable<Integer> {
        @Parameters(index="0", paramLabel="--in", description="Input folder", defaultValue = "samples")
        Path inDir;
        @Option(names="--out", defaultValue="output") Path outDir;
        @Option(names="--geofence", description="polygon:lat,lon;lat,lon;...")
        String polyFence;
        @Option(names="--radius", description="center:lat,lon;km:X")
        String radFence;
        @Option(names="--speed-kts", description="Speed alert threshold (knots)")
        Double speedKts;

        @Override public Integer call() throws Exception {
            Files.createDirectories(outDir);
            Rules rules = Rules.fromCli(polyFence, radFence, speedKts);
            KmlWriter kml = new KmlWriter(outDir.resolve("alerts.kml"));
            Path jsonl = outDir.resolve("alerts.jsonl");
            var speedCache = new Rules.SpeedCache();

            List<Path> files = Files.walk(inDir)
                    .filter(p -> p.toString().endsWith(".xml") || p.toString().endsWith(".cot"))
                    .sorted()
                    .toList();

            for (Path p : files) {
                String xml = Files.readString(p);
                try {
                    CotEvent ev = CotParser.parse(xml);
                    Rules.Result res = rules.evaluate(ev, speedCache);
                    if (res.alert()) {
                        String msg = String.format("ALERT uid=%s type=%s lat=%.6f lon=%.6f spdKts=%.1f cause=%s file=%s",
                                ev.uid, ev.type, ev.lat, ev.lon, res.speedKts(), res.reason(), p.getFileName());
                        System.out.println(msg);
                        kml.addPlacemark(ev, res);
                        Files.writeString(jsonl, res.toJson(ev) + System.lineSeparator(),
                                StandardCharsets.UTF_8,
                                Files.exists(jsonl) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
                    }
                } catch (Exception e) {
                    System.err.println("[!] Parse error in " + p.getFileName() + ": " + e.getMessage());
                }
            }
            kml.close();
            System.out.println("[OK] Done at " + Instant.now());
            return 0;
        }
    }
}

