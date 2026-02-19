"""
auth_test.py

Purpose:
- Bulletproof Oracle auth test for your repo.
- Loads .env reliably (tries multiple likely locations).
- Prints exactly where it's looking + whether it found .env.
- Verifies ORACLE_USERNAME / ORACLE_PASSWORD / ORACLE_DSN exist.
- Attempts connect + prints DB_NAME + SERVICE_NAME.

Usage (from anywhere):
  python analysis/python-tools/auth_test.py

Expected .env keys (recommended in repo root):
  ORACLE_USERNAME=RPTADM
  ORACLE_PASSWORD=...
  ORACLE_DSN=(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.19.96.121)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=jcnwmsdbd01)))
"""

from __future__ import annotations

import os
import sys
from pathlib import Path
from typing import Optional

try:
    import oracledb  # type: ignore
except ImportError:
    print("Missing dependency: oracledb. Install with: pip install oracledb")
    raise SystemExit(1)


def load_dotenv(path: Path) -> bool:
    """
    Load KEY=VALUE lines from a .env file into os.environ (only if key not already set).

    Returns:
        True if the file existed and was processed, else False.
    """
    print(f"\nTrying .env: {path}")
    print(f"Resolved : {path.resolve()}")

    if not path.exists():
        print("Result   : NOT FOUND")
        return False

    print("Result   : FOUND ✅ (loading)")

    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        k, v = line.split("=", 1)
        key = k.strip()
        val = v.strip().strip('"').strip("'")

        # Only set if not already set in environment (so CLI/env overrides win)
        os.environ.setdefault(key, val)

    return True


def find_and_load_env() -> Optional[Path]:
    """
    Try common .env locations relative to this file.

    Returns:
        The path that was loaded, or None if none were found.
    """
    here = Path(__file__).resolve()
    print("This file:", here)
    print("Parents (top 6):")
    for i, p in enumerate(here.parents[:6]):
        print(f"  [{i}] {p}")

    guesses = [
        here.parent / ".env",          # analysis/python-tools/.env
        here.parent.parent / ".env",   # analysis/.env
        here.parents[2] / ".env",      # repo root (most likely for you)
        here.parents[3] / ".env",      # one higher, just in case
    ]

    for g in guesses:
        if load_dotenv(g):
            return g

    return None


def mask(s: Optional[str]) -> str:
    """Mask a secret for console display."""
    if not s:
        return "(empty)"
    return "*" * len(s)


def main() -> int:
    loaded_path = find_and_load_env()
    print("\nLoaded .env:", str(loaded_path) if loaded_path else "None")

    dsn = os.getenv("ORACLE_DSN", "").strip()
    user = os.getenv("ORACLE_USERNAME", "").strip()
    pw = os.getenv("ORACLE_PASSWORD", "")

    print("\nEffective config (sanity check):")
    print("  ORACLE_USERNAME:", repr(user) if user else "(missing)")
    print("  ORACLE_PASSWORD:", f"len={len(pw)} value={mask(pw)}")
    print("  ORACLE_DSN     :", "present ✅" if dsn else "(missing)")

    if not user or not pw or not dsn:
        print("\nERROR: Missing required credentials.")
        print("Fix: create a .env with these keys (recommended in repo root):")
        print("  ORACLE_USERNAME=RPTADM")
        print("  ORACLE_PASSWORD=...")
        print("  ORACLE_DSN=(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.19.96.121)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=jcnwmsdbd01)))")
        return 2

    print("\nAttempting connection...")
    try:
        conn = oracledb.connect(user=user, password=pw, dsn=dsn)
        cur = conn.cursor()
        cur.execute(
            """
            SELECT
              USER AS LOGGED_IN_USER,
              SYS_CONTEXT('USERENV','DB_NAME') AS DB_NAME,
              SYS_CONTEXT('USERENV','SERVICE_NAME') AS SERVICE_NAME
            FROM dual
            """
        )
        row = cur.fetchone()
        print("Connected ✅")
        print("Result:", row)
        cur.close()
        conn.close()
        return 0
    except oracledb.Error as e:
        print("\nConnection FAILED ❌")

        # oracledb provides a rich error object
        err = e.args[0] if e.args else e
        print("Type:", type(e).__name__)

        # These attributes exist for DatabaseError in oracledb
        for attr in ["code", "message", "offset", "context"]:
            if hasattr(err, attr):
                print(f"{attr}:", getattr(err, attr))

        # Fallback: full string
        print("Full:", str(e))

        return 3


if __name__ == "__main__":
    raise SystemExit(main())
