# Canadian Order Deep Dive Analysis - Findings

**Date**: 2025-01-23  
**Order**: 8000141715 (CJR WHOLESALE GROCERS LTD, Mississauga, ON)  
**Purpose**: Extract complete Canadian order data to plan Java implementation query strategy

---

## Executive Summary

Successfully extracted complete data for a Canadian shipment with **17 line items**, demonstrating all table relationships needed for ZPL label generation. The analysis reveals efficient query pathways and identifies several data gaps requiring investigation.

---

## Order Overview

| Field | Value |
|-------|-------|
| **Shipment ID** | 8000141715 |
| **Customer** | CJR WHOLESALE GROCERS LTD |
| **Destination** | 5876 COOPERS AVE, MISSISSAUGA, ON L4Z 2B9, CAN |
| **Carrier** | MDLE (Service: TL) |
| **Doc Number** | 30021144717 |
| **Ship Date** | 2024-07-17 |
| **Status** | C (Complete) |
| **Line Items** | 17 |
| **Stop ID** | STP0382324 |

---

## Data Extraction Results

### ✅ Successfully Retrieved

1. **SHIPMENT Header** (1 record)
   - Carrier, service level, tracking number, dates
   - Stop ID, warehouse code, external doc number
   - Status and timestamps

2. **SHIPMENT_LINE** (17 records)
   - Consolidation batch IDs (CNS3244402-CNS3244424)
   - Shipped quantities (0-224 units per line)
   - Links to order lines (format: ordernum/linenum e.g., "10/0000")

3. **ORD_LINE** (17 records)
   - SKU part numbers (e.g., 10048500019792000)
   - Order/shipped quantities
   - Sales order reference (1000241082)

4. **PRTMST** (17 records)
   - Part numbers retrieved
   - ❗ **Descriptions showing "N/A"** - may need different query or table

5. **ALT_PRTMST** (136 records, 8 per product)
   - Types: GTIN, GTINCS, GTINEA, GTINPAL, GTINRU, SSC, Short, UPC
   - Example: Base `10048500019792000` → UPC `01979`, GTIN `10048500019792`
   - ❗ **No Walmart-specific codes found yet** (may need different ALT_PRT_TYP or table)

6. **ADRMST** (1 record)
   - Complete ship-to address with all fields
   - Phone: 9058902436-218

7. **STOP** (1 record)
   - Stop ID: STP0382324
   - Linked via SHIPMENT.STOP_ID

### ❌ Table Access Issues

1. **TRLR_LOAD** - ORA-00942 (table or view does not exist)
   - RPTADM user lacks access
   - May need DBA to grant permissions or use alternative trailer source

2. **CARMST** - ORA-00942 (table or view does not exist)
   - RPTADM user lacks access
   - Carrier details limited to SHIPMENT.SCAC_CARRIER_ID field

### ⚠️ Data Gaps Requiring Investigation

1. **LPN/Pallet Linkage** (0 LPNs found)
   - Query attempt: `WHERE DSTLOC = SHIPMENT.STGZON` returned 0 results
   - **Next Steps**:
     - Try joining via `INVLOD.CONS_BATCH = SHIPMENT_LINE.CONS_BATCH`
     - Check `INVLOD.ORDNUM` = order number
     - Examine `INVLOD.PCKGR1, PCKGR2, PCKGR3, PCKGR4` fields for linkage
     - Query work orders or wave tables if available

2. **Product Descriptions**
   - PRTMST descriptions returned "N/A" (might be NULL in database)
   - May need:
     - Different column name (LNGDSC, ABRVSC, etc.)
     - Separate product detail table
     - Customer-specific product master

3. **Walmart Item Codes**
   - ALT_PRTMST has 8 types per product but none labeled as "Walmart" or "Customer"
   - Check if:
     - Walmart codes in ORD_LINE.CSTPRT (Customer Part) field (currently NULL)
     - Need different ALT_PRT_TYP value
     - Cross-reference table exists for customer-specific SKUs

4. **Pallet Sequence** ("1 of N" logic)
   - Need to determine total pallet count per shipment
   - Options:
     - Count distinct LPNs per shipment
     - Aggregate SHIPMENT_LINE.PALLET_QTY
     - Query pallet transaction/history table

---

