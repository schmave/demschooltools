package com.feth.play.module.pa.service;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import play.Logger;

public abstract class AbstractUserService implements UserService {

  protected PlayAuthenticate auth;
  static Logger.ALogger sLogger = Logger.of("application");

  public AbstractUserService(PlayAuthenticate auth) {
    this.auth = auth;
    onStart();
  }

  protected void onStart() {
    if (this.auth.hasUserService()) {
      final String oldServiceClass = this.auth.getUserService().getClass().getName();

      sLogger.warn(
          "A user service was already registered - replacing the old one ("
              + oldServiceClass
              + ") with the new one ("
              + getClass().getName()
              + "), however this might hint to a configuration problem if this is a production"
              + " environment.");
    }
    this.auth.setUserService(this);
  }

  @Override
  public AuthUser update(AuthUser knownUser) {
    // Default: just do nothing when user logs in again
    return knownUser;
  }
}
