package stepdefinitions;

import utilities.ConfigReader;
import utilities.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.openqa.selenium.WebDriver;

public class Hooks {

    @Before
    public void setUp() {
        DriverFactory.initDriver();
        WebDriver driver = DriverFactory.getDriver();
        driver.manage().deleteAllCookies();
    }

    @After
    public void tearDown() {
        String keepOpen = ConfigReader.get("keepBrowserOpenAfterTests", "false");
        if (!Boolean.parseBoolean(keepOpen)) {
            DriverFactory.quitDriver();
        }
    }
}