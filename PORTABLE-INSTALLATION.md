# Portable Installation Guide - WMS Pallet Tag System v1.0.0

**For:** Windows, Linux, macOS  
**Package:** `wms-pallet-tag-system-v1.0.0-portable.zip` (~150MB)  
**Contents:** Application JAR + Java 21 JRE (bundled)

---

## Table of Contents

1. [Quick Install](#quick-install)
2. [Detailed Setup](#detailed-setup)
3. [Configuration](#configuration)
4. [Running the Application](#running-the-application)
5. [Troubleshooting](#troubleshooting)
6. [Uninstall](#uninstall)

---

## Quick Install

### Windows

```batch
# 1. Extract the portable ZIP to a folder (e.g., C:\Program Files\wms-pallet-tag-system)
# Right-click ZIP → "Extract All..." → Choose location

# 2. Copy configuration template
cd wms-pallet-tag-system
copy wms-tags.env.example wms-tags.env

# 3. Edit configuration with your credentials
notepad wms-tags.env
# Set: ORACLE_USERNAME, ORACLE_PASSWORD, ORACLE_SERVICE, etc.

# 4. Run the application
# Option A: Launch GUI
wms-pallet-tag-system.bat

# Option B: Launch CLI
wms-tags.bat config
wms-tags.bat db-test
wms-tags.bat run --shipment-id 8000141715 --dry-run
```

### Linux / macOS

```bash
# 1. Extract the portable TAR to a folder
tar -xzf wms-pallet-tag-system-v1.0.0-portable.tar.gz
cd wms-pallet-tag-system

# 2. Copy configuration template
cp wms-tags.env.example wms-tags.env

# 3. Edit configuration
nano wms-tags.env
# Set: ORACLE_USERNAME, ORACLE_PASSWORD, ORACLE_SERVICE, etc.

# 4. Run the application
# Option A: Launch GUI
./wms-pallet-tag-system.sh

# Option B: Launch CLI
./wms-tags.sh config
./wms-tags.sh db-test
./wms-tags.sh run --shipment-id 8000141715 --dry-run
```

---

## Detailed Setup

### Step 1: Download & Extract

**Windows:**
1. Download `wms-pallet-tag-system-v1.0.0-portable.zip` from GitHub Releases
2. Right-click the ZIP file
3. Select "Extract All..."
4. Choose destination: `C:\Program Files\wms-pallet-tag-system`
5. Click "Extract"

**Linux/macOS:**
```bash
cd ~/Downloads
wget https://github.com/notzune/wms-pallet-tag-system/releases/download/v1.0.0-beta/wms-pallet-tag-system-v1.0.0-portable.tar.gz
tar -xzf wms-pallet-tag-system-v1.0.0-portable.tar.gz
mv wms-pallet-tag-system ~/opt/wms-pallet-tag-system  # or preferred location
cd ~/opt/wms-pallet-tag-system
```

### Step 2: Verify Extraction

Check that the following files/directories exist:

```
wms-pallet-tag-system/
├── jre/                          ← Bundled Java Runtime (no install needed)
├── lib/                          ← Application JAR and dependencies
│   └── cli-1.0.0-SNAPSHOT.jar
├── config/                       ← Configuration templates and data
│   ├── wms-tags.env.example
│   ├── walmart-sku-matrix.csv
│   ├── TBG3002/
│   │   ├── printers.yaml
│   │   └── printer-routing.yaml
│   └── templates/
│       └── walmart-canada-label.zpl
├── wms-tags.bat                  ← Windows CLI launcher
├── wms-tags.sh                   ← Linux/macOS CLI launcher
├── wms-pallet-tag-system.bat     ← Windows GUI launcher
├── wms-pallet-tag-system.sh      ← Linux/macOS GUI launcher
└── README.md
```

### Step 3: Configure Application

**Create configuration file:**

**Windows:**
```batch
cd wms-pallet-tag-system
copy config\wms-tags.env.example wms-tags.env
```

**Linux/macOS:**
```bash
cd wms-pallet-tag-system
cp config/wms-tags.env.example wms-tags.env
```

**Edit the configuration file** with your Oracle WMS credentials and site information:

```env
############################################
# Runtime Mode
############################################
ACTIVE_ENV=PROD
WMS_ENV=PROD

############################################
# Active Site
############################################
ACTIVE_SITE=TBG3002

############################################
# Oracle Connection (JDBC)
############################################
ORACLE_USERNAME=your_wms_username
ORACLE_PASSWORD=your_wms_password
ORACLE_PORT=1521
ORACLE_SERVICE=WMSP
ORACLE_DSN=(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.19.96.121)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=jcnwmsdbd01)))

############################################
# Site Information
############################################
SITE_TBG3002_NAME=Jersey City
SITE_TBG3002_HOST=10.19.68.61

############################################
# Database Pool Settings
############################################
DB_POOL_MAX_SIZE=5
DB_POOL_CONN_TIMEOUT_MS=3000
DB_POOL_VALIDATION_TIMEOUT_MS=2000

############################################
# Printer Configuration
############################################
PRINTER_INVENTORY_FILE=config/TBG3002/printers.yaml
PRINTER_ROUTING_FILE=config/TBG3002/printer-routing.yaml
```

**Important:** 
- Never commit or share `wms-tags.env` (contains credentials)
- The configuration file should be in the same directory as the launcher scripts
- All environment variables can also be set via system environment variables (higher priority than file)

---

## Running the Application

### Command-Line Interface (CLI)

**Windows:**
```batch
wms-tags.bat [COMMAND] [OPTIONS]
```

**Linux/macOS:**
```bash
./wms-tags.sh [COMMAND] [OPTIONS]
```

### Available Commands

#### 1. Show Configuration
```bash
wms-tags.bat config
```
**Output:** All configuration values (passwords redacted for security)

#### 2. Test Database Connection
```bash
wms-tags.bat db-test
```
**Expected:** 
- ✓ Connection successful
- ✓ Read-only mode confirmed
- ✓ Sample query executed

#### 3. Generate Labels (Dry-Run)
```bash
wms-tags.bat run --shipment-id 8000141715 --dry-run --output-dir out/
```
**Output:** 
- ZPL files generated in `out/` directory
- No printing to physical printers
- Safe for testing

#### 4. Generate and Print Labels
```bash
wms-tags.bat run --shipment-id 8000141715 --output-dir labels/
```
**Output:** 
- ZPL files saved
- Labels sent to configured printers
- Printer selection based on routing rules

#### 5. Manual Printer Override
```bash
wms-tags.bat run --shipment-id 8000141715 --printer DISPATCH --output-dir labels/
```
**Note:** `DISPATCH` printer ID must be configured in `config/TBG3002/printers.yaml`

### Graphical User Interface (GUI)

**Windows:**
```batch
wms-pallet-tag-system.bat
# or double-click: wms-pallet-tag-system.bat
```

**Linux/macOS:**
```bash
./wms-pallet-tag-system.sh
# or: chmod +x wms-pallet-tag-system.sh && ./wms-pallet-tag-system.sh
```

**GUI Workflow:**
1. Enter **Shipment ID** (e.g., 8000141715)
2. Select **Printer** from dropdown
3. Click **Preview** to load shipment details
4. Verify shipment information and pallet count
5. Click **Confirm Print** to generate and print labels
6. Monitor progress in status area
7. Find generated artifacts in `out/gui-<shipment>-<timestamp>/`

---

## Configuration Details

### Environment Variables

All settings can be specified as environment variables (higher priority than file):

```bash
# Windows PowerShell
$env:ORACLE_USERNAME = "your_user"
$env:ORACLE_PASSWORD = "your_pass"
$env:WMS_ENV = "PROD"
```

```bash
# Linux/macOS
export ORACLE_USERNAME=your_user
export ORACLE_PASSWORD=your_pass
export WMS_ENV=PROD
```

### Configuration File Precedence

1. **Environment Variables** (highest priority)
2. **wms-tags.env** file in application directory
3. **Built-in defaults** (lowest priority)

### Key Configuration Options

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `ORACLE_USERNAME` | Yes | - | WMS database user |
| `ORACLE_PASSWORD` | Yes | - | WMS database password |
| `ORACLE_SERVICE` | Yes | WMSP | Oracle service name |
| `ORACLE_PORT` | No | 1521 | Oracle port |
| `ORACLE_HOST` (or `ORACLE_DSN`) | Yes | - | Oracle host or full JDBC URL |
| `WMS_ENV` | No | PROD | Environment: PROD or QA |
| `ACTIVE_SITE` | No | TBG3002 | Site code for routing |
| `PRINTER_ROUTING_FILE` | No | config/TBG3002/printer-routing.yaml | Routing rules location |
| `PRINTER_INVENTORY_FILE` | No | config/TBG3002/printers.yaml | Printer config location |

### Printer Configuration (printers.yaml)

```yaml
printers:
  DISPATCH:
    address: 10.19.68.100
    port: 9100
    description: Dispatch Area Printer
    
  RECEIVING:
    address: 10.19.68.101
    port: 9100
    description: Receiving Dock Printer
```

### Printer Routing Rules (printer-routing.yaml)

```yaml
rules:
  - stagingLocation: "DISPATCH_AREA"
    operator: "EQUALS"
    printerId: "DISPATCH"
    
  - stagingLocation: "REC"
    operator: "STARTS_WITH"
    printerId: "RECEIVING"
```

---

## Output Directory Structure

After running labels, the output directory contains:

```
labels/
├── snapshot_<shipment>_<jobid>_<timestamp>.json    # Full shipment data
├── shipment_<shipment>_<jobid>_<timestamp>_1.zpl   # Pallet 1 label
├── shipment_<shipment>_<jobid>_<timestamp>_2.zpl   # Pallet 2 label
└── ... (one per pallet)

gui-8000141715-20260218-143022/  # GUI session output
├── snapshot_*.json
├── *.zpl
└── summary.txt
```

---

## Troubleshooting

### Java Not Found

**Error:** `Java command not found`

**Solution:** 
- The bundled Java JRE should be automatically used
- If not, check that `jre/` directory exists in the application folder
- Or install Java 11+ globally and update launcher scripts

### Configuration File Not Found

**Error:** `Configuration file not found`

**Solution:**
```bash
# Verify wms-tags.env exists in the application directory
cd wms-pallet-tag-system
ls -la wms-tags.env    # Linux/macOS
dir wms-tags.env       # Windows
```

### Database Connection Failed

**Error:** `Cannot connect to Oracle database`

**Solutions:**
1. Verify credentials in `wms-tags.env`:
   ```bash
   wms-tags.bat config
   # Check that ORACLE_USERNAME, ORACLE_PASSWORD are correct
   ```

2. Test network connectivity to WMS:
   ```bash
   ping 10.19.96.121  # (or your ORACLE_HOST)
   ```

3. Check Oracle service name:
   ```bash
   wms-tags.bat db-test
   # Look for: ORACLE_SERVICE = WMSP
   ```

4. Run diagnostics:
   ```bash
   wms-tags.bat db-test -v  # Verbose output
   ```

### Printer Not Found

**Error:** `Printer DISPATCH not found`

**Solutions:**
1. List configured printers:
   ```bash
   cat config/TBG3002/printers.yaml
   ```

2. Add printer to inventory:
   ```yaml
   # In config/TBG3002/printers.yaml
   DISPATCH:
     address: 10.19.68.100
     port: 9100
   ```

3. Test printer connectivity:
   ```bash
   # On Windows from PowerShell:
   Test-NetConnection -ComputerName 10.19.68.100 -Port 9100
   
   # On Linux/macOS:
   nc -zv 10.19.68.100 9100
   ```

### GUI Window Not Visible

**Error:** GUI doesn't appear after launching

**Solutions:**
1. Check taskbar for minimized window
2. Click on the wms-pallet-tag-system taskbar button to restore
3. Run CLI first to verify configuration is correct:
   ```bash
   wms-tags.bat config
   ```

### SKU Matrix File Not Found

**Error:** `SKU mapping CSV not found`

**Solutions:**
1. Verify file exists:
   ```bash
   # Should be in one of these locations:
   config/walmart-sku-matrix.csv
   config/walmart_sku_matrix.csv
   config/TBG3002/walmart-sku-matrix.csv
   ```

2. If file is missing, copy from source:
   ```bash
   # From GitHub repository root
   copy walmart_sku_matrix.csv config/walmart-sku-matrix.csv
   ```

### ZPL Template Not Found

**Error:** `ZPL template not found`

**Solutions:**
```bash
# Template should be at:
config/templates/walmart-canada-label.zpl

# If missing, check file structure:
ls config/templates/  # Linux/macOS
dir config\templates  # Windows
```

---

## Performance Tips

- **Dry-run first:** Always use `--dry-run` before actual printing
- **Batch processing:** Can process multiple shipments sequentially
- **Network optimization:** Keep printer network latency <50ms for best throughput
- **Database tuning:** Contact WMS team if queries take >5 seconds

---

## Security Notes

1. **Credentials:** Never share or commit `wms-tags.env` file
2. **Read-Only Mode:** Application connects to WMS in read-only mode
3. **Dry-Run Testing:** Always test with `--dry-run` first
4. **Network Security:** Printer network should be isolated from public internet
5. **Logs:** Check `logs/wms-tags.log` for security events

---

## Updates & Patches

To update to a newer version:

1. Download new portable ZIP from GitHub Releases
2. Extract to a new folder (e.g., `wms-pallet-tag-system-v1.0.1`)
3. Copy your `wms-tags.env` file from old folder to new folder
4. Update any customized printer/routing configs if needed
5. Test with `--dry-run` before switching over
6. Keep old folder as backup until confident in new version

---

## Getting Help

### Diagnostic Commands

```bash
# Show all configuration (passwords redacted)
wms-tags.bat config

# Test database connectivity
wms-tags.bat db-test

# Dry-run with verbose logging
wms-tags.bat run --shipment-id 8000141715 --dry-run -v

# Check logs
# Windows: type logs/wms-tags.log
# Linux/macOS: tail -f logs/wms-tags.log
```

### Support Contact

- **Author:** Zeyad Rashed
- **Email:** zeyad.rashed@tropicana.com
- **GitHub Issues:** https://github.com/notzune/wms-pallet-tag-system/issues

---

## Uninstall

**Windows:**
```batch
# Simply delete the application folder
rmdir /s C:\Program Files\wms-pallet-tag-system
```

**Linux/macOS:**
```bash
# Simply delete the application folder
rm -rf ~/opt/wms-pallet-tag-system
```

No additional cleanup needed - no system registry entries or installed packages.

---

**Thank you for using WMS Pallet Tag System!**

**Version:** 1.0.0-BETA  
**Release Date:** February 18, 2026  
**License:** See LICENSE file

