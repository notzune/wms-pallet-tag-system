# Rail Label Generation Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved rail label improvements from issue `#42`: larger readable labels, documented physical-media layout, multi-train input, one combined PDF, and explicit row print selection.

**Architecture:** Keep parsing, physical layout, rendering, workflow orchestration, printable-row filtering, and Swing UI state in separate focused classes. The existing single-train workflow remains the primitive; new multi-train behavior composes it and preserves deterministic order. The GUI preview owns operator row inclusion state explicitly through table model data, while rendering receives only the selected cards.

**Tech Stack:** Java 17, Maven multi-module project, JUnit 5, Swing, PDFBox 2.0.31, Picocli, GitHub issue `#42`.

---

## Process Rules

- Work from issue `#42`.
- Use conventional commits with logical scope, for example `feat(rail): parse multiple train codes`.
- Commit after each self-contained task.
- Keep versioning semantic-version aware. This is user-facing feature work and should be documented as a candidate minor release unless the project release process decides otherwise.
- Do not create the final PR until all tests pass and the user approves the shippable result.
- Avoid unrelated refactors and ignore unrelated dirty worktree files.

## File Structure

Create:

- `core/src/main/java/com/tbg/wms/core/rail/RailTrainInputParser.java`  
  Parses operator train-code text into normalized ordered train IDs only.

- `core/src/test/java/com/tbg/wms/core/rail/RailTrainInputParserTest.java`  
  Covers delimiter, normalization, dedupe, and empty-input behavior.

- `core/src/main/java/com/tbg/wms/core/rail/RailLabelSheetLayout.java`  
  Owns physical sheet dimensions, inch-to-point conversion, 2x5 slot bounds, and Javadoc with all approved measurements/calculations.

- `core/src/test/java/com/tbg/wms/core/rail/RailLabelSheetLayoutTest.java`  
  Verifies sheet math and slot positions in points.

- `core/src/main/java/com/tbg/wms/core/rail/RailLabelTypography.java`  
  Optional focused constants holder for font sizes/styles if it keeps `RailCardRenderer` small.

- `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailPrintableCardTableModel.java`  
  Owns printable checkbox state and row-to-card mapping for the preview table.

- `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailPrintableCardTableModelTest.java`  
  Covers default checked state, select all, clear all, invert, and selected-card extraction.

Modify:

- `core/src/main/java/com/tbg/wms/core/rail/RailWorkflowService.java`  
  Add multi-train composition while keeping existing `prepare(String)` behavior.

- `core/src/main/java/com/tbg/wms/core/rail/RailCardRenderer.java`  
  Use `RailLabelSheetLayout` and larger typography; render only provided cards.

- `core/src/test/java/com/tbg/wms/core/rail/RailWorkflowServiceTest.java`  
  Add multi-train order and strict failure tests.

- `core/src/test/java/com/tbg/wms/core/rail/RailCardRendererTest.java`  
  Add pagination and layout smoke coverage.

- `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailDialogExecutionSupport.java`  
  Parse multi-train preview input and validate selected-card generation.

- `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailDialogActionSupport.java`  
  Report selected/total counts in preview and generation outcomes as needed.

- `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailWorkflowService.java`  
  Prepare multiple trains and generate PDFs from selected cards.

- `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailLabelsDialog.java`  
  Add checkbox table model, multi-select controls, and selected-card generation routing.

- `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailDialogExecutionSupportTest.java`  
  Add parse and zero-selected validation coverage.

- `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailDialogActionSupportTest.java`  
  Update preview/generation outcome expectations.

- `cli/src/main/java/com/tbg/wms/cli/commands/rail/RailPrintCommand.java`  
  Accept multi-train input and generate one combined PDF.

- `cli/src/main/java/com/tbg/wms/cli/commands/rail/RailPrintCliSupport.java`  
  Validate/format multi-train preview text.

- `cli/src/test/java/com/tbg/wms/cli/commands/rail/RailPrintCliSupportTest.java`  
  Add multi-train validation/preview coverage.

