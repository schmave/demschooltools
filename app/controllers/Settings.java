package controllers;

import java.util.*;

import io.ebean.Ebean;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import io.ebean.SqlUpdate;

import org.mindrot.jbcrypt.BCrypt;

import models.*;

import service.MyUserService;

import play.data.*;
import play.mvc.*;

import javax.inject.Inject;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class Settings extends Controller {

    FormFactory mFormFactory;

    @Inject
    public Settings(final FormFactory ff) {
        mFormFactory = ff;
    }

    public Result viewSettings(Http.Request request) {
        List<NotificationRule> rules = NotificationRule.find.query().where()
            .eq("organization", Organization.getByHost(request))
            .order("the_type DESC, tag.id")
            .findList();

        return ok(views.html.view_settings.render(rules, Organization.getByHost(request), Public.sConfig.getConfig("school_crm")));
    }

    public Result editSettings(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        final Organization org = Organization.getByHost(request);
        org.updateFromForm(values, org);
        CachedPage.clearAll(org);

        {
            String new_password = null;
            if (values.containsKey("custodia_student_password")) {
                new_password = values.get("custodia_student_password")[0];
            }
            if (new_password != null && !new_password.trim().isEmpty()) {
                Utils.setCustodiaPassword(new_password, org);
            }
        }

        {
            String new_password = null;
            if (values.containsKey("electronic_signin_password")) {
                new_password = values.get("electronic_signin_password")[0];
            }
            if (new_password != null && !new_password.trim().isEmpty()) {
                String username = org.short_name;
                User user = User.findByEmail(username);
                if (user == null) {
                    user = User.create(username, "Check-in app user", org);
                }
                user.hashed_password = BCrypt.hashpw(new_password, BCrypt.gensalt());
                user.save();
                for (UserRole r : user.roles) {
                    r.delete();
                }
                UserRole.create(user, UserRole.ROLE_CHECKIN_APP);
                UserRole.create(user, UserRole.ROLE_VIEW_JC);
            }
        }

        return redirect(routes.Settings.viewSettings());
    }

    public Result editNotifications(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();

        if (values.containsKey("remove_notification_id")) {
            NotificationRule.findById(
                Integer.parseInt(values.get("remove_notification_id")[0]), Organization.getByHost(request)).delete();
        } else {
            String email = values.get("email")[0];
            if (!email.matches("^\\S+@\\S+.\\S+$")) {
                return redirect(routes.Settings.viewSettings()).flashing(
                        "error", "'" + email + "' does not seem to be a valid email address. Notification not created.");
            }

            if (values.containsKey("tag_id")) {
                Tag t = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Organization.getByHost(request));
                NotificationRule.create(NotificationRule.TYPE_TAG, t, email, Organization.getByHost(request));
            }

            if (values.containsKey("comment")) {
                NotificationRule.create(NotificationRule.TYPE_COMMENT, null, email, Organization.getByHost(request));
            }
            if (values.containsKey("school_meeting")) {
                NotificationRule.create(NotificationRule.TYPE_SCHOOL_MEETING, null, email, Organization.getByHost(request));
            }
        }

        return redirect(routes.Settings.viewSettings());
    }

    public Result viewTaskLists(Http.Request request) {
        Form<TaskList> list_form = mFormFactory.form(TaskList.class);
        return ok(views.html.view_task_lists.render(TaskList.allForOrg(Organization.getByHost(request)), list_form));
    }

    public Result viewTaskList(Integer id, Http.Request request) {
        TaskList list = TaskList.findById(id, Organization.getByHost(request));
        Form<TaskList> list_form = mFormFactory.form(TaskList.class);
        Form<Task> task_form = mFormFactory.form(Task.class);
        return ok(views.html.settings_task_list.render(
            list, list_form.fill(list), task_form));
    }

    public Result newTask(Http.Request request) {
        Form<Task> task_form = mFormFactory.form(Task.class);
        Task t = task_form.bindFromRequest(request).get();
        t.enabled = true;
        t.save();

        return redirect(routes.Settings.viewTaskList(t.task_list.id));
    }

    public Result newTaskList(Http.Request request) {
        Form<TaskList> list_form = mFormFactory.form(TaskList.class);
        TaskList list = list_form.bindFromRequest(request).get();
        list.organization = Organization.getByHost(request);

        list.title = list.title.trim();
        if (list.title.equals("")) {
            list.title = "Untitled checklist";
        }

        Map<String, String[]> form_data = request.body().asFormUrlEncoded();
        if (form_data.get("tag_id") == null) {
            return redirect(routes.Settings.viewTaskLists()).flashing("error", "No tag specified for checklist. No checklist was created.");
        }
        list.tag = Tag.findById(Integer.parseInt(form_data.get("tag_id")[0]), Organization.getByHost(request));

        list.save();
        list.refresh();

        return redirect(routes.Settings.viewTaskList(list.id));
    }

    public Result editTask(Integer id, Http.Request request) {
        Form<Task> task_form = mFormFactory.form(Task.class);
        Form<Task> filled_form = task_form.fill(Task.findById(id, Organization.getByHost(request)));

        return ok(views.html.edit_task.render(filled_form,
            TaskList.allForOrg(Organization.getByHost(request))));
    }

    public Result saveTask(Http.Request request) {
        Form<Task> task_form = mFormFactory.form(Task.class);
        Form<Task> filled_form = task_form.bindFromRequest(request);
        Task t = filled_form.get();
        if (filled_form.apply("enabled").value().get().equals("false")) {
            t.enabled = false;
        }
        t.update();

        return redirect(routes.Settings.viewTaskList(t.task_list.id));
    }

    public Result saveTaskList(Http.Request request) {
        Form<TaskList> list_form = mFormFactory.form(TaskList.class);
        Form<TaskList> filled_form = list_form.bindFromRequest(request);
        TaskList list = filled_form.get();

        Map<String, String[]> form_data = request.body().asFormUrlEncoded();
        if (form_data.containsKey("tag_id")) {
            list.tag = Tag.findById(Integer.parseInt(form_data.get("tag_id")[0]), Organization.getByHost(request));
        }

        list.update();

        return redirect(routes.Settings.viewTaskList(list.id));
    }

    public Result viewAccess(Http.Request request) {
        Organization org = Organization.getByHost(request);
        List<User> users =
            User.find.query().where().eq("organization", org)
            .order("name ASC")
            .findList();

        List<User> users_to_show = new ArrayList<>();
        for (User user : users) {
            // Hide dummy users and the check-in app user
            if (!user.name.equals(MyUserService.DUMMY_USERNAME) &&
                !user.email.equals(org.short_name)) {
                users_to_show.add(user);
            }
        }

        String allowed_ip = "";
        String sql = "select ip from allowed_ips where organization_id=:org_id";
        SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
        sqlQuery.setParameter("org_id", org.id);
        List<SqlRow> result = sqlQuery.findList();
        if (result.size() > 0) {
            allowed_ip = result.get(0).getString("ip");
        }

        Form<User> user_form = mFormFactory.form(User.class);
        return ok(views.html.view_access.render(users_to_show, allowed_ip, user_form));
    }

    public Result saveAccess(Http.Request request) {
        Map<String, String[]> form_data = request.body().asFormUrlEncoded();
        Organization org = Organization.getByHost(request);

        if (form_data.containsKey("allowed_ip")) {
            String sql = "DELETE from allowed_ips where organization_id=:org_id";
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            update.setParameter("org_id", org.id);
            update.execute();

            sql = "INSERT into allowed_ips (ip, organization_id) VALUES(:ip, :org_id)";
            update = Ebean.createSqlUpdate(sql);
            update.setParameter("org_id", org.id);
            update.setParameter("ip", form_data.get("allowed_ip")[0]);
            update.execute();
        }

        return redirect(routes.Settings.viewAccess());
    }

    public Result editUser(Integer id, Http.Request request) {
        User u = User.findById(id, Organization.getByHost(request));

        Form<User> user_form = mFormFactory.form(User.class);
        return ok(views.html.edit_user.render(u, user_form.fill(u)));
    }

    public Result saveUser(Http.Request request) {
        Form<User> user_form = mFormFactory.form(User.class);
        Form<User> filled_form = user_form.bindFromRequest(request);
        User u = filled_form.get();
        Map<String, String[]> form_data = request.body().asFormUrlEncoded();

        User orig_user = User.findById(u.id, Organization.getByHost(request));

        for (UserRole r : orig_user.roles) {
            r.delete();
        }

        for (String role : UserRole.ALL_ROLES) {
            if (form_data.containsKey(role) && form_data.get(role)[0].equals("true")) {
                UserRole.create(u, role);
            }
        }

        if (filled_form.apply("active").value().get().equals("false")) {
            u.active = false;
            Ebean.deleteAll(orig_user.linkedAccounts);
        }

        if (!orig_user.email.equals(u.email)) {
            Ebean.deleteAll(orig_user.linkedAccounts);
        }

        u.update();

        return redirect(routes.Settings.viewAccess());
    }

    public Result newUser(Http.Request request) {
        Map<String, String[]> form_data = request.body().asFormUrlEncoded();

        Organization org = Organization.getByHost(request);
        final String email = form_data.get("email")[0].trim();
        final String name = form_data.get("name")[0].trim();

        User existing_user = User.findByEmail(email);
        User new_user = null;

        if (existing_user != null) {
            if (existing_user.organization.id.equals(org.id)) {
                if (existing_user.name.equals(MyUserService.DUMMY_USERNAME)) {
                    new_user = existing_user;
                    new_user.name = name;
                    new_user.save();
                } else {
                    return redirect(routes.Settings.viewAccess()).flashing("error", "That email address (" + email + ") already has an account.");
                }
            } else {
                return redirect(routes.Settings.viewAccess()).flashing("error", "That email address (" + email + ") already has an account for another school. " +
                        "Please contact Evan at schmave@gmail.com for help.");
            }
        }

        if (new_user == null) {
            new_user = User.create(email, name, org);
        } else {
            for (UserRole r : new_user.roles) {
                r.delete();
            }
        }

        UserRole.create(new_user, UserRole.ROLE_VIEW_JC);

        return redirect(routes.Settings.viewAccess());
    }
}