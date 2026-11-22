from http.cookies import SimpleCookie
from zalo_backend.core.security import AUTH_COOKIE, decode_access_token

def _user_id_from_cookie(environ) -> int | None:
    morsel = SimpleCookie(environ.get("HTTP_COOKIE", "")).get(AUTH_COOKIE)
    return decode_access_token(morsel.value) if morsel else None

def _user_id_from_bearer(environ) -> int | None:
    authorization = environ.get("HTTP_AUTHORIZATION", "")
    if not authorization.startswith("Bearer "):
        return None

    token = authorization.removeprefix("Bearer ").strip()
    return decode_access_token(token)

def get_socket_user_id(environ) -> int | None:
    cookie_id = _user_id_from_cookie(environ)
    bearer_id = _user_id_from_bearer(environ)

    if cookie_id is not None and bearer_id is not None:
        if cookie_id != bearer_id: # cannot verify 2 user_id different
            return None
        return cookie_id # return cookie_id from web

    if cookie_id is not None:
        return cookie_id

    if bearer_id is not None:
        return bearer_id
    
    return None