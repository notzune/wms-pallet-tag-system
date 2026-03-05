Option Explicit

Public TrainID As String
Sub RunReports()

TrainID = Sheets("Inputs").Range("E5")
If TrainID = "" Then
    MsgBox ("Please enter a Train ID.")
    Exit Sub
End If

Call CleanupRailTempSheets
    Sheets.Add After:=Sheets(Sheets.Count)
    ActiveSheet.Name = "Working"
    Sheets.Add After:=Sheets(Sheets.Count)
    ActiveSheet.Name = "_TrainDetail"
    Sheets.Add After:=Sheets(Sheets.Count)
    ActiveSheet.Name = "_Footprints"

Call RefreshData
Call ApplyTrainDetailFormatting
Call RefreshFootprints
Call Count_Product
Sheets("Working").Delete

If Sheets("Code").Visible = True Then Sheets("Code").Visible = xlVeryHidden

Sheets("_TrainDetail").Select
Range("A1").Select
End Sub
