import database.DBFInvoice;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import service.ZipTool;
import database.ASAConnection;
import database.InitInvoiceList;
import mail.EMailer;
import java.io.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import service.ConfigApp;

public class MainClass {
    static final Logger logger = LogManager.getLogger(MainClass.class.getName());

    public static void main(String args[]) throws Exception {
        logger.info("Programm started");

        String mailfolder=new ConfigApp().getPropValue("mailfolder")+"/out/";

        for (File myFile : new File(mailfolder).listFiles()) {
            //System.out.println(myFile.getAbsolutePath()+"/"+myFile.getName());
            if (myFile.isFile()) myFile.delete();
            logger.info(mailfolder+" cleaned");
        }

        ASAConnection dbConnect = new ASAConnection();

        EMailer eMailer = new EMailer(true);

        String invID_list; //="";
        List<File> attachments=new ArrayList<>();
        ResultSet rs;
        InitInvoiceList initInvoiceList = new InitInvoiceList(dbConnect);
        rs=initInvoiceList.GetInvoiceList();
        while (rs.next()){
            invID_list=rs.getString("list_id");
            DBFInvoice dbfInvoice = new DBFInvoice(dbConnect);
            attachments = dbfInvoice.GetDBF(invID_list);
            logger.info("Create e-invoices for "+ rs.getString("NameS")+". ## "+invID_list);
            List<File> zipAttachments=ZipTool.doZip(attachments);
            if ( zipAttachments.size()>0){
               /*for(File items: attachments){
                   items.delete();
               }*/
               if(eMailer.sendMessage(rs.getString("EMail"),"Электрнные накладные","№№ "+invID_list,zipAttachments)){
                   logger.info("отправлено сообщение для "+rs.getString("Name")+"; eMail="+rs.getString("EMail"));
                   Statement s = dbConnect.getConnection().createStatement();
                   if (s.execute("update invoice_expedit set IsENaklSend=2 where Invoice_ID in("+invID_list+"); commit;")){}
                   for(File f : zipAttachments){
                       if(f.isFile()) f.delete();
                   }
               }
            }
        }
        dbConnect.getConnection().close();
        if(attachments.size()>0) {
            System.out.println(attachments.toString());
        }
        logger.info("Program closed!!!");
    }
}
