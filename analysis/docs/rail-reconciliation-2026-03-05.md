# Rail Reconciliation Evidence (2026-03-05)

## Scope
Independent DB baseline validation against Java `rail-print` output after logic updates.

## Method
1. Query rail cases from WMS using the same train filter and rail row sources as the app.
2. Resolve family and cases-per-pallet from WMS (`PRTFAM`, `UC_PARS_FLG`, `DEFFTP_FLG=1`, `PAL_FLG=1`).
3. Compute pallets per short code as `CEIL(total_cases / casesPerPallet)`.
4. Aggregate to railcar totals by family buckets (`CAN`, `DOM`, `KEV`).
5. Compare with Java CLI preview output.

## Train: JC07292025 (random JC)
Java preview:
- 202 / TPIX3232 -> CAN 0, DOM 76, KEV 0

DB baseline:
- 202 / TPIX3232 -> CAN 0, DOM 76, KEV 0

Result: MATCH

## Train: JC01272026 (random JC)
Java preview:
- 171 / TPIX3116 -> 0/76/0
- 172 / TPIX3146 -> 0/76/0
- 173 / TPIX3220 -> 0/76/0
- 174 / TPIX3338 -> 0/76/0
- 175 / TPIX3003 -> 0/76/0
- 176 / TPIX3123 -> 0/76/0
- 177 / TPIX3167 -> 0/76/0
- 178 / TPIX3212 -> 0/76/0
- 179 / TPIX3228 -> 0/76/0
- 180 / TPIX3349 -> 0/76/0
- 181 / TPIX3198 -> 0/76/0

DB baseline:
- 171 / TPIX3116 -> 0/76/0
- 172 / TPIX3146 -> 0/76/0
- 173 / TPIX3220 -> 0/76/0
- 174 / TPIX3338 -> 0/76/0
- 175 / TPIX3003 -> 0/76/0
- 176 / TPIX3123 -> 0/76/0
- 177 / TPIX3167 -> 0/76/0
- 178 / TPIX3212 -> 0/76/0
- 179 / TPIX3228 -> 0/76/0
- 180 / TPIX3349 -> 0/76/0
- 181 / TPIX3198 -> 0/76/0

Result: MATCH

## Notes
- Full train ID filter is enforced (`VC_TRAIN_NUM = ?`) for parity across Java and VBA.
- KEV is now tracked as its own bucket in preview/render paths.
- Missing footprint items are surfaced on rendered cards as `MISSING: <count>`.
