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

}

