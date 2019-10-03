package com.example.mailapp;

import java.security.Security;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

public class MailChecker extends javax.mail.Authenticator {
    private String mailhost;
    private String host;
    private String protocol;
    private String file;
    private String user;
    private String password;

    private Session session;
    private Store store;
    private Folder folder;
    private URLName url;

    static {
        Security.addProvider(new JSSEProvider());
    }

    public MailChecker(String user, String password) {
        this.user = user;
        this.password = password;
        this.host = "imap";
        this.protocol = "imaps";
        this.file = "INBOX";

        String[] tempHost = user.split("@");
        mailhost = host + "." + tempHost[1];

        Properties props = createDefaultCheckerProps();
        session = Session.getInstance(props, this);
        this.url = new URLName(protocol, mailhost, 993, file, this.user, this.password);
    }

    private Properties createDefaultCheckerProps() {
        Properties props = new Properties();
        props.setProperty("mail.host", mailhost);
        props.put("mail.imap.auth", "true");
        props.put("mail.imap.port", "993");
        props.put("mail.imap.socketFactory.port", "993");
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.imap.quitwait", "false");
        return props;
    }

    public void login() throws Exception {
        this.store = session.getStore(url);
        store.connect();
        this.folder = store.getFolder(url);
        folder.open(Folder.READ_WRITE);
    }

    public boolean isLoggedIn() {
        return store.isConnected();
    }

    public void logout() throws MessagingException {
        folder.close(false);
        store.close();
        store = null;
        session = null;
    }

    public int getMessageCount() {
        int messageCount = 0;
        try {
            messageCount = folder.getMessageCount();
        } catch (MessagingException me) {
            me.printStackTrace();
        }
        return messageCount;
    }

    public Message[] getMessages() throws MessagingException {
        return folder.getMessages();
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }
}
