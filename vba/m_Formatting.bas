Option Explicit

Sub ApplyTrainDetailFormatting()
Dim lrow As Long
lrow = Cells(Rows.Count, 2).End(xlUp).Row
Range("A1:I" & lrow).Select
Selection.AutoFilter

'sort
Selection.Sort Key1:=Range("E2"), Order1:=xlAscending, key2:=Range("H2"), Order2:=xlDescending, Header:=xlYes, _
        OrderCustom:=1, MatchCase:=False, Orientation:=xlTopToBottom, DataOption1:=xlSortNormal



'combine CLUB
'Selection.AutoFilter Field:=9, Criteria1:="<>"
'    If Cells(Rows.Count, 1).End(xlUp).Row > 1 Then Range("G2:G" & lrow).SpecialCells(xlCellTypeVisible).Value = "COSTCO"
'Selection.AutoFilter Field:=9
Columns(9).Delete

Dim lcol As Long
'flatten to 1 row per order
Application.ScreenUpdating = False
Dim ws As Worksheet
Dim rowIdx As Long
Dim lastDataRow As Long

Set ws = ActiveSheet
rowIdx = 2
lastDataRow = ws.Cells(ws.Rows.Count, 3).End(xlUp).Row

' Preserve original combine logic, but iterate by index so processing does not stop early
' when an intermediate blank appears in column C.
Do While rowIdx < lastDataRow
    If Trim(CStr(ws.Cells(rowIdx, 3).Value)) <> "" _
       And Trim(CStr(ws.Cells(rowIdx, 3).Value)) = Trim(CStr(ws.Cells(rowIdx + 1, 3).Value)) Then
        lcol = ws.Cells(rowIdx, ws.Columns.Count).End(xlToLeft).Column + 1
        ws.Range(ws.Cells(rowIdx + 1, 7), ws.Cells(rowIdx + 1, 8)).Cut ws.Cells(rowIdx, lcol)
        ws.Rows(rowIdx + 1).Delete
        lastDataRow = lastDataRow - 1
    Else
        rowIdx = rowIdx + 1
    End If
Loop

Columns("G:G").Insert

'formatting
    Range("F1").FormulaR1C1 = "ITEM_NBR 1"
    Range("G1").FormulaR1C1 = "TOTAL_CS ITM 1"
    Range("H1").FormulaR1C1 = "ITEM_NBR 2"
    Range("I1").FormulaR1C1 = "TOTAL_CS ITM 2"
    Range("J1").FormulaR1C1 = "ITEM_NBR 3"
    Range("K1").FormulaR1C1 = "TOTAL_CS ITM 3"
    Range("L1").FormulaR1C1 = "ITEM_NBR 4"
    Range("M1").FormulaR1C1 = "TOTAL_CS ITM 4"
    Range("N1").FormulaR1C1 = "ITEM_NBR 5"
    Range("O1").FormulaR1C1 = "TOTAL_CS ITM 5"
    Range("P1").FormulaR1C1 = "ITEM_NBR 6"
    Range("Q1").FormulaR1C1 = "TOTAL_CS ITM 6"
    Range("R1").FormulaR1C1 = "ITEM_NBR 7"
    Range("S1").FormulaR1C1 = "TOTAL_CS ITM 7"
    Range("T1").FormulaR1C1 = "ITEM_NBR 8"
    Range("U1").FormulaR1C1 = "TOTAL_CS ITM 8"
    Range("V1").FormulaR1C1 = "ITEM_NBR 9"
    Range("W1").FormulaR1C1 = "TOTAL_CS ITM 9"
    Range("X1").FormulaR1C1 = "ITEM_NBR 10"
    Range("Y1").FormulaR1C1 = "TOTAL_CS ITM 10"
    Range("Z1").FormulaR1C1 = "ITEM_NBR 11"
    Range("AA1").FormulaR1C1 = "TOTAL_CS ITM 11"
    Range("AB1").FormulaR1C1 = "ITEM_NBR 12"
    Range("AC1").FormulaR1C1 = "TOTAL_CS ITM 12"
    Range("AD1").FormulaR1C1 = "ITEM_NBR 13"
    Range("AE1").FormulaR1C1 = "TOTAL_CS ITM 13"
    
    ActiveWindow.Zoom = 90
    Cells.Font.Name = "Tahoma"
    Cells.Font.Size = 9
    
    Rows(1).Font.Bold = True
    Rows(1).VerticalAlignment = xlCenter
    Rows(1).RowHeight = 25
        
    Cells.Select
    Selection.ColumnWidth = 40
    Columns("F:AE").ColumnWidth = 16
    Cells.EntireColumn.AutoFit
    
    Columns("G:AF").HorizontalAlignment = xlCenter
    Columns("E:F").HorizontalAlignment = xlCenter
    Columns(2).HorizontalAlignment = xlCenter
    
    ActiveSheet.AutoFilterMode = False
    ActiveWindow.DisplayGridlines = False
    lrow = Cells(Rows.Count, 1).End(xlUp).Row
    
        With Range("A1:AE" & lrow)
            .Borders.ThemeColor = 1
            .Borders.TintAndShade = -0.14996795556505
            .Borders.Weight = xlThin
        End With
            
        With Range("A1:AE1")
            .Borders(xlEdgeBottom).ColorIndex = 1
            .Borders(xlEdgeBottom).Weight = xlThin
            .Borders(xlEdgeTop).ColorIndex = 1
            .Borders(xlEdgeTop).Weight = xlThin
            .Interior.Pattern = xlSolid
            .Interior.PatternColorIndex = xlAutomatic
            .Interior.ThemeColor = xlThemeColorDark1
            .Interior.TintAndShade = -0.149998474074526
            .Interior.PatternTintAndShade = 0
        End With
        
    Columns("AF:ZZ").Delete Shift:=xlToLeft

    Range("A1").Select
    ActiveSheet.UsedRange

End Sub
