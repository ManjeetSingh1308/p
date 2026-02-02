package stepdefinitions;

import pages.HomeLoanPage;
import utilities.DriverFactory;
import io.cucumber.java.en.*;

public class HomeLoanSteps {

    private HomeLoanPage homePage;

    @Given("I open the Home Loan EMI Calculator")
    public void i_open_home_loan_emi_calculator() {
        homePage = new HomeLoanPage(DriverFactory.getDriver());
        homePage.open();
    }

    @When("I set Loan Amount {string}, Interest {string}, Tenure years {string}")
    public void i_set_inputs(String amount, String rate, String tenure) {
        homePage.fillInputs(amount, rate, tenure);
    }

    @Then("I export the yearly schedule to Excel at {string}")
    public void i_export_yearly_schedule(String outPath) throws Exception {
        homePage.exportYearlyScheduleToExcel(outPath);
    }
}