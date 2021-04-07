package mail;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import service.ConfigApp;

public class EMailer {
    private Message message = null;
    private Session session;
    private Folder inbox;
    private ConfigApp cfg = new ConfigApp();
    private Properties props=new Properties();

    public EMailer(final boolean useSSL){
        //props.put("mail.mime.charset", "windows-1251");
        props.put("mail.smtp.host", cfg.getPropValue("smtpsrv"));
        props.put("mail.smtp.port", cfg.getPropValue("smtpport")  );
        props.put("mail.smtp.auth", "true"     );
        props.put("mail.store.protocol" , "pop3"  );
        props.put("mail.pop3.host",cfg.getPropValue("pop3srv"));
        props.put("mail.pop3.port",cfg.getPropValue("pop3port"));
        if (useSSL) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.pop3.socketFactory.fallback", "false");
            props.setProperty("mail.pop3.socketFactory.port", cfg.getPropValue("pop3port"));
        }
    }

    public boolean sendMessage (final String aEmailTo, final String aSubject, final String text, List<File> attachments){
        boolean result = false;
        Authenticator auth = new EmailAuthenticator(cfg.getPropValue("mailuser"),cfg.getPropValue("mailpwd"));
        session = Session.getDefaultInstance(props,auth);
        session.setDebug(false);
        try {
            InternetAddress email_from = new InternetAddress(cfg.getPropValue("mailfrom"));
            InternetAddress email_to   = new InternetAddress(aEmailTo);
            message = new MimeMessage(session);
            message.setFrom(email_from);
            message.setRecipient(Message.RecipientType.TO, email_to);
            message.setSubject(aSubject);
            // Содержимое сообщения
            Multipart mmp = new MimeMultipart();
            // Текст сообщения
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(text, "text/plain; charset=utf-8");
            mmp.addBodyPart(bodyPart);
            // Вложение файла в сообщение
            if (attachments != null) {
                for(File aFile:attachments) {
                    MimeBodyPart mbr = createFileAttachment(aFile);
                    mmp.addBodyPart(mbr);
                }
            }
            // Определение контента сообщения
            message.setContent(mmp);
            // Отправка сообщения
            Transport.send(message);
            result = true;
        } catch (MessagingException e){
            // Ошибка отправки сообщения
            System.err.println(e.getMessage());
        }
        return result;
    }

    private MimeBodyPart createFileAttachment(File attch) throws MessagingException{
        // Создание MimeBodyPart
        MimeBodyPart mbp = new MimeBodyPart();
        // Определение файла в качестве контента
        FileDataSource fds = new FileDataSource(attch);
        mbp.setDataHandler(new DataHandler(fds));
        mbp.setFileName(fds.getName());
        return mbp;
    }

    public  void recieve() {
        Authenticator auth = new EmailAuthenticator(cfg.getPropValue("reciveuser"),cfg.getPropValue("recivepwd"));
        Session session = Session.getDefaultInstance(props, auth);
        session.setDebug(false);
        //String path="";
        try {
            Store store = session.getStore();

            // Подключение к почтовому серверу
            store.connect(cfg.getPropValue("pop3srv"), cfg.getPropValue("reciveuser"),cfg.getPropValue("recivepwd"));

            // Папка входящих сообщений
            inbox = store.getFolder("INBOX");

            // Открываем папку в режиме только для чтения
            inbox.open(Folder.READ_WRITE);

            System.out.println("Количество сообщений : " +
                    String.valueOf(inbox.getMessageCount()));
            if (inbox.getMessageCount() == 0)
                return;
            // Последнее сообщение; первое сообщение под номером 1
            message = inbox.getMessage(inbox.getMessageCount());
            if (message.isMimeType("multipart/*")){
                Multipart mp = (Multipart) message.getContent();

                // Вывод содержимого в консоль
                for (int i = 0; i < mp.getCount(); i++){
                    BodyPart  bp = mp.getBodyPart(i);
                    if (bp.getFileName() == null)
                        System.out.println("    " + i + ". сообщение : '" +
                                bp.getContent() + "'");
                    else {
                        Address[] froms =message.getFrom();
                        System.out.println("    " + i + ". файл : '" +
                                (javax.mail.internet.MimeUtility.decodeWord(bp.getFileName())) + "'");
                        System.out.println("    " + i + ". ОТ : '" +
                                ((InternetAddress)froms[0]).getAddress() + "'");
                        File savedir = new File(cfg.getPropValue("mailfolder")+((InternetAddress)froms[0]).getAddress() );
                        if (!savedir.exists()) {
                            savedir.mkdirs();
                        }
                        File savefile = new File(savedir+"/"+(javax.mail.internet.MimeUtility.decodeWord(bp.getFileName())));
                        if (!savefile.exists()) {
                            saveFile(savefile, bp);
                        }
                    }
                }
            }
            deleteMsg(message.getMessageNumber());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static int saveFile(File saveFile, Part part) throws Exception {
        BufferedOutputStream bos = new BufferedOutputStream( new
                FileOutputStream(saveFile) );
        byte[] buff = new byte[2048];
        InputStream is = part.getInputStream();
        int ret = 0, count = 0;
        while( (ret = is.read(buff)) > 0 ){
            bos.write(buff, 0, ret);
            count += ret;
        }
        bos.close();
        is.close();
        return count;
    }

    private void deleteMsg(int msgNumber) throws MessagingException{
        //Message msgDel[] = {message};
        inbox.setFlags(1,msgNumber,new Flags(Flags.Flag.DELETED), true);
        inbox.close(true);
        inbox.open(Folder.READ_WRITE);
        message = inbox.getMessage(msgNumber);
        System.out.println("Удаление письма №"+msgNumber+" прошло успешно");
    }
}
