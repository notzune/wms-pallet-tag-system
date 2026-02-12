================================================================================
STANDALONE PYTHON WMS DATABASE ANALYSIS TOOL
Copy-Paste Prompt for Laptop Agent
================================================================================

PROJECT CONTEXT
===============

You are working on the WMS Pallet Tag System - a production system for
generating barcode labels for pallet shipments using the Zebra ZPL format.

The Java/Maven-based analysis tooling was removed because Maven/Java are not
available on the work laptop. This Python program replaces it with a standalone
utility that connects directly to the WMS Oracle database and performs schema
discovery.

PROJECT LOCATION
================

C:\Users\zrash\OneDrive\Documents\GitHub\wms-pallet-tag-system

Key files:
  - wms_analysis.py (this analysis tool)
  - INSTRUCTIONS.md (project requirements - READ THIS FIRST)
  - .env (database credentials - ALREADY CONFIGURED)
  - db-dumps/ (where analysis output goes)

PREREQUISITE SETUP (DO ONCE)
=============================

1. Ensure Python 3.8+ is installed:
   python --version

2. Install Oracle Python driver:
   pip install oracledb

3. Verify database credentials exist in .env file:
   cat .env
   
   Should show:
   ORACLE_USERNAME=RPTADM
   ORACLE_PASSWORD=Report_Password12!@#
   SITE_TBG3002_HOST=10.19.68.61
   ORACLE_PORT=1521
   ORACLE_SERVICE=WMSP

UNDERSTANDING WHAT WE NEED FROM THE DATABASE
==============================================

The analysis tool extracts information needed to generate accurate pallet labels.
You need to understand what we're looking for:

ORDER INFORMATION (from shipment tables):
  - Order ID, Shipment ID, or Load ID (how shipments are identified)
  - Ship-To Address (company name, street, city, state, postal code, country)
  - Ship-From (our location)
  - Carrier information (FedEx, YRC, XPO, etc.)
  - Service level (Ground, Express, etc.)
  - Dates (ship date, expected delivery)
  - Walmart-specific fields if applicable

PALLET/LPN INFORMATION (from pallet/LPN tables):
  - LPN (License Plate Number) - unique identifier for each pallet
  - Pallet sequence (1 of 3, 2 of 3, etc.)
  - Number of cases on pallet
  - Total units on pallet
  - Weight and dimensions (if available)

LINE ITEMS (what products are on this LPN):
  - SKU (supplier code)
  - Product description
  - Quantity
  - Unit of Measure (EA, CS, PLT, etc.)
  - Case pack size
  - IMPORTANT: Walmart Item Code (different from SKU!)

STAGING LOCATION (determines printer routing):
  - Location code (ROSSI = Canadian hub, OFFICE = office location, etc.)
  - This is critical for Canadian orders

CANADIAN WALMART ORDERS (special case):
  - Look for ROSSI staging location or SHIP_TO_COUNTRY = 'CA'
  - These have specific Walmart Canada requirements
  - Find complete example of order -> LPN -> items flow

RUNNING THE ANALYSIS
====================

The tool has 4 phases:

PHASE 1 - Schema Discovery (list all tables and columns):
  python wms_analysis.py --phase 1 --output db-dumps

  Output: 01_schema_discovery.txt
  Shows: All tables in database with column names and data types
  Time: ~30 seconds

PHASE 2 - Sample Data (100 rows from each table):
  python wms_analysis.py --phase 2 --output db-dumps

  Output: 01_schema_discovery.txt + 02_sample_data.txt
  Shows: What actual data looks like in each table
  Time: ~2 minutes

PHASE 3 - Targeted Analysis (focus on shipment/order/LPN):
  python wms_analysis.py --phase 3 --output db-dumps

  Output: Phases 1-2 output + 03_shipment_analysis.txt
  Shows: Detailed look at shipment, order, pallet, line item tables
  Time: ~3 minutes

