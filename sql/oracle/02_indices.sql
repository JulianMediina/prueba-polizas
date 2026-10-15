-- Índices de cobertura: el filtro y el join se resuelven con índices, sin leer las tablas.
-- ONLINE evita bloquear DML durante la creación (Enterprise Edition).

CREATE INDEX ix_customers_country_cov
  ON customers (country, customer_id, customer_name)
  ONLINE;

CREATE INDEX ix_orders_customer_cov
  ON orders (customer_id, order_date, total_amount, order_id)
  ONLINE;

-- Alternativa mínima si orders tiene mucha escritura: solo la FK
-- CREATE INDEX ix_orders_customer ON orders (customer_id) ONLINE;

-- Variante con país normalizado a ISO
-- ALTER TABLE customers ADD country_code CHAR(2);
-- UPDATE customers SET country_code = 'MX' WHERE country IN ('México', 'Mexico', 'MEXICO');
-- ALTER TABLE customers ADD CONSTRAINT ck_customers_country_code CHECK (REGEXP_LIKE(country_code, '^[A-Z]{2}$'));
-- CREATE INDEX ix_customers_cc_cov ON customers (country_code, customer_id, customer_name) ONLINE;

BEGIN
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'CUSTOMERS', cascade => TRUE);
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'ORDERS', cascade => TRUE);
END;
/
