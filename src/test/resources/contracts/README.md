# OpenAPI validation source

`openapi-3.1-2022-10-07.json` is the unmodified OpenAPI Initiative document
structure schema from https://spec.openapis.org/oas/3.1/schema/2022-10-07,
retrieved 2026-10-21. SHA256:
`da01ba28852cac0de53893797cb8d1942bc3b05084f526dcc216717dec314ed0`.

It validates OpenAPI document structure, not the embedded Schema Objects.
Tests separately validate those objects against the bundled JSON Schema
2020-12 meta-schema and validate actual MVC payloads with NetworkNT 1.5.9
(Apache-2.0, test scope only). No network call is needed during these tests.
Source: https://github.com/OAI/OpenAPI-Specification (Apache-2.0).

Nonempty API examples in tests are synthetic software fixtures. They are never
wired to production adapters and do not establish live data coverage.
