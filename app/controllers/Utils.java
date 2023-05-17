package controllers;

import com.fasterxml.jackson.databind.*;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.typesafe.config.Config;
import io.ebean.DB;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import models.*;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import play.Logger;
import play.data.Form;
import play.mvc.Http;
import play.twirl.api.Html;
import play.twirl.api.HtmlFormat;

@Singleton
public class Utils {
  static MustacheFactory sMustache = new DefaultMustacheFactory();
  static ExecutorService sCustodiaService = Executors.newSingleThreadExecutor();
  static Logger.ALogger sLogger = Logger.of("application");

  public static Calendar parseDateOrNow(String date_string) {
    Calendar result = new GregorianCalendar();

    try {
      Date parsed_date = new SimpleDateFormat("yyyy-M-d").parse(date_string);
      if (parsed_date != null) {
        result.setTime(parsed_date);
      }
    } catch (ParseException e) {
      System.out.println("Failed to parse given date (" + date_string + "), using current");
    }

    return result;
  }

  // day_of_week is, e.g., Calendar.TUESDAY
  public static void adjustToPreviousDay(Calendar date, int day_of_week) {
    int dow = date.get(Calendar.DAY_OF_WEEK);
    if (dow >= day_of_week) {
      date.add(GregorianCalendar.DATE, -(dow - day_of_week));
    } else {
      date.add(GregorianCalendar.DATE, day_of_week - dow - 7);
    }
  }

