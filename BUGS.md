# AI Goal Coach — Simulated Bug Hunt

Below are three **realistic defect stories** (common in AI-backed APIs). They describe what *would* go wrong if guardrails and strict contracts were missing. The **current codebase is written to avoid these bugs**; JUnit tests prove the good behavior.

**If `BUGS.md` feels abstract**, use the companion folder **`reproducible-bugs/`**: plain-English READMEs per scenario, **example “buggy” JSON** files to compare against, and **shell scripts** you can run against a locally started server (`StandaloneGoalCoachServer`).

---

Each bug below includes severity, the story, and the automated test(s) that would catch a regression.

## Bug 1 — Hallucinated goals for nonsense input

- **Title**: Gibberish input returns “confident” refined goal + KRs  
- **Severity**: Critical (violates “must not hallucinate” constraint; can mislead users and create unsafe outputs)

### Repro steps

1. Send:
   - `POST /api/goal-coach`
   - body: `{"goal":"asdlkjasd qwwerty zzzzzz 9999 !!!!"}`
2. Observe response.

### Expected

- `confidence_score <= 2`
- `refined_goal == ""` (or otherwise empty)
- `key_results == []`

### Actual (buggy)

- `confidence_score` is high (e.g., 7–10)
- Non-empty `refined_goal`
- 3–5 “plausible” but invented KRs

### Tests that catch it

- `tests/src/test/java/com/example/goalcoach/GoalCoachApiContractTest.java`
  - `gibberishGetsLowConfidenceAndNoHallucination()`
- `tests/src/test/java/com/example/goalcoach/GoalCoachUiSeleniumTest.java`
  - `gibberishDoesNotHallucinateInUi()`

## Bug 2 — Contract drift: missing field or null field

- **Title**: Response sometimes omits `key_results` or returns it as `null`  
- **Severity**: High (breaks clients; downstream code often assumes arrays are present)

### Repro steps

1. Send any valid goal input, e.g. `{"goal":"Improve in sales"}`
2. Observe response intermittently missing fields (often due to error handling paths or model timeouts).

### Expected

- JSON always contains:
  - `refined_goal` as a string (possibly empty)
  - `key_results` as an array (possibly empty)
  - `confidence_score` as an integer 1–10

### Actual (buggy)

- `key_results` missing or `null`
- or `confidence_score` returned as string `"7"`

### Tests that catch it

- `tests/src/test/java/com/example/goalcoach/GoalCoachApiContractTest.java`
  - `returnsStrictSchemaAndRequestIdHeader()` (JSON Schema + type/range assertions)

## Bug 3 — Extra/unknown fields added to response (breaking strict clients)

- **Title**: API adds `explanation`/`reasoning` field without versioning  
- **Severity**: Medium–High (breaks strict deserializers; causes silent client drift)

### Repro steps

1. Deploy a new version that adds an extra field:
   - `{"refined_goal":"...","key_results":[...],"confidence_score":8,"explanation":"..."}`
2. Observe contract violation for consumers expecting strict schema.

### Expected

- Response matches the published schema only (or uses explicit versioning)

### Actual (buggy)

- Additional fields appear without a new version / compatibility plan

### Tests that catch it

- `tests/src/test/java/com/example/goalcoach/GoalCoachApiContractTest.java`
  - `doesNotAllowAdditionalFieldsInResponse()` (schema has `additionalProperties: false`)

