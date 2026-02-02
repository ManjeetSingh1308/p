Feature: Loan Calculator - UI and Tabs

  Scenario: EMI Calculator UI checks
    Given I open the Loan Calculator page
    Then I validate the EMI Calculator UI components and toggle

  Scenario: Loan Amount (Affordability) tab inputs and recalculation
    Given I open the Loan Calculator page
    When I navigate to the "Affordability" tab
    And I fill EMI calculator inputs EMI "50000", Rate "9.5", Tenure "1"
    Then I verify recalculated Loan Amount is visible

  Scenario: Loan Tenure Calculator - set fields by id
    Given I open the Loan Calculator page
    When I navigate to the "Tenure" tab
    And I set fields by id: loanamount "1500000", loaninterest "9.5", loanemi "131525"
    Then I verify recalculated Loan Amount is visible