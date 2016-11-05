package models;

import java.util.*;

import javax.persistence.*;

import com.avaje.ebean.*;

@Entity
public class UserRole extends Model {
	@Id
	public int id;

    @ManyToOne()
    public User user;

    public static final String ROLE_VIEW_JC = "view-jc";
    public static final String ROLE_EDIT_MANUAL = "edit-manual";
    public static final String ROLE_EDIT_RECENT_JC = "edit-recent-jc";
    public static final String ROLE_EDIT_ALL_JC = "edit-all-jc";
    public static final String ROLE_ALL_ACCESS = "all-access";

    public static final String[] ALL_ROLES = {
        ROLE_VIEW_JC,
        ROLE_EDIT_MANUAL,
        ROLE_EDIT_RECENT_JC,
        ROLE_EDIT_ALL_JC,
        ROLE_ALL_ACCESS,
    };

    public String role;

	public static final Finder<Integer, UserRole> find = new Finder<Integer, UserRole>(
			UserRole.class);

    public static UserRole create(User u, String r) {
        UserRole result = new UserRole();
        result.user = u;
        result.role = r;

        result.save();
        return result;
    }

    // Return true iff a user who has greater_role also has all the powers
    // of lesser_role.
    public static boolean includes(String greater_role, String lesser_role) {
        if (greater_role.equals(ROLE_ALL_ACCESS)) {
            return true;
        } else if (greater_role.equals(ROLE_EDIT_ALL_JC)) {
            return lesser_role.equals(ROLE_EDIT_RECENT_JC) ||
                lesser_role.equals(ROLE_VIEW_JC);
        } else if (greater_role.equals(ROLE_EDIT_RECENT_JC)) {
            return lesser_role.equals(ROLE_VIEW_JC);
        }

        return false;
    }

    public static String getDescription(String role) {
        String jc_name = OrgConfig.get().str_jc_name_short;
        if (role.equals(ROLE_VIEW_JC)) {
            return "View " + jc_name + " records & rules";
        } else if (role.equals(ROLE_EDIT_MANUAL)) {
            return "Edit " + OrgConfig.get().str_manual_title_short;
        } else if (role.equals(ROLE_EDIT_RECENT_JC)) {
            return "Edit recent " + jc_name + " records + the active " + OrgConfig.get().str_res_plans;
        } else if (role.equals(ROLE_EDIT_ALL_JC)) {
            return "Edit all " + jc_name + " records";
        } else if (role.equals(ROLE_ALL_ACCESS)) {
            return "View and Edit everything";
        }

        throw new RuntimeException("unknown role: " + role);
    }
}

