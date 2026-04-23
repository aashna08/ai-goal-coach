# AI Goal Coach — What We Built + Setup Guide

This repository contains an **automated test harness** for an “AI Goal Coach” system: it validates **JSON contracts**, **guardrails** (no hallucination on nonsense/adversarial input), and a **minimal HTML UI flow** using **Selenium Java**.

The “AI” itself is implemented as a **deterministic stub** (not a live LLM call) so tests are **stable in CI** and do not require API keys.

## What we implemented

### 1) Deterministic Goal Coach stub (production-like behavior)

- **Class**: `tests/src/main/java/com/example/goalcoach/service/GoalCoachStub.java`
- **Behavior**:
  - For coherent goal-like inputs: returns a SMART-ified `refined_goal`, `key_results`, and a higher `confidence_score`.
  - For empty/whitespace, gibberish heuristics, profanity markers, or “unsafe/injection-like” markers: returns **low confidence** and **does not fabricate** goals/KRs (empty `refined_goal`, empty `key_results`).

This stub is what we treat as the “system under test” for automation.

### 2) Lightweight HTTP server used only by tests

- **Class**: `tests/src/test/java/com/example/goalcoach/testsupport/GoalCoachTestServer.java`
- **Endpoints**:
  - `POST /api/goal-coach` (JSON in/out)
  - `GET /` (HTML form)
  - `POST /submit` (form post → HTML result page)
- **Observability**: responses include `X-Request-Id` (generated if missing).

The server starts **in-process** during tests (random localhost port).

### 3) Automated tests

#### API / contract tests

- **File**: `tests/src/test/java/com/example/goalcoach/GoalCoachApiContractTest.java`
- **Covers**:
  - JSON Schema validation (`additionalProperties: false`) using `tests/src/test/resources/schema/goal_coach_response.schema.json`
  - Functional “happy path” invariants (confidence bounds, KR count bounds)
  - Adversarial cases (SQL-ish injection string, profanity, gibberish, empty input)
  - Optional lightweight latency smoke (p95 threshold on the stub)

#### UI tests (Selenium Java)

- **File**: `tests/src/test/java/com/example/goalcoach/GoalCoachUiSeleniumTest.java`
- **Driver selection**: `tests/src/test/java/com/example/goalcoach/testsupport/WebDriverFactory.java`
  - Defaults to **`HtmlUnitDriver`** (embedded engine; no Chrome/Edge install required)
  - Can be switched to **Chrome** or **Edge** via config (see below)

### 4) Written deliverables (strategy + bug hunt)

- `TEST_STRATEGY.md`: what/why/how to test, CI posture, model drift strategy, risks, telemetry
- `BUGS.md`: three simulated bugs + reproduction + which tests catch them  
- `reproducible-bugs/`: plain-English walkthroughs, example “buggy” JSON, and **Java + RestAssured** manual checks (`ReproBugHttpChecks`) against `StandaloneGoalCoachServer`

## Repository layout (relevant parts)

- `pom.xml`: Maven **parent aggregator** (imports cleanly into IntelliJ as a multi-module project)
- `app/pom.xml`: small placeholder app module (keeps the repo as a standard Maven layout)
- `tests/pom.xml`: Maven module for the stub + tests
- `tests/src/main/java/...`: stub + engine implementations + shared config loader
- `tests/src/main/resources/goalcoach.properties`: default settings (WebDriver, engine, Ollama placeholders)
- `tests/src/test/java/...`: tests + embedded server
- `tests/README.md`: quick commands (mirrors this guide, scoped to `tests/`)

## Prerequisites

- **JDK**: Java **21+** (`tests/pom.xml` sets `maven.compiler.release=21` and Enforcer requires `[21,)`)
- **Maven**: **3.6+** recommended (Surefire 3.x)

Verify versions:

```bash
java -version
mvn -version
```

On macOS, if `java -version` still shows Java 8, point `JAVA_HOME` at JDK 21 before running Maven:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## Setup (first time)

1. Clone the repository (or use your existing workspace checkout).
2. From the repository root, download dependencies and compile tests:

```bash
mvn -pl tests -q test -DskipTests
```

## Run the full automated suite

From the repository root:

```bash
mvn test
```

Or run only the test module:

```bash
mvn -pl tests test
```

Expected result: **BUILD SUCCESS** with **10 tests** (8 API + 2 UI).

## IntelliJ import note (fixes “outside module source root”)

