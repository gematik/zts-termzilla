# Testing HTTP Caching with nginx

This guide describes how to run an nginx Docker container as a caching reverse proxy in front
of the terminology server to verify that HTTP caching works end-to-end.

No custom Docker image is required. The official `nginx` image is used with a locally mounted
configuration file.

---

## Response Headers Overview

The terminology server sets the following caching-relevant response headers on every GET/HEAD
response:

| Header          | Example value                         | Purpose                                                        |
|-----------------|---------------------------------------|----------------------------------------------------------------|
| `ETag`          | `W/"<sha256>"`                        | Fingerprint for conditional requests (`If-None-Match`)         |
| `Cache-Control` | `public, max-age=300, must-revalidate`| Cache lifetime and revalidation policy                         |
| `Vary`          | `Accept, Accept-Language`             | Tells caches which request headers affect the response content |

`Vary: Accept-Encoding` is added separately by Spring Boot's compression filter and is always
present regardless of operating mode.

---

## How It Works

1. The server sets `ETag`, `Cache-Control`, and `Vary` on every GET response.
2. nginx caches the first response and serves subsequent requests from cache.
3. When a cache entry expires (`max-age` elapsed), nginx **revalidates** by sending a conditional
   `GET` with `If-None-Match` to the upstream.
4. If the upstream responds with `304 Not Modified`, nginx refreshes the cache entry without
   re-transferring the response body.

The `X-Cache-Status` response header (added by nginx) shows whether a response was served from
cache (`HIT`) or fetched from the upstream (`MISS`, `REVALIDATED`, `EXPIRED`).

### Vary and cache efficiency

In **standard mode** the server sends `Vary: Accept, Accept-Language`. This is semantically
correct — the response body and format can differ per `Accept` value (JSON vs. XML) and per
`Accept-Language` value (designation language). However, some CDNs create a separate cache
entry for every distinct combination of these headers, which reduces cache hit rates.

In **CDN-optimized mode** (`core.caching.cdnOptimized=true`) the server strips incoming
`Accept` and `Accept-Language` headers and always responds with its configured defaults (JSON,
system language). As a result `Vary: Accept, Accept-Language` is omitted and every URL maps to
exactly one cache entry — significantly improving CDN efficiency.

---

## Standard Mode

### nginx configuration

Create `nginx-cache.conf`:

```nginx
proxy_cache_path /tmp/nginx_cache
    levels=1:2
    keys_zone=fhir_cache:10m
    max_size=1g
    inactive=60m
    use_temp_path=off;

server {
    listen 80;

    location / {
        proxy_pass http://host.docker.internal:8080;

        # Enable caching for GET and HEAD
        proxy_cache         fhir_cache;
        proxy_cache_methods GET HEAD;

        # Respect Cache-Control from upstream (max-age, must-revalidate)
        # proxy_cache_valid is used as a fallback when no Cache-Control is present
        proxy_cache_valid   200 10m;

        # When a cached entry expires, revalidate using If-None-Match (ETag)
        proxy_cache_revalidate on;

        # Forward original client IP
        proxy_set_header X-Real-IP        $remote_addr;
        proxy_set_header X-Forwarded-For  $proxy_add_x_forwarded_for;
        proxy_set_header Host             $host;

        # Show cache status in the response (HIT / MISS / REVALIDATED / EXPIRED / BYPASS)
        add_header X-Cache-Status $upstream_cache_status always;
    }
}
```

> **`proxy_cache_revalidate on`**
> When a cached entry expires, nginx sends a conditional `GET` with `If-None-Match` to the
> upstream. If the upstream returns `304`, the cache entry is refreshed without re-transferring
> the response body.

> **Vary handling**
> The upstream sends `Vary: Accept, Accept-Language`. nginx respects this and stores a separate
> cache entry for each combination of `Accept` and `Accept-Language` values seen in requests.
> In deployments where clients send many different `Accept` combinations, consider switching to
> CDN-optimized mode (see below).

### Start the nginx container

**macOS:**

```bash
docker run --rm \
  --name nginx-fhir-cache \
  -p 8081:80 \
  -v "$(pwd)/nginx-cache.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:stable-alpine
```

**Linux:**

On Linux, `host.docker.internal` is not resolved automatically. Pass it as an extra host:

```bash
docker run --rm \
  --name nginx-fhir-cache \
  -p 8081:80 \
  --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/nginx-cache.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:stable-alpine
```