## Table Relationships Discovered

```
SHIPMENT (8000141715)
  ├─ RT_ADR_ID (A000094956) → ADRMST (ship-to address)
  ├─ STOP_ID (STP0382324) → STOP (routing)
  └─ SHIPMENT_LINE (17 lines)
       ├─ CONS_BATCH (CNS3244402-CNS3244424) → INVLOD ❓(not yet confirmed)
       └─ ORDNUM/ORDLIN (8000141715/10) → ORD_LINE
            ├─ PRTNUM (10048500019792000) → PRTMST (product master)
            └─ PRTNUM → ALT_PRTMST (136 alternate codes)
```

---

## Query Pathway Recommendations for Java

### Approach 1: Single Complex Join (Recommended for Performance)

**Pros**: Single database round-trip, efficient execution plan  
**Cons**: Complex SQL, harder to debug, may return duplicate rows requiring deduplication

```sql
SELECT 
    -- Shipment header
    s.SHPNUM, s.SCAC_CARRIER_ID, s.SERV_LVL, s.TRLR_NUM,
    s.STOP_ID, s.EXT_SHPNUM, s.TRKNUM, s.ADD_DTE, s.CFM_CFM_DATE,
    
    -- Ship-to address
    a.ADRNAM, a.ADRLN1, a.ADRLN2, a.ADRLN3,
    a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME, a.PHNNUM,
    
    -- Shipment line
    sl.SHPLIN_ID, sl.ORDNUM, sl.ORDLIN, sl.ORDSLN,
    sl.CONS_BATCH, sl.SHIP_QTY, sl.CASE_QTY, sl.PALLET_QTY,
    
    -- Order line details
    ol.PRTNUM, ol.CSTPRT, ol.ORDQTY, ol.SHPQTY, ol.UNTPAK,
    
    -- Product master
    pm.LNGDSC, pm.ABRVSC, pm.UOM, pm.NETWGT,
    
    -- Stop info
    st.STOP_SEQ, st.ARRIVL_DTE

FROM WMSP.SHIPMENT s
INNER JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
INNER JOIN WMSP.SHIPMENT_LINE sl ON s.SHPNUM = sl.SHPNUM
INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM 
                            AND sl.ORDLIN = ol.ORDLIN 
                            AND sl.ORDSLN = ol.ORDSLN
LEFT JOIN WMSP.PRTMST pm ON ol.PRTNUM = pm.PRTNUM
LEFT JOIN WMSP.STOP st ON s.STOP_ID = st.STOP_ID

WHERE s.SHPNUM = ?

ORDER BY sl.SHPLIN_LINE, ol.ORDLIN;
```

**LPN Join** (once linkage confirmed):
```sql
-- Add to FROM clause:
LEFT JOIN WMSP.INVLOD lpn ON sl.CONS_BATCH = lpn.CONS_BATCH
                         AND lpn.STOLOC IS NOT NULL  -- exclude pending
```

**Alternate Parts** (separate query due to 1:N relationship):
```sql
SELECT 
    alt.PRTNUM AS BASE_PRTNUM,
    alt.ALT_PRT,
    alt.ALT_PRT_TYP
FROM WMSP.ALT_PRTMST alt
WHERE alt.PRTNUM IN (
    SELECT DISTINCT ol.PRTNUM
    FROM WMSP.SHIPMENT_LINE sl
    INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM 
                                AND sl.ORDLIN = ol.ORDLIN
    WHERE sl.SHPNUM = ?
)
AND alt.ALT_PRT_TYP IN ('UPC', 'GTIN', 'WALMART', 'CUST')  -- filter relevant types
ORDER BY alt.PRTNUM, alt.ALT_PRT_TYP;
```

### Approach 2: Hierarchical Queries (Recommended for Debugging)

**Pros**: Clear structure, easier debugging, matches entity model  
**Cons**: Multiple round-trips, more database connections

