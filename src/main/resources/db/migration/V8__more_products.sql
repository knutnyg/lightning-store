CREATE TABLE bundle
(
    id   serial PRIMARY KEY,
    name varchar(50)
);

CREATE TABLE bundle_product
(
    bundle_id  int references bundle (id),
    product_id VARCHAR(36) references products (id)
);

ALTER TABLE products
    ADD COLUMN uri varchar(200);

ALTER TABLE products
    ADD COLUMN bundle_id int references bundle (id);

INSERT INTO bundle (name)
VALUES ('GALLERY');

INSERT INTO products(id, name, price, payload, uri, bundle_id)
VALUES ('ec533145-47fa-464e-8cf0-fd36e3709ad3', 'GALLERY-BUNDLE', 1, null, null, 1);

INSERT INTO products(id, name, price, payload, uri)
VALUES ('a1afc48b-23bc-4297-872a-5e7884d6975a', 'IMAGE-1', 1, null, 'file://ai-1.png');

INSERT INTO bundle_product(bundle_id, product_id)
VALUES (1, 'a1afc48b-23bc-4297-872a-5e7884d6975a')