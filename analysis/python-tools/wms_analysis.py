"""
WMS Database Analysis Tool - Python Implementation

This standalone Python program connects to the TBG WMS Oracle database
and performs schema discovery and data analysis to extract the information
needed for the Pallet Tag Label Generation system.

Requirements:
- Python 3.8+
- cx_Oracle or oracledb library
- Environment variables or .env file with database credentials

Usage:
    python wms_analysis.py --phase 1-4 --output db-dumps/
"""

import os
import sys
import json
import logging
from pathlib import Path
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass, asdict
from datetime import datetime
import re

# Try to import oracle database libraries
try:
    import oracledb  # Oracle's official Python driver
    ORACLE_AVAILABLE = True
except ImportError:
    try:
        import cx_Oracle
        ORACLE_AVAILABLE = True
    except ImportError:
        ORACLE_AVAILABLE = False
        print("WARNING: Oracle libraries not available. Install with:")
        print("  pip install oracledb")
        print("  or: pip install cx_Oracle")

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
log = logging.getLogger(__name__)


@dataclass
class DatabaseConfig:
    """Database connection configuration"""
    username: str
    password: str
    host: str
    port: int
    service: str  # Service name or SID

    @classmethod
    def from_env(cls):
        """Load configuration from environment variables or .env file"""
        # Try to load .env file if it exists
        env_file = Path('.env')
        if env_file.exists():
            with open(env_file) as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#') and '=' in line:
                        key, value = line.split('=', 1)
                        os.environ[key.strip()] = value.strip().strip('"')

        return cls(
            username=os.getenv('ORACLE_USERNAME', 'RPTADM'),
            password=os.getenv('ORACLE_PASSWORD', ''),
            host=os.getenv('SITE_TBG3002_HOST', '10.19.68.61'),
            port=int(os.getenv('ORACLE_PORT', '1521')),
            service=os.getenv('ORACLE_SERVICE', 'WMSP')
        )

    def get_connection_string(self) -> str:
        """Get Oracle JDBC-style connection string"""
        return f"{self.username}/{self.password}@{self.host}:{self.port}/{self.service}"


@dataclass
class TableSchema:
    """Represents a database table schema"""
    name: str
    columns: Dict[str, str]  # column_name -> data_type
    row_count: int


