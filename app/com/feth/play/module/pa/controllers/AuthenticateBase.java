package com.feth.play.module.pa.controllers;

import play.mvc.Controller;

import play.mvc.Result;

public class AuthenticateBase extends Controller {

	protected static final String PAYLOAD_KEY = "p";

	public static Result noCache(final Result result) {
		// http://stackoverflow.com/questions/49547/making-sure-a-web-page-is-not-cached-across-all-browsers
		return result.withHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate") // HTTP 1.1
				.withHeader(PRAGMA, "no-cache")  // HTTP 1.0.
		.withHeader(EXPIRES, "0");  // Proxies.
	}
}
