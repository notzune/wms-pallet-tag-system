# SRP Cleanup Handoff

Date: 2026-06-16

Branch/workspace: local workspace at `C:\Users\zrashed\Documents\Code\wms-pallet-tag-system`

## Current State

We paused the workspace-wide SRP/cleanup effort after completing the first narrow correctness pass around label selection.

The initial audit found the highest SRP pressure in GUI orchestration and analyzer query services, with `LabelSelectionRef` selected as the first safe pass because it was directly referenced by the user and had a small, testable surface.

## Completed Work

Files intentionally changed:

- `core/src/main/java/com/tbg/wms/core/label/LabelSelectionRef.java`
  - Added validation that carrier-move `stopPosition` must be `>= 1`.
  - Added value-based `equals`.
  - Added value-based `hashCode`.
  - Added stable `toString`.
- `core/src/main/java/com/tbg/wms/core/label/LabelSelectionSupport.java`
  - `selectLpnsByRefs` now rejects null selected refs.
  - `selectLpnsByRefs` now rejects selected LPN IDs that are not present in available LPNs instead of silently dropping them.
- `core/src/test/java/com/tbg/wms/core/label/LabelSelectionSupportTest.java`
  - Added tests for normalized value equality/hash behavior.
  - Added test for invalid carrier-move stop position.
  - Added tests for null and stale selected label refs.

## TDD Evidence

Red run:

```powershell
.\mvnw.cmd -pl core -Dtest=LabelSelectionSupportTest test
```

Expected failures were observed before implementation:

- `labelSelectionRef_shouldCompareByNormalizedValue`
- `labelSelectionRef_shouldRejectInvalidCarrierMoveStopPosition`
- `selectLpnsByRefs_shouldRejectNullSelectionRef`
- `selectLpnsByRefs_shouldRejectSelectionsThatDoNotMatchAvailableLpns`

Green/final verification:

```powershell
.\mvnw.cmd -pl core -Dtest=LabelSelectionSupportTest test
.\mvnw.cmd -pl core test
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintWorkflowServiceTest,GuiPreviewDisplaySupportTest,GuiPrintExecutionSupportTest,GuiPrintFlowSupportTest,PreviewSelectionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Results:

- Focused label selection test passed: 11 tests.
- Full `core` test suite passed: 261 tests.
- Downstream GUI selection/print tests passed: 22 tests.

Note: `mvn` was not on PATH; use `.\mvnw.cmd`.

## Existing Dirty Worktree Context

Unrelated files were already modified before this SRP pass and were not touched:

- `cli/src/main/java/com/tbg/wms/cli/commands/BarcodeCommand.java`
- `cli/src/test/java/com/tbg/wms/cli/commands/BarcodeCommandTest.java`
- `docs/superpowers/plans/2026-06-12-putty-terminal-barcode-prototype.md`

SRP pass files currently modified:

- `core/src/main/java/com/tbg/wms/core/label/LabelSelectionRef.java`
- `core/src/main/java/com/tbg/wms/core/label/LabelSelectionSupport.java`
- `core/src/test/java/com/tbg/wms/core/label/LabelSelectionSupportTest.java`

## Audit Findings To Resume

Recommended next passes, in order:

1. `LabelGuiFrame` SRP pass
   - File: `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
   - Issue: 1,094-line frame owns printer loading, preview, print execution, DB status, update checks, uninstall flow, generated preview dialogs, and UI state transitions.
   - Suggested first extraction: reusable async/SwingWorker runner for busy/ready/error/status handling.

2. Analyzer DB/query pass
   - First target: `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryService.java`
   - Issue: large embedded SQL, DB lifecycle, row mapping, and business calculations in one class.
   - Suggested split: SQL/query definition, data-source/repository execution, row mapper.

