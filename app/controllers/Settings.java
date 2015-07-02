package controllers;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Expression;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.csvreader.CsvWriter;

import models.*;

import play.*;
import play.data.*;
import play.mvc.*;
import play.mvc.Http.Context;

@Security.Authenticated(EditorSecured.class)
@With(DumpOnError.class)
public class Settings extends Controller {

    static Form<Task> task_form = Form.form(Task.class);
    static Form<TaskList> list_form = Form.form(TaskList.class);

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
        }

        if (values.containsKey("tag_id")) {
            NotificationRule.create(
                NotificationRule.TYPE_TAG,
                Tag.findById(Integer.parseInt(values.get("tag_id")[0])),
                values.get("email")[0]);
        }

        if (values.containsKey("comment")) {
            NotificationRule.create(
                NotificationRule.TYPE_COMMENT,
                null,
                values.get("email")[0]);
        }

        if (values.containsKey("donation")) {
            NotificationRule.create(
                NotificationRule.TYPE_DONATION,
                null,
                values.get("email")[0]);
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
}

