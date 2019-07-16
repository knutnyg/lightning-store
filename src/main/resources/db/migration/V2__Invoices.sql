CREATE TABLE invoices(
  id VARCHAR(36) PRIMARY KEY,
  rhash VARCHAR(50),
  payment_req VARCHAR(500),
  settled TIMESTAMP,
  memo VARCHAR(200)
);
