# Strict SRP Refactor Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce GUI coordinator responsibility by extracting ZPL preview document state from the Swing dialog.

**Architecture:** `ZplPreviewToolDialog` should own Swing controls, render scheduling, and async preview rendering. `ZplPreviewDocumentModel` should own document collection state, current index, navigation bounds, edit persistence, and display labels.

**Tech Stack:** Java 17, Swing, Maven, JUnit 5.

---

### Task 1: Characterize ZPL Preview Document State

**Files:**
- Create: `gui/src/test/java/com/tbg/wms/cli/gui/ZplPreviewDocumentModelTest.java`
- Create: `gui/src/main/java/com/tbg/wms/cli/gui/ZplPreviewDocumentModel.java`
- Modify: `gui/src/main/java/com/tbg/wms/cli/gui/ZplPreviewToolDialog.java`
- Modify: `CHANGELOG.md`
- Modify: `docs/architecture-solid-audit.md`

- [x] **Step 1: Write failing model tests**

Cover null filtering, first-document selection, index clamping, current-document edit persistence, and empty-state display metadata.

- [x] **Step 2: Run focused test and verify red**

Run: `.\mvnw.cmd -q -pl gui -am "-Dtest=ZplPreviewDocumentModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: compile failure because `ZplPreviewDocumentModel` does not exist.

- [x] **Step 3: Extract focused model**

Move document list, current index, navigation availability, edit persistence, and display label calculation into `ZplPreviewDocumentModel`.

- [x] **Step 4: Wire dialog to the model**

Keep `ZplPreviewToolDialog` responsible for Swing controls, file loading, render scheduling, and async image rendering.

- [x] **Step 5: Verify focused and GUI tests**

Run:

```powershell
.\mvnw.cmd -q -pl gui -am "-Dtest=ZplPreviewDocumentModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -q -pl gui -am test
```

- [x] **Step 6: Run final verification gate**

Run:

```powershell
.\mvnw.cmd -q -pl core,db,gui,cli -am test
.\mvnw.cmd -q -pl cli -am package -DskipTests
java -jar cli\target\cli-1.8.0-SNAPSHOT.jar --help
```

- [ ] **Step 7: Commit and push branch**

```powershell
git add CHANGELOG.md docs/architecture-solid-audit.md docs/superpowers/plans/2026-06-01-strict-srp-refactor-pass.md gui/src/main/java/com/tbg/wms/cli/gui/ZplPreviewDocumentModel.java gui/src/main/java/com/tbg/wms/cli/gui/ZplPreviewToolDialog.java gui/src/test/java/com/tbg/wms/cli/gui/ZplPreviewDocumentModelTest.java
git commit -m "refactor(gui): isolate zpl preview document state"
git push -u origin refactor/strict-srp-refactor-pass
```
