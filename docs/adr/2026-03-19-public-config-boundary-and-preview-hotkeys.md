# ADR: Public Config Boundary And Preview Hotkeys

**Date:** 2026-03-19
**Status:** Accepted

## Context

The application now supports a Tropicana-only setup flow that installs working database credentials outside the install directory. However, packaged CLI smoke testing showed that public app-image builds still resolved a working bundled `wms-tags.env` and could authenticate successfully against production. The issue was traced to the checked-in example config still containing real credentials.

At the same time, operators use the main label generation and rail labels windows as task-focused workflow screens. These windows already support terminal-style right-click clipboard behavior, but they still require mouse interaction for preview actions.

## Decision

1. Public artifacts must never ship working Tropicana credentials.
2. The checked-in example config must contain only dummy, non-functional values.
3. Tropicana working credentials remain external to the install directory and are supplied only through internal installer/configuration paths.
4. `Ctrl+F` is reserved in the workflow windows to trigger preview actions:
   - main label generation: `Preview`
   - rail labels: `Load Preview`

## Rationale

### Public config boundary

- Public releases must remain safe to download without granting production DB access.
- A template/example file that contains working credentials is equivalent to shipping secrets, even if bundle scripts no longer copy `.env`.
- Keeping Tropicana config external to the install directory preserves configuration across updates and keeps internal configuration distribution separate from public release publishing.

### Preview hotkeys

- These windows behave more like operator consoles than document editors.
- Users spend most of their time in one input field and repeatedly trigger a preview-oriented action.
- `Ctrl+F` is a natural mnemonic for find/fetch/load in this context and is faster than forcing mouse travel.
- Standard text-find behavior is less valuable here because these screens are not general text-editing surfaces.

## Consequences

### Positive

- Public builds no longer expose production DB access by default.
- Tropicana setup remains the only supported way to install working credentials.
- Operators get a consistent keyboard shortcut for preview actions in the two key workflows.

### Negative

- Public smoke tests that previously passed against production DB access must now fail until real config is installed.
- `Ctrl+F` diverges from classic text-search expectations inside these task windows.

## Implementation Notes

- Validate public packaged CLI behavior with `config` and `db-test` smoke checks.
- Bind `Ctrl+F` at the window/root-pane level and reuse the existing button actions instead of duplicating preview logic.
