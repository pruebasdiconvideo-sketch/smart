# Cambios del rediseno profesional (2026-11-18)

## Reconciliación con el informe: catálogo de modelos + instancias con serie única (nuevo)
El informe pide un modelo de dos niveles que la implementación no tenía:
(1) un **catálogo de modelos** independiente (código, modelo, marca) y (2) un
**ingreso** donde se selecciona el modelo del catálogo y se registra un
**número de serie único por cada unidad** + la OC, incrementando el stock.
El inventario se consolida por modelo/código/marca y cada unidad mantiene su
serie para trazabilidad individual.

Cambios aplicados:
- **Catálogo de modelos** (`Material`): se eliminó el campo `numeroModelo`
  (no está en el informe — la serie es por unidad, no del modelo). El catálogo
  queda con `codigo`, `modelo`, `marca`, `descripcion`, `categoria`, etc.
  (CRUD independiente en `almacen/catalogo`). Quitado de la entidad, los 3
  DTOs, `MaterialService`, `schema.sql`, la vista y las migraciones.
- **Nueva entidad `MaterialInstancia`** + `MaterialInstanciaRepository` + tabla
  `material_instancia`: una fila por unidad física, con `numero_serie` UNIQUE,
  FK a `material` (el modelo), FK a `orden_compra`, `fecha_ingreso`,
  `id_usuario` y `estado` (EN_ALMACEN / SALIDA / MERMADO). Da la trazabilidad
  individual de cada unidad.
- **Ingreso** (`MaterialService.registrarIngreso`): `IngresoItem` ahora acepta
  `numeroSeries` (una por unidad). Por cada serie valida unicidad (en el
  ingreso y contra la BD), crea una `MaterialInstancia` EN_ALMACEN, e
  incrementa el `stockActual` del modelo en la misma transacción. La vista
  `almacen/ingresos` reemplazó el "digitar código + cantidad" por un **select
  del modelo del catálogo** + un **textarea de series** (una por línea = una
  unidad). Sigue permitiendo cantidad sin series para ingresos legacy.
- **Trazabilidad individual**: nuevo endpoint `GET /api/almacen/instancias`
  (?serie= para buscar una unidad, ?material= para listar las de un modelo).
  La vista `trazabilidad/consulta` añadió una sección **"Buscar por número de
  serie"** que muestra modelo, marca, código, OC de origen, fecha de ingreso y
  estado de la unidad.
- **Despacho y merma**: al descontar stock, marcan las instancias más antiguas
  (FIFO) como SALIDA / MERMADO (best-effort, sin romper el decremento de stock
  si faltan instancias — p. ej. stock heredado sin series).
- **Migraciones**: `schema.sql` crea `material_instancia` y ya no crea
  `numero_modelo`. `MIGRAR_BASE_EXISTENTE.sql` y
  `V2__material_extra_columns.sql` ahora **eliminan** `numero_serie`/
  `numero_modelo` legacy (si existen) y crean `material_instancia`. Nuevo
  `db/FIX_INSTANCIAS.sql` para ejecutar una vez sobre la base MySQL existente.
  `SecurityConfig` añade la regla para `/api/almacen/instancias`.

Verificación (smoke test H2, login almacenero):
- Crear modelo (código/modelo/marca, sin numeroModelo) ✓
- Crear OC + registrar ingreso con 2 series → stock 2, "2 serie(s) registradas" ✓
- Stock consolidado por modelo = 2 ✓
- Lookup por serie → modelo/marca/código/OC/estado EN_ALMACEN ✓
- Serie duplicada rechazada (HTTP 400) ✓
- Segundo ingreso → stock 4, 4 instancias listadas ✓
- JS de catálogo, ingresos y trazabilidad válido (`node --check`) ✓

## Corrección de migración MySQL: numero_serie → numero_modelo conservando datos (nuevo)
El rename `numeroSerie → numeroModelo` provocaba en bases MySQL existentes el error
`Unknown column 'm1_0.numero_modelo' in 'field list'`, porque la migración **añadía**
una columna `numero_modelo` vacía en vez de **renombrar** la `numero_serie` existente
(con datos). Además el procedimiento `sp_migrar_base_existente` abortaba en
`parametro_prueba` (tabla inexistente) y el `schema.sql` fallaba al hacer
`DROP TABLE orden_compra` por la FK de `detalle_orden_compra`.

