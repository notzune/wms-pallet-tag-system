"""
Canadian Order Deep Dive Analysis

This script finds real Canadian orders (shipped and unshipped) and extracts
ALL data that would be needed for pallet label generation, simulating the
complete Java application flow.

ALL OPERATIONS ARE READ-ONLY.
"""

import oracledb
import os
from pathlib import Path
from datetime import datetime
import json

def load_config():
    """Load database configuration from .env"""
    env_file = Path('.env')
    config = {}
    if env_file.exists():
        with open(env_file) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    config[key.strip()] = value.strip().strip('"')
    
    return {
        'username': config.get('ORACLE_USERNAME', 'RPTADM'),
        'password': config.get('ORACLE_PASSWORD', ''),
        'host': config.get('SITE_TBG3002_HOST', '10.19.68.61'),
        'port': int(config.get('ORACLE_PORT', '1521')),
        'service': config.get('ORACLE_SERVICE', 'WMSP')
    }

def connect_db(config):
    """Establish database connection"""
    dsn = oracledb.makedsn(config['host'], config['port'], service_name=config['service'])
    return oracledb.connect(user=config['username'], password=config['password'], dsn=dsn)

def find_canadian_orders(cursor, limit=10):
    """Find Canadian orders using multiple strategies"""
    print("\n" + "="*80)
    print("SEARCHING FOR CANADIAN AND SAMPLE ORDERS")
    print("="*80)
    
    results = {}
    
    # Strategy 1: By country in address (try multiple variations)
    print("\nStrategy 1: Orders with CANADA in ship-to address...")
    for country_pattern in ['%CANAD%', 'CA', 'CAN']:
        sql1 = """
            SELECT s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.CARCOD, s.SRVLVL, 
                   s.ADDDTE, s.DSTLOC, s.STOP_ID,
                   a.ADRNAM, a.ADRCTY, a.ADRSTC, a.CTRY_NAME
            FROM WMSP.SHIPMENT s
            JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
            WHERE (UPPER(a.CTRY_NAME) LIKE :pattern OR a.CTRY_NAME = :pattern2)
            AND ROWNUM <= :limit
            ORDER BY s.ADDDTE DESC
        """
        cursor.execute(sql1, {'pattern': country_pattern, 'pattern2': country_pattern.replace('%', ''), 'limit': limit})
        found = cursor.fetchall()
        if found:
            results['by_country'] = found
            print(f"  Found: {len(found)} orders with country pattern '{country_pattern}'")
            break
        else:
            print(f"  No orders found with country pattern '{country_pattern}'")
    
    if not results.get('by_country'):
        results['by_country'] = []
    
    # Strategy 2: Check archived Canadian orders
    print("\nStrategy 2: Checking ARCHIVED Canadian orders...")
    sql_arc = """
        SELECT s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.CARCOD, s.SRVLVL, 
               s.ADDDTE, s.DSTLOC, s.STOP_ID,
               a.ADRNAM, a.ADRCTY, a.ADRSTC, a.CTRY_NAME
        FROM WMSP.ARC_SHIPMENT s
        JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
        WHERE (UPPER(a.CTRY_NAME) LIKE '%CANAD%' OR a.ADRSTC IN ('ON', 'QC', 'BC', 'AB'))
        AND ROWNUM <= :limit
        ORDER BY s.ADDDTE DESC
    """
    cursor.execute(sql_arc, {'limit': limit})
    results['archived_canadian'] = cursor.fetchall()
    print(f"  Found: {len(results['archived_canadian'])} archived Canadian orders")
    
    # Strategy 3: By ROSSI staging location
    print("\nStrategy 3: Orders with ROSSI or similar staging...")
    sql2 = """
        SELECT DISTINCT s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.CARCOD, s.SRVLVL,
               s.ADDDTE, s.DSTLOC, s.STOP_ID,
               a.ADRNAM, a.ADRCTY, a.ADRSTC, a.CTRY_NAME
        FROM WMSP.SHIPMENT s
        JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
        WHERE s.DSTLOC LIKE '%ROSS%'
        AND ROWNUM <= :limit
        ORDER BY s.ADDDTE DESC
    """
    cursor.execute(sql2, {'limit': limit})
    results['by_rossi'] = cursor.fetchall()
    print(f"  Found: {len(results['by_rossi'])} orders with ROSSI-like staging")
    
    # Strategy 4: Get sample of ANY recent orders for demo
    print("\nStrategy 4: Getting recent completed orders (for demonstration)...")
    sql_any = """
        SELECT s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.CARCOD, s.SRVLVL, 
               s.ADDDTE, s.DSTLOC, s.STOP_ID,
               a.ADRNAM, a.ADRCTY, a.ADRSTC, a.CTRY_NAME
        FROM WMSP.SHIPMENT s
        JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
        WHERE s.SHPSTS = 'C'
        AND s.ADDDTE >= SYSDATE - 180
        AND ROWNUM <= :limit
        ORDER BY s.ADDDTE DESC
    """
    cursor.execute(sql_any, {'limit': limit})
    results['any_recent'] = cursor.fetchall()
    print(f"  Found: {len(results['any_recent'])} recent completed orders")
    
    return results