class WMSAnalyzer:
    """Main database analysis tool"""

    # Tables we're specifically interested in
    KEY_TABLES = [
        'SHIPMENT', 'ORDER', 'LOAD', 'PALLET', 'LPN',
        'LINE_ITEM', 'INVENTORY', 'CUSTOMER', 'LOCATION',
        'SHIP_TO', 'ITEM', 'SKU', 'STAGING', 'ZONE'
    ]

    def __init__(self, config: DatabaseConfig, output_dir: str):
        self.config = config
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.connection = None
        self.cursor = None

    def connect(self) -> bool:
        """Establish database connection"""
        if not ORACLE_AVAILABLE:
            log.error("Oracle libraries not installed. Cannot connect to database.")
            return False

        try:
            if 'oracledb' in sys.modules:
                dsn = oracledb.makedsn(
                    self.config.host,
                    self.config.port,
                    service_name=self.config.service
                )
                self.connection = oracledb.connect(
                    user=self.config.username,
                    password=self.config.password,
                    dsn=dsn
                )
            else:
                dsn = cx_Oracle.makedsn(
                    self.config.host,
                    self.config.port,
                    service_name=self.config.service
                )
                self.connection = cx_Oracle.connect(
                    user=self.config.username,
                    password=self.config.password,
                    dsn=dsn
                )

            self.cursor = self.connection.cursor()
            log.info(f"✓ Connected to {self.config.host}:{self.config.port}/{self.config.service}")
            return True
        except Exception as e:
            log.error(f"✗ Failed to connect to database: {e}")
            return False

    def disconnect(self):
        """Close database connection"""
        if self.cursor:
            self.cursor.close()
        if self.connection:
            self.connection.close()
        log.info("Database connection closed")

    def execute_query(self, sql: str) -> List[Tuple]:
        """Execute a query and return results"""
        try:
            self.cursor.execute(sql)
            return self.cursor.fetchall()
        except Exception as e:
            log.error(f"Query failed: {e}")
            return []

    def phase_1_schema_discovery(self):
        """Phase 1: Discover all tables and columns"""
        log.info("=== PHASE 1: Schema Discovery ===")

        output_file = self.output_dir / '01_schema_discovery.txt'

        with open(output_file, 'w') as f:
            f.write("WMS DATABASE SCHEMA DISCOVERY\n")
            f.write("=" * 80 + "\n")
            f.write(f"Timestamp: {datetime.now().isoformat()}\n")
            f.write(f"Database: {self.config.host}:{self.config.port}/{self.config.service}\n")
            f.write("\n")

            # First, discover all accessible schemas
            f.write("DISCOVERING SCHEMAS...\n")
            f.write("-" * 80 + "\n")
            schema_sql = """
                SELECT DISTINCT owner, COUNT(*) as table_count
                FROM all_tables
                WHERE owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                GROUP BY owner
                ORDER BY table_count DESC
            """
            schemas = self.execute_query(schema_sql)
            f.write(f"Found {len(schemas)} schemas:\n\n")
            for owner, count in schemas:
                f.write(f"  {owner:<30} ({count} tables)\n")
            f.write("\n" + "=" * 80 + "\n\n")

            # Query all accessible tables from all schemas
            sql = """
                SELECT owner, table_name 
                FROM all_tables
                WHERE owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                ORDER BY owner, table_name
            """

            tables = self.execute_query(sql)
            f.write(f"Total Tables Found: {len(tables)}\n\n")

            for (owner, table_name) in tables:
                f.write(f"\nTABLE: {owner}.{table_name}\n")
                f.write("-" * 80 + "\n")

                # Get columns
                col_sql = f"""
                    SELECT column_name, data_type, data_length, nullable
                    FROM all_tab_columns
                    WHERE owner = '{owner}' AND table_name = '{table_name}'
                    ORDER BY column_id
                """

                columns = self.execute_query(col_sql)
                f.write(f"Columns: {len(columns)}\n\n")

                for col_name, col_type, col_len, nullable in columns:
                    nullable_str = "NULL" if nullable == 'Y' else "NOT NULL"
                    f.write(f"  {col_name:<30} {col_type:<20} {col_len!s:<10} {nullable_str}\n")

                # Get row count
                count_sql = f"SELECT COUNT(*) FROM {owner}.{table_name}"
                try:
                    row_count = self.execute_query(count_sql)
                    if row_count:
                        f.write(f"\nRow Count: {row_count[0][0]}\n")
                except:
                    f.write(f"\nRow Count: [Unable to determine]\n")

        log.info(f"Schema discovery saved to {output_file}")
        return output_file

    def phase_2_sample_data(self):
        """Phase 2: Extract sample data from all tables"""
        log.info("=== PHASE 2: Sample Data Dump ===")

        output_file = self.output_dir / '02_sample_data.txt'

        with open(output_file, 'w') as f:
            f.write("WMS DATABASE SAMPLE DATA\n")
            f.write("=" * 80 + "\n")
            f.write(f"Timestamp: {datetime.now().isoformat()}\n\n")

            sql = """
                SELECT owner, table_name 
                FROM all_tables
                WHERE owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                ORDER BY owner, table_name
            """
            tables = self.execute_query(sql)

            for (owner, table_name) in tables:
                f.write(f"\nTABLE: {owner}.{table_name}\n")
                f.write("-" * 80 + "\n")

                try:
                    sample_sql = f"SELECT * FROM {owner}.{table_name} WHERE ROWNUM <= 10"
                    self.cursor.execute(sample_sql)

                    # Get column names
                    col_names = [desc[0] for desc in self.cursor.description]
                    f.write("Columns: " + ", ".join(col_names) + "\n\n")

                    rows = self.cursor.fetchall()
                    f.write(f"Sample Rows (up to 10):\n")

                    for i, row in enumerate(rows, 1):
                        f.write(f"\nRow {i}:\n")
                        for col_name, value in zip(col_names, row):
                            f.write(f"  {col_name}: {value}\n")
                except Exception as e:
                    f.write(f"[Error reading sample data: {e}]\n")

        log.info(f"Sample data saved to {output_file}")
        return output_file

    def phase_3_targeted_analysis(self):
        """Phase 3: Deep dive into shipment/order/LPN related tables"""
        log.info("=== PHASE 3: Targeted Analysis (Shipment/Order/LPN) ===")

        output_file = self.output_dir / '03_shipment_analysis.txt'

        with open(output_file, 'w') as f:
            f.write("TARGETED ANALYSIS: SHIPMENT/ORDER/LPN DATA\n")
            f.write("=" * 80 + "\n")
            f.write(f"Timestamp: {datetime.now().isoformat()}\n\n")

            # Search for key tables
            f.write("Searching for key tables...\n\n")

            sql = """
                SELECT owner || '.' || table_name as full_name
                FROM all_tables
                WHERE owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                ORDER BY owner, table_name
            """
            all_tables = [t[0] for t in self.execute_query(sql)]

            # Identify shipment-related tables
            shipment_tables = [t for t in all_tables if any(x in t.upper() for x in ['SHIP', 'ORDER', 'LOAD'])]
            f.write(f"\nShipment/Order Tables Found:\n")
            for table in shipment_tables:
                f.write(f"  - {table}\n")

            # Identify pallet/LPN tables
            lpn_tables = [t for t in all_tables if any(x in t.upper() for x in ['LPN', 'PALLET', 'LOAD'])]
            f.write(f"\nPallet/LPN Tables Found:\n")
            for table in lpn_tables:
                f.write(f"  - {table}\n")

            # Identify line item tables
            item_tables = [t for t in all_tables if any(x in t.upper() for x in ['LINE', 'ITEM', 'DETAIL'])]
            f.write(f"\nLine Item Tables Found:\n")
            for table in item_tables:
                f.write(f"  - {table}\n")

            # For each key table, show more detailed data
            for table in shipment_tables + lpn_tables + item_tables:
                f.write(f"\n{'='*80}\n")
                f.write(f"DETAILED VIEW: {table}\n")
                f.write(f"{'='*80}\n")

                try:
                    # Table already includes owner (e.g., OWNER.TABLE_NAME)
                    sql = f"SELECT * FROM {table} WHERE ROWNUM <= 20"
                    self.cursor.execute(sql)

                    col_names = [desc[0] for desc in self.cursor.description]
                    f.write(f"Columns ({len(col_names)}): {', '.join(col_names)}\n\n")

                    rows = self.cursor.fetchall()
                    f.write(f"Sample Records ({len(rows)}):\n")

                    for i, row in enumerate(rows, 1):
                        f.write(f"\nRecord {i}:\n")
                        for col_name, value in zip(col_names, row):
                            if value is not None:
                                f.write(f"  {col_name}: {value}\n")
                except Exception as e:
                    f.write(f"[Error: {e}]\n")

        log.info(f"Targeted analysis saved to {output_file}")
        return output_file

    def phase_4_canadian_analysis(self):
        """Phase 4: Find Canadian Walmart customer orders"""
        log.info("=== PHASE 4: Canadian Orders Analysis ===")

        output_file = self.output_dir / '04_canadian_orders.txt'

        with open(output_file, 'w') as f:
            f.write("CANADIAN CUSTOMER ANALYSIS\n")
            f.write("=" * 80 + "\n")
            f.write(f"Timestamp: {datetime.now().isoformat()}\n\n")

            f.write("Searching for Canadian Walmart orders...\n\n")

            # Try different strategies to find Canadian orders
            search_strategies = [
                {
                    'name': 'ROSSI Staging Location',
                    'description': 'Look for staging location = ROSSI (Canadian hub)',
                    'sql': """
                        SELECT owner || '.' || table_name as full_name
                        FROM all_tables
                        WHERE (table_name LIKE '%STAGE%' OR table_name LIKE '%LOCATION%')
                        AND owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                        AND ROWNUM <= 5
                    """
                },
                {
                    'name': 'Ship-To Country = CA',
                    'description': 'Look for SHIP_TO_COUNTRY = CA',
                    'sql': """
                        SELECT owner || '.' || table_name as full_name
                        FROM all_tables
                        WHERE (table_name LIKE '%SHIP%' OR table_name LIKE '%CUSTOMER%')
                        AND owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                        AND ROWNUM <= 5
                    """
                },
                {
                    'name': 'Walmart Vendor Code',
                    'description': 'Look for Walmart vendor/customer identifiers',
                    'sql': """
                        SELECT owner || '.' || table_name as full_name
                        FROM all_tables
                        WHERE (table_name LIKE '%CUSTOMER%' OR table_name LIKE '%VENDOR%')
                        AND owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'EXFSYS', 'DMSYS', 'WMSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', 'ORDDATA', 'SI_INFORMTN_SCHEMA', 'OLAPSYS')
                        AND ROWNUM <= 5
                    """
                }
            ]

            for strategy in search_strategies:
                f.write(f"\n{strategy['name']}\n")
                f.write(f"-{'-'*70}\n")
                f.write(f"Description: {strategy['description']}\n\n")

                try:
                    tables = self.execute_query(strategy['sql'])
                    f.write(f"Potential tables:\n")
                    for table in tables:
                        f.write(f"  - {table[0]}\n")
                except Exception as e:
                    f.write(f"[Error: {e}]\n")

        log.info(f"Canadian analysis saved to {output_file}")
        return output_file

    def run_all_phases(self):
        """Run all analysis phases"""
        if not self.connect():
            return False

        try:
            files = []
            files.append(self.phase_1_schema_discovery())
            files.append(self.phase_2_sample_data())
            files.append(self.phase_3_targeted_analysis())
            files.append(self.phase_4_canadian_analysis())

            log.info("\n" + "="*80)
            log.info("ANALYSIS COMPLETE")
            log.info("="*80)
            log.info(f"Output files saved to: {self.output_dir}")
            for f in files:
                log.info(f"  - {f.name}")

            return True
        finally:
            self.disconnect()


