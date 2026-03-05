Option Explicit

Sub Delete()
Application.ScreenUpdating = False

On Error Resume Next
    If Sheets("Working").Visible = False Then Sheets("Working").Visible = True
    If Sheets("_TrainDetail").Visible = False Then Sheets("_TrainDetail").Visible = True
    If Sheets("_Footprints").Visible = False Then Sheets("_Footprints").Visible = True
    Application.DisplayAlerts = False
    Sheets("Working").Delete
    Sheets("_TrainDetail").Delete
    Sheets("_Footprints").Delete
  On Error GoTo 0

Sheets("Inputs").Select
End Sub
