package models;

import javax.persistence.*;

import io.ebean.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserRole extends Model {
	@Id
    private int id;

    @ManyToOne()
    private User user;

    public static final String ROLE_ACCOUNTING = "accounting";
    public static final String ROLE_ATTENDANCE = "attendance";
    public static final String ROLE_VIEW_JC = "view-jc";
    public static final String ROLE_EDIT_MANUAL = "edit-manual";
    public static final String ROLE_EDIT_RESOLUTION_PLANS = "edit-rps";
    public static final String ROLE_EDIT_7_DAY_JC = "edit-recent-jc";
    public static final String ROLE_EDIT_31_DAY_JC = "edit-recent-31-jc";
    public static final String ROLE_EDIT_ALL_JC = "edit-all-jc";
    public static final String ROLE_ALL_ACCESS = "all-access";
    public static final String ROLE_CHECKIN_APP = "checkin-app";

    public static final String[] ALL_ROLES = {
        ROLE_ACCOUNTING,
        ROLE_VIEW_JC,
        ROLE_EDIT_RESOLUTION_PLANS,
        ROLE_EDIT_7_DAY_JC,
        ROLE_EDIT_31_DAY_JC,
        ROLE_EDIT_ALL_JC,
        ROLE_EDIT_MANUAL,
        ROLE_ATTENDANCE,
        ROLE_ALL_ACCESS,
    };

    private String role;

	public static final Finder<Integer, UserRole> find = new Finder<>(
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
            return lesser_role.equals(ROLE_EDIT_31_DAY_JC) ||
                lesser_role.equals(ROLE_EDIT_7_DAY_JC) ||
                lesser_role.equals(ROLE_VIEW_JC) ||
                lesser_role.equals(ROLE_EDIT_RESOLUTION_PLANS);
        } else if (greater_role.equals(ROLE_EDIT_31_DAY_JC)) {
            return lesser_role.equals(ROLE_EDIT_7_DAY_JC) ||
                    lesser_role.equals(ROLE_VIEW_JC) ||
                    lesser_role.equals(ROLE_EDIT_RESOLUTION_PLANS);
        } else if (greater_role.equals(ROLE_EDIT_7_DAY_JC)) {
            return lesser_role.equals(ROLE_VIEW_JC) ||
                lesser_role.equals(ROLE_EDIT_RESOLUTION_PLANS);
        }

        return false;
    }

    public static String getDescription(OrgConfig orgConfig, String role) {
        String jc_name = orgConfig.str_jc_name_short;
        switch (role) {
            case ROLE_VIEW_JC:
                return "View " + jc_name + " records & rules";
            case ROLE_EDIT_MANUAL:
                return "Edit " + orgConfig.str_manual_title_short;
            case ROLE_EDIT_RESOLUTION_PLANS:
                return "Check off " + jc_name + " " + orgConfig.str_res_plans;
            case ROLE_EDIT_7_DAY_JC:
                return "Edit " + jc_name + " records up to 7 days old";
            case ROLE_EDIT_31_DAY_JC:
                return "Edit " + jc_name + " records up to 31 days old";
            case ROLE_EDIT_ALL_JC:
                return "Edit all " + jc_name + " records";
            case ROLE_ALL_ACCESS:
                return "View and Edit everything";
            case ROLE_ACCOUNTING:
                return "Manage accounts and create transactions";
            case ROLE_ATTENDANCE:
                return "Manage attendance records";
        }

        throw new RuntimeException("unknown role: " + role);
    }
}
