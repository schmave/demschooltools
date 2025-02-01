package models;

public enum RoleType {
  Individual,
  Committee,
  Group;

  public String toString(Organization org) {
    switch (this) {
      case Individual:
        return org.getRolesIndividualTerm();
      case Committee:
        return org.getRolesCommitteeTerm();
      case Group:
        return org.getRolesGroupTerm();
      default:
        throw new IllegalArgumentException();
    }
  }
}
