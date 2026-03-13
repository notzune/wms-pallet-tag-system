# QA PuTTY / Telnet Discovery

Date: 2026-03-12

## Scope

This document captures what was verified against the QA terminal host at `10.19.96.122:4550` over Telnet, what database access was confirmed in QA, how terminal identity behaves, what menu/navigation behavior was discovered, and the practical limits of automating trailer load/unload workflows from this environment.

## Connectivity Verified

- QA terminal host reachable at `10.19.96.122:4550` using Telnet.
- PuTTY GUI configuration from the supplied screenshot matches the reachable endpoint.
- PuTTY/Plink is installed locally:
  - `C:\Program Files\PuTTY\putty.exe`
  - `C:\Program Files\PuTTY\plink.exe`
- Raw Telnet negotiation succeeds when the client responds as a VT-style terminal and advertises window size.

## QA Database Verified

- QA Oracle login succeeded with:
  - user: `wmsd`
  - password: `Password1`
  - host: `10.19.96.121`
  - port: `1521`
  - service: `jcnwmsdbd01`
- Verified database identity:
  - `DB_NAME = jcnwmsdb`
- `USER = WMSD`

## Terminal families corrected

The correct RF/vehicle terminal family for this environment is `VMA`, not `LXE`.

Verified in QA:

- `DEVMST` contains `VMA001` through at least `VMA500`
- `RF_TERM_MST` maps these as `TERM_TYP = vehicle`
- warehouse is `3002`
- locale is `US_ENGLISH`

This correction matters for future clean-terminal testing.

## Important Terminal Behavior

### 1. Terminal IDs are real application objects

The terminal prompt is not just a free-form login banner. Terminal IDs map to configured devices in QA.

Relevant tables:

- `DEVMST`
- `RF_TERM_MST`
- `DEVICE_CONTEXT`

Examples confirmed in `DEVMST` / `RF_TERM_MST`:

- `FREDDY`
- `LXE241`
- `LXE242`
- `LXE150`
- `RDTSR`

### 2. Different terminal IDs behave differently

- `FREDDY` resumes an already logged-in stateful application session.
- `VMA###` terminals are the correct vehicle-terminal family to use for clean RF testing.
- previously tested `LXE...` terminals also showed login screens, but the user later clarified `VMA` is the right family for this site.
- Some workstation-style device IDs such as `TESTGUI` did not proceed from the terminal prompt during testing.
- The QA Oracle credential `wmsd / Password1` is valid for the QA database but was rejected by the WMS application login on `LXE241` with `Bad Login - Please Retry`.

### 3. Session state persists across reconnects

Reconnecting with `FREDDY` does not reset the app to a neutral home screen. It resumes whatever form/menu was last active.

Observed resumed states included:

- main menu
- yard menu
- yard audit
- reverse receipt
- logout confirmation

This is the biggest operational risk for unattended automation.

### 4. The host buffers keystrokes across prompts

If a prompt is cleared and the next keystroke arrives too quickly, the character can spill into the next screen and be interpreted as input there. This caused false errors such as:

- `Invalid Option - Please Retry`
- `Invalid Response - Press Enter`
- `Invalid Inventory ID - Press Enter`

This makes naive stdin piping unreliable.

## Telnet Automation Findings

### What works

- Raw socket/Telnet automation works if the client handles negotiation.
- The server expected at least:
  - terminal type negotiation (`TERMINAL-TYPE`)
  - window size negotiation (`NAWS`)
  - suppress-go-ahead / echo handling
- Responding as `VT220` was sufficient to reach the application.
- ANSI cursor-addressed screens can be reconstructed in software.
- Function-key escape sequences are accepted by the host.
- Some prompts consume a single character immediately and do not want `Enter`.
- Using the saved PuTTY QA profile behavior (`TerminalType = xterm`) with a bare carriage return for terminal submission works better than `VT220` plus naive line endings.
- Sending terminal ID `FREDDY` followed by bare `CR` reaches the WMS `Login` screen reliably enough for scripted testing.

