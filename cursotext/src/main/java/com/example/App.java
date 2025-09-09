package com.example;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {
    public static void main( String[] args ) throws InterruptedException{

        FirefoxProfile profile = new FirefoxProfile();

        // Definindo o User-Agent para imitar um navegador real
        // Use um User-Agent atualizado, que você pode encontrar facilmente na internet
        profile.setPreference("general.useragent.override", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/117.0");

        // Desabilitando a detecção de automação "navigator.webdriver"
        // O "dom.webdriver.enabled" é a flag que os sites verificam
        profile.setPreference("dom.webdriver.enabled", false);

        // Desabilitando a flag "webdriver" para o JavaScript
        profile.setPreference("useAutomationExtension", false);

        // Adicionando o perfil às opções do Firefox
        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        //options.addArguments("--headless");

        FirefoxDriver driver = new FirefoxDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        driver.navigate().to("https://mooc.campusvirtual.fiocruz.br/rea/saude-digital/modulo2/aula1/topico3.html");
        List<WebElement> botoesExpandir = wait.until(dri -> 
                dri.findElements(By.cssSelector("button[aria-expanded='false'], .btn-expand, .accordion-button, [data-toggle='collapse']"))
            );
            
        for (WebElement botao : botoesExpandir) {
            try {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", botao);
                Thread.sleep(500); 
                botao.click();
                Thread.sleep(1000); 
            } catch (Exception e) {
            }
        }
        
        Thread.sleep(3000);

        WebElement div = driver.findElement(By.id("page-content"));
        wait.until(dri -> {
            String content = (String) ((JavascriptExecutor) dri)
                .executeScript("return arguments[0].textContent;", div);
            return content.length() > 100; 
        });

        String texto = (String) ((JavascriptExecutor) driver)
            .executeScript("return arguments[0].innerText;", div);

        System.out.println("Texto: " + texto);

        /* 
        driver.navigate().to("https://chatgpt.com/");
        Thread.sleep(1000);

        WebElement promptDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("prompt-textarea")));

        WebElement paragraph = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("p.placeholder[data-placeholder='Ask anything']")));

        String textToSend = "Olá, esse é o texto que eu quero enviar.";

        // Injeta o texto diretamente usando o JavaScript Executor
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].textContent = arguments[1];", paragraph, textToSend);
        
        System.out.println("Texto injetado com sucesso!");
        Thread.sleep(1000);
        driver.findElement(By.id("composer-submit-button")).click();
*/
/* 
        driver.navigate().to("https://www.editpad.org/tool/pt/text-summarizer");

        WebElement textArea = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("text")));
        String textToSend = "Olá, esse é o texto que eu quero enviar.";
        textArea.sendKeys(textToSend);

        driver.findElement(By.className("main-btn")).click();
        */
        //driver.quit();
    }
}
