import logging

import jwt
from django.conf import settings
from django.contrib import auth
from django.contrib.auth.backends import BaseBackend
from django.http import HttpRequest
from django.utils.deprecation import MiddlewareMixin

from dst.models import AllowedIp, Organization, User, UserRole

LOGGER = logging.getLogger(__name__)


def get_client_ip(request: HttpRequest) -> str:
    """Get the client IP address from the request, handling proxies."""
    x_forwarded_for = request.META.get("HTTP_X_FORWARDED_FOR")
    if x_forwarded_for:
        # Take the first IP in the chain (the original client)
        ip = x_forwarded_for.split(",")[0].strip()
    else:
        ip = request.META.get("REMOTE_ADDR", "")
    return ip or ""


def get_or_create_ip_user(client_ip: str, organization: Organization) -> User:
    username = client_ip
    email = client_ip
    name = f"IP Access User - {organization.name}"

    user, unused_created = User.objects.get_or_create(
        username=username,
        organization=organization,
        defaults={
            "email": email,
            "name": name,
            "is_active": True,
            "email_validated": False,
        },
    )

    UserRole.objects.get_or_create(user=user, role=UserRole.VIEW_JC)

    return user


def get_ip_user(request, org):
    client_ip = get_client_ip(request)
    LOGGER.debug(f"Looking for {client_ip=}")
    if not client_ip:
        return None

    if AllowedIp.objects.filter(ip=client_ip, organization=org).exists():
        user = get_or_create_ip_user(client_ip, org)

        LOGGER.info(f"Found AllowedIp for IP {client_ip}, org {org.name}")
        return user

    return None


class PlaySessionBackend(BaseBackend):
    def authenticate(
        self, request: HttpRequest, username=None, password=None, **kwargs
    ):
        raise NotImplementedError("PlaySessionBackend doesn't do authenticate()")

    def get_user(self, user_id):
        return User.objects.filter(id=user_id).first()


def get_user_for_play_session(request) -> User | None:
    raw_token = request.COOKIES.get("PLAY_SESSION")
    if not raw_token:
        return None
    try:
        data = jwt.decode(raw_token, settings.APPLICATION_SECRET, algorithms=["HS256"])
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

    * Authenticate using a JWT sent as the cookie named "PLAY_SESSION".
        * or an IP address
    * Set request.org
    """

    def process_request(self, request: HttpRequest):
        new_user = get_user_for_play_session(request)

        org = Organization.objects.get(hosts__host=request.get_host())

        if new_user is None:
            new_user = get_ip_user(request, org)

        if new_user is None and request.user.is_authenticated:
            LOGGER.debug("JWT/IP user went away, logging out")
            auth.logout(request)
            return

        if new_user is not None:
            if request.user.id != new_user.id:
                # If the user is already authenticated and that user is the user we are
                # getting passed in the headers, then the correct user is already
                # persisted in the session and we don't need to continue.
                LOGGER.debug("got a new JWT/IP user, logging them in")
                auth.logout(request)
                auth.login(request, new_user, "demschooltools.auth.PlaySessionBackend")

            request.org = org
            LOGGER.info(f"host={request.get_host()}, organization={request.org}")
