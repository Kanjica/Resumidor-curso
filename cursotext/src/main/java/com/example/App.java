package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class App {
    public static void main( String[] args ) throws InterruptedException{

        FirefoxProfile profile = new FirefoxProfile();

        profile.setPreference("general.useragent.override", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/117.0");
        profile.setPreference("dom.webdriver.enabled", false);
        profile.setPreference("useAutomationExtension", false);

        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        options.addArguments("--headless");

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

        System.out.println("Resumindo o texto com Hugging Face...");
        try {
            String resumo = resumirTexto(texto);
            //String textoDeTeste = "O sol é uma estrela no centro do nosso sistema solar. A Terra gira ao redor do sol. O sol é muito importante para a vida no nosso planeta.";
            //String resumo = resumirTexto(textoDeTeste);
            System.out.println("Resumo: " + resumo);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        driver.quit();
    }

    public static String resumirTexto(String textoParaResumir) throws IOException, InterruptedException {
        String apiKey = "Bearer hf_chave_do_pai";

        HttpClient client = HttpClient.newHttpClient();

        String modelId = "facebook/bart-large-cnn"; 

        String textoLimpo = textoParaResumir
                                .replace("\n", " ")
                                .replaceAll("\\s+", " ")
                                .trim();

        String prompt = textoLimpo;

        Gson gson = new Gson();
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("inputs", prompt);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("max_length", 200); 
        parameters.addProperty("min_length", 50);  
        parameters.addProperty("do_sample", false); 
        requestJson.add("parameters", parameters);

        String jsonBody = gson.toJson(requestJson); 
        System.out.println("JSON Body sendo enviado:\n" + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api-inference.huggingface.co/models/" + modelId))
                                .header("Content-Type", "application/json")
                                .header("Authorization", apiKey)
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();

            JsonArray jsonArray = gson.fromJson(responseBody, JsonArray.class);
            JsonObject resultObject = jsonArray.get(0).getAsJsonObject();
            String resumo = resultObject.get("summary_text").getAsString();

            return resumo;
        } else {
            System.err.println("Erro na requisição: " + response.statusCode());
            System.err.println("Corpo da resposta: " + response.body());
            System.err.println("URL da API: " + "https://api-inference.huggingface.co/models/" + modelId);

            return "Erro ao resumir o texto.";
        }
    }
}
