package controllers;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.persistence.*;

import com.fasterxml.jackson.databind.*;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.OrgConfig;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import play.Configuration;
import play.Play;
import play.twirl.api.Html;
import play.twirl.api.HtmlFormat;

public class Utils
{
    static MustacheFactory sMustache = new DefaultMustacheFactory();

    static ExecutorService sCustodiaService = Executors.newFixedThreadPool(1);

    public static Calendar parseDateOrNow(String date_string) {
        Calendar result = new GregorianCalendar();

        try {
            Date parsed_date = new SimpleDateFormat("yyyy-M-d").parse(date_string);
            if (parsed_date != null) {
                result.setTime(parsed_date);
            }
        } catch (ParseException  e) {
            System.out.println("Failed to parse given date (" + date_string + "), using current");
        }

        return result;
    }

    public static void eatIfUniqueViolation(PersistenceException pe) throws PersistenceException {
        SQLException sqlEx = null;
        PersistenceException persistEx = pe;

        while (persistEx != null && sqlEx == null) {
            Throwable cause = persistEx.getCause();
            if (cause instanceof SQLException) {
                sqlEx = (SQLException) cause;
            } else if (cause instanceof PersistenceException) {
                persistEx = (PersistenceException) cause;
            } else {
                break;
            }
        }

        if (sqlEx == null) {
            throw pe;
        }

        if (!sqlEx.getSQLState().equals("23505")) {
            throw pe;
        } else {
            System.out.println("Ate a 23505 error");
        }
    }

    // day_of_week is, e.g., Calendar.TUESDAY
    public static void adjustToPreviousDay(Calendar date, int day_of_week) {
        int dow = date.get(Calendar.DAY_OF_WEEK);
        if (dow >= day_of_week) {
            date.add(GregorianCalendar.DATE, -(dow - day_of_week));
        } else {
            date.add(GregorianCalendar.DATE, day_of_week - dow - 7);
        }
    }

    public static String toJson(Object o) {
        ObjectMapper m = new ObjectMapper();
        m.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        // Something like this should work, but I can't get it to work now.
        // If you fix this, head to edit_attendance_week.scala.html and
        // simplify it a bit.
        //SimpleModule module = new SimpleModule("MyMapKeySerializerModule",
        //    new Version(1, 0, 0, null));
        //
        //module.addKeySerializer(Person.class, new PersonKeySerializer());
        //m = m.registerModule(module);
        //
        try
        {
            return m.writeValueAsString(o);
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String renderMustache(String templateName, Object scopes) {
        StringWriter writer = new StringWriter();
        sMustache.compile(
            new InputStreamReader(
                Play.application().resourceAsStream("public/mustache/" + templateName)),
            templateName)
            .execute(writer, scopes);
        return writer.toString();
    }

    public static Html newlineToBr(String input) {
        return HtmlFormat.raw(HtmlFormat.escape(input.trim()).body().replace("\n", "<br/>"));
    }

    public static boolean getBooleanFromFormValue(String form_value) {
        return form_value != null && (form_value.equals("true") || form_value.equals("on"));
    }

    private static void makeCustodiaPost(CloseableHttpClient client, String url, List<NameValuePair> data) {
        CloseableHttpResponse response = null;
        try {
            try {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(new UrlEncodedFormEntity(data));
                response = client.execute(httpPost);
                HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loginToCustodia(CloseableHttpClient client, Configuration play_config) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("username", "admin"));
        nvps.add(new BasicNameValuePair("password", play_config.getString("custodia_password")));
        makeCustodiaPost(client, play_config.getString("custodia_url") + "/users/login", nvps);
    }

    public static void setCustodiaPassword(String new_password) {
        final OrgConfig config = OrgConfig.get();
        final Configuration play_config = Application.getConfiguration();
        sCustodiaService.submit(new Runnable() {
            public void run() {
                try {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    loginToCustodia(httpclient, play_config);

                    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                    nvps.add(new BasicNameValuePair("username", config.org.short_name));
                    nvps.add(new BasicNameValuePair("password", new_password));
                    makeCustodiaPost(httpclient,
                            play_config.getString("custodia_url") + "/users/password",
                            nvps);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void updateCustodia() {
        final Configuration play_config = Application.getConfiguration();
        sCustodiaService.submit(new Runnable() {
            public void run() {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                loginToCustodia(httpclient, play_config);

                makeCustodiaPost(httpclient,
                        play_config.getString("custodia_url") + "/updatefromdst",
                        new ArrayList<NameValuePair>());
            }
        });
    }
}

//class PersonKeySerializer extends JsonSerializer<Person>
//{
//    @Override
//    public void serialize(Person p, JsonGenerator jgen, SerializerProvider provider)
//        throws IOException, JsonProcessingException
//    {
//        jgen.writeFieldName(String.valueOf(p.person_id));
//    }
//}
