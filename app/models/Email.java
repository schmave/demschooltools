package models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.persistence.*;

import javax.mail.*;
import javax.mail.internet.*;
import org.codehaus.jackson.annotate.*;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@javax.persistence.Entity
public class Email extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_id_seq")
    public Integer id;

    public String message;
	public boolean sent;
	public boolean deleted;

    public static Finder<Integer, Email> find = new Finder(
        Integer.class, Email.class
    );

	public static Email create(String message) {
		Email e = new Email();
		e.message = message;
		e.save();
		return e;
	}

    @Transient
    public MimeMessage parsedMessage;

    public void markSent() {
        this.sent = true;
		this.save();
    }
	
	public void markDeleted() {
		this.deleted = true;
		this.save();
	}

    public void parseMessage() {
        try {
			// Get system properties
			Properties properties = new Properties();

			// Setup mail server
			properties.setProperty("mail.smtp.host", "smtp.mandrillapp.com");
			properties.setProperty("mail.smtp.port", "587");

			properties.setProperty("mail.smtp.auth", "true");
			Authenticator authenticator = new Authenticator();
			properties.setProperty("mail.smtp.submitter", authenticator.getPasswordAuthentication().getUserName());

			// Get the default Session object.
			Session session = Session.getInstance(properties, new Authenticator());
			// session.setDebug(true);
			parsedMessage = new MimeMessage(session, new ByteArrayInputStream(message.getBytes()));
			
			for (Enumeration e = parsedMessage.getAllHeaders(); e.hasMoreElements() ;) {
				Header h = (Header)e.nextElement();
				if (!h.getName().toLowerCase().equals("content-type") &&
					!h.getName().toLowerCase().equals("subject")) {
					parsedMessage.removeHeader(h.getName());
				}
			}
			parsedMessage.saveChanges();
        }
		catch (MessagingException e) {
			e.printStackTrace();
		}
    }
}

 class Authenticator extends javax.mail.Authenticator {
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication("schmave@gmail.com", "uQeL5cx3hLXLzRznlP1YYA");
	}
}