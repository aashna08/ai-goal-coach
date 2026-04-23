# Bug 03 — Extra top-level fields (silent client drift)

## What this means (simple)

Someone deploys a “helpful” API change and adds another field, e.g. `explanation` or `reasoning`, **without** a new API version. Clients generated from a **strict** schema (`additionalProperties: false`) suddenly fail deserialization even though the new field is “harmless.”

## What “good” looks like

The JSON object has **only** these keys:

- `refined_goal`  
- `key_results`  
- `confidence_score`  

## What “bad” looks like

See [`bug-03-extra-explanation-field.json`](../example-buggy-responses/bug-03-extra-explanation-field.json).

## Reproduce the **fixed** behavior

```bash
curl -sS -X POST "$BASE_URL/api/goal-coach" \
  -H 'Content-Type: application/json' \
  -d '{"goal":"Improve public speaking"}'
```

Or run **`ReproBugHttpChecks`** as in [`../README.md`](../README.md).

## Test that catches regression

`GoalCoachApiContractTest.doesNotAllowAdditionalFieldsInResponse`