```java
// In OracleDbQueryRepository.java

public ShipmentWithLabels findShipmentForLabelGeneration(String shipmentId) {
    // 1. Get shipment header + address (single query with JOIN)
    Shipment header = findShipmentHeader(shipmentId);
    
    // 2. Get all shipment lines
    List<ShipmentLine> lines = findShipmentLines(shipmentId);
    
    // 3. Get order line details (batch query for all lines)
    Map<String, OrderLine> orderDetails = findOrderLineDetails(lines);
    
    // 4. Get product descriptions (batch query)
    Map<String, Product> products = findProducts(orderDetails.keySet());
    
    // 5. Get alternate part numbers (batch query)
    Map<String, List<AlternatePart>> alternates = findAlternateParts(products.keySet());
    
    // 6. Get LPNs for shipment (once linkage confirmed)
    List<LPN> lpns = findLPNsForShipment(shipmentId);
    
    return assembleShipmentData(header, lines, orderDetails, products, alternates, lpns);
}
```

**Individual Query Examples**:

```sql
-- 1. Shipment header with address
SELECT s.*, a.*
FROM WMSP.SHIPMENT s
INNER JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
WHERE s.SHPNUM = ?;

-- 2. Shipment lines
SELECT *
FROM WMSP.SHIPMENT_LINE
WHERE SHPNUM = ?
ORDER BY SHPLIN_LINE;

-- 3. Order line details (batch)
SELECT *
FROM WMSP.ORD_LINE
WHERE (ORDNUM, ORDLIN, ORDSLN) IN (
    VALUES (?, ?, ?), (?, ?, ?), ...  -- from shipment lines
);

-- 4. Product master (batch)
SELECT *
FROM WMSP.PRTMST
WHERE PRTNUM IN (?, ?, ?, ...);  -- from order lines

-- 5. Alternate parts (batch)
SELECT *
FROM WMSP.ALT_PRTMST
WHERE PRTNUM IN (?, ?, ?, ...)
AND ALT_PRT_TYP IN ('UPC', 'GTIN', 'WALMART');
```

### Architecture Recommendation

**For v1.0 (Current Sprint)**:
- Use **Approach 2 (Hierarchical)** initially
- Easier to debug and validate data correctness
- Clear entity mapping matches domain model
- Performance adequate for single shipment queries

**For v2.0 (Optimization)**:
- Migrate to **Approach 1 (Single Join)** if performance testing shows issues
- Critical for batch label generation (multiple shipments)
- Add result set deduplication logic
- Monitor query execution plans

---

## Critical Implementation Notes

### 1. LPN Linkage Investigation Required

**Current Status**: INVLOD query returned 0 results using `DSTLOC = STGZON` approach

**Recommended Investigation Steps**:
```sql
-- Test 1: Direct consolidation batch link
SELECT COUNT(*) FROM WMSP.INVLOD 
WHERE CONS_BATCH IN ('CNS3244402', 'CNS3244403', ..., 'CNS3244424');

-- Test 2: Order number link
SELECT COUNT(*) FROM WMSP.INVLOD 
WHERE ORDNUM = '8000141715';

-- Test 3: Examine INVLOD structure for this warehouse
SELECT DISTINCT STOLOC, DSTLOC, PCKGR1, PCKGR2, PCKGR3, PCKGR4
FROM WMSP.INVLOD
WHERE WRKREF LIKE '%8000141715%'
OR MAN_CONS LIKE '%8000141715%';

-- Test 4: Check work order or wave tables
SELECT * FROM WMSP.WRKQUE WHERE ORDNUM = '8000141715';
SELECT * FROM WMSP.WAVE_ALLOCATION WHERE ORDNUM = '8000141715';
```

### 2. Product Description Enhancement

**Issue**: PRTMST.LNGDSC returning NULL or "N/A"

**Options**:
```sql
-- Try alternative description columns
SELECT PRTNUM, 
       LNGDSC AS long_desc,
       ABRVSC AS short_desc,
       PRTNOTE AS notes,
       DESCR AS description
FROM WMSP.PRTMST WHERE PRTNUM = '10048500019792000';

-- Check for extended product table
SELECT * FROM WMSP.PRTMST_EXT WHERE PRTNUM = '10048500019792000';
SELECT * FROM WMSP.PRODUCT_DETAIL WHERE PRTNUM = '10048500019792000';
```

### 3. Walmart Item Code Location

**Goal**: Find Walmart's customer-facing SKU (different from internal TBG SKU)

