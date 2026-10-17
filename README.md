# VidaSalud MS Appointments

Microservicio de VidaSalud encargado de gestionar las atenciones o citas de los pacientes. Está construido con Spring Boot, Spring Data JPA y PostgreSQL.

## Responsabilidades

- Consultar las atenciones registradas.
- Consultar una atención por su identificador.
- Crear nuevas atenciones.
- Cambiar el estado de una atención mediante transiciones controladas.
- Persistir la información en PostgreSQL/Supabase.
- Publicar en RabbitMQ un evento por cada cita creada o cambio de estado.

## Tecnologías

- Java 17.
- Spring Boot 3.2.5.
- Spring Web.
- Spring Data JPA e Hibernate.
- PostgreSQL JDBC Driver.
- Spring AMQP (RabbitMQ).
- Maven.

## Puerto y endpoints

El servicio utiliza el puerto `8081`.

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/appointments` | Lista las atenciones y permite filtros por query params. |
| GET | `/api/appointments/{id}` | Obtiene una atención con su historial de cambios de estado. |
| GET | `/api/appointments/disponibilidad?servicioId=&fecha=` | Horas del día de una prestación, indicando cuáles están ocupadas y por qué. |
| POST | `/api/appointments` | Crea una atención. |
| PUT | `/api/appointments/{id}/status` | Cancela (o cambia manualmente) el estado de una atención, registrando quién lo hizo. |

## Flujo automático y agenda (simulación)

Para mostrar cómo funcionaría el centro en la vida real, el servicio simula su operación de forma acelerada. Todo se configura en `application.yml` (`vidasalud.flujo`).

- **Flujo automático** (`flujo/FlujoAutomatico`): cada 5 segundos avanza las citas en curso cuando cumplen su tiempo en cada estado: *Solicitada → (revisión automática, 15 s) → Confirmada → (llegada, 25 s) → En espera → (llamado al box, 25 s) → En atención → (40 s) → Cerrada*. Cada paso publica su evento en RabbitMQ, así notify emite el ticket y el comprobante sin intervención manual.
- **Historial** (`historial_atenciones`): cada cambio queda registrado con fecha, responsable (paciente, sistema o recepción) y detalle.
- **Agenda** (`service/AgendaService`): una hora no está disponible si ya está reservada, si ya pasó, si es domingo o si la agenda del profesional la tiene bloqueada (20 % de las horas, estable por día y especialidad). Reservar una hora ocupada responde **409**.
- **Demanda simulada** (`flujo/DemandaSimulada`): cada 60 segundos un paciente ficticio puede reservar una hora libre (y a veces cancelar otra), así la agenda se va ocupando sola. Hay un tope de 12 reservas simuladas a futuro, y las simuladas que llevan 30 minutos finalizadas o canceladas se eliminan solas (las citas reales nunca). Se apaga con la variable `DEMANDA_SIMULADA=false`.

## Mensajería (RabbitMQ)

Cada vez que se **crea una cita** o **cambia su estado**, el servicio publica un evento JSON en el exchange topic `vidasalud.citas.topic`. Los consume `vidasalud-ms-notify`.

| Evento | Routing key |
|---|---|
| Cita creada | `cita.creada` |
| Confirmada / En espera / En atención | `cita.confirmada`, `cita.en_espera`, `cita.en_atencion` |
| Cerrada / Cancelada | `cita.cerrada`, `cita.cancelada` |

- El exchange y la routing key de cada evento están escritos en `application.yml` (`vidasalud.mensajeria.exchanges` y `routing-keys`, con la misma estructura que ms-notify). La configuración está en `messaging/RabbitConfig`. Si falta alguna routing key, el servicio no arranca.
- El controlador depende de la interfaz `CitaEventPublisher`, no de RabbitMQ.
- Se activan las confirmaciones del broker (publisher confirms) y se registran en el log los mensajes que no llegan a ninguna cola.
- Si RabbitMQ no está disponible, la cita **se guarda igual** y solo se registra el error: la mensajería no altera la lógica existente.

Variables: `RABBITMQ_ADDRESSES` (por defecto `localhost:5672,localhost:5673`), `RABBITMQ_USER` y `RABBITMQ_PASS`.

## Ejecución local

Requisitos: Java 17, Maven y una base de datos PostgreSQL accesible.

```bash
mvn spring-boot:run
```

Para generar el ejecutable:

```bash
mvn clean package -DskipTests
java -jar target/appointments-1.0.0.jar
```

## Base de datos

La conexión se configura en `src/main/resources/application.yml`. El proyecto está preparado para utilizar el pooler de Supabase mediante PostgreSQL y `prepareThreshold=0`.

No se deben almacenar contraseñas reales en el repositorio. En un entorno productivo se recomienda utilizar variables de entorno o un gestor de secretos.

## Docker

El servicio puede ejecutarse como contenedor en el puerto `8081`:

```bash
docker build -t appointments .
docker run -d --name appointments -p 8081:8081 appointments
```

## Verificación rápida

```bash
curl -i http://localhost:8081/api/appointments
```
