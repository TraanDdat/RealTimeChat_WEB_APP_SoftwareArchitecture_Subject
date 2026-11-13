from enum import Enum

class ErrorCode(str, Enum):

    # ==========================================
    # AUTHENTICATION
    # ==========================================
    AUTH_INVALID_CREDENTIALS = "AUTH_001"
    AUTH_INVALID_TOKEN = "AUTH_002"
    AUTH_TOKEN_EXPIRED = "AUTH_003"
    AUTH_MISSING_TOKEN = "AUTH_004"
    AUTH_INVALID_REFRESH_TOKEN = "AUTH_005"
    AUTH_ACCOUNT_DISABLED = "AUTH_006"
    AUTH_ACCOUNT_NOT_VERIFIED = "AUTH_007"
    AUTH_PASSWORD_INCORRECT = "AUTH_008"
    AUTH_RESET_TOKEN_EXPIRED = "AUTH_009"
    AUTH_PASSWORD_RESET_FAILED = "AUTH_010"

    # ==========================================
    # USER
    # ==========================================
    USER_NOT_FOUND = "USER_001"
    USER_ALREADY_EXISTS = "USER_002"
    USER_INVALID_DATA = "USER_003"
    USER_EMAIL_ALREADY_EXISTS = "USER_004"
    USER_USERNAME_ALREADY_EXISTS = "USER_006"
    USER_PROFILE_UPDATE_FAILED = "USER_007"
    USER_DELETE_FAILED = "USER_008"

    # ==========================================
    # PERMISSION / AUTHORIZATION
    # ==========================================
    PERM_ACCESS_DENIED = "PERM_001"
    PERM_INSUFFICIENT_PRIVILEGES = "PERM_002"
    PERM_RESOURCE_ACCESS_DENIED = "PERM_005"

    # ==========================================
    # VALIDATION
    # ==========================================
    VAL_REQUIRED_FIELD = "VAL_001"
    VAL_INVALID_FORMAT = "VAL_002"
    VAL_INVALID_VALUE = "VAL_003"
    VAL_LENGTH_EXCEEDED = "VAL_004"
    VAL_INVALID_REQUEST_BODY = "VAL_005"
    VAL_INVALID_QUERY_PARAMETER = "VAL_006"
    VAL_INVALID_PATH_PARAMETER = "VAL_007"

    # ==========================================
    # DATABASE
    # ==========================================
    DB_CONNECTION_FAILED = "DB_001"
    DB_QUERY_FAILED = "DB_002"
    DB_TRANSACTION_FAILED = "DB_003"
    DB_CONSTRAINT_VIOLATION = "DB_004"
    DB_TIMEOUT = "DB_005"
    DB_UNAVAILABLE = "DB_006"

    # ==========================================
    # FILE / MEDIA
    # ==========================================
    FILE_NOT_FOUND = "FILE_001"
    FILE_UPLOAD_FAILED = "FILE_002"
    FILE_DOWNLOAD_FAILED = "FILE_003"
    FILE_UNSUPPORTED_FORMAT = "FILE_004"
    FILE_SIZE_EXCEEDED = "FILE_005"
    FILE_CORRUPTED = "FILE_006"
    FILE_PERMISSION_DENIED = "FILE_007"

    # ==========================================
    # CHAT / MESSAGE
    # ==========================================
    CHAT_CONVERSATION_NOT_FOUND = "CHAT_001"
    CHAT_MESSAGE_NOT_FOUND = "CHAT_002"
    CHAT_SEND_MESSAGE_FAILED = "CHAT_003"
    CHAT_NOT_CONVERSATION_MEMBER = "CHAT_004"
    CHAT_DELETE_MESSAGE_FAILED = "CHAT_005"
    CHAT_CONVERSATION_ALREADY_EXISTS = "CHAT_006"

    # ==========================================
    # FRIEND
    # ==========================================
    FRIEND_NOT_FOUND = "FRIEND_001"
    FRIEND_REQUEST_ALREADY_SENT = "FRIEND_002"
    FRIEND_REQUEST_NOT_FOUND = "FRIEND_003"
    FRIEND_CANNOT_ADD_SELF = "FRIEND_004"
    FRIEND_ALREADY_EXISTS = "FRIEND_005"
    FRIEND_REQUEST_REJECTED = "FRIEND_006"

    # ==========================================
    # SYSTEM
    # ==========================================
    SYS_INTERNAL_SERVER_ERROR = "SYS_001"
    SYS_SERVICE_UNAVAILABLE = "SYS_002"
    SYS_REQUEST_TIMEOUT = "SYS_003"
    SYS_CONFIGURATION_ERROR = "SYS_004"
    SYS_EXTERNAL_SERVICE_ERROR = "SYS_005"
    SYS_UNEXPECTED_ERROR = "SYS_006"