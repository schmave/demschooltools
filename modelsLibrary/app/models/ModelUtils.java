package models;

import play.data.Form;

import javax.persistence.PersistenceException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ModelUtils {
    public static boolean getBooleanFromFormValue(String value) {
        return value.equals("true") || value.equals("on");
    }

    public static boolean getBooleanFromFormValue(Form.Field field) {
        return field.value().filter(ModelUtils::getBooleanFromFormValue).isPresent();
    }

    public static void adjustToPreviousDay(Calendar date, int day_of_week) {
        int dow = date.get(Calendar.DAY_OF_WEEK);
        if (dow >= day_of_week) {
            date.add(GregorianCalendar.DATE, -(dow - day_of_week));
        } else {
            date.add(GregorianCalendar.DATE, day_of_week - dow - 7);
        }
    }

    public static String forDateInput(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }
    public static String formatAsPercent(double d) {
        return String.format("%,.1f", d * 100) + "%";
    }

    public static String formatDayOfWeek(Date d) {
        return new SimpleDateFormat("EE").format(d);
    }

    public static Date getDateFromString(String date_string) {
        if (!date_string.equals("")) {
            try
            {
                return new SimpleDateFormat("yyyy-MM-dd").parse(date_string);
            } catch (ParseException e) {
                return null;
            }
        } else {
            return null;
        }
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

    public static Date getStartOfYear() {
        return getStartOfYear(new Date());
    }

    public static Date getStartOfYear(Date d) {
        Date result = (Date)d.clone();

        if (result.getMonth() < 7) { // july or earlier
            result.setYear(result.getYear() - 1);
        }
        result.setMonth(Calendar.AUGUST);
        result.setDate(1);

        result.setHours(0);
        result.setMinutes(0);
        result.setSeconds(0);

        return result;
    }
}
