package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RotinaparaOffsetMtx4 {

    private static final String RESET = "\u001B[0m";
    private static final String MAGENTA = "\u001B[35m";

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @PostMapping("/mudar-canal-mtx4")
    public ResponseEntity<Map<String, Object>> mudarCanalMTX4(@RequestParam String canal) {
        Map<String, Object> resposta = new HashMap<>();

        try {
            System.out.println(MAGENTA + "=== Iniciando mudança de canal MTX4 ===" + RESET);
            System.out.println(MAGENTA + "Canal solicitado: " + canal + RESET);

            // Validar canal
            if (!canal.matches("\\d+")) {
                resposta.put("status", "erro");
                resposta.put("mensagem", "Canal inválido. Deve ser um número.");
                return ResponseEntity.badRequest().body(resposta);
            }

            // Executar a automação
            Map<String, Object> resultado = executarMudancaCanal(canal);

            if ("sucesso".equals(resultado.get("status"))) {
                resposta.put("status", "sucesso");
                resposta.put("mensagem", "Canal configurado com sucesso");
                resposta.put("canal", canal);
                resposta.put("canal_antes", resultado.get("canal_antes"));
                resposta.put("canal_depois", resultado.get("canal_depois"));

                return ResponseEntity.ok(resposta);
            } else {
                resposta.put("status", "erro");
                resposta.put("mensagem", resultado.get("mensagem"));
                resposta.put("canal", canal);

                return ResponseEntity.status(500).body(resposta);
            }

        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro inesperado: " + e.getMessage());
            resposta.put("canal", canal);

            return ResponseEntity.status(500).body(resposta);
        }
    }

    // FUNÇÃO "PRINCIPAL"
    private Map<String, Object> executarMudancaCanal(String canal) {
        Map<String, Object> resultado = new HashMap<>();
        WebDriver driver = null;

        try {
            // Configurar ChromeDriver
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-gpu");
            // options.addArguments("--headless"); // Descomente para modo headless
            options.addArguments("--incognito");
            options.addArguments("--disable-cache");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // 1. Acessar a página
            driver.get("http://10.10.103.103/debug/");
            System.out.println(MAGENTA + "Página acessada" + RESET);

            // 2. Fazer login
            fazerLogin(driver, wait);
            System.out.println(MAGENTA + "Login realizado" + RESET);

            // 3. Acessar Modulator 4
            acessarModulator4(driver, wait);
            System.out.println(MAGENTA + "Modulator4 acessado" + RESET);

            // 4. Pegar canal atual (antes)
            String canalAntes = obterCanalAtual(driver, wait);
            System.out.println(MAGENTA + "Canal atual: " + canalAntes + RESET);

            // 5. Clicar para editar o canal
            clicarParaEditarCanal(driver, wait);
            System.out.println(MAGENTA + "Campo de canal clicado" + RESET);

            // 6. Aguardar e preencher o diálogo
            Thread.sleep(300);
            preencherNovoCanal(driver, wait, canal);
            System.out.println(MAGENTA + "Novo canal preenchido: " + canal + RESET);

            // 7. Confirmar
            confirmarMudancaCanal(driver, wait);
            System.out.println(MAGENTA + "Mudança confirmada" + RESET);

            // 8. Aguardar aplicação
            //Thread.sleep(2000);

            // 9. Verificar canal depois
            String canalDepois = obterCanalAtual(driver, wait);
            System.out.println(MAGENTA + "Canal depois: " + canalDepois + RESET);

            // 10. Salvar log
            salvarLog(canal, canalAntes, canalDepois);

            resultado.put("status", "sucesso");
            resultado.put("canal_antes", canalAntes);
            resultado.put("canal_depois", canalDepois);

        } catch (Exception e) {
            System.err.println("Erro na execução: " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println(MAGENTA + "Driver finalizado" + RESET);
            }
        }

        return resultado;
    }

    // FAZER LOGIN
    private void fazerLogin(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement usernameField = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='text']")));
        usernameField.clear();
        usernameField.sendKeys(username);
        Thread.sleep(300);

        WebElement passwordField = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='password']")));
        passwordField.clear();
        passwordField.sendKeys(password);
        Thread.sleep(300);

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[text()='Sign In']")));
        loginButton.click();
        Thread.sleep(300);
    }

    // ACESSAR MODULADOR
    private void acessarModulator4(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement modulator4 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator4___']/input")));
        modulator4.click();
        Thread.sleep(300);
    }

    // ACESSAR POWERAMP
    private void acessarPowerAmp4(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement modulator4 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier4___']/input")));
        modulator4.click();
        Thread.sleep(300);
    }

    // ACESSAR INTERNAL
    private void acessarInternal4(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement modulator4 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Internal4___']/input")));
        modulator4.click();
        Thread.sleep(300);
    }

    // COMBO PARA FAZER A MUDANÇA DE VALORES DO CANAL
    // ETAPA 1
    private String obterCanalAtual(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Tentar diferentes formas de obter o canal
            WebElement canalElement = driver.findElement(
                    By.id("Modulator4_Config_UpConverter_ChannelNumber"));

            String canal = canalElement.getText();
            if (canal == null || canal.trim().isEmpty()) {
                canal = canalElement.getAttribute("value");
            }

            return canal != null ? canal.trim() : "N/A";

        } catch (Exception e) {
            return "N/A";
        }
    }

    // ETAPA 2
    private void clicarParaEditarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        // Tentar clicar no elemento do canal
        WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("Modulator4_Config_UpConverter_ChannelNumber")));

        // Dar duplo clique para editar
        new org.openqa.selenium.interactions.Actions(driver)
                .doubleClick(canalElement)
                .perform();

        Thread.sleep(300);
    }

    // ETAPA 3
    private void preencherNovoCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        // Procurar campo de input ativo (diálogo deve estar aberto)
        WebElement activeElement = driver.switchTo().activeElement();

        if (activeElement.getTagName().equals("input")) {
            // Limpar e preencher
            activeElement.clear();
            Thread.sleep(300);
            activeElement.sendKeys(canal);
        } else {
            // Alternativa: procurar por qualquer input
            WebElement inputField = driver.findElement(
                    By.xpath("//input[@type='text' or @type='number']"));
            inputField.clear();
            Thread.sleep(300);
            inputField.sendKeys(canal);
        }
    }

    // ETAPA 4
    private void confirmarMudancaCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        // Pressionar Enter
        new org.openqa.selenium.interactions.Actions(driver)
                .sendKeys(Keys.ENTER)
                .perform();

        Thread.sleep(300);

        // Tentar clicar em OK se existir
        try {
            WebElement okButton = driver.findElement(
                    By.xpath("//button[contains(text(), 'OK') or contains(text(), 'Apply')]"));
            if (okButton.isDisplayed()) {
                okButton.click();
            }
        } catch (Exception e) {
            // Ignorar se não encontrar botão OK
        }
    }

    // COMBO PARA FAZER A MUDANÇA DE VALORES DO OFFSET
    // ETAPA 1
    private String obterOffSetAtual(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Tentar diferentes formas de obter o canal
            WebElement canalElement = driver.findElement(
                    By.id("Internal4_power_offset"));

            String canal = canalElement.getText();
            if (canal == null || canal.trim().isEmpty()) {
                canal = canalElement.getAttribute("value");
            }

            return canal != null ? canal.trim() : "N/A";

        } catch (Exception e) {
            return "N/A";
        }
    }

    // ETAPA 2
    private void clicarParaEditarOffSet(WebDriver driver, WebDriverWait wait) throws Exception {
        // Tentar clicar no elemento do canal
        WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("Internal4_power_offset")));

        // Dar duplo clique para editar
        new org.openqa.selenium.interactions.Actions(driver)
                .doubleClick(canalElement)
                .perform();

        Thread.sleep(300);
    }

    // ETAPA 3
    private void preencherNovoOffSet(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        // Procurar campo de input ativo (diálogo deve estar aberto)
        WebElement activeElement = driver.switchTo().activeElement();

        if (activeElement.getTagName().equals("input")) {
            // Limpar e preencher
            activeElement.clear();
            Thread.sleep(300);
            activeElement.sendKeys(canal);
        } else {
            // Alternativa: procurar por qualquer input
            WebElement inputField = driver.findElement(
                    By.xpath("//input[@type='text' or @type='number']"));
            inputField.clear();
            Thread.sleep(300);
            inputField.sendKeys(canal);
        }
    }

    // ETAPA 4
    private void confirmarMudancaOffSet(WebDriver driver, WebDriverWait wait) throws Exception {
        // Pressionar Enter
        new org.openqa.selenium.interactions.Actions(driver)
                .sendKeys(Keys.ENTER)
                .perform();

        Thread.sleep(300);

        // Tentar clicar em OK se existir
        try {
            WebElement okButton = driver.findElement(
                    By.xpath("//button[contains(text(), 'OK') or contains(text(), 'Apply')]"));
            if (okButton.isDisplayed()) {
                okButton.click();
            }
        } catch (Exception e) {
            // Ignorar se não encontrar botão OK
        }
    }

    // COMBO PARA FAZER A MUDANÇA DE VALORES DA POTENCIA
    // ETAPA 1
    private String obterPotenciaAtual(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Tentar diferentes formas de obter o canal
            WebElement canalElement = driver.findElement(
                    By.id("PowerAmplifier4_Config_OutputPower"));

            String canal = canalElement.getText();
            if (canal == null || canal.trim().isEmpty()) {
                canal = canalElement.getAttribute("value");
            }

            return canal != null ? canal.trim() : "N/A";

        } catch (Exception e) {
            return "N/A";
        }
    }

    // ETAPA 2
    private void clicarParaEditarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        // Tentar clicar no elemento do canal
        WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("PowerAmplifier4_Config_OutputPower")));

        // Dar duplo clique para editar
        new org.openqa.selenium.interactions.Actions(driver)
                .doubleClick(canalElement)
                .perform();

        Thread.sleep(300);
    }

    // ETAPA 3
    private void preencherNovaPotencia(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        // Procurar campo de input ativo (diálogo deve estar aberto)
        WebElement activeElement = driver.switchTo().activeElement();

        if (activeElement.getTagName().equals("input")) {
            // Limpar e preencher
            activeElement.clear();
            Thread.sleep(300);
            activeElement.sendKeys(canal);
        } else {
            // Alternativa: procurar por qualquer input
            WebElement inputField = driver.findElement(
                    By.xpath("//input[@type='text' or @type='number']"));
            inputField.clear();
            Thread.sleep(300);
            inputField.sendKeys(canal);
        }
    }

    // ETAPA 4
    private void confirmarMudancaPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        // Pressionar Enter
        new org.openqa.selenium.interactions.Actions(driver)
                .sendKeys(Keys.ENTER)
                .perform();

        Thread.sleep(300);

        // Tentar clicar em OK se existir
        try {
            WebElement okButton = driver.findElement(
                    By.xpath("//button[contains(text(), 'OK') or contains(text(), 'Apply')]"));
            if (okButton.isDisplayed()) {
                okButton.click();
            }
        } catch (Exception e) {
            // Ignorar se não encontrar botão OK
        }
    }

    // COMBO PARA FAZER A MUDANÇA DE VALORES DO RFMASTER
    // ETAPA 1
    private String obterRfMasterAtual(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Tentar diferentes formas de obter o canal
            WebElement canalElement = driver.findElement(
                    By.id("PowerAmplifier4_Config_RfMasterOn"));

            String canal = canalElement.getText();
            if (canal == null || canal.trim().isEmpty()) {
                canal = canalElement.getAttribute("value");
            }

            return canal != null ? canal.trim() : "N/A";

        } catch (Exception e) {
            return "N/A";
        }
    }

    // ETAPA 2
    private void clicarParaEditarRfMaster(WebDriver driver, WebDriverWait wait) throws Exception {
        // Tentar clicar no elemento do canal
        WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("PowerAmplifier4_Config_RfMasterOn")));

        // Dar duplo clique para editar
        new org.openqa.selenium.interactions.Actions(driver)
                .doubleClick(canalElement)
                .perform();

        Thread.sleep(300);
    }

    // ETAPA 3
    private void preencherNovoRfMaster(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        // Procurar campo de input ativo (diálogo deve estar aberto)
        WebElement activeElement = driver.switchTo().activeElement();

        if (activeElement.getTagName().equals("input")) {
            // Limpar e preencher
            activeElement.clear();
            Thread.sleep(300);
            activeElement.sendKeys(canal);
        } else {
            // Alternativa: procurar por qualquer input
            WebElement inputField = driver.findElement(
                    By.xpath("//input[@type='text' or @type='number']"));
            inputField.clear();
            Thread.sleep(300);
            inputField.sendKeys(canal);
        }
    }

    // ETAPA 4
    private void confirmarMudancaRfMaster(WebDriver driver, WebDriverWait wait) throws Exception {
        // Pressionar Enter
        new org.openqa.selenium.interactions.Actions(driver)
                .sendKeys(Keys.ENTER)
                .perform();

        Thread.sleep(300);

        // Tentar clicar em OK se existir
        try {
            WebElement okButton = driver.findElement(
                    By.xpath("//button[contains(text(), 'OK') or contains(text(), 'Apply')]"));
            if (okButton.isDisplayed()) {
                okButton.click();
            }
        } catch (Exception e) {
            // Ignorar se não encontrar botão OK
        }
    }


    private void salvarLog(String canal, String antes, String depois) {
        String filePath = System.getProperty("user.dir") + "/logs_canal_mtx4.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | Canal: " + canal +
                    " | Antes: " + antes +
                    " | Depois: " + depois + "\n");
            System.out.println(MAGENTA + "Log salvo em: " + filePath + RESET);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }
}