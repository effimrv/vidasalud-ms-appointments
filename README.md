# VidaSalud MS Appointments

Microservicio de VidaSalud encargado de gestionar las atenciones o citas de los pacientes. Está construido con Spring Boot, Spring Data JPA y PostgreSQL.

## Responsabilidades

- Consultar las atenciones registradas.
- Consultar una atención por su identificador.
- Crear nuevas atenciones.
- Cambiar el estado de una atención mediante transiciones controladas.
- Persistir la información en PostgreSQL/Supabase.

## Tecnologías

- Java 17.
- Spring Boot 3.2.5.
- Spring Web.
- Spring Data JPA e Hibernate.
- PostgreSQL JDBC Driver.
- Maven.

## Puerto y endpoints

El servicio utiliza el puerto `8081`.

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/appointments` | Lista las atenciones y permite filtros por query params. |
| GET | `/api/appointments/{id}` | Obtiene una atención por ID. |
| POST | `/api/appointments` | Crea una atención. |
| PUT | `/api/appointments/{id}/status` | Cambia el estado de una atención. |

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
