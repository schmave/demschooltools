package controllers;

import java.io.*;
import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.SqlUpdate;

import models.*;

import play.*;
import play.data.*;
import play.mvc.*;

@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
@With(DumpOnError.class)
public class Settings extends Controller {

    static Form<Task> task_form = Form.form(Task.class);
    static Form<TaskList> list_form = Form.form(TaskList.class);
    static Form<User> user_form = Form.form(User.class);

    public static Result viewNotifications() {
        List<NotificationRule> rules = NotificationRule.find.where()
            .eq("organization", OrgConfig.get().org)
            .order("the_type DESC, tag.id")
            .findList();

        return ok(views.html.view_notifications.render(rules));
    }

    public static Result editNotifications() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        if (values.containsKey("remove_notification_id")) {
            NotificationRule.findById(
                Integer.parseInt(values.get("remove_notification_id")[0])).delete();
        } else {
			String email = values.get("email")[0];
			if (!email.matches("^\\S+@\\S+.\\S+$")) {
				flash("error", "'" + email + "' does not seem to be a valid email address. Notification not created.");
				return redirect(routes.Settings.viewNotifications());
			}

			if (values.containsKey("tag_id")) {
				Tag t = Tag.findById(Integer.parseInt(values.get("tag_id")[0]));
				NotificationRule.create(NotificationRule.TYPE_TAG, t, email);
			}

			if (values.containsKey("comment")) {
				NotificationRule.create(NotificationRule.TYPE_COMMENT, null, email);
			}

			if (values.containsKey("donation")) {
				NotificationRule.create(NotificationRule.TYPE_DONATION, null, email);
			}
		}
		
        return redirect(routes.Settings.viewNotifications());
    }

    public static Result viewTaskLists() {
        return ok(views.html.view_task_lists.render(TaskList.allForOrg(), list_form));
    }

    public static Result viewTaskList(Integer id) {
        TaskList list = TaskList.findById(id);
        return ok(views.html.settings_task_list.render(
            list, list_form.fill(list), task_form));
    }

    public static Result newTask() {
        Task t = task_form.bindFromRequest().get();
        t.enabled = true;
        t.save();

        return redirect(routes.Settings.viewTaskList(t.task_list.id));
    }

    public static Result newTaskList() {
        TaskList list = list_form.bindFromRequest().get();
        list.organization = OrgConfig.get().org;

        Map<String, String[]> form_data = request().body().asFormUrlEncoded();
        list.tag = Tag.findById(Integer.parseInt(form_data.get("tag_id")[0]));

        list.save();
        list.refresh();

        return redirect(routes.Settings.viewTaskList(list.id));
    }

    public static Result editTask(Integer id) {
        Form<Task> filled_form = task_form.fill(Task.findById(id));

        return ok(views.html.edit_task.render(filled_form,
            TaskList.allForOrg()));
    }

    public static Result saveTask() {
        Form<Task> filled_form = task_form.bindFromRequest();
        Task t = filled_form.get();
        if (filled_form.apply("enabled").value().equals("false")) {
            t.enabled = false;
        }
        t.update();

        return redirect(routes.Settings.viewTaskList(t.task_list.id));
    }

    public static Result saveTaskList() {
        Form<TaskList> filled_form = list_form.bindFromRequest();
        TaskList list = filled_form.get();

        Map<String, String[]> form_data = request().body().asFormUrlEncoded();
        if (form_data.containsKey("tag_id")) {
            list.tag = Tag.findById(Integer.parseInt(form_data.get("tag_id")[0]));
        }

        list.update();

        return redirect(routes.Settings.viewTaskList(list.id));
    }

    public static Result viewAccess() {
        Organization org = OrgConfig.get().org;
        List<User> users =
            User.find.where().eq("organization", org)
            .order("name ASC")
            .findList();

        String allowed_ip = "";
        String sql = "select ip from allowed_ips where organization_id=:org_id";
        SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
        sqlQuery.setParameter("org_id", org.id);
        List<SqlRow> result = sqlQuery.findList();
        if (result.size() > 0) {
            allowed_ip = result.get(0).getString("ip");
        }

        return ok(views.html.view_access.render(users, allowed_ip, user_form));
    }

    public static Result saveAccess() {
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();
        Organization org = OrgConfig.get().org;

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

    public static Result editUser(Integer id) {
        User u = User.findById(id);

        return ok(views.html.edit_user.render(u, user_form.fill(u)));
    }

    public static Result saveUser() {
        Form<User> filled_form = user_form.bindFromRequest();
        User u = filled_form.get();
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();

        User orig_user = User.findById(u.id);

        for (UserRole r : orig_user.roles) {
            r.delete();
        }

        for (String role : UserRole.ALL_ROLES) {
            if (form_data.containsKey(role) && form_data.get(role)[0].equals("true")) {
                UserRole.create(u, role);
            }
        }

        if (filled_form.apply("active").value().equals("false")) {
            u.active = false;
            Ebean.delete(orig_user.linkedAccounts);
        }

        if (!orig_user.email.equals(u.email)) {
            Ebean.delete(orig_user.linkedAccounts);
        }

        u.update();

        return redirect(routes.Settings.viewAccess());
    }

    public static Result newUser() {
        Map<String, String[]> form_data = request().body().asFormUrlEncoded();

        User u = User.create(
            form_data.get("email")[0].trim(),
            form_data.get("name")[0].trim(),
            OrgConfig.get().org);

        UserRole.create(u, UserRole.ROLE_VIEW_JC);

        return redirect(routes.Settings.viewAccess());
    }
}