3. Advanced print workflow pass
   - File: `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
   - Issue: queue orchestration, carrier-move preparation, checkpoint wiring, print execution, resume handling, and DTO ownership in one service.
   - Suggested split: queue workflow, carrier-move workflow, checkpoint resume.

4. Print task planning pass
   - File: `gui/src/main/java/com/tbg/wms/cli/gui/PrintTaskPlanner.java`
   - Issue: task planning, label data building, artifact naming, info-tag insertion, selection filtering, and count estimation are still bundled.
   - Suggested split: pallet label task builder vs info-tag task builder.

5. DB connection consolidation pass
   - Files: `core/src/main/java/com/tbg/wms/core/db/DataSourceFactory.java`, `db/src/main/java/com/tbg/wms/db/DbConnectionPool.java`
   - Issue: Hikari setup exists in more than one place.
   - Suggested timing: after analyzer query services are cleaned up.

## 2026-06-19 Progress Note

Completed the DB connection consolidation pass.

Files intentionally changed:

- `core/src/main/java/com/tbg/wms/core/db/OracleHikariConfigSupport.java`
  - Added shared Oracle/Hikari configuration builder.
  - Preserves common settings: JDBC URL, credentials, max pool size, connection timeout, validation timeout, pool name, auto-commit, and Oracle validation query.
  - Allows optional caller-specific settings for read-only mode, minimum idle, initialization fail timeout, and leak detection.
- `core/src/main/java/com/tbg/wms/core/db/DataSourceFactory.java`
  - Replaced local Hikari setup with `OracleHikariConfigSupport`.
  - Kept existing core factory behavior and pool name.
- `db/src/main/java/com/tbg/wms/db/DbConnectionPool.java`
  - Replaced local Hikari setup with `OracleHikariConfigSupport`.
  - Kept DB pool candidate behavior: read-only, `minimumIdle=0`, deferred initialization, site-specific pool name, and 60s leak detection.
- `core/src/test/java/com/tbg/wms/core/db/OracleHikariConfigSupportTest.java`
  - Added tests for common Oracle pool settings.
  - Added tests for read-only probe pool settings.

TDD evidence:

```powershell
.\mvnw.cmd -pl core -Dtest=OracleHikariConfigSupportTest test
```

Red result before implementation:

- Compilation failed because `OracleHikariConfigSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl core -Dtest=OracleHikariConfigSupportTest test
.\mvnw.cmd -pl db -am test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused core Hikari config test passed: 2 tests.
- `db -am` reactor passed: core 263 tests, db 44 tests.
- `gui -am` reactor passed: gui 204 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 2

Completed another narrow `LabelGuiFrame` SRP slice for DB status refresh handling.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/GuiDbStatusRefreshSupport.java`
  - Added a small helper that owns DB-status refresh state transitions around a connectivity probe.
  - Keeps checking, connected, and failure classification logic out of the frame callback.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - `refreshDbStatusAsync` now delegates status-state decisions through `GuiDbStatusRefreshSupport`.
  - The frame still supplies the concrete `DbConnectionPool` probe so the helper remains unit-testable.
- `gui/src/test/java/com/tbg/wms/cli/gui/GuiDbStatusRefreshSupportTest.java`
  - Added tests for checking state, successful probe execution, connectivity failure mapping, and non-connectivity failure filtering.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiDbStatusRefreshSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `GuiDbStatusRefreshSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiDbStatusRefreshSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused GUI DB status refresh test passed: 4 tests.
- `gui -am` reactor passed: gui 208 tests.

## 2026-06-19 Progress Note 3

Completed another narrow `LabelGuiFrame` SRP slice for generated-label preview routing.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/GeneratedLabelPreviewSupport.java`
  - Added a helper that owns generated-label preview titles and shipment vs carrier-move document routing.
  - Uses a small `DocumentBuilder` interface so routing decisions can be unit-tested without generating real ZPL.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - Generated-label preview document construction and title selection now delegate through `GeneratedLabelPreviewSupport`.
  - Dialog ownership and visibility remain in the frame.
- `gui/src/test/java/com/tbg/wms/cli/gui/GeneratedLabelPreviewSupportTest.java`
  - Added tests for title selection, shipment document routing, and carrier-move document routing.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GeneratedLabelPreviewSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `GeneratedLabelPreviewSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GeneratedLabelPreviewSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused generated-label preview support test passed: 3 tests.
- `gui -am` reactor passed: gui 211 tests.

## 2026-06-19 Progress Note 4

