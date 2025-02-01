package models;

import java.util.*;

public enum AccountType {
  Cash,
  PersonalChecking,
  Committee,
  Group,
  Individual;

  public static Map<String, String> options(Organization org) {
    LinkedHashMap<String, String> vals = new LinkedHashMap<>();
    for (AccountType type : AccountType.values()) {
      // don't include PersonalChecking -- that kind of account is generated and may not be created
      // by users manually
      if (type != AccountType.PersonalChecking && type != AccountType.Cash) {
        vals.put(type.name(), type.toString(org));
      }
    }
    return vals;
  }

  public String toString(Organization org) {
    switch (this) {
      case Cash:
        return "Cash";
      case PersonalChecking:
        return "Personal";
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
