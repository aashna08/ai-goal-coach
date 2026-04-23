# What the automated tests cover

This document maps each automated test to the behavior it checks. For strategy (why we test this way, CI posture, risks), see `../TEST_STRATEGY.md`. For intentional defect examples tied to tests, see `../BUGS.md`.

## System under test

All tests start an **in-process HTTP server** (`GoalCoachTestServer`) on a random localhost port. The coach logic behind `POST /api/goal-coach` is either:

- **`stub`** (default): deterministic Java implementation (`GoalCoachStub` / `StubGoalCoachEngine`), or  
- **`ollama`**: local LLM via `OllamaGoalCoachEngine` (same HTTP contract; slower and nondeterministic).

Shared **input guardrails** (`GoalInputSafety`) run before the stub or the model for obvious nonsense, profanity markers, and unsafe patterns.

Endpoints exercised:

| Method | Path | Role in tests |
|--------|------|----------------|
| `POST` | `/api/goal-coach` | JSON API contract and guardrails (`GoalCoachApiContractTest`) |
| `GET` | `/` | HTML form (`GoalCoachUiSeleniumTest`) |
| `POST` | `/submit` | Form post → HTML result page (`GoalCoachUiSeleniumTest`) |

Every response is expected to include a non-empty **`X-Request-Id`** header (generated if the client did not send one).

## JSON contract (all API responses)

Responses must match the JSON Schema in `src/test/resources/schema/goal_coach_response.schema.json`:

- **`refined_goal`**: string  
- **`key_results`**: array of strings, length 0–5  
- **`confidence_score`**: integer 1–10  
- **`additionalProperties`**: false (no extra top-level fields)

Several tests use this schema explicitly; others assert fields that are compatible with it.

---

## `GoalCoachApiContractTest` (REST / JSON)

| Test | What it verifies |
|------|------------------|
| **`returnsStrictSchemaAndRequestIdHeader`** | Happy-path JSON: HTTP 200, JSON content type, response matches the strict schema, `refined_goal` and `key_results` present, `confidence_score` in 1–10, and **`X-Request-Id`** is set. Uses goal: *“I want to improve in sales”*. |
| **`confidenceHighMeansNonEmptyRefinedGoalAnd3To5KeyResults`** | For the same sales goal, a **high-confidence** outcome: `confidence_score` ≥ 6, non-empty `refined_goal`, and **3–5** key results (matches product expectation that strong inputs get structured output). |
| **`emptyGoalGetsLowConfidenceAndNoHallucinatedOutputs`** | Whitespace-only goal: low confidence (≤ 2), empty `refined_goal`, **no** key results (size 0). Ensures the API does not invent SMART text from nothing. |
| **`adversarialSqlInjectionGetsLowConfidenceAndNoHallucination`** | Goal string containing SQL-like / injection-style markers: treated as unsafe; low confidence (≤ 2), empty refined goal, no key results. Ensures the service does not “helpfully” complete the attack narrative. |
| **`profanityGetsLowConfidenceAndNoHallucination`** | Goal containing configured profanity markers: low confidence (≤ 2), empty refined goal, no key results. |
| **`gibberishGetsLowConfidenceAndNoHallucination`** | Keyboard-mash style input: low confidence (≤ 2), empty refined goal, no key results (anti-hallucination on nonsense). |
| **`doesNotAllowAdditionalFieldsInResponse`** | Another coherent goal (*“Improve public speaking”*): response still matches the **strict** schema (`additionalProperties: false`). Catches accidental extra fields in the JSON serializer or handler. |
| **`basicPerformanceP95Under250msForStub`** | Runs **50** sales-goal requests, measures latency, and asserts **p95 &lt; 250 ms**. **Only runs when `goalcoach.engine` is `stub`** (skipped for Ollama or any other engine), because local LLM latency is not comparable to the stub. |

---

## `GoalCoachUiSeleniumTest` (browser / HTML)

Driver is chosen via `WebDriverFactory` (default **HtmlUnit**; optional Chrome/Edge). Tests exercise the **form on `/`** and the **result page** from **`POST /submit`**, reading DOM ids: `goal`, `submit`, `confidence`, `refinedGoal`, `keyResults`.

| Test | What it verifies |
|------|------------------|
| **`happyPathSalesGoalRendersResultPage`** | User flow: open home page, enter a normal sales-improvement goal, submit, land on result page with parseable confidence. **Stub engine**: confidence ≥ 6, non-empty refined goal, key results list has substantial text. **Non-stub (e.g. Ollama)**: confidence must stay in **1–10**; if the model reports confidence ≥ 6, refined goal and key results must be non-trivial (same bar as stub for that branch). Lower confidence is allowed without failing the test, because local LLMs can be conservative while the page remains valid. |
| **`gibberishDoesNotHallucinateInUi`** | Same flow through the **HTML UI** with gibberish input: confidence ≤ 2 and **blank** refined goal on the rendered page (aligned with API guardrails; with `ollama`, early exit usually avoids calling the model). |
| **`homePageShowsGoalForm`** | `GET /` renders the main heading, a `textarea#goal`, submit control, and a form that posts to `/submit`. |
| **`emptyGoalViaUiShowsLowConfidence`** | Submit with an empty goal field: confidence ≤ 2, blank refined goal, and no `li.kr` key-result rows. |
| **`backLinkFromResultReturnsToHomeForm`** | After a successful submit, `#back` is visible; clicking it returns to the home URL and the form controls are present again. |
| **`adversarialSqlInjectionViaUiGetsLowConfidence`** | SQL-injection-style goal text in the UI: confidence ≤ 2 and blank refined goal (same guardrail story as the API test). |
| **`profanityViaUiGetsLowConfidence`** | Profanity in the goal field: confidence ≤ 2 and blank refined goal. |
| **`htmlInUserGoalIsEscapedInRenderedOutput`** | **Stub only** (skipped otherwise): goal contains a literal `<script>` snippet; the rendered `#refinedGoal` and each key result’s `innerHTML` must not contain a raw `<script` substring (server-side HTML escaping). |
| **`stubHappyPathShowsExpectedKeyResultListSize`** | **Stub only**: sales happy path produces **3–5** `li.kr` elements under `#keyResults`. |

---

## Stub vs Ollama: what stays strict

| Area | Stub (default CI) | Ollama / other engines |
|------|-------------------|-------------------------|
| Schema + headers on API | Same expectations | Same expectations |
| Guardrail cases (empty, SQL-ish, profanity, gibberish) | Same | Same (many short-circuit in `GoalInputSafety` before HTTP to Ollama) |
| “High confidence ⇒ filled fields” on **API** | Sales goal must hit ≥ 6, etc. | Same assertion on **API** (may be flaky if a model misbehaves; increase timeout or model quality if needed) |
| **p95 &lt; 250 ms** | Enforced | Test **skipped** |
| **UI** happy path strictness | Strict (≥ 6, text present) | Relaxed as described above |

---

## Where to run and configure

- Commands and WebDriver options: `README.md`  
- Full setup, Ollama env vars, and config files: `../SETUP_GUIDE.md`  
- Default properties: `src/main/resources/goalcoach.properties`
