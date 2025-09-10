package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class App {
    static List<String> textosOriginais = new ArrayList<>();
    static List<String> textosResumidos = new ArrayList<>();
    static List<String> urlsVisitadas = new ArrayList<>();
    public static void main( String[] args ) throws InterruptedException, IOException{

        FirefoxProfile profile = new FirefoxProfile();

        profile.setPreference("general.useragent.override", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/117.0");
        profile.setPreference("dom.webdriver.enabled", false);
        profile.setPreference("useAutomationExtension", false);

        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        options.addArguments("--headless");

        FirefoxDriver driver = new FirefoxDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        driver.navigate().to("https://mooc.campusvirtual.fiocruz.br/rea/saude-digital/modulo1/aula1/topico1.html");
        
        while(!urlsVisitadas.contains("https://mooc.campusvirtual.fiocruz.br/rea/saude-digital/modulo4/encerramento.html")) {

            System.out.println("\nURL atual: " + driver.getCurrentUrl());

            List<WebElement> botoesExpandir = wait.until(dri -> 
                    dri.findElements(By.cssSelector("button[aria-expanded='false'], .btn-expand, .accordion-button, [data-toggle='collapse']"))
                );
                
            for (WebElement botao : botoesExpandir) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", botao);
                    Thread.sleep(500); 
                    botao.click();
                    Thread.sleep(500); 
                } catch (Exception e) {
                }
            }
            
            Thread.sleep(1000);

            WebElement div = driver.findElement(By.id("page-content"));

            wait.until(dri -> {
                String content = (String) ((JavascriptExecutor) dri)
                    .executeScript("return arguments[0].textContent;", div);
                return content.length() > 100; 
            });

            String texto = (String) ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].innerText;", div);

            System.out.println("Texto: " + texto);

            List<String> info = getModuloAulaTopico(driver.getCurrentUrl());

            String modulo = info.get(0);
            String aula = info.get(1);
            String topico = info.get(2);

            System.out.println("\nMódulo: " + modulo);
            System.out.println("Aula: " + aula);
            System.out.println("Tópico: " + topico + "\n");

            StringBuilder cabecalho = new StringBuilder();
            cabecalho.append("Módulo: ").append(modulo).append("\n");
            if (!aula.isEmpty()) {
                cabecalho.append("Aula: ").append(aula).append("\n");
            }
            if (!topico.isEmpty()) {
                cabecalho.append("Tópico: ").append(topico).append("\n");
            }
            StringBuilder sb = new StringBuilder(texto);
            sb.insert(0, cabecalho.toString());
            textosOriginais.add(sb.toString());

            System.out.println("Resumindo o texto com Hugging Face...");

            try {
                List<String> partes = dividirTexto(texto, 1500); 
                textosResumidos.add("Resumo do " + cabecalho.toString());
                for (String parte : partes) {
                    Thread.sleep(1000);
                    String resumo = resumirTexto(parte);
                    System.out.println("\n ===== Resumo parcial ===== \n" + resumo + "\n");
                    textosResumidos.add(resumo);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            List<WebElement> links = driver.findElements(By.cssSelector("a.fio-button.fio-button-primary[rel='next']"));

            if (!links.isEmpty()) {
                String href = links.get(0).getAttribute("href");
                System.out.println("Próximo tópico: " + href);

                if(href.equals("https://cursos.campusvirtual.fiocruz.br/mod/quiz/view.php?id=125728") 
                    || href.contains("https://cursos.campusvirtual.fiocruz.br/mod/quiz/view.php?")) {
                    System.out.println("Chegou no quiz, finalizando.");
                    break;
                }

                urlsVisitadas.add(driver.getCurrentUrl());

                if(!(href.equals("https://cursos.campusvirtual.fiocruz.br/mod/quiz/view.php?id=125728") 
                    && !(href.contains("https://cursos.campusvirtual.fiocruz.br/mod/quiz/view.php?")))) {
                    driver.navigate().to(href);
                }
                else {
                    System.out.println("Próximo é o quiz, finalizando.");
                    break;
                }

            } else {
                System.out.println("Nenhum link encontrado.");
            }

            //Thread.sleep(1000);

        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("textos_originais.txt"))) {
            for (String texto : textosOriginais) {
                writer.write(texto);
                writer.write("\n====================================\n");
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("textos_resumidos.txt"))) {
            for (String texto : textosResumidos) {
                writer.write(texto);
                writer.write("\n------------------------------------\n");
            }
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
        parameters.addProperty("max_length", 600); 
        parameters.addProperty("min_length", 50);  
        parameters.addProperty("do_sample", false); 
        requestJson.add("parameters", parameters);

        String jsonBody = gson.toJson(requestJson); 
        //System.out.println("\n\nJSON Body sendo enviado:\n" + jsonBody);

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

    public static List<String> dividirTexto(String texto, int tamanhoMaximo) {
        List<String> partes = new ArrayList<>();
        String[] palavras = texto.split(" ");
        StringBuilder buffer = new StringBuilder();

        for (String palavra : palavras) {
            if (buffer.length() + palavra.length() + 1 > tamanhoMaximo) {
                partes.add(buffer.toString().trim());
                buffer = new StringBuilder();
            }
            buffer.append(palavra).append(" ");
        }
        if (buffer.length() > 0) {
            partes.add(buffer.toString().trim());
        }
        return partes;
    }

    public static List<String> getModuloAulaTopico(String url) {
        Pattern p = Pattern.compile(".*/(modulo\\d+)(?:/(aula\\d+))?(?:/(topico\\d+|apresentacao|encerramento))?\\.html");
        Matcher m = p.matcher(url);
        List<String> resultado = new ArrayList<>();
        
        if (m.matches()) {
            resultado.add(m.group(1)); 
            resultado.add(m.group(2) != null ? m.group(2) : ""); // aula pode não existir
            resultado.add(m.group(3) != null ? m.group(3) : ""); // pode ser topicoX, apresentacao, encerramento
        }
        return resultado;
    }
}
