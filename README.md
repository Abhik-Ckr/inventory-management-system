# Inventory Management System

Java 17 / Spring Boot / Gradle backend implementing the flow and ER design
worked out earlier: Product (catalog) + Inventory (stock levels) as
separate 1:1 tables, Sale/SaleItem for transactions, and an append-only
StockMovement log that backs both the audit trail and the reports.

## Stack
- Java 17, Spring Boot 3.3
- Gradle
- Spring Web, Spring Data JPA, Spring Validation
- PostgreSQL
- Lombok
- JUnit 5 + Mockito

## Running it

1. Create a local Postgres database:
   ```
   createdb inventory_db
   ```
2. Set credentials. `DB_PASSWORD` is **required** — there is no default
   baked into `src/main/resources/application.properties`, so the app will
   fail to start if it isn't set. `DB_USERNAME` defaults to `postgres`.
   ```
   export DB_USERNAME=postgres
   export DB_PASSWORD=your-db-password
   ```
3. Run:
   ```
   ./gradlew bootRun
   ```
   (You'll need to run `gradle wrapper` once first if the `gradlew` script
   isn't present, or just use a local Gradle install.)

The app starts on `http://localhost:8080`. `ddl-auto: update` will create
the schema automatically on first run — fine for development, but swap in
Flyway/Liquibase before this goes near a real environment.

## API surface

| Action | Method & path |
|---|---|
| Create product | `POST /api/products` |
| List products | `GET /api/products` |
| Get product | `GET /api/products/{id}` |
| Update product | `PUT /api/products/{id}` |
| Deactivate product | `DELETE /api/products/{id}` |
| Add stock | `PATCH /api/inventory/{productId}/add` |
| Remove/adjust stock | `PATCH /api/inventory/{productId}/remove` |
| Create sale | `POST /api/sales` |
| List sales | `GET /api/sales` |
| Get sale | `GET /api/sales/{id}` |
| Stock report | `GET /api/reports/stock` |
| Low-stock report | `GET /api/reports/low-stock` |
| Sales report | `GET /api/reports/sales?from=YYYY-MM-DD&to=YYYY-MM-DD` |

## Tests

```
./gradlew test
```

Covers `ProductServiceImpl` (create, duplicate SKU) and `SaleServiceImpl`
(stock reduction on sale, insufficient-stock rejection) with Mockito.

## Not included yet

- **Authentication/authorization** — no Spring Security dependency is
  wired in, per the tech stack notes ("can be added later"). Every
  endpoint is currently open.
- **Database migrations** — using `ddl-auto: update` for now.
- **Integration tests** — the tests here are unit tests against mocked
  repositories; add Testcontainers + `@SpringBootTest` if you want
  end-to-end coverage against a real Postgres instance.