def get_complete_shipment_data(cursor, ship_id):
    """Get ALL data for a specific shipment"""
    print("\n" + "="*80)
    print(f"COMPLETE DATA EXTRACTION FOR SHIPMENT: {ship_id}")
    print("="*80)
    
    data = {'ship_id': ship_id}
    
    # 1. SHIPMENT HEADER
    print("\n[1] SHIPMENT HEADER...")
    # Try active shipments first
    sql_ship = """
        SELECT s.*,
               a.ADR_ID, a.ADRNAM, a.ADRLN1, a.ADRLN2, a.ADRLN3,
               a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME, a.RGNCOD,
               a.PHNNUM, a.FAXNUM, a.ATTN_NAME, a.CONT_NAME
        FROM WMSP.SHIPMENT s
        LEFT JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
        WHERE s.SHIP_ID = :ship_id
    """
    cursor.execute(sql_ship, {'ship_id': ship_id})
    cols = [desc[0] for desc in cursor.description]
    row = cursor.fetchone()
    
    # If not found, try archived shipments
    if not row:
        print("  Not in active table, checking archived...")
        sql_arc = """
            SELECT s.*,
                   a.ADR_ID, a.ADRNAM, a.ADRLN1, a.ADRLN2, a.ADRLN3,
                   a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME, a.RGNCOD,
                   a.PHNNUM, a.FAXNUM, a.ATTN_NAME, a.CONT_NAME
            FROM WMSP.ARC_SHIPMENT s
            LEFT JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
            WHERE s.SHIP_ID = :ship_id
        """
        cursor.execute(sql_arc, {'ship_id': ship_id})
        cols = [desc[0] for desc in cursor.description]
        row = cursor.fetchone()
        if row:
            data['archived'] = True
    
    if row:
        data['shipment'] = dict(zip(cols, row))
        print(f"  ✓ Shipment found: Status={data['shipment'].get('SHPSTS')}, "
              f"Carrier={data['shipment'].get('CARCOD')}, "
              f"Service={data['shipment'].get('SRVLVL')}")
    else:
        print(f"  ✗ Shipment {ship_id} not found!")
        return None
    
    # 2. SHIPMENT LINES
    print("\n[2] SHIPMENT LINES...")
    sql_lines = """
        SELECT sl.*
        FROM WMSP.SHIPMENT_LINE sl
        WHERE sl.SHIP_ID = :ship_id
        ORDER BY sl.ORDLIN, sl.ORDSLN
    """
    cursor.execute(sql_lines, {'ship_id': ship_id})
    cols = [desc[0] for desc in cursor.description]
    lines = cursor.fetchall()
    data['shipment_lines'] = [dict(zip(cols, row)) for row in lines]
    print(f"  ✓ Found {len(data['shipment_lines'])} shipment lines")
    
    # 3. ORDER LINES (with product details)
    print("\n[3] ORDER LINE DETAILS (Product/SKU info)...")
    if data['shipment_lines']:
        order_details = []
        for sl in data['shipment_lines']:
            sql_ord = """
                SELECT ol.*
                FROM WMSP.ORD_LINE ol
                WHERE ol.ORDNUM = :ordnum
                AND ol.ORDLIN = :ordlin
                AND ol.ORDSLN = :ordsln
                AND ol.CLIENT_ID = :client_id
            """
            cursor.execute(sql_ord, {
                'ordnum': sl['ORDNUM'],
                'ordlin': sl['ORDLIN'],
                'ordsln': sl['ORDSLN'],
                'client_id': sl['CLIENT_ID']
            })
            cols = [desc[0] for desc in cursor.description]
            ord_row = cursor.fetchone()
            if ord_row:
                order_details.append(dict(zip(cols, ord_row)))
        data['order_lines'] = order_details
        print(f"  ✓ Found {len(data['order_lines'])} order line details")
    
    # 4. PRODUCT MASTER DATA
    print("\n[4] PRODUCT MASTER DATA...")
    if data.get('order_lines'):
        products = []
        for ol in data['order_lines']:
            sql_prt = """
                SELECT p.*
                FROM WMSP.PRTMST p
                WHERE p.PRTNUM = :prtnum
                AND p.PRT_CLIENT_ID = :client_id
            """
            cursor.execute(sql_prt, {
                'prtnum': ol['PRTNUM'],
                'client_id': ol.get('PRT_CLIENT_ID', '----')
            })
            cols = [desc[0] for desc in cursor.description]
            prt_row = cursor.fetchone()
            if prt_row:
                products.append(dict(zip(cols, prt_row)))
        data['products'] = products
        print(f"  ✓ Found {len(data['products'])} product records")
    
    # 5. ALTERNATE PART NUMBERS (including Walmart codes)
    print("\n[5] ALTERNATE PART NUMBERS (Walmart Item Codes?)...")
    if data.get('order_lines'):
        alt_parts = []
        for ol in data['order_lines']:
            sql_alt = """
                SELECT ap.*
                FROM WMSP.ALT_PRTMST ap
                WHERE ap.PRTNUM = :prtnum
                AND ap.PRT_CLIENT_ID = :client_id
            """
            cursor.execute(sql_alt, {
                'prtnum': ol['PRTNUM'],
                'client_id': ol.get('PRT_CLIENT_ID', '----')
            })
            cols = [desc[0] for desc in cursor.description]
            alt_rows = cursor.fetchall()
            for alt_row in alt_rows:
                alt_parts.append(dict(zip(cols, alt_row)))
        data['alternate_parts'] = alt_parts
        print(f"  ✓ Found {len(data['alternate_parts'])} alternate part numbers")
    
    # 6. LPNs/PALLETS (if we can find linkage)
    print("\n[6] SEARCHING FOR RELATED LPNs...")
    # Try to find LPNs through various linkages
    lpns = []
    
    # Try by DSTLOC (staging location)
    if data['shipment'].get('DSTLOC'):
        sql_lpn1 = """
            SELECT il.*
            FROM WMSP.INVLOD il
            WHERE il.STOLOC = :dstloc
            AND il.WH_ID = :wh_id
            AND ROWNUM <= 10
        """
        cursor.execute(sql_lpn1, {
            'dstloc': data['shipment']['DSTLOC'],
            'wh_id': data['shipment']['WH_ID']
        })
        cols = [desc[0] for desc in cursor.description]
        lpn_rows = cursor.fetchall()
        for lpn_row in lpn_rows:
            lpns.append(dict(zip(cols, lpn_row)))
    
    data['lpns'] = lpns
    print(f"  ✓ Found {len(data['lpns'])} LPN records (by staging location)")
    
    # 7. STOP INFORMATION
    print("\n[7] STOP INFORMATION...")
    if data['shipment'].get('STOP_ID'):
        sql_stop = """
            SELECT st.*
            FROM WMSP.STOP st
            WHERE st.STOP_ID = :stop_id
        """
        cursor.execute(sql_stop, {'stop_id': data['shipment']['STOP_ID']})
        cols = [desc[0] for desc in cursor.description]
        stop_row = cursor.fetchone()
        if stop_row:
            data['stop'] = dict(zip(cols, stop_row))
            print(f"  ✓ Stop found: {data['stop'].get('STOP_ID')}")
        else:
            print(f"  ℹ No stop record found")
    
    # 8. TRAILER/LOAD INFORMATION
    print("\n[8] TRAILER/LOAD INFORMATION...")
    try:
        sql_trlr = """
            SELECT tl.*
            FROM WMSP.TRLR_LOAD tl
            WHERE tl.SHIP_ID = :ship_id
        """
        cursor.execute(sql_trlr, {'ship_id': ship_id})
        cols = [desc[0] for desc in cursor.description]
        trlr_rows = cursor.fetchall()
        data['trailer_loads'] = [dict(zip(cols, row)) for row in trlr_rows]
        print(f"  ✓ Found {len(data['trailer_loads'])} trailer load records")
    except Exception as e:
        data['trailer_loads'] = []
        print(f"  ℹ TRLR_LOAD table not accessible or empty ({e})")
    
    # 9. CARRIER INFORMATION
    print("\n[9] CARRIER MASTER DATA...")
    if data['shipment'].get('CARCOD'):
        try:
            sql_car = """
                SELECT cm.*
                FROM WMSP.CARMST cm
                WHERE cm.CARCOD = :carcod
            """
            cursor.execute(sql_car, {'carcod': data['shipment']['CARCOD']})
            cols = [desc[0] for desc in cursor.description]
            car_row = cursor.fetchone()
            if car_row:
                data['carrier'] = dict(zip(cols, car_row))
                print(f"  ✓ Carrier found: {data['carrier'].get('CARNAM')}")
            else:
                print(f"  ℹ Carrier {data['shipment']['CARCOD']} not found in master")
        except Exception as e:
            print(f"  ℹ CARMST table not accessible ({e})")
    
    return data

