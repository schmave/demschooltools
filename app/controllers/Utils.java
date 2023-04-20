package controllers;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.inject.Singleton;
import javax.persistence.*;

import com.fasterxml.jackson.databind.*;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.typesafe.config.Config;
import models.OrgConfig;
import models.Organization;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import play.data.Form;
import play.twirl.api.Html;
import play.twirl.api.HtmlFormat;

@Singleton
public class Utils
{
    static MustacheFactory sMustache = new DefaultMustacheFactory();
    static ExecutorService sCustodiaService = Executors.newSingleThreadExecutor();

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
                Public.sEnvironment.resourceAsStream("public/mustache/" + templateName)),
            templateName)
            .execute(writer, scopes);
        return writer.toString();
    }

    public static Html newlineToBr(String input) {
        return HtmlFormat.raw(HtmlFormat.escape(input.trim()).body().replace("\n", "<br/>"));
    }

    public static boolean getBooleanFromFormValue(String value) {
        return value.equals("true") || value.equals("on");
    }

    public static boolean getBooleanFromFormValue(Form.Field field) {
        return field.value().filter(Utils::getBooleanFromFormValue).isPresent();
    }

    public static boolean lessThanDaysOld(Date date, int num_days) {
        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        long one_day = 24 * 60 * 60 * 1000;
        return diff < one_day * num_days;
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

    private static void loginToCustodia(CloseableHttpClient client, Config play_config) {
        Config school_crm_config = play_config.getConfig("school_crm");
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("username", "admin"));
        nvps.add(new BasicNameValuePair("password",
                school_crm_config.getString(
                        "custodia_password")));
        makeCustodiaPost(client, school_crm_config.getString("custodia_url") + "/users/login", nvps);
    }

    public static void setCustodiaPassword(String new_password, Organization org) {
        final OrgConfig config = OrgConfig.get(org);
        sCustodiaService.submit(() -> {
            try {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                loginToCustodia(httpclient, Public.sConfig);

                List<NameValuePair> nvps = new ArrayList<>();
                nvps.add(new BasicNameValuePair("username", config.org.short_name));
                nvps.add(new BasicNameValuePair("password", new_password));
                makeCustodiaPost(httpclient,
                        Public.sConfig.getString("custodia_url") + "/users/password",
                        nvps);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateCustodia() {
        sCustodiaService.submit(() -> {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            loginToCustodia(httpclient, Public.sConfig);

            makeCustodiaPost(httpclient,
                    Public.sConfig.getString("custodia_url") + "/updatefromdst",
                    new ArrayList<>());
        });
    }

    public static Date localNow(OrgConfig orgConfig) {
        Date utcNow = new Date();
        return new Date(utcNow.getTime() + orgConfig.time_zone.getOffset(utcNow.getTime()));
    }
}
