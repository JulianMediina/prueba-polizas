-- Esquema principal de pólizas de arrendamiento.
-- Regla de diseño: el canon de la póliza es la suma de los cánones de sus riesgos activos
-- y la prima es canon x meses de vigencia. Ambos se recalculan en el dominio.

CREATE SEQUENCE poliza_numero_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE poliza (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero                   VARCHAR(20)    NOT NULL UNIQUE,
    tipo                     VARCHAR(12)    NOT NULL CHECK (tipo IN ('INDIVIDUAL', 'COLECTIVA')),
    estado                   VARCHAR(12)    NOT NULL CHECK (estado IN ('ACTIVA', 'RENOVADA', 'CANCELADA')),
    tomador_tipo_documento   VARCHAR(5)     NOT NULL,
    tomador_documento        VARCHAR(20)    NOT NULL,
    tomador_nombre           VARCHAR(150)   NOT NULL,
    tomador_email            VARCHAR(150),
    tomador_telefono         VARCHAR(20),
    fecha_inicio             DATE           NOT NULL,
    fecha_fin                DATE           NOT NULL,
    meses_vigencia           INTEGER        NOT NULL CHECK (meses_vigencia BETWEEN 1 AND 60),
    canon_mensual            NUMERIC(15, 2) NOT NULL CHECK (canon_mensual >= 0),
    prima                    NUMERIC(17, 2) NOT NULL CHECK (prima >= 0),
    fecha_cancelacion        DATE,
    version                  BIGINT         NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_poliza_vigencia CHECK (fecha_fin > fecha_inicio)
);

CREATE INDEX ix_poliza_tipo_estado ON poliza (tipo, estado);
CREATE INDEX ix_poliza_fecha_fin ON poliza (fecha_fin) WHERE estado <> 'CANCELADA';

CREATE TABLE riesgo (
    id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    poliza_id                  BIGINT         NOT NULL REFERENCES poliza (id),
    direccion                  VARCHAR(200)   NOT NULL,
    ciudad                     VARCHAR(80)    NOT NULL,
    arrendatario_documento     VARCHAR(20)    NOT NULL,
    arrendatario_nombre        VARCHAR(150)   NOT NULL,
    arrendador_documento       VARCHAR(20)    NOT NULL,
    arrendador_nombre          VARCHAR(150)   NOT NULL,
    canon_mensual              NUMERIC(15, 2) NOT NULL CHECK (canon_mensual > 0),
    estado                     VARCHAR(10)    NOT NULL CHECK (estado IN ('ACTIVO', 'CANCELADO')),
    fecha_inclusion            DATE           NOT NULL,
    fecha_cancelacion          DATE,
    version                    BIGINT         NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX ix_riesgo_poliza ON riesgo (poliza_id, estado);

CREATE TABLE renovacion (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    poliza_id                BIGINT         NOT NULL REFERENCES poliza (id),
    anio_ipc                 INTEGER        NOT NULL,
    ipc_aplicado             NUMERIC(6, 3)  NOT NULL,
    canon_anterior           NUMERIC(15, 2) NOT NULL,
    canon_nuevo              NUMERIC(15, 2) NOT NULL,
    prima_anterior           NUMERIC(17, 2) NOT NULL,
    prima_nueva              NUMERIC(17, 2) NOT NULL,
    fecha_inicio_anterior    DATE           NOT NULL,
    fecha_fin_anterior       DATE           NOT NULL,
    fecha_inicio_nueva       DATE           NOT NULL,
    fecha_fin_nueva          DATE           NOT NULL,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX ix_renovacion_poliza ON renovacion (poliza_id);
