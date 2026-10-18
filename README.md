# Artist Portfolio

Spring Boot 3 application with Thymeleaf, Spring Security and PostgreSQL.
Requires Java 21 or later and Maven (or the included Maven wrapper).

## Run

Create a PostgreSQL database named `artist_portfolio`. Configure credentials
through environment variables:

- `DB_URL`: defaults to `jdbc:postgresql://localhost:5432/artist_portfolio`.
- `DB_USERNAME`: defaults to `postgres`.
- `DB_PASSWORD`: password for the database user.
- `APP_ADMIN_USERNAME`: initial administrator username; defaults to `admin`.
- `APP_ADMIN_PASSWORD`: initial administrator password. If unset, no account is created.

Run `mvn spring-boot:run` or `./mvnw spring-boot:run` (Windows: `mvnw.cmd`).
Open http://localhost:8080. The administrator login is at `/login`.

Bootstrap credentials only create an account when that username does not exist.
They do not change an existing password. Rotate any previously created default
accounts and the previously committed database password before deployment.

Artwork files are stored in `uploads/artworks` and served at `/uploads/artworks/`.
Set `APP_UPLOAD_DIR` to change the storage directory; public URLs remain the same.
The author photo is not included; add a real portrait before adding it to the home page.

## Tests

Run `mvn test`. Tests use an isolated H2 database and require no PostgreSQL server.
Regression tests cover populated templates, artwork editing, image delivery,
order availability, restricted order fields, drafts and blog pagination.



## Fixes in this copy (2026-10-07)

- Unpublished artworks return 404 on public detail and order routes.
- Missing artworks and blog posts return 404 instead of 500.
- Repeated blog titles/slugs receive a numeric suffix; editing keeps an existing unique slug.
- Auto-generated slugs preserve Ukrainian letters and have a fallback for symbol-only titles.
- Valid contact submissions are stored in the `contact_messages` table. The confirmation says the message was saved. Email delivery is not configured; there is no contact inbox page yet.
- Added regression tests for public routes, contacts, blog slugs, login navigation and logout. All 16 tests pass with Java 24 and H2.

The repository contains source code and the original images, without build output or a database dump.

## Administrator Navigation

The public menu shows Login to visitors and the administrator panel to signed-in
administrators. Logout ends the session and returns to the home page.
Both login and logout are covered by regression tests.