### What did not work reliably

- Simple `plink` stdin redirection was not sufficient for robust navigation.
- Stateless request/response assumptions are wrong because the app is screen-oriented and stateful.
- Reusing a dirty terminal ID without first restoring a known screen is unsafe.
- Sending `CRLF` after terminal ID entry caused the host to treat the `LF` as an extra response and often produce `Invalid Terminal ID - Press Enter`.
- Character timing matters. If the first character arrives before the field is ready, the host can drop it; this was observed when `FREDDY` was received as `REDDY`.

## Menu Discovery

## Main menu discovered

Using `FREDDY`, the following top-level menu was captured:

```text
0    Undirected Menu
 1 Picking Menu      6 Cycle Count Menu
 2 Inventory Menu    7 Yard Menu
 3 Receiving Menu    8 Workflow Menu
 4 Shipping Menu     9 Directed Work
 5 Production Menu
   Enter Option:
```

## Yard menu discovered

```text
1    Yard Menu
 1 Select Yard Work
 2 Yard Audit
   Enter Option:
```

## Receiving menu discovered

At one point the `Receiving Menu` was reached from the persisted `FREDDY` session:

```text
1 Load Receive      6 Reopen Trailer
2 Case Receive      7 Complete Rcv
3 Case Rcv To Lod   8 TL/LTL Return
4 Reverse Receipt   9 Return Arrival
5 Sorted Deposit    0 Next
```

The `Reverse Receipt` form was also captured:

```text
Reverse Receipt
ID:
Mst Rcpt:
Trl Num:
Car Cod:
Sup:
Rcp:
```

## Function key behavior discovered

Function-key escape sequences were accepted over Telnet.

Observed behavior:

- `F1` from `Yard Audit` cleared the error prompt.
- `F1` again returned to `Yard Menu`.
- `F1` from `Yard Menu` returned to the top-level main menu.
- Some function keys from main menu triggered logout confirmation:
  - `OK To Logout? (Y|N):`

Because the session was already dirty when testing continued, not every F-key mapping was isolated cleanly enough to document with certainty.

## Prompt behavior discovered

The host uses more than one input model:

- menu selections generally behave like line input
- confirmation prompts can behave like immediate single-character input

Observed examples:

- `Y` without `Enter` on an audit-related prompt advanced the prompt chain
- sending `Y` followed by `Enter` in the wrong state often caused `Invalid Response - Press Enter`

This distinction matters for automation. A client that always appends `Enter` will misdrive some screens.

Additional concrete findings from live QA navigation:

- `Enter` is required to clear `...Press Enter` error prompts.
- `OK To Logout? (Y|N):` expects a single-character response.
- Sending `N` without `Enter` from that prompt returns the session to the top-level menu.
- `F1` reliably backs out one level from transactional forms.
- On the WMS `Login` screen, `Enter` does not behave like a safe no-op. It can redraw the form and preserve or reset partially populated field state.
- On the `Login` screen, focus can land on `Password` instead of `User ID`.
- From that landing state, `Tab` moved focus into `User ID` during scripted testing.
- Entering text into the wrong field can be transformed by the form. A mistargeted password entry was uppercased and stripped of punctuation when it landed in `User ID`.

## Stable recovery path for `FREDDY`

By the end of testing, `FREDDY` was no longer usable as a fresh session, but it was still recoverable.

Observed recovery sequence:

1. Reconnect with terminal ID `FREDDY`
2. If the screen says `Invalid Response - Press Enter`, send `Enter`
3. If the next screen says `OK To Logout? (Y|N):`, send `N` without `Enter`
4. This returns to the top-level menu

If the session instead lands in a transactional form, repeated `F1` presses back it out one menu at a time.

Later state change:

