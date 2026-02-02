package pages;

import org.openqa.selenium.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CarLoanPage extends BasePage {

    public CarLoanPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get("https://emicalculator.net/");
        acceptCookiesIfAny();
        driver.findElement(By.xpath("//a[text()='Car Loan']")).click();
    }

    public void enterLoanAmount(String amount) {
        WebElement el = driver.findElement(By.id("loanamount"));
        selectAllAndType(el, amount);
    }

    public void enterInterestRate(String rate) {
        WebElement el = driver.findElement(By.id("loaninterest"));
        selectAllAndType(el, rate);
    }

    public void enterTenureYears(String years) {
        WebElement el = driver.findElement(By.id("loanterm"));
        selectAllAndType(el, years);
    }

    public void switchToMonthlyScheduleIfAvailable() {
        // Try various selectors for "Monthly" schedule toggle
        List<By> candidates = List.of(
                By.xpath("//button[contains(.,'Month')]"),
                By.xpath("//label[contains(.,'Monthly') or contains(.,'Month')]"),
                By.xpath("//div[contains(@class,'btn-group')]/label[contains(@class,'btn')]")
        );
        WebElement btn = findFirstPresent(candidates, 5);
        if (btn != null) btn.click();
    }

    public void scrollToYearHeader() {
        WebElement span = driver.findElement(By.xpath("//div/table/tbody/tr/th[contains(@class,'col')]"));
        scrollIntoView(span);
    }

    public void selectYear(String year) {
        driver.findElement(By.id("year" + year)).click();
    }

    public String[] getFirstMonthPrincipalAndInterest(String year) {
        WebElement firstRow = driver.findElement(By.xpath("//tr[@id='monthyear" + year + "']/td/div/table/tbody/tr[1]"));
        String principal = firstRow.findElement(By.xpath("./td[2]")).getText();
        String interest  = firstRow.findElement(By.xpath("./td[3]")).getText();
        return new String[]{principal, interest};
    }

    public List<String> getYearlyHeaders() {
        WebElement table = driver.findElement(By.xpath("//table[.//th[normalize-space()='Year']]"));
        return table.findElements(By.cssSelector("thead th"))
                .stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
    }

    public List<List<String>> getYearlyRows() {
        WebElement table = driver.findElement(By.xpath("//table[.//th[normalize-space()='Year']]"));
        List<List<String>> rows = new ArrayList<>();
        for (WebElement tr : table.findElements(By.cssSelector("tbody tr"))) {
            List<WebElement> tds = tr.findElements(By.cssSelector("td"));
            if (tds.isEmpty()) continue;
            List<String> vals = tds.stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
            rows.add(vals);
        }
        return rows;
    }
}