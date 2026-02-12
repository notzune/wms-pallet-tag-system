#!/bin/bash
# WMS Database Analysis Script
# Run from WSL terminal in project root

echo "=========================================="
echo "WMS Database Analysis - Start"
echo "=========================================="
echo ""

# Verify we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "ERROR: pom.xml not found. Please run from project root."
    echo "Current directory: $(pwd)"
    exit 1
fi

echo "Project location verified: $(pwd)"
echo ""

# Check prerequisites
echo "Checking prerequisites..."
echo ""

if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found"
    echo "Install: sudo apt-get install default-jdk"
    exit 1
fi
echo "✓ Java: $(java -version 2>&1 | head -1)"

if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found"
    echo "Install: sudo apt-get install maven"
    exit 1
fi
echo "✓ Maven: $(mvn -v | head -1)"

# Check .env
if [ ! -f ".env" ]; then
    echo ""
    echo "WARNING: .env file not found"
    echo "Create .env with database credentials:"
    echo "  ACTIVE_SITE=TBG3002"
    echo "  WMS_ENV=QA"
    echo "  ORACLE_USERNAME=username"
    echo "  ORACLE_PASSWORD=password"
    echo "  ORACLE_HOST=host"
    echo "  ORACLE_PORT=1521"
    echo "  ORACLE_SERVICE=service"
    exit 1
fi
echo "✓ .env file found"

echo ""
echo "Building project..."
echo "=========================================="
mvn clean install -DskipTests=true

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Build failed"
    exit 1
fi

echo ""
echo "Build successful!"
echo ""
echo "Running database analysis..."
echo "=========================================="
echo ""

cd test-analysis

echo "Phase 1: Schema Discovery"
echo "-----------"
mvn exec:java \
  -Dexec.mainClass="com.tbg.wms.analysis.DatabaseAnalysisCli" \
  -Dexec.args="db-dumps 1"

echo ""
echo "=========================================="
echo "Analysis output in: ../db-dumps/"
echo "=========================================="
echo ""
echo "Next: Review db-dumps/01_schema_discovery.txt"
echo "  head -500 db-dumps/01_schema_discovery.txt"
echo ""

