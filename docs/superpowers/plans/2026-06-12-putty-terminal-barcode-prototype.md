# Putty Terminal Barcode Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** Code complete and tests green; label-generation/printing verified on hardware. End-to-end scanning is **blocked on scanner configuration** — the keyboard-wedge scanner currently drops `ESC`/`TAB`, so F7 and the field Tab don't fire. The encoded sequence itself is confirmed correct against the operator's manual steps (see Results → Open blocker).

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

| Preset | Caption | Scanned payload (decoded) |
| --- | --- | --- |
| `BREAK_START` | `BREAK START` | `ESC [ 1 8 ~ 0 3 B R E A K <TAB> START <CR>` |
| `BREAK_STOP` | `BREAK STOP` | `ESC [ 1 8 ~ 0 3 B R E A K <TAB> STOP <CR>` |
| `BREAK_SHEET` | both | combined single label stacking START over STOP |

Payloads are emitted as CODE128 using ZPL hex-field encoding (`^FH`), so the
control bytes are present in the barcode symbol (whether the *scanner* re-emits
them as keystrokes is a separate, scanner-config concern — see the open blocker
below):

```
^FH^FD_1B_5B_31_38_7E_30_33_42_52_45_41_4B_09_53_54_41_52_54_0D^FS   (BREAK START)
^FH^FD_1B_5B_31_38_7E_30_33_42_52_45_41_4B_09_53_54_4F_50_0D^FS       (BREAK STOP)
```

Decode: `_1B`=ESC, `_5B`=`[`, `18`, `_7E`=`~`, `03`, `BREAK`, `_09`=TAB, `START`/`STOP`, `_0D`=CR.

### Verified manual terminal sequence (operator, 2026-06-15)

The encoded sequence matches the real manual steps confirmed by the operator:

1. Top-level **Undirected Menu**
2. **F7** (function key) → opens the **Tools** menu screen
3. `0` → next page
4. `3` → **Activity Login**
5. first field → type `BREAK`
6. **Tab** (or arrow) to the next field
7. type `START` (or `STOP`)
8. **Enter**

`F7` over the xterm/VT220 host is `ESC [ 1 8 ~`, so the prototype payload
(`ESC[18~` + `0` + `3` + `BREAK` + TAB + `START`/`STOP` + CR) is logically correct.

### Open blocker: scanner drops control bytes

Live scan testing showed the symbol-motion (keyboard-wedge) scanner transmits
only printable ASCII plus Enter:

- `ESC` is dropped → **F7 never fires**; the bare `1` `8` fall through as menu
  selections (Picking Menu, then Auto Allocate Load).
- `TAB` is dropped → `BREAK` and `START` merge into one field (`BREAKSTART`).
- `CR` transmits → the field submits and errors with `order doesn't exist`.

The label content is correct; the fix is **scanner configuration** — the scanner
must be put into a mode that transmits function keys / control characters (commonly
"Function Key Mapping" / "Control Character Output"). F7 is unavoidable, so a
plain-printable-only barcode cannot reach the Tools menu. Next step is to identify
the scanner make/model and enable function-key transmission, then re-test the
existing labels unchanged.

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
