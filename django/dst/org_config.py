import datetime
from zoneinfo import ZoneInfo

from dst.models import Organization

ALL_ORG_CONFIGS: dict[str, "OrgConfig"] = {}


def get_org_config(org: Organization) -> "OrgConfig":
    return ALL_ORG_CONFIGS[org.name]


class OrgConfig:
    def __init__(self, name):
        ALL_ORG_CONFIGS[name] = self
        self.org = Organization.objects.filter(name=name).first()

        self.name = name
        self.people_url = ""
        self.str_manual_title = ""
        self.str_manual_title_short = ""
        self.str_res_plan_short = ""
        self.str_res_plan = ""
        self.str_res_plan_cap = ""
        self.str_res_plans = ""
        self.str_res_plans_cap = ""
        self.str_jc_name = "Judicial Committee"
        self.str_jc_name_short = "JC"
        self.str_findings = "Findings"
        self.str_guilty = "Guilty"
        self.str_not_guilty = "Not Guilty"
        self.str_na = "N/A"

        self.show_no_contest_plea = False
        self.show_na_plea = False
        self.show_severity = False
        self.show_entry = True
        self.show_plea = True

        self.use_minor_referrals = True
        self.show_checkbox_for_res_plan = True
        self.track_writer = True
        self.filter_no_charge_cases = False
        self.show_findings_in_rp_list = True
        self.use_year_in_case_number = False
        self.hide_location_for_print = False
        self.euro_dates = False

        self.enable_file_sharing = False

        # TODO: Delete this and all overrides of it in subclasses of OrgConfig.
        # As of the Custodia -> DST merger, this value is now stored in the
        # Organization model. Be sure that the database is up to date with
        # these values.
        self.time_zone = ZoneInfo("US/Eastern")

    def get_referral_destination(self, charge) -> str:
        return "School Meeting"

    def get_case_number_prefix(self, meeting):
        date_format = ""

        if self.use_year_in_case_number:
            date_format = "dd-MM-YYYY-" if self.euro_dates else "YYYY-MM-dd-"
        else:
            date_format = "dd-MM-" if self.euro_dates else "MM-dd-"

        # Convert Java's SimpleDateFormat to Python's strftime format
        date_format = (
            date_format.replace("YYYY", "%Y").replace("MM", "%m").replace("dd", "%d")
        )

        return datetime.datetime.strftime(meeting.get_date(), date_format)

    def translate_plea(self, plea):
        if plea == "Guilty":
            return self.str_guilty
        elif plea == "Not Guilty":
            return self.str_not_guilty
        elif plea == "N/A":
            return self.str_na

        return plea


class ThreeRiversVillageSchool(OrgConfig):
    def __init__(self):
        super().__init__("Three Rivers Village School")
        self.people_url = "https://trvs.demschooltools.com"

        self.str_manual_title = "Management Manual"
        self.str_manual_title_short = "Manual"
        self.str_res_plan_short = "agreement"
        self.str_res_plan = "agreement"
        self.str_res_plan_cap = "Agreement"
        self.str_res_plans = "agreements"
        self.str_res_plans_cap = "Agreements"
        self.str_jc_name = "Resolution Committee"
        self.str_jc_name_short = "RC"
        self.str_findings = "Discussion Record"

        self.show_checkbox_for_res_plan = False
        self.enable_file_sharing = True
        self.use_minor_referrals = False
        self.show_entry = False
        self.show_plea = False


class PhillyFreeSchool(OrgConfig):
    def __init__(self):
        super().__init__("Philly Free School")
        self.people_url = "https://pfs.demschooltools.com"

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Consequence"
        self.str_res_plan = "consequence"
        self.str_res_plan_cap = "Consequence"
        self.str_res_plans = "consequences"
        self.str_res_plans_cap = "Consequences"
        self.str_guilty = "Accept Responsibility"
        self.str_not_guilty = "Reject Responsibility"

        self.show_no_contest_plea = True
        self.show_na_plea = True
        self.show_severity = True