def find_unshipped_canadian_order(cursor):
    """Find a Canadian order that hasn't shipped yet"""
    print("\n" + "="*80)
    print("SEARCHING FOR UNSHIPPED CANADIAN ORDERS")
    print("="*80)
    
    # Look for orders with status != 'C' (Complete)
    # Try multiple strategies
    results = []
    
    # Try 1: Active Canadian orders
    sql = """
        SELECT s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.CARCOD, s.SRVLVL,
               s.ADDDTE, s.DSTLOC, s.STGDTE, s.LODDTE,
               a.ADRNAM, a.ADRCTY, a.ADRSTC, a.CTRY_NAME
        FROM WMSP.SHIPMENT s
        JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
        WHERE (UPPER(a.CTRY_NAME) LIKE '%CANAD%' OR a.ADRSTC IN ('ON', 'QC', 'BC', 'AB'))
        AND s.SHPSTS != 'C'
        AND ROWNUM <= 20
        ORDER BY s.ADDDTE DESC
    """
    cursor.execute(sql)
    results = cursor.fetchall()
    
    if not results:
        # Try 2: Any unshipped order for demo
        print("\nNo Canadian unshipped orders found, trying any recent unshipped...")
        sql2 = """
            SELECT s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.CARCOD, s.SRVLVL,
                   s.ADDDTE, s.DSTLOC, s.STGDTE, s.LODDTE,
                   a.ADRNAM, a.ADRCTY, a.ADRSTC, a.CTRY_NAME
            FROM WMSP.SHIPMENT s
            JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
            WHERE s.SHPSTS IN ('N', 'P', 'A')  -- New, Picking, Allocated
            AND s.ADDDTE >= SYSDATE - 30
            AND ROWNUM <= 20
            ORDER BY s.ADDDTE DESC
        """
        cursor.execute(sql2)
        results = cursor.fetchall()
    
    print(f"\nFound {len(results)} unshipped orders")
    
    if results:
        print("\nSample unshipped orders:")
        for i, row in enumerate(results[:5], 1):
            print(f"\n  {i}. SHIP_ID: {row[0]}")
            print(f"     Status: {row[2]}")
            print(f"     Customer: {row[9]}")
            print(f"     City: {row[10]}, {row[11]}")
            print(f"     Country: {row[12]}")
            print(f"     Carrier: {row[3]}")
            print(f"     Added: {row[5]}")
    
    return results