Correcciones aplicadas:
- **`MIGRAR_BASE_EXISTENTE.sql` y `V2__material_extra_columns.sql`**: el paso de
  `numero_modelo` ahora **renombra** (`ALTER TABLE material CHANGE COLUMN
  numero_serie numero_modelo VARCHAR(100)`) conservando los datos, en vez de
  añadir una columna vacía. Maneja los 3 escenarios: sólo `numero_serie` →
  renombra; ambas (serie con datos + modelo vacía de un intento previo) → copia
  los datos y elimina `numero_serie`; ninguna → crea `numero_modelo`.
- **`MIGRAR_BASE_EXISTENTE.sql`**: añadido `DECLARE CONTINUE HANDLER FOR
  SQLEXCEPTION BEGIN END;` al procedimiento, de modo que una tabla/columna
  inexistente (p. ej. `parametro_prueba`) ya no aborta toda la migración — el
  procedimiento continúa con los pasos siguientes.
- **`schema.sql`**: corregido el orden del "reseteo defensivo" — se añadió
  `DROP TABLE IF EXISTS detalle_orden_compra` (antes de `orden_compra`) y
  `DROP TABLE IF EXISTS empresa` (después de `proyecto`), resolviendo el error
  de FK 3730 al recrear el schema desde cero.
- **Nuevo `db/FIX_NUMERO_MODELO.sql`**: script independiente para ejecutar UNA
  VEZ sobre la base MySQL existente y corregir el error de inmediato (renombra
  `numero_serie → numero_modelo` conservando datos). Uso:
  `USE smart_power_on_db; source db/FIX_NUMERO_MODELO.sql;`

Verificación: build limpio; en H2 el schema con el nuevo orden de DROPs carga
sin errores; el catálogo devuelve HTTP 200 y crear un material con `modelo` +
`numeroModelo` funciona; `/almacen/series` y `/api/almacen/series` siguen en 404.

## Eliminación de "Series de materiales" y stock por modelo/número de modelo/categoría (nuevo)
- **Eliminada la opción "Series de materiales"** del menú y de toda la lógica:
  se borró la vista `almacen/series.html`, la ruta `/almacen/series` del
  `PageController`, la entrada del sidebar, el endpoint `GET /api/almacen/series`
  del `AlmacenApiController`, el método `listarSeries` del `MaterialService` y la
  regla de seguridad de `/api/almacen/series` en `SecurityConfig`. Verificado:
  la ruta y la API ahora devuelven **404**, y el sidebar ya no muestra la opción.
- **Renombrado `numeroSerie` → `numeroModelo`** en todo el proyecto para alinear
  la terminología con "número de modelo" (el stock se identifica por modelo +
  número de modelo + categoría) y eliminar la contradicción de tener un campo
  "serie" tras quitar la feature de series. Cambios: columna `numero_serie` →
  `numero_modelo` en `schema.sql`, `V2__material_extra_columns.sql` y
  `MIGRAR_BASE_EXISTENTE.sql`; campo y `@Column` en `Material.java`; los 3 DTOs
  (`MaterialDto`, `MaterialCreate`, `MaterialUpdate`); 4 referencias en
  `MaterialService`; y la etiqueta del catálogo "Número de serie" → "Número de
  modelo". `data.sql` no se vio afectado (no poblaba esa columna).
- **Stock ligado al material (no independiente)**: no existe una entidad `Stock`
  aparte. El stock es el campo `stockActual` de `Material`, por lo que **si no
  hay materiales registrados, no hay stock** — el catálogo, las alertas y los
  reportes se calculan siempre a partir de los materiales existentes. Cada
  material lleva `modelo`, `numeroModelo` y `categoria`, así que el stock queda
  efectivamente identificado por esos tres atributos. El `stockActual` no es
  editable a mano en el catálogo (se actualiza sólo desde ingresos/salidas); el
  catálogo sólo permite editar precio y stock mínimo.
- **Verificación**: build limpio; `/almacen/series` y `/api/almacen/series` →
  404; catálogo muestra "Número de modelo" sin residuales de "serie"; crear un
  material con `modelo` + `numeroModelo` funciona; el JS del catálogo pasa
  `node --check`.

## Fix de errores JS, selectores modales y limpieza de codigo repetido (nuevo)
- **Corregido SyntaxError `Unexpected token '<'`** en la vista de ingresos: un
  backtick duplicado en la llamada a `initModalSelector` cerraba prematuramente
  un template literal y hacía que el navegador interpretara el HTML como JS.
  Además, se reescribio el JS de `ingresos.html` y `proyectos.html` con
  concatenacion de strings (sin template literals anidados) para eliminar
  cualquier riesgo de backticks desbalanceados.
