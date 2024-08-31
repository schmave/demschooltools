package models;

import java.util.*;
import play.data.*;

public class RolesReport {

  public Integer personId;
  public Person person;
  public List<RolesReportEntry> entries;

  public static RolesReport create(Form<RolesReport> form, Organization org) {
    RolesReport report = form.get();
    report.runReport(org);
    return report;
  }

  public void runReport(Organization org) {
    entries = new ArrayList<RolesReportEntry>();
    if (personId == null) {
      return;
    }
    person = Person.findById(personId, org);
    if (person == null) {
      return;
    }
    Map<String, List<RoleRecord>> groups = groupRecordsByRole();
    for (Map.Entry<String, List<RoleRecord>> group : groups.entrySet()) {
      List<RoleRecord> records = group.getValue();
      RoleType roleType = records.get(0).getRole().getType();
      List<RolesReportSubEntry> subEntries = getSubEntries(records, org, roleType);
      RolesReportEntry entry = new RolesReportEntry(group.getKey(), subEntries);
      entries.add(entry);
    }
    Collections.sort(entries, Collections.reverseOrder());
  }

  private Map<String, List<RoleRecord>> groupRecordsByRole() {
    Map<String, List<RoleRecord>> groups = new HashMap<String, List<RoleRecord>>();
    for (RoleRecordMember m : person.getRoleRecordMembers()) {
      Role role = m.getRecord().getRole();
      if (!groups.containsKey(role.getName())) {
        groups.put(
            role.getName(), RoleRecord.findByRoleName(role.getName(), role.getOrganization()));
      }
    }
    return groups;
  }

  private List<RolesReportSubEntry> getSubEntries(
      List<RoleRecord> records, Organization org, RoleType roleType) {
    List<RolesReportSubEntry> subEntries = new ArrayList<RolesReportSubEntry>();
    List<RolesReportDate> chairDates =
        getDates(records, new RoleRecordMemberType[] {RoleRecordMemberType.Chair});
    List<RolesReportDate> backupDates =
        getDates(records, new RoleRecordMemberType[] {RoleRecordMemberType.Backup});
    List<RolesReportDate> memberDates =
        getDates(records, new RoleRecordMemberType[] {RoleRecordMemberType.Member});
    if (chairDates.size() > 0) {
      String description = "Chair";
      if (roleType == RoleType.Individual) {
        description = backupDates.size() > 0 ? RoleType.Individual.toString(org) : "";
      }
      subEntries.add(new RolesReportSubEntry(description, chairDates));
    }
    if (backupDates.size() > 0) {
      String description = "Backup";
      subEntries.add(new RolesReportSubEntry(description, backupDates));
    }
    if (memberDates.size() > 0) {
      String description = "";
      if (chairDates.size() > 0) {
        description = "Member";
        memberDates =
            getDates(
                records,
                new RoleRecordMemberType[] {
                  RoleRecordMemberType.Member, RoleRecordMemberType.Chair
                });
      }
      subEntries.add(new RolesReportSubEntry(description, memberDates));
    }
    return subEntries;
  }

  private List<RolesReportDate> getDates(
      List<RoleRecord> records, RoleRecordMemberType[] memberTypes) {
    List<RolesReportDate> groups = new ArrayList<RolesReportDate>();
    RolesReportDate currentGroup = null;
    RolesReportDate previousGroup = null;
    Collections.sort(records);
    for (RoleRecord record : records) {
      boolean isPersonPresent =
          record.getMembers().stream()
              .filter(
                  m ->
                      m.getPerson().equals(person)
                          && Arrays.asList(memberTypes).contains(m.getType()))
              .findFirst()
              .isPresent();
      if (isPersonPresent) {
        if (currentGroup == null) {
          if (previousGroup != null
              && areDateMonthsEqual(previousGroup.endDate, record.getDateCreated())) {
            // As of this record, the person has this role now and they had it earlier in the same
            // month, but there was a period in the middle of the month when they didn't have it,
            // so we ignore the intermediate period by continuing the previous date group
            currentGroup = previousGroup;
            currentGroup.endDate = record.getDateCreated();
          } else {
            // As of this record, the person has this role but did not previously have it,
            // so we start a new date group
            currentGroup = new RolesReportDate(record.getDateCreated());
            groups.add(currentGroup);
          }
        } else {
          // As of this record, the person has this role and already had it before,
          // so we just update the end date of the date group
          currentGroup.endDate = record.getDateCreated();
        }
      } else if (currentGroup != null) {
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
      // The final group was not terminated, so that means the person still has this role as of
      // today
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