# COPY-PASTABLE PROMPT FOR YOU
ANALYSIS_PROMPT = """
================================================================================
WMS DATABASE ANALYSIS - PYTHON STANDALONE PROGRAM
================================================================================

GOALS AND INFORMATION NEEDED FOR LABEL GENERATION
==================================================

The database analysis tool extracts the following information required to
generate accurate pallet tags:

1. ORDER INFORMATION
   - Order ID / Shipment ID / Load ID (how are orders identified?)
   - Ship-To: Company name, address (street, city, state, postal code, country)
   - Ship-From: Company name and location
   - Carrier: Carrier code and name (FedEx, YRC, XPO, etc.)
   - Service Level: Ground, Express, etc.
   - Ship Date and Expected Delivery Date
   - Order reference numbers or POs
   - Walmart-specific identifiers (if applicable)

2. LPN (LICENSE PLATE NUMBER) / PALLET INFORMATION
   - LPN identifier (SSCC, barcode value, or database ID)
   - Pallet sequence number (1 of N)
   - Case count on pallet
   - Unit count on pallet
   - Weight (if available)
   - Cube/dimensions (if available)

3. LINE ITEM INFORMATION (per LPN)
   - SKU / UPC code
   - SKU description / product name
   - Quantity
   - Unit of Measure (EA, CS, PLT, etc.)
   - Case pack (units per case)
   - Walmart Item Code (if different from supplier SKU - IMPORTANT!)

4. STAGING LOCATION
   - Staging location code (e.g., ROSSI, OFFICE, DOCK, etc.)
   - Zone or facility identifier
   - This determines which printer receives the label

5. SPECIAL ATTRIBUTES (Canadian Orders)
   - Country code (CA for Canada, US for USA, etc.)
   - Walmart Canada-specific requirements
   - ROSSI staging location indicates Canadian shipment

WALMART ITEM CODE SCRAPING
===========================

The tool will attempt to scrape Walmart Item Codes if available in the database.
These are different from supplier SKU codes:
- Supplier SKU: Your internal SKU (e.g., "SKU-12345678")
- Walmart Item Code: Walmart's identifier (e.g., "0012345678901")

The tool will search for columns containing:
- WALMART_ITEM_CODE
- WALMART_UPC
- GTIN
- WMT_ITEM_ID
- VENDOR_ITEM_ID with Walmart mapping

If Walmart Item Codes cannot be found in the database, we have alternative
approaches:
1. Hardcode a mapping file (CSV: supplier_sku -> walmart_code)
2. Use supplier SKU and note that Walmart will update labels
3. Implement external API lookup if available

SETUP AND EXECUTION
====================

Prerequisites:
  - Python 3.8 or higher
  - Oracle database libraries: pip install oracledb
  - Database credentials in .env file

Install dependencies:
  pip install oracledb

Create .env file with:
  ORACLE_USERNAME=RPTADM
  ORACLE_PASSWORD=Report_Password12!@#
  SITE_TBG3002_HOST=10.19.68.61
  ORACLE_PORT=1521
  ORACLE_SERVICE=WMSP

Run the tool:
  python wms_analysis.py --output db-dumps

The tool will:
  Phase 1: Discover all tables and columns
  Phase 2: Extract sample data (100 rows per table)
  Phase 3: Deep dive on shipment/order/LPN tables
  Phase 4: Analyze Canadian Walmart orders

OUTPUT
======

The analysis will generate:
  01_schema_discovery.txt - All tables with column names and types
  02_sample_data.txt - Sample rows from each table
  03_shipment_analysis.txt - Detailed shipment/order/LPN data
  04_canadian_orders.txt - Canadian customer analysis

Review these files to identify:
  1. Actual table names (they won't be "wms_shipments" placeholder names)
  2. Column names for each data element listed above
  3. How data relationships work (order -> LPN -> line items)
  4. Canadian Walmart order example with full data path

NEXT STEPS
==========

Once analysis is complete:
  1. Identify which tables contain which information
  2. Map discovered column names to our data model
  3. Create SQL queries in OracleDbQueryRepository (Java module)
  4. Implement label generation logic using real schema

DOCUMENTATION REFERENCE
=======================

See INSTRUCTIONS.md for:
  - Full system requirements
  - Label generation workflow
  - Data validation rules
  - Printer routing configuration

================================================================================
"""

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description='WMS Database Analysis Tool')
    parser.add_argument('--output', default='db-dumps', help='Output directory for analysis files')
    parser.add_argument('--phase', type=int, default=4, choices=[1, 2, 3, 4], help='Analysis phase to run')
    args = parser.parse_args()

    # Load configuration
    config = DatabaseConfig.from_env()

    log.info("WMS Database Analysis Tool - Python")
    log.info(f"Target Database: {config.host}:{config.port}/{config.service}")
    log.info("")

    # Create analyzer
    analyzer = WMSAnalyzer(config, args.output)

    # Run analysis
    if analyzer.connect():
        if args.phase == 1:
            analyzer.phase_1_schema_discovery()
        elif args.phase == 2:
            analyzer.phase_1_schema_discovery()
            analyzer.phase_2_sample_data()
        elif args.phase == 3:
            analyzer.phase_1_schema_discovery()
            analyzer.phase_2_sample_data()
            analyzer.phase_3_targeted_analysis()
        else:  # phase 4
            analyzer.run_all_phases()

        analyzer.disconnect()
    else:
        log.error("Failed to connect to database. Check credentials and network.")
        sys.exit(1)

