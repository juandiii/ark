## [Unreleased]

### Changed

- **Project structure**: Maven modules are now grouped under semantic
  subdirectories at the repo root: `core/`, `execution-models/`, `transports/`,
  `serializers/`, `proxies/`, `starters/`, `extensions/`. Maven coordinates
  (groupId, artifactId, version) are **unchanged** — users do not need to
  update dependency declarations.

### Breaking

- **Spring starter packages renamed**:
  - `xyz.juandiii.spring.*` → `xyz.juandiii.ark.spring.*`
    (ark-spring-boot-starter)
  - `xyz.juandiii.spring.webflux.*` → `xyz.juandiii.ark.spring.webflux.*`
    (ark-spring-boot-starter-webflux)

  This affects users who explicitly import classes such as
  `EnableArkClients`, `ArkProperties`, `EnableArkWebFluxClients`, or
  `ArkWebFluxProperties`. Users who only depend on the starter via Maven
  (the typical case — relying on Spring Boot auto-configuration) are not
  affected.

---

## [v1.0.6](https://github.com/juandiii/ark/releases/tag/v1.0.6) — 2026-04-22

### 🐛 Bug Fixes

- fix: improve github actions (#67) @juandiii
- Develop (#75) @juandiii

### 📦 Other Changes

- ci: bump softprops/action-gh-release from 2 to 3 (#68) @app/dependabot
- ci: bump actions/setup-java from 4 to 5 (#69) @app/dependabot
- chore(deps): bump the spring-boot group with 3 updates (#70) @app/dependabot
- chore(deps): bump the quarkus group with 3 updates (#71) @app/dependabot
- chore(deps-dev): bump org.apache.maven.plugins:maven-javadoc-plugin from 3.11.2 to 3.12.0 (#72) @app/dependabot
- chore(deps): bump org.codehaus.mojo:flatten-maven-plugin from 1.6.0 to 1.7.3 (#73) @app/dependabot
- chore(deps): bump org.sonatype.central:central-publishing-maven-plugin from 0.7.0 to 0.10.0 (#74) @app/dependabot

**Full Changelog**: https://github.com/juandiii/ark/compare/v1.0.5...v1.0.6

---

## [v1.0.5](https://github.com/juandiii/ark/releases/tag/v1.0.5) — 2026-04-02

### 🐛 Bug Fixes

- Fix Ark-BOM (#64) @juandiii

---

## [v1.0.3](https://github.com/juandiii/ark/releases/tag/v1.0.3) — 2026-04-02

---

## [v1.0.2](https://github.com/juandiii/ark/releases/tag/v1.0.2) — 2026-04-01

### 🐛 Bug Fixes

- fix: maven ark-quarkus-jackson (#53) @juandiii

---

## [v1.0.1](https://github.com/juandiii/ark/releases/tag/v1.0.1) — 2026-04-01

### 📦 Other Changes

- chore: add maven central metadata to all module POMs (#51) @juandiii

---

## [v1.0.0](https://github.com/juandiii/ark/releases/tag/v1.0.0) — 2026-04-01

### ✨ Features

- feat: support auto-configuration spring-boot-webflux (#40) @juandiii
- add support multipart (#42) @juandiii

---
