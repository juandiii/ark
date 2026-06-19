# Compatibility Matrix

Ark publishes a single artifact line, but supports a small range of Spring
Boot and Quarkus versions verified by CI. This page records what is known
to work, what is known not to, and what is untested.

## Current matrix

| Ark version | Spring Boot       | Quarkus           | Java       | Status                              |
|-------------|-------------------|-------------------|------------|-------------------------------------|
| 1.0.x       | 4.1.x (default)   | 3.35.x (default)  | 17, 21, 25 | ✅ Verified on every PR             |
| 1.0.x       | 4.0.x latest      | 3.35.x latest     | 17         | ✅ Verified weekly (latest patches) |
| 1.0.x       | 4.1.x latest      | 3.33.x LTS latest | 17         | ✅ Verified weekly (latest patches) |
| 1.0.x       | 4.0.x latest      | 3.33.x LTS latest | 17         | ✅ Verified weekly (latest patches) |
| 1.0.x       | 3.5.x or earlier  | any               | any        | ⚠️ Untested — likely incompatible (Spring 6.x vs 7.x APIs) |
| 1.0.x       | any               | 3.15.x or earlier | any        | ⚠️ Untested                         |

Legend:
- **Verified in CI** — full test suite passes on every PR.
- **Untested** — may work, no guarantee.
- **Known-broken** — has a documented failure mode (see notes below).

## How CI verifies this

Two GitHub Actions workflows in `.github/workflows/`:

1. **`test.yml` → `test` job** — runs on every PR and push to `main`. Java
   matrix (17, 21, 25) against the default Spring Boot and Quarkus
   versions pinned in root `pom.xml`. Catches Ark-code regressions
   immediately.

2. **`compat-weekly.yml` → `framework-matrix` job** — runs every Monday
   06:00 UTC (and on-demand via `workflow_dispatch`). Queries Maven
   Central for the latest patches of Spring Boot 4.0.x / 4.1.x and
   Quarkus 3.33.x LTS / 3.35.x, then runs the full test suite against
   four combinations. Catches **upstream regressions** introduced by new
   patch releases of Spring Boot or Quarkus.

When the weekly run fails, an issue is auto-created with the
`compat-regression` label.

## Why these versions

- **Spring Boot 4.0+**: Ark depends on Spring Framework 7.x APIs
  (`@HttpExchange`, AOT processors, `ConfigurationProperties` with record
  binding). Spring Boot 3.x ships Spring Framework 6.x; supporting both
  major lines would require split modules or a maintenance branch.
- **Quarkus 3.33 LTS**: current Quarkus long-term-support release.
  Many enterprise users pin to LTS.
- **Quarkus 3.35** (default): latest Quarkus release at the time of Ark
  1.0 release.
- **Java 17/21/25**: Java 17 is the minimum baseline.

## Known incompatibilities

None known.

## Reporting a new combination

If you use Ark on a combination not in the matrix and it works (or
doesn't), please open an issue with:

- Spring Boot version
- Quarkus version (if applicable)
- Java version
- Output of `./mvnw clean verify` (or your build's equivalent)
