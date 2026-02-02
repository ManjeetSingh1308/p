package pages;

import helpers.JSHelper;
import helpers.WaitHelper;
import helpers.SliderHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class BasePage {

    protected final WebDriver driver;
    protected final WaitHelper wait;
    protected final JSHelper js;
    protected final SliderHelper slider;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitHelper(driver);
        this.js = new JSHelper(driver);
        this.slider = new SliderHelper(driver);
    }

    // ---------- Common Utilities ----------
    public void acceptCookiesIfAny() {
        List<By> candidates = Arrays.asList(
                By.id("onetrust-accept-btn-handler"),
                By.cssSelector("button[aria-label='Accept']"),
                By.xpath("//button[contains(.,'Accept')]")
        );
        for (By c : candidates) {
            try {
                List<WebElement> els = driver.findElements(c);
                if (!els.isEmpty() && els.get(0).isDisplayed()) {
                    els.get(0).click();
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    public void typeAfterLabel(String labelText, String value) {
        By inputNearLabel = By.xpath(
                "//*[self::label or self::*[self::div or self::span][contains(., '" + labelText + "')]][1]" +
                        "/following::*[self::input or self::textarea][1]"
        );
        WebElement input = wait.waitClickable(inputNearLabel, 20);
        wait.type(input, value);
    }

    public void ensureTenureUnit(String targetUnit) {
        By yrToggle = By.xpath("//*[contains(.,'Loan Tenure')]/following::*[normalize-space()='Yr'][1]");
        By moToggle = By.xpath("//*[contains(.,'Loan Tenure')]/following::*[normalize-space()='Mo'][1]");
        if ("Yr".equalsIgnoreCase(targetUnit)) {
            WebElement yr = wait.waitClickable(yrToggle, 15);
            js.clickJS(yr);
        } else {
            WebElement mo = wait.waitClickable(moToggle, 15);
            js.clickJS(mo);
        }
    }

    public void pressTab() {
        try { new Actions(driver).sendKeys(Keys.TAB).perform(); } catch (Exception ignored) {}
    }

    public WebElement findFirstPresent(List<By> locators, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            for (By by : locators) {
                try {
                    List<WebElement> els = driver.findElements(by);
                    for (WebElement el : els) {
                        if (el.isDisplayed()) return el;
                    }
                } catch (Exception ignored) {}
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    public void waitForAnyClickable(List<By> locators, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            for (By by : locators) {
                try {
                    new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(2))
                            .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(by));
                    return;
                } catch (Exception ignored) {}
            }
        }
        throw new TimeoutException("None of the expected elements became clickable.");
    }

    public void scrollIntoView(WebElement el) { js.scrollIntoView(el); }

    public void selectAllAndType(WebElement el, String value) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.DELETE);
        el.sendKeys(value);
    }
}
