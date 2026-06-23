package com.tbg.wms.cli.gui.analyzers.openloads;

final class OpenLoadsSql {
    private static final String QUERY = """
            select ssv.wh_id,ssv.car_move_id,pickers.ordnum,ssv.ship_id,case when t.live_load_flg = 1 then 'LIVE' else 'DROP' end d_l,ssv.carcod,ssv.trlr_num,ssv.yard_loc,dm.lngdsc shpsts
            ,appt.start_dte appt,shd.dstloc,am.adrnam customer,ch.carnam
            ,pickers.casepicks,pickers.case_picks_comp,case when pickers.casepicks <= 0 then 0 else pickers.casepicks-case_picks_comp end picks_rem
            ,(select max(uc_sug_asset_typ) from ord_line ol where ol.ordnum = pickers.ordnum) platform
            ,a.shp_dck_flg,t.trlr_cod,ano.nottxt
            ,(SELECT COUNT(DISTINCT lodnum) FROM inventory_view,locmst,aremst
            WHERE ship_line_id IN (SELECT ship_line_id FROM shipment_line WHERE ship_id = ssv.ship_id)
            AND inventory_view.stoloc = locmst.stoloc AND inventory_view.wh_id = locmst.wh_id
            AND locmst.arecod = aremst.arecod AND aremst.wh_id = locmst.wh_id
            AND aremst.stgflg = 1 ) staged
            ,ssv.stop_seq,shorts.short
             from ship_struct_view ssv,adrmst am,appt,dscmst dm,carhdr ch,trlr t,wh,ord o,locmst l,aremst a,appt_note ano
             ,shp_dst_loc shd,
            (select ords.ordnum ship_id,case when dem > inv then 'SHORT' end short from
            (select ordnum,sum(ordqty) dem from ord_line group by ordnum) ords,
            (select ship_id,sum(untqty) inv from inventory_view iv,shipment_line sl
            where iv.ship_line_id = sl.ship_line_id group by ship_id) invs
            where ords.ordnum = invs.ship_id(+)
            and ords.dem > invs.inv) shorts,
             (select ordpicks.ordnum,ordpicks.ship_id,sum(picks) casepicks,sum(compicks) case_picks_comp,max(ordpicks.i_hold) i_hold
            from
            (select cp.ordnum,cp.ship_id,nvl(sum(cp.partial_qty),0) picks
            ,nvl(sum(cp.comp_qty),0) compicks
            ,max(inv_hold) i_hold
            from
            (select ol.ordnum,sl.ship_id,ol.ordlin,ol.ordqty
            ,case when mod(ol.ordqty,pd.untqty) <> 0 then mod(ol.ordqty,pd.untqty)else 0 end partial_qty
            ,sum(pv.appqty) comp_qty
            ,max(ol.uc_inv_hld_flg) inv_hold
            from ord_line ol,(select * from pckwrk_view where pckwrk_view.adddte > trunc(sysdate) - 7
            and pckqty <> to_number(substr(ftpcod,1,instr(ftpcod,'X')-1))) pv,prtftp_dtl pd,shipment_line sl,prtmst pm,prtftp
            where
            sl.ordsln = ol.ordsln
            and sl.ordlin = ol.ordlin
            and sl.ordnum = ol.ordnum
            and ol.prtnum = pm.prtnum
            and ol.wh_id = pm.wh_id_tmpl
            and ol.ordnum = pv.ordnum(+)
            and ol.ordlin = pv.ordlin(+)
            and ol.ordsln = pv.ordsln(+)
            and ol.prtnum = pd.prtnum
            and pd.uomcod = 'PA'
            and pd.prtnum = prtftp.prtnum
            and pd.ftpcod = prtftp.ftpcod
            and pd.wh_id = prtftp.wh_id
            and prtftp.defftp_flg = 1
            and ol.wh_id = pd.wh_id
            group by
             ol.ordnum,sl.ship_id,ol.ordlin,ol.ordqty
            ,case when mod(ol.ordqty,pd.untqty) <> 0 then mod(ol.ordqty,pd.untqty)else 0 end
              ) cp
            group by cp.ordnum,cp.ship_id) ordpicks
            group by ordpicks.ordnum,ordpicks.ship_id) pickers
             where o.st_adr_id = am.adr_id
             and ssv.appt_id = appt.appt_id
             and dm.colnam = 'shpsts'
             and ssv.shpsts <> 'C'
             and ssv.ship_id = shd.ship_id (+)
             and dm.colval = ssv.shpsts
             and ch.carcod(+) = ssv.carcod
             and ssv.wh_id = wh.wh_id
             and ssv.ship_id = o.ordnum
             and ssv.trlr_id = t.trlr_id(+)
             and ssv.ship_id = pickers.ship_id(+)
             and ssv.yard_loc = l.stoloc(+)
             and l.arecod = a.arecod(+)
             and appt.appt_id = ano.appt_id(+)
             and ano.notlin(+) = '0'
             and ssv.ship_id = shorts.ship_id(+)
            order by appt,car_move_id,stop_seq
            """;

    private OpenLoadsSql() {
    }

    static String query() {
        return QUERY;
    }
}