Completed another narrow `LabelGuiFrame` SRP slice for printer-combo selection policy.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/FramePrinterSelectionSupport.java`
  - Added `SelectionAction` and a pure `resolveSelectionAction` policy method.
  - Encodes separator handling: restore last valid printer when available, otherwise restore the default selection.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - Printer combo action listener now delegates selection-policy decisions to `FramePrinterSelectionSupport`.
  - The frame still owns the actual combo-box mutation and recursion guard.
- `gui/src/test/java/com/tbg/wms/cli/gui/FramePrinterSelectionSupportTest.java`
  - Added tests for separator-with-last-valid, separator-without-last-valid, and selectable-printer actions.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=FramePrinterSelectionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `SelectionAction` and `resolveSelectionAction` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=FramePrinterSelectionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused frame printer selection support test passed: 5 tests.
- `gui -am` reactor passed: gui 214 tests.

## 2026-06-19 Progress Note 5

Completed another narrow `LabelGuiFrame` SRP slice for update-check start/duplicate handling.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/GuiUpdateExecutionSupport.java`
  - Added `CheckStartPlan` and `planCheckStart`.
  - Encodes the update-check guard policy: duplicate user checks report "already in progress", duplicate background checks stay quiet, and fresh checks optionally emit "Checking for updates...".
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - `checkForUpdatesAsync` now delegates the start/duplicate decision to `GuiUpdateExecutionSupport`.
  - The frame still owns async execution, release prompt display, and UI mutation.
- `gui/src/test/java/com/tbg/wms/cli/gui/GuiUpdateExecutionSupportTest.java`
  - Added tests for duplicate user checks, duplicate background checks, fresh checks with a status output, and fresh checks without a status output.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiUpdateExecutionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `CheckStartPlan` and `planCheckStart` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiUpdateExecutionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused GUI update execution support test passed: 6 tests.
- `gui -am` reactor passed: gui 215 tests.

## 2026-06-19 Progress Note 6

Verified and recorded previously unrecorded SRP slices that were already present in the workspace.

Files intentionally changed before this checkpoint:

- `gui/src/main/java/com/tbg/wms/cli/gui/GuiAsyncTaskRunner.java`
  - Added a reusable SwingWorker wrapper for background task execution and EDT completion/failure callbacks.
  - `LabelGuiFrame` now uses it for printer loading, preview preparation, print execution, DB status refresh, update checks, and guided update download.
- `gui/src/main/java/com/tbg/wms/cli/gui/GuiActionButtonStateSupport.java`
  - Added focused button-state helper for busy, ready, and post-print action state.
  - `LabelGuiFrame` now delegates repeated preview/clear/show-labels/print enablement transitions.
- `gui/src/main/java/com/tbg/wms/cli/gui/CarrierMoveWorkflowSupport.java`
  - Added carrier-move preparation workflow support around stop resolution, printable shipment filtering, and stop group creation.
  - `AdvancedPrintWorkflowService.prepareCarrierMoveJob` now delegates this workflow.
- `gui/src/main/java/com/tbg/wms/cli/gui/PalletLabelPrintTaskSupport.java`
  - Extracted pallet-label print task construction from `PrintTaskPlanner`.
- `gui/src/main/java/com/tbg/wms/cli/gui/InfoTagPrintTaskSupport.java`
  - Extracted shipment, stop, and final info-tag print task construction from `PrintTaskPlanner`.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsSql.java`
  - Extracted the Open Loads SQL text from `OpenLoadsQueryService`.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryRow.java`
  - Added a raw query-row record for Open Loads result mapping.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsRowMapper.java`
  - Extracted Open Loads `ResultSet` reading and null-to-zero mapping from `OpenLoadsQueryService`.

Verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiAsyncTaskRunnerTest,GuiActionButtonStateSupportTest,CarrierMoveWorkflowSupportTest,PalletLabelPrintTaskSupportTest,InfoTagPrintTaskSupportTest,OpenLoadsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Results:

- Focused helper tests passed: 17 tests.

## 2026-06-19 Progress Note 7

Completed another narrow `LabelGuiFrame` SRP slice for advanced print service dispatch.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/GuiAdvancedPrintRunner.java`
  - Added a small adapter implementing `GuiPrintExecutionSupport.PrintRunner`.
  - Owns shipment vs carrier-move calls into `AdvancedPrintWorkflowService`.
  - Provides a package-private `Gateway` seam for focused tests without real print execution.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - Replaced the inline anonymous `PrintRunner` in `confirmAndPrint` with `GuiAdvancedPrintRunner`.
- `gui/src/test/java/com/tbg/wms/cli/gui/GuiAdvancedPrintRunnerTest.java`
  - Added tests for shipment print delegation and carrier-move print delegation.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiAdvancedPrintRunnerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `GuiAdvancedPrintRunner` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiAdvancedPrintRunnerTest,GuiPrintExecutionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused adapter/print execution tests passed: 7 tests.
- `gui -am` reactor passed: gui 217 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 8