- **Selector modal con búsqueda para proveedor** (al registrar una OC): el
  formulario de orden de compra ahora usa la ventana modal de búsqueda
  (`modalSelectorMarkup` + `initModalSelector`) para elegir el proveedor
  registrado, en lugar de un dropdown plano. Muestra razón social + RUC + contacto
  y filtra al escribir. Incluye un enlace a `/empresa/proveedores` para registrar
  uno nuevo si no existe.
- **Selector modal con búsqueda para cliente** (al crear un proyecto): la vista
  de proyectos usa el mismo selector modal para elegir el cliente registrado.
  Muestra razón social + RUC + contacto. El `ProyectoCreate` ahora acepta
  `idEmpresa` y el `ProyectoService` trae razón social + RUC del cliente
  registrado. La tabla de proyectos muestra la columna RUC.
- **Favicon corregido**: la petición a `/favicon.ico` devolvía 500 porque el
  handler global capturaba `NoResourceFoundException` como error interno. Ahora
  hay un `@ExceptionHandler` que devuelve 404 para recursos estáticos faltantes,
  y se añadió un `favicon.ico` real (rayo azul de la marca) en `static/`.
- **Limpieza de codigo repetido**: se eliminó el cluster legacy `model/`
  (`SolicitudProduccion`, `DetalleSolicitud`) junto con `ProduccionController`,
  `ProduccionService` y `SolicitudProduccionRepository`. Era código muerto que
  duplicaba `entity/Solicitud` + `entity/DetalleSolicitud` y mapeaba la misma
  tabla `detalle_solicitud` (potencial conflicto JPA). Las vistas activas
  (`/produccion/solicitudes`, `/produccion/aprobaciones`) ya usan el
  `ProduccionApiController` + `SolicitudService` (entity/), así que no hay
  impacto funcional. Verificado: las solicitudes siguen funcionando.
- **Verificacion completa**: las 12 vistas con JS inline pasan `node --check`
  (sin errores de sintaxis). El sidebar no tiene entradas duplicadas. Smoke test
  end-to-end: OC con proveedor modal + proyecto con cliente modal + favicon 200.

## Registro de empresas, OC con multiples materiales y mejoras de trazabilidad/calidad (nuevo)
- **Nuevo modulo de Empresas** (proveedores y clientes): tabla `empresa` con
  RUC, razon social, razon comercial y contacto (nombre, telefono, correo).
  Una sola tabla con discriminador `tipo` (PROVEEDOR / CLIENTE). El usuario
  registra la empresa una sola vez y la reutiliza al crear ordenes de compra
  y proyectos, sin volver a digitar los datos.
- **Dos nuevas vistas**: `/empresa/proveedores` y `/empresa/clientes`, con CRUD
  completo (crear, editar, desactivar, reactivar) y filtros por texto y estado.
  Nueva seccion "Empresas" en el menu lateral.
- **Orden de compra con multiples materiales de varias categorias**: nueva tabla
  `detalle_orden_compra`. Una OC puede tener uno o varios items, cada uno
  vinculado a un material del catalogo (con su cantidad y precio). La vista de
  ingresos muestra los items de cada OC (expandibles) y permite agregar
  materiales al crear/editar la OC.
- **OC vinculada al proveedor registrado**: el formulario de OC ahora usa un
  dropdown de proveedores registrados (en lugar de texto libre). Al elegir un
  proveedor, el sistema trae su razon social y RUC automaticamente.
- **Pruebas de calidad con tecnico externo**: nueva columna `tecnico_externo` en
  `prueba_calidad`. El inspector registra el nombre del tecnico (personal
  externo sin cuenta) que realiza las mediciones. La vista de pruebas muestra
  una columna "Tecnico" y un campo para ingresarlo.
- **Trazabilidad corregida y mejorada**: se corrigio el bug que dejaba
  `fecha_movimiento` en null (ahora se registra la fecha/hora real). La vista
  de trazabilidad ahora muestra: nombre del material, tarjetas resumen
  (entradas, salidas, devoluciones, mermas), stock calculado por movimientos,
  y una tabla con badges de color por tipo de movimiento + responsable +
  proyecto destino + OC. Incluye un datalist para seleccionar el material.
