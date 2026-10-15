-- Vista materializada del join con refresco incremental y query rewrite.
-- Requisitos: privilegios CREATE MATERIALIZED VIEW y QUERY REWRITE; parámetro query_rewrite_enabled = TRUE.

CREATE MATERIALIZED VIEW LOG ON orders
  WITH ROWID, SEQUENCE (customer_id, order_date, total_amount)
  INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW LOG ON customers
  WITH ROWID, SEQUENCE (customer_id, customer_name, country)
  INCLUDING NEW VALUES;

-- Los ROWID de ambas tablas son obligatorios para FAST REFRESH en vistas de join
CREATE MATERIALIZED VIEW mv_pedidos_por_pais
  BUILD IMMEDIATE
  REFRESH FAST ON DEMAND
  ENABLE QUERY REWRITE
AS
SELECT c.country,
       o.order_id,
       o.order_date,
       c.customer_name,
       o.total_amount,
       o.ROWID AS o_rid,
       c.ROWID AS c_rid
FROM   orders o
JOIN   customers c ON o.customer_id = c.customer_id;

CREATE INDEX ix_mv_pedidos_pais ON mv_pedidos_por_pais (country, order_date);

-- Refresco programado cada 5 minutos (alternativa a ON COMMIT, que encarece cada transacción)
BEGIN
  DBMS_SCHEDULER.CREATE_JOB(
    job_name        => 'JOB_REFRESH_MV_PEDIDOS',
    job_type        => 'PLSQL_BLOCK',
    job_action      => 'BEGIN DBMS_MVIEW.REFRESH(''MV_PEDIDOS_POR_PAIS'', method => ''F''); END;',
    repeat_interval => 'FREQ=MINUTELY;INTERVAL=5',
    enabled         => TRUE);
END;
/

-- Con query rewrite, la consulta original se reescribe sola contra la vista:
-- EXPLAIN PLAN FOR SELECT ... WHERE c.country = 'México';  -- debe mostrar MAT_VIEW REWRITE ACCESS
