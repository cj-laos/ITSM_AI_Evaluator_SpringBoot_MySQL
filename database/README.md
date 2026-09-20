# Base de datos

La base se administra con Flyway:

- `src/main/resources/db/migration/V1__classification_evaluation.sql`
- `src/main/resources/db/migration/V2__adaptive_assignment_foundation.sql`

Con MySQL levantado, basta iniciar Spring Boot; las migraciones se aplican en orden.
El catálogo de severidades queda exactamente así:

| id | nivel |
|---:|---|
| 1 | Baja |
| 2 | Media |
| 3 | Alta |
| 4 | Crítica |

La migración V2 solo prepara la persistencia de la segunda fase. No altera las métricas
ni asigna técnicos durante el experimento de clasificación.
