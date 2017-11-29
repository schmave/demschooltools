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
            if (type != AccountType.PersonalChecking) {
                vals.put(type.name(), type.toString());
            }
        }
        return vals;
    }

    @Override
   	public String toString() {
    	switch (this) {
    		case Cash: return "Cash";
    		case PersonalChecking: return "Checking";
    		case Committee: return "Committee";
    		case Corporation: return "Corporation";
    		case Clerk: return "Clerk";
    		default: throw new IllegalArgumentException();
    	}
   	}
}