# Git Commands for Your Laptop

## To Pull Latest Changes

### Simple Command (Recommended)
```bash
git pull origin dev
```

This will fetch and merge all changes from the remote dev branch.

## What You'll Get

- `wms_analysis.py` - Python database analysis tool
- `PYTHON_ANALYSIS_PROMPT.md` - Complete execution guide  
- `PYTHON_ANALYSIS_QUICK_START.md` - Quick reference
- `PYTHON_ANALYSIS_README.md` - Status and overview
- Updated project files (Java analysis removed)

## If You're Not On dev Branch

```bash
git checkout dev
git pull origin dev
```

## After Pulling

You'll be ready to run:

```bash
pip install oracledb
python wms_analysis.py
```

## That's It!

Everything you need is committed and ready to pull.

