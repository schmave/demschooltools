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
}
