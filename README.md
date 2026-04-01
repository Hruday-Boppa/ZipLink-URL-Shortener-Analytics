# URL Shortener (Bitly-style)

Production-focused URL shortener built with Spring Boot.

## Features

- `POST /api/v1/urls`: Create short links from long URLs
- `GET /{shortCode}`: High-performance redirect with Redis cache
- `GET /api/v1/urls/{shortCode}`: Link metadata and click stats
- Redis-backed rate limiting on link creation
- Indexed URL table for fast code/hash lookups
- Custom hash-based short code generation with collision retries

## Quick Start

1. Create a PostgreSQL database:

```bash
createdb url_shortener
```

2. Start Redis on `localhost:6379`.
3. Run the app:

```bash
mvn spring-boot:run
```

If your local Postgres credentials differ, set:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/url_shortener
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

4. Create a short URL:

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com/some/very/long/path"}'
```

5. Use the returned short URL in a browser or with `curl -i`.

## Configuration

Main settings live in `src/main/resources/application.yml`:

- `app.base-url`
- `app.short-code.length`
- `app.short-code.secret`
- `app.cache.default-ttl`
- `app.rate-limit.requests-per-minute`
- `app.rate-limit.window`

## Production Notes

- Keep Redis highly available.
- Set a strong `app.short-code.secret`.
- Place behind a reverse proxy and forward `X-Forwarded-For`.
