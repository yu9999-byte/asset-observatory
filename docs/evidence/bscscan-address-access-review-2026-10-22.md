# BscScan address transfers: bounded access review

Observed on 2026-10-22, including a browser extraction timestamp of
2026-10-22T08:07:51.306Z. This is source-discovery evidence, not an approved
ingestion path or a complete address history.

## Scope

User reference:
https://bscscan.com/address/0xd9D7236A4e76Ec8C51c657Fc1a97977A45F7088C#tokentxns

The address overview identifies a contract on BNB Smart Chain (chain ID 56).
That does not establish a multisig identity, entity label, or association with
JCT. The Token Transfers table is rendered in the same-origin iframe
`address-tokenpage?m=light&a=0xd9D7236A4e76Ec8C51c657Fc1a97977A45F7088C`.
The parent document text alone omitted this table.

The rendered table reported one visible BEP-20 transfer event. It explicitly
said suspicious/unsafe/spam/brand-infringement tokens and zero-value transfers
were hidden by site settings. No settings were changed and no full-history
claim is supported.

One displayed example was an incoming aPriori transfer with quantity
40,000,000, block 129092491, and displayed timestamp 2026-10-22 07:47:10
(timezone not independently established). The token link identified contract
`0x299ad4299da5b2b93fba4c96967b040c7f611099`. Transaction reference:
https://bscscan.com/tx/0x9dda96989edc7fac028d2425861be35f2fc64782d727e4ed83fd33edb3945475

These are page observations only. No independent transaction receipt/log,
decimals, canonicality, historical completeness, or event-time USD price was
verified. A displayed $0.00 is not evidence that the asset or transfer has zero
economic value. Do not publish it as a verified price or valuation.

## Official API probe

A browser GET to the following official V2 endpoint, with no API key:

```text
https://api.etherscan.io/v2/api?chainid=56&module=account&action=tokentx&address=0xd9D7236A4e76Ec8C51c657Fc1a97977A45F7088C&startblock=0&endblock=999999999&page=1&offset=5&sort=desc
```

returned this body:

```json
{"status":"0","message":"NOTOK","result":"Free API access is not supported for this chain. Please upgrade your api plan for full chain coverage. https://etherscan.io/apis"}
```

The browser body extraction did not capture HTTP response status. A separate
direct CLI attempt failed before an HTTP response with curl exit 28. That
transport failure is not evidence of empty transfers.

Official documentation at https://docs.etherscan.io/supported-chains listed
BNB Smart Chain Mainnet, chain ID 56, as Paid Tier Only. It separately said
source-code and ABI endpoints are available on all chains for every plan;
that exception does not establish free address-transfer access.

No account/key or paid plan was used. Registering a free account alone is not
an unlock for the tested BSC transfer endpoint. Paid access fields, quota,
pagination, historical coverage and rights remain to be validated.

## Use conditions and decision

The official terms https://bscscan.com/terms, showing last updated October 22,
2026 when inspected, restrict automated access/scraping in section 6 and
reproduction of website/API/CSV content without consent or authorization.
They also require prior written consent for third-party applications that
interact with the service. Further automated page collection was not pursued
after reviewing these restrictions.

Do not implement page scraping, CSV harvesting, or runtime ingestion from
this review. No adapter, source approval flag, production data, or frontend
was changed. Use the official page as a human reference link.

Possible unlocks:

- Official eligible BSC API plan plus terms/authorization covering the actual
  external product, retention and derived metrics, followed by live keyed
  pagination and independent chain reconciliation.
- Independently obtain the underlying public chain events through an eligible
  BSC RPC/indexer provider. Validate its access, permitted use, historical
  window and quotas separately. Match chain, token contract, transaction hash
  and log index; obtain block timestamp and token decimals. RPC logs do not
  supply BscScan's labels or event-time USD prices, and no such alternative
  has been verified for this address in this review.

MySQL is optional downstream storage for validated events and collection
checkpoints, not a data acquisition source. No zero-budget complete-history
or production-readiness claim follows from this observation.
