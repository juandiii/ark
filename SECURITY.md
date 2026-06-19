# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in Ark, please **do not** open a
public GitHub issue. Use one of the following channels:

1. **Preferred — GitHub private security advisory**:
   <https://github.com/juandiii/ark/security/advisories/new>
2. **Email**: juandiegolopezve@gmail.com (subject: `[Ark Security] <short summary>`)

You can expect:

- Acknowledgement within 3 business days.
- A coordinated disclosure timeline once the impact is confirmed.
- Credit in the release notes for the version that fixes the issue
  (unless you prefer to remain anonymous).

## Supported Versions

Security fixes are released for the latest minor version line. Older
minor versions are best-effort. Major versions follow Semantic Versioning.

| Version | Supported          |
|---------|--------------------|
| 1.x     | :white_check_mark: |

## Scope

This policy covers all artifacts published from this repository under the
`xyz.juandiii:ark-*` group on Maven Central. Vulnerabilities in transitive
dependencies should be reported to the upstream project first; Ark will
coordinate updates after the upstream fix lands.
