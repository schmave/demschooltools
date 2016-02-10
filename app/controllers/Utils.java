package controllers;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import models.Person;

public class Utils
{
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
