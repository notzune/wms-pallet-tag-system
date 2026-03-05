Option Explicit

Sub RefreshFootprints()
    Dim code As Variant
    Dim code1 As Variant

    code1 = Sheets("Code").Range("A2")
    code = Replace(code1, "TRAINIDINPUT", TrainID)

    ' Populate train-specific short-code footprint map from WMS so pallet math
    ' does not depend on stale Item_Family rows.
    code = "SELECT src.alt_prtnum AS SHORT_CODE, " & _
           "CASE " & _
           " WHEN src.can_flag = 1 THEN '*CAN*' " & _
           " WHEN src.kev_flag = 1 THEN 'KEV' " & _
           " ELSE 'DOM' " & _
           "END AS ITEM_FAMILY, " & _
           "src.case_pa AS CASE_PA " & _
           "FROM ( " & _
           " SELECT ap.alt_prtnum, " & _
           "  MAX(CASE WHEN (UPPER(NVL(p.prtfam, '')) LIKE '%CAN%' OR NVL(p.uc_pars_flg, 0) = 1) THEN 1 ELSE 0 END) AS can_flag, " & _
           "  MAX(CASE WHEN UPPER(NVL(p.prtfam, '')) LIKE '%KEV%' THEN 1 ELSE 0 END) AS kev_flag, " & _
           "  MAX(CASE WHEN d.pal_flg = 1 THEN d.untqty END) AS case_pa " & _
           " FROM trlr t " & _
           " JOIN rcvtrk rt ON rt.trlr_id = t.trlr_id " & _
           " JOIN rcvlin rl ON rl.trknum = rt.trknum " & _
           " JOIN alt_prtmst ap ON ap.prtnum = rl.prtnum AND ap.alt_prt_typ = 'UPC' " & _
           " LEFT JOIN prtmst p ON p.prtnum = ap.prtnum AND p.prt_client_id = ap.prt_client_id " & _
           " LEFT JOIN prtftp pf ON pf.prtnum = ap.prtnum AND pf.prt_client_id = ap.prt_client_id AND pf.defftp_flg = 1 " & _
           " LEFT JOIN prtftp_dtl d ON d.prtnum = pf.prtnum AND d.prt_client_id = pf.prt_client_id AND d.wh_id = pf.wh_id AND d.ftpcod = pf.ftpcod " & _
           " WHERE t.vc_train_num = '" & TrainID & "' " & _
           " GROUP BY ap.alt_prtnum " & _
           ") src " & _
           "WHERE src.case_pa IS NOT NULL " & _
           "ORDER BY src.alt_prtnum"

    Sheets("_Footprints").Cells.Clear
    Sheets("_Footprints").Range("A1").Resize(1, 3).Value = Array("SHORT_CODE", "ITEM_FAMILY", "CASE_PA")

    Sheets("_Footprints").Select
    On Error Resume Next
    With ActiveSheet.ListObjects.Add(SourceType:=0, _
        Source:=Array(Array("ODBC;DSN=TBG3002;UID=RPTADM;PWD=Report_Password12!@#;DBA=W;APA=T;EXC=F;FEN=T;QTO=T;FRC=10;FDL=10;LOB=T;RST=T;BTD=F;BNF=F;BAM=IfAllSucc"), _
        Array("essful;NUM=NLS;DPM=F;MTS=F;MDI=F;CSR=F;FWC=F;FBS=64000;TLO=0;MLD=0;ODA=F;"), _
        Array("cessful;MTS=F;MDI=F;CSR=F;FWC=F;PFC=10;TLO=0;"), Array("")), Destination:=Range("$A$1")).QueryTable
        .CommandText = code
        .ListObject.DisplayName = "Footprints"
        .Refresh BackgroundQuery:=False
    End With
    On Error GoTo 0
End Sub
