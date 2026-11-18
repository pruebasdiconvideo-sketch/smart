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

Sólo para una base nueva (el esquema contiene instrucciones `DROP TABLE`):

```bash
# Desde el directorio del proyecto:
mysql -u smartpoweron -p smart_power_on_db < src/main/resources/db/schema.sql
mysql -u smartpoweron -p smart_power_on_db < src/main/resources/db/data.sql
```

Para actualizar una base existente, realizar primero un respaldo y aplicar en
orden `V2__material_extra_columns.sql` hasta `V6__mermas_devoluciones_logical_delete.sql`.
No volver a ejecutar `schema.sql` ni `data.sql` sobre una base con datos.

### 2.3 Ajustar credenciales

Configurar las variables de entorno del usuario del servicio; no insertar la
contraseña en `application.properties`:

```
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/smart_power_on_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='smartpoweron'
export SPRING_DATASOURCE_PASSWORD='<contraseña-segura>'
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
