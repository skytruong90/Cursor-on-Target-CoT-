package dev.dk.cotwatch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CotParserTest {
    @Test
    void parsesBasicEvent() throws Exception {
        String xml = """
          <event version="2.0" type="a-f-A-M-F" uid="alpha1" time="2025-01-01T00:00:00Z">
            <point lat="37.245" lon="-115.805" hae="1200" ce="5" le="10"/>
          </event>
        """;
        CotEvent ev = CotParser.parse(xml);
        assertEquals("alpha1", ev.uid);
        assertEquals("a-f-A-M-F", ev.type);
        assertEquals(37.245, ev.lat, 1e-6);
        assertEquals(-115.805, ev.lon, 1e-6);
        assertEquals(1200.0, ev.hae, 1e-6);
    }
}
