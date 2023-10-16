Feature: Integration test to verify the functionality of the health endpoint

  Background:
    Given url baseUrl
    Given path '/healthz'

  Scenario: Health endpoint returns 200

    When method GET
    Then status 200