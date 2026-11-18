-- =====================================================================
-- Smart Power On S.A.C. - Datos iniciales (Anexo B ítem 15)
-- Permite ejecutar los 46 casos de prueba del §7.7
-- =====================================================================

-- ROLES (5) - RF02
INSERT INTO rol (id_rol, nombre, descripcion) VALUES
 (1,'ADMIN','Administrador del sistema'),
 (2,'JEFE_PRODUCCION','Jefe de Producción'),
 (3,'ALMACENERO','Almacenero'),
 (4,'OPERARIO_ENSAMBLE','Operario de Ensamble'),
 (5,'INSPECTOR','Inspector de Calidad');

-- PERMISOS (22)
INSERT INTO permiso (codigo, descripcion) VALUES
 ('USUARIO_GESTIONAR','Crear, editar y desactivar usuarios (RF03)'),
 ('PARAMETRO_CONFIGURAR','Configurar rangos técnicos (RF30)'),
 ('MATERIAL_REGISTRAR','Registrar material en catálogo (RF05)'),
 ('MATERIAL_CONSULTAR','Consultar catálogo (RF06)'),
 ('MATERIAL_ACTUALIZAR','Actualizar material (RF07)'),
 ('INGRESO_REGISTRAR','Registrar ingreso contra OC (RF08-RF10)'),
 ('ALERTA_VER','Ver alertas de stock mínimo (RF11)'),
 ('PROYECTO_GESTIONAR','Registrar proyectos (RF12)'),
 ('SOLICITUD_GESTIONAR','Registrar y validar solicitudes con BOM (RF13-RF15)'),
 ('SOLICITUD_APROBAR','Aprobar o rechazar solicitudes retenidas (RF16)'),
 ('SOLICITUD_VERIFICAR','Verificar stock y generar faltantes (RF17-RF18)'),
 ('DESPACHO_VER','Ver bandeja de despachos (RF19)'),
 ('DESPACHO_REGISTRAR','Registrar y validar despacho (RF20-RF22)'),
 ('DESPACHO_CONFIRMAR','Confirmar recepción y descontar stock (RF23-RF24)'),
 ('PRUEBA_REGISTRAR','Registrar mediciones de calidad (RF25-RF27)'),
 ('PRUEBA_CONSULTAR','Consultar historial de pruebas (RF29)'),
 ('CERTIFICADO_EMITIR','Emitir certificado de conformidad PDF (RF28)'),
 ('DEVOLUCION_REGISTRAR','Registrar devolución de sobrantes (RF31)'),
 ('DEVOLUCION_REINTEGRAR','Reintegrar o marcar merma (RF32-RF33)'),
 ('MERMA_VALIDAR','Validar baja por merma (RF34)'),
 ('MERMA_REPORTE','Ver reporte de mermas (RF35)'),
 ('TRAZABILIDAD_CONSULTAR','Consultar trazabilidad de un material (RF36)');

-- Matriz rol - permiso (RF02)
INSERT INTO rol_permiso (id_rol, id_permiso) VALUES
 -- ADMIN: gestión + consultas + certificado
 (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,11),
 (1,12),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,22),
 -- JEFE_PRODUCCION
 (2,4),(2,8),(2,9),(2,11),(2,12),(2,15),(2,16),(2,18),(2,20),(2,21),(2,22),
 -- ALMACENERO
 (3,3),(3,4),(3,5),(3,6),(3,7),(3,12),(3,13),(3,18),(3,19),(3,21),
 -- OPERARIO_ENSAMBLE
 (4,14),(4,18),
 -- INSPECTOR
 (5,4),(5,15),(5,16),(5,17);

-- USUARIOS (uno por rol). Contraseñas en texto plano (sólo referencia):
--   admin@smart.pe     / Admin123
--   jefe@smart.pe       / Jefe123
--   almacen@smart.pe    / Alma123
--   operario@smart.pe   / Oper123
--   inspector@smart.pe  / Inspec123
INSERT INTO usuario (id_usuario, id_rol, nombre_completo, correo, password_hash, activo) VALUES
 (1,1,'Walter Izquierdo','admin@smart.pe',
    '$2b$10$S6rH/aDsBhQFoL1zC8YiRe18LsOZ.P9gaCsJMTcUmNsFB1tYaR3w2', TRUE),
 (2,2,'Briyit Valencia','jefe@smart.pe',
    '$2b$10$zb0YUrtO85WaunBHXuIDqeya5fMj8Mo3pj.AtEoj6qh9GS0mydP1K', TRUE),
 (3,3,'Sebastián Orellana','almacen@smart.pe',
    '$2b$10$u4MQq9jZx0KGV1gGZ2TjhOly2SRq9IUDhPDm1m2NuA6AIvqPDrHiK', TRUE),
 (4,4,'Gian Quispe','operario@smart.pe',
    '$2b$10$34TLFxfxahMA5N2slMVCUOaCVw1mWU9Kzjc9XSedZLgVEQzC7OoPW', TRUE),
 (5,5,'Yanpier Rodas','inspector@smart.pe',
    '$2b$10$MRdAmGSeJIBcFPxRrWZ3aOYKmyIE7mkaKwMW.XyLe7R5KF6EiMoUG', TRUE);