- after repeated transactional testing, `FREDDY` eventually fell back to the plain WMS login screen
- at that point it no longer resumed the authenticated workflow
- when tested from that login screen, `ZRASHED / #Pr0j3ctW1nd` was also rejected with `Bad Login - Please Retry`
- later scripted testing showed that `FREDDY` could still reach the WMS `Login` screen if the client:
  - negotiated as `xterm`
  - submitted terminal ID with bare `CR`
  - avoided `CRLF` at terminal submission time
- however, scripted credential entry was still fragile because the login form focus could drift between `User ID` and `Password`
- repeated parallel or overlapping `FREDDY` sessions triggered:
  - `WARNING!`
  - `Your session has been disconnected by [ip:port] client connection.`
  - `CAUSE: other client connection is configured with same Terminal ID.`
- a later single-session login succeeded with:
  - terminal ID: `FREDDY`
  - user: `ZRASHED`
  - password: `orange1`
- the successful scripted login path was:
  1. connect with Telnet using `xterm`
  2. submit `FREDDY` with bare `CR`
  3. from the `Login` form, send `Tab`
  4. enter `ZRASHED`
  5. send `Tab`
  6. enter `orange1`
  7. send `Enter`
- after successful login the next confirmed screen was `Work Information`

## Clean `VMA` terminal path

Fresh `VMA` terminals are significantly easier to automate than `FREDDY`.

Confirmed examples:

- `VMA498`
- `VMA497`
- `VMA496`
- `VMA495`
- `VMA493`
- `VMA491`
- `VMA490`
- `VMA489`

Observed behavior:

- a fresh `VMA###` terminal lands on a clean `Login` form
- unlike `FREDDY`, focus started on `User ID`
- the working login sequence on clean `VMA` terminals was:
  1. submit terminal ID with bare `CR`
  2. enter `ZRASHED`
  3. send `Tab`
  4. enter `orange1`
  5. send `Enter`
- after successful login, `Work Information` appeared with:
  - `Warehouse ID = 3002`
  - `Loc` blank
  - `Vehicle Type` blank
  - `Work Area` blank
- the accepted clean-terminal work-information sequence was:
  - `Loc = DOOR03`
  - `Vehicle Type = TRD2`
  - `Work Area = WA_MAIN`

This is now the safest automation baseline. It avoids the resumed dirty menu/forms that make `FREDDY` hard to drive.

## Post-login work information screen

After successful `FREDDY` login with `ZRASHED / orange1`, the following screen was reached:

```text
Work Information

Warehouse ID: 3002
Loc:
Vehicle Type: TRD2
Work Area:
```

Observed behavior:

- this screen appears immediately after successful WMS login
- it is a required workflow gate before returning to transactional menus
- `Warehouse ID` was already populated as `3002`
- `Vehicle Type` was already populated as `TRD2`
- `Loc` and `Work Area` were blank during the successful scripted login
- field order is:
  - `Loc`
  - `Vehicle Type`
  - `Work Area`
- `Tab` from `Loc` advances into `Vehicle Type`, not directly to `Work Area`
- overwriting `Vehicle Type` causes `Invalid Vehicle Type - Press Enter`
- explicitly setting all three fields was required once the screen became dirty from prior failed attempts
- a successful accepted combination for the dock workflow path was:
  - `Loc = DOOR03`
  - `Vehicle Type = TRD2`
  - `Work Area = WA_MAIN`
- `WA_MAIN` was derived from `ZONMST`, where `WZ_MTDOCK -> WRKARE = WA_MAIN`
- after submitting `DOOR03 / TRD2 / WA_MAIN`, the screen advanced to `Confirm Workflow`

## Confirm workflow screen

After accepted `Work Information`, the next confirmed screen was:

```text
Confirm Workflow
ID: FREDDY

Perform Vehicle Safety Check

Confirm? - Press Enter
```

Observed behavior:

- this screen is a post-work-information gate before operational menus
- it appeared only after `Loc`, `Vehicle Type`, and `Work Area` all validated together
- further workflow discovery must stay on a single active `FREDDY` session to avoid duplicate-terminal disconnects

