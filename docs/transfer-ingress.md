# Transfer ingress validation contract

Date: 2026-10-22. Implements a bounded part of R02, R17 and R22 / DA04.
This contract tightens the existing Blockscout page mapper; it does not enable
the source, persist events or establish continuous/reorg-safe coverage.

| ID | Behavior | Verification |
| --- | --- | --- |
| TX01 | Transaction hashes must be exactly 32 bytes of hex, normalized to lowercase before constructing `ethereum:{hash}:{logIndex}`. | Short/nonhex/wrong-type hashes rejected; mixed-case aliases deduplicate. |
| TX02 | `log_index`, `block_number` and token decimals accept integral JSON numbers or canonical unsigned decimal strings only. No floating values, coercion, exponent strings, booleans, missing fields or overflow. | Negative/fractional/boolean/object/oversized identity fields rejected. |
| TX03 | Public numeric identity fields are bounded by JavaScript's exact integer maximum, 9007199254740991, because the API/frontend represent them as numbers. | Boundary retained exactly; the next integer rejected rather than rounded. |
| TX04 | Token raw amount is a decimal string in [0, 2^256-1], at most 78 digits. Quantity conversion preserves precision using the verified token decimals. | Zero/mint/burn/self events and uint256 maximum accepted; negative, fractional, numeric and overflowing amounts rejected. |
| TX05 | Absent/null event time stays null. A present timestamp must be a nonblank valid ISO instant and is normalized to UTC. Observation time never substitutes for block time. | Missing remains null; malformed/wrong-type dates reject the page. |
| TX06 | Repeated canonical event IDs collapse only when all mapped fields agree. Conflicting duplicates reject the entire new page; no arbitrary first/last value is chosen. | Identical and mixed-case duplicate aliases collapse; conflicting amount/block/address/time rejected. |
| TX07 | An invalid row rejects the new page as unavailable, or preserves the cache's explicitly stale prior page. No valid subset is presented as a successfully mapped page. | Service test verifies empty unavailable response for a mixed valid/invalid page. |

Contract/token precision and monitored-address membership checks remain required.
Zero-address endpoints are valid ERC-20 mint/burn participants. A contract address
alone never proves a multisignature wallet or project/exchange attribution.

The response schema retains its current fields. `hash` and `id` now declare the
fixed hash form and numeric identity fields declare the exact integer ceiling.
The response still lacks block-hash/finality semantics and is explicitly an
observed index page. Block-hash-based reorg detection, persisted checkpoints,
full-window flow statistics and independent reconciliation remain open R22 work.
