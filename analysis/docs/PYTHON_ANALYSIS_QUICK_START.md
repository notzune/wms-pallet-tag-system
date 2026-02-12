# Python WMS Analysis Tool - Quick Reference

## ONE-LINE SETUP
```bash
pip install oracledb && python wms_analysis.py
```

## WHAT WE NEED FROM DATABASE

**Order/Shipment:**
- Order/Shipment/Load ID
- Ship-To Address
- Carrier Info
- Dates

**Pallet/LPN:**
- LPN identifier
- Pallet sequence
- Case count, unit count
- Weight/dimensions

**Line Items:**
- SKU
- Description
- Quantity/UOM
- Case pack
- **Walmart Item Code** (important!)

**Location:**
- Staging location (ROSSI=Canada)
- Zone/facility

## QUICK EXECUTION

```bash
# Phase 1 - Schema only (30 sec)
python wms_analysis.py --phase 1

# Phase 4 - All phases (10 min, RECOMMENDED)
python wms_analysis.py --phase 4

# Or just run (defaults to phase 4):
python wms_analysis.py
```

## OUTPUT FILES

```
db-dumps/
├── 01_schema_discovery.txt      # All tables and columns
├── 02_sample_data.txt           # Sample rows from each table
├── 03_shipment_analysis.txt     # Shipment/order/LPN focus
└── 04_canadian_orders.txt       # Canadian Walmart analysis
```

## WHAT TO LOOK FOR

In **01_schema_discovery.txt**:
- Find tables: SHIPMENT, ORDER, LOAD, PALLET, LPN, LINE_ITEM, CUSTOMER

In **03_shipment_analysis.txt**:
- How tables relate to each other
- What columns contain key data
- Sample data to understand format

In **04_canadian_orders.txt**:
- Canadian customer identifier
- Complete order -> pallet -> items flow
- Walmart-specific fields

## TROUBLESHOOTING

| Issue | Fix |
|-------|-----|
| `No module named 'oracledb'` | `pip install oracledb` |
| `ORA-12170 TCP timeout` | Check VPN, ping 10.19.68.61 |
| `Connection refused` | Verify .env credentials |
| `Invalid username/password` | Check RPTADM password in .env |

## DOCUMENT YOUR FINDINGS

Create FINDINGS.txt with:
```
Table Names:
  Shipment Table: [actual name]
  Pallet Table: [actual name]
  Line Item Table: [actual name]

Column Mappings:
  Order ID: [table].[column]
  Ship-To: [table].[column]
  LPN: [table].[column]
  Walmart Item Code: [table].[column]

Example Canadian Order:
  Order ID: [sample value]
  Customer: [sample value]
  Staging: ROSSI
```

## SUCCESS CHECKLIST

- ✓ Connected to database
- ✓ All 4 phases completed
- ✓ Files in db-dumps/
- ✓ Identified table names
- ✓ Found Canadian Walmart example
- ✓ All required fields present
- ✓ Created FINDINGS.txt

Then: `git checkout dev` and report findings

