# SSCC Label Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the old Amazon SSCC generation path with a local Java SSCC label tool that imports CSV, supports manual row entry, and renders the exact existing ZPL-based layout.

**Architecture:** Move the label definition into a new `core/sscc` package that owns CSV parsing, barcode formatting, row grouping, and ZPL generation. Add a dedicated Swing dialog in `gui` for import/manual entry/preview, wire it into the Tools menu, and reuse the existing ZPL preview dialog for visual verification so the layout stays anchored to the proven ZPL template.

**Tech Stack:** Java 21, Swing, PDF/ZPL preview support already present in the app, RFC4180-style CSV parsing with in-repo helpers, barcode helpers built on the existing Code 128 utilities.

---

### Task 1: Define the local SSCC label domain and template in `core`

**Files:**
- Create: `core/src/main/java/com/tbg/wms/core/sscc/SsccLabelRow.java`
- Create: `core/src/main/java/com/tbg/wms/core/sscc/SsccLabelGroup.java`
- Create: `core/src/main/java/com/tbg/wms/core/sscc/SsccCsvSupport.java`
- Create: `core/src/main/java/com/tbg/wms/core/sscc/SsccBarcodeSupport.java`
- Create: `core/src/main/java/com/tbg/wms/core/sscc/SsccLabelTemplate.java`
- Create: `core/src/main/java/com/tbg/wms/core/sscc/package-info.java`
- Test: `core/src/test/java/com/tbg/wms/core/sscc/SsccCsvSupportTest.java`
- Test: `core/src/test/java/com/tbg/wms/core/sscc/SsccLabelTemplateTest.java`
- Test: `core/src/test/java/com/tbg/wms/core/sscc/SsccBarcodeSupportTest.java`
- Delete: `core/src/main/java/com/tbg/wms/core/amazon/AmazonSsccPdfGenerator.java`
- Delete: `core/src/test/java/com/tbg/wms/core/amazon/AmazonSsccPdfGeneratorTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void csvParser_requiresWorkbookHeaders_and_preserves_row_values()

@Test
void barcodeHelper_formats_18_digit_sscc_into_gs1_code128_payload()

@Test
void template_renders_exact_zpl_layout_with_mixed_sku_and_ship_from_block()
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -pl core test -Dtest=Sscc*Test -DfailIfNoTests=false`

- [ ] **Step 3: Implement the minimal core code**

Implement CSV header normalization and row parsing, label grouping by `Sales Order # + New Received LPN`, mixed-SKU detection, SSCC barcode formatting, and a dedicated SSCC ZPL template that matches the current working ZPL output.

- [ ] **Step 4: Run the tests and verify they pass**

Run: `mvn -pl core test -Dtest=Sscc*Test`

- [ ] **Step 5: Remove the obsolete Amazon generator**

Delete the old Amazon-specific Java generator and its test now that the new SSCC core owns the behavior.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/java/com/tbg/wms/core/sscc core/src/test/java/com/tbg/wms/core/sscc
git rm core/src/main/java/com/tbg/wms/core/amazon/AmazonSsccPdfGenerator.java core/src/test/java/com/tbg/wms/core/amazon/AmazonSsccPdfGeneratorTest.java
git commit -m "feat: add local sscc label core"
```

### Task 2: Build the SSCC label tool dialog in `gui`

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/sscc/SsccLabelDialog.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/sscc/SsccLabelTableModel.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/sscc/SsccLabelDialogSupport.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/sscc/SsccLabelDialogActions.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/sscc/SsccLabelDialogExecutionSupport.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/sscc/package-info.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupport.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuActions.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/GuiHelpTopics.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/ZplPreviewToolDialog.java` only if a direct SSCC preview shortcut is needed
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/sscc/SsccLabelDialogSupportTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void tool_menu_includes_sscc_label_tool()

@Test
void help_topic_lists_required_csv_headers()

@Test
void dialog_can_add_manual_rows_and_import_csv_rows()
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -pl gui test -Dtest=Sscc*Test -DfailIfNoTests=false`

- [ ] **Step 3: Implement the dialog**

Create a modal SSCC tool window with:
- CSV import button and file chooser
- Manual entry fields for the same row data
- A table showing staged rows
- Validation/help text that explicitly names the required CSV headers
- Preview/export actions that use the core SSCC template and barcode helpers

- [ ] **Step 4: Wire the tool into the main window**

Add the new menu item under Tools, expose the new action on `LabelGuiFrame`, and add a help topic entry that explains the CSV header contract.

- [ ] **Step 5: Run GUI tests**

Run: `mvn -pl gui test -Dtest=Sscc*Test`

- [ ] **Step 6: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/sscc gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupport.java gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuActions.java gui/src/main/java/com/tbg/wms/cli/gui/GuiHelpTopics.java
git commit -m "feat: add sscc label tool dialog"
```

### Task 3: Clean up obsolete Amazon SSCC artifacts and update docs

**Files:**
- Delete: `scripts/generate-amazon-sscc-labels.ps1`
- Modify: `README.md`
- Modify: `CHANGELOG.md` if the repository keeps user-visible workflow notes there
- Modify: any tool/menu docs that mention the Amazon SSCC generator or the old script

- [ ] **Step 1: Write the failing documentation check**

Create a quick grep-based check or a focused test if documentation references the deleted Amazon SSCC path.

- [ ] **Step 2: Remove obsolete docs and scripts**

Delete the PowerShell generator script and update any operator docs to point to the new GUI SSCC tool and its CSV header requirements.

- [ ] **Step 3: Verify there are no stale references**

Run: `Get-ChildItem -Recurse -File | Select-String -Pattern 'generate-amazon-sscc-labels|AmazonSsccPdfGenerator|amazon-sscc-labels-combined|amazon SSCC'`

- [ ] **Step 4: Run the full relevant test set**

Run: `mvn test -pl core,gui`

- [ ] **Step 5: Commit**

```bash
git add README.md CHANGELOG.md scripts
git commit -m "docs: remove legacy amazon sscc workflow"
```

### Task 4: Manual verification

**Files:**
- No code changes expected

- [ ] **Step 1: Launch the app**

Run the GUI and open the new SSCC tool from the Tools menu.

- [ ] **Step 2: Verify import and manual entry**

Import a CSV with the required headers, add one row manually, and confirm the grouped labels and barcode formatting match the existing ZPL template.

- [ ] **Step 3: Verify preview**

Open the generated preview and compare the ship-from block, mixed-SKU banner, pallet SSCC, and bottom item/date block against the working ZPL-based output.

- [ ] **Step 4: Final commit**

```bash
git add .
git commit -m "feat: ship new sscc label tool"
```
