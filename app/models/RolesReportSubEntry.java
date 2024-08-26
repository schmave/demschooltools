package models;

import java.util.*;

public class RolesReportSubEntry implements Comparable<RolesReportSubEntry> {

    public String description;
    public List<RolesReportDate> dates;

    public RolesReportSubEntry(String description, List<RolesReportDate> dates) {
        this.description = description;
        this.dates = dates;
    }

    public Date getFinalDate() {
        Date date = new Date();
        if (dates.size() > 0) {
            Collections.sort(dates, Collections.reverseOrder());
            date = dates.get(0).endDate;
        }
        return date;
    }

    @Override
    public int compareTo(RolesReportSubEntry e) {
        return getFinalDate().compareTo(e.getFinalDate());
    }
}