- `CHANGELOG.md`  
  Add an unreleased conventional entry if the project already uses such a section. If not, defer release-note editing until final versioning is decided.

---

### Task 1: Create Implementation Branch And Link Issue

**Files:**
- No source changes expected.

- [ ] **Step 1: Create a dedicated branch**

Run:

```powershell
git switch -c feat/rail-label-generation-improvements
```

Expected: branch created from the current HEAD.

- [ ] **Step 2: Confirm issue link**

Run:

```powershell
git status --short
```

Expected: existing unrelated dirty files may remain, but no rail implementation files are changed yet.

- [ ] **Step 3: Commit status**

No commit for branch creation. Subsequent commits should reference `#42` in the body when useful.

### Task 2: Train Input Parser

**Files:**
- Create: `core/src/main/java/com/tbg/wms/core/rail/RailTrainInputParser.java`
- Create: `core/src/test/java/com/tbg/wms/core/rail/RailTrainInputParserTest.java`

- [ ] **Step 1: Write failing parser tests**

Test cases:

```java
@Test
void parse_shouldAcceptConfiguredDelimitersAndPreserveOrder() {
    RailTrainInputParser parser = new RailTrainInputParser();

    assertEquals(
            List.of("JC04152026", "JC05012026", "JC06012026"),
            parser.parse("jc04152026, JC05012026 / jc06012026")
    );
}

@Test
void parse_shouldDeduplicateAndRejectEmptyInput() {
    RailTrainInputParser parser = new RailTrainInputParser();

    assertEquals(List.of("JC04152026"), parser.parse("JC04152026; jc04152026"));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.parse(" , / ; "));
    assertEquals("At least one train ID is required.", ex.getMessage());
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailTrainInputParserTest test
```

Expected: FAIL because the parser class does not exist.

- [ ] **Step 3: Implement parser**

Implementation rules:

```java
public final class RailTrainInputParser {
    private static final Pattern DELIMITERS = Pattern.compile("[,\\s:/;]+");

    public List<String> parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("At least one train ID is required.");
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String token : DELIMITERS.split(input.trim())) {
            String normalized = token.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one train ID is required.");
        }
        return List.copyOf(values);
    }
}
```

- [ ] **Step 4: Run parser tests**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailTrainInputParserTest test
```

Expected: PASS.

- [ ] **Step 5: Commit parser**

```powershell
git add core/src/main/java/com/tbg/wms/core/rail/RailTrainInputParser.java core/src/test/java/com/tbg/wms/core/rail/RailTrainInputParserTest.java
git commit -m "feat(rail): parse multiple train codes"
```

### Task 3: Physical Sheet Layout Class

**Files:**
- Create: `core/src/main/java/com/tbg/wms/core/rail/RailLabelSheetLayout.java`
- Create: `core/src/test/java/com/tbg/wms/core/rail/RailLabelSheetLayoutTest.java`

- [ ] **Step 1: Write failing layout tests**

Test cases:

```java
@Test
void defaultLayout_shouldUseApprovedPhysicalMedia() {
    RailLabelSheetLayout layout = RailLabelSheetLayout.defaultLayout();

    assertEquals(612.0f, layout.pageWidthPoints(), 0.001f);
    assertEquals(792.0f, layout.pageHeightPoints(), 0.001f);
    assertEquals(288.0f, layout.labelWidthPoints(), 0.001f);
    assertEquals(144.0f, layout.labelHeightPoints(), 0.001f);
    assertEquals(10, layout.slotsPerPage());
}

