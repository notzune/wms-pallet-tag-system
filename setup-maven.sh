#!/bin/bash
# Maven Setup for WMS Pallet Tag System
# This script will add Maven to your PATH

# Check if Maven is already installed
if command -v mvn &> /dev/null; then
    echo "Maven is already in PATH:"
    mvn -v
    exit 0
fi

# Try using Maven wrapper if it exists
if [ -f "./mvnw" ]; then
    echo "Using Maven wrapper in project..."
    ./mvnw -v
    exit 0
fi

# For Windows PowerShell - instructions
echo "Maven not found in PATH. To install Maven on Windows:"
echo ""
echo "Option 1: Using Chocolatey (if installed)"
echo "  choco install maven"
echo ""
echo "Option 2: Manual installation"
echo "  1. Download: https://maven.apache.org/download.cgi"
echo "  2. Extract to: C:\Program Files\apache-maven-3.9.6"
echo "  3. Add to PATH: C:\Program Files\apache-maven-3.9.6\bin"
echo "  4. Verify: mvn -v"
echo ""
echo "Option 3: Using Windows Package Manager (winget)"
echo "  winget install ApacheMaven.Maven"
echo ""
exit 1

