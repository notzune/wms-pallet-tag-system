#!/bin/bash
# WMS Database Analysis - Complete Execution Script
# Run this in WSL terminal from project root
# Usage: bash run-analysis.sh

set -e

echo "=========================================="
echo "WMS Database Analysis - Full Execution"
echo "=========================================="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo ""
    echo "Create .env file with:"
    echo "  ACTIVE_SITE=TBG3002"
    echo "  WMS_ENV=QA"
    echo "  ORACLE_USERNAME=your_username"
    echo "  ORACLE_PASSWORD=your_password"
    echo "  ORACLE_HOST=your_host"
    echo "  ORACLE_PORT=1521"
    echo "  ORACLE_SERVICE=your_service"
    echo ""
    exit 1
fi

echo "Prerequisites Check"
echo "==================="
echo ""

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found in PATH"
    echo "Install Maven: apt-get install maven"
    exit 1
fi
echo "✓ Maven: $(mvn -v | head -1)"

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found in PATH"
    exit 1
fi
echo "✓ Java: $(java -version 2>&1 | head -1)"

echo ""
echo "Building Project"
echo "================"
mvn clean install -DskipTests=true 2>&1 | tail -20

echo ""
echo "Phase 1: Schema Discovery"
echo "========================="
echo "Discovering all tables and columns..."
echo ""

cd test-analysis
mvn exec:java \
  -Dexec.mainClass="com.tbg.wms.analysis.DatabaseAnalysisCli" \
  -Dexec.args="db-dumps 1" \
  2>&1

echo ""
echo "Schema discovery complete!"
echo "Output: db-dumps/01_schema_discovery.txt"
echo ""
echo "Review the schema discovery file:"
echo "  cat db-dumps/01_schema_discovery.txt | head -100"
echo ""

# Ask for continuation
read -p "Continue to Phase 2 (Broad Data Dump)? [y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Phase 2: Broad Data Dump"
    echo "======================="
    echo "Sampling 100 rows from each table..."
    echo ""

    mvn exec:java \
      -Dexec.mainClass="com.tbg.wms.analysis.DatabaseAnalysisCli" \
      -Dexec.args="db-dumps 2" \
      2>&1

    echo ""
    echo "Data dump complete!"
    echo "Output files: db-dumps/02_sample_*.txt"
    echo ""
fi

read -p "Continue to Phase 3 (Targeted Analysis)? [y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Phase 3: Targeted Analysis"
    echo "=========================="
    echo "Focusing on shipment/order/customer tables..."
    echo ""

    mvn exec:java \
      -Dexec.mainClass="com.tbg.wms.analysis.DatabaseAnalysisCli" \
      -Dexec.args="db-dumps 3" \
      2>&1

    echo ""
    echo "Targeted analysis complete!"
    echo "Output files: db-dumps/03_full_*.txt"
    echo ""
fi

read -p "Continue to Phase 4 (Canadian Orders Deep Dive)? [y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Phase 4: Canadian Orders Analysis"
    echo "=================================="
    echo "Searching for Canadian orders..."
    echo ""

    mvn exec:java \
      -Dexec.mainClass="com.tbg.wms.analysis.DatabaseAnalysisCli" \
      -Dexec.args="db-dumps 4" \
      2>&1

    echo ""
    echo "Canadian analysis complete!"
    echo "Output files: db-dumps/04_canadian_*.txt or CANADIAN_*.txt"
    echo ""
fi

echo ""
echo "=========================================="
echo "Analysis Complete!"
echo "=========================================="
echo ""
echo "Output Location: db-dumps/"
echo ""
echo "Next Steps:"
echo "1. Review 01_schema_discovery.txt - identify actual table names"
echo "2. Review 02_sample_*.txt - see actual data format"
echo "3. Review 03_full_*.txt - examine shipment/order tables"
echo "4. Review 04_canadian_*.txt - find Canadian order example"
echo ""
echo "Commands to review files:"
echo "  ls -lah db-dumps/"
echo "  head -200 db-dumps/01_schema_discovery.txt"
echo "  grep -i 'canada\|walmart' db-dumps/02_sample_*.txt"
echo ""