- **Stock por categoria**: la etiqueta "Stock total" paso a "Stock disponible"
  para mayor claridad. El stock NO se ingresa manualmente: se calcula
  automaticamente sumando los movimientos (ingresos, despachos, devoluciones,
  mermas) por material.
- **Endpoint nuevo**: `GET /api/empresa` (con `?tipo=PROVEEDOR|CLIENTE` y
  `?incluirInactivos=true`), mas CRUD completo (`POST`, `PUT`, `PATCH
  /{id}/desactivar`, `PATCH /{id}/reactivar`).
- **Migracion**: `MIGRAR_BASE_EXISTENTE.sql` ahora crea la tabla `empresa`,
  `detalle_orden_compra`, anade `id_empresa` a `orden_compra` y `proyecto`, y
  `tecnico_externo` a `prueba_calidad` (todo idempotente).

## Codigo automatico segun categoria (nuevo)
- Al registrar un material, el **codigo se genera automaticamente** a partir de
  la categoria: `<PREFIJO>-<NNNN>` donde el prefijo son las primeras 4 letras
  de la categoria (sin acentos, mayusculas) y NNNN es el consecutivo.
  Ej.: PROTECCION -> `PROT-0001`, INTERRUPCION -> `INTE-0001`,
  CABLE -> `CABL-0001`, CLIMATIZACION -> `CLIM-0001`.
- El campo **Codigo del formulario es de solo lectura**: muestra el placeholder
  "Se genera segun la categoria" y, al escribir la categoria, aparece un preview
  en vivo del proximo codigo (ej. "Proóximo codigo: PROT-0001").
- El campo **Categoria** ahora ofrece sugerencias (datalist) con las categorias
  existentes, pero permite escribir una nueva.
- **La serie es un campo SEPARADO e independiente**: se digita manualmente y
  no tiene relacion con el codigo. Confirmado: un material puede tener
  codigo `PROT-0001` y serie `SN-TEST-001`.
- Nuevo endpoint: `GET /api/almacen/materiales/siguiente-codigo?categoria=X`
  devuelve el proximo codigo que se asignaria para esa categoria.
- Al editar un material existente, el codigo se mantiene (no se regenera).

## Series de materiales y stock por categoria (nuevo)
- **Nueva vista "Series de materiales"** (`/almacen/series`): lista todos los
  productos con su **numero de serie**, marca, modelo, MLFB y ubicacion
  (pasillo/estante/nivel). Cada material muestra su propia serie resaltada.
- **Nueva vista "Stock por categoria"** (`/almacen/categorias`): resume el
  inventario agrupado por categoria con tarjetas de estadisticas (categorias,
  stock total, valor inventario, materiales en alerta) y una tabla con cobertura
  visual (barra de progreso verde/amarilla/roja).
- **Filtros nuevos** en ambas vistas:
  - Series: busqueda libre (codigo, nombre, serie, marca, modelo, MLFB) +
    dropdown de categoria + dropdown de estado (Activo/Inactivo).
  - Categorias: filtro por nombre de categoria.
  - Filtros reactivos (aplican al escribir/cambiar, sin boton).
- **3 endpoints API nuevos**:
  - `GET /api/almacen/series?texto=&categoria=&estado=`
  - `GET /api/almacen/categorias` (stock agrupado + valor inventario + alertas)
  - `GET /api/almacen/categorias-lista` (lista de categorias para dropdowns)
- **Menu lateral**: dos nuevas entradas bajo Inventario:
  "Series de materiales" e "Stock por categoria".
- Accesible para ADMIN, ALMACENERO, JEFE_PRODUCCION y OPERARIO_ENSAMBLE.

## Limpieza de codigos de requerimiento (UI)
- Se eliminaron **todos** los codigos internos de la interfaz visible:
  `RFxx`, `P1`-`P5`, `M6`, `Tx`, `RN0x` y la frase "Modulo N".
- La seccion "Modulos del sistema" del home se renombra a
  **"Informacion del sistema"**: 6 tarjetas con icono, titulo y descripcion,
  sin codigos de requerimiento ni de modulo.
- Los subtitulos de las 14 paginas internas (catalogo, ingresos, solicitudes,
  despachos, pruebas, parametros, usuarios, mermas, trazabilidad, etc.)
  ahora muestran una descripcion generica de uso en lugar de los codigos.
