package models;

import java.util.*;

public class RolesRecordsReportEntryDate implements Comparable<RolesRecordsReportEntryDate> {

    @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
    public Date startDate;

    @play.data.format.Formats.DateTime(pattern = "MM/dd/yyyy")
    public Date endDate;

    public RolesRecordsReportEntryDate(Date date) {
        startDate = date;
        endDate = date;
    }

    @Override
    public int compareTo(RolesRecordsReportEntryDate d) {
        return endDate.compareTo(d.endDate);
    }
}
