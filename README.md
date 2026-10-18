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

## Design and Languages

The public site uses a monochrome studio image with an orange accent. The studio
background is generated artwork; it is not a photograph of the artist's own studio.
Uploaded paintings are displayed in their original colours without cropping.

Ukrainian is the default interface language. The UKR / EN links change the current
page language and remember the choice in a cookie for one year. Gallery filters
and journal pagination are preserved. Navigation, forms, validation and admin
pages are translated through the message bundles in src/main/resources.

Artwork editing includes optional English title and description fields. Journal
editing includes English title, excerpt and content fields. Empty translations
fall back to the original text. Existing records are not automatically translated
or overwritten. Standard Ukrainian medium, surface and genre values have English
labels; custom category names retain their stored text.

On the next normal PostgreSQL startup, Hibernate's configured ddl-auto=update
adds the nullable English columns. It does not create a separate database. Back up
the database before deploying a schema change. Existing accounts and image paths
remain unchanged.

## Artist Biography

The About page is available at /about. Administrators can edit both language
versions of the artist name, short introduction and biography at /admin/profile.
The initial draft contains only the artist name, materials and approaches supplied
by the owner. A public page visit never creates or overwrites the stored profile.

A JPEG or PNG portrait (up to 5 MB and 30 megapixels) can be uploaded from the
same form. Portraits are validated, re-encoded as PNG and stored in uploads/artist.
Set APP_ARTIST_UPLOAD_DIR to change that directory. A previous photograph remains
when saving text without a new upload. Until a portrait is supplied, the page uses
an explicitly captioned studio illustration, not a fabricated artist photograph.

The light neutral canvas background uses a generated raster texture with a
translucent off-white overlay across public pages and the admin interface.

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
- Added five regression tests; all 15 tests pass with Java 21 and H2. PostgreSQL deployment has not been tested here.

The ZIP contains source code and the original images, without build output or a database dump.