Import/open the **root** `pom.xml` as a Maven project. IntelliJ should create modules:

- `dummyproject-parent` (root)
- `dummyproject-app` (`app/`)
- `ai-goal-coach-tests` (`tests/`)

Then `tests/src/test/java` is recognized as **Test Sources** automatically.

If IntelliJ still shows stale roots: **Maven tool window → Reload All Maven Projects**.

## Configure Selenium WebDriver + headless/headed mode

The UI tests read configuration from **JVM system properties** first, then **environment variables**.

### Driver selection

- **Property**: `goalcoach.webdriver`
- **Env var**: `GOALCOACH_WEBDRIVER`
- **Values**:
  - `htmlunit` (**default**)
  - `chrome`
  - `edge`

### Headless vs visible browser (Chrome/Edge only)

- **Property**: `goalcoach.headless`
- **Env var**: `GOALCOACH_HEADLESS`
- **Values**: `true|false` (also accepts `1/0`, `yes/no`, `on/off`)
- **Default**: `true` for `chrome`/`edge` (headless), **ignored** for `htmlunit` (no real browser UI)

### UI observation pause (headed Chrome/Edge only)

When `goalcoach.headless=false`, the UI tests **sleep briefly before `driver.quit()`** so you can see the final page.

- **Property**: `goalcoach.ui.holdOpenMs`
- **Env var**: `GOALCOACH_UI_HOLD_OPEN_MS`
- **Values**: positive integer milliseconds
- **Default**: `3000` when headed + real browser; `0` otherwise

Additionally, the UI tests insert a small delay **between actions** (navigate/type/click) so the flow is visible:

- **Property**: `goalcoach.ui.stepDelayMs`
- **Env var**: `GOALCOACH_UI_STEP_DELAY_MS`
- **Values**: non-negative integer milliseconds (`0` disables)
- **Default**: `500` when headed + real browser; `0` otherwise

### Examples

**Default (HtmlUnit, best for CI):**

```bash
mvn -pl tests test
```

**Chrome headless:**

```bash
mvn -pl tests test \
  -Dgoalcoach.webdriver=chrome \
  -Dgoalcoach.headless=true
```

**Chrome headed (watch the browser):**

```bash
mvn -pl tests test \
  -Dgoalcoach.webdriver=chrome \
  -Dgoalcoach.headless=false
```

**Note (why the browser disappears immediately):** JUnit ends each test quickly and the suite calls `driver.quit()` in `@AfterEach`.
For local observation, the UI tests now **pause briefly** before quitting in **headed real browsers** (not HtmlUnit), by default ~**3 seconds**.

Override the pause:

```bash
mvn -pl tests test \
  -Dgoalcoach.webdriver=chrome \
  -Dgoalcoach.headless=false \
  -Dgoalcoach.ui.holdOpenMs=20000
```

**Edge headless:**

```bash
mvn -pl tests test \
  -Dgoalcoach.webdriver=edge \
  -Dgoalcoach.headless=true
```

**Using environment variables instead:**

```bash
export GOALCOACH_WEBDRIVER=chrome
export GOALCOACH_HEADLESS=false
mvn -pl tests test
```

### Driver/browser requirements (Chrome/Edge)

This module uses **Selenium 4**, which can usually resolve drivers via **Selenium Manager**.

**Optional fallback:** if Selenium Manager downloads are blocked, force **WebDriverManager**:

- `-Dgoalcoach.webdrivermanager=true` (or env `GOALCOACH_WEBDRIVERMANAGER=true`)

If both are blocked/offline, provide a driver binary explicitly:

- JVM properties:
  - `-Dwebdriver.chrome.driver=/absolute/path/to/chromedriver`
  - `-Dwebdriver.edge.driver=/absolute/path/to/msedgedriver`

### Why HtmlUnit remains the default

For this UI (simple form + server-rendered HTML), HtmlUnit is usually enough and avoids operational failures from missing browsers/drivers in CI.

If you need pixel-perfect rendering or heavy client-side JavaScript, prefer **Chrome/Edge** (often headed locally, headless in CI).

## Configuration files (`goalcoach.properties`)

Defaults ship in `tests/src/main/resources/goalcoach.properties`. At runtime, values resolve in this order:

1. JVM system properties (`-Dgoalcoach...=`)
2. Environment variables (`GOALCOACH_...` where applicable)
3. Properties file layer: classpath `goalcoach.properties`, then an optional **external overlay** merged on top
4. Code fallbacks (only when a key is still unset)

