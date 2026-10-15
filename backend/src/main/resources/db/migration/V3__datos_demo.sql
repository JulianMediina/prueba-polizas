-- Datos de demostración (sintéticos). Coherentes con las reglas:
-- canon de la póliza = suma de cánones de riesgos activos; prima = canon x meses.

INSERT INTO poliza (numero, tipo, estado, tomador_tipo_documento, tomador_documento, tomador_nombre, tomador_email, tomador_telefono,
                    fecha_inicio, fecha_fin, meses_vigencia, canon_mensual, prima, fecha_cancelacion) VALUES
    ('IND-0000001', 'INDIVIDUAL', 'ACTIVA',    'CC',  '1020304050', 'Laura Gómez Restrepo',                    'laura.gomez@example.com',   '3001234567', DATE '2025-11-01', DATE '2026-11-01', 12, 2500000.00, 30000000.00, NULL),
    ('IND-0000002', 'INDIVIDUAL', 'ACTIVA',    'CC',  '1037654321', 'Andrés Pérez Londoño',                    'andres.perez@example.com',  '3109876543', DATE '2026-03-01', DATE '2027-03-01', 12, 1800000.00, 21600000.00, NULL),
    ('IND-0000003', 'INDIVIDUAL', 'CANCELADA', 'CC',  '1144556677', 'Sofía Martínez Caicedo',                  'sofia.martinez@example.com','3157654321', DATE '2025-06-01', DATE '2026-06-01', 12, 1200000.00, 14400000.00, DATE '2026-02-15'),
    ('IND-0000004', 'INDIVIDUAL', 'RENOVADA',  'CC',  '72345678',   'Diego Torres Barrios',                    'diego.torres@example.com',  '3001112233', DATE '2026-02-01', DATE '2027-02-01', 12, 3153000.00, 37836000.00, NULL),
    ('COL-0000005', 'COLECTIVA',  'ACTIVA',    'NIT', '900123456',  'Inmobiliaria Andina S.A.S.',              'polizas@andina.example.com','6017654321', DATE '2025-12-01', DATE '2026-12-01', 12, 6250000.00, 75000000.00, NULL),
    ('COL-0000006', 'COLECTIVA',  'ACTIVA',    'NIT', '900765432',  'Conjunto Torres del Parque P.H.',         'admin@torresparque.example.com','6045551234', DATE '2026-04-01', DATE '2026-10-01', 6, 2750000.00, 16500000.00, NULL),
    ('COL-0000007', 'COLECTIVA',  'CANCELADA', 'NIT', '901222333',  'Inmobiliaria del Caribe S.A.S.',          'contacto@caribe.example.com','6056667788', DATE '2025-08-01', DATE '2026-08-01', 12, 2000000.00, 24000000.00, DATE '2026-05-20'),
    ('IND-0000008', 'INDIVIDUAL', 'ACTIVA',    'CE',  '5123987',    'Valentina Rojas Díaz',                    'valentina.rojas@example.com','3204445566', DATE '2026-09-01', DATE '2027-09-01', 12, 1650000.00, 19800000.00, NULL);

INSERT INTO riesgo (poliza_id, direccion, ciudad, arrendatario_documento, arrendatario_nombre, arrendador_documento, arrendador_nombre,
                    canon_mensual, estado, fecha_inclusion, fecha_cancelacion)
