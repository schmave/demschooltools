package com.feth.play.module.pa.providers.oauth2.facebook;

import static com.feth.play.module.pa.providers.oauth2.OAuth2AuthProvider.Constants.ACCESS_TOKEN;
import static com.feth.play.module.pa.providers.oauth2.OAuth2AuthProvider.Constants.REFRESH_TOKEN;

import com.fasterxml.jackson.databind.JsonNode;
import com.feth.play.module.pa.providers.oauth2.OAuth2AuthInfo;
import java.util.Date;

public class FacebookAuthInfo extends OAuth2AuthInfo {

  /** */
  private static final long serialVersionUID = 1L;

  private static final String EXPIRES_IN = "expires_in";

  public FacebookAuthInfo(final JsonNode json) {
    super(
        json.get(ACCESS_TOKEN).asText(),
        new Date().getTime() + json.get(EXPIRES_IN).asLong() * 1000,
        json.get(REFRESH_TOKEN) != null ? json.get(REFRESH_TOKEN).asText() : null);
  }
}
