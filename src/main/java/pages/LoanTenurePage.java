package pages;

import org.openqa.selenium.WebDriver;

public class LoanTenurePage extends LoanCalculatorPage {
    public LoanTenurePage(WebDriver driver) {
        super(driver);
    }

    // This class can host tenure-specific behavior if the site splits routes.
    // For now, we reuse LoanCalculatorPage methods.
}