PHASE 4 - All Phases + Canadian Order Analysis (RECOMMENDED):
  python wms_analysis.py --phase 4 --output db-dumps
  (or just: python wms_analysis.py)

  Output: All of above + 04_canadian_orders.txt
  Shows: All analysis + Canadian Walmart order examples
  Time: ~5-10 minutes

EXPECTED OUTPUT
===============

When analysis completes successfully, you will see:

  [INFO] ✓ Connected to 10.19.68.61:1521/WMSP
  [INFO] === PHASE 1: Schema Discovery ===
  [INFO] Schema discovery saved to db-dumps/01_schema_discovery.txt
  [INFO] === PHASE 2: Sample Data Dump ===
  [INFO] Sample data saved to db-dumps/02_sample_data.txt
  ...
  [INFO] ================================================================================
  [INFO] ANALYSIS COMPLETE
  [INFO] ================================================================================
  [INFO] Output files saved to: db-dumps
  [INFO]   - 01_schema_discovery.txt
  [INFO]   - 02_sample_data.txt
  [INFO]   - 03_shipment_analysis.txt
  [INFO]   - 04_canadian_orders.txt

Files will be in: db-dumps/ directory

TROUBLESHOOTING
===============

ERROR: "No module named 'oracledb'"
SOLUTION: pip install oracledb

ERROR: "Failed to connect to database: ORA-12170 TCP timeout"
SOLUTION: 
  - Is VPN connected? (required for internal network)
  - Can you reach host? ping 10.19.68.61
  - Check .env file has correct host, port, credentials

ERROR: "Connection refused"
SOLUTION:
  - Database service might be down
  - Check ORACLE_SERVICE=WMSP is correct
  - Contact DBA to verify service is running

ERROR: "Invalid username/password"
SOLUTION:
  - Edit .env file with correct RPTADM password
  - Verify no extra spaces around credentials

ANALYZING THE OUTPUT FILES
===========================

After running the analysis (Phase 4 recommended):

