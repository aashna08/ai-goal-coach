# Bug 01 — Hallucinated goals for nonsense input

## What this means (simple)

A user types random keys (`asdlkjasd qwwerty…`). A **bad** system still answers as if it were a real career goal: invented SMART text and high confidence. That misleads users and looks “smart” while being **made up**.

## What “good” looks like

- `confidence_score` is **low** (here: ≤ 2).  
- `refined_goal` is **empty**.  
- `key_results` is an **empty array**.

## What “bad” looks like

Open [`../example-buggy-responses/bug-01-hallucinated-on-gibberish.json`](../example-buggy-responses/bug-01-hallucinated-on-gibberish.json) — high confidence plus invented goal and KRs.

## Reproduce the **fixed** behavior (curl)

With `BASE_URL` pointing at a running `StandaloneGoalCoachServer`:

```bash
curl -sS -X POST "$BASE_URL/api/goal-coach" \
  -H 'Content-Type: application/json' \
  -d '{"goal":"asdlkjasd qwwerty zzzzzz 9999 !!!!"}'
```

You should see JSON matching the good rules above.

Or run **`ReproBugHttpChecks`** (RestAssured) as in [`../README.md`](../README.md) — terminal 2 command.

## Test that catches regression

`GoalCoachApiContractTest.gibberishGetsLowConfidenceAndNoHallucination`  
`GoalCoachUiSeleniumTest.gibberishDoesNotHallucinateInUi`
