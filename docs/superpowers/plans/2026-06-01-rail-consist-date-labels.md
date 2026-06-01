# Rail Consist Date Labels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add database-backed rail consist item markers and operator-selected label dates to rail label previews and PDFs.

**Architecture:** Core rail workflow owns item-family counting and print-ready card data. GUI owns date entry, validation, and passing the selected date into the shared workflow. Rendering stays limited to PDF layout.

**Tech Stack:** Java 17, Swing, Maven, JUnit 5, PDFBox.

---

### Task 1: Consist Marker Core

**Files:**
- Create: `core/src/main/java/com/tbg/wms/core/rail/RailConsistMarkerSupport.java`
- Test: `core/src/test/java/com/tbg/wms/core/rail/RailConsistMarkerSupportTest.java`
- Modify: `core/src/main/java/com/tbg/wms/core/rail/RailWorkflowService.java`
- Modify: `core/src/main/java/com/tbg/wms/core/rail/RailCarCard.java`

- [ ] Write failing tests for `D-4 C-3` style marker generation from item families.
- [ ] Implement a small support class that counts distinct valid card line items by `RailFamilyClassifier.FamilyBucket`.
- [ ] Add `labelDate` and `consistMarker` to `RailCarCard`.
- [ ] Have `RailWorkflowService` compute the marker from sorted card items and resolved footprints.
- [ ] Run focused core tests and commit.

### Task 2: Date Entry And Rendering

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailLabelDateSupport.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/rail/RailLabelDateSupportTest.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailLabelsDialog.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailDialogExecutionSupport.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/rail/RailWorkflowService.java`
- Modify: `core/src/main/java/com/tbg/wms/core/rail/RailCardRenderer.java`
- Test: `core/src/test/java/com/tbg/wms/core/rail/RailCardRendererTest.java`

- [ ] Write failing tests for typed `MM-DD-YY` parsing and PDF date/marker text.
- [ ] Add a date text field, spinner-backed calendar dropdown, and Today button to the rail dialog.
- [ ] Pass the selected date into preview preparation so previews and generated PDFs match.
- [ ] Render the date beside the sequence and render the consist marker under the route header.
- [ ] Run focused GUI/core tests and commit.

### Task 3: Help And Documentation

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/GuiHelpTopics.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/rail-db-analysis.md`

- [ ] Update contextual help to mention the date control, Today button, consist markers, and table shortcuts.
- [ ] Update README rail workflow notes.
- [ ] Update changelog and rail DB documentation.
- [ ] Run module tests, package build, push the branch, and update the PR.
