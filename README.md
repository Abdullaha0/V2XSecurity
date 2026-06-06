# V2X Sybil Resistance Simulation

A Java simulation of a lightweight Sybil attack mitigation mechanism for Vehicle-to-Everything (V2X) communication systems. The mechanism combines SHA-256 Proof-of-Work (PoW) token mining with a time-windowed rate-control layer to limit how many pseudonymous identities a single vehicle can activate within a given interval.

This repository contains the implementation accompanying the paper published at FiCloud 2026.

---

## Citation

If you find this code helpful, we recommend that you read and cite our paper:

> Fayyad, Maher; Awad, Abdullah; Roque, A.D.S.; de Morais, W. O. and De Freitas, E.P., 2026, August. Mitigating Sybil Attacks in V2X Communication through Cryptographic Trust Anchors. In *2026 13th International Conference on Future Internet of Things and Cloud (FiCloud)*, Granada, Spain. IEEE.

---

## Overview

In V2X networks, vehicles use temporary pseudonymous identities to protect driver privacy. This opens the door for Sybil attacks, where a single attacker creates multiple fake identities to inject false information into the network.

The proposed mechanism defends against this by requiring each identity token to be obtained through a SHA-256 Proof-of-Work puzzle, and by enforcing a hard cap on accepted tokens per time window. An attacker attempting to flood the network must solve multiple puzzles within the same window, multiplying the required computational effort. The mechanism is infrastructure-free and requires no RSUs or centralized authorities.

---

## How It Works

Each vehicle must mine a valid PoW token before it can activate a pseudonymous identity. Mining means finding a nonce such that:

```
SHA-256(vehicleId : windowId : nonce)
```

starts with `DIFFICULTY` leading hexadecimal zeros. Once mined, the token is submitted to the rate-control layer, which enforces the following sequential checks:

1. **Hash & freshness check** — is the hash valid and does it belong to the current time window?
2. **Replay check** — has this exact token been submitted before?
3. **Rate limit check** — has the window's acceptance cap been reached?

A token is accepted only if it passes all three checks.

---

## Architecture

The system consists of two layers:

**Cryptographic Layer** (`cryptoLayer.java`)  
Implements SHA-256 mining and verification. Mining searches for a valid nonce; verification requires only a single hash computation (O(1)), keeping it cheap for all nodes.

**Rate-Control Layer** (`rateControlLayer.java`)  
Enforces a hard upper bound on accepted tokens per time window. Tokens are bound to their originating window, so stale tokens and replays are automatically rejected.

---

## Project Structure

```
├── config.java             # Simulation parameters
├── token.java              # Data class for a solved PoW token
├── cryptoLayer.java        # SHA-256 mining and verification
├── rateControlLayer.java   # Token admission and rate enforcement
├── vehicle.java            # Vehicle thread (legitimate or Sybil)
└── sim.java                # Main entry point and summary report
```

---

## Configuration

All parameters are set in `config.java`:

| Parameter               | Default | Description                                        |
|-------------------------|---------|----------------------------------------------------|
| `DIFFICULTY`            | `4`     | Number of leading zeros required in the hash       |
| `WINDOW_SECONDS`        | `10`    | Duration of each time window (seconds)             |
| `MAX_TOKENS_PER_WINDOW` | `5`     | Max tokens accepted across all vehicles per window |
| `NUM_VEHICLES`          | `8`     | Total number of vehicles in the simulation         |
| `NUM_SYBIL`             | `3`     | Number of Sybil attackers                          |
| `SIM_DURATION`          | `30`    | Total simulation time (seconds)                    |
| `TOKEN_INTERVAL`        | `2`     | Seconds between each vehicle's token attempt       |

Sybil vehicles attempt four times more frequently than legitimate vehicles to model a realistic attack.

---

## How to Run

**Requirements:** Java 11 or later

```bash
# Compile
javac *.java

# Run
java Sim
```

---

## Output

The simulation prints live events during execution, followed by a summary report.

**Live output:**
```
  [legit] V-04          hash=0000a3f7b2c1e509…  nonce=  8431  mine=0.012s  → ACCEPTED
  [SYBIL] SYBIL-01      hash=00003d8af1b72c40…  nonce= 21053  mine=0.031s  → RATE_LIMIT_EXCEEDED
```

**Summary report:**
```
======================================================================
  PER-VEHICLE SUMMARY
======================================================================
  Vehicle       Type     Attempts  Accepted  Rejected  Avg Mine(s)
  ----------------------------------------------------------------
  SYBIL-01      SYBIL          12         3         9        0.028
  V-04          legit           5         5         0        0.014
  ...
======================================================================
  RATE-CONTROL WINDOW SUMMARY
======================================================================
  Window [12:00:00]  accepted=  5  rejected= 18  total= 23
======================================================================
  OVERALL STATISTICS
======================================================================
  Avg PoW mining time   : 0.0210s
  Sybil tokens accepted : 8
  Legit tokens accepted : 22
  Total rejected        : 31
  Rate-limit efficiency : 30/45 slots used across 3 window(s)
======================================================================
```

---

## Rejection Reasons

| Reason                  | Description                                               |
|-------------------------|-----------------------------------------------------------|
| `ACCEPTED`              | Token passed all checks and was admitted                  |
| `INVALID_HASH_OR_STALE` | Hash is incorrect or token belongs to a past time window  |
| `REPLAY_DETECTED`       | This token has already been submitted                     |
| `RATE_LIMIT_EXCEEDED`   | The window's acceptance cap has been reached              |
