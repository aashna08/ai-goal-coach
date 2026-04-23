package com.example.goalcoach.testsupport;
import com.example.goalcoach.config.GoalCoachRuntimeConfig;
import org.openqa.selenium.*;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public  class CommonUtils {

    private CommonUtils() {
        // prevent instantiation
    }

    // =========================
    // 🧪 WAIT UTILS
    // =========================
    public static WebElement waitForVisibility(WebDriver driver, By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // =========================
    // ⏳ UI DELAY UTILS
    // =========================

    public static void maybeHoldOpenForObservation(WebDriver driver) {
        if (!isHeadedRealBrowser(driver)) return;

        Integer explicit = parsePositiveInt(
                GoalCoachRuntimeConfig.resolve("goalcoach.ui.holdOpenMs", "GOALCOACH_UI_HOLD_OPEN_MS", "")
        );

        int holdMs = explicit != null ? explicit : 3000;
        if (holdMs > 0) {
            sleepQuietly(holdMs);
        }
    }

    public static void maybeUiStepDelay(WebDriver driver) {
        if (!isHeadedRealBrowser(driver)) return;

        Integer explicit = parseNonNegativeInt(
                GoalCoachRuntimeConfig.resolve("goalcoach.ui.stepDelayMs", "GOALCOACH_UI_STEP_DELAY_MS", "")
        );

        int stepMs = explicit != null ? explicit : 500;
        if (stepMs > 0) {
            sleepQuietly(stepMs);
        }
    }

    // =========================
    // 🔍 DRIVER CHECK
    // =========================

    private static boolean isHeadedRealBrowser(WebDriver driver) {
        if (driver == null || driver instanceof HtmlUnitDriver) return false;

        String kind = GoalCoachRuntimeConfig
                .resolve("goalcoach.webdriver", "GOALCOACH_WEBDRIVER", "htmlunit")
                .trim().toLowerCase();

        boolean isRealBrowser =
                "chrome".equals(kind) ||
                        "chromium".equals(kind) ||
                        "edge".equals(kind) ||
                        "msedge".equals(kind);

        if (!isRealBrowser) return false;

        boolean headless = parseBoolean(
                GoalCoachRuntimeConfig.resolve("goalcoach.headless", "GOALCOACH_HEADLESS", "true")
        );

        return !headless;
    }

    // =========================
    // 🔧 HELPER METHODS
    // =========================

    public static void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean parseBoolean(String raw) {
        if (raw == null) return false;
        String v = raw.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y") || v.equals("on");
    }

    public static Integer parsePositiveInt(String raw) {
        try {
            int n = Integer.parseInt(raw.trim());
            return n > 0 ? n : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer parseNonNegativeInt(String raw) {
        try {
            int n = Integer.parseInt(raw.trim());
            return n >= 0 ? n : null;
        } catch (Exception e) {
            return null;
        }
    }
}