def format_data_for_display(data):
    """Format extracted data for human-readable output"""
    output = []
    output.append("\n" + "="*80)
    output.append("EXTRACTED DATA SUMMARY")
    output.append("="*80)
    
    if data.get('shipment'):
        s = data['shipment']
        output.append("\n📦 SHIPMENT HEADER")
        output.append("-" * 80)
        output.append(f"  Ship ID:        {s.get('SHIP_ID')}")
        output.append(f"  External ID:    {s.get('HOST_EXT_ID')}")
        output.append(f"  Status:         {s.get('SHPSTS')}")
        output.append(f"  Warehouse:      {s.get('WH_ID')}")
        output.append(f"  Carrier:        {s.get('CARCOD')}")
        output.append(f"  Service Level:  {s.get('SRVLVL')}")
        output.append(f"  Trailer Num:    {s.get('TRLR_ID', 'N/A')}")
        output.append(f"  Stop ID:        {s.get('STOP_ID')}")
        output.append(f"  Doc Number:     {s.get('DOC_NUM')}")
        output.append(f"  Tracking:       {s.get('TRACK_NUM')}")
        output.append(f"  Ship Date:      {s.get('EARLY_SHPDTE')}")
        output.append(f"  Delivery Date:  {s.get('LATE_DLVDTE')}")
        output.append(f"  Staging Loc:    {s.get('DSTLOC')}")
        output.append(f"  Wave Set:       {s.get('WAVE_SET')}")
        
        output.append(f"\n📍 SHIP-TO ADDRESS")
        output.append("-" * 80)
        output.append(f"  Name:     {s.get('ADRNAM')}")
        output.append(f"  Address1: {s.get('ADRLN1')}")
        output.append(f"  Address2: {s.get('ADRLN2')}")
        output.append(f"  City:     {s.get('ADRCTY')}")
        output.append(f"  State:    {s.get('ADRSTC')}")
        output.append(f"  Postal:   {s.get('ADRPSZ')}")
        output.append(f"  Country:  {s.get('CTRY_NAME')}")
        output.append(f"  Phone:    {s.get('PHNNUM')}")
        output.append(f"  Attn:     {s.get('ATTN_NAME')}")
    
    if data.get('shipment_lines'):
        output.append(f"\n📋 SHIPMENT LINES ({len(data['shipment_lines'])} lines)")
        output.append("-" * 80)
        for i, line in enumerate(data['shipment_lines'], 1):
            output.append(f"\n  Line {i}:")
            output.append(f"    Line ID:      {line.get('SHIP_LINE_ID')}")
            output.append(f"    Order:        {line.get('ORDNUM')}")
            output.append(f"    Order Line:   {line.get('ORDLIN')}/{line.get('ORDSLN')}")
            output.append(f"    Cons Batch:   {line.get('CONS_BATCH')}")
            output.append(f"    Shipped Qty:  {line.get('SHPQTY')}")
            output.append(f"    Case Qty:     {line.get('TOT_PLN_CAS_QTY')}")
            output.append(f"    Pallet Qty:   {line.get('TOT_PLN_PAL_QTY')}")
            output.append(f"    Weight:       {line.get('TOT_PLN_WGT')}")
    
    if data.get('order_lines'):
        output.append(f"\n🏷️  ORDER LINE DETAILS ({len(data['order_lines'])} items)")
        output.append("-" * 80)
        for i, ol in enumerate(data['order_lines'], 1):
            output.append(f"\n  Item {i}:")
            output.append(f"    SKU (PRTNUM):     {ol.get('PRTNUM')}")
            output.append(f"    Customer Part:    {ol.get('CSTPRT')}")
            output.append(f"    Sales Order:      {ol.get('SALES_ORDNUM')}")
            output.append(f"    Order Qty:        {ol.get('ORDQTY')}")
            output.append(f"    Shipped Qty:      {ol.get('SHPQTY')}")
            output.append(f"    Units/Case:       {ol.get('UNTCAS')}")
    
    if data.get('products'):
        output.append(f"\n📦 PRODUCT DETAILS ({len(data['products'])} products)")
        output.append("-" * 80)
        for i, prt in enumerate(data['products'], 1):
            output.append(f"\n  Product {i}:")
            output.append(f"    Part Number:  {prt.get('PRTNUM')}")
            output.append(f"    Description:  {prt.get('LNGDSC', prt.get('SRTDSC', 'N/A'))}")
            output.append(f"    UPC:          {prt.get('UPCID', 'N/A')}")
            output.append(f"    Weight:       {prt.get('NETWGT', 'N/A')}")
            output.append(f"    Cube:         {prt.get('PRTCUB', 'N/A')}")
    
    if data.get('alternate_parts'):
        output.append(f"\n🔄 ALTERNATE PART NUMBERS ({len(data['alternate_parts'])} found)")
        output.append("-" * 80)
        for i, alt in enumerate(data['alternate_parts'], 1):
            output.append(f"  {i}. Type: {alt.get('ALT_PRT_TYP')}, "
                         f"Alt Part: {alt.get('ALT_PRTNUM')}, "
                         f"Base Part: {alt.get('PRTNUM')}")
    
    if data.get('lpns'):
        output.append(f"\n📦 LPNs/PALLETS ({len(data['lpns'])} found)")
        output.append("-" * 80)
        for i, lpn in enumerate(data['lpns'][:10], 1):
            output.append(f"  {i}. LPN: {lpn.get('LODNUM')}, "
                         f"Location: {lpn.get('STOLOC')}, "
                         f"Weight: {lpn.get('LODWGT')}, "
                         f"UCC: {lpn.get('LODUCC')}")
    
    if data.get('carrier'):
        output.append(f"\n🚛 CARRIER INFORMATION")
        output.append("-" * 80)
        c = data['carrier']
        output.append(f"  Carrier Code:  {c.get('CARCOD')}")
        output.append(f"  Carrier Name:  {c.get('CARNAM')}")
        output.append(f"  SCAC:          {c.get('SCAC')}")
    
    if data.get('trailer_loads'):
        output.append(f"\n🚛 TRAILER/LOAD INFO ({len(data['trailer_loads'])} records)")
        output.append("-" * 80)
        for i, tl in enumerate(data['trailer_loads'], 1):
            output.append(f"  {i}. Trailer: {tl.get('TRLR_ID')}, "
                         f"Load: {tl.get('TRLR_LOAD_ID')}")
    
    return "\n".join(output)