Completed another narrow `AdvancedPrintWorkflowService` SRP slice for queue workflow orchestration.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/QueueWorkflowSupport.java`
  - Added `prepareQueue` to own queue normalization plus shipment/carrier item resolution through a small `PreparationGateway`.
  - Added `printQueue` to own queue item dispatch and result aggregation through a small `PrintGateway`.
  - Existing normalization and summarization behavior remains in the same support class.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - `prepareQueue` now delegates queue resolution to `QueueWorkflowSupport`.
  - `printQueue` now delegates queue dispatch/aggregation to `QueueWorkflowSupport`.
  - Queue DTO constructors/factories were relaxed from private to package-private so package support classes can construct prepared queue objects while keeping the public API unchanged.
- `gui/src/test/java/com/tbg/wms/cli/gui/QueueWorkflowSupportTest.java`
  - Added tests for mixed shipment/carrier queue preparation order.
  - Added tests for mixed shipment/carrier queue print dispatch and aggregate totals.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=QueueWorkflowSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `QueueWorkflowSupport.PreparationGateway`, `QueueWorkflowSupport.PrintGateway`, `prepareQueue`, and `printQueue` did not exist.
- Compilation also showed the queue DTO factories/constructor were too private for the intended package-level support class.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=QueueWorkflowSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused queue workflow tests passed: 4 tests.
- `gui -am` reactor passed: gui 219 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 9

Completed another narrow `AdvancedPrintWorkflowService` SRP slice for prepared print-job task planning and execution dispatch.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintJobPrintSupport.java`
  - Added a package-private helper that owns shipment and carrier-move print task planning before checkpoint execution.
  - Shipment path filters selected LPNs, builds shipment print tasks, and dispatches through an `ExecutionGateway`.
  - Carrier-move path builds carrier task batches, resolves first-shipment routing, and dispatches through the same gateway.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - `printShipmentJob` now delegates filtering/task-building/execution dispatch to `AdvancedPrintJobPrintSupport`.
  - `printCarrierMoveJob` now delegates task-building/routing/execution dispatch to `AdvancedPrintJobPrintSupport`.
  - Public print APIs remain unchanged.
- `gui/src/test/java/com/tbg/wms/cli/gui/AdvancedPrintJobPrintSupportTest.java`
  - Added tests for selected-shipment LPN filtering plus pallet/info task dispatch.
  - Added tests for carrier-move task building and routing handoff.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintJobPrintSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `AdvancedPrintJobPrintSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintJobPrintSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused print-job support tests passed: 2 tests.
- `gui -am` reactor passed: gui 221 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 10

Completed a follow-up `AdvancedPrintWorkflowService` SRP cleanup for print execution gateway ownership.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintJobPrintSupport.java`
  - Changed the support to own a constructor-injected `ExecutionGateway`.
  - Removed per-call gateway parameters from shipment and carrier-move print methods.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - Creates one `AdvancedPrintJobPrintSupport` instance wired to `AdvancedPrintExecutionSupport`.
  - Shipment and carrier-move print methods now delegate directly without creating anonymous gateway adapters per call.
- `gui/src/test/java/com/tbg/wms/cli/gui/AdvancedPrintJobPrintSupportTest.java`
  - Updated focused tests to verify the constructor-injected gateway API.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintJobPrintSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `AdvancedPrintJobPrintSupport` did not accept an `ExecutionGateway` constructor argument.
- Compilation also failed because print methods still required a per-call gateway parameter.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintJobPrintSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused print-job support tests passed: 2 tests.
- `gui -am` reactor passed: gui 221 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 11

Completed a follow-up `AdvancedPrintWorkflowService` SRP cleanup for queue gateway ownership.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/QueueWorkflowSupport.java`
  - Changed the support to own constructor-injected preparation and print gateways.
  - Removed per-call gateway parameters from queue preparation and queue printing.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - Creates one `QueueWorkflowSupport` instance wired to the existing shipment/carrier preparation and print methods.
  - `prepareQueue` and `printQueue` now delegate directly without creating anonymous gateway adapters per call.
- `gui/src/test/java/com/tbg/wms/cli/gui/QueueWorkflowSupportTest.java`
  - Updated focused tests to verify the constructor-injected gateway API.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=QueueWorkflowSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `QueueWorkflowSupport` did not accept preparation/print gateways in its constructor.
- Compilation also failed because `prepareQueue` and `printQueue` still required per-call gateway parameters.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=QueueWorkflowSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused queue workflow tests passed: 4 tests.
- `gui -am` reactor passed: 221 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 12

Completed a follow-up `AdvancedPrintWorkflowService` SRP cleanup for checkpoint execution gateway wiring.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintCheckpointGateway.java`
  - Added a package-private adapter implementing `AdvancedPrintExecutionSupport.CheckpointGateway`.
  - Owns the bridge from execution support checkpoint calls to `PrintCheckpointSupport`.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - Replaced inline checkpoint gateway wiring in the constructor with `AdvancedPrintCheckpointGateway`.
  - Removed the now-unused `PrinterConfig` import.
