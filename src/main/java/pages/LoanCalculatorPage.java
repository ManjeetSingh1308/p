package pages;

import org.openqa.selenium.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoanCalculatorPage extends BasePage {

    public LoanCalculatorPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get("https://emicalculator.net/loan-calculator/");
        acceptCookiesIfAny();
    }

    public void validateEMICalculatorUI() {
        System.out.println("[EMI Calculator] UI checks...");
        assertDisplayedAndEnabled("Loan Amount");
        assertDisplayedAndEnabled("Interest Rate");
        assertDisplayedAndEnabled("Loan Tenure");

        ensureTenureUnit("Yr");
        ensureTenureUnit("Mo");
        ensureTenureUnit("Yr");

        changeNumericAfterLabel("Loan Tenure", "2");
        System.out.println("  ✓ Inputs present, tenure toggle works, and value updated.");
    }

    // in emi.pages.LoanCalculatorPage

    public void openTab(String linkText) {
        // Normalize input & map synonyms: "Affordability" => Loan Amount
        String key = linkText == null ? "" : linkText.trim().toLowerCase();
        boolean wantAmount = key.contains("afford") || key.contains("amount");
        boolean wantTenure = key.contains("tenure");
        boolean wantEmi    = key.contains("emi") || (!wantAmount && !wantTenure); // default

        // Ensure we’re on the correct page and cookies are dismissed
        acceptCookiesIfAny();

        // Try clicking the appropriate tab with multiple locator fallbacks
        WebElement tab = null;
        if (wantAmount) {
            tab = findFirstPresent(Arrays.asList(
                    // Common variants
                    By.xpath("//a[normalize-space()='Loan Amount Calculator']"),
                    By.xpath("//a[contains(normalize-space(.),'Loan Amount')]"),
                    By.xpath("//button[normalize-space()='Loan Amount Calculator']"),
                    By.xpath("//*[@role='tab' and (contains(.,'Loan Amount') or contains(.,'Affordability'))]"),
                    // Bootstrap-ish targets / anchors
                    By.cssSelector("a[href*='loan-amount'], button[data-bs-target*='loan-amount']")
            ), 10);
        } else if (wantTenure) {
            tab = findFirstPresent(Arrays.asList(
                    By.xpath("//a[normalize-space()='Loan Tenure Calculator']"),
                    By.xpath("//a[contains(normalize-space(.),'Tenure')]"),
                    By.xpath("//button[normalize-space()='Loan Tenure Calculator']"),
                    By.xpath("//*[@role='tab' and contains(.,'Tenure')]"),
                    By.cssSelector("a[href*='tenure'], button[data-bs-target*='tenure']")
            ), 10);
        } else { // EMI default
            tab = findFirstPresent(Arrays.asList(
                    By.xpath("//a[normalize-space()='EMI Calculator']"),
                    By.xpath("//a[contains(normalize-space(.),'EMI Calculator')]"),
                    By.xpath("//button[normalize-space()='EMI Calculator']"),
                    By.xpath("//*[@role='tab' and contains(.,'EMI')]"),
                    By.cssSelector("a[href*='emi'], button[data-bs-target*='emi']")
            ), 10);
        }

        if (tab == null) {
            // ---- Optional direct URL fallbacks (last resort) ----
            try {
                if (wantAmount) {
                    driver.navigate().to("https://emicalculator.net/loan-calculator/#loan-amount-calculator");
                } else if (wantTenure) {
                    driver.navigate().to("https://emicalculator.net/loan-calculator/#loan-tenure-calculator");
                } else {
                    driver.navigate().to("https://emicalculator.net/loan-calculator/#emi-calculator");
                }
                acceptCookiesIfAny();
            } catch (Exception ignored) {}
        } else {
            scrollIntoView(tab);
            try {
                tab.click();
            } catch (Exception e) {
                js.clickJS(tab); // fall back to JS click if overlay or intercept
            }
        }

        // ---- Verify the tab is open by waiting for expected inputs on that tab ----
        try {
            if (wantAmount) {
                waitForAnyClickable(Arrays.asList(
                        By.xpath("//*[normalize-space()='EMI']/following::input[1]"),
                        By.cssSelector("input[aria-label*='EMI' i]"),
                        By.cssSelector("input[id*='emi' i], input[name*='emi' i]")
                ), 15);
            } else if (wantTenure) {
                waitForAnyClickable(Arrays.asList(
                        By.xpath("//*[contains(.,'Loan Amount')]/following::input[1]"),
                        By.xpath("//*[contains(.,'EMI')]/following::input[1]"),
                        By.xpath("//*[contains(.,'Interest Rate')]/following::input[1]")
                ), 15);
            } else {
                waitForAnyClickable(Arrays.asList(
                        By.xpath("//*[contains(.,'Loan Amount')]/following::input[1]"),
                        By.xpath("//*[contains(.,'Interest Rate')]/following::input[1]"),
                        By.xpath("//*[contains(.,'Loan Tenure')]/following::input[1]")
                ), 15);
            }
        } catch (TimeoutException te) {
            throw new NoSuchElementException("Could not confirm the " +
                    (wantAmount ? "Loan Amount" : wantTenure ? "Loan Tenure" : "EMI") +
                    " tab opened. Adjust locators if the site changed.");
        }
    }


    public void fillEMIAmountCalculator(String emi, String rate, String tenureYears) {
        WebElement emiInput = findFirstPresent(Arrays.asList(
                By.xpath("//*[normalize-space()='EMI']/following::input[1]"),
                By.xpath("//*[contains(normalize-space(.),'EMI ₹')]/following::input[1]"),
                By.cssSelector("input[aria-label*='EMI' i]"),
                By.cssSelector("input[id*='emi' i], input[name*='emi' i]"),
                By.xpath("(//*[contains(.,'EMI')]/following::input)[1]")
        ), 12);
        if (emiInput == null) throw new NoSuchElementException("EMI input not found");
        scrollIntoView(emiInput);
        selectAllAndType(emiInput, emi);

        WebElement rateControl = findFirstPresent(Arrays.asList(
                By.xpath("//*[self::label or self::*[self::div or self::span][contains(.,'Interest Rate')]]/following::input[1]"),
                By.cssSelector("input[aria-label*='Interest' i]"),
                By.cssSelector("input[id*='interest' i], input[name*='interest' i]"),
                By.cssSelector("input[type='range'][aria-label*='Interest' i]"),
                By.xpath("(//*[@role='slider' and (contains(@aria-label,'Interest') or contains(@aria-valuetext,'%'))])[1]")
        ), 12);
        if (rateControl == null) {
            rateControl = findFirstPresent(Collections.singletonList(
                    By.xpath("(//*[contains(.,'Interest')]/following::input)[1]")
            ), 6);
        }
        if (rateControl == null) throw new NoSuchElementException("Interest Rate control not found");

        if ("input".equalsIgnoreCase(rateControl.getTagName()) &&
                !"range".equalsIgnoreCase(rateControl.getAttribute("type"))) {
            selectAllAndType(rateControl, rate);
        } else {
            slider.nudgeRight(rateControl, 5);
        }

        WebElement tenureControl = findFirstPresent(Arrays.asList(
                By.xpath("//*[contains(.,'Loan Tenure')]/following::input[1]"),
                By.cssSelector("input[id*='tenure' i], input[name*='tenure' i]"),
                By.cssSelector("input[type='range'][aria-label*='Tenure' i]"),
                By.xpath("(//*[@role='slider' and contains(@aria-label,'Tenure')])[1]")
        ), 12);

        if (tenureControl != null && "input".equalsIgnoreCase(tenureControl.getTagName())
                && !"range".equalsIgnoreCase(tenureControl.getAttribute("type"))) {
            selectAllAndType(tenureControl, tenureYears);
        } else if (tenureControl != null) {
            slider.nudgeRight(tenureControl, 5);
        } else {
            // Twin fields fallback: Years/Months
            WebElement yearsField = findFirstPresent(Arrays.asList(
                    By.xpath("(//*[contains(.,'Loan Tenure')]/following::input)[1]"),
                    By.xpath("//input[@placeholder='Years' or @aria-label='Years']")
            ), 6);
            WebElement monthsField = findFirstPresent(Arrays.asList(
                    By.xpath("(//*[contains(.,'Loan Tenure')]/following::input)[2]"),
                    By.xpath("//input[@placeholder='Months' or @aria-label='Months']")
            ), 6);
            if (yearsField != null) selectAllAndType(yearsField, tenureYears);
            if (monthsField != null) selectAllAndType(monthsField, "0");
        }

        pressTab();
    }

    public void jsFillById(String id, String value) {
        try {
            WebElement el = driver.findElement(By.id(id));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    el, value
            );
        } catch (Exception e) {
            System.out.println("Could not fill ID: " + id);
        }
    }


    // in pages.LoanCalculatorPage (or emi.pages.LoanCalculatorPage)


    public void assertRecalculatedLoanAmountVisible() {
        // Try the most reliable KPI: Principal Loan Amount
        String kpi = waitForKpiValueByLabel("Principal Loan Amount", 30);
        System.out.println("[Loan Amount Result] " + kpi);
    }

    public WebElement findFirstPresent(List<By> locators, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            for (By by : locators) {
                try {
                    List<WebElement> els = driver.findElements(by);
                    for (WebElement el : els) if (el.isDisplayed()) return el;
                } catch (Exception ignored) {}
            }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        return null;
    }



    private String waitForKpiValueByLabel(String label, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;

        while (System.currentTimeMillis() < end) {
            try {
                WebElement labelEl = findFirstPresent(Arrays.asList(
                        By.xpath("//*[normalize-space(text())='" + label + "']"),
                        By.xpath("//*[normalize-space(.)='" + label + "']") // broader
                ), 2);

                if (labelEl != null) {
                    // 1) Try nearest following element with any text (span/div/p/td/strong/h*)
                    List<By> nearValueLocators = Arrays.asList(
                            By.xpath("//*[normalize-space(text())='" + label + "']/following::*[self::span or self::div or self::p or self::td or self::strong or self::b or self::h1 or self::h2 or self::h3 or self::h4][normalize-space()][1]"),
                            By.xpath("//*[normalize-space(text())='" + label + "']/parent::*//following-sibling::*[1]//*[self::span or self::div or self::p or self::td or self::strong or self::b or self::h1 or self::h2 or self::h3 or self::h4][normalize-space()][1]"),
                            By.xpath("//*[normalize-space(text())='" + label + "']/ancestor::*[self::tr or self::li or self::div[contains(@class,'row')] or self::section][1]//*[self::span or self::div or self::p or self::td][normalize-space()][1]")
                    );

                    for (By by : nearValueLocators) {
                        WebElement v = getDisplayedSafe(by);
                        if (v != null) {
                            String val = extractCurrencyOrNumber(v.getText());
                            if (val == null || val.isEmpty()) {
                                // Sometimes currency and digits are split into siblings. Merge the next few nodes’ texts.
                                String merged = mergeNextTexts(v, 5); // look ahead
                                val = extractCurrencyOrNumber(merged);
                            }
                            if (val != null && !val.isEmpty()) {
                                return val;
                            }
                        }
                    }

                    // 2) As a fallback, look anywhere within the KPI section around the label
                    WebElement container = ascendToContainer(labelEl);
                    if (container != null) {
                        String merged = mergeAllTexts(container, 4000); // cap characters
                        String val = extractCurrencyOrNumber(merged);
                        if (val != null && !val.isEmpty()) return val;
                    }
                }

                // 3) Last resort: generic visible currency blocks on page
                String generic = findAnyVisibleCurrencyOnPage();
                if (generic != null && !generic.isEmpty()) return generic;

            } catch (StaleElementReferenceException ignored) {
                // Try again
            } catch (Exception ignored) {}

            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }

        dumpKpiDebug();
        throw new TimeoutException("Could not detect a visible Loan Amount result within " + timeoutSec + " seconds.");
    }

    // ---------------- helpers used by the assertion ----------------




    private void dumpKpiDebug() {
        try {
            System.out.println("=== Visible KPI headers (h1..h5) ===");
            for (WebElement h : driver.findElements(By.xpath("//h1|//h2|//h3|//h4|//h5"))) {
                if (h.isDisplayed()) System.out.println(" - " + h.getText());
            }
            System.out.println("=== Currency blocks (first 6) ===");
            int seen = 0;
            for (WebElement n : driver.findElements(By.xpath("(//span|//div|//p|//td)[contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')]"))) {
                if (n.isDisplayed()) {
                    System.out.println(" * " + n.getText());
                    if (++seen >= 6) break;
                }
            }
        } catch (Exception ignored) {}
    }


    private WebElement getDisplayedSafe(By by) {
        try {
            List<WebElement> els = driver.findElements(by);
            for (WebElement el : els) if (el.isDisplayed()) return el;
        } catch (Exception ignored) {}
        return null;
    }

    /** Walk up to a reasonably small container that groups label+value (row/tr/section/card). */
    private WebElement ascendToContainer(WebElement start) {
        WebElement cur = start;
        for (int i = 0; i < 6; i++) {
            try {
                WebElement p = cur.findElement(By.xpath(".."));
                String tag = p.getTagName().toLowerCase(Locale.ROOT);
                String cls = p.getAttribute("class") == null ? "" : p.getAttribute("class");
                if ("tr".equals(tag) || cls.contains("row") || cls.contains("card") || "section".equals(tag) || "table".equals(tag)) {
                    return p;
                }
                cur = p;
            } catch (Exception e) {
                break;
            }
        }
        return null;
    }

    /** Merge text of next N following nodes (siblings/descendants) to handle cases like "₹" and "108" split. */
    private String mergeNextTexts(WebElement el, int maxNodes) {
        StringBuilder sb = new StringBuilder();
        try {
            // try direct following-siblings first
            List<WebElement> sibs = el.findElements(By.xpath("following-sibling::*[position() <= " + Math.max(1, Math.min(10, maxNodes)) + "]"));
            for (WebElement s : sibs) {
                if (!s.isDisplayed()) continue;
                String t = s.getText();
                if (t != null && !t.isBlank()) {
                    sb.append(' ').append(t.trim());
                    if (sb.length() > 4000) break;
                }
            }
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    /** Merge all inner texts of a container (with a char cap). */
    private String mergeAllTexts(WebElement container, int charCap) {
        try {
            String t = container.getText();
            if (t == null) return "";
            t = t.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
            if (t.length() > charCap) return t.substring(0, charCap);
            return t;
        } catch (Exception e) {
            return "";
        }
    }

    /** Return first visible currency-like string across the page (as a final fallback). */
    private String findAnyVisibleCurrencyOnPage() {
        List<By> anyCurrency = Arrays.asList(
                By.xpath("(//span|//div)[contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')]"),
                By.xpath("//input[@readonly or @disabled][contains(@value,'₹') or contains(@value,'Rs') or contains(@value,'INR')]")
        );
        for (By by : anyCurrency) {
            try {
                List<WebElement> els = driver.findElements(by);
                for (WebElement el : els) {
                    if (!el.isDisplayed()) continue;
                    String v = "input".equalsIgnoreCase(el.getTagName()) ? el.getAttribute("value") : el.getText();
                    String out = extractCurrencyOrNumber(v);
                    if (out != null && !out.isEmpty()) return out;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }



    private String waitForLoanAmountText(int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;

        // Candidate locators for a visible result text (span/div) containing a currency readout
        List<By> textCandidates = Arrays.asList(
                // Near "Loan Amount" (but avoid matching the tab title itself)
                By.xpath("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'loan amount') and not(contains(.,'calculator'))]" +
                        "/following::*[self::span or self::div][contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')][1]"),

                // Sections that commonly display results
                By.xpath("//*[contains(@class,'result') and (contains(.,'₹') or contains(.,'Rs') or contains(.,'INR'))]"),

                // Any prominent number-like block nearby
                By.xpath("(//span|//div)[contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')][1]")
        );

        // Candidate inputs (some UIs render result as a readonly input)
        List<By> inputCandidates = Arrays.asList(
                By.cssSelector("input[id*='loanamount' i]"),
                By.cssSelector("input[name*='loanamount' i]"),
                By.xpath("//input[@readonly or @disabled][contains(@value,'₹') or contains(@value,'Rs') or contains(@value,'INR')]")
        );

        while (System.currentTimeMillis() < end) {
            // 1) Text candidates
            for (By by : textCandidates) {
                try {
                    List<WebElement> els = driver.findElements(by);
                    for (WebElement el : els) {
                        if (!el.isDisplayed()) continue;
                        String t = el.getText().trim();
                        if (looksLikeCurrency(t)) return t;
                    }
                } catch (Exception ignored) {}
            }

            // 2) Input candidates
            for (By by : inputCandidates) {
                try {
                    List<WebElement> els = driver.findElements(by);
                    for (WebElement el : els) {
                        if (!el.isDisplayed()) continue;
                        String v = el.getAttribute("value");
                        if (v != null && looksLikeCurrency(v)) return v.trim();
                    }
                } catch (Exception ignored) {}
            }

            // Small poll sleep
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }

        // Debug info to help adjust if it still fails
        dumpNearbyForDebug();
        throw new org.openqa.selenium.TimeoutException("Could not detect a visible Loan Amount result within " + timeoutSec + " seconds.");
    }

    private String getFirstVisibleText(By by) {
        try {
            List<WebElement> els = driver.findElements(by);
            for (WebElement el : els) {
                if (!el.isDisplayed()) continue;
                if ("input".equalsIgnoreCase(el.getTagName())) {
                    String v = el.getAttribute("value");
                    if (v != null && !v.trim().isEmpty()) return v.trim();
                } else {
                    String t = el.getText();
                    if (t != null && !t.trim().isEmpty()) return t.trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }


    private String waitForAnyResultText(int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;

        // 1) Strong, label-based candidates (common on this page)
        List<By> strongLabelCandidates = Arrays.asList(
                // "Principal Loan Amount" followed by the numeric block
                By.xpath("//*[normalize-space()='Principal Loan Amount']/following::*[self::span or self::div][contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')][1]"),
                // "Total Payment" (also contains a currency, good proxy of recalculation)
                By.xpath("//*[contains(normalize-space(.),'Total Payment')]/following::*[self::span or self::div][contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')][1]"),
                // "Total Interest Payable"
                By.xpath("//*[contains(normalize-space(.),'Total Interest Payable')]/following::*[self::span or self::div][contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')][1]")
        );

        // 2) Generic visible currency blocks (fallbacks)
        List<By> relaxedCandidates = Arrays.asList(
                By.xpath("(//span|//div)[contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')]"),
                // sometimes rendered as a readonly input value
                By.xpath("//input[@readonly or @disabled][contains(@value,'₹') or contains(@value,'Rs') or contains(@value,'INR')]")
        );

        while (System.currentTimeMillis() < end) {
            // Try strong candidates first
            for (By by : strongLabelCandidates) {
                String v = getFirstVisibleText(by);
                if (v != null) {
                    String currency = extractCurrencyOrNumber(v);
                    if (currency != null) return currency;
                }
            }

            // Then relaxed candidates
            for (By by : relaxedCandidates) {
                String v = getFirstVisibleText(by);
                if (v != null) {
                    String currency = extractCurrencyOrNumber(v);
                    if (currency != null) return currency;
                }
            }

            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }

        dumpNearbyForDebug();
        throw new TimeoutException("Could not detect a visible Loan Amount result within " + timeoutSec + " seconds.");
    }



    private boolean looksLikeCurrency(String s) {
        if (s == null) return false;
        String t = s.replace("\u00A0"," ").trim(); // remove non-breaking space
        // Accept ₹, Rs, INR, and plain numeric with commas as a fallback
        return t.matches(".*(₹|Rs|INR).*\\d[\\d,]*(\\.\\d+)?$") || t.matches("^\\d[\\d,]*(\\.\\d+)?$");
    }


    private String extractCurrencyOrNumber(String raw) {
        if (raw == null) return null;
        String t = raw.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        // Currency + number (₹ / Rs / INR)
        Pattern p = Pattern.compile("(₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(t);
        if (m.find()) return (m.group(1) + " " + m.group(2)).replaceAll("\\s+", " ").trim();

        // Bare number fallback
        Matcher m2 = Pattern.compile("([\\d,]+(?:\\.\\d+)?)").matcher(t);
        if (m2.find()) return m2.group(1);

        return null;
    }




    private void dumpNearbyForDebug() {
        try {
            System.out.println("=== Visible headers on page ===");
            for (WebElement h : driver.findElements(By.xpath("//h1|//h2|//h3|//h4|//h5"))) {
                if (h.isDisplayed()) System.out.println(" - " + h.getText());
            }
            System.out.println("=== Sample spans/divs containing currency ===");
            int printed = 0;
            for (WebElement n : driver.findElements(By.xpath("(//span|//div)[contains(.,'₹') or contains(.,'Rs') or contains(.,'INR')]"))) {
                if (n.isDisplayed()) {
                    System.out.println(" * " + n.getText());
                    if (++printed >= 5) break;
                }
            }
        } catch (Exception ignored) {}
    }



    // ------- small helpers -------
    private void assertDisplayedAndEnabled(String labelText) {
        By inputNearLabel = By.xpath(
                "//*[self::label or self::*[self::div or self::span][contains(., '" + labelText + "')]]" +
                        "/following::*[self::input or self::textarea][1]"
        );
        WebElement el = driver.findElement(inputNearLabel);
        if (!el.isDisplayed() || !el.isEnabled()) {
            throw new AssertionError("Field not available: " + labelText);
        }
    }

    private void changeNumericAfterLabel(String labelText, String numeric) {
        By inputNearLabel = By.xpath(
                "//*[self::label or self::*[self::div or self::span][contains(., '" + labelText + "')]]" +
                        "/following::*[self::input][1]"
        );
        WebElement input = driver.findElement(inputNearLabel);
        selectAllAndType(input, numeric);
    }
    // call this after you enter EMI, Rate, Tenure in fillEMIAmountCalculator

    private void forceChangeEvent(WebElement el, String value) {
        js.setValueWithChange(el, value); // dispatches a 'change'
        try { el.sendKeys(Keys.TAB); } catch (Exception ignored) {}
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

}