- Botones y tooltips limpios: "Generar certificado PDF" (sin `(RF28)`),
  "Verificar stock disponible" (sin `RF17/RF18`), texto de confirmacion
  de despacho (sin `P3-RN03`).
- Proyecto final presentable: ningun codigo academico queda visible para
  el usuario final.

## Configuracion
- `application.properties`: contrasena MySQL por defecto = `123456789`
  (sobreescribible con la variable de entorno `SPRING_DATASOURCE_PASSWORD`).

## Rediseno visual
- **`styles.css`** (reescrito): paleta corporativa azul + ambar, tipografia Inter,
  Bootstrap Icons, cards con sombras suaves, stat cards para dashboard,
  module cards con borde de acento, sidebar con secciones agrupadas,
  topbar con gradient y avatar de iniciales, login con fondo gradient y
  halos decorativos, scrollbar moderna, responsive movil/tablet/desktop.
- **`layout.html`** (redisenado): topbar con brand mark + avatar + badge de rol,
  sidebar con secciones (Principal, Inventario, Produccion, Despacho,
  Calidad y mermas, Trazabilidad, Administracion), iconos Bootstrap Icons
  en cada item, estado activo automatico basado en la URL actual,
  boton de menu hamburguesa en movil.
- **`login.html`** (redisenado): tarjeta centrada con gradient de fondo,
  icono de marca, campos con iconos, boton mostrar/ocultar contrasena,
  panel plegable de usuarios de prueba (clic para autocompletar),
  spinner durante el login, notificaciones toast.
- **`home.html`** (redisenado): stat cards dinamicas (materiales, proyectos,
  solicitudes, alertas), tarjeta de alertas de stock con deficit,
  6 module cards con codigo de modulo y descripcion, fecha actual.

## Modal selector mejorado (`app.js`)
- Resaltado de coincidencias con `<mark>` ambar.
- Contador de resultados ("N de M resultados").
- Navegacion por teclado: flechas arriba/abajo navegar, Enter seleccionar, Esc cerrar.
- Icono de busqueda en el campo.
- Hint visible para el usuario.
- Busqueda case-insensitive.
- Funcion `spToast()` global para notificaciones no intrusivas.

## Nuevo archivo Java
- `controller/GlobalModelAttributes.java`: `@ControllerAdvice` que anade
  `currentPath` y `currentMenu` al modelo de todas las vistas, para que
  el sidebar marque automaticamente el item activo.

## Base de datos: migracion para base existente
- **Nuevo**: `db/MIGRAR_BASE_EXISTENTE.sql` — script consolidado idempotente
  que anade las columnas de borrado logico (`anulada`, `motivo_anulacion`)
  a `solicitud`, `despacho`, `prueba_calidad` y `merma_devolucion`, mas
  `activo` en `parametro_prueba`. Pensado para correr contra tu base
  `basecompleta.sql` ya cargada en MySQL, sin perder datos ni stored procedures.
  Un solo comando:
  ```
  mysql -u root -p smart_power_on_db < src/main/resources/db/MIGRAR_BASE_EXISTENTE.sql
  ```
- Los stored procedures de tu dump (`sp_ConfirmarEntregaYDescontar`,
  `sp_RegistrarIngresoMaterial`, `sp_ValidarMermaDevolucion`) coexisten
  sin conflicto: la app usa JPA `@Transactional`, no los invoca.
- `MANUAL_DESPLEGUE.md` seccion 2.2 ahora documenta la Opcion B
  (base existente con `basecompleta.sql`) y la 2.3 la contrasena por
  defecto `123456789`.

## Como aplicar en tu maquina (D:\BDCLIENTES\SOFTWARE\smart)
1. Deten la app si esta corriendo (Ctrl+C en la consola).
2. Borra la carpeta `src` vieja: `rmdir /s /q src`
3. Borra `target`: `rmdir /s /q target`
4. Descomprime este zip dentro de `D:\BDCLIENTES\SOFTWARE\smart`.
5. **Migra tu base MySQL** (un solo comando, idempotente):
   ```
   mysql -u root -p smart_power_on_db < src\main\resources\db\MIGRAR_BASE_EXISTENTE.sql
   ```
   Esto anade las columnas que la app necesita sin tocar tus datos ni
   tus stored procedures.
6. La contrasena MySQL ya viene configurada como `123456789`.
7. Compila: `mvn clean package`
8. Ejecuta: `java -jar target\gestion-materiales-1.0.0.jar`
9. Abre: http://localhost:8080/login
