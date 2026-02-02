package helpers;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class SliderHelper {

    private final WebDriver driver;

    public SliderHelper(WebDriver driver) {
        this.driver = driver;
    }

    public void nudgeRight(WebElement slider, int steps) {
        slider.click();
        Actions a = new Actions(driver);
        for (int i = 0; i < steps; i++) a.sendKeys(Keys.ARROW_RIGHT).perform();
    }
}