# Daily Operations Analyzer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a default `Daily Operations` composite dashboard analyzer while preserving existing table analyzers and enabling future dashboard-style analyzers.

**Architecture:** Extend the analyzer framework with presentation-aware definitions so the dialog can render either the existing table view or a new scrollable dashboard view. Implement `Daily Operations` as a dashboard analyzer composed of independently loaded sections with section-scoped error handling, then register follow-on table analyzers in the shared registry.

**Tech Stack:** Java 17, Swing, JUnit 5, Maven, existing `AppConfig`/DB access utilities, Oracle SQL via JDBC/Hikari.

---

## File Structure

### Existing files to modify

- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDefinition.java`
  - Add presentation contract support without breaking existing analyzer responsibilities.
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
  - Switch center content between table and dashboard presentations.
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java`
  - Register `Daily Operations` first and add additional table analyzers.
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`
  - Cover presentation switching and dashboard default behavior.
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java`
  - Assert registry default ordering and visible analyzer names.

### New framework files

- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerPresentation.java`
  - Shared presentation interface or sealed hierarchy for table vs dashboard rendering.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/TableAnalyzerPresentation.java`
  - Presentation adapter for existing row/column analyzers.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/DashboardAnalyzerPresentation.java`
  - Presentation adapter for composite dashboards.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSnapshot.java`
  - Top-level immutable dashboard result model.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionSnapshot.java`
  - Per-section success/error snapshot model.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanel.java`
  - Scrollable Swing panel that renders dashboard sections.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionPanel.java`
  - Reusable titled section container with content/error state.
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanelTest.java`
  - Verify section rendering and error isolation.

### New Daily Operations files

- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsAnalyzerDefinition.java`
  - Dashboard analyzer definition and metadata.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProvider.java`
  - Coordinator that loads all dashboard sections into one snapshot.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsSectionLoader.java`
  - Small section loader contract for dashboard sections.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummarySectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputSectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivitySectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotSectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacitySectionLoader.java`
  - Each loader owns one SQL query and section-specific row mapping.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/*.java` supporting section row/snapshot models as needed
  - Keep models narrow per section rather than forcing one shared mega-model.
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProviderTest.java`
  - Verify successful assembly and section error capture.
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsAnalyzerDefinitionTest.java`
  - Verify metadata and presentation wiring.
- Create: section-level test files under `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/`
  - Verify mapper normalization, date buckets, and shift splits.

### New drill-down table analyzer files

- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/*`
  - `OpenLoadsAnalyzerDefinition`, query service, row model, columns, row styler, optional rule classifier.
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/*`
  - `AllDockDoorsAnalyzerDefinition`, query service, row model, columns, row styler, optional rule classifier.
- Create: corresponding tests under `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/openloads/` and `.../dockdoors/`
  - Follow the existing `unpicked` analyzer test pattern.

## Task 1: Add Presentation-Aware Analyzer Contracts

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDefinition.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerPresentation.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/TableAnalyzerPresentation.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/DashboardAnalyzerPresentation.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write the failing framework test for presentation selection**

```java
@Test
void dialog_shouldRenderDashboardPresentationWhenAnalyzerUsesDashboard() {
    FakeDashboardAnalyzerDefinition analyzer = new FakeDashboardAnalyzerDefinition();
    AnalyzerDialog dialog = new AnalyzerDialog(null, new AnalyzerRegistry(List.of(analyzer)), context());

    dialog.openForTest();

    assertEquals("dashboard", dialog.activePresentationForTest());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because the dialog has no dashboard presentation support yet.

- [ ] **Step 3: Add the minimal presentation contract**

```java
public interface AnalyzerDefinition<R> {
    AnalyzerPresentation<R> presentation();
}
```

Add a table presentation adapter that wraps the existing column set and row styler behavior.

- [ ] **Step 4: Update fake analyzer test fixtures to implement the new contract**

```java
@Override
public AnalyzerPresentation<String> presentation() {
    return TableAnalyzerPresentation.of(columns(), rowStyler());
}
```

- [ ] **Step 5: Run the test to verify the contract compiles and still fails for the right reason**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because the dialog still renders only tables.

- [ ] **Step 6: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDefinition.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerPresentation.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/TableAnalyzerPresentation.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/DashboardAnalyzerPresentation.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "refactor(gui): add analyzer presentation contract"
```

## Task 2: Teach AnalyzerDialog To Render Table Or Dashboard Content

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanel.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionPanel.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSnapshot.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionSnapshot.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanelTest.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write the failing dashboard rendering tests**

```java
@Test
void dashboardPanel_shouldRenderSectionTitlesAndErrors() {
    AnalyzerDashboardSnapshot snapshot = new AnalyzerDashboardSnapshot(List.of(
        AnalyzerDashboardSectionSnapshot.success("Case Pick Summary", componentModel),
        AnalyzerDashboardSectionSnapshot.failure("Unload and Load Activity", "ORA-00942")
    ));

    AnalyzerDashboardPanel panel = new AnalyzerDashboardPanel();
    panel.showSnapshot(snapshot);

    assertEquals(List.of("Case Pick Summary", "Unload and Load Activity"), panel.sectionTitlesForTest());
    assertTrue(panel.sectionErrorTextForTest("Unload and Load Activity").contains("ORA-00942"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest,AnalyzerDashboardPanelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because no dashboard UI classes exist yet.

- [ ] **Step 3: Implement minimal dashboard snapshot and panel types**

```java
public record AnalyzerDashboardSectionSnapshot(String title, boolean failed, String errorText, JComponent content) {}
```

Render a titled vertical list in a scroll pane and expose test-only inspectors similar to existing dialog tests.

- [ ] **Step 4: Update AnalyzerDialog to switch center content by presentation type**

Use a `CardLayout` or equivalent so table analyzers keep the existing table path and dashboard analyzers render the dashboard panel.

- [ ] **Step 5: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest,AnalyzerDashboardPanelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanel.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionPanel.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSnapshot.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionSnapshot.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanelTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): render dashboard analyzers in dialog"
```

## Task 3: Build Daily Operations Dashboard Assembly

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsAnalyzerDefinition.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProvider.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsSectionLoader.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsAnalyzerDefinitionTest.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProviderTest.java`

- [ ] **Step 1: Write the failing dashboard assembly tests**

```java
@Test
void load_shouldAssembleAllSectionSnapshotsInOrder() throws Exception {
    DailyOperationsDataProvider provider = new DailyOperationsDataProvider(List.of(
        section("Case Pick Summary"),
        section("Case Pick Shift Throughput")
    ));

    AnalyzerDashboardSnapshot snapshot = provider.load(context());

    assertEquals(List.of("Case Pick Summary", "Case Pick Shift Throughput"),
            snapshot.sections().stream().map(AnalyzerDashboardSectionSnapshot::title).toList());
}

@Test
void load_shouldCaptureSectionFailureAndContinue() throws Exception {
    DailyOperationsDataProvider provider = new DailyOperationsDataProvider(List.of(
        section("Case Pick Summary"),
        failingSection("Unload and Load Activity", "boom")
    ));

    AnalyzerDashboardSnapshot snapshot = provider.load(context());

    assertFalse(snapshot.sections().get(0).failed());
    assertTrue(snapshot.sections().get(1).failed());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=DailyOperationsAnalyzerDefinitionTest,DailyOperationsDataProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because the dashboard analyzer types do not exist yet.

- [ ] **Step 3: Implement minimal dashboard analyzer definition and sequential loader coordinator**

```java
for (DailyOperationsSectionLoader loader : sectionLoaders) {
    sections.add(loader.loadSection(context));
}
return new AnalyzerDashboardSnapshot(sections, Instant.now(context.clock()));
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=DailyOperationsAnalyzerDefinitionTest,DailyOperationsDataProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsAnalyzerDefinition.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProvider.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsSectionLoader.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsAnalyzerDefinitionTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProviderTest.java
git commit -m "feat(gui): add daily operations dashboard analyzer"
```

## Task 4: Implement Case Pick Summary And Shift Throughput Sections

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummarySectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummaryRow.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputSectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputRow.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummarySectionLoaderTest.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputSectionLoaderTest.java`

- [ ] **Step 1: Write the failing section-mapping tests**

```java
@Test
void summaryLoader_shouldNormalizeNullCountsToZero() { ... }

@Test
void shiftLoader_shouldMapAllShiftBucketsInDisplayOrder() { ... }
```

Use fake result-row adapters or extraction helpers so the tests do not require a live DB.

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=CasePickSummarySectionLoaderTest,CasePickShiftThroughputSectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because the section loaders and row models do not exist yet.

- [ ] **Step 3: Implement the minimal SQL-backed loaders and row mapping**

Keep the SQL in each loader or in a small adjacent query service class. Normalize null numeric values to `0`, convert date columns to `LocalDate`, and map shift columns into explicit fields.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=CasePickSummarySectionLoaderTest,CasePickShiftThroughputSectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummarySectionLoader.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummaryRow.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputSectionLoader.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputRow.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickSummarySectionLoaderTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/CasePickShiftThroughputSectionLoaderTest.java
git commit -m "feat(gui): add case pick dashboard sections"
```

## Task 5: Implement Appointments And Load/Unload Activity Sections

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsRow.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivitySectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivityRow.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoaderTest.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivitySectionLoaderTest.java`

- [ ] **Step 1: Write the failing section tests**

```java
@Test
void appointmentsLoader_shouldComputeRemainingCountsFromCompletedValues() { ... }

@Test
void unloadLoadLoader_shouldMapRailAndTruckSeriesSeparately() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AppointmentsSectionLoaderTest,UnloadLoadActivitySectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL

- [ ] **Step 3: Implement minimal loaders and mappers**

Preserve the dashboard section boundaries even if multiple SQL statements feed one section.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AppointmentsSectionLoaderTest,UnloadLoadActivitySectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoader.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsRow.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivitySectionLoader.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivityRow.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoaderTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/UnloadLoadActivitySectionLoaderTest.java
git commit -m "feat(gui): add appointments and load activity sections"
```

## Task 6: Implement Production And Storage/Capacity Sections

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotSectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotRow.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacitySectionLoader.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacityRow.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotSectionLoaderTest.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacitySectionLoaderTest.java`

- [ ] **Step 1: Write the failing section tests**

```java
@Test
void productionLoader_shouldMapRecentWorkOrders() { ... }

@Test
void storageLoader_shouldPreserveNullSeparatorRowsForLayoutBreaks() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=ProductionSnapshotSectionLoaderTest,StorageCapacitySectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL

- [ ] **Step 3: Implement minimal loaders and layout-friendly mapping**

Handle the unioned null separator row explicitly rather than letting it become a renderer error.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=ProductionSnapshotSectionLoaderTest,StorageCapacitySectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotSectionLoader.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotRow.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacitySectionLoader.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacityRow.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/ProductionSnapshotSectionLoaderTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/StorageCapacitySectionLoaderTest.java
git commit -m "feat(gui): add production and storage dashboard sections"
```

## Task 7: Add Open Loads Table Analyzer

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsAnalyzerDefinition.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryService.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsDataProvider.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsRow.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsColumns.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsRowStyler.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsAnalyzerDefinitionTest.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryServiceTest.java`

- [ ] **Step 1: Write the failing analyzer tests**

```java
@Test
void definition_shouldExposeOpenLoadsAsTableAnalyzer() { ... }

@Test
void queryService_shouldMapPlatformAndStagedCounts() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=OpenLoadsAnalyzerDefinitionTest,OpenLoadsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL

- [ ] **Step 3: Implement the minimal table analyzer using the existing unpicked pattern**

Mirror the existing `unpicked` package structure so the analyzer remains easy to reason about.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=OpenLoadsAnalyzerDefinitionTest,OpenLoadsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads gui/src/test/java/com/tbg/wms/cli/gui/analyzers/openloads
git commit -m "feat(gui): add open loads analyzer"
```

## Task 8: Add All Dock Doors Table Analyzer

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsAnalyzerDefinition.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsQueryService.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsDataProvider.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsRow.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsColumns.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsRowStyler.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsAnalyzerDefinitionTest.java`
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dockdoors/AllDockDoorsQueryServiceTest.java`

- [ ] **Step 1: Write the failing analyzer tests**

```java
@Test
void definition_shouldExposeAllDockDoorsAsTableAnalyzer() { ... }

@Test
void queryService_shouldMapRossiAndShortFlags() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AllDockDoorsAnalyzerDefinitionTest,AllDockDoorsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL

- [ ] **Step 3: Implement the minimal table analyzer**

Reuse the shared table presentation path from Task 1 instead of branching in the dialog.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AllDockDoorsAnalyzerDefinitionTest,AllDockDoorsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dockdoors gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dockdoors
git commit -m "feat(gui): add all dock doors analyzer"
```

## Task 9: Wire Registry Defaults And Regression Coverage

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write the failing registry regression test**

```java
@Test
void defaultRegistry_shouldExposeDailyOperationsFirst() {
    AnalyzerRegistry registry = AnalyzerRegistry.defaultRegistry();

    assertEquals(List.of("Daily Operations", "Unpicked Partials", "Open Loads", "All Dock Doors"),
            registry.definitions().stream().map(AnalyzerDefinition::displayName).toList());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerRegistryTest,AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: FAIL because the registry still exposes the old analyzer list.

- [ ] **Step 3: Update the default registry ordering and any dialog default assertions**

Keep `Daily Operations` first so it becomes the default selection automatically.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerRegistryTest,AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistry.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerRegistryTest.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): make daily operations default analyzer"
```

## Task 10: Final Verification

**Files:**
- Verify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/**`
- Verify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/**`
- Verify: `docs/superpowers/specs/2026-03-25-daily-operations-analyzer-design.md`

- [ ] **Step 1: Run targeted GUI analyzer tests**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest,AnalyzerRegistryTest,AnalyzerDashboardPanelTest,DailyOperationsAnalyzerDefinitionTest,DailyOperationsDataProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

- [ ] **Step 2: Run the full GUI module test suite**

Run: `.\mvnw.cmd -q -pl gui -am test`
Expected: PASS

- [ ] **Step 3: Run any formatter or project verification command already used by the repo if needed**

Run: `.\mvnw.cmd -q -pl gui -am verify`
Expected: PASS, or document any known unrelated failure with exact output.

- [ ] **Step 4: Review git diff for accidental churn**

Run: `git status --short`
Expected: only intended analyzer/dashboard changes.

- [ ] **Step 5: Commit final cleanup if needed**

```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers gui/src/test/java/com/tbg/wms/cli/gui/analyzers
git commit -m "test(gui): verify analyzer dashboard integration"
```
