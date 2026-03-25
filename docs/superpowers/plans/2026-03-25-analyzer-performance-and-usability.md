# Analyzer Performance And Usability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the analyzer dashboard usability and performance problems by correcting the appointments query bug, moving analyzer loads off the UI thread, reusing in-memory snapshots after the first live load, and making the `Daily Operations` dashboard substantially more readable.

**Architecture:** Keep the existing analyzer registry and presentation model, but add dialog-level runtime state for asynchronous loading and per-analyzer snapshots. For `Daily Operations`, introduce a dashboard load coordinator that shares one datasource per refresh cycle and executes section loaders with bounded parallelism. Improve dashboard rendering with clearer section containers, headers, formatting, and empty/error/loading states.

**Tech Stack:** Java 21+/Swing, existing analyzer framework, HikariCP datasource lifecycle, JUnit 5, Maven, packaged smoke scripts.

---

## File Structure

### Existing files to modify

- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
  - Move analyzer loading off the UI thread.
  - Track per-analyzer last successful snapshots and in-flight requests.
  - Show cached data immediately when allowed and suppress stale async completions.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanel.java`
  - Add analyzer-level loading/empty handling hooks and swap behavior for cached/live snapshots.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionPanel.java`
  - Improve section framing, headers, empty state, and concise inline error rendering.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProvider.java`
  - Replace sequential section execution with coordinated bounded-parallel execution and shared datasource lifecycle.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoader.java`
  - Fix SQL aliases and type mapping bug causing `ORA-17004`.
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`
  - Add async/cached snapshot coverage.
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanelTest.java`
  - Add loading, empty, and error rendering coverage.
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProviderTest.java`
  - Add shared datasource / partial failure / bounded parallel behavior coverage.
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoaderTest.java`
  - Add regression coverage for the appointments mapper bug.

### New files to create

- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadSnapshot.java`
  - Immutable wrapper for last successful analyzer result plus timestamp.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadSessionState.java`
  - Runtime state holder for cached results and in-flight request sequencing.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsLoadCoordinator.java`
  - Shared datasource + bounded-parallel section execution for dashboard loads.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardLoadingView.java`
  - Compact loading/empty presentation component if the current panel structure needs a dedicated view.

### Verification and reference files

- `docs/superpowers/specs/2026-03-25-analyzer-performance-and-usability-design.md`
- `scripts/run-smoke-tests.ps1`

---

### Task 1: Commit The Approved Spec

**Files:**
- Modify: `docs/superpowers/specs/2026-03-25-analyzer-performance-and-usability-design.md`

- [ ] **Step 1: Review the spec file for final wording only**

Read:
`docs/superpowers/specs/2026-03-25-analyzer-performance-and-usability-design.md`

Expected: no implementation changes needed, only the approved design text.

- [ ] **Step 2: Commit the approved spec**

Run:
```bash
git add docs/superpowers/specs/2026-03-25-analyzer-performance-and-usability-design.md
git commit -m "docs(spec): add analyzer performance and usability design"
```

Expected: spec committed cleanly without unrelated files.

---

### Task 2: Add The Appointments Regression Test

**Files:**
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoaderTest.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoader.java`

- [ ] **Step 1: Write the failing test for distinct date and numeric mappings**

Add a test shaped like:

```java
@Test
void mapsAppointmentDaySeparatelyFromTruckCount() {
    var loader = new AppointmentsSectionLoader();
    var row = new AppointmentsSectionLoader.QueryRow(
            LocalDate.of(2026, 3, 25),
            12,
            8,
            3,
            5,
            2
    );

    var mapped = loader.mapRow(row);

    assertEquals(LocalDate.of(2026, 3, 25), mapped.appointmentDay());
    assertEquals(12, mapped.trucks());
}
```

Also add a query-service-facing regression that verifies the result-set columns are read by distinct aliases such as `apptday` and `trucks`.

- [ ] **Step 2: Run the appointments test to verify it fails**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AppointmentsSectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because the current loader still uses the `trucks` alias for both date and integer reads.

- [ ] **Step 3: Implement the minimal appointments query fix**

Update the SQL and result mapping so:

```java
select a.apptday apptday,
       count(distinct a.car_move_id) outbounds,
       count(distinct loddte) completed,
       count(distinct trknum) inbounds,
       count(distinct clsdte) inb_completed
```

Then map:

```java
toLocalDate(resultSet, "apptday")
integerValue(resultSet, "trucks")
```

Only after the truck-count expression has its own proper alias.

- [ ] **Step 4: Run the appointments test to verify it passes**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AppointmentsSectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoader.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/sections/AppointmentsSectionLoaderTest.java
git commit -m "fix(gui): correct appointments section column mapping"
```

---

### Task 3: Add Analyzer Session Snapshot State

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadSnapshot.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadSessionState.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write failing tests for cached snapshot reuse semantics**

Add tests that prove:

- first analyzer open has no cached snapshot
- a successful load records a snapshot
- revisiting the same analyzer can render the cached snapshot immediately
- a later refresh can replace the cached snapshot

Example shape:

```java
@Test
void reusesLastSuccessfulSnapshotOnRevisit() {
    // arrange dialog with fake provider returning snapshot A then snapshot B
    // first load stores A
    // second selection shows A immediately before refresh completes
}
```

- [ ] **Step 2: Run the dialog test slice to verify it fails**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because the dialog has no analyzer session snapshot state.

- [ ] **Step 3: Implement minimal runtime snapshot state objects**

Create focused files:

```java
public record AnalyzerLoadSnapshot<R>(List<R> rows, Instant fetchedAt) { }
```

and a small mutable session-state helper that tracks:

- last successful snapshot
- whether a load is currently in progress
- monotonically increasing request id for stale-result suppression