**Search Strategy**:
```sql
-- Check order line customer part field
SELECT ORDNUM, ORDLIN, PRTNUM, CSTPRT 
FROM WMSP.ORD_LINE 
WHERE ORDNUM = '8000141715' AND CSTPRT IS NOT NULL;

-- Search for "Walmart" or "WMT" alt part types
SELECT DISTINCT ALT_PRT_TYP FROM WMSP.ALT_PRTMST;

-- Check customer-specific tables
SELECT * FROM WMSP.CSTMST_DETAILS WHERE CST_ID = ?;
```

### 4. Unshipped Order Testing

**Goal**: Verify same data available for orders in progress (non-Complete status)

```sql
-- Find recent unshipped Canadian orders
SELECT s.SHPNUM, s.SHPSTS, s.ADD_DTE, a.ADRNAM
FROM WMSP.SHIPMENT s
INNER JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
WHERE a.CTRY_NAME = 'CAN'
AND s.SHPSTS != 'C'  -- Not complete
AND s.ADD_DTE >= SYSDATE - 7  -- Last 7 days
ORDER BY s.ADD_DTE DESC;

-- Test full extraction on one unshipped order
-- Verify SHIPMENT_LINE, ORD_LINE, etc. populated
```

### 5. Pallet Sequence Logic ("1 of N")

**Requirement**: Each label needs to show "Pallet 1 of 3", "Pallet 2 of 3", etc.

**Implementation Options**:

**Option A**: Count LPNs per shipment
```sql
SELECT 
    lpn.LODNUM AS pallet_id,
    ROW_NUMBER() OVER (PARTITION BY sl.SHPNUM ORDER BY lpn.LODNUM) AS pallet_seq,
    COUNT(*) OVER (PARTITION BY sl.SHPNUM) AS total_pallets
FROM WMSP.SHIPMENT_LINE sl
INNER JOIN WMSP.INVLOD lpn ON sl.CONS_BATCH = lpn.CONS_BATCH
WHERE sl.SHPNUM = ?;
```

**Option B**: Use pallet quantity aggregation
```sql
SELECT SUM(PALLET_QTY) AS total_pallets
FROM WMSP.SHIPMENT_LINE
WHERE SHPNUM = ?;
```

**Option C**: Query pallet transaction history
```sql
SELECT COUNT(DISTINCT LODNUM) AS total_pallets
FROM WMSP.PALLET_MOVE
WHERE SHPNUM = ?;
```

---

## Sample Data Reference

### Representative SKUs
- `10098100010019000` - 15 units shipped
- `10048500019792000` - 30 units shipped
- `10048500019815000` - 60 units shipped
- `10048500203542000` - 224 units shipped (largest line)

### Alternate Part Number Patterns
Base SKU: `10048500019792000`
- UPC: `01979`
- GTIN: `10048500019792`
- GTINEA: `48500019795`
- GTINPAL: `20048500019799`
- SSC: `004850001979`
- Short: `A01979000`

### Consolidation Batches
Format: `CNS3244402` through `CNS3244424` (23 batches for 17 lines)

---

## Next Steps for Java Implementation

1. **Immediate** (Blocking label generation):
   - [ ] Investigate LPN linkage (test consolidation batch join)
   - [ ] Confirm product description column name/table
   - [ ] Determine Walmart item code source

2. **High Priority** (Required for complete labels):
   - [ ] Implement `OracleDbQueryRepository.java` with hierarchical queries
   - [ ] Add null-safe handling for all optional fields
   - [ ] Create integration tests using order 8000141715 as golden sample

3. **Medium Priority** (Optimization):
   - [ ] Test query performance with various shipment sizes
   - [ ] Implement single-join approach if needed
   - [ ] Add query result caching for product/alternate lookups

4. **Low Priority** (Enhancement):
   - [ ] Request DBA access to TRLR_LOAD and CARMST tables
   - [ ] Implement archived shipment support (ARC_SHIPMENT)
   - [ ] Add support for multi-stop shipments

---

## Files Generated

- `db-dumps/canadian-analysis/analysis_log.txt` - Full console log
- `db-dumps/canadian-analysis/complete_order_8000141715.txt` - Failed (encoding issue)
- `canadian_order_deep_dive_fixed.py` - Analysis script (780 lines)

---

**Analysis completed**: 2025-01-23  
**Next update**: After LPN linkage investigation
