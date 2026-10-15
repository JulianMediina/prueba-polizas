-- Reproduce el escenario del Módulo 3 en PostgreSQL con datos sintéticos y compara planes.
-- Uso: psql -d <base_de_pruebas> -f sql/postgres/benchmark.sql
-- Volumen por defecto: 500 000 clientes y 10 000 000 de pedidos (~1 GB). Ajuste las variables para algo más liviano.
\set clientes 500000
\set pedidos 10000000
\timing on

DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers (
  customer_id   BIGINT PRIMARY KEY,
  customer_name TEXT NOT NULL,
  country       TEXT NOT NULL
);

CREATE TABLE orders (
  order_id     BIGINT PRIMARY KEY,
  customer_id  BIGINT NOT NULL REFERENCES customers (customer_id),
  order_date   DATE NOT NULL,
  total_amount NUMERIC(14, 2) NOT NULL
);

-- Distribución sesgada: México ~3 % de los clientes
INSERT INTO customers
SELECT i,
       'Cliente ' || i,
       CASE WHEN i % 33 = 0 THEN 'México'
            WHEN i % 5 = 0 THEN 'Colombia'
            WHEN i % 3 = 0 THEN 'Perú'
            ELSE 'Estados Unidos' END
FROM generate_series(1, :clientes) AS i;

INSERT INTO orders
SELECT i,
       1 + (random() * (:clientes - 1))::BIGINT,
       DATE '2020-01-01' + (random() * 2000)::INT,
       round((random() * 5000)::NUMERIC, 2)
FROM generate_series(1, :pedidos) AS i;

ANALYZE customers;
ANALYZE orders;

\echo '=== ANTES: sin índices (seq scan de 10 M filas + hash join) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'México';

CREATE INDEX ix_customers_country_cov ON customers (country) INCLUDE (customer_id, customer_name);
CREATE INDEX ix_orders_customer_cov ON orders (customer_id) INCLUDE (order_date, total_amount, order_id);
VACUUM ANALYZE customers;
VACUUM ANALYZE orders;

\echo '=== DESPUÉS: índices de cobertura (index-only scans + nested loop) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'México';

\echo '=== Con paginación y rango de fechas (caso de uso real) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
WHERE c.country = 'México'
  AND o.order_date >= DATE '2025-01-01'
ORDER BY o.order_date DESC
LIMIT 50;

-- Equivalente a la vista materializada de Oracle (refresco manual o programado con pg_cron)
CREATE MATERIALIZED VIEW mv_pedidos_por_pais AS
SELECT c.country, o.order_id, o.order_date, c.customer_name, o.total_amount
FROM orders o JOIN customers c ON o.customer_id = c.customer_id;
CREATE INDEX ix_mv_pedidos_pais ON mv_pedidos_por_pais (country, order_date DESC);
CREATE UNIQUE INDEX ux_mv_pedidos_order ON mv_pedidos_por_pais (order_id);  -- habilita REFRESH CONCURRENTLY

\echo '=== Vista materializada ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT order_id, order_date, customer_name, total_amount
FROM mv_pedidos_por_pais
WHERE country = 'México';
