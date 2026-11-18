# Manual de Despliegue — Smart Power On S.A.C.

Este manual describe cómo desplegar la aplicación en el servidor **NAS Synology
DS224+** de la empresa (RNF05, RNF12, OE8). La aplicación se ejecuta en la red
interna de la planta.

## 1. Requisitos del servidor NAS

| Recurso | Mínimo | Recomendado |
|---|---|---|
| CPU | Intel Celeron J4125 | — |
| RAM | 2 GB | 4 GB |
| Disco | 2 × WD Red Plus 4 TB (RAID 1) | — |
| Java | OpenJDK 21 | Temurin 21 LTS |
| MySQL | 8.0 | 8.0.x |
| Memoria de la app | 512 MB | 1 GB |

## 2. Instalación paso a paso

### 2.1 Preparar MySQL en el NAS

Abrir SSH al NAS y:

```bash
# Instalar MariaDB/MySQL desde el Centro de Paquetes de Synology,
# luego desde shell:
mysql -u root -p
```

```sql
CREATE DATABASE smart_power_on_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'smartpoweron'@'localhost' IDENTIFIED BY '<contraseña-segura>';
GRANT ALL PRIVILEGES ON smart_power_on_db.* TO 'smartpoweron'@'localhost';
FLUSH PRIVILEGES;
```

### 2.2 Cargar el esquema y los datos iniciales

#### Opción A — Base nueva (desde cero)

Sólo para una base nueva (el esquema contiene instrucciones `DROP TABLE`):

```bash
# Desde el directorio del proyecto:
mysql -u smartpoweron -p smart_power_on_db < src/main/resources/db/schema.sql
mysql -u smartpoweron -p smart_power_on_db < src/main/resources/db/data.sql
```

No volver a ejecutar `schema.sql` ni `data.sql` sobre una base con datos.

#### Opción B — Base existente `basecompleta.sql` (recomendado si ya tienes datos)

Si ya cargaste tu base `basecompleta.sql` en MySQL (con tus 10 materiales,
3 órdenes de compra, 3 proyectos, 5 usuarios y los stored procedures), sólo
falta añadir las columnas de borrado lógico que usa la aplicación. Hay un
**único script consolidado idempotente**:

```bash
mysql -u root -p smart_power_on_db < src/main/resources/db/MIGRAR_BASE_EXISTENTE.sql
```

Este script:
- Verifica cada columna contra `INFORMATION_SCHEMA` y sólo añade las que falten.
- Añade `anulada` + `motivo_anulacion` a `solicitud`, `despacho`,
  `prueba_calidad` y `merma_devolucion`.
- Añade `activo` a `parametro_prueba`.
- Conserva todos tus datos y tus stored procedures existentes
  (`sp_ConfirmarEntregaYDescontar`, `sp_RegistrarIngresoMaterial`,
  `sp_ValidarMermaDevolucion`).
- Puede ejecutarse cuantas veces sea necesario (es idempotente).

> Alternativa granular: aplicar en orden `V2` hasta `V6` desde
> `src/main/resources/db/migration/`. El consolidado hace exactamente lo mismo
> en un solo paso.

### 2.3 Ajustar credenciales

La contraseña MySQL viene configurada por defecto como `123456789` en
`application.properties`:

```
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:123456789}
```

Por defecto la app se conecta a `jdbc:mysql://localhost:3306/smart_power_on_db`
con usuario `root` y contraseña `123456789`. Si tu configuración es diferente,
sobreescribe con variables de entorno (sin tocar el archivo):

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/smart_power_on_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='123456789'
```

### 2.4 Compilar el JAR

```bash
./mvnw clean package -DskipTests
# Genera target/gestion-materiales-1.0.0.jar
```

### 2.5 Copiar al NAS y ejecutar como servicio

```bash
# Copiar el JAR al NAS (vía SCP / File Station)
scp target/gestion-materiales-1.0.0.jar admin@nas:/volume1/smartpoweron/

# En el NAS, crear un servicio con el Task Scheduler de Synology:
#   java -jar /volume1/smartpoweron/gestion-materiales-1.0.0.jar
#   Usuario: smartpoweron
#   Inicio: al arrancar el NAS
```

### 2.6 Verificar

Desde un equipo de la planta:

```
http://<ip-nas>:8080/login
```

Iniciar sesión con `admin@smart.pe` / `Admin123`.

## 3. Backup de la base de datos (RNF13)

Programar en el **Task Scheduler** del NAS un backup diario a las 23:00:

```bash
mysqldump -u smartpoweron -p<contraseña-segura> smart_power_on_db \
  | gzip > /volume1/smartpoweron/backup/smart_power_on_$(date +%Y%m%d).sql.gz

# Conservar 30 días:
find /volume1/smartpoweron/backup/ -name '*.sql.gz' -mtime +30 -delete
```

## 4. Rotación del secreto JWT

El secreto del JWT está en `app.jwt.secreto` de `application.properties`. Se
recomienda rotarlo cada 6 meses:

1. Generar uno nuevo: `openssl rand -base64 64`
2. Reemplazarlo en `application.properties`.
3. Reiniciar la app. Los tokens vigentes se invalidan y los usuarios deben
   reautenticarse (RF04).

## 5. Cierre por inactividad (RF04)

El token JWT vence a los 480 minutos (8 horas) por defecto. Para acortarlo a
30 minutos (recomendado para ambiente de planta):

```
app.jwt.expiracion-minutos=30
```

## 6. Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| 401 al primer request | Token no enviado o vencido | Reautenticar en /login |
| 403 en una operación | El rol del usuario no está autorizado | Revisar Tabla 70 del informe |
| 400 "La orden de compra no está ABIERTA" | Se intenta ingresar contra OC CERRADA | Usar una OC ABIERTA (PF-13) |
| 400 "El código ya se encuentra registrado" | Material duplicado (RF05) | Usar otro código |
| 400 "Stock insuficiente..." | Intenta confirmar entrega sin stock (R-INV-03) | Registrar ingreso primero |
| PDF no se genera | Hay parámetros sin medir o NO_CONFORME (R-CAL-02) | Repetir pruebas hasta CONFORME |

## 7. Comandos útiles

```bash
# Ver logs en vivo
tail -f /volume1/smartpoweron/app.log

# Reiniciar la app
pkill -f gestion-materiales
nohup java -jar /volume1/smartpoweron/gestion-materiales-1.0.0.jar \
  > /volume1/smartpoweron/app.log 2>&1 &

# Backup manual
mysqldump -u smartpoweron -p smart_power_on_db > backup_manual.sql
```
