# Rail Label Generation Improvements Design

Date: 2026-05-29

## Purpose

Improve rail label generation so operators can:

- Render more readable 4 inch by 2 inch rail labels.
- Enter multiple train codes at once.
- Preview all generated railcar rows before output.
- Explicitly choose which rows are included in the generated PDF and print job.

The generated PDF remains one combined document. Cards are ordered by train-code input order, then by the existing deterministic railcar/card order for each train.

## Existing Context

The rail workflow is already split across:

- `core/src/main/java/com/tbg/wms/core/rail`: WMS rail data aggregation, card planning, PDF rendering, and print support.
- `cli/src/main/java/com/tbg/wms/cli/commands/rail`: command-line rail print entry points.
- `gui/src/main/java/com/tbg/wms/cli/gui/rail`: Swing rail-label dialog, preview table, diagnostics, PDF generation, and print actions.

The current renderer already emits letter-sized PDFs with a 2 column by 5 row grid, but typography is too small and the sheet measurements are implicit in renderer constants. The new work should make physical-media assumptions explicit and isolated.

## Physical Media

Future maintainers need to see the physical label-stock math in code documentation, not only in external notes. The media specification belongs on the class that owns sheet geometry, proposed as `RailLabelSheetLayout`.

Sheet:

- Page width: 8.5 inches
- Page height: 11.0 inches
- Orientation: portrait
- Columns: 2
- Rows: 5
- Labels per sheet: 10

Label:

- Width: 4.0 inches
- Height: 2.0 inches

Margins and gap:

- Left margin: 5/32 inch, or 0.15625 inch
- Right margin: 5/32 inch, or 0.15625 inch
- Top margin: 0.5 inch
- Bottom margin: 0.5 inch
- Center gap: 3/16 inch, or 0.1875 inch

Validation:

```text
(4.0 * 2) + 0.1875 + (0.15625 * 2) = 8.5
(2.0 * 5) + (0.5 * 2) = 11.0
```

PDFBox uses points, not pixels. The implementation should store values in inches, then convert to points at render time:

```text
points = inches * 72
```

For comparison with 300 DPI print previews:

```text
pixels = inches * 300

Page:  8.5 * 300 = 2550 px, 11 * 300 = 3300 px
Label: 4.0 * 300 = 1200 px, 2 * 300 = 600 px
Left/right margin: 0.15625 * 300 = 46.875 px, rounded 47 px
Center gap: 0.1875 * 300 = 56.25 px, rounded 56 px
Top/bottom margin: 0.5 * 300 = 150 px
```

The 300 DPI reference coordinates are:

```text
Row 1 Col 1: x=47,   y=150
Row 1 Col 2: x=1303, y=150
Row 2 Col 1: x=47,   y=750
Row 2 Col 2: x=1303, y=750
Row 3 Col 1: x=47,   y=1350
Row 3 Col 2: x=1303, y=1350
Row 4 Col 1: x=47,   y=1950
Row 4 Col 2: x=1303, y=1950
Row 5 Col 1: x=47,   y=2550
Row 5 Col 2: x=1303, y=2550
```

## Typography

The renderer should use centralized typography constants, expressed in points:

| Element | Default | Style |
| --- | ---: | --- |
| Sequence number | 18 pt | Bold, italic, underlined |
| Main rail car/product code | 30 pt | Bold, italic, underlined, right-aligned |
| Route/header line | 8 pt | Italic |
| Door/lane code | 9 pt | Bold |
| Item/quantity rows | 8 pt | Regular or medium |
| Primary CAN/DOM count | 30 pt | Bold, centered |
| Secondary CAN/DOM count | 22 pt | Bold, italic, underlined, centered |
| Footer fields | 8 pt | Bold, underlined labels with writable underline |

Visual priority:

1. Rail car/product code and CAN/DOM count.
2. Sequence number.
3. Item quantities and routing information.
4. PASS/FUEL/BH handwritten-entry fields.

Rendering placement:

- Sequence number: top-left, underlined, bold, italic.
- Rail car/product code: top-right, right-aligned, underlined, bold, italic.
- Route/load header: upper-left supporting text.
- Door/lane code: below route/header when available.
- Item rows: left body area, capped at 5 visible rows.
- Primary destination count: centered in the open body area.
- Secondary destination count: centered directly below primary count when applicable.
- Footer: bottom edge as `PASS:_____   FUEL:_____   BH:_____`, with about 0.5 inch of writable underline per field.

If more than 5 item rows exist, reduce the item font proportionally but not below 6 pt. If the text still cannot fit, truncate and show continuation text.

## Multiple Train Input

Add a small parser with one responsibility: turning operator text into normalized train IDs.

Proposed class: `RailTrainInputParser`.

Rules:

- Accept comma, whitespace, colon, slash, semicolon, or any combination as separators.
- Trim each token.
- Normalize to uppercase.
- Drop blank tokens.
- Deduplicate while preserving first occurrence order.
- Reject input that produces no train IDs.

Examples:

