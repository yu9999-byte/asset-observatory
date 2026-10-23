# DeBank address history API review - 2026-10-22

## Scope and evidence

This is a bounded review of DeBank Cloud's official API documentation and
Terms of Service for the address-transfer-event workflow in an externally
accessible research product. The source documents were saved verbatim under
`evidence/acquisition/debank-address-api-20261022/` from the official
documentation index. No DeBank account was registered, no API key was obtained,
and no credentialed endpoint was called. The saved documentation hashes are
recorded by the acquisition capture; they should be rechecked before any future
integration because the terms page is dated 2022-10-25.

## Capability found

The official User API documents `GET /v1/user/history_list` on
`https://pro-openapi.debank.com`. It requires an address (`id`) and documents
optional `chain_id`, `token_id`, `start_time`, and `page_count`; a page is
limited to 20 entries. The sample response includes transaction hash/time,
transaction type, token metadata, amount, USD value, From/To, gas values and
some CEX counterparty information. These are documentation examples only, not
live JCT observations. The API is potentially relevant to address events, but
the docs reviewed do not establish that it gives complete raw token-transfer
logs, all historical records, a stable cursor, reorg/finality semantics,
event-time market price, or a specific coverage/refresh SLA. USD values and
labels must be treated as provider-derived fields until their methodology and
provenance are validated.

The OpenAPI documentation says an account registration is required to obtain
an AccessKey and units may need to be purchased. It states a Pro Plan limit of
up to 100 requests/second; that is not evidence that the product has a free
ongoing quota. The same page says policy changes such as pricing and terms will
be announced in advance.

## Use-rights assessment

The official Terms of Service page retrieved for this review identifies itself
as last updated and effective 2022-10-25. Clause 2.1 limits a free trial to at
most 14 days and says the agreement automatically terminates after the trial
unless a paid subscription is started or DeBank agrees to an extension. Clause
2.2 allows the trial to be suspended or discontinued at any time. Clause 4(m)
says the subscriber shall not resell or share the account and data with other
entities.

The reviewed text does not expressly grant this product the right to display
DeBank API data to external users, store/cache it, redistribute it, or publish
metrics derived from it. Clause 4(m) creates a material restriction for the
planned public product, but this review does not infer a broader legal
interpretation from that clause alone. Publicly viewable DeBank pages and API
documentation do not establish API reuse rights.

## Decision and unlock conditions

Keep DeBank disabled. It is not compatible with the current zero-budget,
external-user scope on evidence available in this review. Do not create an
adapter, expose sample payloads as observations, or describe DeBank as a
verified source of complete JCT transfer history.

Reconsider only after obtaining (1) an eligible account/plan and stable quota,
(2) current terms or written provider confirmation that explicitly covers
external display, storage/cache, redistribution to product users, and derived
metrics, and (3) a credentialed exact-JCT endpoint response for the confirmed
chain and contract. Then verify pagination/history coverage, timestamps,
event identity and reconciliation against an independent chain source before
implementation.

## Captured official documents

- `acquisition/debank-address-api-20261022/api-index.md`
- `acquisition/debank-address-api-20261022/open-api.md`
- `acquisition/debank-address-api-20261022/user-api.md`
- `acquisition/debank-address-api-20261022/terms-of-service.md`

