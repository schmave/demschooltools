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
        Map<String, List<RoleRecord>> groups = groupRecordsByRole(org);
        for (Map.Entry<String, List<RoleRecord>> group : groups.entrySet()) {
            RolesRecordsReportEntry entry = new RolesRecordsReportEntry(group.getKey());
            entry.dates = groupRecordsByDate(group.getValue());
            entries.add(entry);
        }
        Collections.sort(entries, Collections.reverseOrder());
    }

    private Map<String, List<RoleRecord>> groupRecordsByRole(Organization org) {
        Map<String, List<RoleRecord>> groups = new HashMap<String, List<RoleRecord>>();
        for (RoleRecordMember m : person.getRoleRecordMembers()) {
            Role role = m.getRecord().getRole();
            String roleName = role != null ? role.getName() : m.getRecord().getRoleName();
            if (groups.containsKey(roleName)) {
                continue;
            }
            List<RoleRecord> records = role != null ? role.getRecords() : RoleRecord.findByRoleName(roleName, org);
            groups.put(roleName, records);
        }
        return groups;
    }

    private List<RolesRecordsReportEntryDate> groupRecordsByDate(List<RoleRecord> records) {
        List<RolesRecordsReportEntryDate> groups = new ArrayList<RolesRecordsReportEntryDate>();
        RolesRecordsReportEntryDate currentGroup = null;
        Collections.sort(records);
        for (RoleRecord record : records) {
            boolean isPersonPresent = record.getMembers().contains(person);
            if (isPersonPresent) {
                if (currentGroup == null) {
                    // As of this record, the person has this role but did not previously have it,
                    // so we start a new date group
                    currentGroup = new RolesRecordsReportEntryDate(record.getDateCreated());
                    groups.add(currentGroup);
                }
                else {
                    // As of this record, the person has this role and already had it before,
                    // so we just update the end date of the date group
                    currentGroup.endDate = record.getDateCreated();
                }
            }
            else if (currentGroup != null) {
                // As of this record, the person had this role before but does not have it anymore,
                // so we terminate the date group
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
}
