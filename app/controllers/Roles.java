package controllers;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.*;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.*;

@Secured.Auth(UserRole.ROLE_VIEW_JC)
public class Roles extends Controller {

  FormFactory mFormFactory;
  final MessagesApi mMessagesApi;

  @Inject
  public Roles(FormFactory formFactory, MessagesApi messagesApi) {
    mFormFactory = formFactory;
    mMessagesApi = messagesApi;
  }

  public Result index(Http.Request request) {
    Organization org = Utils.getOrg(request);
    return ok(roles_index.render(request, mMessagesApi.preferred(request)));
  }

  public static String rolesJson(Organization org) {
  	return "";
  	// List<Role> roles = Role.all(org);
  	// ObjectMapper objectMapper = new ObjectMapper();
  	// return ObjectMapper.writeValueAsString(roles);

    // List<Map<String, String>> result = new ArrayList<>();
    // List<Role> roles = Role.all(org);
    // for (Role role : roles) {
    //   HashMap<String, String> values = new HashMap<>();
    //   values.put("id", "" + role.getId());
    //   values.put("type", role.getType().toString(org));
    //   values.put("name", role.getName());
    //   values.put("notes", role.getNotes());
    //   values.put("description", role.getDescription());
    //   values.put("isActive", role.getIsActive() ? "true" : "false");
    //   result.add(values);
    // }
    // return Json.stringify(Json.toJson(result));
  }

  public static String peopleJson(Organization org) {
  	return "";
    // List<Map<String, String>> result = new ArrayList<>();
    // List<Person> people = attendancePeople(org);
    // for (Person person : people) {
    //   HashMap<String, String> values = new HashMap<>();
    //   values.put("id", "" + person.getPersonId());
    //   values.put("name", person.getDisplayName());
    //   result.add(values);
    // }
    // return Json.stringify(Json.toJson(result));
  }

  @Secured.Auth(UserRole.ROLE_ROLES)
  public Result newRole(Http.Request request) {
    Form<Role> form = mFormFactory.form(Role.class);
    return ok(roles_new.render(form, request, mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_ROLES)
  public Result makeNewRole(Http.Request request) {
    Form<Role> form = mFormFactory.form(Role.class);
    Form<Role> filledForm = form.bindFromRequest(request);
    if (filledForm.hasErrors()) {
      System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
      return badRequest(roles_new.render(filledForm, request, mMessagesApi.preferred(request)));
    } else {
      try {
        Role role = Role.create(filledForm, Utils.getOrg(request));
        return redirect(routes.Roles.index());
      } catch (Exception ex) {
        return badRequest(ex.toString());
      }
    }
  }
}