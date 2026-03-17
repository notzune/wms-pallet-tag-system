# PR Draft: v1.7.1

## Title

`release: prepare v1.7.1`

## Summary

- ship the `1.7.1` installer/update flow and packaged-install maintenance path
- finish GUI/CLI workflow consistency work for selected-label printing and info-tag control
- harden runtime operations with configurable `out/` cleanup, advanced non-secret config editing, and checksum-verified guided upgrades
- complete the review/refactor pass by extracting settings/task-planning helpers and fixing duplicate-LPN shipment hydration

## Highlights

- Added per-user installer/app-image packaging while keeping the portable ZIP/manual install path.
- Added GUI update checks, notification badge, guided installer download, checksum verification, and clean uninstall / clean-install prep flow.
- Added configurable stale `out/` cleanup plus advanced runtime config editing for non-secret files under `config/`.
- Unified selected-label shipment printing across GUI and CLI, then extended the same preview subset behavior to carrier moves.
- Moved printer workflow scoping to config-driven `capabilities`.
- Upgraded the project baseline to Java 17.
- Reduced SRP/DRY pressure by extracting `MainSettingsDialog`, `LabelingSupport`, `PrintTaskPlanner`, and `ArtifactNameSupport`.
- Fixed Oracle shipment hydration so duplicate pallet rows from mixed inventory-detail joins do not create duplicate labels.

## Testing

```powershell
.\mvnw.cmd -q -pl core,db,gui,cli -am -DskipTests compile
.\mvnw.cmd -q -pl db,gui -am "-Dtest=OracleDbQueryRepositoryTest,AdvancedPrintWorkflowServiceTest,LabelWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -q -pl core,gui,cli -am "-Dtest=VersionSupportTest,AdvancedPrintWorkflowServiceTest,LabelWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -q -pl cli -am "-Dmaven.test.skip=true" package
powershell -ExecutionPolicy Bypass -File .\scripts\build-jpackage-bundle.ps1 -InstallerType exe
```

## Notes

- Do not push until final review/approval.
- `#12`, `#11`, `#13`, `#9`, `#10`, `#14`, `#18`, `#16`, `#17`, `#19`, `#20`, `#21`, and the follow-up review cleanup work are all present locally but not yet pushed.
