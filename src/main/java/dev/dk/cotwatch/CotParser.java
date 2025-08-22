package dev.dk.cotwatch;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class CotParser {

    public static CotEvent parse(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setExpandEntityReferences(false);
        var db = dbf.newDocumentBuilder();
        var doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        Element e = (Element) doc.getElementsByTagName("event").item(0);
        if (e == null) throw new IllegalArgumentException("Missing <event>");

        CotEvent ev = new CotEvent();
        ev.uid = optAttr(e, "uid");
        ev.type = optAttr(e, "type");
        String time = optAttr(e, "time");
        ev.time = time != null && !time.isBlank() ? Instant.parse(time) : Instant.now();

        Element pt = (Element) e.getElementsByTagName("point").item(0);
        if (pt == null) throw new IllegalArgumentException("Missing <point>");
        ev.lat = Double.parseDouble(pt.getAttribute("lat"));
        ev.lon = Double.parseDouble(pt.getAttribute("lon"));
        if (pt.hasAttribute("hae")) ev.hae = Double.parseDouble(pt.getAttribute("hae"));
        if (pt.hasAttribute("ce"))  ev.ce  = Double.parseDouble(pt.getAttribute("ce"));
        if (pt.hasAttribute("le"))  ev.le  = Double.parseDouble(pt.getAttribute("le"));

        // Optional speed from detail (some producers embed it)
        NodeList details = e.getElementsByTagName("detail");
        if (details.getLength() > 0) {
            Element det = (Element) details.item(0);
            if (det.hasAttribute("speedKts")) {
                try { ev.speedKts = Double.parseDouble(det.getAttribute("speedKts")); } catch (Exception ignore) {}
            }
        }
        return ev;
    }

    private static String optAttr(Element e, String name) {
        return e.hasAttribute(name) ? e.getAttribute(name) : null;
    }
}

