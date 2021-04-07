package database;

import com.linuxense.javadbf.DBFDataType;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFWriter;
import service.ConfigApp;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBFInvoice {

    private static ASAConnection connection;

    public DBFInvoice(ASAConnection aConnection) {
        connection = aConnection;
    }

    public List<File> GetDBF(String invID_list) throws SQLException {
        String aDocnumber; //="";
        List<File> res=new ArrayList<>();
        ConfigApp cfg = new ConfigApp();
        Statement[] statement = new Statement[3];
        ResultSet rs_h;
        ResultSet rs_b;
        for (int i=0; i<statement.length;i++){
            statement[i]=connection.getConnection().createStatement();
        }
        File hDBF=new File(cfg.getPropValue("mailfolder") + "/out/" + "h_"
                +(invID_list.indexOf(",",0)>0 ? invID_list.substring(0,invID_list.indexOf(",",0)) : invID_list )  + ".dbf");
        File bDBF=new File(cfg.getPropValue("mailfolder") + "/out/" + "b_"
                +(invID_list.indexOf(",",0)>0 ? invID_list.substring(0,invID_list.indexOf(",",0)) : invID_list )  + ".dbf");
        File pDBF=new File(cfg.getPropValue("mailfolder") + "/out/" + "p_"
                +(invID_list.indexOf(",",0)>0 ? invID_list.substring(0,invID_list.indexOf(",",0)) : invID_list )  + ".dbf");

        DBFWriter hWriter = new DBFWriter(hDBF,Charset.forName("CP866"));
        DBFField[] hFields = initDBFFields('H');
        hWriter.setFields(hFields);

        DBFWriter bWriter = new DBFWriter(bDBF,Charset.forName("CP866"));
        DBFField[] bFields = initDBFFields('B');
        bWriter.setFields(bFields);

        DBFWriter pWriter = null;

        String sql_h="select I.ID, I.DocNumber1, I.RegDate1, I.Contractor2_ID, I.Consignee_ID, I.PayDate, I.Remark, I.FinOrder_ID, Co.Group_ID,\n" +
                "  Co1.Name Company, Co1.Address Adress, Co1.INN, isnull("+( cfg.getPropValue("contr").equals("")?"null":cfg.getPropValue("contr"))+",0) Contr, '' Order_Num, \n" +
                "  (select sum(SI.SaleStrSum*SI.Type1) from Invoice_Spec SI where SI.SpecStatus>0 and SI.invoice_id=I.ID) SaleStrSum,\n" +
                "  (if ie.isInvalid is not null then ul.name else ''  endif) SignUserLogin,  ie.Regdate SignRegDate, Mt.Name SignManagerType,\n" +
                "  GetManagerFIO(M.ID) SignUserName, ie.id SignID, I.Type_ID InvoiceTypeID \n" +
                "from Invoice I \n" +
                "  left outer join ((InvoiceEDO ie left outer join UsersEDO ue1 on ue1.id=ie.UsersEDO_id) left outer join UsersList ul on ul.id=ue1.UsersList_id) on ie.Invoice_id=I.ID and  ie.isInvalid=0\n" +
                "  left outer join (Manager M left outer join Manager_Type Mt on Mt.Id=M.Manager_Type_ID) on M.id=ul.Manager_ID\n" +
                "  , (Contractor C2 key join Company Co), (Contractor C3 key join Company Co1) where C2.ID=I.Contractor2_ID and C3.ID=I.Consignee_ID and I.ID in ("+invID_list+")";

        rs_h = statement[0].executeQuery(sql_h);
        Object hRowData[] = new Object[10];
        while (rs_h.next()){
            aDocnumber=rs_h.getString("docnumber1");
            hRowData[0]=aDocnumber;
            hRowData[1]=rs_h.getDate("Regdate1");
            hRowData[2]=rs_h.getString("INN");
            hRowData[3]=rs_h.getInt("Contr");
            hRowData[4]=505; //rs_h.getInt("Consignee_ID");
            hRowData[5]=0; //rs_h.getInt("FinOrder_ID");
            hRowData[6]=rs_h.getString("Remark");
            hRowData[7]=""; //rs_h.getString("Order_Num");
            hRowData[8]=rs_h.getString("docnumber1");
            hRowData[9]=rs_h.getDate("Regdate1");
            hWriter.addRecord(hRowData);

            String sql_b="select SI.ID, SI.Real_ID, SI.Goods_ID, G.Name, G.Firm_ID, G.Country_ID, SI.Quantity, G.Unit,\n" +
                    "  (SI.SaleMargin+SI.BayMargin) SaleMargin, SI.SaleMargin RealSaleMargin, SI.SalePriceWN, SI.SaleStrSumWN, SI.SalePrice, SI.SaleStrSum,\n" +
                    "  SI.SaleNDSSum, (SI.SaleStrSumWN - SI.StrSumWN) SaleMarginSum, SI.NDS, SI.SaleNDS, SI.NDSSum, SI.SaleNDSSum,\n" +
                    "  SI.PriceF, SI.PriceReg, SI.CCDNumber, B.RegDate2 RegDateIn,\n" +
                    "  F.Name FirmName, C.Name CountryName,\n" +
                    //        "  //(select CodeLS from Invoice_Spec where ID=SI.Real_ID) CodeLS,\n" +
                    "  LS.CodeLS, S.SpiritValue, S.SpiritExcise, S.SpiritSum,G.Spirit_Q,\n" +
                    "  (select first BAR.BARCode from BAR where BAR.NewGoods_ID=G.ID and BAR.BAR_TYPE_ID=1 and BAR.IsActive=1 order by id) BARCode,\n" +
                    "  G.IsDec, A.QInPack,\n" +
                    "  SI.PriceWN, SI.StrSumWN, SI.Price, SI.StrSum,\n" +
                    "  SI.OacMargin, SI.OacPriceWN, SI.OacStrSumWN,\n" +
                    "  isnull((select cast(O.GlobalID as char) nom  from Order_In O  key join Order_In_Spec Os  key join Order_In_Rel R where R.Invoice_Spec_ID=SI.ID),'') OrderNum,\n" +
                    "  I.Name InterName\n" +
                    "from Goods G\n" +
                    "  key left outer join Firm F\n" +
                    "  key left outer join Country C\n" +
                    "  KEY LEFT OUTER JOIN (Trade key join GenTrade key join Inter I)\n" +
                    "  key join Invoice_Spec SI\n" +
                    "  left outer join SpiritRelSpec S on S.Invoice_Spec_ID=SI.ID\n" +
                    "  left outer join (Invoice_Spec A key join Invoice B) on A.ID=SI.Real_ID\n" +
                    "  left outer join (Invoice_Spec_LS LS key join Invoice_Spec_LS_Rel RLS) on RLS.Invoice_Spec_ID=SI.ID\n" +
                    "where SI.SpecStatus>0 and SI.Invoice_ID="+rs_h.getString("ID")+"  \n" +
                    "order by G.Name";
            rs_b=statement[1].executeQuery(sql_b);
            Object bRowData[]= new Object[33];
            while (rs_b.next()){
                bRowData[0] = aDocnumber;
                bRowData[1] = rs_b.getInt("Goods_ID");
                bRowData[2] = rs_b.getString("Name");
                bRowData[3] = rs_b.getInt("Country_ID");
                bRowData[4] = rs_b.getString("CountryName");
                bRowData[5] = rs_b.getInt("Firm_ID");
                bRowData[6] = rs_b.getString("FirmName");
                bRowData[7] = rs_b.getFloat("Quantity");
                bRowData[8] = rs_b.getFloat("PriceReg");
                bRowData[9] = rs_b.getFloat("PriceF");
                bRowData[10]= rs_b.getFloat("SaleMargin");
                bRowData[11]= rs_b.getFloat("SaleMarginSum");
                bRowData[12]= rs_b.getFloat("SalePriceWN");
                bRowData[13]= rs_b.getFloat("SaleStrSumWN");
                bRowData[14]= rs_b.getFloat("SalePrice");
                bRowData[15]= rs_b.getFloat("SaleStrSum");
                bRowData[16]= rs_b.getFloat("SaleNDS");
                bRowData[17]= rs_b.getFloat("SaleNDSSum");
                bRowData[18]= 0; //rs_b.getFloat("N5");
                bRowData[19]= 0; //rs_b.getFloat("N5Sum");
                bRowData[20]= rs_b.getString("CCDNumber");
                bRowData[32]= rs_b.getString("BARCode");
                String seriesSql="select  B.ID, B.Series_ID, B.Invoice_Spec_ID, SI.ID RealInvoice_Spec_ID,\n" +
                        "  (if B.Invoice_Spec_ID=SI.ID then B.Quantity else cast(0 as tQuantity) endif) Quantity,\n" +
                        "  A.Series, A.BestBeforeDate, A.Analysis, A.Goods_ID, CCQ.Name,\n" +
                        "  A.AnalysisValidity, A.AnalysisGiveOut_ID, A.DoM, Co.Name Declarator,\n" +
                        "  (if A.IsNoReg=1 then 'Без регистарции' else Sa.AnalysisChel endif) AnalysisChel,\n" +
                        "  (if A.IsNoReg=1 then null else Sa.AnalysisDateReg endif) AnalysisDateReg,\n" +
                        "  (if B.Invoice_Spec_ID=SI.Real_ID then cast(1 as bit) else cast(0 as bit) endif) IsFromIn,\n" +
                        "  A.DeclatationDate,SI_A.*\n" +
                        "from Invoice_Spec SI\n" +
                        "  join (SeriesRelSpec B key join Series A) on\n" +
                        "    B.Invoice_Spec_ID=(if exists(select C.ID from SeriesRelSpec C where C.Invoice_Spec_ID=SI.ID) then SI.ID else SI.Real_ID endif)\n" +
                        "  left outer join CCQ on CCQ.ID = A.AnalysisGiveOut_ID\n" +
                        "  left outer join SeriesAnalysis Sa ON Sa.Series_Id=A.ID and Sa.Region_ID=GetConsigneeRegionID(SI.Invoice_ID)\n" +
                        "       and ((Sa.Invoice_Id is null and GetConsigneeRegionID(SI.Invoice_ID) in(1,2)) or\n" +
                        "       (Sa.Region_ID=GetConsigneeRegionID(SI.Invoice_ID) and Sa.Invoice_Id=(select Invoice_Id from Invoice_Spec si3 where si3.id=SI.Real_ID)))\n" +
                        "  left outer join Company Co ON Co.Id=A.Declarator_Id\n" +
                        "  ,lateral (\n" +
                        "             select list(S.Series,'^') series2 ,list(DATEFORMAT(isnull(S.BestBeforeDate,current date),'dd.mm.yyyy'),'^') BestBeforeDate2, list(isnull(S.Analysis,' - '),'^') Analysis2,list(isnull(Co.Name,' - '),'^') Declarator2,\n" +
                        "                     list(isnull(CCQ.Name,' - '),'^') Name2,list(DATEFORMAT(isnull(S.AnalysisValidity,current date),'dd.mm.yyyy'),'^') AnalysisValidity2, list(S.AnalysisGiveOut_ID,'^') AnalysisGiveOut_ID2, list(DATEFORMAT(isnull(S.DoM,current date),'dd.mm.yyyy'),'^') DoM2\n" +
                        "                 from  Invoice_Spec_Series ISS \n" +
                        "                        join Series S on S.id=(select first id from Series where goods_id=ISS.goods_id and series=ISS.series order by id desc ) \n" +
                        "                        left outer join CCQ on CCQ.ID = S.AnalysisGiveOut_ID   \n" +
                        "                        left outer join Company Co ON Co.Id=S.Declarator_Id\n" +
                        "                where ISS.invoice_id=isnull((select first UnionInvoice_ID\n" +
                        "                                               from UnionInvoice_Spec\n" +
                        "                                               join Invoice_Spec on Invoice_Spec.Invoice_ID=UnionInvoice_Spec.Invoice_ID and Invoice_Spec.goods_ID=ISS.Goods_ID\n" +
                        "                                              where UnionInvoice_Spec.Invoice_ID=SI.invoice_ID),SI.invoice_ID)\n" +
                        " and SI.goods_id=ISS.goods_id ) SI_A\n" +
                        "where si.id="+rs_b.getString("ID");
                ResultSet seriesRS = statement[2].executeQuery(seriesSql);
                while (seriesRS.next()){
                    bRowData[21]= seriesRS.getDate("BestBeforeDate");
                    bRowData[22]= seriesRS.getDate("DoM");
                    bRowData[23]= seriesRS.getString("Series");
                    bRowData[24]= seriesRS.getString("Analysis");
                    bRowData[25]= seriesRS.getDate("AnalysisDateReg");
                    bRowData[26]= seriesRS.getDate("AnalysisValidity");
                    bRowData[27]= seriesRS.getInt("AnalysisGiveOut_ID");
                    bRowData[28]= seriesRS.getString("Declarator");
                    bRowData[29]= seriesRS.getString("AnalysisChel");
                    bRowData[30]= seriesRS.getDate("DeclatationDate");
                    bRowData[31]= seriesRS.getInt("ID");
                }
                bWriter.addRecord(bRowData);
                if(rs_b.getInt("IsDec")==1){
                    String pSQL="SELECT ID, Number, CompanyName, MarginP, Margin, Price, Invoice_Spec_ID,\n" +
                                "  GetContractorName(1) pName\n" +
                                "FROM Invoice_spec_GNVLS\n" +
                                "WHERE Invoice_Spec_ID="+rs_b.getString("ID")+"\n"+
                                "ORDER BY  Number";
                    Statement pStmt=connection.getConnection().createStatement();
                    ResultSet pRS = pStmt.executeQuery(pSQL);
                    while (pRS.next()){
                        if(!pDBF.exists()){
                            pWriter = new DBFWriter(pDBF,Charset.forName("CP866"));
                        }
                        Object pRowData[]= new Object[6];
                        pRowData[0]=pRS.getInt("Invoice_Spec_ID");
                        pRowData[1]=pRS.getInt("Number");
                        pRowData[2]=pRS.getString("CompanyName");
                        pRowData[3]= pRS.getFloat("MarginP");
                        pRowData[4]=pRS.getFloat("Margin");
                        pRowData[5]= pRS.getFloat("Price");
                        pWriter.addRecord(pRowData);
                    }
                }
            }
        }
        bWriter.close();
        res.add(bDBF);
        hWriter.close();
        res.add(hDBF);
        if(pDBF.exists()){
            pWriter.close();
            res.add(pDBF);
            System.out.println("p-File добавлен");
        }
        return res;
    }

    private DBFField[] initDBFFields(char aType){
        DBFField[] res=null;
        switch (aType){
            case 'H':
                DBFField[] hFields = new DBFField[10];
                for (int i=0; i<10;i++){
                    hFields[i]=new DBFField();
                }
                hFields[0].setName("DocNumber");
                hFields[0].setType(DBFDataType.CHARACTER);
                hFields[0].setLength(15);

                hFields[1].setName("RegDate");
                hFields[1].setType(DBFDataType.DATE);

                hFields[2].setName("Inn");
                hFields[2].setType(DBFDataType.CHARACTER);
                hFields[2].setLength(22);

                hFields[3].setName("Contr");
                hFields[3].setType(DBFDataType.NUMERIC);
                hFields[3].setLength(10);

                hFields[4].setName("Consig");
                hFields[4].setType(DBFDataType.NUMERIC);
                hFields[4].setLength(10);

                hFields[5].setName("Order");
                hFields[5].setType(DBFDataType.NUMERIC);
                hFields[5].setLength(10);

                hFields[6].setName("Remark");
                hFields[6].setType(DBFDataType.CHARACTER);
                hFields[6].setLength(100);

                hFields[7].setName("Order_Num");
                hFields[7].setType(DBFDataType.CHARACTER);
                hFields[7].setLength(200);

                hFields[8].setName("VATInv");
                hFields[8].setType(DBFDataType.CHARACTER);
                hFields[8].setLength(25);

                hFields[9].setName("VATInvDT");
                hFields[9].setType(DBFDataType.DATE);

                res=hFields;
                break;
            case 'B':
                DBFField[] bFields = new DBFField[33];
                for (int i=0; i<33;i++){
                    bFields[i]=new DBFField();
                }
                bFields[0].setName("DocNumber");
                bFields[0].setType(DBFDataType.CHARACTER);
                bFields[0].setLength(15);

                bFields[1].setName("GoodsID");
                bFields[1].setType(DBFDataType.NUMERIC);
                bFields[1].setLength(10);

                bFields[2].setName("GoodsN");
                bFields[2].setType(DBFDataType.CHARACTER);
                bFields[2].setLength(150);

                bFields[3].setName("CountryID");
                bFields[3].setType(DBFDataType.NUMERIC);
                bFields[3].setLength(10);

                bFields[4].setName("CountryN");
                bFields[4].setType(DBFDataType.CHARACTER);
                bFields[4].setLength(100);

                bFields[5].setName("FirmID");
                bFields[5].setType(DBFDataType.NUMERIC);
                bFields[5].setLength(10);

                bFields[6].setName("FirmN");
                bFields[6].setType(DBFDataType.CHARACTER);
                bFields[6].setLength(100);

                bFields[7].setName("Quantity");
                bFields[7].setType(DBFDataType.NUMERIC);
                bFields[7].setLength(15);
                bFields[7].setDecimalCount(2);

                bFields[8].setName("PriceReg");
                bFields[8].setType(DBFDataType.NUMERIC);
                bFields[8].setLength(15);
                bFields[8].setDecimalCount(2);

                bFields[9].setName("PriceF");
                bFields[9].setType(DBFDataType.NUMERIC);
                bFields[9].setLength(15);
                bFields[9].setDecimalCount(2);

                bFields[10].setName("Margin");
                bFields[10].setType(DBFDataType.NUMERIC);
                bFields[10].setLength(10);
                bFields[10].setDecimalCount(2);

                bFields[11].setName("MarginSum");
                bFields[11].setType(DBFDataType.NUMERIC);
                bFields[11].setLength(10);
                bFields[11].setDecimalCount(2);

                bFields[12].setName("PriceWN");
                bFields[12].setType(DBFDataType.NUMERIC);
                bFields[12].setLength(15);
                bFields[12].setDecimalCount(2);

                bFields[13].setName("StrSumWN");
                bFields[13].setType(DBFDataType.NUMERIC);
                bFields[13].setLength(15);
                bFields[13].setDecimalCount(2);

                bFields[14].setName("Price");
                bFields[14].setType(DBFDataType.NUMERIC);
                bFields[14].setLength(15);
                bFields[14].setDecimalCount(2);

                bFields[15].setName("StrSum");
                bFields[15].setType(DBFDataType.NUMERIC);
                bFields[15].setLength(15);
                bFields[15].setDecimalCount(2);

                bFields[16].setName("NDS");
                bFields[16].setType(DBFDataType.NUMERIC);
                bFields[16].setLength(10);
                bFields[16].setDecimalCount(2);

                bFields[17].setName("NDSSum");
                bFields[17].setType(DBFDataType.NUMERIC);
                bFields[17].setLength(15);
                bFields[17].setDecimalCount(2);

                bFields[18].setName("N5");
                bFields[18].setType(DBFDataType.NUMERIC);
                bFields[18].setLength(10);
                bFields[18].setDecimalCount(2);

                bFields[19].setName("N5Sum");
                bFields[19].setType(DBFDataType.NUMERIC);
                bFields[19].setLength(15);
                bFields[19].setDecimalCount(2);

                bFields[20].setName("CCDNumber");
                bFields[20].setType(DBFDataType.CHARACTER);
                bFields[20].setLength(25);

                bFields[21].setName("BestBefore");
                bFields[21].setType(DBFDataType.DATE);

                bFields[22].setName("DoM");
                bFields[22].setType(DBFDataType.DATE);

                bFields[23].setName("Series");
                bFields[23].setType(DBFDataType.CHARACTER);
                bFields[23].setLength(20);

                bFields[24].setName("Analysis");
                bFields[24].setType(DBFDataType.CHARACTER);
                bFields[24].setLength(20);

                bFields[25].setName("AnalysisDR");
                bFields[25].setType(DBFDataType.DATE);

                bFields[26].setName("AnalysisV");
                bFields[26].setType(DBFDataType.DATE);

                bFields[27].setName("AnalysisID");
                bFields[27].setType(DBFDataType.NUMERIC);
                bFields[27].setLength(10);

                bFields[28].setName("AnalysisN");
                bFields[28].setType(DBFDataType.CHARACTER);
                bFields[28].setLength(100);

                bFields[29].setName("Analysin");
                bFields[29].setType(DBFDataType.CHARACTER);
                bFields[29].setLength(20);

                bFields[30].setName("AnalysinDR");
                bFields[30].setType(DBFDataType.DATE);

                bFields[31].setName("PField1");
                bFields[31].setType(DBFDataType.NUMERIC);
                bFields[31].setLength(18);

                bFields[32].setName("BARCode");
                bFields[32].setType(DBFDataType.CHARACTER);
                bFields[32].setLength(13);
                res=bFields;
                break;
            case 'P':
                DBFField[] pFields = new DBFField[33];
                for (int i=0; i<6;i++){
                    pFields[i]=new DBFField();
                }
                pFields[0].setName("PField1");
                pFields[0].setType(DBFDataType.NUMERIC);
                pFields[0].setLength(18);

                pFields[1].setName("Number");
                pFields[1].setType(DBFDataType.NUMERIC);
                pFields[1].setLength(8);
                pFields[1].setDecimalCount(0);

                pFields[2].setName("Company");
                pFields[2].setType(DBFDataType.CHARACTER);
                pFields[2].setLength(240);

                pFields[3].setName("MarginP");
                pFields[3].setType(DBFDataType.NUMERIC);
                pFields[3].setLength(15);
                pFields[3].setDecimalCount(2);

                pFields[4].setName("Margin");
                pFields[4].setType(DBFDataType.NUMERIC);
                pFields[4].setLength(15);
                pFields[4].setDecimalCount(2);

                pFields[5].setName("Price");
                pFields[5].setType(DBFDataType.NUMERIC);
                pFields[5].setLength(10);
                pFields[5].setDecimalCount(2);
        }
    return  res;
    }
}