1. FIRST: Read 01_schema_discovery.txt
   - Look for table names containing: SHIPMENT, ORDER, LOAD, PALLET, LPN, LINE, ITEM
   - These are the tables we need to query
   - Note the actual names (e.g., maybe it's "SHIPMENT_HEADER" not "SHIPMENT")
   - Find which columns are relevant:
     * Order identifier column (ORDER_ID? LOAD_ID? SHIPMENT_ID?)
     * Customer/ship-to columns
     * LPN columns
     * Location/staging columns

2. THEN: Read 03_shipment_analysis.txt
   - Shows actual data from the tables we identified
   - Look for relationships:
     * How does Order link to Pallet?
     * How does Pallet link to Line Items?
     * Where is location/staging stored?
   - Check data quality (are fields populated? formats?)

3. FINALLY: Read 04_canadian_orders.txt
   - Find Canadian Walmart order example
   - Trace the full path: Order -> LPN -> Items
   - Verify all required fields are available

DOCUMENTING YOUR FINDINGS
==========================

As you review the output, document:

  Table Mapping:
    - Shipment/Order table: [actual table name]
    - Pallet/LPN table: [actual table name]
    - Line Item table: [actual table name]
    - Customer table: [actual table name]
    - Location/Staging table: [actual table name]

  Column Mapping:
    - Order Identifier: [table].[column]
    - Ship-To Company: [table].[column]
    - Ship-To Address: [table].[column]
    - LPN: [table].[column]
    - SKU: [table].[column]
    - Walmart Item Code: [table].[column] (if exists)
    - Staging Location: [table].[column]

  Relationships:
    - Order to LPN: [how they join]
    - LPN to Line Items: [how they join]
    - Example: ORDER.ORDER_ID = LPN.ORDER_ID (join like this)

  Canadian Orders:
    - How to identify: [ROSSI location / CA country / other]
    - Example order found: [sample data]

WALMART ITEM CODE SCRAPING
===========================

The tool searches for Walmart Item Codes in columns like:
  - WALMART_ITEM_CODE
  - WALMART_UPC
  - GTIN
  - WMT_ITEM_ID
  - VENDOR_ITEM_ID

Review 03_shipment_analysis.txt and 02_sample_data.txt to see if these exist.

If NOT found in database:
  - We can create a mapping file (CSV format)
  - Or use supplier SKU and Walmart updates labels
  - Or implement API lookup to Walmart catalog

This is OK - Walmart Item Codes are optional, we have workarounds.

NEXT STEPS AFTER ANALYSIS
==========================

Once you have completed the analysis and documented findings:

1. Create a FINDINGS.txt file with your documented table/column mapping
2. Share the findings with main development team
3. Return to dev branch: git checkout dev
4. Move the db-dumps output files somewhere safe for reference
5. Main team will:
   - Update OracleDbQueryRepository with actual SQL queries
   - Implement data model mappings
   - Build label generation logic
   - Create integration tests

DO NOT COMMIT ANALYSIS OUTPUT
==============================

The db-dumps/ directory contains temporary discovery artifacts.
DO NOT commit these to git - they are temporary and large.

DO commit:
  - Your FINDINGS.txt documentation
  - Any mapping files you create

DO NOT commit:
  - db-dumps/*.txt files (temporary discovery data)
  - Any database dumps or sample data files

SCRIPT FEATURES
===============

The wms_analysis.py tool includes:

✓ Configuration from .env file (credentials already there)
✓ Oracle connection with error handling
✓ Phase-based execution (run phase 1, 2, 3, or all 4)
✓ Schema discovery and table enumeration
✓ Sample data extraction from all tables
✓ Targeted analysis of key tables (shipment, pallet, line items)
✓ Canadian order search and analysis
✓ Walmart Item Code detection
✓ Formatted output to text files in db-dumps/

EXPECTED TIMING
===============

From network location with VPN:
  Phase 1 (schema only): 30 seconds - 1 minute
  Phase 2 (+ samples): 1-2 minutes
  Phase 3 (+ targeted): 2-3 minutes
  Phase 4 (complete): 5-10 minutes

From home/remote without VPN:
  Connection will timeout - see TROUBLESHOOTING above

SUCCESS CRITERIA
================

Analysis is successful when:
  ✓ All 4 phases complete without errors
  ✓ Files generated in db-dumps/ directory
  ✓ You identified at least 3 shipment-related tables
  ✓ You identified pallet/LPN table
  ✓ You identified line item table
  ✓ You found customer/location information
  ✓ You traced a complete Canadian Walmart order example
  ✓ All required fields are available in database

Once successful, you are ready for implementation phase:
  - Java team uses findings to build database queries
  - Implement label generation logic
  - Create integration tests with real data

QUICK REFERENCE COMMANDS
========================

# Install dependencies:
pip install oracledb

# Run all phases:
python wms_analysis.py

# Run specific phase:
python wms_analysis.py --phase 1
python wms_analysis.py --phase 2
python wms_analysis.py --phase 3
python wms_analysis.py --phase 4

# Specify custom output directory:
python wms_analysis.py --output my-analysis/

# View schema discovery:
cat db-dumps/01_schema_discovery.txt

# View sample data:
cat db-dumps/02_sample_data.txt

# View shipment/order analysis:
cat db-dumps/03_shipment_analysis.txt

# View Canadian orders:
cat db-dumps/04_canadian_orders.txt

# Switch back to main development:
git checkout dev

REFERENCE DOCUMENTATION
=======================

Important files to review:
  - INSTRUCTIONS.md (overall system requirements)
  - README.md (project overview)
  - CHANGELOG.md (what's been implemented)

The analysis tool was designed following these principles from INSTRUCTIONS.md:
  - Explicit and conservative with assumptions
  - Clean interface boundaries
  - TODO markers for unknown schema details
  - Comprehensive logging and diagnostics
  - Proper error handling and remediation hints

================================================================================
END OF PROMPT - Good luck with the analysis!
================================================================================

