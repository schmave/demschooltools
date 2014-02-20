package controllers;

import play.Logger;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class EditorSecured extends Secured {

	@Override
	public String getUsername(final Context ctx) {
        String result = getUsername(ctx, false);
        if (result == null) {
            ctx.flash().put("notice", "You are trying to access a page that requires you to log in.");
        }

        return result;
	}

}