# ERC-8004 Signature Verifier + Agent Skill Document

**One Line Intro:** Verifies ERC-8004 agent signatures on Arbitrum and teaches AI agents to do the same using only web fetch — no libraries needed.

## Problem

AI agents are becoming economic actors on-chain — trading, publishing, and executing contracts. But there's no standard way for one agent to verify that content was actually signed by the agent claiming to have signed it. Without cryptographic identity verification, agent-to-agent communication is untrustable.

Worse, existing verification requires crypto libraries (ethers, viem, web3.py) that most AI agents can't install or run. Agents are typically sandboxed to HTTP requests only. This means the fastest-growing category of on-chain participants — autonomous agents — is locked out of verifying each other's identity.

## Solution

Two components, one system:

**1. Browser-based Signature Verifier** — A client-side tool on Arbitrum that recovers the signer from an ECDSA signature and checks it against the ERC-8004 on-chain identity registry. Humans paste a signed message or click a verify link and get an instant VERIFIED/FAILED result. No server, no data leaves the browser.

**2. ERC8004-SKILL.md — A machine-readable skill document** that teaches AI agents to perform the same verification using nothing but HTTP `fetch` calls. An agent reads the document, follows a 4-step procedure (extract fields, recover signer via `eth_call` with inline EVM bytecode, query `getAgentWallet`, query `ownerOf`), and determines whether a signature is valid. Zero dependencies. No crypto libraries. No API keys. Just JSON-RPC to Arbitrum's public RPC.

The key technical innovation: since public RPCs don't expose `personal_ecRecover`, we use `eth_call` with inline Solidity creation bytecode that applies the EIP-191 message prefix and calls the ecrecover precompile as a one-off computation. This means any agent that can make an HTTP POST can verify any ERC-8004 signature on Arbitrum — the entire verification chain runs through `eth_call`.

## How It Works

**For Humans:**

1. Paste a signed content block or click a verify link
2. The verifier parses the ECDSA signature, content hash, and agent identity (`eip155:42161:<registry>:<agentId>`)
3. Recovers the signer address and reads the agent's registered wallet from the on-chain registry
4. Displays VERIFIED if the signer matches the agent wallet or NFT owner

**For AI Agents:**

1. Fetch `ERC8004-SKILL.md` from the hosted URL
2. Parse the signed block format (separator, sig, hash, agent identity)
3. Make 3 concurrent `eth_call` requests to Arbitrum's public RPC:
   - Recover signer address (inline EVM bytecode — no deployed contract needed)
   - Read agent wallet via `getAgentWallet(uint256)`
   - Read NFT owner via `ownerOf(uint256)`
4. Compare addresses: if the recovered signer matches either the agent wallet or NFT owner, the signature is verified

**Trust Forwarding:** A verifying agent can relay its result to downstream agents, creating decentralized trust chains without a central authority.

## What Makes This Different

- **Zero-dependency verification** — No ethers, no viem, no web3.py. An agent with HTTP fetch can verify signatures on Arbitrum.
- **Inline EVM bytecode for ecrecover** — Public RPCs don't support `personal_ecRecover`. We solved this by sending Solidity creation bytecode directly in `eth_call`, executing EIP-191 prefix + ecrecover as a stateless computation. No contract deployment required.
- **Machine-readable skill document** — Not documentation for developers. A structured markdown document designed for AI agents to parse and follow autonomously.
- **Fully client-side** — The browser verifier sends nothing to any server. All verification happens against Arbitrum directly.

## Tech Stack

- **Language:** ClojureScript
- **Build:** shadow-cljs (advanced compilation)
- **On-chain:** viem (Ethereum client), ERC-8004 registry on Arbitrum (42161) and Arbitrum Sepolia (421614)
- **State:** zustand, zod validation
- **Package manager:** bun
- **Hosting:** Static site (no server)

## Links

- **Live:** <https://erc8004.qstorage.quilibrium.com>
- **Skill Doc:** <https://erc8004.qstorage.quilibrium.com/ERC8004-SKILL.md>
- **Source:** <https://github.com/ngmisl/erc8004-verifier-arbitrum>
- **ERC-8004 Spec:** <https://eips.ethereum.org/EIPS/eip-8004>

**Contract Address:** `0x8004A169FB4a3325136EB29fA0ceB6D2e539a432` (Arbitrum One, chain 42161)
