package models;

import java.util.*;

public class RolesReportEntry implements Comparable<RolesReportEntry> {

    public String roleName;
    public List<RolesReportSubEntry> subEntries;

    public RolesReportEntry(String roleName, List<RolesReportSubEntry> subEntries) {
        this.roleName = roleName;
        this.subEntries = subEntries;
    }

    public Date getFinalDate() {
        Date date = new Date();
        if (subEntries.size() > 0) {
            Collections.sort(subEntries, Collections.reverseOrder());
            date = subEntries.get(0).getFinalDate();
        }
        return date;
    }

    public int getTotalDates() {
        int total = 0;
        for (RolesReportSubEntry subEntry : subEntries) {
            total += subEntry.dates.size();
        }
        return total;
    }

    @Override
    public int compareTo(RolesReportEntry e) {
        return getFinalDate().compareTo(e.getFinalDate());
    }
}
