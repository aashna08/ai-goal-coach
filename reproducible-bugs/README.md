# Reproducible bug scenarios (training set)

`BUGS.md` in the repo root describes realistic defect stories. They are **not** live defects in the current code: the implementation is written to **avoid** them, and automated tests **prove** the correct behavior.

This folder exists so you can:

1. **Understand in plain language** what would go wrong in production if the guardrails disappeared.  
2. **See concrete “bad” JSON** under `example-buggy-responses/` (what a broken API might return).  
3. **Run the coach HTTP server locally** and run **Java + RestAssured** checks (`ReproBugHttpChecks`) that assert the **fixed** (correct) behavior today — same stack as `GoalCoachApiContractTest`.

## Quick mental model

| ID | Plain English | What “broken” looks like | What we check today |
|----|-----------------|---------------------------|----------------------|
| **01** | Nonsense goal, but the “AI” still invents SMART goals | High confidence + long `refined_goal` + fake KRs | Low confidence + empty fields |
| **02** | API shape drifts: missing array or wrong types | `key_results` missing/`null`, or `confidence_score` as a string | JSON Schema + `confidence_score` is an `Integer` |
| **03** | API adds surprise fields (e.g. `explanation`) | Extra top-level keys | Schema `additionalProperties: false` |
| **04** | Adversarial goal text still gets confident output | SQL-injection style payload produces non-empty goal/KRs | Low confidence + empty fields for adversarial text |

## Run the server, then RestAssured checks

From the repository root, with **Java 21** and **Maven**:

**Terminal 1 — start embedded coach (stub by default; same engine config as tests)**

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"

mvn -q -pl tests exec:java -Dexec.classpathScope=test \
  -Dexec.mainClass=com.example.goalcoach.repro.StandaloneGoalCoachServer
```

**Terminal 2 — HTTP checks (RestAssured, no Python)**

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"

mvn -q -pl tests exec:java -Dexec.classpathScope=test \
  -Dexec.mainClass=com.example.goalcoach.repro.ReproBugHttpChecks \
  -Drepro.baseUrl=http://127.0.0.1:PORT
```

Replace `PORT` with the value printed in terminal 1. You can use **`REPRO_BASE_URL`** instead of **`-Drepro.baseUrl`** if you prefer.

Stop the server with **Enter** in terminal 1.

## Per-scenario docs

- [01 — Gibberish hallucination](01-gibberish-hallucination/README.md)  
- [02 — Contract drift (missing / null / wrong types)](02-contract-drift/README.md)  
- [03 — Extra unknown fields](03-extra-fields/README.md)  
- [04 — Adversarial input hallucination](04-adversarial-input-hallucination/README.md)  

## Automated tests (source of truth)

The same scenarios are locked in JUnit + RestAssured:

- `tests/src/test/java/com/example/goalcoach/GoalCoachApiContractTest.java`  
- `tests/src/test/java/com/example/goalcoach/GoalCoachUiSeleniumTest.java`  

Details stay in `BUGS.md`; this folder is the **hands-on** companion.

## JUnit vs RestAssured for API tests

**Use both:** JUnit 5 is the **runner** (`@Test`, lifecycle, CI reporting). **RestAssured** is the **HTTP client and assertion DSL** (`given` / `when` / `then`, JSON body matchers, JSON Schema). The contract tests already follow that pattern; replacing JUnit with “only RestAssured” would lose standard test discovery and lifecycle hooks. For **manual** repro without Surefire, `ReproBugHttpChecks` uses RestAssured from a `main` method.
