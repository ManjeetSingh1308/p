package stepdefinitions;

import pages.CarLoanPage;
import utilities.DriverFactory;
import io.cucumber.java.en.*;

public class CarLoanSteps {

    private CarLoanPage carPage;

    @Given("I navigate to the Car Loan page")
    public void i_navigate_to_car_loan_page() {
        carPage = new CarLoanPage(DriverFactory.getDriver());
        carPage.open();
    }

    @When("I enter loan amount {string}")
    public void i_enter_loan_amount(String amount) {
        carPage.enterLoanAmount(amount);
    }

    @When("I enter interest rate {string}")
    public void i_enter_interest_rate(String rate) {
        carPage.enterInterestRate(rate);
    }

    @When("I enter tenure {string}")
    public void i_enter_tenure(String years) {
        carPage.enterTenureYears(years);
    }

    @When("I switch to monthly view")
    public void i_switch_to_monthly_view() {
        carPage.switchToMonthlyScheduleIfAvailable();
        carPage.scrollToYearHeader();
    }

    @Then("I capture first month principal and interest for year {string}")
    public void i_capture_first_month_principal_and_interest(String year) {
        carPage.selectYear(year);
        String[] vals = carPage.getFirstMonthPrincipalAndInterest(year);
        System.out.println("--- Car Loan (First Month) ---");
        System.out.println("Principal Amount: " + vals[0]);
        System.out.println("Interest Amount: " + vals[1]);
    }
}