- `gui/src/test/java/com/tbg/wms/cli/gui/AdvancedPrintCheckpointGatewayTest.java`
  - Added a focused print-to-file test proving checkpoint creation and task execution delegate through `PrintCheckpointSupport`.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintCheckpointGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `AdvancedPrintCheckpointGateway` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintCheckpointGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused checkpoint gateway test passed: 1 test.
- `gui -am` reactor passed: 222 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 13

Completed a follow-up `AdvancedPrintWorkflowService` SRP cleanup for checkpoint resume ownership.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintResumeSupport.java`
  - Added a package-private support class for resume-related workflow operations.
  - Owns listing incomplete checkpoint candidates and mapping resumed checkpoints to `PrintResult`.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - Creates one `AdvancedPrintResumeSupport` wired to `PrintCheckpointSupport` and `AdvancedPrintResultSupport`.
  - `listIncompleteJobs` and `resumeJob` now delegate through the resume support.
- `gui/src/test/java/com/tbg/wms/cli/gui/AdvancedPrintResumeSupportTest.java`
  - Added a focused test for listing incomplete jobs and resuming a print-to-file checkpoint through the new support.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintResumeSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `AdvancedPrintResumeSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintResumeSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused resume support test passed: 1 test.
- `gui -am` reactor passed: 223 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 14

Completed a follow-up `AdvancedPrintWorkflowService` SRP cleanup for print-job execution gateway wiring.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintJobExecutionGateway.java`
  - Added a package-private adapter implementing `AdvancedPrintJobPrintSupport.ExecutionGateway`.
  - Owns the bridge from print-job support to `AdvancedPrintExecutionSupport`.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - Replaced inline print execution gateway wiring in the constructor with `AdvancedPrintJobExecutionGateway`.
  - Removed the now-unused `PrinterRoutingService` import.
- `gui/src/test/java/com/tbg/wms/cli/gui/AdvancedPrintJobExecutionGatewayTest.java`
  - Added a focused print-to-file shipment execution test proving the adapter delegates to `AdvancedPrintExecutionSupport`.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintJobExecutionGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `AdvancedPrintJobExecutionGateway` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintJobExecutionGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused print-job execution gateway test passed: 1 test.
- `gui -am` reactor passed: 224 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 15

Completed a follow-up `AdvancedPrintWorkflowService` SRP cleanup for queue gateway wiring.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintQueueGateway.java`
  - Added a package-private adapter implementing both `QueueWorkflowSupport.PreparationGateway` and `QueueWorkflowSupport.PrintGateway`.
  - Owns the bridge from queue workflow support to configured shipment/carrier preparation and print operations.
- `gui/src/main/java/com/tbg/wms/cli/gui/AdvancedPrintWorkflowService.java`
  - Replaced inline queue preparation and print gateway wiring in the constructor with one `AdvancedPrintQueueGateway`.
  - Wires the gateway with method references instead of anonymous classes.
- `gui/src/test/java/com/tbg/wms/cli/gui/AdvancedPrintQueueGatewayTest.java`
  - Added a focused test proving prepare and print methods delegate to configured operations.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintQueueGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `AdvancedPrintQueueGateway` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=AdvancedPrintQueueGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused queue gateway test passed: 1 test.
- `gui -am` reactor passed: 225 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 16

Completed another narrow Open Loads analyzer SRP slice for query execution ownership.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryRepository.java`
  - Added a package-private repository that owns Open Loads SQL execution against an injected `DataSource`.
  - Uses the existing `OpenLoadsSql` and `OpenLoadsRowMapper` helpers.
  - Owns JDBC connection, statement, and result-set resource closure.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryService.java`
  - Reduced `fetchRows` to app datasource creation, repository delegation, and Hikari lifecycle closure.
  - Removed inline query execution and row accumulation from the service.
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryRepositoryTest.java`
  - Added a focused JDBC proxy test proving SQL preparation, row mapping, and resource closure.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=OpenLoadsQueryRepositoryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `OpenLoadsQueryRepository` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=OpenLoadsQueryRepositoryTest,OpenLoadsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused Open Loads repository/service tests passed: 4 tests.
- `gui -am` reactor passed: 226 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-19 Progress Note 17

Completed another narrow Open Loads analyzer SRP slice for datasource lifecycle ownership.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsDataSourceSupport.java`
  - Added a package-private lifecycle helper for creating the app datasource and closing Hikari datasources after query execution.
  - Provides a small injected `DataSourceProvider` seam for focused tests without opening a real DB connection.
  - Runs query work through a `QueryOperation` callback so JDBC execution remains owned by `OpenLoadsQueryRepository`.
