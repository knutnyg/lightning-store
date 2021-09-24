ALTER TABLE products add column mediatype text not null default 'text';
ALTER TABLE products add column payload_v2 bytea