package models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.*;

import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.message.*;
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
		return e;
	}

    @Transient
    public StringBuffer textBody;
    @Transient
    public StringBuffer htmlBody;

    @Transient
    public Message parsedMessage;

    @Transient
    public ArrayList<BodyPart> attachments;

    public void markSent() {
        this.sent = true;
		this.save();
    }
	
	public void markDeleted() {
		this.deleted = true;
		this.save();
	}

    public void parseMessage() {
        textBody = new StringBuffer();
        htmlBody = new StringBuffer();
        attachments = new ArrayList<BodyPart>();

        try {
            parsedMessage = new DefaultMessageBuilder().parseMessage(new ByteArrayInputStream(message.getBytes()));

            //If message contains many parts - parse all parts
            if (parsedMessage.isMultipart()) {
                Multipart multipart = (Multipart) parsedMessage.getBody();
                parseBodyParts(multipart);
            } else {
                //If it's single part message, just get text body
                String text = getTxtPart(parsedMessage);
                textBody.append(text);
            }
        } catch (IOException ex) {
            ex.fillInStackTrace();
        }
    }

    /**
     * This method classifies bodyPart as text, html or attached file
     */
    private void parseBodyParts(Multipart multipart) throws IOException {
        for (org.apache.james.mime4j.dom.Entity p : multipart.getBodyParts()) {
            BodyPart part = (BodyPart) p;
            if (part == null) {
                continue;
            }
            if (part.isMimeType("text/plain")) {
                String txt = getTxtPart(part);
                textBody.append(txt);
            } else if (part.isMimeType("text/html")) {
                String html = getTxtPart(part);
                htmlBody.append(html);
            } else if (part.getDispositionType() != null && !part.getDispositionType().equals("")) {
                //If DispositionType is null or empty, it means that it's multipart, not attached file
                attachments.add(part);
            }

            //If current part contains other, parse it again by recursion
            if (part.isMultipart()) {
                parseBodyParts((Multipart) part.getBody());
            }
        }
    }

    private String getTxtPart(org.apache.james.mime4j.dom.Entity part) throws IOException {
        //Get content from body
        TextBody tb = (TextBody) part.getBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tb.writeTo(baos);
        return new String(baos.toByteArray());
    }
}
