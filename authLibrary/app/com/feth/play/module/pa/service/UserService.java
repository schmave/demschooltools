package com.feth.play.module.pa.service;

import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import play.mvc.Http;

public interface UserService {

  /**
   * Saves auth provider/id combination to a local user
   *
   * @param authUser
   * @return The local identifying object or null if the user existed
   */
  public Object save(final AuthUser authUser, final Http.Request request);

  /**
   * Returns the local identifying object if the auth provider/id combination has been linked to a
   * local user account already or null if not. This gets called on any login to check whether the
   * session user still has a valid corresponding local user
   *
   * @param identity
   * @return
   */
  public Object getLocalIdentity(final AuthUserIdentity identity);
}