On clean `VMA` terminals, the full safety-check chain was longer than initially observed.

Confirmed prompt order after `Confirm? - Press Enter`:

1. `Has Equipment Been Sanitized?`
2. `Lights working?`
3. `Horn working?`
4. `Hydraulics working?`
5. `Harness attached?`
6. `Tires OK?`
7. `Rotating beacon working?`
8. `Safety shoes on?`
9. `Brakes working correctly?`
10. `All systems checked for leaking?`
11. `Overhead cage in-place?`
12. `Load backrest in-place?`
13. `Attachments secure?`
14. `Are the Battery Connection Free of Damag...`

Observed behavior:

- each of these prompts expects immediate `Y` or `N`
- after the final battery-connection prompt is answered, the terminal returns to:
  - `Loading Policies, Please Wait`
  - `Loading Menus, Please Wait`
  - top-level `Undirected Menu`
- sending extra `Y` keystrokes after the menu appears causes `Invalid Option - Please Retry`

## Confirmed menu paths

From the top-level menu:

- `3` -> `Receiving Menu`
- `4` -> `Shipping Menu`

Confirmed `Shipping Menu` options:

```text
1 Load Trailer      6 Close Trailer
2 Parcel Manifest   7 Unload Trailer
3 Trailer Display   8 LTL Loading
4 Pickup Trailer    9 Super Shipment
5 Complete Stop     0 Next
```

## Shipping workflow findings

### Trailer Display

`Shipping Menu -> 3 Trailer Display`

This form is keyed by trailer number and carrier code. It does not naturally start on `Yrd Loc`.

Confirmed lookup:

- `PC031608 / CPU`
  - `Yrd Loc: DOOR03`
  - `Trl Cod: SHIP`
  - `Trl Sts: LDG`

### Pickup Trailer

`Shipping Menu -> 4 Pickup Trailer`

This is currently the best candidate for removing trailers from dock doors.

Fields observed:

- `Trl Num`
- `Car Cod`
- trailing `Src Loc` confirmation field

Confirmed behavior:

- `PC031608 / CPU`
  - resolves to `Src Loc: DOOR03`
  - `Trl Sts: O/LING`
  - returns `Trailer is busy - Press Enter`
- `CPU0224 / CPU`
  - resolves to `Src Loc: DOOR04`
  - `Trl Sts: O/OSHP`
  - after entering `DOOR04` in the trailing `Src Loc` field, the form advances to:
    - `Ok to move trailer? (Y|N):`

Important inference:

- the trailing `Src Loc` field is not a destination
- it is a confirmation of the trailer's current location
- entering a different location such as `YI` returns `Trailer is not at Location - Press Enter`

Revised finding from clean `VMA` testing:

- on the clean `VMA` path, confirming `Ok to move trailer? (Y|N):` did not open an auth screen
- instead, it advanced directly into `Trailer Deposit`

### Trailer Deposit

`Shipping Menu -> Pickup Trailer -> confirm move`

Confirmed screen:

```text
Trailer Deposit
Trl Num:
Car Cod:
Src Loc:
Dep Loc:
Deposit To:
Rear Axle Min:
Rear Axle Max:
Front Axle Pos:
```

Observed behavior:

- for `CPU0224 / CPU / DOOR04`, after confirming move, the terminal opened `Trailer Deposit`
- entering `YI` in `Deposit To` triggered:
  - `OK To Begin Activity? (Y|N):`
- answering `Y` completed the move with:
  - `Trailer Deposited - Press Enter`
- after clearing the success prompt, the form returned to a blank `Trailer Pickup` screen ready for another move
- `Dep Loc` remained visually blank during this successful path; the operative field was `Deposit To`

This is the first confirmed end-to-end dock-clear path from RF workflow.

### Confirmed successful dock-clear action

On `2026-03-13`, the following move was executed successfully in QA:

- trailer: `CPU0224`
- carrier: `CPU`
- source door: `DOOR04`
- destination entered in RF: `YI`

