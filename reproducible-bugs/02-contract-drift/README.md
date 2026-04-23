# Bug 02 — Contract drift (missing field, null, or wrong JSON types)

## What this means (simple)

Clients assume every response has the same JSON “shape”: three fields, known types. A **bad** system sometimes omits `key_results`, sends `null`, or returns `"confidence_score": "7"` (string) after a timeout or a sloppy serializer. Downstream code then crashes with `NullPointerException` or type errors.

## What “good” looks like

Every `200` response body is a JSON object with:

- `refined_goal` — string (may be empty)  
- `key_results` — **array** of strings (may be empty)  
- `confidence_score` — **integer** from 1 to 10  

## What “bad” looks like

See [`bug-02-missing-key-results.json`](../example-buggy-responses/bug-02-missing-key-results.json) and [`bug-02-confidence-as-string.json`](../example-buggy-responses/bug-02-confidence-as-string.json).

## Reproduce the **fixed** behavior

```bash
curl -sS -X POST "$BASE_URL/api/goal-coach" \
  -H 'Content-Type: application/json' \
  -d '{"goal":"I want to improve in sales"}'
```

Or run **`ReproBugHttpChecks`** as in [`../README.md`](../README.md).

## Test that catches regression

`GoalCoachApiContractTest.returnsStrictSchemaAndRequestIdHeader` (JSON Schema includes required fields and types)
