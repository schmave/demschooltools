package models;

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
            vals.put(type.name(), type.name());
        }
        return vals;
    }

    @Override
   	public String toString() {
    	switch (this) {
    		case Cash: return "Cash";
    		case PersonalChecking: return "Personal Checking";
    		case Committee: return "Committee Checking";
    		case Corporation: return "Corporation Checking";
    		case Clerk: return "Clerk Checking";
    		default: throw new IllegalArgumentException();
    	}
   	}
}