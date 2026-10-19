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

## Artwork Media, Prices and Sizes

Each artwork keeps its cover plus up to six additional JPEG/PNG photos (5 MB and
30 megapixels each) and one MP4 video (H.264, AAC audio or silent, up to 100 MB).
Manage them in the artwork editor. New photos append in selection order; mark
existing photos for removal to free slots. Selecting a video replaces the old one.
Removal and replacement happen only after saving. Failed database transactions
keep previous files and remove newly uploaded files. Artwork deletion removes
its media after the database commit. Videos do not autoplay and support seeking.

The editor accepts dimensions in centimetres or inches. Values are stored as
decimal centimetres; one inch is exactly 2.54 cm. Existing integer dimensions
remain valid. Visitors can select centimetres/inches and UAH/USD; the browser
remembers their choice. Enter UAH and USD prices independently: no exchange-rate
service or automatic conversion is used. An unset price in the selected currency
is displayed as "Price on request".

The existing Hibernate update setting adds artwork_photos and video_url/price_usd
and changes width_cm/height_cm to decimal columns. Back up PostgreSQL and the
uploads directory before deploying; schema updates have been tested with H2,
not with a live PostgreSQL database. Include the new JCodec Maven dependency in
deployment. Multipart limits are 100 MB per file and 136 MB per request; a reverse
proxy, when used, must also allow the configured request size.

## Gallery and Presentation

Gallery filters combine material, surface, technique, genre, year, availability
and a price range in the selected currency. Sorting supports year, price and the
localized title; unknown prices are last in either price ordering. Drafts are
excluded from public results and filter values. Legacy category URLs still work
and combine with the other filters.

Administrators can add dictionary entries and update their English translations
at /admin/settings. Existing stored values remain available in the artwork editor.
The added artwork_terms table and technique column use the existing Hibernate
schema-update setting; back up the database before deployment.

Additional images are a single horizontally scrollable thumbnail strip. Video
opens in a native dialog, loads only when opened and is stopped/unloaded on close.
No social profile links are configured yet; the four contact buttons are disabled.
No buyer registration or social-login integration was added.

The studio background includes a subdued grayscale interpretation of the supplied
harbour painting on the distant easel. The menu underline uses a generated
transparent paint-stroke bitmap. The cursor pair uses an orange
arrow across the page and a custom thin, pointed round artist's brush over links
and actions, with standard text-entry cursors retained. Both are native SVG
cursor assets; touch devices keep their normal behaviour. The easel and canvas
have also been reduced together for a deeper viewpoint and rebuilt support legs.
Contact icons are from Simple Icons (CC0; license included).

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
