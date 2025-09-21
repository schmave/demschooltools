package models;

import java.text.SimpleDateFormat;
import java.util.*;

public class RolesReportDate implements Comparable<RolesReportDate> {

  public Date startDate;
  public Date endDate;

  private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy");

  public RolesReportDate(Date date) {
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
  public int compareTo(RolesReportDate d) {
    return endDate.compareTo(d.endDate);
  }
}
