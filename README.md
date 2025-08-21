# COT Watch — Geofence & Speed Alerts for Cursor-on-Target Streams (Java)

A lightweight Java tool that ingests **Cursor-on-Target (CoT)** XML events over UDP or from files, applies **geofence + speed** rules, and exports **KML** so you can visualize alerts in Google Earth or other tools.

> ⚠️ Disclaimer: This project is for educational, unclassified use. Always follow your organization’s data-handling, OPSEC, and classification policies.

---

## Why I Built This

In defense environments, situational awareness often rides on **CoT** event streams (used by TAK/ATAK and interoperable tools). I wanted a compact Java project that shows:
- **Parsing CoT** XML safely and predictably
- **Applying mission rules** (geo & kinematics) in a reusable engine
- **Producing audit artifacts** (KML + JSON) that work offline/air-gapped

This repo is intentionally small, easy to review, and friendly for unit testing—perfect for interviews and portfolio reviews.

---

## What It Does

- **Ingests CoT** events
  - From a **UDP socket** (default: `239.2.3.1:6969` multicast or `0.0.0.0:6969` unicast)
  - From **.xml** or **.cot** files (batch processing)
- **Parses** core CoT fields: UID, type, time, latitude, longitude, altitude, speed (where present)
- **Rules Engine**
  - **Geofence**: polygon or radius-based includes/excludes
  - **Speed threshold**: flag units exceeding configured speed
- **Outputs**
  - **Console alerts** for matches
  - **KML file** with placemarks for each alert
  - **JSONL log** for downstream analysis
- **Tested** with sample CoT messages included under `samples/`

---

## Example Use Cases (Unclassified)

- Quick-look **range safety**: trigger on fast movers exiting a boundary
- **Blue force** monitoring: alert if a unit leaves an assigned sector
- **Training**/exercise analytics: batch run a folder of captured CoT logs

---

## Project Structure

---

## Quick Start

## Build & Run

## 1) Build
mvn -q -DskipTests package

## 2a) Run on a UDP port (unicast)
java -jar target/cot-watch-1.0.0.jar udp --port 6969

## 2b) Run on a UDP multicast group
java -jar target/cot-watch-1.0.0.jar udp --port 6969 --mcast 239.2.3.1

## 2c) Batch process from files
java -jar target/cot-watch-1.0.0.jar files --in samples --out output \
  --geofence "polygon:37.242,-115.819;37.252,-115.80;37.24,-115.78" \
  --radius "center:37.245,-115.805;km:5" \
  --speed-kts 300
