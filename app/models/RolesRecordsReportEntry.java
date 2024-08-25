package models;

import java.util.*;

public class RolesRecordsReportEntry implements Comparable<RolesRecordsReportEntry> {

    public String roleName;
    public List<RolesRecordsReportEntryDate> dates = new ArrayList<RolesRecordsReportEntryDate>();

    public RolesRecordsReportEntry(String roleName) {
        this.roleName = roleName;
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
    public int compareTo(RolesRecordsReportEntry e) {
        return getFinalDate().compareTo(e.getFinalDate());
    }
}
