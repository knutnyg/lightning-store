ALTER TABLE products
    ADD COLUMN uri varchar(200);

INSERT INTO products(id, name, price, payload, uri)
VALUES ('a1afc48b-23bc-4297-872a-5e7884d6975a', 'IMAGE-1', 1, null, 'ai-1.png');