@Test
void slotBounds_shouldMatchTwoByFiveSheet() {
    RailLabelSheetLayout layout = RailLabelSheetLayout.defaultLayout();

    RailLabelSheetLayout.LabelSlot first = layout.slot(0);
    RailLabelSheetLayout.LabelSlot second = layout.slot(1);
    RailLabelSheetLayout.LabelSlot rowTwo = layout.slot(2);

    assertEquals(11.25f, first.left(), 0.001f);
    assertEquals(612.0f, first.bottom(), 0.001f);
    assertEquals(312.75f, second.left(), 0.001f);
    assertEquals(612.0f, second.bottom(), 0.001f);
    assertEquals(11.25f, rowTwo.left(), 0.001f);
    assertEquals(468.0f, rowTwo.bottom(), 0.001f);
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailLabelSheetLayoutTest test
```

Expected: FAIL because class does not exist.

- [ ] **Step 3: Implement layout with detailed Javadoc**

Required Javadoc content:

- 8.5 by 11 inch portrait page.
- 4 by 2 inch labels.
- Left/right margins 5/32 inch.
- Top/bottom margins 0.5 inch.
- Center gap 3/16 inch.
- Validation formulas:
  - `(4.0 * 2) + 0.1875 + (0.15625 * 2) = 8.5`
  - `(2.0 * 5) + (0.5 * 2) = 11.0`
- PDFBox point conversion: `points = inches * 72`.
- 300 DPI reference calculations for maintainers.

Keep the class focused on layout only. No fonts, no card data, no PDF rendering.

- [ ] **Step 4: Run layout tests**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailLabelSheetLayoutTest test
```

Expected: PASS.

- [ ] **Step 5: Commit layout**

```powershell
git add core/src/main/java/com/tbg/wms/core/rail/RailLabelSheetLayout.java core/src/test/java/com/tbg/wms/core/rail/RailLabelSheetLayoutTest.java
git commit -m "feat(rail): document label sheet layout"
```

### Task 4: Renderer Typography And Layout Refactor

**Files:**
- Modify: `core/src/main/java/com/tbg/wms/core/rail/RailCardRenderer.java`
- Optional create: `core/src/main/java/com/tbg/wms/core/rail/RailLabelTypography.java`
- Modify: `core/src/test/java/com/tbg/wms/core/rail/RailCardRendererTest.java`

- [ ] **Step 1: Add failing renderer tests**

Add tests for:

- 11 cards create 2 PDF pages.
- Renderer can render a representative card with large CAN/DOM text without throwing.
- Alignment template still creates one page.

Example:

```java
@Test
void renderPdf_shouldPaginateAfterTenCards() throws Exception {
    Path output = Files.createTempFile("rail-cards-pagination", ".pdf");
    new RailCardRenderer().renderPdf(cards(11), output);

    try (PDDocument doc = PDDocument.load(output.toFile())) {
        assertEquals(2, doc.getNumberOfPages());
    }
}
```

- [ ] **Step 2: Run renderer tests**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailCardRendererTest test
```

Expected: existing tests pass; new test may pass already for pagination, but layout/typography assertions remain pending through implementation review.

- [ ] **Step 3: Refactor renderer to depend on layout**

Required behavior:

- Default constructor uses `RailLabelSheetLayout.defaultLayout()`.
- Calibration constructor keeps existing config support for center gap and offsets.
- Slot math comes from layout, not duplicated renderer constants.
- Border is 1 to 2 px equivalent in points and uses rounded corners if PDFBox primitive support is practical; otherwise use rectangular border and document limitation.

- [ ] **Step 4: Add large typography**

Use PDFBox Type 1 font equivalents:

- Sequence: `HELVETICA_BOLD_OBLIQUE`, 18 pt, underline drawn manually.
- Vehicle/product code: `HELVETICA_BOLD_OBLIQUE`, 30 pt, right-aligned, underline drawn manually.
- Route/load header: `HELVETICA_OBLIQUE`, 8 pt.
- Door/lane: `HELVETICA_BOLD`, 9 pt when available.
- Item rows: `HELVETICA`, 8 pt, shrink no lower than 6 pt for more rows.
- Primary count: `HELVETICA_BOLD`, 30 pt, centered.
- Secondary count: `HELVETICA_BOLD_OBLIQUE`, 22 pt, centered and underlined when applicable.
- Footer labels: `HELVETICA_BOLD`, 8 pt, bottom-aligned with writable underline.

- [ ] **Step 5: Run renderer tests**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailCardRendererTest,RailLabelSheetLayoutTest test
```

Expected: PASS.

- [ ] **Step 6: Commit renderer**

```powershell
git add core/src/main/java/com/tbg/wms/core/rail/RailCardRenderer.java core/src/main/java/com/tbg/wms/core/rail/RailLabelTypography.java core/src/test/java/com/tbg/wms/core/rail/RailCardRendererTest.java
git commit -m "feat(rail): enlarge rail label typography"
```

If `RailLabelTypography.java` is not created, omit it from `git add`.

### Task 5: Core Multi-Train Workflow

**Files:**
- Modify: `core/src/main/java/com/tbg/wms/core/rail/RailWorkflowService.java`
- Modify: `core/src/test/java/com/tbg/wms/core/rail/RailWorkflowServiceTest.java`

- [ ] **Step 1: Write failing multi-train workflow tests**

Add tests:

```java
@Test
void prepareAll_shouldPreserveInputOrderThenCardOrder() {
    RailWorkflowService service = new RailWorkflowService(fakeRepositoryForTwoTrains());

    RailWorkflowService.RailWorkflowBatchResult result =
            service.prepareAll(List.of("TRAINB", "TRAINA"));

    assertEquals(List.of("TRAINB", "TRAINA"), result.getTrainIds());
    assertEquals("TRAINB", result.getCards().get(0).getTrainId());
    assertEquals("TRAINA", result.getCards().get(1).getTrainId());
}

@Test
void prepareAll_shouldFailWhenAnyTrainIsMissing() {
    RailWorkflowService service = new RailWorkflowService(repositoryMissingSecondTrain());

    IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.prepareAll(List.of("TRAIN1", "MISSING"))
    );

    assertTrue(ex.getMessage().contains("MISSING"));
}
```

- [ ] **Step 2: Run workflow tests**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailWorkflowServiceTest test
```

Expected: FAIL because batch result/API does not exist.

- [ ] **Step 3: Implement batch result**

Add a nested immutable `RailWorkflowBatchResult` with:

- `List<String> getTrainIds()`
- `List<RailWorkflowResult> getResults()`
- `List<RailStopRecord> getRawRows()`
- `List<RailCarAggregate> getAggregates()`
- `List<RailCarCard> getCards()`
- Combined footprint/missing diagnostics as needed.

Do not change existing `RailWorkflowResult` constructor behavior except where needed for testability.

- [ ] **Step 4: Implement `prepareAll(List<String>)`**

Rules:

- Reject null/empty list.
- Normalize through existing `prepare(String)` path or shared normalization.
- Call `prepare` per train in input order.
- Let strict failures identify the train code.
- Flatten cards in input order.

- [ ] **Step 5: Run workflow tests**

Run:

```powershell
.\mvnw.cmd -pl core -Dtest=RailWorkflowServiceTest,RailTrainInputParserTest test
```

Expected: PASS.

- [ ] **Step 6: Commit workflow**

```powershell
git add core/src/main/java/com/tbg/wms/core/rail/RailWorkflowService.java core/src/test/java/com/tbg/wms/core/rail/RailWorkflowServiceTest.java
git commit -m "feat(rail): prepare multiple trains"
```

### Task 6: GUI Printable Table Model

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailPrintableCardTableModel.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailPrintableCardTableModelTest.java`

- [ ] **Step 1: Write failing table model tests**

Test cases:

```java
@Test
void setCards_shouldDefaultAllRowsToPrintable() {
    RailPrintableCardTableModel model = new RailPrintableCardTableModel();

    model.setCards(List.of(card("1"), card("2")));

    assertEquals(2, model.getRowCount());
    assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
    assertEquals(2, model.selectedCards().size());
}

@Test
void selectionActions_shouldUpdatePrintableRows() {
    RailPrintableCardTableModel model = new RailPrintableCardTableModel();
    model.setCards(List.of(card("1"), card("2"), card("3")));

    model.clearAllPrintable();
    assertTrue(model.selectedCards().isEmpty());

    model.setAllPrintable();
    model.invertPrintable();
    assertTrue(model.selectedCards().isEmpty());
}
```

- [ ] **Step 2: Run GUI model tests**

Run:

```powershell
.\mvnw.cmd -pl gui -Dtest=RailPrintableCardTableModelTest test
```

Expected: FAIL because class does not exist.

- [ ] **Step 3: Implement table model**

Columns:

1. `PRINT` Boolean editable checkbox.
2. `TRAIN`
3. `SEQ`
4. `VEHICLE`
5. `CAN`
6. `DOM`
7. `KEV`
8. `LOAD_NBR`

Methods:

- `setCards(List<RailCarCard> cards)`
- `RailCarCard cardAt(int modelRow)`
- `List<RailCarCard> selectedCards()`
- `int selectedCount()`
- `int totalCount()`
- `void setAllPrintable()`
- `void clearAllPrintable()`
- `void invertPrintable()`
- `void togglePrintableRows(int[] modelRows)`

- [ ] **Step 4: Run table model tests**

Run:

```powershell
.\mvnw.cmd -pl gui -Dtest=RailPrintableCardTableModelTest test
```

Expected: PASS.

- [ ] **Step 5: Commit table model**

```powershell
git add gui/src/main/java/com/tbg/wms/cli/gui/rail/RailPrintableCardTableModel.java gui/src/test/java/com/tbg/wms/cli/gui/rail/RailPrintableCardTableModelTest.java
git commit -m "feat(gui): track printable rail rows"
```

### Task 7: GUI Multi-Train Preview And Selected Generation

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailDialogExecutionSupport.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailDialogActionSupport.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailWorkflowService.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailLabelsDialog.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailDialogExecutionSupportTest.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailDialogActionSupportTest.java`

- [ ] **Step 1: Write failing dialog support tests**

Update preview request expectation:

```java
assertEquals(
        List.of("JC03182026", "JC04152026"),
        support.preparePreviewRequest("JC03182026, JC04152026").trainIds()
);
```

Add generation validation:

```java
IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> support.prepareGenerationRequest(job, List.of(), "", printer, false, false)
);
assertEquals("Select at least one rail row to generate.", ex.getMessage());
```

- [ ] **Step 2: Run affected GUI tests**

Run:

```powershell
.\mvnw.cmd -pl gui -Dtest=RailDialogExecutionSupportTest,RailDialogActionSupportTest test
```

Expected: FAIL due old request shape and generation signature.

- [ ] **Step 3: Update GUI service**

Changes:

- `prepareRailJob(String trainId)` becomes `prepareRailJob(List<String> trainIds)` or overloads with a new method.
- Use core `RailWorkflowService.prepareAll(trainIds)`.
- `PreparedRailJob` stores batch result.
- `generatePdf(PreparedRailJob job, List<RailCarCard> selectedCards, Path outputDir, String printerId)` renders selected cards.
- File naming uses first train plus `-plus-N` for additional trains.

- [ ] **Step 4: Update dialog support**

Changes:

- `PreviewRequest` stores `List<String> trainIds`.
- Use `RailTrainInputParser`.
- `GenerationRequest` accepts selected-card count or selected cards.
- Reject zero selected cards.

- [ ] **Step 5: Update dialog UI**

Changes:

- Replace `DefaultTableModel` with `RailPrintableCardTableModel`.
- Set `previewTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)`.
- Add buttons near preview table: `Select All`, `Clear All`, `Invert`.
- Wire buttons to table model.
- Use selected printable cards for `generate`.
- Convert selected view rows to model rows before toggling any highlighted-row operation.
- Keep card preview based on highlighted row, not printable checkbox state.

- [ ] **Step 6: Run GUI rail tests**

Run:

```powershell
.\mvnw.cmd -pl gui -Dtest="com.tbg.wms.cli.gui.rail.*Test" test
```

Expected: PASS.

- [ ] **Step 7: Commit GUI workflow**

```powershell
git add gui/src/main/java/com/tbg/wms/cli/gui/rail gui/src/test/java/com/tbg/wms/cli/gui/rail
git commit -m "feat(gui): select rail rows before printing"
```

### Task 8: CLI Multi-Train Support

**Files:**
- Modify: `cli/src/main/java/com/tbg/wms/cli/commands/rail/RailPrintCommand.java`
- Modify: `cli/src/main/java/com/tbg/wms/cli/commands/rail/RailPrintCliSupport.java`
- Modify: `cli/src/test/java/com/tbg/wms/cli/commands/rail/RailPrintCliSupportTest.java`

- [ ] **Step 1: Write failing CLI support tests**

Add tests for:

- `--train` accepting delimiter syntax.
- Preview text showing train count or train IDs.
- Single-train validation remains accepted.

- [ ] **Step 2: Run CLI support tests**

Run:

```powershell
.\mvnw.cmd -pl cli -Dtest=RailPrintCliSupportTest test
```

Expected: FAIL where new parsing/formatting is expected.

- [ ] **Step 3: Update CLI command**

Changes:

- Parse `trainId` with `RailTrainInputParser`.
- Call `RailWorkflowService.prepareAll(trainIds)`.
- Generate one combined PDF.
- Keep all cards included in CLI output.
- Update generated filename for multiple trains.

- [ ] **Step 4: Run CLI tests**

Run:

```powershell
.\mvnw.cmd -pl cli -Dtest=RailPrintCliSupportTest,RailPrintCommandTest test
```

Expected: PASS.

- [ ] **Step 5: Commit CLI support**

```powershell
git add cli/src/main/java/com/tbg/wms/cli/commands/rail/RailPrintCommand.java cli/src/main/java/com/tbg/wms/cli/commands/rail/RailPrintCliSupport.java cli/src/test/java/com/tbg/wms/cli/commands/rail/RailPrintCliSupportTest.java
git commit -m "feat(cli): generate rail labels for multiple trains"
```

### Task 9: Documentation And Release Notes

**Files:**
- Modify: `CHANGELOG.md`
- Optional modify: `README.md` or `INSTRUCTIONS_RAILCAR.md` if they already document rail print workflow.

- [ ] **Step 1: Inspect docs format**

Run:

```powershell
Get-Content -LiteralPath CHANGELOG.md -TotalCount 80
```

Expected: identify existing changelog style.

- [ ] **Step 2: Add logical user-facing note**

Content should mention:

- Larger rail label text.
- Multi-train input delimiters.
- Combined PDF output.
- GUI row selection before generating/printing.
- Issue `#42`.

