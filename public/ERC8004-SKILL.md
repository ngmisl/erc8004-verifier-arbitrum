# ERC8004-SKILL: Agent Signing & Verification

AI agents sign and verify ERC-8004 content on Arbitrum using only `curl`. No crypto libraries required.

## Agent Identity

Format: `eip155:<chainId>:<registryAddress>:<agentId>`
Example: `eip155:42161:0x8004b3A873394d8B0Af8fD5D9C5D5a432:5`

Components: `eip155` (literal), `chainId` (decimal), `registryAddress` (0x hex, 20 bytes), `agentId` (decimal NFT token ID).

## RPC Endpoints

| chainId | Network | RPC |
|---------|---------|-----|
| 42161 | Arbitrum One | `https://arb1.arbitrum.io/rpc` |
| 421614 | Arbitrum Sepolia | `https://sepolia-rollup.arbitrum.io/rpc` |

## Signed Block Format

```
<content>

---
Signed by <Name> (<fullId>)
sig: 0x<130 hex chars, 65-byte ECDSA>
hash: 0x<64 hex chars, EIP-191 hash>
ts: <ISO 8601>           [optional]
Verify: <URL>            [optional]
```

Parser anchors on the `Signed by` line, then finds the nearest `---` above it (skipping blanks). Content = everything before the separator. Footer field order is conventional, not enforced.

**hash is pre-computed.** Pass it directly to ecRecover. Do not recompute from content.

## Signing

Tool: `sign_content`. Input: raw content text only (no labels/commentary/formatting). Output: complete signed block ready to display.

**Display rules:** Present as a single code block. No preamble or commentary. Do not modify the footer. Keep the Verify link intact.

## Verification

Produces three addresses, then compares. Steps 2-4 are independent and may run concurrently.

**Use `curl` via bash for all RPC calls.** Web fetch tools commonly fail 400 on JSON-RPC.

### Step 1: Extract

From the signed block, extract: `hash`, `sig`, `chainId`, `registryAddress`, `agentId`.

### Step 2: Recover Signer

Split the 65-byte `sig` (130 hex chars after `0x`):

| Part | Chars (0-indexed from `0x`) | Size |
|------|----------------------------|------|
| `r` | sig[2..66] | 32 bytes |
| `s` | sig[66..130] | 32 bytes |
| `v` | sig[130..132] | 1 byte |

Build `data` field by concatenating (no spaces):
1. `0x`
2. Fixed bytecode (below)
3. `hash` without `0x` (64 hex)
4. `v` left-padded to 64 hex
5. `r` (64 hex)
6. `s` (64 hex)

**Fixed bytecode** (EIP-191 prefix + ecrecover, same for every verification):
```
608060405234801561001057600080fd5b5060405161012b38038061012b83398101604081905261002f916100e6565b6040517f19457468657265756d205369676e6564204d6573736167653a0a3332000000006020820152603c8101859052600090605c0160408051601f198184030181528282528051602091820120600080855291840180845281905260ff88169284019290925260608301869052608083018590529092509060019060a0016020604051602081039080840390855afa1580156100d0573d6000803e3d6000fd5b5050506020604051035190508060005260206000f35b600080600080608085870312156100fc57600080fd5b84519350602085015160ff8116811461011457600080fd5b604086015160609096015194979096509250505056fe
```

```bash
curl -s -X POST <RPC> -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"eth_call","params":[{"data":"0x<bytecode><hash><v-padded><r><s>"},"latest"]}'
```

Result: recovered signer address (left-padded to 32 bytes).

### Step 3: Read Agent Wallet

Selector: `getAgentWallet(uint256)` = `0x00339509`

```bash
curl -s -X POST <RPC> -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"eth_call","params":[{"to":"<registryAddress>","data":"0x00339509<agentId padded to 64 hex>"},"latest"]}'
```

ABI encoding: convert `agentId` (decimal) to hex, left-pad with zeros to 64 chars.

Example: agentId=5 -> `0000000000000000000000000000000000000000000000000000000000000005`

Result: agent wallet address (left-padded to 32 bytes).

### Step 4: Read NFT Owner

Selector: `ownerOf(uint256)` = `0x6352211e`

```bash
curl -s -X POST <RPC> -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"eth_call","params":[{"to":"<registryAddress>","data":"0x6352211e<agentId padded to 64 hex>"},"latest"]}'
```

Result: NFT owner address (left-padded to 32 bytes).

### Step 5: Compare

Lowercase all three addresses, then:

```
verified = (signer == wallet) OR (signer == owner)
```

Both the agent wallet and NFT owner are authorized signers.

## Error Conditions

| Condition | Action |
|-----------|--------|
| RPC call failed | Report: unable to verify (network error). Do not report as unverified. |
| Signer matches neither address | Report: NOT VERIFIED. Signature not from an authorized key. |
| Zero/empty address returned | Report: agent identity not found in registry. |

## Trust Forwarding

A verifying agent may relay results downstream by stating: outcome (verified/not verified/error), the full agent ID, and the content hash. Include the verifying agent's own identity. Downstream trust is based on the relayer, not the relay format.