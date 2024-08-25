package models;

import java.util.*;
import java.text.SimpleDateFormat;

public class RolesRecordsReportEntryDate implements Comparable<RolesRecordsReportEntryDate> {

    public Date startDate;
    public Date endDate;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");

    public RolesRecordsReportEntryDate(Date date) {
        startDate = date;
        endDate = date;
    }

    public String formatStartDate() {
        return dateFormat.format(startDate);
    }

    public String formatEndDate() {
        return dateFormat.format(endDate);
    }

    @Override
    public int compareTo(RolesRecordsReportEntryDate d) {
        return endDate.compareTo(d.endDate);
    }
}
