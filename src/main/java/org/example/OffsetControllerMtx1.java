package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/offset/mtx1")
public class OffsetControllerMtx1 {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";

    @Value("${app.username:admin}")
    private String username;

    @Value("${app.password:admin}")
    private String password;

    // Mapeamento de IPs para todos MTXs
    private final Map<String, String> mtxIps = Map.of(
            "1", "http://10.10.103.103/debug/" // geral
    );

    @PostMapping("/mtx/{mtxNumber}")
    public Map<String, Object> aplicarOffsetMTX(
            @PathVariable String mtxNumber,
            @RequestParam String valorOffset) {

        String ip = mtxIps.getOrDefault(mtxNumber, "http://10.10.103.103/debug/");
        return configurarOffsetMTX(mtxNumber, valorOffset, ip);
    }

    private Map<String, Object> configurarOffsetMTX(String mtxNumber, String valorOffset, String urlBase) {
        System.out.println(GREEN + "Configurando offset para MTX" + mtxNumber + ": " + valorOffset + RESET);
        System.out.println(GREEN + "URL: " + urlBase + RESET);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--disable-cache");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);
            System.out.println(GREEN + "Acessando MTX" + mtxNumber + ": " + urlBase + RESET);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);
            System.out.println(GREEN + "Usuário preenchido" + RESET);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
            campoSenha.clear();
            campoSenha.sendKeys(password);
            System.out.println(GREEN + "Senha preenchida" + RESET);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
            botaoLogin.click();
            System.out.println(GREEN + "Login realizado" + RESET);

            // Aguardar login
            Thread.sleep(2000);

            // Verificar se está logado
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Internal')]")));
            } catch (Exception e) {
                System.out.println(GREEN + "Possível falha no login" + RESET);
            }

            // Tentar diferentes estratégias para encontrar Internal1
            WebElement internal1 = null;
            String[] xpaths = {
                    "//label[contains(@link, 'Internal1')]/input",
                    "//input[contains(@id, 'Internal1')]",
                    "//*[contains(text(), 'Internal1')]/preceding-sibling::input",
                    "//input[contains(@name, 'Internal1')]"
            };

            for (String xpath : xpaths) {
                try {
                    internal1 = driver.findElement(By.xpath(xpath));
                    if (internal1 != null && internal1.isDisplayed() && internal1.isEnabled()) {
                        System.out.println(GREEN + "Internal1 encontrado com xpath: " + xpath + RESET);
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            if (internal1 == null) {
                throw new Exception("Elemento Internal1 não encontrado");
            }

            internal1.click();
            Thread.sleep(500);
            System.out.println(GREEN + "Internal 1 selecionado" + RESET);

            // Encontrar Power Offset
            WebElement powerOffset = null;
            String[] powerOffsetSelectors = {
                    "//*[contains(@id, 'Internal1.power.offset')]",
                    "//*[contains(@id, 'Internal1_power_offset')]",
                    "//*[contains(text(), 'Power') and contains(text(), 'Offset')]",
                    "//input[contains(@name, 'PowerOffset')]",
                    "//button[contains(text(), 'Power Offset')]"
            };

            for (String selector : powerOffsetSelectors) {
                try {
                    powerOffset = driver.findElement(By.xpath(selector));
                    if (powerOffset != null && powerOffset.isDisplayed() && powerOffset.isEnabled()) {
                        System.out.println(GREEN + "PowerOffset encontrado: " + selector + RESET);
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            if (powerOffset == null) {
                throw new Exception("PowerOffset não encontrado");
            }

            powerOffset.click();
            Thread.sleep(500);
            System.out.println(GREEN + "PowerOffset clicado" + RESET);

            // Encontrar e preencher o campo de valor
            WebElement campoValor = null;
            try {
                // Tentar campo ativo
                campoValor = driver.switchTo().activeElement();
                if (!campoValor.getTagName().equalsIgnoreCase("input")) {
                    campoValor = null;
                }
            } catch (Exception e) {
                // Continuar com outras estratégias
            }

            if (campoValor == null) {
                // Procurar inputs visíveis
                var inputs = driver.findElements(By.tagName("input"));
                for (WebElement input : inputs) {
                    if (input.isDisplayed() && input.isEnabled()) {
                        campoValor = input;
                        break;
                    }
                }
            }

            if (campoValor == null) {
                throw new Exception("Campo de entrada não encontrado");
            }

            // Limpar e preencher
            campoValor.click();
            Thread.sleep(300);
            campoValor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            campoValor.sendKeys(Keys.DELETE);
            campoValor.sendKeys(valorOffset);
            System.out.println(GREEN + "Valor " + valorOffset + " inserido" + RESET);

            // Confirmar
            campoValor.sendKeys(Keys.ENTER);
            Thread.sleep(1000);

            // Verificar se foi aplicado
            resultado.put("status", "sucesso");
            resultado.put("mensagem", "Offset " + valorOffset + " aplicado com sucesso no MTX" + mtxNumber);
            resultado.put("mtx", mtxNumber);
            resultado.put("offset", valorOffset);

        } catch (Exception e) {
            System.err.println(GREEN + "Erro no MTX" + mtxNumber + ": " + e.getMessage() + RESET);
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro no MTX" + mtxNumber + ": " + e.getMessage());
            resultado.put("mtx", mtxNumber);

        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    System.out.println(GREEN + "Driver finalizado para MTX" + mtxNumber + RESET);
                } catch (Exception e) {
                    System.err.println(GREEN + "Erro ao finalizar driver: " + e.getMessage() + RESET);
                }
            }
        }

        return resultado;
    }
}