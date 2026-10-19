CREATE SCHEMA identity;

CREATE TABLE identity.app_user (
                                   id                 uuid PRIMARY KEY,
                                   email              varchar(255) NOT NULL UNIQUE,
                                   password_hash      varchar(255) NOT NULL,
                                   role               varchar(20)  NOT NULL,
                                   failed_login_count integer      NOT NULL DEFAULT 0,
                                   locked_until       timestamptz,
                                   created_at         timestamptz  NOT NULL
);