Post-action database verification:

- `TRLR.YARD_LOC = YI`
- `TRLR.TRLR_STAT = CI`
- `YARD_LOC_TRLR_VIEW` no longer shows `CPU0224` at `DOOR04`
- `DOOR04` is now empty

On the same date, the following second move was also executed successfully:

- trailer: `BOZZ134`
- carrier: `CPU`
- source door: `DOOR05`
- destination entered in RF: `YI`

Post-action database verification:

- `YARD_LOC_TRLR_VIEW` no longer shows `BOZZ134` at `DOOR05`
- `DOOR05` is now empty
- `TRLR` shows an active `YI / CI` row for appointment `APP0473088`

At that point, the targeted `DOOR03-05` set had been reduced to:

- `DOOR03` -> `PC031608 / CPU / LDG`
- `DOOR04` -> empty
- `DOOR05` -> empty

### Close Trailer

`Shipping Menu -> 6 Close Trailer`

This form is not keyed by trailer number.

Observed fields:

- `Dock`
- `Carrier Move`
- `Car Cod`

Likely intended usage for `PC031608`:

- Dock: `DOOR03`
- Carrier Move: `1000352025`

Field navigation was not fully resolved during this pass.

Later clean-`VMA` testing resolved the field order and next gates:

- actual input order is:
  1. `Carrier Move`
  2. `Car Cod`
  3. `Dock`
- for `PC031608` the valid entry set was:
  - `Carrier Move = 1000352025`
  - `Car Cod = CPU`
  - `Dock = DOOR03`
- after valid submission the screen prompts:
  - `OK to split shipment and complete stop? (Y|N):`
- answering `Y` advances to:
  - `New Staging Lane`
  - field: `Staging Lane`
- `STAGE03` was accepted as a valid staging lane for the `DOOR03` path
- after `STAGE03`, the next screen is:
  - `Stop Seal`
  - `Stop: STP0412789`
  - `Seal:`

Observed blocker:

- submitting a blank seal advanced out of `Stop Seal`
- the workflow then failed with:
  - `No release rule or command is defined to complete this release. - Press Enter`
- post-action QA data showed no state change:
  - `PC031608` remained `YARD_LOC = DOOR03`
  - `TRLR_STAT = LDG`
  - stop completion flags remained `0`

Current interpretation:

- the `DOOR03` close path is correctly mapped through RF
- but QA appears to be missing the release-rule configuration needed to complete this split/close release
- this is a system/configuration blocker, not a terminal-navigation blocker
- entering a nonblank sample seal such as `12345` still produced the same release-rule failure
- so the blocker is not just a missing seal value

### Complete Stop

`Shipping Menu -> 5 Complete Stop`

This form is simpler than `Close Trailer`.

Confirmed screen:

```text
Complete Stop
Dock:
```

Observed behavior:

- entering `DOOR03` returned:
  - `Stop is not fully loaded. - Press Enter`

This confirms `Complete Stop` cannot clear `PC031608` directly while the trailer is still in `LDG`.

### Unload Trailer

`Shipping Menu -> 7 Unload Trailer`

This appears to be pallet-level, not a simple dock-clear action.

Observed fields:

- `ID`
- `Car Cod`
- `Trl Num`
- `Stop`
- `Dst`
- `ToID`

This is likely part of rework / pallet removal / mismatch correction, not the simplest dock-removal path.

Later clean-`VMA` capture confirmed the exact form layout:

```text
Unload Trailer
ID:
Car Cod:
Trl Num:
Stop:
Dst:
ToID:
```

Observed behavior:

- the form opens from `Shipping Menu -> 7`
- sending no values returns `Entry Required`
- this path was not required for the successful dock-clear moves

### Load Trailer

`Shipping Menu -> 1 Load Trailer`

Later clean-`VMA` capture confirmed this form:

```text
Load Trailer
Dock:
Carrier Move:
Car Cod:
Stop:
Trl Num:
```

