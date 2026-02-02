Feature: Car Loan EMI Calculation

  Scenario: Validate first month EMI breakup
    Given I navigate to the Car Loan page
    When I enter loan amount "1500000"
    And I enter interest rate "9.5"
    And I enter tenure "1"
    And I switch to monthly view
    Then I capture first month principal and interest for year "2026"