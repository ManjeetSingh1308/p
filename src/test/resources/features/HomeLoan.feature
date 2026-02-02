Feature: Home Loan EMI Calculator - Export Yearly Schedule

  Scenario: Export yearly schedule to Excel
    Given I open the Home Loan EMI Calculator
    When I set Loan Amount "5000000", Interest "9", Tenure years "20"
    Then I export the yearly schedule to Excel at "target/home_loan_yearly_schedule.xlsx"