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
import com.fasterxml.jackson.databind.JsonNode;

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

  public static String rolesJson(Organization org) throws Exception {
  	List<Role> roles = Role.all(org);
    return Utils.toJson(roles);
  }

  @Secured.Auth(UserRole.ROLE_ROLES)
  public Result newRole(Http.Request request) {
    Form<Role> form = mFormFactory.form(Role.class);
    return ok(roles_new.render(form, request, mMessagesApi.preferred(request)));
  }

  @Secured.Auth(UserRole.ROLE_ROLES)
  public Result makeNewRole(Http.Request request) throws Exception {
    Form<Role> form = mFormFactory.form(Role.class);
    Form<Role> filledForm = form.bindFromRequest(request);
    RoleType type = RoleType.valueOf(filledForm.field("type").value().get());
    RoleEligibility eligibility = RoleEligibility.valueOf(filledForm.field("eligibility").value().get());
    String name = filledForm.field("name").value().get();
    String notes = filledForm.field("notes").value().get();
    String description = filledForm.field("description").value().get();
    Role.create(Utils.getOrg(request), type, eligibility, name, notes, description);
    return redirect(routes.Roles.index());
  }

  @Secured.Auth(UserRole.ROLE_ROLES)
  public Result updateRole(Integer id, String roleJson, Http.Request request) {
    Role role = Role.findById(id, Utils.getOrg(request));
    JsonNode node = Json.parse(roleJson);
    role.update(
      RoleEligibility.valueOf(node.get("eligibility").textValue()),
      node.get("name").textValue(),
      node.get("notes").textValue(),
      node.get("description").textValue(),
      membersJsonToList(node.get("chairs")),
      membersJsonToList(node.get("backups")),
      membersJsonToList(node.get("members"))
    );
    return ok();
  }

  private static List<Map.Entry<Integer, String>> membersJsonToList(JsonNode json) {
    List<Map.Entry<Integer, String>> results = new ArrayList<Map.Entry<Integer, String>>();
    Iterator<JsonNode> iterator = json.elements();
    for (JsonNode element; iterator.hasNext();) {
      element = iterator.next();
      String label = element.get("label").textValue();
      Integer id = 0;
      try {
        id = Integer.parseInt(element.get("id").textValue());
      }
      catch (NumberFormatException e) {
      }
      results.add(new AbstractMap.SimpleEntry<Integer, String>(id, label));
    }
    return results;
  }
}