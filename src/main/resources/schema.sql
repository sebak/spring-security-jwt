-- data base springbootsecuritydb must be first created manually, activate in properties file : spring.sql.init.mode=always
-- select nextval('users_id_seq'); to see the next value of id
CREATE SEQUENCE IF NOT EXISTS users_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS users (
    id SERIAL NOT NULL,
    email VARCHAR(255) not null,
    password VARCHAR(255) not null,
    full_name VARCHAR(255) not null,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id)
)