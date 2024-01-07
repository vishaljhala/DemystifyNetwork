CREATE TABLE STATS
(
    PK serial NOT NULL,
    USER_ID integer NOT NULL,
    ADDRESS text NOT NULL,
    ENDPOINT text NOT NULL,
    SCORE text,
    INVOKED_AT timestamp without time zone NOT NULL,
    IP text,
    ADDITIONAL_INFO text,
    INSIGHTS text,
    PRIMARY KEY (PK)
);

CREATE TABLE USERS
(
    pk serial NOT NULL,
    email text NOT NULL,
    password text NOT NULL,
    is_active boolean DEFAULT true,
    expiry_date timestamp without time zone NOT NULL,
    daily_calls integer NOT NULL,
    monthly_calls integer NOT NULL,
    concurrent_calls integer NOT NULL DEFAULT 1,
    PRIMARY KEY (pk)
);