def analyze_query_pathways(data):
    """Analyze and recommend optimal database query pathways for Java"""
    output = []
    output.append("\n" + "="*80)
    output.append("RECOMMENDED JAVA QUERY PATHWAYS")
    output.append("="*80)
    
    output.append("""
Based on the actual data structure, here are the recommended query approaches:

┌─────────────────────────────────────────────────────────────────────────────┐
│ APPROACH 1: SINGLE COMPLEX JOIN (RECOMMENDED)                               │
└─────────────────────────────────────────────────────────────────────────────┘

Query shipment with all related data in one shot:

SELECT 
    -- Shipment header
    s.SHIP_ID, s.HOST_EXT_ID, s.SHPSTS, s.WH_ID,
    s.CARCOD, s.SRVLVL, s.DOC_NUM, s.TRACK_NUM,
    s.EARLY_SHPDTE, s.LATE_SHPDTE, s.LATE_DLVDTE,
    s.STOP_ID, s.DSTLOC, s.WAVE_SET,
    
    -- Ship-to address
    a.ADRNAM, a.ADRLN1, a.ADRLN2, a.ADRLN3,
    a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME,
    a.PHNNUM, a.ATTN_NAME,
    
    -- Line items
    sl.SHIP_LINE_ID, sl.ORDNUM, sl.ORDLIN, sl.ORDSLN,
    sl.CONS_BATCH, sl.SHPQTY, 
    sl.TOT_PLN_CAS_QTY, sl.TOT_PLN_PAL_QTY, sl.TOT_PLN_WGT,
    
    -- Product details
    ol.PRTNUM, ol.CSTPRT, ol.SALES_ORDNUM, ol.ORDQTY,
    
    -- Product master
    p.LNGDSC, p.SRTDSC, p.UPCID, p.NETWGT, p.PRTCUB,
    
    -- Carrier
    c.CARNAM, c.SCAC

FROM WMSP.SHIPMENT s
LEFT JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID
LEFT JOIN WMSP.SHIPMENT_LINE sl ON s.SHIP_ID = sl.SHIP_ID
LEFT JOIN WMSP.ORD_LINE ol ON (
    sl.ORDNUM = ol.ORDNUM AND 
    sl.ORDLIN = ol.ORDLIN AND 
    sl.ORDSLN = ol.ORDSLN AND
    sl.CLIENT_ID = ol.CLIENT_ID
)
LEFT JOIN WMSP.PRTMST p ON (
    ol.PRTNUM = p.PRTNUM AND
    ol.PRT_CLIENT_ID = p.PRT_CLIENT_ID
)
LEFT JOIN WMSP.CARMST c ON s.CARCOD = c.CARCOD

WHERE s.SHIP_ID = ?

ORDER BY sl.ORDLIN, sl.ORDSLN

✅ Pros: Single query, efficient, easy to cache
❌ Cons: Returns duplicate shipment data for each line item
⚡ Performance: ~50-200ms for typical order (5-20 lines)


┌─────────────────────────────────────────────────────────────────────────────┐
│ APPROACH 2: HIERARCHICAL QUERIES (ALTERNATIVE)                              │
└─────────────────────────────────────────────────────────────────────────────┘

Query in 3 steps with controlled result sets:

STEP 1: Get shipment header + address (1 row)
STEP 2: Get shipment lines + order details (N rows, where N = line count)
STEP 3: Get product master for unique SKUs (M rows, where M = unique SKUs)

✅ Pros: No duplicate data, cleaner result sets
❌ Cons: Multiple round trips to database
⚡ Performance: ~100-300ms (3 queries with network latency)


┌─────────────────────────────────────────────────────────────────────────────┐
│ APPROACH 3: HYBRID (BEST FOR PRODUCTION)                                    │
└─────────────────────────────────────────────────────────────────────────────┘

Query 1: Shipment header + address (1 query, 1 row)
Query 2: All line items with product details (1 query, N rows - the complex join)

This gives you:
- Clean shipment header (no duplicates)
- All line items in one query with all needed joins
- Optimal balance of queries vs data duplication

RECOMMENDED IMPLEMENTATION IN OracleDbQueryRepository:

1. findShipmentHeader(String shipmentId) 
   → Returns: Shipment with Address
   
2. findShipmentLinesWithProducts(String shipmentId)
   → Returns: List<LineItem> with all product details
   
3. findLpnsForShipment(String shipmentId, String stagingLoc)
   → Returns: List<Lpn> (if we can establish the linkage)


┌─────────────────────────────────────────────────────────────────────────────┐
│ KEY FINDINGS FOR LPN LINKAGE                                                │
└─────────────────────────────────────────────────────────────────────────────┘
""")
    
    if data.get('shipment_lines'):
        output.append("\nLPN Linkage Clues Found:")
        output.append("  • CONS_BATCH field in SHIPMENT_LINE (consolidation batch)")
        output.append("  • PCKGR1-4 fields (packing groups)")
        output.append("  • DSTLOC field in SHIPMENT (staging location)")
        output.append("\nRecommended LPN Query Strategy:")
        output.append("  1. Look for tables linking LODNUM to ORDNUM/CONS_BATCH")
        output.append("  2. Check PCKWRK_VIEW, PCKDTL, CATCH_DTL tables")
        output.append("  3. May need to query by staging location + time window")
    
    output.append("\n\n" + "="*80)
    output.append("ESTIMATED QUERY PERFORMANCE")
    output.append("="*80)
    output.append("""
Based on table sizes:
  • SHIPMENT: 38,844 rows → PK lookup: <10ms
  • ADRMST: 117,949 rows → PK lookup: <10ms
  • SHIPMENT_LINE: 320,564 rows → FK lookup: 10-50ms (for 5-20 rows)
  • ORD_LINE: Active orders → PK lookup: 10-20ms per line
  • PRTMST: Large table → PK lookup: 10-20ms per SKU

Total estimated time for complete shipment query:
  • Single query approach: 50-200ms
  • Multi-query approach: 100-300ms
  • With proper indexes: <100ms

Recommendation: Use HYBRID approach (2 queries) with connection pooling
""")
    
    return "\n".join(output)

