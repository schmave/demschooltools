package module;

import java.util.*;

import com.feth.play.module.pa.providers.oauth2.OAuth2AuthProvider;
import com.feth.play.module.pa.providers.oauth2.OAuth2AuthInfo;

public class MyFacebookAuthInfo extends OAuth2AuthInfo  {
    public MyFacebookAuthInfo(final Map<String, String> m) {
        super(  m.get(OAuth2AuthProvider.Constants.ACCESS_TOKEN),
                    new Date().getTime() +
                    Long.parseLong(m.get(OAuth2AuthProvider.Constants.EXPIRES_IN)) * 1000,
                m.get(OAuth2AuthProvider.Constants.REFRESH_TOKEN));
    }
}