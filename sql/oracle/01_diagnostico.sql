-- Diagnóstico: plan estimado, plan real y estadísticas
EXPLAIN PLAN FOR
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM   orders o
JOIN   customers c ON o.customer_id = c.customer_id
WHERE  c.country = 'México';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(format => 'TYPICAL'));

-- Plan real con filas estimadas vs. reales
SELECT /*+ GATHER_PLAN_STATISTICS */ o.order_id, o.order_date, c.customer_name, o.total_amount
FROM   orders o
JOIN   customers c ON o.customer_id = c.customer_id
WHERE  c.country = 'México';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));

-- ¿Existe índice sobre la FK orders.customer_id?
SELECT index_name, column_name, column_position
FROM   user_ind_columns
WHERE  table_name = 'ORDERS'
ORDER  BY index_name, column_position;

-- Distribución de clientes por país (justifica el histograma)
SELECT country, COUNT(*) AS clientes
FROM   customers
GROUP  BY country
ORDER  BY clientes DESC
FETCH FIRST 20 ROWS ONLY;

-- Estadísticas e histograma sobre country
BEGIN
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'CUSTOMERS', method_opt => 'FOR ALL COLUMNS SIZE AUTO', cascade => TRUE);
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'ORDERS', method_opt => 'FOR ALL COLUMNS SIZE AUTO', cascade => TRUE);
END;
/

SELECT column_name, num_distinct, histogram
FROM   user_tab_col_statistics
WHERE  table_name = 'CUSTOMERS' AND column_name = 'COUNTRY';
