# Módulo 3 – Optimización de la consulta de pedidos por país

```sql
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM   orders o
JOIN   customers c ON o.customer_id = c.customer_id
WHERE  c.country = 'México';
```

`orders`: 10 M filas · `customers`: 500 K filas · consulta frecuente y lenta.

## Diagnóstico: por qué es lenta

Sin índices adecuados, el plan típico es **full scan de `customers`** (500 K filas para quedarse con las de
México) y **full scan de `orders`** (10 M filas) con un *hash join*. El costo lo domina leer `orders` completa.
Las causas más comunes:

1. No hay índice sobre la FK `orders.customer_id` (Oracle **no** lo crea automáticamente).
2. No hay índice sobre `customers.country`, o las estadísticas no reflejan la distribución sesgada del país.
3. Se leen columnas que obligan a ir a la tabla (`customer_name`, `order_date`, `total_amount`).
4. El texto `'México'` es frágil: tildes, mayúsculas y *collation* ("México" ≠ "Mexico").

**Primer paso siempre: medir.** En Oracle:

```sql
EXPLAIN PLAN FOR <consulta>;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
-- Plan real con estadísticas de ejecución:
SELECT /*+ GATHER_PLAN_STATISTICS */ ... ;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

En PostgreSQL: `EXPLAIN (ANALYZE, BUFFERS)`. Se compara *estimated rows* contra *actual rows*: si difieren mucho,
el problema son las estadísticas antes que los índices.

## Estrategias (priorizadas)

### 1. Estadísticas e histogramas al día

```sql
BEGIN
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'CUSTOMERS', method_opt => 'FOR COLUMNS SIZE AUTO COUNTRY', cascade => TRUE);
  DBMS_STATS.GATHER_TABLE_STATS(USER, 'ORDERS', cascade => TRUE);
END;
/
```

- **Qué hace:** el optimizador conoce cuántos clientes hay por país (histograma) y elige el plan correcto: si
  México es el 2 % de los clientes, conviene índice + *nested loops*; si es el 60 %, *full scan* + *hash join*.
- **Costo:** casi nulo. Es lo primero que se revisa.

### 2. Índices adecuados (la mejora más grande)

```sql
-- Filtro + columnas que se leen de customers: el filtro se resuelve solo con el índice
CREATE INDEX ix_customers_country_cov ON customers (country, customer_id, customer_name);

-- FK de orders + columnas que se proyectan: index-only scan, sin tocar la tabla de 10 M filas
CREATE INDEX ix_orders_customer_cov ON orders (customer_id, order_date, total_amount, order_id);
```

- **Qué hace:** el plan pasa a *index range scan* de los clientes de México → *nested loops* (o *hash join* si son
  muchos) contra el índice de `orders` por `customer_id`. Se leen solo los bloques de los pedidos de esos clientes.
- **Por qué mejora:** de leer 10 M filas a leer solo las de México (p. ej. 200 K), y de bloques de tabla a bloques
  de índice, que son más compactos.
- **Costo:** espacio en disco y más trabajo en cada `INSERT/UPDATE` de `orders`. Un índice de cobertura en la tabla
  más escrita debe justificarse; si no, basta `orders(customer_id)`.

### 3. Vista materializada (consulta frecuente, datos que toleran segundos/minutos de retraso)

```sql
CREATE MATERIALIZED VIEW LOG ON orders    WITH ROWID, SEQUENCE (customer_id, order_date, total_amount) INCLUDING NEW VALUES;
CREATE MATERIALIZED VIEW LOG ON customers WITH ROWID, SEQUENCE (customer_id, customer_name, country)  INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW mv_pedidos_por_pais
  BUILD IMMEDIATE
  REFRESH FAST ON COMMIT
  ENABLE QUERY REWRITE
AS
SELECT c.country, o.order_id, o.order_date, c.customer_name, o.total_amount,
       o.ROWID AS o_rid, c.ROWID AS c_rid
FROM   orders o JOIN customers c ON o.customer_id = c.customer_id;

