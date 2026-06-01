Option Explicit

Private Const ITEM_SLOT_COUNT As Long = 13
Private Const MAX_DETAIL_ROW As Long = 200

Sub Count_Product()
    Dim trainWs As Worksheet
    Dim calcWs As Worksheet
    Dim countWs As Worksheet
    Dim footprints As Object
    Dim rowIdx As Long
    Dim lastRow As Long

    Set trainWs = Sheets("_TrainDetail")
    Set calcWs = Sheets("CALC")
    Set countWs = Sheets("Family_Count")
    Set footprints = BuildFootprintMap()

    trainWs.Range("A:D").Insert

    trainWs.Range("A1:D1").Value = Array("Date", "Item_1", "Item_2", "Item_3")
    trainWs.Range("A2:A" & MAX_DETAIL_ROW).NumberFormat = "mm-dd-yy"
    trainWs.Columns("A").ColumnWidth = 16
    trainWs.Columns("B:D").ColumnWidth = 10

    PrepareCalcSheet calcWs

    lastRow = trainWs.Cells(trainWs.Rows.Count, "E").End(xlUp).Row
    If lastRow < 2 Then lastRow = 2
    If lastRow > MAX_DETAIL_ROW Then lastRow = MAX_DETAIL_ROW

    For rowIdx = 2 To lastRow
        PlanTrainDetailRow trainWs, calcWs, footprints, rowIdx
    Next rowIdx

    countWs.Range("B2").Formula = "=IFERROR(SUMPRODUCT(COUNTIF(_TrainDetail!B:D,""*CAN*"")),"""")"
    countWs.Range("B3").Formula = "=IFERROR(SUMPRODUCT(COUNTIF(_TrainDetail!B:D,""*DOM*"")),"""")"
    countWs.Range("B4").Formula = "=IFERROR(SUMPRODUCT(COUNTIF(_TrainDetail!B:D,""*KEV*"")),"""")"
End Sub

Private Sub PrepareCalcSheet(ByVal calcWs As Worksheet)
    Dim slotIdx As Long
    Dim familyCol As Long

    calcWs.Range("A1:AG" & MAX_DETAIL_ROW).ClearContents

    For slotIdx = 1 To ITEM_SLOT_COUNT
        familyCol = 1 + ((slotIdx - 1) * 2)
        calcWs.Cells(1, familyCol).Value = "Item " & slotIdx
        calcWs.Cells(1, familyCol + 1).Value = "PA_Count"
    Next slotIdx

    calcWs.Range("AA1:AD1").Value = Array("Total_PA_Count", "CAN", "DOM", "KEV")
End Sub

Private Sub PlanTrainDetailRow(ByVal trainWs As Worksheet, _
                               ByVal calcWs As Worksheet, _
                               ByVal footprints As Object, _
                               ByVal rowIdx As Long)
    Dim palletCounts As Object
    Dim slotIdx As Long
    Dim itemCol As Long
    Dim qtyCol As Long
    Dim calcFamilyCol As Long
    Dim itemNumber As String
    Dim familyBucket As String
    Dim casesPerPallet As Double
    Dim caseCount As Double
    Dim palletCount As Long
    Dim totalPalletCount As Long
    Dim sortedBuckets As Variant
    Dim outIdx As Long

    Set palletCounts = CreateObject("Scripting.Dictionary")
    palletCounts.CompareMode = vbTextCompare
    palletCounts("CAN") = 0&
    palletCounts("DOM") = 0&
    palletCounts("KEV") = 0&

    trainWs.Range("B" & rowIdx & ":D" & rowIdx).ClearContents

    If Trim$(CStr(trainWs.Cells(rowIdx, "E").Value)) <> "" Then
        trainWs.Cells(rowIdx, "A").Formula = "=Inputs!C$10"
    Else
        trainWs.Cells(rowIdx, "A").ClearContents
    End If

    For slotIdx = 1 To ITEM_SLOT_COUNT
        itemCol = 10 + ((slotIdx - 1) * 2)
        qtyCol = itemCol + 1
        calcFamilyCol = 1 + ((slotIdx - 1) * 2)
        itemNumber = Trim$(CStr(trainWs.Cells(rowIdx, itemCol).Text))
        caseCount = ParseNumber(trainWs.Cells(rowIdx, qtyCol).Value)

        calcWs.Cells(rowIdx, calcFamilyCol).ClearContents
        calcWs.Cells(rowIdx, calcFamilyCol + 1).ClearContents

        If itemNumber <> "" And caseCount > 0 Then
            If TryGetFootprint(footprints, itemNumber, familyBucket, casesPerPallet) Then
                palletCount = DivideCeiling(caseCount, casesPerPallet)
                palletCounts(familyBucket) = CLng(palletCounts(familyBucket)) + palletCount
                totalPalletCount = totalPalletCount + palletCount
                calcWs.Cells(rowIdx, calcFamilyCol).Value = familyBucket
                calcWs.Cells(rowIdx, calcFamilyCol + 1).Value = palletCount
            End If
        End If
    Next slotIdx

    calcWs.Cells(rowIdx, "AA").Value = totalPalletCount
    calcWs.Cells(rowIdx, "AB").Value = palletCounts("CAN")
    calcWs.Cells(rowIdx, "AC").Value = palletCounts("DOM")
    calcWs.Cells(rowIdx, "AD").Value = palletCounts("KEV")

    sortedBuckets = SortBucketsByCount(palletCounts)
    outIdx = 0
    For slotIdx = LBound(sortedBuckets) To UBound(sortedBuckets)
        familyBucket = CStr(sortedBuckets(slotIdx))
        If CLng(palletCounts(familyBucket)) > 0 Then
            outIdx = outIdx + 1
            trainWs.Cells(rowIdx, 1 + outIdx).Value = familyBucket & ":" & CStr(palletCounts(familyBucket))
            If outIdx = 3 Then Exit For
        End If
    Next slotIdx
End Sub

Private Function BuildFootprintMap() As Object
    Dim result As Object

    Set result = CreateObject("Scripting.Dictionary")
    result.CompareMode = vbTextCompare

    If SheetExists("_Footprints") Then
        AddFootprintsFromSheet result, Sheets("_Footprints"), "A", "B", "C", 2
    End If
    AddFootprintsFromSheet result, Sheets("Item_Family"), "A", "H", "J", 2

    Set BuildFootprintMap = result
End Function

Private Sub AddFootprintsFromSheet(ByVal result As Object, _
                                   ByVal ws As Worksheet, _
                                   ByVal itemColumn As String, _
                                   ByVal familyColumn As String, _
                                   ByVal casesColumn As String, _
                                   ByVal firstRow As Long)
    Dim lastRow As Long
    Dim rowIdx As Long
    Dim itemNumber As String
    Dim familyBucket As String
    Dim casesPerPallet As Double

    lastRow = LastValueRow(ws, itemColumn, firstRow)
    For rowIdx = firstRow To lastRow
        itemNumber = Trim$(CStr(ws.Cells(rowIdx, itemColumn).Text))
        familyBucket = ClassifyFamily(ws.Cells(rowIdx, familyColumn).Value)
        casesPerPallet = ParseNumber(ws.Cells(rowIdx, casesColumn).Value)

        If itemNumber <> "" And casesPerPallet > 0 Then
            AddFootprint result, itemNumber, familyBucket, casesPerPallet
        End If
    Next rowIdx
End Sub

Private Function LastValueRow(ByVal ws As Worksheet, ByVal columnName As String, ByVal fallbackRow As Long) As Long
    Dim found As Range

    Set found = ws.Columns(columnName).Find(What:="*", _
                                           LookIn:=xlValues, _
                                           SearchOrder:=xlByRows, _
                                           SearchDirection:=xlPrevious)
    If found Is Nothing Then
        LastValueRow = fallbackRow - 1
    Else
        LastValueRow = found.Row
    End If
End Function

Private Sub AddFootprint(ByVal result As Object, _
                         ByVal itemNumber As String, _
                         ByVal familyBucket As String, _
                         ByVal casesPerPallet As Double)
    Dim payload As Variant
    Dim trimmedItem As String
    Dim noLeadingZeroItem As String

    payload = Array(familyBucket, casesPerPallet)
    trimmedItem = Trim$(itemNumber)
    If Not result.Exists(trimmedItem) Then result.Add trimmedItem, payload

    noLeadingZeroItem = TrimLeadingZeros(trimmedItem)
    If noLeadingZeroItem <> "" And Not result.Exists(noLeadingZeroItem) Then result.Add noLeadingZeroItem, payload
End Sub

Private Function TryGetFootprint(ByVal footprints As Object, _
                                 ByVal itemNumber As String, _
                                 ByRef familyBucket As String, _
                                 ByRef casesPerPallet As Double) As Boolean
    Dim key As String
    Dim payload As Variant

    key = Trim$(itemNumber)
    If Not footprints.Exists(key) Then key = TrimLeadingZeros(key)
    If Not footprints.Exists(key) Then Exit Function

    payload = footprints(key)
    familyBucket = CStr(payload(0))
    casesPerPallet = CDbl(payload(1))
    TryGetFootprint = casesPerPallet > 0
End Function

Private Function ClassifyFamily(ByVal rawFamily As Variant) As String
    Dim normalized As String

    normalized = UCase$(Trim$(CStr(rawFamily)))
    If InStr(1, normalized, "CAN", vbTextCompare) > 0 Then
        ClassifyFamily = "CAN"
    ElseIf InStr(1, normalized, "KEV", vbTextCompare) > 0 Then
        ClassifyFamily = "KEV"
    Else
        ClassifyFamily = "DOM"
    End If
End Function

Private Function DivideCeiling(ByVal dividend As Double, ByVal divisor As Double) As Long
    If dividend <= 0 Or divisor <= 0 Then
        DivideCeiling = 0
    Else
        DivideCeiling = CLng(Application.WorksheetFunction.RoundUp(dividend / divisor, 0))
    End If
End Function

Private Function SortBucketsByCount(ByVal palletCounts As Object) As Variant
    Dim buckets As Variant
    Dim i As Long
    Dim j As Long
    Dim temp As Variant

    buckets = Array("CAN", "DOM", "KEV")
    For i = LBound(buckets) To UBound(buckets) - 1
        For j = i + 1 To UBound(buckets)
            If BucketComesAfter(CStr(buckets(i)), CStr(buckets(j)), palletCounts) Then
                temp = buckets(i)
                buckets(i) = buckets(j)
                buckets(j) = temp
            End If
        Next j
    Next i

    SortBucketsByCount = buckets
End Function

Private Function BucketComesAfter(ByVal leftBucket As String, _
                                  ByVal rightBucket As String, _
                                  ByVal palletCounts As Object) As Boolean
    Dim leftCount As Long
    Dim rightCount As Long

    leftCount = CLng(palletCounts(leftBucket))
    rightCount = CLng(palletCounts(rightBucket))

    If leftCount < rightCount Then
        BucketComesAfter = True
    ElseIf leftCount = rightCount Then
        BucketComesAfter = (StrComp(leftBucket, rightBucket, vbTextCompare) > 0)
    End If
End Function

Private Function ParseNumber(ByVal rawValue As Variant) As Double
    If IsError(rawValue) Or IsEmpty(rawValue) Or Trim$(CStr(rawValue)) = "" Then
        ParseNumber = 0#
    ElseIf IsNumeric(rawValue) Then
        ParseNumber = CDbl(rawValue)
    Else
        ParseNumber = CDbl(Replace(CStr(rawValue), ",", ""))
    End If
End Function

Private Function TrimLeadingZeros(ByVal rawValue As String) As String
    Dim idx As Long
    Dim value As String

    value = Trim$(rawValue)
    idx = 1
    Do While idx < Len(value) And Mid$(value, idx, 1) = "0"
        idx = idx + 1
    Loop

    TrimLeadingZeros = Mid$(value, idx)
End Function

Private Function SheetExists(ByVal sheetName As String) As Boolean
    Dim ws As Worksheet

    On Error Resume Next
    Set ws = Sheets(sheetName)
    SheetExists = Not ws Is Nothing
    On Error GoTo 0
End Function
