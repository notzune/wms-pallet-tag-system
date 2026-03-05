Option Explicit

Sub RefreshData()

Application.ScreenUpdating = False
Dim code As Variant
Dim code1 As Variant
code1 = Sheets("Code").Range("A2")
code = Replace(code1, "TRAINIDINPUT", TrainID)


     Sheets("Working").Select
        On Error Resume Next
            With ActiveSheet.ListObjects.Add(SourceType:=0, Source:=Array(Array("ODBC;DSN=TBG3002;UID=RPTADM;PWD=Report_Password12!@#;DBA=W;APA=T;EXC=F;FEN=T;QTO=T;FRC=10;FDL=10;LOB=T;RST=T;BTD=F;BNF=F;BAM=IfAllSucc"), Array("essful;NUM=NLS;DPM=F;MTS=F;MDI=F;CSR=F;FWC=F;FBS=64000;TLO=0;MLD=0;ODA=F;"), Array("cessful;MTS=F;MDI=F;CSR=F;FWC=F;PFC=10;TLO=0;"), Array("")), Destination:=Range("$B$2")).QueryTable
                .CommandText = code
                .ListObject.DisplayName = "Table"
                .Refresh BackgroundQuery:=False
            End With
      
If Err.Number = 0 Then
        ActiveSheet.ListObjects("Table").TableStyle = ""
        Application.GoTo ActiveSheet.ListObjects("Table").Range
             
        If Range("B65536").End(xlUp).Row <> 2 Then
            Selection.Copy
            Sheets("_TrainDetail").Select
            Range("A1").Select
            Selection.PasteSpecial Paste:=xlPasteValues
            Selection.PasteSpecial Paste:=xlFormats
       End If
End If

If Err.Number <> 0 Or Cells(Rows.Count, 1).End(xlUp).Row = 1 Then
    Call Delete
    Sheets("Inputs").Select
    Application.ScreenUpdating = True
    MsgBox ("Error executing report.  Confirm you are using a valid Train ID.")
    End
End If
    
    
  
End Sub



