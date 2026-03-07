# Rail DB Analysis

Date: 2026-03-05
Scope: railcar pallet math parity across Java `rail-print` and Excel VBA macro flow.

## Goal

Produce identical pallet breakdowns per railcar card from WMS source-of-truth data, with explicit
`CAN`, `DOM`, and `KEV` totals.

## Source Of Truth

- Rail row extraction:
    - `WMSP.TRLR` (train, vehicle, sequence)
    - `WMSP.RCVTRK`
    - `WMSP.RCVINV` (load number)
    - `WMSP.RCVLIN` (cases)
    - `WMSP.ALT_PRTMST` (`ALT_PRT_TYP='UPC'` short code)
- Family classification:
    - `WMSP.PRTMST.PRTFAM`
    - `WMSP.PRTMST.UC_PARS_FLG` (`1` treated as CAN)
- Cases-per-pallet:
    - `WMSP.PRTFTP` with `DEFFTP_FLG=1`
    - `WMSP.PRTFTP_DTL` where `PAL_FLG=1`

## Math Rule

For each item on a railcar:

`pallets = CEILING(cases / casesPerPallet)`

Then sum by family bucket:

- `CAN`
- `DOM`
- `KEV` (separate, not folded into DOM)

## Grouping Key

Operationally one card per traincar:

- aggregate key: `(train, seq, vehicle)`
- display: merged load numbers on the same card

## Deep Findings

1. Java undercount root cause (fixed):
    - Ambiguous short-code footprints were dropped entirely, causing missing pallet contributions.
    - Resolver updated to retain candidates when normalized family bucket is consistent (`CAN`/`DOM`/`KEV`) and choose
      deterministic UPP.

2. Excel undercount root cause (fixed):
    - Static `Item_Family` sheet lacked many active short codes (`20571`, `20554`, etc.).
    - VBA now refreshes a train-specific DB footprint map and uses that in formulas with fallback only if needed.

3. Train ID matching:
    - Java rail query now expects full train ID in DB filter (`VC_TRAIN_NUM = ?`), preventing accidental overlap
      behavior from short numeric matching.
    - VBA footprint refresh query was aligned to full train ID filter as well.

## Validation Samples

### Train `0307`

DB baseline and Java now match exactly:

- `1/TPIX3316 -> 0/76/0`
- `2/TPIX3377 -> 0/76/0`
- `4/TPIX3009 -> 0/76/0`
- `5/TPIX3136 -> 0/76/0`
- `6/TPIX3167 -> 10/66/13`
- `7/TPIX3267 -> 0/76/0`
- `8/TPIX3301 -> 0/76/0`
- `211/TPIX3072 -> 0/76/0`
- `212/TPIX3192 -> 0/76/0`
- `213/TPIX3259 -> 0/76/0`
- `301/TPIX3290 -> 76/0/0`
- `302/TPIX3123 -> 0/76/0`
- `305/TPIX3289 -> 0/76/0`
- `307/TPIX3240 -> 76/0/0`

Format above: `CAN/DOM/KEV`.

### Random Train `JC08312025`

Independent SQL recomputation matched Java output line-for-line:

- `310/TPIX3035 -> 0/76/0`
- `311/TPIX3168 -> 0/76/0`
- `312/TPIX3271 -> 0/72/0`
- `313/TPIX3278 -> 0/54/0`
- `314/TPIX3279 -> 0/76/0`
- `315/TPIX3325 -> 0/76/0`
- `316/TPIX3349 -> 0/76/0`
- `317/TPIX3087 -> 0/67/0`

## Recommendations

1. Keep rail family and footprint lookups DB-driven in both Java and VBA.
2. Keep `DEFFTP_FLG=1` footprint policy unless business provides explicit override rules.
3. Preserve per-run diagnostics:
    - unresolved short codes
    - missing items used in card math
4. Keep reconciliation checks available for regression:
    - DB baseline vs Java preview
    - DB baseline vs VBA `_TrainDetail` outputs
