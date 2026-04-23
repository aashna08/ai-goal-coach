# AI Goal Coach — Automated Tests (Java + Selenium)

This folder contains a **self-contained** Maven project:

- A deterministic **stub** "AI Goal Coach" implementation (`src/main/java`)
- A tiny **embedded HTTP server** used by tests (`src/test/java/.../GoalCoachTestServer.java`)
- **API contract tests** (JUnit 5 + RestAssured + JSON Schema)
- **UI smoke tests** using **Selenium Java** (driver is configurable; defaults to `HtmlUnitDriver`)

For a fuller walkthrough (architecture + troubleshooting), see `../SETUP_GUIDE.md`.

For a **per-test account** of what each case checks, see `TEST_CASES.md`.

**API tests:** JUnit 5 runs the suite; **RestAssured** performs HTTP calls and assertions (`given`/`when`/`then`, Hamcrest, JSON Schema). Use both together—RestAssured is not a replacement for JUnit (lifecycle, discovery, reports). For **manual** repro against a long-lived server, see `../reproducible-bugs/README.md` and `ReproBugHttpChecks`.

## IntelliJ module roots

Open/import the **repository root** `pom.xml` in IntelliJ (multi-module Maven project). That prevents `tests/src/test/java` from being treated as “outside the module source root”.

## Prereqs

- **Java 21+** (this module uses `maven.compiler.release=21` + Enforcer)
- **Maven 3.6+** (Surefire 3.x)

Set `JAVA_HOME` to a JDK 21 install before running Maven (your machine may default to Java 8 otherwise).

## Run the test suite

From the repo root:

```bash
mvn -pl tests test
```

Or run the whole reactor:

```bash
mvn test
```

## What’s included

- **Goal coach endpoint**: `POST /api/goal-coach` with body `{"goal":"..."}` returning:
  - `refined_goal` (string)
  - `key_results` (array of strings)
  - `confidence_score` (int 1–10)
- **UI**:
  - `GET /` simple form
  - `POST /submit` renders result page (used by Selenium tests)
- **Observability header**: `X-Request-Id` is returned on responses (generated if not provided)

## Notes on determinism

The stub intentionally uses simple heuristics so that tests are stable and reproducible in CI.

## Configuration file (`goalcoach.properties`)

Committed defaults live at `src/main/resources/goalcoach.properties` (engine, Ollama URL/model placeholders, WebDriver defaults, UI timing keys).

Resolution order for each setting:

1. JVM system property (`-Dgoalcoach...=`)
2. Environment variable (`GOALCOACH_...`)
3. Properties file layer (see below)
4. Hard-coded fallback in code (where applicable)

Optional **extra** file on disk (merged **on top of** the classpath file; use for machine-local settings you do not commit):

- **Property**: `goalcoach.config.path`
- **Env var**: `GOALCOACH_CONFIG_PATH`
- Must point to a **regular file** if set (otherwise the JVM fails fast at first config access).

Loader: `src/main/java/com/example/goalcoach/config/GoalCoachRuntimeConfig.java`.

## Backend engine selection (Stub vs Ollama)

The embedded server selects an implementation via:

- **System property**: `goalcoach.engine`
- **Environment variable**: `GOALCOACH_ENGINE`
- **Properties file**: `goalcoach.engine` in `goalcoach.properties` (or external overlay)
- **Values**: `stub` (default), `ollama`

When using Ollama, set a model name:

- `GOALCOACH_OLLAMA_MODEL` (or `-Dgoalcoach.ollama.model=...`, or `goalcoach.ollama.model` in a properties file)

See `../SETUP_GUIDE.md` for full details.

## UI WebDriver configuration (HtmlUnit vs Chrome vs Edge)

Implementation: `src/test/java/com/example/goalcoach/testsupport/WebDriverFactory.java`

### Choose driver

- **System property**: `goalcoach.webdriver`
- **Environment variable**: `GOALCOACH_WEBDRIVER`
- **Properties file**: `goalcoach.webdriver` in `goalcoach.properties`
- **Values**: `htmlunit` (default), `chrome`, `edge`

### Choose headless vs headed (Chrome/Edge only)

- **System property**: `goalcoach.headless`
- **Environment variable**: `GOALCOACH_HEADLESS`
- **Properties file**: `goalcoach.headless`
- **Values**: `true|false` (default `true` for chrome/edge)

### Keep the browser visible long enough to inspect (headed Chrome/Edge)

JUnit tears down the browser quickly (`driver.quit()` in `@AfterEach`). For headed real browsers, tests pause briefly before quitting.

- **System property**: `goalcoach.ui.holdOpenMs`
- **Environment variable**: `GOALCOACH_UI_HOLD_OPEN_MS`
- **Default**: ~3000ms when `goalcoach.headless=false` (0 for headless / HtmlUnit)

There is also a small delay between UI actions:

- **System property**: `goalcoach.ui.stepDelayMs`
- **Environment variable**: `GOALCOACH_UI_STEP_DELAY_MS`
- **Default**: ~500ms when `goalcoach.headless=false` (set to `0` to disable)

### Driver download / matching (Chrome/Edge)

Selenium **4** typically resolves drivers via **Selenium Manager**.

If Selenium Manager downloads are blocked, you can force **WebDriverManager** instead:

- **System property**: `goalcoach.webdrivermanager`
- **Environment variable**: `GOALCOACH_WEBDRIVERMANAGER`
- **Values**: `true|false` (default `false`)

### Examples

```bash
# Default CI-friendly driver
mvn -pl tests test

# Watch Chrome execute the UI tests
mvn -pl tests test \
  -Dgoalcoach.webdriver=chrome \
  -Dgoalcoach.headless=false \
  -Dgoalcoach.ui.holdOpenMs=20000

# Offline / corp policy: provide chromedriver explicitly (no WebDriverManager)
mvn -pl tests test \
  -Dgoalcoach.webdriver=chrome \
  -Dgoalcoach.headless=false \
  -Dwebdriver.chrome.driver=/absolute/path/to/chromedriver
```

For full details (including driver setup on PATH / `webdriver.*.driver`), see `../SETUP_GUIDE.md`.

