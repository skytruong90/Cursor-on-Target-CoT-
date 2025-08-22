package dev.dk.cotwatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class KmlWriter implements AutoCloseable {
    private final Path path;
    private final StringBuilder sb = new StringBuilder();

    public KmlWriter(Path path) throws IOException {
        this.path = path;
        sb.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <name>COT Watch Alerts</name>
            """);
    }

    public synchronized void addPlacemark(CotEvent ev, Rules.Result res) {
        sb.append(String.format("""
              <Placemark>
                <name>%s</name>
                <description><![CDATA[type=%s speedKts=%.1f reason=%s]]></description>
                <Point><coordinates>%.6f,%.6f,%s</coordinates></Point>
              </Placemark>
            """,
            ev.uid != null ? ev.uid : "unknown",
            ev.type != null ? ev.type : "n/a",
            res.speedKts() != null ? res.speedKts() : 0.0,
            res.reason(),
            ev.lon, ev.lat, ev.hae != null ? ev.hae.toString() : "0"
        ));
    }

    @Override public synchronized void close() throws IOException {
        sb.append("""
              </Document>
            </kml>
            """);
        Files.createDirectories(path.getParent());
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
                Files.exists(path) ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE);
    }
}

