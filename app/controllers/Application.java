package controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.feth.play.module.pa.PlayAuthenticate;

import models.*;

import play.*;
import play.data.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;

@Security.Authenticated(Secured.class)
public class Application extends Controller {

    public static Result index() {
        return ok(views.html.index.render());
    }

    public static Result editTodaysMinutes() {
        Meeting the_meeting = Meeting.find.where().eq("date", new Date()).findUnique();
        if (the_meeting == null) {
            the_meeting = Meeting.create(new Date());
            the_meeting.save();
        }
        return editMinutes(the_meeting);
    }

    public static Result editMinutes(Meeting meeting) {
        return ok(views.html.edit_minutes.render(meeting));
    }
}
