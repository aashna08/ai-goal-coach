# Example “buggy” API responses (for comparison only)

These JSON files **do not** come from your running server. They illustrate **incorrect** shapes described in `BUGS.md`:

| File | Illustrates |
|------|----------------|
| `bug-01-hallucinated-on-gibberish.json` | Nonsense input but high confidence + invented goal/KRs |
| `bug-02-missing-key-results.json` | Missing `key_results` entirely |
| `bug-02-confidence-as-string.json` | `confidence_score` as a string instead of integer |
| `bug-03-extra-explanation-field.json` | Extra top-level `explanation` field |
| `bug-04-adversarial-input-hallucination.json` | SQL-injection style goal text still returns confident coaching output |
| `bug-05-profanity-input-hallucination.json` | Profanity input still returns high-confidence refined goal and KRs |

Compare them to a real response from `StandaloneGoalCoachServer` while running **`ReproBugHttpChecks`** (RestAssured; see `../README.md`).
