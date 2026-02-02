package stepdefinitions;

import pages.LoanCalculatorPage;
import utilities.DriverFactory;
import io.cucumber.java.en.*;

public class CalculatorSteps {

    private LoanCalculatorPage calcPage;

    @Given("I open the Loan Calculator page")
    public void i_open_loan_calculator() {
        calcPage = new LoanCalculatorPage(DriverFactory.getDriver());
        calcPage.open();
    }

    @Then("I validate the EMI Calculator UI components and toggle")
    public void i_validate_emi_ui() {
        calcPage.validateEMICalculatorUI();
    }

    @When("I navigate to the {string} tab")
    public void i_navigate_to_tab(String tabName) {
        calcPage.openTab(tabName);
    }

    @When("I fill EMI calculator inputs EMI {string}, Rate {string}, Tenure {string}")
    public void i_fill_emi_amount_calculator(String emi, String rate, String tenure) {
        calcPage.fillEMIAmountCalculator(emi, rate, tenure);
    }

    @Then("I verify recalculated Loan Amount is visible")
    public void i_verify_recalculated_loan_amount() {
        calcPage.assertRecalculatedLoanAmountVisible();
    }

    @When("I set fields by id: loanamount {string}, loaninterest {string}, loanterm {string}")
    public void i_set_by_id_amount_rate_term(String amount, String rate, String term) {
        calcPage.jsFillById("loanamount", amount);
        calcPage.jsFillById("loaninterest", rate);
        calcPage.jsFillById("loanterm", term);
    }

    @When("I set fields by id: loanamount {string}, loaninterest {string}, loanemi {string}")
    public void i_set_by_id_amount_rate_emi(String amount, String rate, String emi) {
        calcPage.jsFillById("loanamount", amount);
        calcPage.jsFillById("loaninterest", rate);
        calcPage.jsFillById("loanemi", emi);
    }
}