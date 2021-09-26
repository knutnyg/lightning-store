INSERT INTO bundle (name)
VALUES ('TOKEN_GALLERY_BUNDLE');

UPDATE products
SET bundle_id = 2
where id = 'a64d4344-f964-4dfe-99a6-7b39a7eb91c1';

INSERT INTO bundle_product(bundle_id, product_id)
VALUES (2, 'a1afc48b-23bc-4297-872a-5e7884d6975a')