- [ ] **Step 4: Run the dialog test slice to verify it passes**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS for the new state behavior tests or at least the new helper tests if split out.

- [ ] **Step 5: Commit**

Run:
```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadSnapshot.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerLoadSessionState.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): add analyzer session snapshot state"
```

---

### Task 4: Move Analyzer Loads Off The UI Thread

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write failing tests for async load and stale-result suppression**

Add tests that prove:

- selecting an analyzer enters a loading state immediately
- result application happens after the async worker completes
- old async completions are ignored when the user has already switched analyzers

Use controllable fake providers or latches rather than sleeping.

- [ ] **Step 2: Run the dialog tests to verify they fail**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because loading is still synchronous and stale completions are not modeled.

- [ ] **Step 3: Implement the minimal async loading path**

Update `AnalyzerDialog` to:

- submit loads to a background executor
- set status to `Loading...` immediately
- record a request token per analyzer selection
- only apply results if the completion token still matches the latest selection
- render cached snapshot immediately when present, then start refresh when required

Do not redesign the dialog layout in this task.

- [ ] **Step 4: Run the dialog tests to verify they pass**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): load analyzers asynchronously"
```

---

### Task 5: Add Shared Dashboard Load Coordination

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsLoadCoordinator.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProvider.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProviderTest.java`

- [ ] **Step 1: Write failing tests for bounded-parallel section loading and partial failure preservation**

Add tests that prove:

- all sections are still returned when one section fails
- shared infrastructure is created once per dashboard load
- section execution is coordinated outside the UI thread

Keep the tests focused on orchestration, not real DB parallel timing.

- [ ] **Step 2: Run the daily operations provider tests to verify they fail**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=DailyOperationsDataProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because the provider still loops sequentially with per-section setup.

- [ ] **Step 3: Implement minimal coordinator and provider integration**

Create a coordinator that:

- builds one datasource for the dashboard load
- submits section work to a small fixed executor
- collects `AnalyzerDashboardSectionSnapshot` results in the original display order
- closes the datasource and executor at the end of the load

Then make `DailyOperationsDataProvider` delegate to it.

- [ ] **Step 4: Run the provider tests to verify they pass**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=DailyOperationsDataProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsLoadCoordinator.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProvider.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dailyops/DailyOperationsDataProviderTest.java
git commit -m "feat(gui): parallelize daily operations section loading"
```

---

### Task 6: Improve Dashboard Readability States

**Files:**
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardLoadingView.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanel.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionPanel.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanelTest.java`

- [ ] **Step 1: Write failing tests for loading, empty, and section error presentation**

Add tests proving:

- dashboard can render an analyzer-level loading state
- empty sections render `No data`
- failure sections render concise error text rather than raw table placeholders
- section titles remain visible

- [ ] **Step 2: Run the dashboard panel tests to verify they fail**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDashboardPanelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because the current panel only swaps raw section components.

- [ ] **Step 3: Implement the minimal presentation improvements**

Add:

- a dedicated loading/empty view if needed
- visible section headers
- padded panel borders
- right-aligned numeric cells where straightforward
- `No data` rendering for empty tables
- concise inline section error panels

Keep styling pragmatic and consistent with Swing defaults.

- [ ] **Step 4: Run the dashboard panel tests to verify they pass**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDashboardPanelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardLoadingView.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanel.java gui/src/main/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardSectionPanel.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/dashboard/AnalyzerDashboardPanelTest.java
git commit -m "feat(gui): improve analyzer dashboard readability states"
```

---

### Task 7: Wire Snapshot Reuse Into Dashboard And Table Analyzers

**Files:**
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java`
- Modify: `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java`

- [ ] **Step 1: Write failing tests for first-load-live and subsequent cached reuse**

Cover:

- first selection performs live load before data appears
- second selection of the same analyzer shows cached data immediately
- explicit refresh forces a live reload
- auto-refresh updates the snapshot timestamp after completion

- [ ] **Step 2: Run the dialog tests to verify they fail**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL until the cache policy is fully wired.

- [ ] **Step 3: Implement minimal snapshot reuse behavior**

In `AnalyzerDialog`, enforce:

- no cached display on the first-ever load of an analyzer
- cached display on revisit if a last successful snapshot exists
- background refresh trigger on explicit refresh and auto-refresh
- replacement of the cached snapshot only when the new live result succeeds

- [ ] **Step 4: Run the dialog tests to verify they pass**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:
```bash
git add gui/src/main/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialog.java gui/src/test/java/com/tbg/wms/cli/gui/analyzers/AnalyzerDialogTest.java
git commit -m "feat(gui): reuse analyzer snapshots after first live load"
```

---

### Task 8: Full Verification On The Merged Behavior

**Files:**
- Modify: none expected unless verification reveals defects

- [ ] **Step 1: Run targeted analyzer tests**

Run:
```bash
.\mvnw.cmd -q -pl gui -am "-Dtest=AnalyzerDialogTest,AnalyzerDashboardPanelTest,DailyOperationsDataProviderTest,AppointmentsSectionLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 2: Run the full GUI suite**

Run:
```bash
.\mvnw.cmd -q -pl gui -am test
```

Expected: PASS with existing known warning noise only.

- [ ] **Step 3: Run packaged smoke**

Run:
```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-smoke-tests.ps1 -Mode packaged -TargetPath .\dist\wms-pallet-tag-system-1.7.6-app -ConfigPath .\.env
```

Expected: PASS.

- [ ] **Step 4: Commit any final follow-up if verification required code changes**

Run:
```bash
git status --short
```

Expected: no unexpected modified code files.

- [ ] **Step 5: Prepare execution handoff**

Expected: branch is ready for either subagent-driven execution or inline execution.