CREATE INDEX ix_mv_pedidos_pais ON mv_pedidos_por_pais (country);
```

- **Qué hace:** precalcula el join; la consulta lee solo la porción de México. Con *query rewrite* la aplicación ni
  siquiera cambia su SQL.
- **Costo:** espacio y costo de refresco. `ON COMMIT` encarece cada transacción sobre `orders`; si se tolera
  retraso, mejor `REFRESH FAST ON DEMAND` programado (cada 5 minutos).

### 4. Particionamiento

```sql
-- orders por rango de fecha (intervalo mensual) + índice local
CREATE TABLE orders (
  order_id NUMBER PRIMARY KEY, customer_id NUMBER NOT NULL, order_date DATE NOT NULL, total_amount NUMBER(14,2)
)
PARTITION BY RANGE (order_date) INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
(PARTITION p_inicial VALUES LESS THAN (DATE '2020-01-01'));

-- customers por lista de país
CREATE TABLE customers (...) PARTITION BY LIST (country) (
  PARTITION p_mx VALUES ('MX'), PARTITION p_co VALUES ('CO'), PARTITION p_otros VALUES (DEFAULT));
```

- **Qué hace:** *partition pruning*: si la consulta filtra por periodo ("pedidos de México del último mes"), Oracle
  lee solo esas particiones. Con *reference partitioning* `orders` puede heredar la partición de su cliente.
- **Costo:** licencia (Partitioning es una opción de Enterprise Edition), diseño de claves y mantenimiento. Aporta
  más cuando la consulta incluye un rango de fechas, que es lo usual en la práctica.

### 5. Reescritura de la consulta y del modelo

- Normalizar el país a un **código ISO** (`country_code = 'MX'`) con *check constraint*: evita problemas de tildes y
  *collation* y permite índices más pequeños.
- **Limitar el resultado**: paginar (`FETCH FIRST 50 ROWS ONLY` con `ORDER BY o.order_date DESC`) y filtrar por
  rango de fechas. Rara vez se necesitan todos los pedidos históricos de un país.
- Evitar funciones sobre columnas indexadas (`UPPER(country) = 'MÉXICO'` anula el índice), o crear un índice
  basado en función si es inevitable.

```sql
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM   customers c
JOIN   orders o ON o.customer_id = c.customer_id
WHERE  c.country_code = 'MX'
AND    o.order_date >= ADD_MONTHS(TRUNC(SYSDATE), -3)
ORDER  BY o.order_date DESC
FETCH FIRST 50 ROWS ONLY;
```

### 6. Desnormalización y caché

- Copiar `country_code` en `orders` (mantenido por la aplicación o un *trigger*) e indexar
  `orders(country_code, order_date)`: elimina el join para este caso de uso. Costo: redundancia y consistencia.
- **Result Cache** de Oracle (`/*+ RESULT_CACHE */`) o caché en la aplicación (Redis) para resultados que se repiten
  con alta frecuencia y cambian poco.

## Resumen

| Estrategia | Impacto | Esfuerzo | Riesgo / costo |
|---|---|---|---|
| Estadísticas + histogramas | Medio | Muy bajo | Ninguno |
| Índices de cobertura / FK | **Alto** | Bajo | Escrituras más costosas, espacio |
| Vista materializada | Alto | Medio | Refresco, espacio |
| Particionamiento | Alto con filtro por fecha | Alto | Licencia EE, rediseño |
| Reescritura (ISO, paginación, rango) | Alto | Bajo-medio | Cambio en la aplicación |
| Desnormalización / caché | Alto en lecturas | Medio | Consistencia |

**Recomendación:** 1 + 2 + paginación/rango de fechas resuelven la mayoría de los casos. La vista materializada o
el particionamiento se justifican si, después de medir, la consulta sigue fuera del SLO.

## Scripts

- [`sql/oracle/01_diagnostico.sql`](../sql/oracle/01_diagnostico.sql) – planes y estadísticas
- [`sql/oracle/02_indices.sql`](../sql/oracle/02_indices.sql)
- [`sql/oracle/03_vista_materializada.sql`](../sql/oracle/03_vista_materializada.sql)
- [`sql/oracle/04_particionamiento.sql`](../sql/oracle/04_particionamiento.sql)
- [`sql/postgres/benchmark.sql`](../sql/postgres/benchmark.sql) – reproduce el escenario con datos sintéticos y compara
  el plan antes y después de los índices (`psql -f`).
