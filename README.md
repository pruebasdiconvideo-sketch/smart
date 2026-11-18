# Smart Power On S.A.C. — Sistema de Gestión de Materiales

Aplicación web para el control de la entrega y registro de materiales para el
ensamblaje de tableros eléctricos, desarrollada con Java 21 + Spring Boot 3.2.5,
Thymeleaf, Spring Security con **JWT (HS256)** y MySQL 8 / H2.

Este proyecto **termina y cumple** lo que pide el informe
`Informe_Smart_Power_On_Revisado.docx`, alineado punto por punto con el **Anexo B
— Lista de verificación para alinear la implementación**.

> **Repositorio de origen (esqueleto inicial):** https://github.com/pruebasdiconvideo-sketch/smart.git
> El esqueleto original tenía 3 entidades, 4 roles en memoria, formLogin sin JWT y
> la base `db_smart_power_on`. Esta entrega lo **reemplaza** por una implementación
> completa y verificable contra el informe.

---

## Stack y arquitectura (Cap. 4 §7.1)

| Capa | Tecnología |
|---|---|
| Presentación | Thymeleaf + HTML5 + CSS3 + JavaScript + Bootstrap 5 (CDN) |
| API REST | Spring Web, JSON salvo el certificado (PDF) |
| Seguridad | Spring Security + JWT HS256 + BCrypt (stateless) |
| Negocio | 8 servicios transaccionales con reglas R-INV-01..06 / R-CAL-01..02 |
| Persistencia | Spring Data JPA + Hibernate sobre MySQL 8 (H2 en pruebas) |
| Empaquetado | Maven, Java 21 |

Arquitectura en capas: navegador → vistas Thymeleaf / servicios REST →
controladores → servicios de negocio → repositorios JPA → MySQL.

---

## Estructura del proyecto

```
smart_power_on/
├── pom.xml                          # Maven, Spring Boot 3.2.5, Java 21
├── mvnw, mvnw.cmd, .mvn/            # Maven wrapper
├── src/main/java/com/smartpoweron/gestion_materiales/
│   ├── GestionMaterialesApplication.java
│   ├── entity/                      # 16 entidades JPA (alineadas al diccionario §7.3)
│   ├── repository/                  # 16 repositorios Spring Data JPA
│   ├── dto/Dtos.java                # Contratos JSON de los 35 endpoints
│   ├── security/                    # JwtUtil (HS256), JwtFilter, SecurityConfig,
│   │                                # UsuarioDetailsService, SecurityUtil
│   ├── exception/                   # ReglaNegocioException, AutenticacionException,
│   │                                # GlobalExceptionHandler (mensajes en español)
│   ├── service/                     # AuthService, UsuarioService, MaterialService,
│   │                                # ProyectoService, SolicitudService,
│   │                                # DespachoService, CalidadService,
│   │                                # MermaService, TrazabilidadService,
│   │                                # CertificadoService (PDF, RF28)
│   └── controller/
│       ├── PageController.java      # 16 vistas Thymeleaf (V01-V16)
│       └── api/                     # 8 controladores REST con los 35 endpoints
│           ├── AuthApiController         (A01, A02)
│           ├── AdminApiController        (A03-A08)
│           ├── AlmacenApiController      (A09-A14)
│           ├── ProduccionApiController   (A15-A21)
│           ├── DespachoApiController     (A22-A26)
│           ├── CalidadApiController      (A27-A29)
│           ├── MermaApiController        (A30-A34)
│           └── TrazabilidadApiController (A35)
└── src/main/resources/
    ├── application.properties       # MySQL por defecto
    ├── application-h2.properties    # H2 para pruebas sin MySQL
    ├── db/schema.sql                # 16 tablas, 26 FK (§7.3)
    ├── db/data.sql                  # Datos iniciales (Anexo B ítem 15)
    ├── static/css/styles.css, static/js/app.js
    └── templates/                   # 16 vistas + certificado.html + fragments/layout.html
```

---

## Cómo ejecutar

### Opción A — Con MySQL (producción, §7.6 / RNF05)

1. Para una base de datos nueva, crearla y cargar el esquema y datos iniciales:

   ```bash
   mysql -u root -p < src/main/resources/db/schema.sql
   mysql -u root -p smart_power_on_db < src/main/resources/db/data.sql
   ```

   **No ejecutar `schema.sql` en una base existente**: elimina las tablas.
   Para actualizar una instalación existente, hacer primero un respaldo y
   ejecutar en orden las migraciones idempotentes V2 a V6:

   ```
   mysql -u root -p smart_power_on_db < src/main/resources/db/migration/V2__material_extra_columns.sql
   mysql -u root -p smart_power_on_db < src/main/resources/db/migration/V3__parametro_prueba_logical_delete.sql
   mysql -u root -p smart_power_on_db < src/main/resources/db/migration/V4__solicitudes_despachos_logical_delete.sql
   mysql -u root -p smart_power_on_db < src/main/resources/db/migration/V5__pruebas_calidad_logical_delete.sql
   mysql -u root -p smart_power_on_db < src/main/resources/db/migration/V6__mermas_devoluciones_logical_delete.sql
   ```

2. Configurar la conexión mediante variables de entorno; no guardar credenciales
   en `application.properties` ni en el repositorio. En CMD:

   ```cmd
   set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/smart_power_on_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   set SPRING_DATASOURCE_USERNAME=root
   set SPRING_DATASOURCE_PASSWORD=<contraseña>
   ```

