package database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InitInvoiceList {

    private ASAConnection connection;

    public InitInvoiceList(ASAConnection aConnection){
        connection=aConnection;
    }
    public ResultSet GetInvoiceList() throws SQLException {
        Statement statement=connection.getConnection().createStatement();
        ResultSet resultSet=statement.executeQuery("select Ce.eInvoice_type, Ce.eInvoice_tran, Ce.DecimalDelimiter, Co.EMail, Co.MailCode, C.ID Contractor_ID, Co.PriceFormat, GetContractorName(C.ID) Name,\n" +
                " list(I.ID) list_id, list(I.DocNumber1) list_DocNumber, GetContractorNameS(C.ID) NameS,\n" +
                        " list((if S.IsLgot=1 then I.DocNumber1 else null endif)) list_DLODocNumber,\n" +
                        " isnull(Ce.DBF_level, 7) DBF_level, Co.ID CoID\n" +
                        "from Company Co key join Contractor C \n" +
                        "  left join Contractor_DD CDD on CDD.contractor_id=C.id and CDD.isActive=1 \n" +
                        "  left outer join Company_eInvoice Ce on Ce.Company_ID=C.Company_Id,\n" +
                        "    invoice_expedit IE key join invoice I\n" +
                        "  left outer join StoreForPrices S on S.Contractor_ID=I.Contractor1_ID\n" +
                        "where I.Consignee_ID=C.ID and I.type_id=2 and\n" +
                        " isnull(S.IsLgot,0)=0 and \n" +
                        "I.DocStatus=1 and IE.IsENaklSend<2 and  (CDD.id is null or (CDD.id is not null and IE.printTTN=1)) and \n"+
                        " Ce.eInvoice_type is not null and Ce.eInvoice_tran is not null and\n" +
                        "((Ce.eInvoice_tran=5 and Co.EMail is not null and Co.EMail<>'') or\n" +
                        "(Ce.eInvoice_tran in(6,7,8,9,10,11,12,13,14)))  \n" +
                        "group by Ce.eInvoice_type, Ce.eInvoice_tran, Ce.DecimalDelimiter, Co.PriceFormat, Co.EMail, Co.MailCode, Contractor_ID,DBF_level, Co.ID");
        return resultSet;
    }
}