Observed behavior:

- initial focus appears to land on `Carrier Move`
- entering data into the wrong field returns `Invalid Carrier Move - Press Enter`
- this path was not required for the successful dock-clear moves

## QA Data That Supports Navigation

These QA tables/views were verified and are useful for locating trailer/dock workflows:

- `TRLR`
- `TRLRACT`
- `APPT`
- `SHIP_STRUCT_VIEW`
- `YARD_LOC_TRLR_VIEW`
- `RCVTRK`

## Device / terminal tables

- `DEVMST`
- `RF_TERM_MST`
- `DEVICE_CONTEXT`

## Useful data points discovered

### Example docked / outbound trailers

From `YARD_LOC_TRLR_VIEW`:

- `DOOR03` -> trailer `PC031608`, carrier `CPU`, `TRLR_STAT = LDG`, `APPT_ID = APP0472953`, `CAR_MOVE_ID = 1000352025`
- `DOOR04` -> trailer `CPU0224`, carrier `CPU`, `TRLR_STAT = O`, `APPT_ID = APP0473259`, `CAR_MOVE_ID = 1000355025`
- `DOOR05` -> trailer `BOZZ134`, carrier `CPU`, `TRLR_STAT = O`, `APPT_ID = APP0473088`, `CAR_MOVE_ID = 1000351416`

### Example inbound / yard positions

From `TRLR` / `YARD_LOC_TRLR_VIEW`:

- `INB` contains checked-in inbound trailers like `PRIJ220343`, `CB5211144`, `PEND250938`
- `YI` contains many `CI` trailers
- `OS` contains many `CI` trailers
- `DSD` contains checked-in `CB` trailers

### Example shipment linkage

From `SHIP_STRUCT_VIEW`:

- `CPU0224` is linked to shipment `8000509491`, carrier move `1000355025`, shipment status `I`
- `PC031608` is linked to shipments `8000505170` and `8000505171`, carrier move `1000352025`, shipment status `I`

### Example trailer action history

From `TRLRACT`:

For `CPU0224`:

- `TNEW`
- `TCHKIN`
- `TCHG`

For `PC031608`:

- `TNEW`
- `TCHKIN`
- `TMOVE`
- `TSRVLDULD`
- `TCHG`

`TSRVLDULD` is the strongest indicator found that QA has a service load/unload-style transaction behind the terminal workflow.

## High-confidence interpretations

- `TCHKIN` corresponds to trailer check-in.
- `TMOVE` corresponds to moving a trailer to a yard/dock location.
- `TSRVLDULD` likely corresponds to a service load/unload trailer operation.
- `TRLR_STAT` values observed include at least:
  - `CI`
  - `O`
  - `OR`
  - `LDG`
  - `D`
  - `EX`
  - `R`

These interpretations are inferred from action timing and related location data, not from a formal code table.

## What I Was Able To Prove

- QA terminal connectivity is real and usable.
- QA database connectivity is real and usable.
- The terminal app can be driven programmatically without the PuTTY GUI if Telnet negotiation is implemented correctly.
- Menu navigation and function keys work over Telnet.
- Database data is sufficient to identify valid trailers, docks, appointments, and carrier moves to drive terminal lookups.
- There is evidence in `TRLRACT` that QA supports trailer load/unload transactions (`TSRVLDULD`).
- The `Pickup Trailer` flow is reachable and appears to be the direct action for moving a trailer off a dock.
- `Pickup Trailer` does not require a second auth screen on the clean `VMA` workflow
- the commit path is:
  - `Ok to move trailer? (Y|N):`
  - `Trailer Deposit`
  - `Deposit To`
  - `OK To Begin Activity? (Y|N):`
  - `Trailer Deposited - Press Enter`
- `Pickup Trailer -> Trailer Deposit` successfully cleared dock doors for trailers in states:
  - `O`
  - `OR`
  - `R`
