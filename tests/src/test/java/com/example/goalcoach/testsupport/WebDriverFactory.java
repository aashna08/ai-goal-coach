package com.example.goalcoach.testsupport;

import com.example.goalcoach.config.GoalCoachRuntimeConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * Selects a WebDriver implementation via JVM system properties or environment variables.
 *
 * <p>Resolution order: system property, then environment variable, then {@code goalcoach.properties} on the
 * classpath (and optional external file via {@code goalcoach.config.path} / {@code GOALCOACH_CONFIG_PATH}).</p>
 *
 * <ul>
 *   <li>{@code goalcoach.webdriver} / {@code GOALCOACH_WEBDRIVER}: {@code htmlunit} (default), {@code chrome}, {@code edge}</li>
 *   <li>{@code goalcoach.headless} / {@code GOALCOACH_HEADLESS}: {@code true|false} (default {@code true} for chrome/edge; ignored for htmlunit)</li>
 *   <li>{@code goalcoach.webdrivermanager} / {@code GOALCOACH_WEBDRIVERMANAGER}: {@code true|false} (default {@code false}; optional fallback)</li>
 * </ul>
 *
 * <p>For Chrome/Edge, Selenium 4+ can resolve drivers via <b>Selenium Manager</b> in many environments.
 * If downloads are blocked by policy, set {@code goalcoach.webdrivermanager=true} to force WebDriverManager, or set
 * {@code webdriver.chrome.driver} / {@code webdriver.edge.driver} explicitly.</p>
 *
 */
public final class WebDriverFactory {
    private WebDriverFactory() {}

    public static WebDriver createConfiguredDriver() {
        //GoalCoachRuntimeConfig cfg = GoalCoachRuntimeConfig.get();
        String kind = GoalCoachRuntimeConfig.resolve("goalcoach.webdriver", "GOALCOACH_WEBDRIVER", "htmlunit").trim().toLowerCase();

        if ("htmlunit".equals(kind) || "html-unit".equals(kind) || "html".equals(kind)) {
            // HtmlUnit is always "headless" (no real browser UI). JS enabled for form submit navigation.
            return new HtmlUnitDriver(true);
        }

        boolean headless = parseBoolean(GoalCoachRuntimeConfig.resolve("goalcoach.headless", "GOALCOACH_HEADLESS", "true"));

       // boolean useWebDriverManager = parseBoolean(cfg.resolve("goalcoach.webdrivermanager", "GOALCOACH_WEBDRIVERMANAGER", "false"));

        if ("chrome".equals(kind) || "chromium".equals(kind)) {
            // setupRealBrowserDrivers("chrome", useWebDriverManager);
            ChromeOptions options = new ChromeOptions();
            applyCommonChromiumOptions(options, headless);
            return new ChromeDriver(options);
        }

        if ("edge".equals(kind) || "msedge".equals(kind)) {
            //setupRealBrowserDrivers("edge", useWebDriverManager);
            EdgeOptions options = new EdgeOptions();
            applyCommonChromiumOptions(options, headless);
            return new EdgeDriver(options);
        }

        throw new IllegalArgumentException(
                "Unknown goalcoach.webdriver/GOALCOACH_WEBDRIVER value '" + kind + "'. Expected htmlunit|chrome|edge."
        );
    }

    /*private static void setupRealBrowserDrivers(String kind, boolean useWebDriverManager) {
        if (!useWebDriverManager) {
            return;
        }
        try {
            if ("chrome".equals(kind)) {
                WebDriverManager.chromedriver().setup();
            } else if ("edge".equals(kind)) {
                WebDriverManager.edgedriver().setup();
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to initialize browser driver automatically (WebDriverManager). "
                            + "Fix network access, or set -Dwebdriver.chrome.driver / -Dwebdriver.edge.driver explicitly, "
                            + "or disable WebDriverManager (-Dgoalcoach.webdrivermanager=false) and rely on Selenium Manager.",
                    e
            );
        }
    }*/

    /**
     * Common Chromium flags for Chrome/Chromium-based Edge.
     */
    private static void applyCommonChromiumOptions(ChromiumOptions<?> options, boolean headless) {
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1400,900");
    }

    private static boolean parseBoolean(String raw) {
        if (raw == null) return false;
        String v = raw.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "y".equals(v) || "on".equals(v);
    }
}
