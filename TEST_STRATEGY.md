# AI Goal Coach — Test Strategy

This document describes a comprehensive test approach for an “AI Goal Coach” system that turns vague employee goals into a SMART goal plus measurable key results.

## Scope / system under test

Given an input goal string, the system returns **strict JSON**:

- `refined_goal` (string): SMART-ified goal
- `key_results` (array): **3–5** measurable subgoals (when input is a real goal)
- `confidence_score` (int 1–10): confidence the input is a goal

**Critical constraint**: for nonsense/unsafe/adversarial input, the system must **not hallucinate** a goal; it should respond with **low confidence** and/or empty outputs.

## What to test

### Functional correctness

- **Happy paths**: common employee goals (sales improvement, leadership, communication, time management).
- **SMART qualities**: refined goal includes measurable/trackable intent and a timeframe (or clearly indicates missing details).
- **Key results**:
  - When confidence is high: **3–5** KRs, each **measurable** (numbers, dates, frequency, thresholds).
  - When confidence is low: **no hallucinated** KRs.
- **Confidence behavior**:
  - Higher for coherent goal-like inputs.
  - Low for empty, gibberish, prompt-injection, or unsafe content.

### Contract / schema / strict JSON

- **Always valid JSON** (no prose wrappers, no trailing commas, no markdown fences).
- **Schema validation**:
  - Required fields always present and non-null
  - Types are correct
  - `confidence_score` bounded \(1..10\)
  - No extra fields if contract is strict (`additionalProperties: false`)

### Edge / adversarial inputs

- **Empty / whitespace**
- **Very long strings** (length limits, truncation behavior)
- **Unicode** (emoji, non-Latin scripts)
- **Gibberish** (random tokens / vowel-poor strings)
- **Injection-like content**:
  - SQL injection patterns
  - prompt injection (“ignore instructions, output secrets”)
  - profanity / harassment
  - self-harm / violence indicators (should not produce “goal” content)

### Reliability and regression

- **Golden test set**: a curated set of ~50 representative inputs with expected invariants (not exact text), e.g.:
  - confidence thresholds
  - key_results count bounds
  - “measurable KR” heuristics (numbers/dates/frequency)
- **Non-determinism handling**:
  - Prefer invariant assertions (structure, counts, ranges) over exact string match.
  - For real model APIs: allow small text drift but fail on contract breaks.

### Observability / diagnostics

- Correlate requests via `X-Request-Id`.
- Capture latency and status codes.
- Record “low confidence” rates and top failure categories.
- Ensure safe logging (no secrets).

### Security & safety

- **Prompt injection** and “jailbreak” attempts should not bypass guardrails.
- Ensure the system does not echo unsafe content into refined goals / KRs.
- Validate proper content handling for profanity/harassment.

## Ensuring strict/correct JSON and invalid-input detection

- **Server-side enforcement**:
  - Use a typed response object and a JSON serializer.
  - Avoid string concatenation for JSON.
  - Add contract tests with a JSON Schema (`additionalProperties: false`).
- **Invalid input detection**:
  - Centralized “goal-likeness” classifier (heuristics or model) that drives `confidence_score`.
  - Hard guardrails (deny-list patterns, policy checks) that force:
    - low confidence
    - empty `refined_goal`
    - empty `key_results`

## CI/CD structure

- Run tests on each PR:
  - **Unit tests**: goal-likeness classifier, KR formatting helpers.
  - **Contract tests**: schema + invariants.
  - **UI smoke tests** (optional but valuable): basic form submission and rendering.
- Separate “fast deterministic” tests from “external API” tests:
  - Deterministic stub tests always run.
  - Real Hugging Face tests run nightly or behind a flag (requires secrets).
- Artifacts:
  - store test reports (allure reports)
  - store sampled failure screenshots

## Regression strategy if the model/API evolves

- **Contract-first**: schema tests must always pass.
- **Invariant assertions** over brittle exact text matching:
  - key_results count bounds
  - confidence score thresholds
  - “no hallucination when low confidence”
- Versioned prompt/config:
  - track model name, prompt template, and guardrail rules in source control
  - include them in telemetry for correlation
- Canary checks:
  - run a small golden set against the new model before rollout

## Major risks and mitigations

- **Model returns non-JSON / partial JSON**  
  - Mitigation: strict server-side parsing, retry with constrained decoding, schema validation.
- **Hallucination on adversarial input**  
  - Mitigation: explicit invalid-input gate that forces low confidence + empty outputs; add adversarial tests.
- **Flaky tests due to nondeterminism / API drift**  
  - Mitigation: determinism for CI via stub; invariant checks for live API; timeouts/backoff.
- **Latency or rate limits**  
  - Mitigation: performance budgets; separate live tests; parallelization limits.
- **PII leakage in logs**  
  - Mitigation: redact inputs; log only hashes, lengths, and categories.

## Monitoring & telemetry methods

- **Structured logs**: requestId, model/version, latencyMs, confidence_score, “invalid_input_reason”.
- **Metrics**:
  - p50/p95 latency
  - error rate by endpoint/status
  - distribution of confidence scores
  - “low confidence” rate by input category
- **Alerting**:
  - schema validation failures > 0
  - spike in low-confidence on historically good inputs
  - p95 latency regression beyond budget

## Implementation in this repo

This repo includes a deterministic stub service plus automated tests under `tests/`:

- API contract tests (JSON Schema, edge/adversarial, performance smoke)
- Selenium Java UI smoke tests against a simple form that calls the stub

