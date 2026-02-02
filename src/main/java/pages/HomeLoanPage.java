package pages;

import utilities.ExcelWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeLoanPage extends BasePage {

    public HomeLoanPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        try {
            By homeLoanMenu = By.xpath("//nav//a[contains(.,'Home Loan') and (contains(.,'EMI') or contains(.,'Calculator'))]");
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(homeLoanMenu)).click();
        } catch (Exception e) {
            driver.get("https://emicalculator.net/home-loan-emi-calculator/");
        }
        acceptCookiesIfAny();
    }

    public void fillInputs(String amount, String rate, String tenureYears) {
        typeAfterLabel("Loan Amount", amount);
        typeAfterLabel("Interest Rate", rate);
        ensureTenureUnit("Yr");
        typeAfterLabel("Loan Tenure", tenureYears);
        pressTab();
    }

    public WebElement waitYearlyTable() {
        By yearlyTable = By.xpath("//table[.//th[normalize-space()='Year']]");
        return new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.visibilityOfElementLocated(yearlyTable));
    }

    public List<String> getTableHeaders(WebElement table) {
        return table.findElements(By.cssSelector("thead th"))
                .stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
    }

    public List<List<String>> getTableRows(WebElement table) {
        List<List<String>> rows = new ArrayList<>();
        for (WebElement tr : table.findElements(By.cssSelector("tbody tr"))) {
            List<WebElement> tds = tr.findElements(By.cssSelector("td"));
            if (tds.isEmpty()) continue;
            List<String> vals = tds.stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
            rows.add(vals);
        }
        return rows;
    }

    public void exportYearlyScheduleToExcel(String outPath) throws Exception {
        WebElement table = waitYearlyTable();
        List<String> headers = getTableHeaders(table);
        List<List<String>> rows = getTableRows(table);
        ExcelWriter.writeTable(outPath, "YearlySchedule", headers, rows);
    }
}