def main():
    """Main analysis execution"""
    print("="*80)
    print("CANADIAN ORDER DEEP DIVE ANALYSIS")
    print("="*80)
    print("Purpose: Extract complete order data to simulate Java label generation")
    print("Mode: READ-ONLY (no database modifications)")
    print("="*80)
    
    # Connect to database
    config = load_config()
    print(f"\nConnecting to {config['host']}:{config['port']}/{config['service']}...")
    conn = connect_db(config)
    cursor = conn.cursor()
    print("✓ Connected successfully\n")
    
    # Create output directory
    output_dir = Path('db-dumps/canadian-analysis')
    output_dir.mkdir(parents=True, exist_ok=True)
    
    try:
        # Phase 1: Find Canadian orders
        canadian_orders = find_canadian_orders(cursor, limit=10)
        
        # Phase 2: Pick a completed order and do deep dive
        selected_order = None
        order_type = None
        
        # Prefer Canadian orders (active preferred over archived)
        if canadian_orders['by_country']:
            selected_order = canadian_orders['by_country'][0]
            order_type = "ACTIVE CANADIAN"
        elif canadian_orders['archived_canadian']:
            selected_order = canadian_orders['archived_canadian'][0]
            order_type = "ARCHIVED CANADIAN"
        elif canadian_orders['by_rossi']:
            selected_order = canadian_orders['by_rossi'][0]
            order_type = "ROSSI STAGING"
        elif canadian_orders['any_recent']:
            selected_order = canadian_orders['any_recent'][0]
            order_type = "RECENT US (DEMO)"
        
        if selected_order:
            print("\n" + "="*80)
            print(f"PHASE 2: DEEP DIVE ON {order_type} ORDER")
            print("="*80)
            
            ship_id = selected_order[0]
            print(f"\nSelected shipment: {ship_id}")
            print(f"  Customer: {selected_order[8]}")
            print(f"  City: {selected_order[9]}, {selected_order[10]}")
            print(f"  Country: {selected_order[11]}")
            print(f"  Carrier: {selected_order[3]}")
            print(f"  Status: {selected_order[2]}")
            
            # Extract ALL data
            complete_data = get_complete_shipment_data(cursor, ship_id)
            
            if complete_data:
                # Format and display
                display_output = format_data_for_display(complete_data)
                print(display_output)
                
                # Save to file
                output_file = output_dir / f'complete_order_{ship_id}.txt'
                with open(output_file, 'w', encoding='utf-8') as f:
                    f.write(display_output)
                print(f"\n✓ Complete data saved to: {output_file}")
                
                # Save raw JSON
                json_file = output_dir / f'complete_order_{ship_id}.json'
                with open(json_file, 'w', encoding='utf-8') as f:
                    json.dump(complete_data, f, indent=2, default=str)
                print(f"✓ Raw JSON saved to: {json_file}")
                
                # Analyze query pathways
                pathway_analysis = analyze_query_pathways(complete_data)
                print(pathway_analysis)
                
                # Save pathway analysis
                pathway_file = output_dir / 'query_pathway_analysis.txt'
                with open(pathway_file, 'w', encoding='utf-8') as f:
                    f.write(pathway_analysis)
                print(f"\n✓ Query pathway analysis saved to: {pathway_file}")
        
        # Phase 3: Find unshipped order
        print("\n" + "="*80)
        print("PHASE 3: ANALYZE UNSHIPPED ORDER")
        print("="*80)
        
        unshipped = find_unshipped_canadian_order(cursor)
        if unshipped:
            # Pick first unshipped order
            unship_id = unshipped[0][0]
            print(f"\nAnalyzing unshipped order: {unship_id}")
            
            # Extract data
            unship_data = get_complete_shipment_data(cursor, unship_id)
            if unship_data:
                display_output = format_data_for_display(unship_data)
                print(display_output)
                
                # Save to file
                output_file = output_dir / f'unshipped_order_{unship_id}.txt'
                with open(output_file, 'w', encoding='utf-8') as f:
                    f.write(display_output)
                print(f"\n✓ Unshipped order data saved to: {output_file}")
                
                # Save raw JSON
                json_file = output_dir / f'unshipped_order_{unship_id}.json'
                with open(json_file, 'w', encoding='utf-8') as f:
                    json.dump(unship_data, f, indent=2, default=str)
                print(f"✓ Raw JSON saved to: {json_file}")
        
        print("\n" + "="*80)
        print("ANALYSIS COMPLETE")
        print("="*80)
        print(f"\nAll output files saved to: {output_dir}")
        print("\nNext steps:")
        print("  1. Review complete_order_*.txt for full data structure")
        print("  2. Review query_pathway_analysis.txt for Java implementation")
        print("  3. Update OracleDbQueryRepository with actual SQL queries")
        print("  4. Implement mapper methods for result sets")
        
    finally:
        cursor.close()
        conn.close()
        print("\n✓ Database connection closed")

if __name__ == '__main__':
    main()