  public static String toJson(Object o) {
    ObjectMapper m = new ObjectMapper();
    m.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

    // Something like this should work, but I can't get it to work now.
    // If you fix this, head to edit_attendance_week.scala.html and
    // simplify it a bit.
    // SimpleModule module = new SimpleModule("MyMapKeySerializerModule",
    //    new Version(1, 0, 0, null));
    //
    // module.addKeySerializer(Person.class, new PersonKeySerializer());
    // m = m.registerModule(module);
    //
    try {
      return m.writeValueAsString(o);
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  public static String renderMustache(String templateName, Object scopes) {
    StringWriter writer = new StringWriter();
    sMustache
        .compile(
            new InputStreamReader(
                Public.sEnvironment.resourceAsStream("public/mustache/" + templateName)),
            templateName)
        .execute(writer, scopes);
    return writer.toString();
  }

  public static Html newlineToBr(String input) {
    return HtmlFormat.raw(HtmlFormat.escape(input.trim()).body().replace("\n", "<br/>"));
  }

  public static boolean getBooleanFromFormValue(String value) {
    return value.equals("true") || value.equals("on");
  }

  public static boolean getBooleanFromFormValue(Form.Field field) {
    return field.value().filter(Utils::getBooleanFromFormValue).isPresent();
  }

  public static boolean lessThanDaysOld(Date date, int num_days) {
    Date now = new Date();
    long diff = now.getTime() - date.getTime();
    long one_day = 24 * 60 * 60 * 1000;
    return diff < one_day * num_days;
  }

  private static void makeCustodiaPost(
      CloseableHttpClient client, String url, List<NameValuePair> data) {
    CloseableHttpResponse response = null;
    try {
      try {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(data));
        response = client.execute(httpPost);
        HttpEntity entity = response.getEntity();
        EntityUtils.consume(entity);
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new RuntimeException(
              "Non-200 result from Custodia: " + response.getStatusLine().getStatusCode());
        }
      } finally {
        if (response != null) {
          response.close();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void loginToCustodia(CloseableHttpClient client, Config play_config) {
    Config school_crm_config = play_config.getConfig("school_crm");
    List<NameValuePair> nvps = new ArrayList<>();
    nvps.add(new BasicNameValuePair("username", "admin"));
    nvps.add(new BasicNameValuePair("password", school_crm_config.getString("custodiaPassword")));
    makeCustodiaPost(client, school_crm_config.getString("custodia_url") + "/users/login", nvps);
  }

  public static void setCustodiaPassword(String new_password, Organization org) {
    final OrgConfig config = Utils.getOrgConfig(org);
    sCustodiaService.submit(
        () -> {
          try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            loginToCustodia(httpclient, Public.sConfig);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("username", config.org.getShortName()));
            nvps.add(new BasicNameValuePair("password", new_password));
            makeCustodiaPost(
                httpclient, Public.sConfig.getString("custodia_url") + "/users/password", nvps);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  public static void updateCustodia() {
    Future<?> result =
        sCustodiaService.submit(
            () -> {
              CloseableHttpClient httpclient = HttpClients.createDefault();
              loginToCustodia(httpclient, Public.sConfig);
              makeCustodiaPost(
                  httpclient,
                  Public.sConfig.getConfig("school_crm").getString("custodia_url")
                      + "/updatefromdst",
                  new ArrayList<>());
            });
    try {
      result.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Date localNow(OrgConfig orgConfig) {
    Date utcNow = new Date();
    return new Date(utcNow.getTime() + orgConfig.time_zone.getOffset(utcNow.getTime()));
  }

  public static @Nonnull Organization getOrg(Http.Request request) {
    String host = request.host();

    String cache_key = "Organization::getOrg::" + host;

    Optional<Organization> cached_val = Public.sCache.get(cache_key);
    if (cached_val.isPresent()) {
      return cached_val.get();
    }

    String sql = "select organization_id from organization_hosts where host like :host";
    SqlQuery sqlQuery = DB.sqlQuery(sql);
    sqlQuery.setParameter("host", host);

    // execute the query returning a List of MapBean objects
    SqlRow result = sqlQuery.findOne();

    if (result != null) {
      Organization org_result = Organization.find.byId(result.getInteger("organization_id"));
      Public.sCache.set(cache_key, org_result, 60); // cache for 1 minute
      return org_result;
    } else {
      if (host.matches("[a-z]")) {
        sLogger.error("Unknown organization for host: " + host);
      }
    }

    return null;
  }

  public static OrgConfig getOrgConfig(Organization org) {
    OrgConfig result = OrgConfigs.configs.get(org.getName());
    result.org = org;
    return result;
  }

  public static OrgConfig getOrgConfig(Http.Request request) {
    return getOrgConfig(Utils.getOrg(request));
  }
}

class OrgConfigs {
  public static HashMap<String, OrgConfig> configs = new HashMap<>();

  public static void register(OrgConfig config) {
    configs.put(config.name, config);
  }

  static {
    register(new ThreeRiversVillageSchool());
    register(new PhillyFreeSchool());
    register(new Fairhaven());
    register(new TheCircleSchool());
    register(new MakariosLearningCommunity());
    register(new TheOpenSchool());
    register(new TheOpenSchool2());
    register(new Houston());
    register(new Sandbox());
    register(new Clearview());
    register(new Wicklow());
    register(new Tallgrass());
    register(new MiamiSudburySchool());
    register(new SligoSudburySchool());
    register(new LearningProjectIbiza());
    register(new Wilmington());
  }
}

class ThreeRiversVillageSchool extends OrgConfig {
  public ThreeRiversVillageSchool() {
    name = "Three Rivers Village School";
    people_url = "https://trvs.demschooltools.com";

    str_manual_title = "Management Manual";
    str_manual_title_short = "Manual";
    str_res_plan_short = "RP";
    str_res_plan = "resolution plan";
    str_res_plan_cap = "Resolution plan";
    str_res_plans = "resolution plans";
    str_res_plans_cap = "Resolution plans";
    str_jc_name = "Justice Committee";

    show_checkbox_for_res_plan = false;

    enable_file_sharing = true;
  }
}

class PhillyFreeSchool extends OrgConfig {
  public PhillyFreeSchool() {
    name = "Philly Free School";
    people_url = "https://pfs.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Consequence";
    str_res_plan = "consequence";
    str_res_plan_cap = "Consequence";
    str_res_plans = "consequences";
    str_res_plans_cap = "Consequences";
    str_guilty = "Accept Responsibility";
    str_not_guilty = "Reject Responsibility";

    show_no_contest_plea = true;
    show_na_plea = true;
    show_severity = true;
  }
}

class Fairhaven extends OrgConfig {
  public Fairhaven() {
    name = "Fairhaven School";
    people_url = "https://fairhaven.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "JC Report";

    show_findings_in_rp_list = false;
    use_year_in_case_number = true;
  }
}

class TheCircleSchool extends OrgConfig {
  public TheCircleSchool() {
    name = "The Circle School";
    people_url = "https://tcs.demschooltools.com";

    str_manual_title = "Management Manual";
    str_manual_title_short = "Mgmt. Man.";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";

    track_writer = false;
    filter_no_charge_cases = true;
    hide_location_for_print = true;
    show_no_contest_plea = true;
  }

  @Override
  public String getReferralDestination(Charge c) {
    if (c.getPlea().equals("Not Guilty")) {
      return "trial";
    } else {
      return "School Meeting";
    }
  }

  @Override
  public String getCaseNumberPrefix(Meeting m) {
    return new SimpleDateFormat("yyyyMMdd").format(m.getDate());
  }
}

class MakariosLearningCommunity extends OrgConfig {
  public MakariosLearningCommunity() {
    name = "Makarios Learning Community";
    people_url = "https://mlc.demschooltools.com";
    time_zone = TimeZone.getTimeZone("US/Central");

    str_manual_title = "Management Manual";
    str_manual_title_short = "Mgmt. Man.";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";
  }
}

class TheOpenSchool extends OrgConfig {
  public TheOpenSchool() {
    name = "The Open School";
    people_url = "https://tos.demschooltools.com";
    time_zone = TimeZone.getTimeZone("US/Pacific");

    str_manual_title = "Law Book";
    str_manual_title_short = "Law Book";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";
    str_jc_name = "Civics Board";
    str_jc_name_short = "CB";
    str_guilty = "Agree";
    str_not_guilty = "Disagree";
    str_na = "Mediated";

    show_findings_in_rp_list = true;
    use_minor_referrals = false;
    show_no_contest_plea = true;
    show_na_plea = true;
  }
}

class TheOpenSchool2 extends OrgConfig {
  public TheOpenSchool2() {
    name = "The Open School - Riverside";
    people_url = "https://tos-2.demschooltools.com";
    time_zone = TimeZone.getTimeZone("US/Pacific");

    str_manual_title = "Law Book";
    str_manual_title_short = "Law Book";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";
    str_jc_name = "Justice Committee";
    str_jc_name_short = "JC";
    str_guilty = "Confirm";
    str_not_guilty = "Deny";

    show_findings_in_rp_list = true;
    use_minor_referrals = false;
    show_no_contest_plea = true;
  }
}

class Houston extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("US/Central");

  public Houston() {
    name = "Houston Sudbury School";
    people_url = "https://hss.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";

    track_writer = true;
    use_year_in_case_number = true;
  }
}

class Sandbox extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("US/Eastern");

  public Sandbox() {
    name = "DemSchoolTools sandbox area";
    people_url = "https://sandbox.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";

    track_writer = true;
    enable_file_sharing = true;
  }
}

class Clearview extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("US/Central");

  public Clearview() {
    name = "Clearview Sudbury School";
    people_url = "https://css.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";
    str_findings = "Findings";

    track_writer = false;
  }
}

class Wicklow extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("Europe/Dublin");

  public Wicklow() {
    name = "Wicklow Democratic School";
    people_url = "https://wicklow.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sanction";
    str_res_plan = "sanction";
    str_res_plan_cap = "Sanction";
    str_res_plans = "sanctions";
    str_res_plans_cap = "Sanctions";
    str_findings = "Findings";

    track_writer = false;
    euro_dates = true;
    use_year_in_case_number = true;
  }
}

class Tallgrass extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("US/Central");

  public Tallgrass() {
    name = "Tallgrass Sudbury School";
    people_url = "https://tallgrass.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Result";
    str_res_plan = "result";
    str_res_plan_cap = "Result";
    str_res_plans = "results";
    str_res_plans_cap = "Results";
    str_findings = "Findings";
    str_jc_name = "Restoration Committee";
    str_jc_name_short = "RC";

    str_guilty = "Agree";
    str_not_guilty = "Disagree";

    track_writer = false;
  }
}

class MiamiSudburySchool extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("US/Eastern");

  public MiamiSudburySchool() {
    name = "Miami Sudbury School";
    people_url = "https://miami.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Ruling";
    str_res_plan = "ruling";
    str_res_plan_cap = "Ruling";
    str_res_plans = "rulings";
    str_res_plans_cap = "Rulings";
    str_findings = "Complaint & Further Information";

    str_guilty = "Accepts responsibility";
    str_not_guilty = "Does not accept responsibility";

    track_writer = true;
  }
}

class SligoSudburySchool extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("Europe/Dublin");

  public SligoSudburySchool() {
    name = "Sligo Sudbury School";
    people_url = "https://sligo.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sanction";
    str_res_plan = "sanction";
    str_res_plan_cap = "Sanction";
    str_res_plans = "sanctions";
    str_res_plans_cap = "Sanctions";

    track_writer = true;
    euro_dates = true;
    use_year_in_case_number = true;
  }
}

class LearningProjectIbiza extends OrgConfig {
  public TimeZone time_zone = TimeZone.getTimeZone("Europe/Madrid");

  public LearningProjectIbiza() {
    name = "Learning Project Ibiza";
    people_url = "https://tlp.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";

    track_writer = true;
    use_year_in_case_number = true;
  }
}

class Wilmington extends OrgConfig {
  public Wilmington() {
    name = "Wilmington Sudbury School";
    people_url = "https://wilmington.demschooltools.com";

    str_manual_title = "Lawbook";
    str_manual_title_short = "Lawbook";
    str_res_plan_short = "Sentence";
    str_res_plan = "sentence";
    str_res_plan_cap = "Sentence";
    str_res_plans = "sentences";
    str_res_plans_cap = "Sentences";

    track_writer = true;
    use_year_in_case_number = true;
  }
}
