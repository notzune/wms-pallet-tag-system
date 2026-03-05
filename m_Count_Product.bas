Option Explicit

Sub Count_Product()

Sheets("_TrainDetail").Columns(1).Insert
Sheets("_TrainDetail").Columns(1).Insert
Sheets("_TrainDetail").Columns(1).Insert
Sheets("_TrainDetail").Columns(1).Insert


Sheets("_TrainDetail").Range("A2:A200").Formula = "=IF(E2>"""",Inputs!C$10,"""")"
Sheets("_TrainDetail").Range("A2:A200").NumberFormat = "mm-dd-yy"
Sheets("_TrainDetail").Range("B2:B200").Formula = "=IF(INDEX(CALC!$Z2:$AB2,MATCH(LARGE(CALC!$Z2:$AB2,1),CALC!$Z2:$AB2,0))=0,"""",CONCATENATE(IF((LARGE(CALC!$Z2:$AB2,1))=0,"""",(INDEX(CALC!$Z$1:$AB$1,MATCH(LARGE(CALC!$Z2:$AB2,1),CALC!$Z2:$AB2,0)))),"":"",IF((LARGE(CALC!$Z2:$AB2,1))=0,"""",(INDEX(CALC!$Z2:$AB2,MATCH(LARGE(CALC!$Z2:$AB2,1),CALC!$Z2:$AB2,0))))))"
Sheets("_TrainDetail").Range("C2:C200").Formula = "=IF(INDEX(CALC!$Z2:$AB2,MATCH(LARGE(CALC!$Z2:$AB2,2),CALC!$Z2:$AB2,0))=0,"""",CONCATENATE(IF((LARGE(CALC!$Z2:$AB2,2))=0,"""",(INDEX(CALC!$Z$1:$AB$1,MATCH(LARGE(CALC!$Z2:$AB2,2),CALC!$Z2:$AB2,0)))),"":"",IF((LARGE(CALC!$Z2:$AB2,2))=0,"""",(INDEX(CALC!$Z2:$AB2,MATCH(LARGE(CALC!$Z2:$AB2,2),CALC!$Z2:$AB2,0))))))"
Sheets("_TrainDetail").Range("D2:D200").Formula = "=IF(INDEX(CALC!$Z2:$AB2,MATCH(LARGE(CALC!$Z2:$AB2,3),CALC!$Z2:$AB2,0))=0,"""",CONCATENATE(IF((LARGE(CALC!$Z2:$AB2,3))=0,"""",(INDEX(CALC!$Z$1:$AB$1,MATCH(LARGE(CALC!$Z2:$AB2,3),CALC!$Z2:$AB2,0)))),"":"",IF((LARGE(CALC!$Z2:$AB2,3))=0,"""",(INDEX(CALC!$Z2:$AB2,MATCH(LARGE(CALC!$Z2:$AB2,3),CALC!$Z2:$AB2,0))))))"






Sheets("_TrainDetail").Range("A1") = Application.Transpose(Array("Date"))
Sheets("_TrainDetail").Range("B1") = Application.Transpose(Array("Item_1"))
Sheets("_TrainDetail").Range("C1") = Application.Transpose(Array("Item_2"))
Sheets("_TrainDetail").Range("D1") = Application.Transpose(Array("Item_3"))

Sheets("_TrainDetail").Columns("A").ColumnWidth = 16
Sheets("_TrainDetail").Columns("B").ColumnWidth = 10
Sheets("_TrainDetail").Columns("C").ColumnWidth = 10
Sheets("_TrainDetail").Columns("D").ColumnWidth = 10




Sheet3.Range("A2:A200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!J2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!J2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("B2:B200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!K2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!J2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!J2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("C2:C200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!L2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!L2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("D2:D200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!M2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!L2,_Footprints!$A:$A,0)),INDEX(Item_Family!$J$2:$J$977,MATCH(_TrainDetail!L2,Item_Family!$A$2:$A$977,0)))),0),"""")"
Sheet3.Range("E2:E200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!N2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!N2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("F2:F200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!O2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!N2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!N2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("G2:G200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!P2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!P2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("H2:H200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!Q2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!P2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!P2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("I2:I200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!R2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!R2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("J2:J200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!S2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!R2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!R2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("K2:K200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!T2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!T2,Item_Family!A$2:A$977,0)),""""))    "
Sheet3.Range("L2:L200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!U2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!T2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!T2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("M2:M200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!V2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!V2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("N2:N200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!W2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!V2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!V2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("O2:O200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!X2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!X2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("P2:P200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!Y2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!X2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!X2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("Q2:Q200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!Z2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!Z2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("R2:R200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!AA2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!Z2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!Z2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("S2:S200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!AB2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!AB2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("T2:T200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!AC2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!AB2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!AB2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("U2:U200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!AD2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!AD2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("V2:V200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!AE2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!AD2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!AD2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("W2:W200").Formula = "=IFERROR(INDEX(_Footprints!$B:$B,MATCH(_TrainDetail!AF2,_Footprints!$A:$A,0)),IFERROR(INDEX(Item_Family!H$2:H$977,MATCH(_TrainDetail!AF2,Item_Family!A$2:A$977,0)),""""))"
Sheet3.Range("X2:X200").Formula = "=IFERROR(ROUNDUP((_TrainDetail!AG2)/(IFERROR(INDEX(_Footprints!$C:$C,MATCH(_TrainDetail!AF2,_Footprints!$A:$A,0)),INDEX(Item_Family!J$2:J$977,MATCH(_TrainDetail!AF2,Item_Family!A$2:A$977,0)))),0),"""")"
Sheet3.Range("Y2:Y200").Formula = "=SUM(B2,D2,F2,H2,J2,L2,N2,P2,R2,T2,V2,X2)"
Sheet3.Range("Z2:Z200").Formula = "=IF(C2=Z$1,D2,0)+IF(E2=Z$1,F2,0)+IF(G2=Z$1,H2,0)+IF(I2=Z$1,J2,0)+IF(K2=Z$1,L2,0)+IF(M2=Z$1,N2,0)+IF(O2=Z$1,P2,0)+IF(Q2=Z$1,R2,0)+IF(S2=Z$1,T2,0)+IF(U2=Z$1,V2,0)+IF(W2=Z$1,X2,0)+IF(A2=Z$1,B2,0)"
Sheet3.Range("AC2:AC200").Formula = "=IFERROR(Z2/Y2,"""")"
Sheet3.Range("AA2:AA200").Formula = "=IF(C2=AA$1,D2,0)+IF(E2=AA$1,F2,0)+IF(G2=AA$1,H2,0)+IF(I2=AA$1,J2,0)+IF(K2=AA$1,L2,0)+IF(M2=AA$1,N2,0)+IF(O2=AA$1,P2,0)+IF(Q2=AA$1,R2,0)+IF(S2=AA$1,T2,0)+IF(U2=AA$1,V2,0)+IF(W2=AA$1,X2,0)+IF(A2=AA$1,B2,0)"
Sheet3.Range("AD2:AD200").Formula = "=IFERROR(AA2/Y2,"""")"
Sheet3.Range("AB2:AB200").Formula = "=IF(C2=AB$1,D2,0)+IF(E2=AB$1,F2,0)+IF(G2=AB$1,H2,0)+IF(I2=AB$1,J2,0)+IF(K2=AB$1,L2,0)+IF(M2=AB$1,N2,0)+IF(O2=AB$1,P2,0)+IF(Q2=AB$1,R2,0)+IF(S2=AB$1,T2,0)+IF(U2=AB$1,V2,0)+IF(W2=AB$1,X2,0)+IF(A2=AB$1,B2,0)"
Sheet3.Range("AE2:AE200").Formula = "=IFERROR(AB2/Y2,"""")"



Sheet4.Range("B2").Formula = "=IFERROR(SUMPRODUCT(COUNTIF(_TrainDetail!B:D,""*CAN*"")),"""")"
Sheet4.Range("B3").Formula = "=IFERROR(SUMPRODUCT(COUNTIF(_TrainDetail!B:D,""*DOM*"")),"""")"
Sheet4.Range("B4").Formula = "=IFERROR(SUMPRODUCT(COUNTIF(_TrainDetail!B:D,""*KEV*"")),"""")"




End Sub
