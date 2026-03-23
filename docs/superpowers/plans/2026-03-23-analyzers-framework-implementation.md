# Analyzers Framework Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable analyzer dialog framework to the GUI, wire it into the tools menu, and deliver the first `Unpicked Partials` analyzer with auto-load, manual refresh, configurable auto-refresh, and customer-rule row styling.

**Architecture:** Build a generic analyzer plugin contract in the GUI module, backed by small focused support classes for registration, dialog orchestration, scheduling, table rendering, and analyzer-specific providers. Keep `LabelGuiFrame` and the tools menu thin, isolate Oracle query logic behind the analyzer provider boundary, and implement `Unpicked Partials` as a typed analyzer module that can be repeated for future analyzers.

**Tech Stack:** Java 17, Swing, JUnit 5, Maven, Oracle JDBC via `DataSourceFactory`, HikariCP.

---

## File Structure

### Existing files to modify

- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - Wire the new analyzer dialog launcher into the frame using the existing support-class style.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupport.java`
  - Add `Analyzers...` to the tools popup in the correct order.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuActions.java`
  - Add the new analyzer-opening action plumbing.
- `gui/src/test/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupportTest.java`
  - Extend menu-label expectations and action support for the new menu item.

### New generic analyzer infrastructure files

- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDefinition.java`
  - Generic contract for analyzer metadata and factories.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java`
  - Returns the available analyzer definitions and default analyzer.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerContext.java`
  - Carries shared dependencies such as `AppConfig`, time source, and DB factory/service hooks.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerResult.java`
  - Holds rows, fetched time, and result metadata.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDataProvider.java`
  - Contract for loading typed rows.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerColumnSet.java`
  - Defines column names, value accessors, widths, alignment, and formatting.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRowStyler.java`
  - Contract for row appearance resolution.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRefreshScheduler.java`
  - Owns timer behavior, toggling, and reset semantics.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadCoordinator.java`
  - Runs asynchronous loads, suppresses stale results, and exposes load callbacks.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerTableModel.java`
  - Generic typed table model for analyzer rows.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerTableCellRenderer.java`
  - Applies analyzer row-style output to Swing table rendering.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
  - Shared analyzer window with dropdown, toolbar, status line, and table.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogState.java`
  - Small state holder for selected analyzer, current rows, loading, and timestamps if needed.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerColorPalette.java`
  - Central place for reusable analyzer row colors.

### New `Unpicked Partials` analyzer files

- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsAnalyzerDefinition.java`
  - Registers the analyzer metadata and its collaborating pieces.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsDataProvider.java`
  - Executes the order-level query and maps result rows.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsQueryService.java`
  - Encapsulates Oracle query text, statement preparation, and result iteration.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRow.java`
  - Immutable typed row record.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsColumns.java`
  - Column definitions and default sort.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRule.java`
  - Enum for customer/rule buckets.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRuleClassifier.java`
  - Maps sold-to/customer fields to a stable rule.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRowStyler.java`
  - Maps rules to row colors and text contrast.

### New tests

- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java`
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRefreshSchedulerTest.java`
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadCoordinatorTest.java`
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRuleClassifierTest.java`
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRowStylerTest.java`
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsDataProviderTest.java`

## Task 1: Add Tools Menu Entry For Analyzers

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupport.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuActions.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupportTest.java`

- [ ] **Step 1: Write the failing menu test**

```java
@Test
void buildToolsMenu_shouldExposeAnalyzersLabelInOrder() {
    JPopupMenu menu = support.buildToolsMenu(new NoOpActions());
    List<String> labels = new ArrayList<>();
    for (var component : menu.getComponents()) {
        if (component instanceof JMenuItem item) {
            labels.add(item.getText());
        }
    }

    assertEquals(List.of(
            "Rail Labels...",
            "Queue Print...",
            "Barcode Generator...",
            "ZPL Preview...",
            "Analyzers...",
            "Resume Incomplete Job...",
            "Settings..."
    ), labels);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=LabelGuiFrameToolMenuSupportTest#buildToolsMenu_shouldExposeAnalyzersLabelInOrder test`
Expected: FAIL because `Analyzers...` is not present in the menu labels.

- [ ] **Step 3: Write minimal menu wiring**

Add the new action contract and menu item without creating the full dialog yet.

```java
interface MenuActions {
    void openAnalyzersDialog();
}
```

```java
addMenuItem(toolsMenu, "Analyzers...", actions::openAnalyzersDialog);
```