**External overlay (optional, not committed):**

- **Property**: `goalcoach.config.path`
- **Env var**: `GOALCOACH_CONFIG_PATH`
- Value: absolute path to a `.properties` file. If this variable/property is set, the file **must** exist (fail-fast).

Implementation: `tests/src/main/java/com/example/goalcoach/config/GoalCoachRuntimeConfig.java`.

## Optional: Ollama (local LLM) backend

The embedded server (`GoalCoachTestServer`) can call a local Ollama instance instead of the deterministic stub.

### Enable Ollama

- **Property**: `goalcoach.engine`
- **Env var**: `GOALCOACH_ENGINE`
- **Values**: `stub` (default) or `ollama`

### Ollama settings

- **Base URL**
  - **Property**: `goalcoach.ollama.baseUrl`
  - **Env var**: `GOALCOACH_OLLAMA_BASE_URL`
  - **Default**: `http://localhost:11434`
- **Model**
  - **Property**: `goalcoach.ollama.model`
  - **Env var**: `GOALCOACH_OLLAMA_MODEL`
  - **Required** for meaningful output (example: `llama3.1`)
- **Timeouts**
  - `goalcoach.ollama.connectTimeoutMs` / `GOALCOACH_OLLAMA_CONNECT_TIMEOUT_MS` (default `2000`)
  - `goalcoach.ollama.requestTimeoutMs` / `GOALCOACH_OLLAMA_REQUEST_TIMEOUT_MS` (default `300000` — many local models, especially “thinking” variants, routinely exceed 60s)

### Example (manual smoke)

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"

# Terminal 1: run Ollama locally (outside this repo)
#   ollama serve
#   ollama pull llama3.1

# Terminal 2: run tests against Ollama
export GOALCOACH_ENGINE=ollama
export GOALCOACH_OLLAMA_MODEL=llama3.1
mvn -pl tests test
```

### Implementation notes

- Java adapter: `tests/src/main/java/com/example/goalcoach/engine/OllamaGoalCoachEngine.java`
- Engine selection: `tests/src/main/java/com/example/goalcoach/engine/GoalCoachEngines.java`
- Shared guardrails (pre-model): `tests/src/main/java/com/example/goalcoach/safety/GoalInputSafety.java`

## Optional: testing a real public model API (Hugging Face)

This repo’s default tests intentionally **do not** call Hugging Face (keys, rate limits, nondeterminism).

If you want to extend it:

- Add a separate test profile/class that calls the HF endpoint with `Authorization: Bearer ...`
- Keep **fast deterministic stub tests** on every PR; run **live API tests** nightly or behind a flag

## Troubleshooting

### Maven can’t download dependencies

If you’re behind a corporate mirror, you may need to configure `~/.m2/settings.xml` repositories mirrors/credentials. If a dependency resolution failure was cached, retry:

```bash
mvn -pl tests -U test
```

### Wrong Java version picked up by Maven

Ensure `JAVA_HOME` points to the JDK you intend:

```bash
echo $JAVA_HOME
which java
```

### UI tests fail in environments that block HtmlUnit networking

Rare, but possible in locked-down environments. In that case, switch the UI tests to Chrome/Edge (Selenium 4) or run UI tests only on agents with browsers installed.

### Chrome/Edge UI tests fail: “webdriver.chrome.driver must be set …”

This usually means **Selenium Manager couldn’t provision a driver** (offline / blocked) **and** no explicit `webdriver.*.driver` property was provided.

- **Fix (recommended):** ensure the machine can access Selenium Manager’s downloads, or set `-Dwebdriver.chrome.driver=...` / `-Dwebdriver.edge.driver=...` explicitly.
- **Fix (policy networks):** `-Dgoalcoach.webdrivermanager=true` to force WebDriverManager (if your network allows *its* downloads), or vendor the driver binary into the repo/artifact store and point `webdriver.*.driver` at it.

### Chrome/Edge UI tests fail with “cannot find Chrome binary” / driver errors

- Ensure the browser is installed.
- Ensure the matching driver is installed and discoverable (`PATH` or `webdriver.*.driver` system properties).
- If you’re on Linux CI, headed mode (`GOALCOACH_HEADLESS=false`) usually needs a display server (often not available); use headless in CI and headed locally.
