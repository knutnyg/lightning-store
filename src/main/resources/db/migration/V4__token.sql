CREATE TABLE token(
                         id VARCHAR(36) PRIMARY KEY,
                         balance int,
                         macaroon VARCHAR(500),
                         revoked bool
);
