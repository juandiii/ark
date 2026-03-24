# Contributing to Ark

Thank you for your interest in contributing to Ark! This guide will help you get started.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Git

### Clone and Build

```bash
git clone https://github.com/juandiii/ark.git
cd ark
mvn clean install
```

### Run Tests

```bash
mvn test                       # All modules
mvn test -pl ark-core          # Single module
mvn verify                     # Tests + coverage check
```

---

## Development Workflow

1. Create a branch from `main`
2. Make your changes
3. Push and open a PR to `main`
4. CI runs tests (Java 17, 21, 25) + commit lint
5. Add the appropriate label to the PR
6. PR is reviewed and merged

---

## Commit Convention

All commits **must** follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `chore` | Maintenance (deps, config) |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test` | Adding or updating tests |
| `ci` | CI/CD changes |
| `perf` | Performance improvement |
| `style` | Code style (formatting, whitespace) |
| `build` | Build system changes |

### Breaking Changes

Use `!` after the type or add `BREAKING CHANGE` in the commit body:

```
feat!: redesign transport API
```

### Examples

```
feat: add Mutiny execution model
feat(transport): add Apache HttpClient 5 support
fix: resolve null pointer in header validation
fix(proxy): handle unresolved path variables
docs: update README with Quarkus integration
chore: bump Jackson to 3.0
refactor: extract AbstractArkBuilder to top-level
test: add coverage for ArkJdkHttpTransport
ci: add commitlint workflow
perf: optimize URI building
```

### Commit Lint

A CI check validates all commits in your PR. If any commit does not follow the convention, the PR **cannot be merged**. Fix with:

```bash
git commit --amend -m "fix: correct commit message"
git push --force-with-lease
```

---

## PR Labels

Labels are auto-detected from your commit messages. You can also set them manually.

### Release Labels

These determine the version bump when a release is triggered:

| Label | Version Bump |
|-------|-------------|
| `fix` | PATCH (1.0.0 → 1.0.1) |
| `perf` | PATCH (1.0.0 → 1.0.1) |
| `feat` | MINOR (1.0.0 → 1.1.0) |
| `breaking change` | MAJOR (1.0.0 → 2.0.0) |

### Non-Release Labels

These are informational and do not trigger a release:

`docs`, `chore`, `refactor`, `test`, `ci`, `style`, `build`

---

## Project Structure

```
ark-core                       Core interfaces + fluent API
ark-async                      CompletableFuture execution model
ark-reactor                    Mono/Flux execution model
ark-mutiny                     Uni execution model
ark-vertx                      Vert.x Future execution model
ark-jackson                    Jackson serializer
ark-transport-jdk              Java HttpClient transport
ark-transport-reactor          Reactor Netty transport
ark-transport-vertx            Vert.x WebClient transport
ark-transport-vertx-mutiny     Vert.x Mutiny transport
ark-transport-apache           Apache HttpClient 5 transport
ark-proxy-spring               Declarative client from @HttpExchange
ark-spring-boot-starter        Spring MVC auto-config
ark-spring-boot-starter-webflux Spring WebFlux auto-config
ark-bom                        Bill of Materials
```

---

## Code Guidelines

- **Interfaces over concrete types** — `ClientRequest`/`ClientResponse` are interfaces, `Default*` are implementations
- **CRTP for fluent chaining** — `AbstractClientRequest<T>` uses self-referencing generics
- **Template Method** — `AbstractArkClient.createRequest()` defined once, subclasses implement
- **Bridge pattern** — transports wrap pre-configured HTTP clients, don't configure them
- **No duplication** — shared logic in `AbstractClientRequest`, `AbstractArkClient`, `AbstractArkBuilder`
- **Error handling** — `ApiException` for HTTP errors, `ArkException` for transport/config errors, validation in `validateResponse()`
- **Null safety** — validate null keys in `header()`, null types in `TypeRef.of()`

---

## Adding a New Execution Model

1. Create module `ark-<model>` with:
   - `<Model>Ark` interface
   - `<Model>ArkClient extends AbstractArkClient`
   - `<Model>HttpTransport` interface
   - `<Model>ClientRequest` interface + `Default<Model>ClientRequest extends AbstractClientRequest`
   - `<Model>ClientResponse` interface + `Default<Model>ClientResponse`
2. Add to parent `pom.xml` modules
3. Add to `ark-bom`
4. Add tests (minimum 70% coverage)

---

## Adding a New Transport

1. Create module `ark-transport-<name>` with:
   - `Ark<Name>Transport implements HttpTransport` (and/or `AsyncHttpTransport`, etc.)
   - Constructor receives pre-configured client (bridge pattern)
2. Add to parent `pom.xml` modules
3. Add to `ark-bom`
4. Add tests with real HTTP server (`com.sun.net.httpserver.HttpServer`)

---

## Tests

- **JUnit 5** + **Mockito**
- **`given_when_then`** naming convention
- **`@Nested`** classes for grouping
- **JaCoCo** coverage check: 70% instructions, 68% branches minimum
- Transport tests use real HTTP server, not mocks
- Mock tests demonstrate interface mockability (`ArkMockTest`, `AsyncArkMockTest`)

---

## Release Process

Releases are triggered manually via GitHub Actions:

1. PRs accumulate on `main` with labels
2. Maintainer clicks **Actions → Release → Run workflow**
3. Workflow reads PR labels → calculates version → deploys → tags → creates GitHub Release

---

## Questions?

Open an issue or start a discussion. We're happy to help!