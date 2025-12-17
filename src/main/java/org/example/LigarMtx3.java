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
public class LigarMtx3 {

    private static final String RESET = "\u001B[0m";
    private static final String BLUE = "\u001B[34m";

    // Adicione estas linhas para injetar usuário e senha
    @Value("${app.username:admin}")
    private String username;

    @Value("${app.password:admin}")
    private String password;

    @PostMapping("/ligarMtx3")
    public Map<String, String> ligar() {
        return executarAcao("1");
    }

    @PostMapping("/desligarMtx3")
    public Map<String, String> desligar() {
        return executarAcao("2");
    }

    @GetMapping("/statusMtx3")
    public Map<String, String> status() {
        Map<String, String> resposta = new HashMap<>();
        resposta.put("mensagem", "Use: POST /ligar ou POST /desligar");
        resposta.put("ligar", "Envia 1 para o campo RfMasterOn");
        resposta.put("desligar", "Envia 2 para o campo RfMasterOn");
        return resposta;
    }

    private Map<String, String> executarAcao(String valor) {
        Map<String, String> resultado = new HashMap<>();

        // Chama o método original e converte o resultado
        Map<String, Object> resultadoDetalhado = configurarStatusMTX3(valor);

        // Converte para Map<String, String>
        resultado.put("status", (String) resultadoDetalhado.get("status"));

        // Adiciona a mensagem
        if (resultadoDetalhado.containsKey("mensagem")) {
            resultado.put("mensagem", (String) resultadoDetalhado.get("mensagem"));
        } else if (valor.equals("1")) {
            resultado.put("mensagem", "MTX3 ligado");
            resultado.put("acao", "LIGADO");
        } else {
            resultado.put("mensagem", "MTX3 desligado");
            resultado.put("acao", "DESLIGADO");
        }

        return resultado;
    }

    private Map<String, Object> configurarStatusMTX3(String valorStatus) {
        String urlBase = "http://10.10.103.103/debug/";
        System.out.println(BLUE + "URL que será acessada: " + urlBase + RESET);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        // Descomente a linha abaixo para modo headless (sem abrir janela)
        // options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--disable-cache");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);
            System.out.println(BLUE + "Acessando: " + urlBase + RESET);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
            campoSenha.clear();
            campoSenha.sendKeys(password);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
            botaoLogin.click();
            System.out.println(BLUE + "Login realizado" + RESET);

            // Aguardar carregamento
            //Thread.sleep(1000);

            // Acessa PowerAmp 3
            WebElement powerAmp3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier3___']/input")));
            powerAmp3.click();
            Thread.sleep(300);
            System.out.println(BLUE + "PowerAmplifier3 selecionado" + RESET);

            // Clica no RfMasterOn
            WebElement RfMasterOn = null;
            try {
                // Tentativa 1: Pelo ID exato
                RfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("PowerAmplifier3.Config.RfMasterOn")));
                System.out.println(BLUE + "RfMasterOn encontrado pelo ID exato" + RESET);
            } catch (Exception e1) {
                try {
                    // Tentativa 2: Pelo ID com underscore
                    RfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                            By.id("PowerAmplifier3_Config_RfMasterOn")));
                    System.out.println(BLUE + "RfMasterOn encontrado pelo ID com underscore" + RESET);
                } catch (Exception e2) {
                    try {
                        // Tentativa 3: Qualquer elemento com RfMasterOn
                        RfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//*[contains(@id, 'RfMasterOn') or contains(@name, 'RfMasterOn')]")));
                        System.out.println(BLUE + "RfMasterOn encontrado por busca genérica" + RESET);
                    } catch (Exception e3) {
                        resultado.put("status", "erro");
                        resultado.put("mensagem", "RfMasterOn não encontrado");
                        return resultado;
                    }
                }
            }

            RfMasterOn.click();
            Thread.sleep(300);
            System.out.println(BLUE + "RfMasterOn clicado - diálogo aberto" + RESET);

            // Encontrar campo para digitar
            WebElement campoNewValue = null;

            // Estratégia 1: Campo ativo (focado)
            try {
                campoNewValue = driver.switchTo().activeElement();
                if (campoNewValue.getTagName().equals("input") || campoNewValue.getTagName().equals("textarea")) {
                    System.out.println(BLUE + "Campo ativo encontrado" + RESET);
                } else {
                    campoNewValue = null;
                }
            } catch (Exception e) {
                System.out.println(BLUE + "Campo ativo não encontrado" + RESET);
            }

            // Estratégia 2: Procurar qualquer input
            if (campoNewValue == null) {
                try {
                    java.util.List<WebElement> todosInputs = driver.findElements(By.tagName("input"));
                    for (WebElement input : todosInputs) {
                        if (input.isDisplayed() && input.isEnabled()) {
                            campoNewValue = input;
                            System.out.println(BLUE + "Campo input encontrado" + RESET);
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println(BLUE + "Nenhum input encontrado" + RESET);
                }
            }

            if (campoNewValue != null) {
                // Limpar e digitar
                campoNewValue.click();
                Thread.sleep(300);
                campoNewValue.sendKeys(Keys.CONTROL + "a");
                campoNewValue.sendKeys(Keys.DELETE);
                campoNewValue.sendKeys(valorStatus);
                System.out.println(BLUE + "Valor " + valorStatus + " digitado" + RESET);
                Thread.sleep(300);

                // Confirmar
                campoNewValue.sendKeys(Keys.ENTER);
                Thread.sleep(300);

                resultado.put("status", "sucesso");
                resultado.put("mensagem", "Configuração aplicada com sucesso");

            } else {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Campo para digitar não encontrado");
            }

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println(BLUE + "Driver finalizado" + RESET);
            }
        }

        return resultado;
    }
}