```text
JC04152026, JC05012026
JC04152026 JC05012026
JC04152026/JC05012026
JC04152026: JC05012026; JC06012026
```

All examples should produce ordered train IDs.

## Combined Workflow

The current core workflow prepares one train. Add a multi-train path without weakening the existing single-train API.

Proposed behavior:

- Existing `prepare(String trainId)` remains the single-train primitive.
- New `prepareAll(List<String> trainIds)` or equivalent calls the single-train primitive per train.
- Combined result stores:
  - Ordered train IDs.
  - Per-train result details.
  - Flattened card list in print order.
  - Combined diagnostics.
  - Per-train failures with enough context for GUI/CLI messaging.

Failure policy should be strict by default: if any requested train has no WMS rows or cannot prepare, the preview should fail before generating a PDF. This avoids silently skipping a train an operator typed.

## Row Selection

Operators must control which preview rows become labels.

Recommended GUI behavior:

- Preview table includes a `PRINT` checkbox column.
- All generated rows default to checked after preview load.
- Normal table multi-selection should support Ctrl-click and Shift-click through `JTable.MULTIPLE_INTERVAL_SELECTION`.
- Add compact controls:
  - `Select All`
  - `Clear All`
  - `Invert`
- Toggling the `PRINT` checkbox on one row affects that row.
- Pressing Space or using a toolbar action should toggle the checkbox state for all highlighted rows when practical.
- Generate PDF and Print actions use only checked rows.
- If zero rows are checked, generation is blocked with a clear message.

Selection state should not live only in highlighted Swing rows. Highlighted rows are transient UI state; the printable choice should be explicit in the table model.

Proposed support type: `RailPrintableCardSelection`, responsible for filtering cards by checked indexes or stable row IDs.

## Output

Multiple train codes generate one PDF:

- File name can include a compact combined identifier, such as `rail-cards-JC04152026-plus-2-YYYYMMDD-HHmmss.pdf`, to avoid extremely long file names.
- Diagnostics should list all train IDs and the number of cards selected versus total.
- Print uses the same selected-card PDF.

CLI support can follow after GUI support. For CLI, a minimal first pass is:

- Allow `--train` to accept the same multi-code delimiter syntax.
- Generate one combined PDF.
- Keep all rows included because the CLI has no interactive table.

Optional future CLI selection can use explicit sequence filters, but it is not required for this change.

## SRP And SOLID Boundaries

The implementation should preserve strict single responsibility:

- `RailTrainInputParser`: parse train-code text only.
- `RailLabelSheetLayout`: own physical media, inch values, point conversion, and slot bounds only.
- `RailLabelTypography`: own font size/style constants only, if this improves clarity.
- `RailWorkflowService`: orchestrate rail preparation only; no rendering, no Swing, no parser-specific UI behavior.
- `RailCardRenderer`: render provided cards to a provided PDF path using layout and typography collaborators only.
- `RailPrintableCardSelection`: filter selected card rows only.
- `RailLabelsDialog`: assemble Swing components and route user actions only.
- Dialog support classes: continue holding validation, state transitions, and action outcome formatting instead of growing the dialog.

SOLID implications:

- Single Responsibility: each class above has one reason to change.
- Open/Closed: sheet geometry and typography can evolve behind renderer collaborators without changing workflow or UI code.
- Liskov Substitution: keep collaborators simple and behaviorally narrow; avoid subclass-based variation unless a real extension point exists.
- Interface Segregation: do not introduce broad service interfaces for one implementation; use small package-private collaborators where tests need seams.
- Dependency Inversion: high-level workflow should not depend on Swing or PDFBox details; rendering-specific classes may depend on PDFBox.

## Testing

Core tests:

- Parser accepts all required delimiters and combinations.
- Parser normalizes uppercase, trims blanks, dedupes, and rejects empty input.
- Multi-train workflow preserves input train order and existing card order within each train.
- Sheet layout returns expected point bounds for 2 by 5 labels.
- 300 DPI reference math is represented in documentation/tests as needed, while renderer uses point math.
- Renderer creates one page per 10 selected cards.
- Renderer handles 0 cards by creating a blank letter page, preserving current behavior unless changed intentionally.

GUI support tests:

- Preview table defaults all rows to printable.
- Select all, clear all, and invert update the printable checkbox state.
- Generation request blocks when no rows are printable.
- Generation passes only checked cards to the GUI workflow service.

Regression tests:

- Existing single-train CLI behavior still works.
- Alignment template still uses the configured physical media.
- Printer routing behavior is unchanged.

## Implementation Notes

This change should avoid broad refactoring. The highest-value path is:

1. Add parser and tests.
2. Add sheet layout class with detailed physical-media Javadoc and tests.
3. Refactor renderer to use layout and new typography constants.
4. Add multi-train preparation in core and GUI bridge.
5. Add explicit printable checkbox state and multi-select controls to the GUI.
6. Route generation through selected cards only.
7. Add CLI multi-train input support if scope allows in the same implementation pass.