In `LabelGuiFrame`, wire the action to a temporary frame-owned launcher method such as `openAnalyzersDialog()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=LabelGuiFrameToolMenuSupportTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupport.java gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuActions.java gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java gui/src/test/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupportTest.java
git commit -m "feat(gui): add analyzers tools menu entry"
```

## Task 2: Build Generic Analyzer Registry Contract

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDefinition.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerContext.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerResult.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDataProvider.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerColumnSet.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java`

- [ ] **Step 1: Write the failing registry test**

```java
@Test
void registry_shouldReturnUnpickedPartialsAsDefaultAnalyzer() {
    AnalyzerRegistry registry = new AnalyzerRegistry(List.of(
            new UnpickedPartialsAnalyzerDefinition()
    ));

    assertEquals("unpicked-partials", registry.defaultAnalyzer().id());
    assertEquals(1, registry.definitions().size());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerRegistryTest test`
Expected: FAIL because the analyzer contract and registry classes do not exist yet.

- [ ] **Step 3: Write minimal analyzer contract and registry**

Define small interfaces/records only. Keep behavior limited to:

```java
public interface AnalyzerDefinition<R> {
    String id();
    String displayName();
    Duration defaultRefreshInterval();
    AnalyzerDataProvider<R> createProvider(AnalyzerContext context);
    AnalyzerColumnSet<R> columns();
}
```

```java
public final class AnalyzerRegistry {
    private final List<AnalyzerDefinition<?>> definitions;

    public List<AnalyzerDefinition<?>> definitions() { ... }
    public AnalyzerDefinition<?> defaultAnalyzer() { ... }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerRegistryTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java
git commit -m "feat(gui): add analyzer registry contracts"
```

## Task 3: Build Refresh Scheduler And Async Load Coordinator

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRefreshScheduler.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadCoordinator.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogState.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRefreshSchedulerTest.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadCoordinatorTest.java`

- [ ] **Step 1: Write the failing scheduler test**

```java
@Test
void manualRefresh_shouldResetTimerBaseline() {
    FakeRefreshTarget target = new FakeRefreshTarget();
    AnalyzerRefreshScheduler scheduler = new AnalyzerRefreshScheduler(target::refreshNow, Clock.systemUTC());

    scheduler.setEnabled(true);
    scheduler.setInterval(Duration.ofSeconds(30));
    scheduler.markRefreshCompleted(Instant.parse("2026-03-23T10:00:00Z"));
    scheduler.requestImmediateRefresh();

    assertEquals(1, target.invocations());
    assertEquals(Instant.parse("2026-03-23T10:00:00Z"), scheduler.lastRefreshBaseline());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerRefreshSchedulerTest,AnalyzerLoadCoordinatorTest test`
Expected: FAIL because the scheduler/coordinator classes do not exist yet.

- [ ] **Step 3: Write minimal scheduler and load coordinator**

Support:

- enable/disable
- interval updates
- timer reset after manual refresh
- request sequence ids for async loads
- stale-result suppression

Keep coordination free of Swing widgets; expose callbacks instead.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerRefreshSchedulerTest,AnalyzerLoadCoordinatorTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRefreshScheduler.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadCoordinator.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogState.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRefreshSchedulerTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadCoordinatorTest.java
git commit -m "feat(gui): add analyzer refresh and load coordination"
```

## Task 4: Build Generic Analyzer Table And Row Styling Infrastructure

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRowStyler.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerTableModel.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerTableCellRenderer.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerColorPalette.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write the failing table/renderer test**

```java
@Test
void renderer_shouldApplyAnalyzerRowColors() {
    AnalyzerRowStyler<String> styler = row -> AnalyzerRowStyle.of(Color.YELLOW, Color.BLACK);
    AnalyzerTableCellRenderer renderer = new AnalyzerTableCellRenderer(styler);
    JTable table = new JTable(1, 1);

    Component component = renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);

    assertEquals(Color.YELLOW, component.getBackground());
    assertEquals(Color.BLACK, component.getForeground());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerDialogTest test`
Expected: FAIL because the generic table/styling infrastructure does not exist yet.

- [ ] **Step 3: Write minimal table and style classes**

Implement:

- typed row storage
- column value lookup
- default row style fallback
- renderer applying per-row style

Keep the renderer generic and free of `Unpicked Partials` knowledge.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerDialogTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRowStyler.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerTableModel.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerTableCellRenderer.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerColorPalette.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): add analyzer table rendering support"
```

## Task 5: Build Generic Analyzer Dialog

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupportTest.java`

- [ ] **Step 1: Write the failing dialog behavior test**

```java
@Test
void dialog_shouldLoadDefaultAnalyzerOnOpenAndShowStatus() {
    FakeAnalyzerDefinition analyzer = new FakeAnalyzerDefinition("unpicked-partials", "Unpicked Partials");
    AnalyzerDialog dialog = new AnalyzerDialog(null, new AnalyzerRegistry(List.of(analyzer)), fakeContext);

    dialog.openForTest();

    assertEquals("Unpicked Partials", dialog.selectedAnalyzerNameForTest());
    assertEquals("Loading...", dialog.statusTextForTest());
    assertEquals(1, analyzer.provider().loadCount());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerDialogTest test`
Expected: FAIL because `AnalyzerDialog` does not exist yet.

- [ ] **Step 3: Write minimal dialog implementation**

Build:

- dropdown
- refresh button
- auto-refresh toggle
- interval control
- status line
- last updated label
- sortable table

Implement:

- auto-load on open
- auto-load on analyzer switch
- refresh button resets timer
- last-successful table remains visible during reload

Wire `LabelGuiFrame.openAnalyzersDialog()` to show this dialog.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerDialogTest,LabelGuiFrameToolMenuSupportTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java gui/src/test/java/com/tbg/wms/cli/gui/LabelGuiFrameToolMenuSupportTest.java
git commit -m "feat(gui): add reusable analyzer dialog"
```

## Task 6: Implement Unpicked Partials Rule Classification And Styling

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRule.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRuleClassifier.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRowStyler.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRuleClassifierTest.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRowStylerTest.java`

- [ ] **Step 1: Write the failing classifier and styler tests**

```java
@Test
void classify_shouldMapKnownSoldToNamesToRuleBuckets() {
    UnpickedPartialsRuleClassifier classifier = new UnpickedPartialsRuleClassifier();

    assertEquals(UnpickedPartialsRule.LOBLAWS, classifier.classify("LOBLAWS DC 67", "LOBLAWS"));
    assertEquals(UnpickedPartialsRule.METRO, classifier.classify("Metro Richelieu", "METRO"));
    assertEquals(UnpickedPartialsRule.DEFAULT, classifier.classify("Restaurant Depot", "#N/A"));
}
```

```java
@Test
void styler_shouldReturnExpectedPaletteForLoblaws() {
    UnpickedPartialsRowStyler styler = new UnpickedPartialsRowStyler();

    AnalyzerRowStyle style = styler.styleFor(rowWithRule(UnpickedPartialsRule.LOBLAWS));

    assertEquals(AnalyzerColorPalette.LOBLAWS_YELLOW, style.background());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=UnpickedPartialsRuleClassifierTest,UnpickedPartialsRowStylerTest test`
Expected: FAIL because the rule classes do not exist yet.

- [ ] **Step 3: Write minimal classifier and styler**

Implement:

- normalized string matching for known customer groups
- enum-backed rule output
- centralized row colors
- default fallback styling

Model `MR_DAIRY` in the enum and classifier even if it shares the default palette initially.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=UnpickedPartialsRuleClassifierTest,UnpickedPartialsRowStylerTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRule.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRuleClassifier.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRowStyler.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRuleClassifierTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRowStylerTest.java
git commit -m "feat(gui): add unpicked partials rule styling"
```

## Task 7: Implement Unpicked Partials Query Provider

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRow.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsQueryService.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsDataProvider.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsColumns.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsDataProviderTest.java`

- [ ] **Step 1: Write the failing provider test**

Write against a fake query executor seam or injectable `ResultSet`-adapter abstraction rather than a live Oracle dependency.

```java
@Test
void provider_shouldMapQueryRowsIntoTypedAnalyzerRows() {
    FakeUnpickedPartialsQueryService query = new FakeUnpickedPartialsQueryService(List.of(
            fakeRecord("3002", "1000057168", "LOBLAWS", "LOBLAWS DC 67", 255, 0, 0, 0, 255)
    ));
    UnpickedPartialsDataProvider provider = new UnpickedPartialsDataProvider(query, new UnpickedPartialsRuleClassifier(), Clock.systemUTC());

    AnalyzerResult<UnpickedPartialsRow> result = provider.load(fakeAnalyzerContext());

    assertEquals(1, result.rows().size());
    assertEquals("1000057168", result.rows().get(0).orderNumber());
    assertEquals(UnpickedPartialsRule.LOBLAWS, result.rows().get(0).rule());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=UnpickedPartialsDataProviderTest test`
Expected: FAIL because the row/query/provider classes do not exist yet.

- [ ] **Step 3: Write minimal row, query service, and provider**

Implementation requirements:

- use the approved order-level query shape
- keep SQL encapsulated in `UnpickedPartialsQueryService`
- create the Oracle `DataSource` through `DataSourceFactory` from `AnalyzerContext`
- use `PreparedStatement`
- map rows into `UnpickedPartialsRow`
- derive the rule through `UnpickedPartialsRuleClassifier`
- return typed `AnalyzerResult<UnpickedPartialsRow>`

Keep the first version scoped to current `AppConfig` site only. Do not add site selectors or extra filters.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=UnpickedPartialsDataProviderTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsRow.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsQueryService.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsDataProvider.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsColumns.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsDataProviderTest.java
git commit -m "feat(gui): add unpicked partials data provider"
```

## Task 8: Register Unpicked Partials In The Generic Dialog

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsAnalyzerDefinition.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java`
- Test: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write the failing registration test**

```java
@Test
void registry_shouldExposeUnpickedPartialsDisplayName() {
    AnalyzerRegistry registry = AnalyzerRegistry.defaultRegistry();

    assertEquals(List.of("Unpicked Partials"),
            registry.definitions().stream().map(AnalyzerDefinition::displayName).toList());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerRegistryTest,AnalyzerDialogTest test`
Expected: FAIL because the real registry does not yet register `Unpicked Partials`.

- [ ] **Step 3: Write minimal registration and dialog hookup**

Implement:

- `UnpickedPartialsAnalyzerDefinition`
- default registry factory
- dialog table binding to the definition’s columns and styler
- default sort for `Unpicked Partials`

At the end of this step, opening the analyzer dialog should show live `Unpicked Partials` data.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -q -pl gui -Dtest=AnalyzerRegistryTest,AnalyzerDialogTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/unpicked/UnpickedPartialsAnalyzerDefinition.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): register unpicked partials analyzer"
```

## Task 9: Run Focused Verification And Full GUI Module Verification

**Files:**
- Verify only

- [ ] **Step 1: Run focused analyzer tests**

Run:

```bash
.\mvnw.cmd -q -pl gui -Dtest=LabelGuiFrameToolMenuSupportTest,AnalyzerRegistryTest,AnalyzerRefreshSchedulerTest,AnalyzerLoadCoordinatorTest,AnalyzerDialogTest,UnpickedPartialsRuleClassifierTest,UnpickedPartialsRowStylerTest,UnpickedPartialsDataProviderTest test
```

Expected: PASS.

- [ ] **Step 2: Run full GUI module test suite**

Run:

```bash
.\mvnw.cmd -q -pl gui test
```

Expected: PASS.

- [ ] **Step 3: Run application packaging-safe compile check**

Run:

```bash
.\mvnw.cmd -q -pl gui -am "-DskipTests" package
```

Expected: PASS.

- [ ] **Step 4: Manual GUI verification**

Run the app and verify:

```bash
java -jar cli/target/cli-1.7.6.jar gui
```

Manual checks:

- `Tools -> Analyzers...` opens the dialog
- `Unpicked Partials` loads automatically on open
- refresh button reloads and resets timer countdown
- auto-refresh toggle disables/enables polling
- interval change affects the next refresh cadence
- row colors match customer rule buckets
- status line shows loading, last updated, and failure states without modal interruption

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java gui/src/test/java
git commit -m "feat(gui): ship analyzer framework and unpicked partials view"
```

## Notes For Execution

- Follow `@superpowers/test-driven-development` exactly. Do not write production code before the corresponding failing test exists.
- Keep analyzer-specific logic out of `LabelGuiFrame` and the generic analyzer dialog.
- Prefer manual registry registration over service loading for this first version unless a concrete need appears during implementation.
- Keep the first version read-only. Do not add export or copy actions during this plan.
- If the provider test becomes hard to write without Oracle, introduce a smaller query seam rather than weakening test coverage.
- Reuse the repo’s existing small-support-class pattern. Avoid creating one oversized `AnalyzerDialog` helper that owns scheduling, querying, and rendering all at once.
