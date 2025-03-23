import logging

import jwt
from django.conf import settings
from django.contrib import auth
from django.contrib.auth.backends import BaseBackend
from django.http import HttpRequest
from django.utils.deprecation import MiddlewareMixin

from dst.models import Organization, User

LOGGER = logging.getLogger(__name__)


class PlaySessionBackend(BaseBackend):
    def authenticate(self, request: HttpRequest, username=None, password=None):
        raise NotImplementedError("PlaySessionBackend doesn't do authenticate()")

    def get_user(self, user_id):
        return User.objects.filter(id=user_id).first()


def get_user_for_play_session(request) -> User | None:
    raw_token = request.COOKIES.get("PLAY_SESSION")
    if not raw_token:
        return None
    try:
        data = jwt.decode(raw_token, settings.JWT_KEY, algorithms=["HS256"])
    except Exception as e:
        print(e)
        return None

    LOGGER.debug(f"Decoded JWT {data=}")
    inner_data = data["data"]
    pa_user_id = inner_data.get("pa.u.id")
    pa_provider_id = inner_data.get("pa.p.id")
    if pa_user_id is None or pa_provider_id is None:
        return
    if pa_provider_id == "evan-auth-provider":
        result = User.objects.get(email=pa_user_id)
    else:
        result = User.objects.get(
            linked_accounts__provider_user_id=pa_user_id,
            linked_accounts__provider_key=pa_provider_id,
        )
    return result


class PlaySessionMiddleware(MiddlewareMixin):
    """
    Based on django.contrib.auth.RemoteUserMiddleware
    AuthenticationMiddleware is required so that request.user exists.

    Authenticate using a JWT sent as the cookie named "PLAY_SESSION".
    """

    def process_request(self, request: HttpRequest):
        new_user = get_user_for_play_session(request)
        if new_user is None and request.user.is_authenticated:
            LOGGER.debug("JWT user went away, logging out")
            auth.logout(request)
            return

        if new_user is not None:
            if request.user.id != new_user.id:
                # If the user is already authenticated and that user is the user we are
                # getting passed in the headers, then the correct user is already
                # persisted in the session and we don't need to continue.
                LOGGER.debug("got a new JWT user, logging them in")
                auth.logout(request)
                auth.login(request, new_user, "demschooltools.auth.PlaySessionBackend")

            request.org = Organization.objects.get(hosts__host=request.get_host())
            LOGGER.info(f"host={request.get_host()}, organization={request.org}")
