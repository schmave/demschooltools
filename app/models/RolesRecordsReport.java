package models;

import java.util.*;
import play.data.*;

public class RolesRecordsReport {

    public Integer personId;
    public Person person;
    public List<RolesRecordsReportEntry> entries;

    public static RolesRecordsReport create(Form<RolesRecordsReport> form, Organization org) {
        RolesRecordsReport report = form.get();
        report.runReport(org);
        return report;
    }

    public void runReport(Organization org) {
        entries = new ArrayList<RolesRecordsReportEntry>();
        if (personId == null) {
            return;
        }
        person = Person.findById(personId, org);
        if (person == null) {
            return;
        }
        Map<String, List<RoleRecord>> groups = groupRecordsByRole();
        for (Map.Entry<String, List<RoleRecord>> group : groups.entrySet()) {
            RolesRecordsReportEntry entry = new RolesRecordsReportEntry(group.getKey());
            entry.dates = groupRecordsByDate(group.getValue());
            entries.add(entry);
        }
        Collections.sort(entries, Collections.reverseOrder());
    }

    private Map<String, List<RoleRecord>> groupRecordsByRole() {
        Map<String, List<RoleRecord>> groups = new HashMap<String, List<RoleRecord>>();
        for (RoleRecordMember m : person.getRoleRecordMembers()) {
            Role role = m.getRecord().getRole();
            if (!groups.containsKey(role.getName())) {
                groups.put(role.getName(), role.getRecords());
            }
        }
        return groups;
    }

    private List<RolesRecordsReportEntryDate> groupRecordsByDate(List<RoleRecord> records) {
        List<RolesRecordsReportEntryDate> groups = new ArrayList<RolesRecordsReportEntryDate>();
        RolesRecordsReportEntryDate currentGroup = null;
        RolesRecordsReportEntryDate previousGroup = null;
        Collections.sort(records);
        for (RoleRecord record : records) {
            boolean isPersonPresent = record.getMembers().stream().filter(m -> m.getPerson().equals(person)).findFirst().isPresent();
            if (isPersonPresent) {
                if (currentGroup == null) {
                    if (previousGroup != null && areDateMonthsEqual(previousGroup.endDate, record.getDateCreated())) {
                        // As of this record, the person has this role now and they had it earlier in the same
                        // month, but there was a period in the middle of the month when they didn't have it,
                        // so we ignore the intermediate period by continuing the previous date group
                        currentGroup = previousGroup;
                        currentGroup.endDate = record.getDateCreated();
                    }
                    else {
                        // As of this record, the person has this role but did not previously have it,
                        // so we start a new date group
                        currentGroup = new RolesRecordsReportEntryDate(record.getDateCreated());
                        groups.add(currentGroup);
                    }
                }
                else {
                    // As of this record, the person has this role and already had it before,
                    // so we just update the end date of the date group
                    currentGroup.endDate = record.getDateCreated();
                }
            }
            else if (currentGroup != null) {
                // As of this record, the person had this role before but does not have it anymore,
                // so we update the end date of the date group and then terminate it
                currentGroup.endDate = record.getDateCreated();
                previousGroup = currentGroup;
                currentGroup = null;
            }
            // If we get here, the person did not already have the role and still doesn't have it,
            // so there's nothing to do
        }
        if (currentGroup != null) {
            // The final group was not terminated, so that means the person still has this role as of today
            currentGroup.endDate = new Date();
        }
        Collections.sort(groups, Collections.reverseOrder());
        return groups;
    }

    private boolean areDateMonthsEqual(Date a, Date b) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(a);
        int monthA = cal.get(Calendar.MONTH);
        cal.setTime(b);
        int monthB = cal.get(Calendar.MONTH);
        return monthA == monthB;
    }
}
