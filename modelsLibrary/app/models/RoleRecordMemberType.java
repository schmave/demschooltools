package models;

public enum RoleRecordMemberType {
  Member,
  Chair,
  Backup;

  public String toString() {
    switch (this) {
      case Member:
        return "Member";
      case Chair:
        return "Chair";
      case Backup:
        return "Backup";
      default:
        throw new IllegalArgumentException();
    }
  }
}