- `gui/src/main/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsQueryService.java`
  - Removed direct `DataSourceFactory` and `HikariDataSource` handling.
  - Delegates datasource lifecycle to `OpenLoadsDataSourceSupport` and keeps analyzer-facing coordination.
- `gui/src/test/java/com/tbg/wms/cli/gui/analyzers/openloads/OpenLoadsDataSourceSupportTest.java`
  - Added a focused test proving datasource creation, operation execution, and Hikari closure.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=OpenLoadsDataSourceSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `OpenLoadsDataSourceSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=OpenLoadsDataSourceSupportTest,OpenLoadsQueryRepositoryTest,OpenLoadsQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused Open Loads datasource/repository/service tests passed: 5 tests.
- `gui -am` reactor passed: 227 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-23 Progress Note 18

Completed another narrow `LabelGuiFrame` SRP slice for printer-load completion planning.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/FramePrinterSelectionSupport.java`
  - Added `LoadedPrinterPlan` and `planLoadedPrinters`.
  - Centralizes initial printer combo selection index resolution and load-status messaging after printer discovery.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - `loadPrintersAsync` now delegates printer-load completion decisions to `FramePrinterSelectionSupport`.
  - The frame still owns combo-box mutation, sizing, and last-valid selection state.
- `gui/src/test/java/com/tbg/wms/cli/gui/FramePrinterSelectionSupportTest.java`
  - Added a focused test for loaded-printer selection and status planning.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=FramePrinterSelectionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `LoadedPrinterPlan` and `planLoadedPrinters` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=FramePrinterSelectionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused frame printer selection support test passed: 6 tests.
- `gui -am` reactor passed: 228 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## 2026-06-23 Progress Note 19

Completed another narrow `LabelGuiFrame` SRP slice for analyzer-dialog open policy.

Files intentionally changed:

- `gui/src/main/java/com/tbg/wms/cli/gui/GuiAnalyzerDialogOpenSupport.java`
  - Added a pure policy helper for developer-mode gating and analyzer dialog reuse decisions.
  - Encodes blocked, create-and-show, and reuse-and-show outcomes without constructing Swing dialogs.
- `gui/src/main/java/com/tbg/wms/cli/gui/LabelGuiFrame.java`
  - `openAnalyzersDialog` now delegates developer-mode and reusable-dialog decisions to `GuiAnalyzerDialogOpenSupport`.
  - The frame still owns `AnalyzerDialog` construction and visibility.
- `gui/src/test/java/com/tbg/wms/cli/gui/GuiAnalyzerDialogOpenSupportTest.java`
  - Added focused tests for disabled developer mode, enabled mode without a reusable dialog, and enabled mode with a reusable dialog.

TDD evidence:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiAnalyzerDialogOpenSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Red result before implementation:

- Compilation failed because `GuiAnalyzerDialogOpenSupport` did not exist.

Green/final verification:

```powershell
.\mvnw.cmd -pl gui -am "-Dtest=GuiAnalyzerDialogOpenSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am "-Dtest=GuiAnalyzerDialogOpenSupportTest,LabelGuiFrameStartupTest,LabelGuiFrameToolMenuSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl gui -am test
```

Results:

- Focused analyzer dialog open support test passed: 3 tests.
- Focused frame shell/tool menu tests passed: 7 tests.
- `gui -am` reactor passed: 231 tests.

Known recurring test-output noise remains unchanged:

- Local PowerShell profile execution-policy warning.
- Log4j2 simple logger fallback warning.
- ByteBuddy dynamic agent / JVM class sharing warnings.

## Suggested Resume Command

```powershell
cd C:\Users\zrashed\Documents\Code\wms-pallet-tag-system
git status --short
.\mvnw.cmd -pl gui -am "-Dtest=GuiAnalyzerDialogOpenSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Then choose the next pass from the list above and follow TDD for any behavior changes.
