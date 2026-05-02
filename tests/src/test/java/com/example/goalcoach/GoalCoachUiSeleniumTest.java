package com.example.goalcoach;

import com.example.goalcoach.config.GoalCoachRuntimeConfig;
import com.example.goalcoach.testsupport.CommonUtils;
import com.example.goalcoach.testsupport.TestServerConfig;
import com.example.goalcoach.testsupport.WebDriverFactory;
import org.apache.hc.core5.reactor.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import io.qameta.allure.*;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoalCoachUiSeleniumTest extends TestServerConfig {
    private WebDriver driver;

    @AfterEach
    void teardown() {
        try {
           CommonUtils.maybeHoldOpenForObservation(driver);
        } finally {
            if (driver != null) driver.quit();
        }
    }

    @Test()
    void happyPathSalesGoalRendersResultPage() {
        System.out.println("******happyPathSalesGoalRendersResultPage()*************");
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        WebElement goal = CommonUtils.waitForVisibility(driver, By.id("goal"), 10);
        goal.sendKeys("I want to improve in sales");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("confidence"), 10);
        String confidence = driver.findElement(By.id("confidence")).getText().trim();
        String refined = driver.findElement(By.id("refinedGoal")).getText().trim();
        WebElement keyResults = driver.findElement(By.id("keyResults"));
        int conf = Integer.parseInt(confidence);
        if (isStubEngine()) {
            assertTrue(conf >= 6, "Expected confidence >= 6, got " + conf);
            assertFalse(refined.trim().isEmpty(), "Expected refined goal to be non-empty");
            assertTrue(keyResults.getText().length() > 10, "Expected key results to render");
        } else {
            assertTrue(conf >= 1 && conf <= 10, "Expected confidence in 1..10, got " + conf);
            if (conf >= 6) {
                assertFalse(refined.trim().isEmpty(), "Expected refined goal when confidence >= 6");
                assertTrue(keyResults.getText().length() > 10, "Expected key results when confidence >= 6");
            }
        }
    }

    private static boolean isStubEngine() {
        String e = GoalCoachRuntimeConfig.resolve("goalcoach.engine", "GOALCOACH_ENGINE", "stub").trim().toLowerCase();
        return e.isEmpty() || "stub".equals(e);
    }

    @Test
    void gibberishDoesNotHallucinateInUi() {
        System.out.println("******gibberishDoesNotHallucinateInUi()*************");
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);
        driver.findElement(By.id("goal")).sendKeys("asdlkjasd qwwerty zzzzzz 9999 !!!!");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("confidence"), 10);
        int conf = Integer.parseInt(driver.findElement(By.id("confidence")).getText().trim());
        String refined = driver.findElement(By.id("refinedGoal")).getText().trim();

        assertTrue(conf <= 2, "Expected confidence <= 2, got " + conf);
        assertTrue(refined.trim().isEmpty(), "Expected refined goal to be blank for gibberish");
    }

    @Test
    void homePageShowsGoalForm() {
        System.out.println("******homePageShowsGoalForm()()*************");
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.tagName("h1"), 10);
        assertTrue(driver.findElement(By.tagName("h1")).getText().contains("AI Goal Coach"));
        WebElement goalField = driver.findElement(By.id("goal"));
        assertEquals("textarea", goalField.getTagName().toLowerCase());
        assertNotNull(driver.findElement(By.id("submit")));
        assertTrue(driver.findElement(By.cssSelector("form[action='/submit']")).isDisplayed());
    }

    @Test
    void emptyGoalViaUiShowsLowConfidence() {
        System.out.println("******homePageShowsGoalForm()()*************");
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);
        WebElement goalField = driver.findElement(By.id("goal"));
        goalField.clear();
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("confidence"), 10);
        int conf = Integer.parseInt(driver.findElement(By.id("confidence")).getText().trim());
        assertTrue(conf <= 2, "Expected confidence <= 2 for empty goal, got " + conf);
        assertTrue(driver.findElement(By.id("refinedGoal")).getText().trim().isEmpty());
        List<WebElement> items = driver.findElements(By.cssSelector("#keyResults li.kr"));
        assertEquals(0, items.size(), "Expected no key results for empty goal");
    }

    @Test
    void backLinkFromResultReturnsToHomeForm() {
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);
        driver.findElement(By.id("goal")).sendKeys("I want to learn public speaking");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("back"), 10);
        assertTrue(driver.findElement(By.id("back")).isDisplayed());
        driver.findElement(By.id("back")).click();
        CommonUtils.maybeUiStepDelay(driver);

        String url = driver.getCurrentUrl();
        assertTrue(
                url.equals(baseUrl() + "/") || url.equals(baseUrl()),
                "Expected home URL after Back, got: " + url
        );
        driver.findElement(By.id("goal"));
        driver.findElement(By.id("submit"));
    }

    @Test
    void adversarialSqlInjectionViaUiGetsLowConfidence() {
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);
        driver.findElement(By.id("goal")).sendKeys("I want to improve in sales; DROP TABLE users; --");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("confidence"), 10);
        int conf = Integer.parseInt(driver.findElement(By.id("confidence")).getText().trim());
        assertTrue(conf <= 2, "Expected confidence <= 2, got " + conf);
        assertTrue(driver.findElement(By.id("refinedGoal")).getText().trim().isEmpty());
    }

    @Test
    void profanityViaUiGetsLowConfidence() {
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);
        driver.findElement(By.id("goal")).sendKeys("I want to be a better manager you asshole");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("confidence"), 10);
        int conf = Integer.parseInt(driver.findElement(By.id("confidence")).getText().trim());
        assertTrue(conf <= 2, "Expected confidence <= 2, got " + conf);
        assertTrue(driver.findElement(By.id("refinedGoal")).getText().trim().isEmpty());
    }


    /*void htmlInUserGoalIsEscapedInRenderedOutput() {
        Assumptions.assumeTrue(
                isStubEngine(),
                () -> "Markup escaping is validated against deterministic stub output"
        );
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);

        driver.findElement(By.id("goal")).sendKeys("Ship Q1 roadmap <script>alert(1)</script> for the team");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.maybeUiStepDelay(driver);

        String inner = driver.findElement(By.id("refinedGoal")).getAttribute("innerHTML");
        assertNotNull(inner);
        assertFalse(
                inner.toLowerCase().contains("<script"),
                "Refined goal HTML must not contain a raw script tag; use escaped text"
        );
        List<WebElement> krs = driver.findElements(By.cssSelector("#keyResults li.kr"));
        for (WebElement li : krs) {
            String krInner = li.getAttribute("innerHTML");
            assertFalse(
                    krInner != null && krInner.toLowerCase().contains("<script"),
                    "Key result HTML must not contain a raw script tag"
            );
        }
    }*/

    @Test
    void stubHappyPathShowsExpectedKeyResultListSize() {
        Assumptions.assumeTrue(isStubEngine(), () -> "KR list size is asserted for stub semantics");
        driver = WebDriverFactory.createConfiguredDriver();
        driver.get(baseUrl() + "/");
        CommonUtils.waitForVisibility(driver, By.id("goal"), 10);

        driver.findElement(By.id("goal")).sendKeys("I want to improve in sales");
        CommonUtils.maybeUiStepDelay(driver);
        driver.findElement(By.id("submit")).click();
        CommonUtils.waitForVisibility(driver, By.id("keyResults"), 10);
        List<WebElement> items = driver.findElements(By.cssSelector("#keyResults li.kr"));
        assertTrue(items.size() >= 3 && items.size() <= 5, "Expected 3-5 key result rows, got " + items.size());
    }

}