SELECT p.id, r.direccion, r.ciudad, r.arr_doc, r.arr_nombre, r.ador_doc, r.ador_nombre, r.canon, r.estado, r.inclusion, r.cancelacion
FROM (VALUES
    ('IND-0000001', 'Carrera 15 # 93-47 Apto 502',          'Bogotá',       '1020304050', 'Laura Gómez Restrepo',        '79865432',  'Carlos Ruiz Ángel',          2500000.00, 'ACTIVO',    DATE '2025-11-01', NULL::DATE),
    ('IND-0000002', 'Calle 10 # 43-20 Apto 1201',           'Medellín',     '1037654321', 'Andrés Pérez Londoño',        '43123456',  'Marta Lucía Henao',          1800000.00, 'ACTIVO',    DATE '2026-03-01', NULL),
    ('IND-0000003', 'Avenida 6N # 25-30 Casa 4',            'Cali',         '1144556677', 'Sofía Martínez Caicedo',      '16789012',  'Jorge Iván Rengifo',         1200000.00, 'CANCELADO', DATE '2025-06-01', DATE '2026-02-15'),
    ('IND-0000004', 'Carrera 53 # 82-120 Apto 8B',          'Barranquilla', '72345678',   'Diego Torres Barrios',        '8723456',   'Rosa Elena Manotas',         3153000.00, 'ACTIVO',    DATE '2025-02-01', NULL),
    ('COL-0000005', 'Calle 127 # 45-10 Apto 301',           'Bogotá',       '1015432109', 'Camila Herrera Suárez',       '19456789',  'Hernando Castillo Vega',     2000000.00, 'ACTIVO',    DATE '2025-12-01', NULL),
    ('COL-0000005', 'Carrera 7 # 72-15 Apto 704',           'Bogotá',       '80123456',   'Juan Pablo Ortiz',            '41234567',  'Gloria Patricia Mejía',      1500000.00, 'ACTIVO',    DATE '2025-12-01', NULL),
    ('COL-0000005', 'Calle 85 # 11-53 Oficina 402',         'Bogotá',       '900555666',  'Tecnologías Delta S.A.S.',    '79456123',  'Ricardo Salazar Pinto',      2750000.00, 'ACTIVO',    DATE '2026-02-10', NULL),
    ('COL-0000005', 'Diagonal 40A # 13-20 Apto 101',        'Bogotá',       '1032987654', 'Natalia Vargas Cruz',         '19876543',  'Beatriz Amaya Rozo',         1000000.00, 'CANCELADO', DATE '2025-12-01', DATE '2026-06-30'),
    ('COL-0000006', 'Calle 5 Sur # 25-40 Torre 1 Apto 902', 'Medellín',     '1036111222', 'Mateo Zapata Ríos',           '70111222',  'Luis Fernando Arango',       1300000.00, 'ACTIVO',    DATE '2026-04-01', NULL),
    ('COL-0000006', 'Calle 5 Sur # 25-40 Torre 2 Apto 305', 'Medellín',     '1036333444', 'Isabella Gaviria Mesa',       '42333444',  'Claudia Patricia Uribe',     1450000.00, 'ACTIVO',    DATE '2026-04-01', NULL),
    ('COL-0000007', 'Barrio Manga Calle 26 # 20-55',        'Cartagena',    '1047111333', 'Daniel Pombo Ruiz',           '73111333',  'Eduardo Vélez Martelo',       900000.00, 'CANCELADO', DATE '2025-08-01', DATE '2026-05-20'),
    ('COL-0000007', 'Bocagrande Carrera 3 # 8-129 Apto 1503','Cartagena',   '1047222444', 'Mariana Lequerica Díaz',      '45222444',  'Fabio Ernesto Benedetti',    1100000.00, 'CANCELADO', DATE '2025-08-01', DATE '2026-05-20'),
    ('IND-0000008', 'Carrera 27 # 36-14 Apto 603',          'Bucaramanga',  '5123987',    'Valentina Rojas Díaz',        '91234567',  'Pedro Antonio Serrano',      1650000.00, 'ACTIVO',    DATE '2026-09-01', NULL)
) AS r (numero, direccion, ciudad, arr_doc, arr_nombre, ador_doc, ador_nombre, canon, estado, inclusion, cancelacion)
JOIN poliza p ON p.numero = r.numero;

INSERT INTO renovacion (poliza_id, anio_ipc, ipc_aplicado, canon_anterior, canon_nuevo, prima_anterior, prima_nueva,
                        fecha_inicio_anterior, fecha_fin_anterior, fecha_inicio_nueva, fecha_fin_nueva, created_at)
SELECT id, 2025, 5.100, 3000000.00, 3153000.00, 36000000.00, 37836000.00,
       DATE '2025-02-01', DATE '2026-02-01', DATE '2026-02-01', DATE '2027-02-01', TIMESTAMPTZ '2026-01-20 10:00:00-05'
FROM poliza WHERE numero = 'IND-0000004';
