-- Particionamiento (opción Partitioning de Oracle Enterprise Edition).
-- Migración en línea de la tabla existente a particionada por intervalo mensual (Oracle 12.2+).

ALTER TABLE orders MODIFY
  PARTITION BY RANGE (order_date) INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
  (PARTITION p_historico VALUES LESS THAN (DATE '2020-01-01'))
  ONLINE
  UPDATE INDEXES (ix_orders_customer_cov LOCAL);

-- customers por lista de país normalizado
ALTER TABLE customers MODIFY
  PARTITION BY LIST (country_code)
  (PARTITION p_mx VALUES ('MX'),
   PARTITION p_co VALUES ('CO'),
   PARTITION p_us VALUES ('US'),
   PARTITION p_otros VALUES (DEFAULT))
  ONLINE;

-- Consulta que aprovecha partition pruning (filtra por rango de fechas)
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM   customers c
JOIN   orders o ON o.customer_id = c.customer_id
WHERE  c.country_code = 'MX'
AND    o.order_date >= ADD_MONTHS(TRUNC(SYSDATE), -3)
ORDER  BY o.order_date DESC
FETCH FIRST 50 ROWS ONLY;
