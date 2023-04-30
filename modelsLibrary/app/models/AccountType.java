package models;

import java.util.*;

public enum AccountType {
  Cash,
  PersonalChecking,
  Committee,
  Corporation,
  Clerk;

  public static Map<String, String> options(OrgConfig orgConfig) {
    LinkedHashMap<String, String> vals = new LinkedHashMap<>();
    for (AccountType type : AccountType.values()) {
      // don't include PersonalChecking -- that kind of account is generated and may not be created
      // by users manually
      if (type != AccountType.PersonalChecking && type != AccountType.Cash) {
        vals.put(type.name(), type.toString(orgConfig));
      }
    }
    return vals;
  }

  @Override
  // When possible, use toString(OrgConfig) instead
  public String toString() {
    switch (this) {
      case Cash:
        return "Cash";
      case PersonalChecking:
        return "Personal";
      case Committee:
        return "Committee";
      case Corporation:
        return "Corporation";
      case Clerk:
        return "Clerk";
      default:
        throw new IllegalArgumentException();
    }
  }

  public String toString(OrgConfig orgConfig) {
    switch (this) {
      case Cash:
        return "Cash";
      case PersonalChecking:
        return "Personal";
      case Committee:
        return orgConfig.str_committee;
      case Corporation:
        return orgConfig.str_corporation;
      case Clerk:
        return orgConfig.str_clerk;
      default:
        throw new IllegalArgumentException();
    }
  }
}
