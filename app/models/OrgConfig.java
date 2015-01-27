package models;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OrgConfig {
    public String name;
    public String short_name;
    public String people_url;
    public String str_manual_title;
    public String str_manual_title_short;
    public String str_res_plan_short;
    public String str_res_plan;
    public String str_res_plan_cap;
    public String str_res_plans;
    public String str_res_plans_cap;

    public boolean show_no_contest_plea = false;
    public boolean show_severity = false;
    public boolean use_minor_referrals = false;

    public static OrgConfig get() {
        return configs.get(Organization.getByHost().name);
    }

    static HashMap<String, OrgConfig> configs = new HashMap<String, OrgConfig>();
    static void register(String name, OrgConfig config) {
        configs.put(name, config);
    }

    public String toJson() {
        ObjectMapper m = new ObjectMapper();
        try
        {
            return m.writeValueAsString(this);
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}

class ThreeRiversVillageSchool extends OrgConfig {
    private static final ThreeRiversVillageSchool INSTANCE = new ThreeRiversVillageSchool();

    public ThreeRiversVillageSchool() {
        name = "Three Rivers Village School";
        short_name = "TRVS";
        people_url = "http://people.threeriversvillageschool.org";

        str_manual_title = "Management Manual";
        str_manual_title_short = "Manual";
        str_res_plan_short = "RP";
        str_res_plan = "resolution plan";
        str_res_plan_cap = "Resolution plan";
        str_res_plans = "resolution plans";
        str_res_plans_cap = "Resolution plans";

        OrgConfig.register(name, this);
    }

    public static ThreeRiversVillageSchool getInstance() {
        return INSTANCE;
    }
}

class PhillyFreeSchool extends OrgConfig {
    private static final PhillyFreeSchool INSTANCE = new PhillyFreeSchool();

    public PhillyFreeSchool() {
        name = "Philly Free School";
        short_name = "PFS";
        people_url = "http://people-pfs.threeriversvillageschool.org";

        str_manual_title = "Lawbook";
        str_manual_title_short = "Lawbook";
        str_res_plan_short = "sentence";
        str_res_plan = "sentence";
        str_res_plan_cap = "Sentence";
        str_res_plans = "sentences";
        str_res_plans_cap = "Sentences";

        show_no_contest_plea = true;
        show_severity = true;
        use_minor_referrals = true;

        OrgConfig.register(name, this);
    }

    public static PhillyFreeSchool getInstance() {
        return INSTANCE;
    }
}