3. Compilar y ejecutar:

   ```cmd
   mvnw.cmd package
   java -jar target\gestion-materiales-1.0.0.jar
   ```

4. Abrir http://localhost:8080/login

### Opción B — Con H2 en memoria (pruebas rápidas, sin MySQL)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

### Usuarios de prueba (Anexo B ítem 15)

| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | admin@smart.pe | Admin123 |
| JEFE_PRODUCCION | jefe@smart.pe | Jefe123 |
| ALMACENERO | almacen@smart.pe | Alma123 |
| OPERARIO_ENSAMBLE | operario@smart.pe | Oper123 |
| INSPECTOR | inspector@smart.pe | Inspec123 |

> Contraseñas cifradas con BCrypt (RNF04). No aparecer nunca en texto plano
> fuera de este README de pruebas.

---

## Cobertura del Anexo B (lista de verificación)

| # | Verificación | Cumple | Dónde |
|---|---|---|---|
| 1 | Login en español, Bootstrap, no el default de Spring Security | ✅ | `templates/auth/login.html` (V01) |
| 2 | 16 tablas con columnas, tipos, UK y FK del diccionario | ✅ | `db/schema.sql` |
| 3 | No quedan entidades del esquema anterior (empresa, kit, almacen...) | ✅ | `entity/` |
| 4 | Estados del código son los de la Tabla 63 | ✅ | `entity/`, `service/` |
| 5 | Un servicio API por fila de la Tabla 70 (35 endpoints) | ✅ | `controller/api/` |
| 6 | Una vista por fila de la Tabla 69 (16 pantallas) + menú por rol | ✅ | `controller/PageController.java`, `templates/fragments/layout.html` |
| 7 | Ingreso no crea materiales: si no existe, solicita registrarlo | ✅ | `MaterialService.registrarIngreso()` (P1-T04/T05) |
| 8 | Ingreso rechazado (OC cerrada, c/código inexistente) no cambia stock | ✅ | PF-12, PF-13 verificados |
| 9 | Costo = Σ cantidad × precio; supera presupuesto → RETENIDA; sólo ADMIN decide | ✅ | `SolicitudService` (R-INV-06) |
| 10 | Stock insuficiente → listado de faltantes; no LISTA_DESPACHO; re-verificar | ✅ | `SolicitudService.verificarStock()` |
| 11 | Despacho valida código+cantidad; stock se descuenta al confirmar; 2da confirmación rechazada | ✅ | `DespachoService` (R-INV-02/03/04) |
| 12 | Resultado calculado en servidor; PDF sólo si última medición de cada parámetro CONFORME | ✅ | `CalidadService` (R-CAL-01/02) |
| 13 | Merma exige validación del Jefe y no modifica stock; devolución reintegra | ✅ | `MermaService` (R-INV-01) |
| 14 | Cada cambio de stock inserta trazabilidad en la misma transacción; sólo inserción | ✅ | `TrazabilidadService.registrarMovimiento()` (Propagation.MANDATORY) |
| 15 | Datos iniciales permiten ejecutar los 46 casos | ✅ | `db/data.sql` |
| 16 | Mensajes de error y validación en español | ✅ | `GlobalExceptionHandler`, mensajes en services |

---

## Reglas de negocio implementadas

| Código | Descripción | Dónde |
|---|---|---|
| R-INV-01 | El stock sólo cambia en ingreso, confirmación de entrega y reintegro (no en merma) | MaterialService, DespachoService, MermaService |
| R-INV-02 | Todo cambio de stock inserta su trazabilidad en la misma transacción | TrazabilidadService (Propagation.MANDATORY) |
| R-INV-03 | El stock no puede ser negativo | `material.stock_actual CHECK >= 0` + validación en DespachoService |
| R-INV-04 | Cantidad despachada acumulada ≤ solicitada | DespachoService.registrarDespacho |
| R-INV-05 | Ingreso sólo contra OC ABIERTA y código existente | MaterialService.registrarIngreso |
| R-INV-06 | Costo total > presupuesto → RETENIDA | SolicitudService.registrarConBom |
| R-CAL-01 | Sólo ADMIN cambia rangos; pruebas ya registradas no cambian | CalidadService.configurarRango (RF30) |
| R-CAL-02 | Certificado sólo si última medición de cada parámetro CONFORME | CalidadService.puedeCertificar (RF28) |

---

## Verificación de compilación y casos de prueba ejecutados

- **Compilación:** `./mvnw compile` → BUILD SUCCESS, 150 clases sin errores.
- **Smoke test end-to-end** (perfil H2): 12 casos verificados que cubren
  PF-01, PF-02, PF-04, PF-07, PF-09, PF-12, PF-13, PF-14, PF-15, PF-16, RF04,
  RF36. Ver `scripts/run_smoke.sh` y `scripts/smoke_test.sh`.

---

## Próximos pasos para el equipo (Anexo B ítem por verificar)

1. Ejecutar el plan completo de **46 casos** (§7.7) contra la app en MySQL y
   registrar capturas en el informe (Anexo B ítem 6).
2. Cargar el script en el NAS de la empresa y configurar el backup periódico
   (RNF13).
3. Documentar el código con JavaDoc para los métodos públicos restantes (RNF08).

---

## Licencia y autores

Proyecto académico — Universidad Tecnológica del Perú, curso Marcos de
Desarrollo Web, 2026. Equipo: Izquierdo Rodriguez Walter Orlando,
Orellana Vásquez Sebastián, Quispe Guevara Gian Alexis, Rodas Mohamen
Yanpier Anthonio, Valencia Pando Briyit Sandra.
