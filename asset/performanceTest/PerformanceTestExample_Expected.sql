CREATE TABLE IF NOT EXISTS USER2023 (
  ID BINARY(16) PRIMARY KEY,
  CREATE_TIME DATETIME,
  UPDATE_TIME DATETIME,
  CODE BIGINT UNSIGNED,
  DISPLAY_NAME VARCHAR(100),
  EMAIL VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS USER_DIMENSION2023 (
  ID BINARY(16) PRIMARY KEY,
  CREATE_TIME DATETIME,
  UPDATE_TIME DATETIME,
  USER_ID BINARY(16),
  DIM_NAME VARCHAR(500),
  DIM_VALUE BINARY(16)
);

INSERT INTO USER2023(ID, CREATE_TIME, UPDATE_TIME, DISPLAY_NAME, EMAIL) VALUES (UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), NOW(), NOW(), 1000046, 'flyingfish', 'hCXl0tRqp6g@qmail.com');
INSERT INTO USER2023(ID, CREATE_TIME, UPDATE_TIME, DISPLAY_NAME, EMAIL) VALUES (UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), NOW(), NOW(), 1005102, 'divingcat', 'I3Cv-7-hBjk@vmail.com');

-- {ABCDEFGHIJKLMNOPQRSTUVWXYZ}
/* {ZYXWVUTSRQPONMLKJIHGFEDCBA} */
INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), 'BBS_NRBLFHSV', UUID_TO_BIN('9a32463a300a6f492dfb16bf2f50ce58'));
INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), 'GAME_TVMHSRM', UUID_TO_BIN('2e9f8f71b6f7b434bfa5c03e5f0d40d3'));
INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), 'GAME_HGZIIZRO', UUID_TO_BIN('85447153b0ede2a8ec9ab3cc61defe48'));
INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), 'BBS_NRBLFHSV', UUID_TO_BIN('bf2312129f116234a8d79cfb83bd9475'));
INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), 'GAME_TVMHSRM', UUID_TO_BIN('b43db12a322cf37ee229aa1d5bf10080'));
INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), 'GAME_HGZIIZRO', UUID_TO_BIN('805b7b9f97e1566c533060356a4edf84'));

SELECT T1.ID, T2.DIM_NAME, T2.DIM_VALUE
FROM USER2023 T1
INNER JOIN USER_DIMENSION2023 T2 ON T1.ID = T2.USER_ID
WHERE T1.CODE BETWEEN 1000000 AND 2000000
  AND T2.DIM_NAME = :DIM_NAME
  AND T1.EMAIL LIKE '%mail.com';