# CoinCap candidate terms review - 2026-10-22

CoinCap was considered as an alternative aggregate market directory after
CoinPaprika's external-display licensing gate and the unresolved CoinAnk API
entitlement. This review checks whether CoinCap may be used by this
external-facing product; it is not approval to ingest data.

## Official page evidence

At 2026-10-22 04:25:39 UTC, a read-only HTTPS request through the existing
system proxy retrieved `https://coincap.io/` (HTTP 200, 33,191 bytes,
SHA-256 `B521C0C88FC6134F92C98D8C8B1B54D496AE2D6C346D03E65C6A4134F1010BEA`).
The homepage links its terms at
`https://coincap.io/terms-of-service?tab=terms` and API documentation at
`https://docs.coincap.io/`.

The linked terms page returned HTTP 200 at 2026-10-22 04:25:39 UTC, 93,182
bytes, SHA-256
`E93B01EA651CBE3921EEC5D4FE64C932E001B2D75C385910CCEACD9EBF36857E`. Its
visible terms text grants a limited, non-transferable, revocable license to
use the Services "for your personal use only," prohibits professional use,
and says the Services are "not suitable for commercial use." These restrictions
are incompatible with using the service to supply a product to external users.

The exact captured homepage and terms responses and HTTP headers are retained
under `evidence/acquisition/coincap-probe-20261022/`.

## Field and coverage status

No API endpoint was called after the terms review. Therefore live asset count,
provider identifiers, chain/contract identity, price and volume fields,
source timestamps, history, refresh limits, quotas, and data accuracy have not
been tested. No facts about those fields should be inferred from the API-docs
link or the website UI. External display, storage, caching, redistribution and
derived-metric use are not granted by this observed personal-use license.

## Decision and unlock

Reject CoinCap as a runtime source under the current zero-budget,
external-user scope. Do not add an adapter or expose its data. Reconsider only
after receiving a written license or provider terms that expressly cover the
product's external display, operational storage/cache, and derived metrics.
This is a provider-use restriction, not a transport or missing-data failure.
