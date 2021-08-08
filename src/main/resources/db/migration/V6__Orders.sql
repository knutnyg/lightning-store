ALTER TABLE invoices
    ADD COLUMN amount bigint default 0;

CREATE TABLE products
(
    id    VARCHAR(36) PRIMARY KEY,
    name  VARCHAR(50),
    price bigint
);

CREATE TABLE orders
(
    id         VARCHAR(36) PRIMARY KEY,
    token_id   VARCHAR(36) REFERENCES token (id)    NOT NULL,
    invoice_id VARCHAR(36) REFERENCES invoices (id) NOT NULL,
    product_id VARCHAR(36) REFERENCES products (id) NOT NULL
);
