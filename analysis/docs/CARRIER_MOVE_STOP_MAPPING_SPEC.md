# Carrier Move to Shipment Stop Mapping Spec (Phase 0)

Date: 2026-02-26  
Environment validated: `WMSP` (PROD, read-only)

## Executive Summary

- Authoritative Carrier Move mapping is:
  - `WMSP.STOP.CAR_MOVE_ID = :carrier_move_id`
  - `WMSP.SHIPMENT.STOP_ID = WMSP.STOP.STOP_ID`
- Authoritative stop order field is:
  - `WMSP.STOP.STOP_SEQ`
- `WMSP.SHIPMENT.TMS_MOVE_ID` is not usable for this feature in current PROD data:
  - In sampled recent data, it is null for all rows checked.
- `WMSP.STOP.TMS_STOP_SEQ` is not reliable as primary ordering:
  - Frequently `0` and often mismatched vs `STOP_SEQ`.

## Validated Sample Mapping (Requested IDs)

Using `STOP.CAR_MOVE_ID`:

- `205109` -> `8000473513` (stop 1), `8000473512` (stop 2)
- `205110` -> `8000473494` (stop 1), `8000473493` (stop 2)
- `205378` -> `8000474229` (stop 1)

This exactly matches the observed examples.

## DB Mapping Spec (Tables, Joins, Fields)

Tables:

- `WMSP.STOP`
  - `STOP_ID` (PK/identifier)
  - `CAR_MOVE_ID` (carrier move ID)
  - `STOP_SEQ` (primary stop order field)
  - `TMS_STOP_SEQ` (secondary/reference only; not reliable)
- `WMSP.SHIPMENT`
  - `SHIP_ID` (shipment/800 number)
  - `STOP_ID` (FK-like link to `STOP`)
  - `SHPSTS` (shipment status)
  - `ADDDTE` (created datetime)

Join path:

1. Filter stops by `STOP.CAR_MOVE_ID = :cmid`
2. Join shipments by `SHIPMENT.STOP_ID = STOP.STOP_ID`

## Canonical Query (Phase 1 implementation source)

```sql
SELECT
  st.car_move_id,
  st.stop_id,
  st.stop_seq,
  st.tms_stop_seq,
  s.ship_id,
  s.shpsts,
  s.adddte
FROM wmsp.stop st
JOIN wmsp.shipment s
  ON s.stop_id = st.stop_id
WHERE st.car_move_id = :carrier_move_id
ORDER BY st.stop_seq ASC, s.ship_id ASC;
```

Notes:

- `ORDER BY stop_seq, ship_id` makes output deterministic.
- Secondary sort by `ship_id` is only tie-break behavior.

## Deterministic Stop Strategy

Primary stop truth:

- Use `STOP.STOP_SEQ` as the stop number printed in STOP box.

Deterministic ordering:

- Sort by `STOP_SEQ ASC`.
- For ties (multiple shipments on same stop), sort by `SHIP_ID ASC`.

Why this is best available truth:

- `STOP_SEQ` is a first-class stop field on `WMSP.STOP`.
- It is consistently populated in tested data.
- `TMS_STOP_SEQ` is frequently `0` and diverges from real sequence.
- `SHIPMENT.TMS_MOVE_ID` is not populated in recent tested rows.

## Edge Cases and Handling Notes

1. Multiple shipments on the same stop sequence
- Seen in non-numeric/route-style moves (example patterns like `FR...`).
- Handling: group by stop sequence for per-stop actions; within stop, process shipments sorted by `SHIP_ID`.

2. Non-contiguous stop sequences (e.g., starts at 2, gaps)
- Exists in historical numeric moves.
- Handling: do not renumber for STOP box; preserve `STOP_SEQ` as system-of-record.
- UI can still show positional index separately if needed, but printed STOP should be `STOP_SEQ`.

3. No shipment rows for a CMID
- Treat as validation error: "Carrier Move not found or has no shipments."

4. Status filtering ambiguity (`SHPSTS`)
- Current shipment mode does not apply status filtering beyond existence.
- Recommended Phase 1 behavior: include all resolved rows by default to avoid hidden shipments; show status in preview.
- Optional future hardening: configurable include/exclude status list after business confirmation.

5. Duplicate rows risk
- Use `SELECT DISTINCT` only if duplicate joins are observed in implementation testing.
- Current join path (`STOP` -> `SHIPMENT`) did not show missing-stop join issues in sampled diagnostics.

## Additional Validation Findings (Live Discovery)

- `STOP_SEQ` nulls in tested windows: none.
- `TMS_STOP_SEQ` quality: many rows at `0`; many mismatches vs `STOP_SEQ`.
- `SHIPMENT.TMS_MOVE_ID`: null in tested recent joined rows.

## Implementation Readiness

Phase 0 is complete for stop resolution logic.

Approved implementation basis:

- CMID lookup via `STOP.CAR_MOVE_ID`
- Shipment resolution via `SHIPMENT.STOP_ID`
- Stop order and STOP label value from `STOP.STOP_SEQ`
