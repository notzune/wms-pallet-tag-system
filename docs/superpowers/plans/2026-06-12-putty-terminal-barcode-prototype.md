# Putty Terminal Barcode Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** Code complete and tests green; label generation/printing verified on hardware. The final labels use short **Code 128 trigger payloads** (`BRKSTART` / `BRKSTOP`) because Velocity types key-command tokens literally when they arrive as raw scan data. Pending: configure the two Velocity scan handlers on the VM1A, then re-scan and tune `{pause}` / `{tab}` if needed (see Results).

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

Target stack: **Honeywell Thor VM1A** vehicle computer running **Velocity 2.1.6
(Android TE)**, host profile **VT-220**, scanned with a **Granit 1980i**.

Three presets exposed via `barcode --preset <NAME>`:

| Preset | Caption | Barcode payload (Code 128) |
| --- | --- | --- |
| `BREAK_START` | `BREAK START` | `BRKSTART` |
| `BREAK_STOP` | `BREAK STOP` | `BRKSTOP` |
| `BREAK_SHEET` | both | combined label stacking the two triggers |

### Design: short trigger barcode + Velocity scan-handler macro

The barcode does **not** contain the key sequence. It carries a short, distinctive
**trigger** (`BRKSTART` / `BRKSTOP`). On the device, a **Velocity scan handler**
matches the trigger and plays the stored key macro:

```
BRKSTART -> {F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}START{return}
BRKSTOP  -> {F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}STOP{return}
```

The macro maps the operator's confirmed manual sequence: **F7** → Tools menu, `0`
next page, `3` Activity Login, type `BREAK`, **Tab**, type `START`/`STOP`,
**Return** to submit. VT-220 key codes: `{F7}`=`E041`, `{tab}`=`0009`,
`{return}`=`000D` (`{return}`, not the 3270-only `{enter}`). `{pause:500}` lets
each screen redraw (Velocity's default `{pause}` is 250 ms).

### Why it took three tries to land here

1. **Raw control bytes** (`ESC[18~03BREAK<TAB>START<CR>`, Code 128): the
   Granit/Velocity path silently drops control bytes — `ESC` gone (F7 never fired;
   `1`/`8` fell through into Picking → Auto Allocate), `TAB` gone (`BREAKSTART`
   merged), only `CR` transmitted.
2. **Key-command tokens in the barcode** (`{F7}…{return}`): two problems —
   (a) as 1D Code 128 the ~62-char string is ≈ 2211 dots (~10.9″), overflowing the
   4″ label so it would not even scan; switching to **Data Matrix** (2D, which the
   Granit reads) fixed decoding, but (b) Velocity types those tokens **literally**
   from a raw scan (`7` selected Yard Menu, `}` errored) — it only honors key
   commands inside **scripts/macros/buttons/scan handlers**, not raw scanned data.
3. **Trigger + scan-handler macro** (current): the barcode is a short Code 128
   trigger that transmits cleanly; the macro lives where Velocity *does* honor key
   tokens. This is the Ivanti/Zebra "scan a barcode to send a key" pattern.

The core builder retains **Data Matrix** (`^BX`) support from step 2 for any future
long-payload need, but the break presets are short Code 128 again.

### Required device-side configuration (Velocity)

Create two scan handlers on the VM1A host profile:

- match `BRKSTART` → play macro `{F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}START{return}`
- match `BRKSTOP`  → play macro `{F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}STOP{return}`

**Tuning knobs after a live scan:** the `{pause:500}` duration (raise if a screen
is slow to redraw) and `{tab}` vs `{down}` to move between the activity-name and
START/STOP fields. The menu keys `0`/`3` are sent without an Enter because the host
accepts single-key menu selections.

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
