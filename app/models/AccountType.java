package models;

import java.util.*;

public enum AccountType
{
    Cash,
    PersonalChecking,
    Committee,
    Corporation,
    Clerk;

    public static Map<String, String> options(){
        LinkedHashMap<String, String> vals = new LinkedHashMap<String, String>();
        for (AccountType type: AccountType.values()) {
            // don't include PersonalChecking -- that kind of account is generated and may not be created by users manually
            if (type != AccountType.PersonalChecking && type != AccountType.Cash) {
                vals.put(type.name(), type.toString());
            }
        }
        return vals;
    }

    @Override
   	public String toString() {
    	switch (this) {
    		case Cash: return "Cash";
    		case PersonalChecking: return "Personal";
    		case Committee: return OrgConfig.get().str_committee;
    		case Corporation: return OrgConfig.get().str_corporation;
    		case Clerk: return OrgConfig.get().str_clerk;
    		default: throw new IllegalArgumentException();
    	}
   	}
}