# EMS / ASRS Analysis

Date captured: `2026-03-12`

## Verified Findings

- EMS web UI at `http://10.18.228.111/emsp` is reachable and returns `200 OK`.
- The login page identifies the stack as `WAFFLE version 2012.2.4`, which is a RedPrairie / JDA application family.
- The EMS browser client is configured to talk to MOCA at `http://10.18.228.111:4500/service`.
- The QA WMS endpoint `10.19.96.122:4550` is reachable and emits a non-text binary handshake, so it is not a plain command shell. Any automation against it must treat the protocol as telnet-style terminal negotiation or an application-specific session, not a raw line-oriented REPL.
- The sample reconciliation workbook from `2026-03-12 05:30 AM` contains at least these mismatch identifiers:
  - `currentLocationId`
  - `containerId`
  - `sku`
  - `ASRS_LOC_NUM`
  - `LODNUM`
  - `MAX(PRTNUM)`
- The EMS Control Center screenshot from `2026-03-12 06:58 AM` shows repeated host communication reconnects and a rejected bad-message case. That is consistent with the reported drift scenario where WMS and EMS diverge when a pallet misses a PLC scan or a host transaction is rejected during reconnect churn.

## Architectural Read

- EMS side:
  - App server: `10.18.228.111` / `emwmsapp`
  - Web app: `/emsp`
  - Middleware endpoint: `:4500/service`
  - DB server supplied by user: `10.18.228.55` / `bpvwmsdbp`
- WMS side:
  - Existing repo already uses Oracle for WMS reads.
  - QA operator path supplied by user: `10.19.96.122:4550`, saved in PuTTY as `TBG3002-QA`.

## Current Gaps

- EMS database type, port, database name, and credentials are still missing.
- EMS schema details are still missing:
  - inventory table/view
  - location table/view
  - move / transaction history
  - host messaging or bad-message log
- QA login credentials and repair command sequence are still missing.

## Implementation Added

- New CLI workflow: `ems-recon`
- Purpose:
  - parse the daily legacy `.xls` reconciliation file
  - classify mismatch types
  - mark likely persistent mismatches across passes
  - emit a readable report plus a JSON artifact
  - optionally probe the QA telnet endpoint and record reachability evidence

## Safe Fix Strategy

- Phase 1:
  - keep the tool read-only
  - explain each mismatch and generate a suggested repair template only
- Phase 2:
  - add WMS pre-check queries
  - add QA execution against the telnet flow only after login and command semantics are captured
  - require before/after validation plus an audit log for every issued repair

## Why PuTTY GUI Automation Is The Wrong First Move

- Keystroke automation against a desktop PuTTY window is fragile.
- It is hard to verify prompt state, command echo, and partial failures.
- A direct session client or controlled terminal wrapper is easier to audit and safer to gate with pre-checks.
