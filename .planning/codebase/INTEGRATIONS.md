# External Integrations

**Analysis Date:** 2026-02-19

## APIs & External Services

**Blockchain RPC:**
- Arbitrum Mainnet
  - RPC Endpoint: `https://arb1.arbitrum.io/rpc`
  - SDK/Client: viem createPublicClient
  - Purpose: Read on-chain agent registry data

- Arbitrum Sepolia (Testnet)
  - RPC Endpoint: `https://sepolia-rollup.arbitrum.io/rpc`
  - SDK/Client: viem createPublicClient
  - Purpose: Test verification on testnet

**Implementation:** `src/verify/chain.cljs`
- `make-client` function creates viem HTTP transport clients for specified chain
- `recoverMessageAddress` recovers ECDSA signer from signature
- RPC calls made via `.readContract` for on-chain registry queries

## Data Storage

**Databases:**
- None - Application is fully client-side with no persistent backend storage

**File Storage:**
- Local filesystem only - Static build artifacts in `dist/` directory
- No cloud storage integration

**Caching:**
- None - All data is ephemeral, calculated per verification request

## Authentication & Identity

**Auth Provider:**
- Custom on-chain verification via ERC-721 NFT registry

**Implementation:** `src/verify/chain.cljs`
- No traditional authentication (API keys, OAuth, etc.)
- Identity verification via blockchain:
  1. Parse ERC-8004 agent ID: `eip155:<chainId>:<registry>:<agentId>`
  2. Recover signer address from ECDSA signature and content hash
  3. Query on-chain registry contract to get `agent.wallet` (via `getAgentWallet`)
  4. Query registry for agent NFT owner (via `ownerOf`)
  5. Verify signer matches agent wallet or owner address

**Smart Contract Interaction:**
- Registry address extracted from agent ID (ERC-721 contract)
- Agent ID is token ID in the registry
- Functions called:
  - `getAgentWallet(agentId: uint256)` → address - Returns agent's configured wallet
  - `ownerOf(agentId: uint256)` → address - Returns NFT owner (agent owner)
  - `tokenURI(agentId: uint256)` → string - Returns metadata URI (for agent name)

**Metadata Parsing:** `src/verify/chain.cljs`
- Decodes base64-encoded data URIs from `tokenURI` response
- Extracts agent name from JSON metadata

## Monitoring & Observability

**Error Tracking:**
- None - Errors displayed directly to user in UI
- Error messages propagated through state management at `src/verify/state.cljs`

**Logs:**
- Browser console only (no backend logging)
- Error information available in developer tools

## CI/CD & Deployment

**Hosting:**
- Static site hosting (production: erc8004.orbiter.website)
- No dynamic backend infrastructure

**CI Pipeline:**
- Not detected - Manual build and deployment

## Environment Configuration

**Required env vars:**
- None - Application uses hardcoded blockchain RPC endpoints

**RPC Endpoints (hardcoded in code):**
- Arbitrum Mainnet: `https://arb1.arbitrum.io/rpc` (chain ID 42161)
- Arbitrum Sepolia: `https://sepolia-rollup.arbitrum.io/rpc` (chain ID 421614)

**Secrets location:**
- No secrets used - All RPC endpoints are public

## Webhooks & Callbacks

**Incoming:**
- None - Application is purely client-side

**Outgoing:**
- None - No outbound webhooks or callbacks to external services

## Data Flow

**Verification Flow:**

1. **Parse:** User pastes signed content block or loads verify link
   - Location: `src/verify/parse.cljs`
   - Extracts signature, hash, agent ID, timestamp from text
   - Parses agent ID in format `eip155:42161:0x...registry:5`

2. **Validate:** Schema validation via zod
   - Location: `src/verify/schema.cljs`
   - Checks hex format of signature and hash
   - Validates agent ID structure and numeric fields

3. **Recover Signer:** ECDSA signature recovery
   - Location: `src/verify/chain.cljs`
   - Uses viem's `recoverMessageAddress`
   - Message is raw hash bytes (not the content text)

4. **On-Chain Lookup:** RPC calls to Arbitrum registry contract
   - Reads agent wallet address via `getAgentWallet(agentId)`
   - Reads agent owner via `ownerOf(agentId)`
   - Reads metadata URI via `tokenURI(agentId)`

5. **Compare:** Verify recovered signer matches on-chain wallet or owner
   - Case-insensitive address comparison
   - Both wallet match and owner match are reported

6. **Display:** Render verification result to user
   - Location: `src/verify/core.cljs`
   - Shows signature validity, signer address, agent identity, registry link

## URL Fragment Encoding

**Verify Links:** `src/verify/core.cljs`

Format: `https://erc8004.orbiter.website/#<base64-encoded-json>`

JSON structure in URL fragment:
```json
{
  "c": "optional content text",
  "s": "0x<signature-hex>",
  "h": "0x<content-hash-hex>",
  "a": "eip155:42161:0xregistry:agentId",
  "t": "optional ISO8601 timestamp",
  "d": "optional subject/first line",
  "n": "optional agent name"
}
```

- Content (`c`), timestamp (`t`), subject (`d`), and name (`n`) are optional
- Signature (`s`), hash (`h`), and agent ID (`a`) are required for verification
- All encoding uses UTF-8 → base64 to handle Unicode in content and agent names

---

*Integration audit: 2026-02-19*