class Fairhaven(OrgConfig):
    def __init__(self):
        super().__init__("Fairhaven School")
        self.people_url = "https://fairhaven.demschooltools.com"

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "JC Report"

        self.show_findings_in_rp_list = False
        self.use_year_in_case_number = True


class TheCircleSchool(OrgConfig):
    def __init__(self):
        super().__init__("The Circle School")
        self.people_url = "https://tcs.demschooltools.com"

        self.str_manual_title = "Management Manual"
        self.str_manual_title_short = "Mgmt. Man."
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "Findings"

        self.track_writer = False
        self.filter_no_charge_cases = True
        self.hide_location_for_print = True
        self.show_no_contest_plea = True

    def get_referral_destination(self, charge):
        if charge.get_plea() == "Not Guilty":
            return "trial"
        else:
            return "School Meeting"

    def get_case_number_prefix(self, meeting):
        return datetime.datetime.strftime(meeting.get_date(), "%Y%m%d")


class MakariosLearningCommunity(OrgConfig):
    def __init__(self):
        super().__init__("Makarios Learning Community")
        self.people_url = "https://mlc.demschooltools.com"
        self.time_zone = ZoneInfo("US/Central")

        self.str_manual_title = "Management Manual"
        self.str_manual_title_short = "Mgmt. Man."
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "Findings"


class TheOpenSchool(OrgConfig):
    def __init__(self):
        super().__init__("The Open School")
        self.people_url = "https://tos.demschooltools.com"
        self.time_zone = ZoneInfo("US/Pacific")

        self.str_manual_title = "Law Book"
        self.str_manual_title_short = "Law Book"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "Findings"
        self.str_jc_name = "Civics Board"
        self.str_jc_name_short = "CB"
        self.str_guilty = "Agree"
        self.str_not_guilty = "Disagree"
        self.str_na = "Mediated"

        self.show_findings_in_rp_list = True
        self.use_minor_referrals = False
        self.show_no_contest_plea = True
        self.show_na_plea = True


class TheOpenSchoolVirtual(OrgConfig):
    def __init__(self):
        super().__init__("The Open School Virtual")
        self.people_url = "https://tosv.demschooltools.com"
        self.time_zone = ZoneInfo("US/Pacific")

        self.str_manual_title = "Law Book"
        self.str_manual_title_short = "Law Book"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "Findings"
        self.str_jc_name = "Civics Board"
        self.str_jc_name_short = "CB"
        self.str_guilty = "Agree"
        self.str_not_guilty = "Disagree"
        self.str_na = "Mediated"

        self.show_findings_in_rp_list = True
        self.use_minor_referrals = False
        self.show_no_contest_plea = True
        self.show_na_plea = True


class Houston(OrgConfig):
    def __init__(self):
        super().__init__("Houston Sudbury School")
        self.people_url = "https://hss.demschooltools.com"
        self.time_zone = ZoneInfo("US/Central")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "Findings"

        self.track_writer = True
        self.use_year_in_case_number = True


class Sandbox(OrgConfig):
    def __init__(self):
        super().__init__("DemSchoolTools sandbox area")
        self.people_url = "https://sandbox.demschooltools.com"
        self.time_zone = ZoneInfo("US/Eastern")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"
        self.str_findings = "Findings"

        self.track_writer = True
        self.enable_file_sharing = True


class Clearview(OrgConfig):
    def __init__(self):
        super().__init__("Clearview Sudbury School")
        self.people_url = "https://css.demschooltools.com"
        self.time_zone = ZoneInfo("US/Central")

        self.str_manual_title = "Rulebook"
        self.str_manual_title_short = "Rulebook"
        self.str_res_plan_short = "Resolution"
        self.str_res_plan = "resolution"
        self.str_res_plan_cap = "Resolution"
        self.str_res_plans = "resolutions"
        self.str_res_plans_cap = "Resolutions"
        self.str_findings = "Findings"

        self.str_guilty = "Yes"
        self.str_not_guilty = "No"

        self.track_writer = True