- by the end of the latest QA pass, every occupied dock door except `DOOR03` had been cleared

## Expanded successful dock clears on 2026-03-13

Using the clean `VMA` workflow plus:

- `Shipping Menu -> Pickup Trailer`
- confirm current source door
- `Deposit To = YI`
- `Y` to begin activity

the following trailers were successfully moved off dock doors and deposited to `YI`:

- `CPU0224 / CPU / DOOR04`
- `BOZZ134 / CPU / DOOR05`
- `REET264 / REET / DOOR08`
- `WALM319133 / WALM / DOOR10`
- `REET250 / REET / DOOR11`
- `AD2253296 / CPU / DOOR12`
- `WALM322226 / WALM / DOOR14`
- `CB72603 / CB / DOOR15`
- `E-FAIL 4 / CPU / DOOR09`
- `E-FAIL 2 / CPU / DOOR13`
- `CPU919 / CPU / DOOR16`
- `PRIJ215785 / PRIJ / DOOR17`
- `PRIJ220588 / PRIJ / DOOR18`
- `PRIJ220342 / PRIJ / DOOR19`
- `E-FAIL7 / CPU / DOOR22`
- `E-FAIL2 / CPU / DOOR32`

Important confirmed behavior:

- this same RF pickup/deposit path worked for trailers in each of these states:
  - `O`
  - `OR`
  - `R`
- source-location validation is still enforced
- `Deposit To = YI` was accepted consistently for all of the above moves
- after each successful deposit, the terminal returned to a blank `Trailer Pickup` form that was ready for the next trailer

By the end of these moves, the occupied QA dock list had been reduced to a single door:

- `DOOR03 -> PC031608 / CPU / LDG / APPT_TYP = S`

## What I Was Not Able To Complete

I did not complete the final close/split/release sequence for `PC031608` at `DOOR03`.

Reason:

- the clean `VMA` workflow is now known and usable
- `Complete Stop` rejects `DOOR03` directly with:
  - `Stop is not fully loaded. - Press Enter`
- `Close Shipping Trailer` reaches the correct business gates:
  - split shipment confirmation
  - valid staging lane (`STAGE03`)
  - stop seal
- after those inputs, QA fails with:
  - `No release rule or command is defined to complete this release. - Press Enter`
- post-action QA data showed no change to:
  - `TRLR.YARD_LOC`
  - `TRLR.TRLR_STAT`
  - `STOP.STOP_CMPL_FLG`

This is now a QA workflow/configuration blocker, not a terminal automation blocker.

## Practical Limitations

- PuTTY GUI itself is not the requirement; the requirement is the terminal protocol plus a valid device ID and, for clean terminals, app credentials.
- A terminal ID can be valid but still not be safe to automate if it resumes mid-transaction.
- `plink` with redirected stdin is too weak for this app.
- A robust client must:
  - negotiate Telnet options
  - maintain terminal state
  - parse ANSI cursor movement
  - gate inputs on observed screen identity
  - understand function keys
  - detect and clear error prompts before continuing

## Recommended Next Step

The next targeted QA action should stay focused on the one remaining occupied door:

- `PC031608` at `DOOR03`

The mapped terminal path is:

1. `Shipping Menu -> Close Trailer`
2. `Carrier Move = 1000352025`
3. `Car Cod = CPU`
4. `Dock = DOOR03`
5. split shipment confirmation = `Y`
6. `Staging Lane = STAGE03`
7. `Stop Seal`

If QA administrators can add or repair the missing release rule or command for this release flow, this mapped RF path is ready to retry immediately.

## SQL snippets used conceptually

Representative QA queries used during discovery:

- terminal/device lookup in `DEVMST` / `RF_TERM_MST`
- trailer current state in `TRLR`
- yard/dock placement in `YARD_LOC_TRLR_VIEW`
- appointment lookup in `APPT`
- trailer action history in `TRLRACT`
- shipment/carrier move linkage in `SHIP_STRUCT_VIEW`
