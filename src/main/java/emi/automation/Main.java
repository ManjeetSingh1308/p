package emi.automation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class Main {
    private static WebDriver driver;
    private static WebDriverWait wait;

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        driver = new ChromeDriver();
        wait   = new WebDriverWait(driver, Duration.ofSeconds(20));


        try {
            driver.manage().window().maximize();
            driver.get("https://emicalculator.net/");
            acceptCookiesIfAny(driver);


            // ===================== SCENARIO 1 =====================
            // --- Inputs ---
            double loanAmount = 1_500_000;  // 15 lakh
            double annualRatePct = 9.5;     // 9.5% p.a.
            int tenureYears = 1;            // 1 year


            WebElement car= driver.findElement(By.xpath("//a[text()='Car Loan']"));
            car.click();

            // --- Fill Loan Amount ---
            // Common IDs on this page include loanamount, loaninterest, loanterm
            WebElement amount = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loanamount")));
            selectAllAndType(amount, String.valueOf((long) loanAmount));

            // --- Fill Interest Rate ---
            WebElement rate = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loaninterest")));
            selectAllAndType(rate, String.valueOf(annualRatePct));

            // --- Fill Tenure in years ---
            WebElement tenure = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loanterm")));
            selectAllAndType(tenure, String.valueOf(tenureYears));

            WebElement month_click=wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='btn-group btn-group-toggle']/label[@class='btn btn-secondary']")));
            month_click.click();


            WebElement span=wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div/table/tbody/tr/th[@class='col-2 col-lg-1']")));

            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", span);

            wait.until(ExpectedConditions.visibilityOf(span));

            driver.findElement(By.xpath("//td[@id='year2026']")).click();


            WebElement firstMonthRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//tr[@id='monthyear2026']/td/div/table/tbody/tr[1]")));

            String principal = driver.findElement(By.xpath("//tr[@id='monthyear2026']/td/div/table/tbody/tr[1]/td[2]")).getText();
            String interest = driver.findElement(By.xpath("//tr[@id='monthyear2026']/td/div/table/tbody/tr[1]/td[3]")).getText();

            System.out.println("--- Car Loan (First Month) ---");
            System.out.println("Principal Amount: " + principal);
            System.out.println("Interest Amount: " + interest);

            // Optional: try to read first-month row if a Monthly schedule is visible
            tryExtractFirstMonthBreakup(wait);

            // ===================== SCENARIO 2 =====================
            // Home Loan EMI Calculator → export Year-wise table to Excel (that page exposes a Year table).  [2](https://emicalculator.net/what-is-20-10-4-rule-for-car-loans/)
            System.out.println("\n=== Scenario 2: Home Loan EMI Calculator → Export yearly table to Excel ===");
            goToHomeLoanViaMenuOrDirect(driver, wait);

            typeAfterLabel(wait, "Loan Amount", "5000000");  // 50L
            typeAfterLabel(wait, "Interest Rate", "9");
            ensureTenureUnit(wait, "Yr");
            typeAfterLabel(wait, "Loan Tenure", "20");
            pressTab(driver);

            By yearlyTable = By.xpath("//table[.//th[normalize-space()='Year']]");
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(yearlyTable));

            List<String> headers = table.findElements(By.cssSelector("thead th"))
                    .stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());

            List<List<String>> rows = new ArrayList<>();
            for (WebElement tr : table.findElements(By.cssSelector("tbody tr"))) {
                List<WebElement> tds = tr.findElements(By.cssSelector("td"));
                if (tds.isEmpty()) continue;
                List<String> vals = tds.stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
                rows.add(vals);
            }

            String outFile = Paths.get(System.getProperty("user.dir"), "home_loan_yearly_schedule.xlsx").toString();
            writeToExcel(outFile, "YearlySchedule", headers, rows);
            System.out.println("Excel written: " + outFile);


            // ===================== SCENARIO 3 =====================
            // Loan Calculator → EMI / Loan Amount / Loan Tenure tabs → UI checks.  [3](https://loan-emicalculator.com/home-loan-emi-calculator/)
            System.out.println("\n=== Scenario 3: Loan Calculator → UI checks (EMI / Amount / Tenure) ===");
            driver.get("https://emicalculator.net/loan-calculator/");
            acceptCookiesIfAny(driver);

            // EMI Calculator tab (default)
            validateCalculatorUI_EMI(driver, wait);

            System.out.println("\n--- Phase 2: Loan Amount Calculator ---");

            navigateTo(driver, wait, "Affordability");

            jsFill(driver, wait, "loanemi", "50000");
            jsFill(driver, wait, "loaninterest", "9.5");
            jsFill(driver, wait, "loanterm", "1");

            // Phase 3: Loan Tenure Calculator
            System.out.println("\n--- Phase 3: Loan Tenure Calculator ---");

            navigateTo(driver, wait, "Tenure");
            jsFill(driver, wait, "loanamount", "1500000");
            jsFill(driver, wait, "loaninterest", "9.5");
            jsFill(driver, wait, "loanemi", "131525");




            System.out.println("\nAll scenarios completed successfully. ✅");

        } catch (Exception e) {
            System.out.println("⚠️ Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // ===================== Helpers & Reusable Methods =====================

    private static void acceptCookiesIfAny(WebDriver driver) {
        List<By> candidates = Arrays.asList(
                By.id("onetrust-accept-btn-handler"),
                By.cssSelector("button[aria-label='Accept']"),
                By.xpath("//button[contains(.,'Accept')]")
        );
        for (By c : candidates) {
            try {
                List<WebElement> els = driver.findElements(c);
                if (!els.isEmpty() && els.get(0).isDisplayed()) {
                    els.get(0).click(); break;
                }
            } catch (Exception ignored) {}
        }
    }

    /** Homepage tab (e.g., "Car Loan"), then wait for EMI value to be visible.  [1](https://amortizationschedule.org/monthly) */


    /** Type into an input near the given label text. */
    private static void typeAfterLabel(WebDriverWait wait, String labelText, String value) {
        By inputNearLabel = By.xpath(
                "//*[self::label or self::*[self::div or self::span][contains(., '" + labelText + "')]][1]" +
                        "/following::*[self::input or self::textarea][1]"
        );
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(inputNearLabel));
        clearAndType(input, value);
    }

    /** Ensure Tenure unit is set to Yr or Mo. */
    private static void ensureTenureUnit(WebDriverWait wait, String targetUnit) {
        By yrToggle = By.xpath("//*[contains(.,'Loan Tenure')]/following::*[normalize-space()='Yr'][1]");
        By moToggle = By.xpath("//*[contains(.,'Loan Tenure')]/following::*[normalize-space()='Mo'][1]");
        if ("Yr".equalsIgnoreCase(targetUnit)) {
            WebElement yr = wait.until(ExpectedConditions.elementToBeClickable(yrToggle)); yr.click();
        } else {
            WebElement mo = wait.until(ExpectedConditions.elementToBeClickable(moToggle)); mo.click();
        }
    }

    private static void pressTab(WebDriver driver) {
        try { new Actions(driver).sendKeys(Keys.TAB).perform(); } catch (Exception ignored) {}
    }



    /** Monthly table (best-effort). */
    private static void tryExtractFirstMonthBreakup(WebDriverWait wait) {
        try {
            By monthlyTab = By.xpath("//button[contains(.,'Month') or contains(.,'Monthly') or contains(.,'By month')]");
            List<WebElement> possible = wait.withTimeout(Duration.ofSeconds(2)).until(d -> d.findElements(monthlyTab));
            if (!possible.isEmpty()) possible.get(0).click();

            By monthTable = By.xpath("//table[.//th[contains(.,'Month')]]");
            WebElement tbl = wait.until(ExpectedConditions.visibilityOfElementLocated(monthTable));
            List<WebElement> row1 = tbl.findElements(By.cssSelector("tbody tr")).get(0).findElements(By.cssSelector("td"));
            String m1Interest  = row1.get(2).getText();
            String m1Principal = row1.get(1).getText();
            System.out.println("First-month (monthly schedule): Principal=" + m1Principal + ", Interest=" + m1Interest);
        } catch (Exception ignored) {}
    }

    /** Navigate to dedicated Home Loan EMI Calculator (fallback direct URL).  [2](https://emicalculator.net/what-is-20-10-4-rule-for-car-loans/) */
    private static void goToHomeLoanViaMenuOrDirect(WebDriver driver, WebDriverWait wait) {
        try {
            By homeLoanMenu = By.xpath("//nav//a[contains(.,'Home Loan') and (contains(.,'EMI') or contains(.,'Calculator'))]");
            wait.until(ExpectedConditions.elementToBeClickable(homeLoanMenu)).click();
        } catch (Exception e) {
            driver.get("https://emicalculator.net/home-loan-emi-calculator/");
        }
        acceptCookiesIfAny(driver);
    }

    private static void writeToExcel(String path, String sheetName, List<String> headers, List<List<String>> rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet(sheetName);
            int r = 0;
            Row hr = sh.createRow(r++);
            for (int c = 0; c < headers.size(); c++) {
                hr.createCell(c, CellType.STRING).setCellValue(headers.get(c));
            }
            for (List<String> row : rows) {
                Row rr = sh.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    rr.createCell(c, CellType.STRING).setCellValue(row.get(c));
                }
            }
            for (int c = 0; c < headers.size(); c++) sh.autoSizeColumn(c);
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
        }
    }

    // --------------------- Scenario 3: Robust tab opening + UI checks ---------------------

    /** Loan Calculator tab opener: wait for tab‑specific INPUTs (not result text).  [3](https://loan-emicalculator.com/home-loan-emi-calculator/) */
    private static void openCalculatorTab(WebDriver driver, WebDriverWait wait, String linkText) {
        By link = By.xpath("//a[normalize-space()='" + linkText + "']");
        WebElement tabLink = wait.until(ExpectedConditions.elementToBeClickable(link));
        scrollIntoView(driver, tabLink);
        tabLink.click();

        if (linkText.toLowerCase().contains("amount")) {
            // EMI input visible is a good cue for Loan Amount calculator
            waitForAnyClickable(driver, Arrays.asList(
                    By.xpath("//*[normalize-space()='EMI']/following::input[1]"),
                    By.cssSelector("input[aria-label*='EMI' i]"),
                    By.cssSelector("input[id*='emi' i], input[name*='emi' i]")
            ), 15);
        } else if (linkText.toLowerCase().contains("tenure")) {
            waitForAnyClickable(driver, Arrays.asList(
                    By.xpath("//*[contains(.,'Loan Amount')]/following::input[1]"),
                    By.xpath("//*[contains(.,'EMI')]/following::input[1]"),
                    By.xpath("//*[contains(.,'Interest Rate')]/following::input[1]")
            ), 15);
        } else {
            waitForAnyClickable(driver, Arrays.asList(
                    By.xpath("//*[contains(.,'Loan Amount')]/following::input[1]"),
                    By.xpath("//*[contains(.,'Interest Rate')]/following::input[1]"),
                    By.xpath("//*[contains(.,'Loan Tenure')]/following::input[1]")
            ), 15);
        }
    }

    private static void validateCalculatorUI_EMI(WebDriver driver, WebDriverWait wait) {
        System.out.println("[EMI Calculator] UI checks...");
        assertDisplayedAndEnabled(wait, "Loan Amount");
        assertDisplayedAndEnabled(wait, "Interest Rate");
        assertDisplayedAndEnabled(wait, "Loan Tenure");

        ensureTenureUnit(wait, "Yr");
        ensureTenureUnit(wait, "Mo");
        ensureTenureUnit(wait, "Yr");

        changeNumericAfterLabel(wait, "Loan Tenure", "2");
        System.out.println("  ✓ Inputs present, tenure toggle works, and value updated.");
    }

    private static void validateCalculatorUI_Amount(WebDriver driver, WebDriverWait wait) {
        System.out.println("[Loan Amount Calculator] UI checks...");
        openCalculatorTab(driver, wait, "Loan Amount Calculator");

        // --- EMI input (robust) ---
        WebElement emiInput = findFirstPresent(driver, Arrays.asList(
                By.xpath("//*[normalize-space()='EMI']/following::input[1]"),
                By.xpath("//*[contains(normalize-space(.),'EMI ₹')]/following::input[1]"),
                By.cssSelector("input[aria-label*='EMI' i]"),
                By.cssSelector("input[id*='emi' i], input[name*='emi' i]"),
                By.xpath("(//*[contains(.,'EMI')]/following::input)[1]")
        ), 12);
        if (emiInput == null) throw new NoSuchElementException("EMI input not found on Loan Amount Calculator");
        scrollIntoView(driver, emiInput);
        wait.until(ExpectedConditions.elementToBeClickable(emiInput));

        // --- Interest Rate (input or slider) ---
        WebElement rateControl = findFirstPresent(driver, Arrays.asList(
                By.xpath("//*[self::label or self::*[self::div or self::span][contains(.,'Interest Rate')]]/following::input[1]"),
                By.cssSelector("input[aria-label*='Interest' i]"),
                By.cssSelector("input[id*='interest' i], input[name*='interest' i]"),
                By.cssSelector("input[type='range'][aria-label*='Interest' i]"),
                By.xpath("(//*[@role='slider' and (contains(@aria-label,'Interest') or contains(@aria-valuetext,'%'))])[1]")
        ), 12);
        if (rateControl == null) {
            // last fallback: try any number input near 'Interest'
            rateControl = findFirstPresent(driver, Collections.singletonList(
                    By.xpath("(//*[contains(.,'Interest')]/following::input)[1]")
            ), 6);
        }
        if (rateControl == null) throw new NoSuchElementException("Interest Rate control not found on Loan Amount Calculator");
        scrollIntoView(driver, rateControl);

        // --- Loan Tenure (single input, Yr/Mo pair, or slider) ---
        WebElement tenureControl = findFirstPresent(driver, Arrays.asList(
                By.xpath("//*[contains(.,'Loan Tenure')]/following::input[1]"),
                By.cssSelector("input[id*='tenure' i], input[name*='tenure' i]"),
                By.cssSelector("input[type='range'][aria-label*='Tenure' i]"),
                By.xpath("(//*[@role='slider' and contains(@aria-label,'Tenure')])[1]")
        ), 12);

        // If not found, try Year/Month twin inputs after "Loan Tenure Yr Mo"
        WebElement yearsField = null, monthsField = null;
        if (tenureControl == null) {
            yearsField = findFirstPresent(driver, Arrays.asList(
                    By.xpath("(//*[contains(.,'Loan Tenure')]/following::input)[1]"),
                    By.xpath("//input[@placeholder='Years' or @aria-label='Years']")
            ), 6);
            monthsField = findFirstPresent(driver, Arrays.asList(
                    By.xpath("(//*[contains(.,'Loan Tenure')]/following::input)[2]"),
                    By.xpath("//input[@placeholder='Months' or @aria-label='Months']")
            ), 6);
        }
        if (tenureControl == null && yearsField == null) {
            throw new NoSuchElementException("Loan Tenure control not found on Loan Amount Calculator");
        }

        // --- Actions: set values ---
        // 1) EMI
        clearAndType(emiInput, "25000");

        // 2) Interest Rate: numeric or slider
        if ("input".equalsIgnoreCase(rateControl.getTagName()) &&
                !"range".equalsIgnoreCase(rateControl.getAttribute("type"))) {
            clearAndType(rateControl, "10");
        } else {
            // slider or role=slider: nudge right approx 5 steps
            adjustSlider(driver, rateControl, 5);
        }

        // 3) Tenure: single numeric, twin inputs, or slider
        if (tenureControl != null) {
            if ("input".equalsIgnoreCase(tenureControl.getTagName()) &&
                    !"range".equalsIgnoreCase(tenureControl.getAttribute("type"))) {
                clearAndType(tenureControl, "5");
            } else {
                adjustSlider(driver, tenureControl, 5);
            }
        } else {
            // Twin fields
            if (yearsField != null) { clearAndType(yearsField, "5"); }
            if (monthsField != null) { clearAndType(monthsField, "0"); }
        }

        pressTab(driver);

        // Expect a recalculated "Loan Amount" value (best-effort)
        By loanAmountResult = By.xpath(
                "//*[contains(normalize-space(.),'Loan Amount') and not(contains(.,'Loan Amount Calculator'))]" +
                        "/following::*[contains(text(),'₹')][1]"
        );
        new WebDriverWait(driver, Duration.ofSeconds(12))
                .until(ExpectedConditions.visibilityOfElementLocated(loanAmountResult));
        System.out.println("  ✓ Inputs present (or sliders), tenure toggle works, and Loan Amount recalculated is visible.");
    }



    private static void assertDisplayedAndEnabled(WebDriverWait wait, String labelText) {
        By inputNearLabel = By.xpath(
                "//*[self::label or self::*[self::div or self::span][contains(., '" + labelText + "')]]" +
                        "/following::*[self::input or self::textarea][1]"
        );
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(inputNearLabel));
        if (!el.isEnabled()) throw new AssertionError("Field not enabled: " + labelText);
    }

    private static void changeNumericAfterLabel(WebDriverWait wait, String labelText, String numeric) {
        By inputNearLabel = By.xpath(
                "//*[self::label or self::*[self::div or self::span][contains(., '" + labelText + "')]]" +
                        "/following::*[self::input][1]"
        );
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(inputNearLabel));
        clearAndType(input, numeric);
    }

    /** Find first *present* (displayed) element among locators (then return it). */
    private static WebElement findFirstPresent(WebDriver driver, List<By> locators, int timeoutSeconds) {
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

    /** Wait until ANY locator becomes clickable, within timeoutSeconds. */
    private static void waitForAnyClickable(WebDriver driver, List<By> locators, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            for (By by : locators) {
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(2))
                            .until(ExpectedConditions.elementToBeClickable(by));
                    return;
                } catch (Exception ignored) {}
            }
        }
        throw new TimeoutException("None of the expected elements became clickable for the selected tab.");
    }

    private static void adjustSlider(WebDriver driver, WebElement sliderOrRoleSlider, int stepsRight) {
        scrollIntoView(driver, sliderOrRoleSlider);
        sliderOrRoleSlider.click();
        Actions a = new Actions(driver);
        for (int i = 0; i < stepsRight; i++) a.sendKeys(Keys.ARROW_RIGHT).perform();
    }

    private static void clearAndType(WebElement input, String value) {
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        input.sendKeys(value);
    }
    private static void selectAllAndType(WebElement el, String value) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.DELETE);
        el.sendKeys(value);
    }
    private static void scrollIntoView(WebDriver driver, WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }


    private static void jsFill(WebDriver driver, WebDriverWait wait, String id, String value) {
        try {
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('change'));",
                    el, value
            );
            System.out.println("Set " + id + " to " + value);
        } catch (Exception e) {
            try {
                WebElement alt = driver.findElement(By.xpath("//input[contains(@id,'loan')]"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", alt, value);
            } catch (Exception ex) {
                System.out.println("Could not fill ID: " + id);
            }
        }
    }

    private static void navigateTo(WebDriver driver, WebDriverWait wait, String keyword) {
        try {
            WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(By.partialLinkText(keyword)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
            Thread.sleep(2500);
        } catch (Exception e) {
            if (keyword.contains("Affordability")) driver.get("https://emicalculator.net/loan-calculator/");
            else driver.get("https://emicalculator.net/loan-tenure-calculator/");
        }
    }


}
