package controllers;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
}