-- MATERIALES (SIPROTEC-7SJ62 con stock 3 <= mínimo 5 → dispara RF11 / PF-15)
INSERT INTO material (id_material, codigo, nombre, especificacion_tecnica, categoria, precio_unitario, stock_actual, stock_minimo) VALUES
 (1,'SIPROTEC-7SJ62','Relé SIPROTEC 5 7SJ62','Relé de protección numérico, 4UR, módulo de comunicación','PROTECCION',1200.00,3,5),
 (2,'SIPROTEC-7S80','Relé SIPROTEC 7S80','Relé de sobrevelocidad','PROTECCION',980.00,10,3),
 (3,'BREAKER-3VA11','Breaker 3VA11 32A','Interruptor automático 3 polos 32A','INTERRUPCION',180.00,15,5),
 (4,'BREAKER-3VA21','Breaker 3VA21 63A','Interruptor automático 3 polos 63A','INTERRUPCION',210.00,8,4),
 (5,'BORNERA-WAGO','Bornera WAGO 2.5mm','Bornera de tornillo 2.5 mm² gris','CONEXION',6.50,200,50),
 (6,'RIEL-DIN-300','Riel DIN 300mm','Riel DIN simétrico 35 mm x 300 mm','SOPORTE',12.00,40,10),
 (7,'CABLE-2.5-ROJO','Cable 2.5mm² rojo','Conductor flexible 2.5 mm² color rojo, x metro','CABLE',3.20,500,100),
 (8,'CANALETA-40x25','Canaleta 40x25','Canaleta perforada 40x25 mm x metro','CABLE',4.80,80,20),
 (9,'TERMO-25','Termóstato 25°C','Termostato de carcasa NO 25°C','CLIMATIZACION',45.00,6,3),
 (10,'RESIST-100W','Resistencia 100W','Resistencia anticondensación 100W','CLIMATIZACION',38.00,4,3);

-- ÓRDENES DE COMPRA (dos ABIERTAS + una CERRADA para PF-13)
INSERT INTO orden_compra (id_orden_compra, codigo, proveedor, fecha_emision, estado) VALUES
 (1,'OC-2026-001','SIEMENS Perú S.A.',    '2026-10-15','ABIERTA'),
 (2,'OC-2026-002','Eléctrica del Perú',   '2026-11-02','ABIERTA'),
 (3,'OC-2026-003','Distribuidora Norte',  '2026-09-10','CERRADA');

-- PROYECTOS
INSERT INTO proyecto (id_proyecto, codigo, cliente, fecha_inicio, presupuesto, estado) VALUES
 (1,'TAB-2026-001','Minera Yanacocha',   '2026-10-01', 18000.00,'EN_PROCESO'),
 (2,'TAB-2026-002','Luz del Sur S.A.A.', '2026-11-05',  9500.00,'EN_PROCESO'),
 (3,'TAB-2026-003','Sedapal',            '2026-11-12', 12500.00,'EN_PROCESO');

-- PARÁMETROS DE PRUEBA (rangos técnicos, P4-RN03 / RF30)
INSERT INTO parametro_prueba (id_parametro, tipo_prueba, unidad, valor_minimo, valor_maximo, norma_referencia) VALUES
 (1,'TENSION','V',     380.000,  420.000, 'IEC 60947-1'),
 (2,'AISLAMIENTO','MOhm', 100.000, 9999.000, 'IEC 60947-1'),
 (3,'RESISTENCIA_TIERRA','Ohm', 0.500,    5.000,  'NTP 370.311');

-- Las solicitudes, despachos, entregas, pruebas, mermas y trazabilidad se
-- generan dinámicamente desde la aplicación; cada acción del usuario deja su
-- movimiento de trazabilidad correspondiente (R-INV-02).
