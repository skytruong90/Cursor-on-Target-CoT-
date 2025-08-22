package dev.dk.cotwatch;

import java.time.Instant;

public class CotEvent {
    public String uid;
    public String type;
    public Instant time;
    public double lat;
    public double lon;
    public Double hae;      // altitude (HAE), meters
    public Double ce;       // circular error (m)
    public Double le;       // linear error (m)
    public Double speedKts; // optional kts (if present or derived)

    public CotEvent() {}
}

