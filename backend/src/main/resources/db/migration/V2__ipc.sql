-- IPC anual de Colombia (variación año corrido a diciembre, fuente DANE).
-- La renovación aplica el IPC del año anterior al inicio de la nueva vigencia.
CREATE TABLE ipc (
    anio        INTEGER       PRIMARY KEY,
    porcentaje  NUMERIC(6, 3) NOT NULL CHECK (porcentaje > -100),
    fuente      VARCHAR(50)   NOT NULL DEFAULT 'DANE'
);

INSERT INTO ipc (anio, porcentaje) VALUES
    (2019, 3.800),
    (2020, 1.610),
    (2021, 5.620),
    (2022, 13.120),
    (2023, 9.280),
    (2024, 5.200),
    (2025, 5.100);
