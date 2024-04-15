package kell.autoclick;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.awt.Desktop;
import java.net.URI;

public class AutoClickBrowser {
    public static void main(String[] args) {
        try {
            Desktop.getDesktop().browse(new URI("https://scorecount.com/click-counter/"));

            WebDriver driver = new ChromeDriver();

            WebElement element = driver.findElement(By.id("counter1"));
            element.click();

            driver.quit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