class Wicklow(OrgConfig):
    def __init__(self):
        super().__init__("Wicklow Democratic School")
        self.people_url = "https://wicklow.demschooltools.com"
        self.time_zone = ZoneInfo("Europe/Dublin")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sanction"
        self.str_res_plan = "sanction"
        self.str_res_plan_cap = "Sanction"
        self.str_res_plans = "sanctions"
        self.str_res_plans_cap = "Sanctions"
        self.str_findings = "Findings"

        self.track_writer = False
        self.euro_dates = True
        self.use_year_in_case_number = True


class Tallgrass(OrgConfig):
    def __init__(self):
        super().__init__("Tallgrass Sudbury School")
        self.people_url = "https://tallgrass.demschooltools.com"
        self.time_zone = ZoneInfo("US/Central")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Result"
        self.str_res_plan = "result"
        self.str_res_plan_cap = "Result"
        self.str_res_plans = "results"
        self.str_res_plans_cap = "Results"
        self.str_findings = "Findings"
        self.str_jc_name = "Restoration Committee"
        self.str_jc_name_short = "RC"

        self.str_guilty = "Agree"
        self.str_not_guilty = "Disagree"

        self.track_writer = False


class MiamiSudburySchool(OrgConfig):
    def __init__(self):
        super().__init__("Miami Sudbury School")
        self.people_url = "https://miami.demschooltools.com"
        self.time_zone = ZoneInfo("US/Eastern")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Ruling"
        self.str_res_plan = "ruling"
        self.str_res_plan_cap = "Ruling"
        self.str_res_plans = "rulings"
        self.str_res_plans_cap = "Rulings"
        self.str_findings = "Complaint & Further Information"

        self.str_guilty = "Accepts responsibility"
        self.str_not_guilty = "Does not accept responsibility"

        self.track_writer = True


class SligoSudburySchool(OrgConfig):
    def __init__(self):
        super().__init__("Sligo Sudbury School")
        self.people_url = "https://sligo.demschooltools.com"
        self.time_zone = ZoneInfo("Europe/Dublin")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sanction"
        self.str_res_plan = "sanction"
        self.str_res_plan_cap = "Sanction"
        self.str_res_plans = "sanctions"
        self.str_res_plans_cap = "Sanctions"

        self.track_writer = True
        self.euro_dates = True
        self.use_year_in_case_number = True


class LearningProjectIbiza(OrgConfig):
    def __init__(self):
        super().__init__("Learning Project Ibiza")
        self.people_url = "https://tlp.demschooltools.com"
        self.time_zone = ZoneInfo("Europe/Madrid")

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"

        self.track_writer = True
        self.use_year_in_case_number = True


class Wilmington(OrgConfig):
    def __init__(self):
        super().__init__("Wilmington Sudbury School")
        self.people_url = "https://wilmington.demschooltools.com"

        self.str_manual_title = "Lawbook"
        self.str_manual_title_short = "Lawbook"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"

        self.track_writer = True
        self.use_year_in_case_number = True


class SouthJersey(OrgConfig):
    def __init__(self):
        super().__init__("South Jersey Sudbury School")
        self.people_url = "https://sjss.demschooltools.com"

        self.str_manual_title = "Rulebook"
        self.str_manual_title_short = "Rulebook"
        self.str_res_plan_short = "Sentence"
        self.str_res_plan = "sentence"
        self.str_res_plan_cap = "Sentence"
        self.str_res_plans = "sentences"
        self.str_res_plans_cap = "Sentences"

        self.track_writer = True
        self.use_year_in_case_number = True


# Initialize all the configurations
# They will be automatically registered in ALL_ORG_CONFIGS
ThreeRiversVillageSchool()
PhillyFreeSchool()
Fairhaven()
TheCircleSchool()
MakariosLearningCommunity()
TheOpenSchool()
TheOpenSchoolVirtual()
Houston()
Sandbox()
Clearview()
Wicklow()
Tallgrass()
MiamiSudburySchool()
SligoSudburySchool()
LearningProjectIbiza()
Wilmington()
SouthJersey()
