^XA
^FX ═════════════════════════════════════════════════════════════════════
^FX Walmart Canada Pallet Shipping Label - TBG3002 (Jersey City)
^FX Grid Layout with Bordered Cells - 4x6 label at 203 DPI
^FX ═════════════════════════════════════════════════════════════════════

^FX Font and margin setup
^LL812
^PW812

^FX Top margin
^FO10,20

^FX ═════════════════════════════════════════════════════════════════════
^FX SECTION 1: SHIP FROM / SHIP TO (Two-column header)
^FX ═════════════════════════════════════════════════════════════════════

^FX Outer box for header section
^FO20,30^GB772,160,2^FS

^FX Vertical divider between Ship From and Ship To
^FO410,30^GB2,160,2^FS

^FX SHIP FROM label and content (left column)
^FO30,40^A0N,18,18^FDSHIP FROM:^FS
^FO30,62^A0N,16,16^FD{shipFromName}^FS
^FO30,82^A0N,13,13^FD{shipFromAddress}^FS
^FO30,98^A0N,13,13^FD{shipFromCityStateZip}^FS

^FX SHIP TO label and content (right column)
^FO420,40^A0N,18,18^FDSHIP TO:^FS
^FO420,62^A0N,16,16^FD{shipToName}^FS
^FO420,82^A0N,13,13^FD{shipToAddress1}^FS
^FO420,98^A0N,13,13^FD{shipToCity}, {shipToState} {shipToZip}^FS
^FO420,114^A0N,13,13^FD{shipToCountry}^FS

^FX ═════════════════════════════════════════════════════════════════════
^FX SECTION 2: ORDER DETAILS (Four-column grid)
^FX ═════════════════════════════════════════════════════════════════════

^FX Outer box for order details
^FO20,200^GB772,80,2^FS

^FX Vertical dividers (at 25%, 50%, 75%)
^FO213,200^GB2,80,2^FS
^FO410,200^GB2,80,2^FS
^FO607,200^GB2,80,2^FS

^FX Horizontal divider (row separator)
^FO20,230^GB772,2,2^FS

^FX Column 1: P.O. NUMBER
^FO30,210^A0N,12,12^FDP.O. NUMBER^FS
^FO30,240^A0N,16,16^FD{customerPo}^FS

^FX Column 2: CARRIER MOVE
^FO225,210^A0N,12,12^FDCARRIER MOVE^FS
^FO225,240^A0N,16,16^FD{carrierCode}^FS

^FX Column 3: LOCATION NO
^FO420,210^A0N,12,12^FDLOCATION NO^FS
^FO420,240^A0N,16,16^FD{locationNumber}^FS

^FX Column 4: STOP
^FO617,210^A0N,12,12^FDSTOP^FS
^FO617,240^A0N,16,16^FD{stopSequence}^FS

^FX ═════════════════════════════════════════════════════════════════════
^FX SECTION 3: ITEM DETAILS (Two-column grid)
^FX ═════════════════════════════════════════════════════════════════════

^FX Outer box for item details
^FO20,290^GB772,70,2^FS

^FX Vertical divider at 50%
^FO410,290^GB2,70,2^FS

^FX Column 1: Walmart Item Number
^FO30,300^A0N,14,14^FDWAL-MART ITEM #: {walmartItemNumber}^FS

^FX Column 2: TBG SKU
^FO420,300^A0N,14,14^FDTBG SKU: {tbgSku}^FS

^FX Item Description (full width below)
^FO30,330^A0N,13,13^FD{itemDescription}^FS

^FX ═════════════════════════════════════════════════════════════════════
^FX SECTION 4: SSCC BARCODE (Full width)
^FX ═════════════════════════════════════════════════════════════════════

^FX Barcode outer box
^FO20,370^GB772,150,2^FS

^FX SSCC label
^FO30,380^A0N,16,16^FDSSCC:^FS

^FX Barcode rendering (Code 128 format for SSCC-18)
^FX Position centered horizontally on label
^FO180,410^BY3,3,100^BCN,100,Y,N,N^FD{ssccBarcode}^FS

^FX ═════════════════════════════════════════════════════════════════════
^FX SECTION 5: PALLET SEQUENCE (Bottom right)
^FX ═════════════════════════════════════════════════════════════════════

^FO650,790^A0N,28,28^FD{palletSeq} OF {palletTotal}^FS

^FX ═════════════════════════════════════════════════════════════════════
^FX Label end marker
^FX ═════════════════════════════════════════════════════════════════════

^XZ

