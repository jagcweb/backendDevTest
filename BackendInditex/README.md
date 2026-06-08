# Similar Products API

Spring WebFlux service that implements the `GET /product/{productId}/similar` contract from
[`similarProducts.yaml`](../similarProducts.yaml) by composing the two existing endpoints
documented in [`existingApis.yaml`](../existingApis.yaml):

1. `GET /product/{productId}/similarids` — ordered list of similar product ids.
2. `GET /product/{id}` — detail for each of those ids.

## Running it

The mocks (and Grafana/InfluxDB for the load test) come from the repo's root
`docker-compose.yaml` and are published on `localhost:3001`. This app listens on
`localhost:5000`, which is what the k6 script (`host.docker.internal:5000`) expects — start
this app first, then run the k6 test against it.

**Docker only — no local Maven/JDK required** (recommended; the `Dockerfile` is a multi-stage
build that compiles with a Maven+JDK image and ships a slim JRE runtime):

```
docker build -t similar-products .
docker run --rm -p 5000:5000 similar-products
```

The image's `CATALOG_BASE_URL` already defaults to `http://host.docker.internal:3001` (the
container's view of the host, where the mocks are published), so no extra flags are needed —
verified end-to-end against the real k6 load test.

**With Maven + JDK 17 installed instead:**

```
mvn spring-boot:run
```

or build and run the jar directly:

```
mvn package
java -jar target/similar-products-1.0.0.jar
```

In these cases `CATALOG_BASE_URL` falls back to its `application.yml` default of
`http://localhost:3001`, which is correct because the app runs directly on the host
alongside the published mocks.

## Design notes

- **Reactive end-to-end (WebFlux + WebClient).** The mocked catalog can take anywhere from 0ms
  to 50s to answer (`shared/simulado/mocks.json`). Under the k6 load profile (constant 200 VUs)
  a thread-per-request model would exhaust its pool waiting on slow upstream calls; the
  non-blocking pipeline keeps resource usage flat regardless of how slow an individual
  upstream call is.
- **Per-call timeouts, not a single end-to-end timeout** (`CatalogClient`, tunable via
  `catalog.similar-ids-timeout` / `catalog.product-detail-timeout`, default 2s). This bounds
  the latency contributed by any single upstream call without an all-or-nothing cutoff.
- **Partial results over total failure.** The contract explicitly allows an empty/short list
  (`minItems: 0`). If a similar product's detail call fails or times out, `SimilarProductsService`
  logs and drops just that entry (`onErrorResume(... -> Mono.empty())`) instead of failing the
  whole `/similar` request — one slow or broken product (e.g. id `10000`, 50s delay, or id `6`,
  HTTP 500) shouldn't take down the response for the others.
- **Failure on the ids lookup is different**: without the similar-ids list there's nothing to
  aggregate, so a 404 there is propagated as 404 (`ProductNotFoundException`) and any other
  failure becomes a 503 (`UpstreamServiceException`), both mapped centrally in
  `GlobalExceptionHandler`.
- **Order preserved, fan-out bounded.** `flatMapSequential(..., DETAIL_FETCH_CONCURRENCY)`
  fetches product details concurrently (for throughput) while still emitting them in the
  similarity order returned by the upstream, and caps in-flight detail calls per request as a
  simple bulkhead against unexpectedly long similar-id lists.

## Tests

```
mvn test
```

- `SimilarProductsServiceTest` — order preservation, dropping failed details, and error
  propagation rules, using `StepVerifier` against a mocked `CatalogClient`.
- `SimilarProductsControllerTest` — HTTP-level mapping (`200`/`404`/`5xx`) with `WebTestClient`
  against a mocked `SimilarProductsService`.
