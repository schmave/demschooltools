package models;

import java.util.*;

public enum InstitutionType
{
	School,
    Committee,
    Corporation,
    Clerk;

    public static Map<String, String> options(){
        LinkedHashMap<String, String> vals = new LinkedHashMap<String, String>();
        for (InstitutionType type: InstitutionType.values()) {
            vals.put(type.name(), type.name());
        }
        return vals;
    }
}