package mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class EmailAuthenticator extends Authenticator {
    private String login;
    private String password;
    public EmailAuthenticator(String smtpAuthUser, String smtpAuthPwd) {
        this.login = smtpAuthUser;
        this.password = smtpAuthPwd;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(login,password);
    }
}