- [ ] **Step 3: Commit docs**

```powershell
git add CHANGELOG.md README.md INSTRUCTIONS_RAILCAR.md
git commit -m "docs(rail): document label generation improvements"
```

Only add docs that actually changed.

### Task 10: Verification

**Files:**
- No direct edits expected unless verification finds issues.

- [ ] **Step 1: Run focused rail tests**

Run:

```powershell
.\mvnw.cmd -pl core,gui,cli -Dtest="*Rail*Test" test
```

Expected: PASS.

- [ ] **Step 2: Run full test suite**

Run:

```powershell
.\mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 3: Generate a local sample PDF if test data permits**

Use an existing safe smoke path or documented rail command if WMS access is available. If live WMS is unavailable, rely on renderer tests and note the limitation.

- [ ] **Step 4: Review git history**

Run:

```powershell
git log --oneline --decorate -8
git status --short
```

Expected:

- Logical conventional commits.
- Only intended files changed for this branch.
- Unrelated pre-existing dirty files still not touched.

- [ ] **Step 5: Update issue**

Add a GitHub issue comment summarizing verification results and any known limitations.

### Task 11: Final Approval And PR

**Files:**
- No direct edits expected.

- [ ] **Step 1: Present final result to user**

Include:

- Changed behavior.
- Commit list.
- Test evidence.
- Any limitations, especially live WMS/manual print verification if not run.

- [ ] **Step 2: Wait for user approval**

Do not open PR until the user approves the shippable result.

- [ ] **Step 3: Push branch**

Run after approval:

```powershell
git push -u origin feat/rail-label-generation-improvements
```

- [ ] **Step 4: Create PR**

Create PR against the repository default branch with:

- Conventional title, for example `feat(rail): improve label generation workflow`
- Link to `Closes #42`
- Summary of behavior changes
- Test evidence
- Screenshots or generated PDF reference if available

Expected: PR is open for final review.

