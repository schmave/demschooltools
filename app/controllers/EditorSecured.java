package controllers;

import play.Logger;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class EditorSecured extends Secured {

	@Override
	public String getUsername(final Context ctx) {
        return getUsername(ctx, false);
	}

	@Override
	public Result onUnauthorized(final Context ctx) {
		return forbidden();
	}
}