nginx is now reachable at `http://localhost:8081` and proxies to the terminology server on
port `8080`.

### Verifying the cache

All examples use `curl`. `-D -` prints response headers; `-s -o /dev/null` suppresses the body.

#### First request — cache MISS

```bash
curl -s -D - http://localhost:8081/fhir/CodeSystem -o /dev/null
```

Expected headers:

```
HTTP/1.1 200 OK
ETag: W/"<sha256hash>"
Cache-Control: public, max-age=300, must-revalidate
Vary: Accept, Accept-Language
X-Cache-Status: MISS
```

#### Second request — cache HIT (no upstream call)

```bash
curl -s -D - http://localhost:8081/fhir/CodeSystem -o /dev/null
```

Expected headers:

```
HTTP/1.1 200 OK
ETag: W/"<sha256hash>"
Cache-Control: public, max-age=300, must-revalidate
Vary: Accept, Accept-Language
X-Cache-Status: HIT
```

#### Conditional request from the client — 304 from nginx cache

```bash
ETAG=$(curl -s -I http://localhost:8081/fhir/CodeSystem \
  | grep -i '^etag:' | tr -d '\r' | awk '{print $2}')

curl -s -o /dev/null -w "%{http_code}" \
  -H "If-None-Match: $ETAG" \
  http://localhost:8081/fhir/CodeSystem
```

Expected output: `304`

#### After cache expiry — REVALIDATED (upstream receives If-None-Match)

When the `Cache-Control: max-age` period has elapsed, nginx sends a conditional request to the
upstream. If the server responds with `304`, nginx updates the cache entry and returns `200` to
the client:

```
X-Cache-Status: REVALIDATED
```

---

## CDN-Optimized Mode

Enable CDN-optimized mode by setting `core.caching.cdnOptimized=true` (or the environment
variable `CORE_CACHING_CDNOPTIMIZED=true`). In this mode:

- Incoming `Accept` and `Accept-Language` headers are stripped by the server before processing.
- Responses always use the server's configured defaults: JSON format, system language.
- `Vary: Accept, Accept-Language` is **not** sent.
- Each URL maps to exactly one cache entry regardless of what headers the client sent.

### nginx configuration for CDN-optimized mode

Because `Vary: Accept, Accept-Language` is absent, no special Vary handling is needed:

```nginx
proxy_cache_path /tmp/nginx_cache
    levels=1:2
    keys_zone=fhir_cache:10m
    max_size=1g
    inactive=60m
    use_temp_path=off;

server {
    listen 80;

    location / {
        proxy_pass http://host.docker.internal:8080;

        proxy_cache         fhir_cache;
        proxy_cache_methods GET HEAD;
        proxy_cache_valid   200 10m;
        proxy_cache_revalidate on;

        proxy_set_header X-Real-IP        $remote_addr;
        proxy_set_header X-Forwarded-For  $proxy_add_x_forwarded_for;
        proxy_set_header Host             $host;

        add_header X-Cache-Status $upstream_cache_status always;
    }
}
```

### Verifying CDN-optimized mode

#### Accept header is ignored — response is always JSON

```bash
curl -s -D - \
  -H "Accept: application/fhir+xml" \
  http://localhost:8081/fhir/CodeSystem -o /dev/null
```

Expected headers:

```
HTTP/1.1 200 OK
Content-Type: application/fhir+json;charset=utf-8
Cache-Control: public, max-age=300, must-revalidate
X-Cache-Status: MISS
```

Note that `Vary: Accept, Accept-Language` is absent. A second request with a different
`Accept` header will still produce a cache `HIT` because the cache key does not depend on
the `Accept` value.

#### Second request with different Accept — still a HIT

```bash
curl -s -D - \
  -H "Accept: application/fhir+xml" \
  http://localhost:8081/fhir/CodeSystem -o /dev/null
```

Expected headers:

```
HTTP/1.1 200 OK
Content-Type: application/fhir+json;charset=utf-8
X-Cache-Status: HIT
```

---

## Inspecting nginx Logs

The container logs show every proxied request:

```bash
docker logs -f nginx-fhir-cache
```

A `HIT` response does not generate an upstream request — only `MISS` and `REVALIDATED` entries
indicate that nginx contacted the terminology server.

---

## Stopping the Container

```bash
docker stop nginx-fhir-cache
```

Because the container was started with `--rm`, it is removed automatically on stop.
