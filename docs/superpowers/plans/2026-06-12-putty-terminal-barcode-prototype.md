# Putty Terminal Barcode Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** Code complete and tests green; label generation/printing verified on hardware. Payload re-encoded to **Honeywell Velocity key-command tokens** (`{F7}…{tab}…{enter}`) after a live scan proved raw control bytes (`ESC`/`TAB`) are dropped by the Granit/Velocity path. Tokens use VT-220 key codes (`{return}` submit, not the 3270-only `{enter}`). Pending: enable Velocity's *process key commands from scanned data* setting on the VM1A, then re-scan and tune `{pause}` / `{tab}` if needed (see Results).

**Goal:** Add a quick prototype that prints two operator barcodes for the Putty/Telnet break workflow to the `3002_ZEB0` printer.

**Architecture:** Reuse the existing `barcode` command and ZPL generator. Add a small preset layer that maps human-intended actions to fixed barcode payloads and label text, then route printing through the current printer configuration for site `TBG3002`.

**Tech Stack:** Java 17, Picocli, JUnit 5, existing printer routing and ZPL helpers.

---

### Task 1: Inspect and extend the barcode command surface

**Files:**
- Modify: `cli/src/main/java/com/tbg/wms/cli/commands/BarcodeCommand.java`
- Modify: `cli/src/test/java/com/tbg/wms/cli/commands/BarcodeCommandTest.java`

- [x] **Step 1: Add a failing test for a preset-based barcode print path**
- [x] **Step 2: Run the targeted test and confirm the new option is not yet supported**
- [x] **Step 3: Add a minimal preset enum and label/data mapping**
- [x] **Step 4: Run the targeted test and confirm the preset path prints the expected ZPL**
- [x] **Step 5: Commit**

### Task 2: Verify printer routing for `3002_ZEB0`

**Files:**
- Modify: `config/TBG3002/printers.yaml`
- Optional test: `cli/src/test/java/com/tbg/wms/cli/commands/BarcodeCommandTest.java`

- [x] **Step 1: Confirm the printer entry exists and is enabled for site `TBG3002`**
- [x] **Step 2: Add a regression check that the command resolves `3002_ZEB0` from routing**
- [x] **Step 3: Run the relevant tests**
- [x] **Step 4: Commit**

### Task 3: Produce the two operator labels

**Files:**
- Modify: `cli/src/main/java/com/tbg/wms/cli/commands/BarcodeCommand.java`
- Modify: `core/src/main/java/com/tbg/wms/core/barcode/BarcodeZplBuilder.java` if needed

- [x] **Step 1: Wire `BREAK START` and `BREAK STOP` presets to concrete payloads**
- [x] **Step 2: Keep the output simple and readable for quick scanner testing**
- [x] **Step 3: Print the two labels to `3002_ZEB0`**
- [x] **Step 4: Capture the exact payloads/ZPL for the user**
- [x] **Step 5: Commit**

---

## Results

Three presets exposed via `barcode --preset <NAME>`:

| Preset | Caption | Scanned payload |
| --- | --- | --- |
| `BREAK_START` | `BREAK START` | `{F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}START{return}` |
| `BREAK_STOP` | `BREAK STOP` | `{F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}STOP{return}` |
| `BREAK_SHEET` | both | combined single label stacking START over STOP |

### Verified manual terminal sequence (operator, 2026-06-15)

The target hardware is a **Honeywell Thor VM1A** vehicle computer running the
**Honeywell Velocity** terminal emulator, scanned with a **Granit 1980i**. The
operator's confirmed manual break sequence is:

1. Top-level **Undirected Menu**
2. **F7** → opens the **Tools** menu
3. `0` → next page
4. `3` → **Activity Login**
5. first field → type `BREAK`
6. **Tab** to the next field
7. type `START` (or `STOP`)
8. **Enter**

### Why the first encoding failed, and the fix

The original payload encoded the raw host bytes `ESC[18~03BREAK<TAB>START<CR>`.
Live scanning showed the Granit/Velocity path silently drops control bytes: `ESC`
was dropped (so F7 never fired and `1`/`8` fell through into the Picking → Auto
Allocate menus), `TAB` was dropped (`BREAK`+`START` merged), and only `CR`
transmitted (the field submitted and errored).

The correct approach for the Honeywell/Velocity stack is **not** to send raw
control bytes. Velocity parses **brace-token key commands** embedded in scanned
data and replays them as real host key presses:

- `{F7}` (or `{hex:E041}`) → F7
- `{tab}` (or `{hex:0009}`) → Tab
- `{return}` / `{autoenter}` (or `{hex:000D}`) → field submit (the host profile is
  **VT-220**; `{enter}` is a 3270-only token and must not be used here)
- `{pause:500}` → wait 500 ms so each screen redraws before the next key
  (Velocity's default `{pause}` is 250 ms)

So the barcode now carries **only printable characters** (`{F7}…{tab}…{enter}`),
which the scanner transmits intact — no control-byte / function-key scanner mode
is required. The ZPL still uses hex-field encoding (`^FH`) purely to embed the
literal `{` `}` `:` characters reliably; decoded, the symbol contains exactly the
payload string above.

**Required device-side configuration:** Velocity must be set to *process key
commands from scanned data* (its scan handler / data-processing path) — otherwise
it types the literal text `{F7}…` into the field instead of executing the keys.

Target stack: **Velocity 2.1.6 (Android TE)**, host profile **VT-220**.

**Likely tuning knobs after a live scan:** the `{pause:500}` duration (raise if a
screen is slow to redraw), and `{tab}` vs `{down}` arrow to move between the
activity-name and START/STOP fields. The menu keys `0`/`3` are sent without an
Enter because the host accepts single-key menu selections (confirmed by the
original misfire, where `1`/`8` were each taken immediately).

### Usage

```powershell
# Dry-run the combined operator sheet (writes ZPL under ./barcodes, no print)
barcode --preset BREAK_SHEET --dry-run

# Print the combined sheet to the QA label printer
barcode --preset BREAK_SHEET --printer 3002_ZEB0

# Individual labels
barcode --preset BREAK_START --printer 3002_ZEB0
barcode --preset BREAK_STOP  --printer 3002_ZEB0
```

Generated ZPL artifacts land in `./barcodes/` (gitignored).
