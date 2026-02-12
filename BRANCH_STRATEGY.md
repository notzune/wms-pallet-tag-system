# Git Branch Strategy

## Branch Hierarchy

```
main (origin/main)
  - Stable production branch
  - Only receives PRs from dev after testing complete
  - Current commit: feat: add ZPL template engine and DB analysis tools

dev (development)
  - Primary development branch
  - All feature development happens here
  - Must pass full build (mvn clean package -DskipTests) before commits
  - Current commit: feat: add ZPL template engine and DB analysis tools
  - Changes staged: ODBC Setup Warehouse Analyzer Access Instructions.pdf

analysis (temporary)
  - Isolated branch for database schema discovery work
  - Kept separate to avoid cluttering main development
  - Contains improved DatabaseAnalysisCli with DbConnectionPool integration
  - Latest commit: refactor(analysis): rebuild DatabaseAnalysisCli to use DbConnectionPool
```

## Branch Usage

### dev - Main Development
- All core application features (models, services, database queries)
- Regular commits for each completed feature or fix
- Integration tests and unit tests must pass
- Ready to be merged to main once sprints are complete

### analysis - Database Discovery (Temporary)
- Isolated work for WMS schema discovery and validation
- Runs multi-phase analysis (Phase 1-4) against WMS database
- Generated dumps and analysis files go in db-dumps/ directory
- Once analysis is complete and findings documented:
  1. Results are documented in db-dumps/ANALYSIS_PLAN.md
  2. Schema mappings are added to dev branch
  3. test-analysis module can be removed
  4. analysis branch can be deleted

## Switching Branches

```bash
# Check current branch
git rev-parse --abbrev-ref HEAD

# List all branches
git branch -a

# Switch to dev (main development)
git checkout dev

# Switch to analysis (for database work)
git checkout analysis

# Create a new feature branch from dev
git checkout -b feature/my-feature
```

## Commit Standards

All commits must follow the format:
```
type(scope): brief description

Detailed explanation (optional)
- Bullet points for changes
- Related to: ISSUE_ID or feature name
```

Types: feat, fix, refactor, test, docs, chore, ci

Example:
```
feat(core): add shipment data model

- Create Shipment class with required fields
- Add ShipmentBuilder for fluent construction
- Add unit tests for Shipment class
- Related to: Database schema mapping for label generation
```

## Build Requirements

Before any commit:
```bash
# Must be on correct branch
git checkout dev  # or feature branch

# Build must succeed
mvn clean package -DskipTests

# If build fails, fix errors before committing
```

## Merging to Main

1. Ensure all dev work is complete and tested
2. Verify build succeeds on dev: `mvn clean package -DskipTests`
3. Create pull request dev -> main
4. Code review
5. Merge when approved
6. Tag release: `git tag v1.0.0`

## Analysis Phase Workflow

1. Switch to analysis branch: `git checkout analysis`
2. Run analysis: `mvn -pl test-analysis exec:java -Dexec.mainClass='com.tbg.wms.analysis.DatabaseAnalysisCli' -Dexec.args='db-dumps all'`
3. Review dumps in db-dumps/ directory
4. Document findings in db-dumps/ANALYSIS_PLAN.md
5. Switch to dev: `git checkout dev`
6. Add findings to schema documentation
7. Delete analysis branch when complete

## Current Status

- Build Status: PASSING (all modules compile successfully)
- Active Branch: dev
- Analysis Branch: Ready for database discovery work
- Next Step: Connect to database and run Phase 1-4 analysis

