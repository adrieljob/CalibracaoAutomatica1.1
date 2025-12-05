/*
package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RotinaPrincipal {

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @Value("${app.ajustar-offset-url:http://localhost:8080}")
    private String ajustarOffsetBaseUrl;

    @PostMapping("/executar-rotina-completa")
    public ResponseEntity<Map<String, Object>> executarRotinaCompleta() {
        Map<String, Object> respostaGeral = new HashMap<>();
        Map<String, Object> resultados = new HashMap<>();

        try {
            System.out.println("=== Iniciando Rotina Completa ===");
            System.out.println("Hora de início: " + LocalDateTime.now());

            // 1. Mudar para canal 14 e executar AjustarOffSet1
            System.out.println("\n=== FASE 1: Canal 14 ===");
            Map<String, Object> resultadoCanal14 = executarSequenciaParaCanal("14");
            resultados.put("canal_14", resultadoCanal14);

            if (!"sucesso".equals(resultadoCanal14.get("status"))) {
                throw new RuntimeException("Falha na fase do canal 14: " + resultadoCanal14.get("mensagem"));
            }

            // 2. Mudar para canal 34 e executar AjustarOffSet1
            System.out.println("\n=== FASE 2: Canal 34 ===");
            Map<String, Object> resultadoCanal34 = executarSequenciaParaCanal("34");
            resultados.put("canal_34", resultadoCanal34);

            if (!"sucesso".equals(resultadoCanal34.get("status"))) {
                throw new RuntimeException("Falha na fase do canal 34: " + resultadoCanal34.get("mensagem"));
            }

            // 3. Mudar para canal 51 e executar AjustarOffSet1
            System.out.println("\n=== FASE 3: Canal 51 ===");
            Map<String, Object> resultadoCanal51 = executarSequenciaParaCanal("51");
            resultados.put("canal_51", resultadoCanal51);

            if (!"sucesso".equals(resultadoCanal51.get("status"))) {
                throw new RuntimeException("Falha na fase do canal 51: " + resultadoCanal51.get("mensagem"));
            }

            // Resposta final
            respostaGeral.put("status", "sucesso");
            respostaGeral.put("mensagem", "Rotina completa executada com sucesso");
            respostaGeral.put("hora_inicio", LocalDateTime.now());
            respostaGeral.put("hora_fim", LocalDateTime.now());
            respostaGeral.put("resultados", resultados);

            System.out.println("\n=== Rotina Completa Finalizada ===");
            System.out.println("Hora de fim: " + LocalDateTime.now());

            return ResponseEntity.ok(respostaGeral);

        } catch (Exception e) {
            respostaGeral.put("status", "erro");
            respostaGeral.put("mensagem", "Erro na rotina completa: " + e.getMessage());
            respostaGeral.put("hora_inicio", LocalDateTime.now());
            respostaGeral.put("resultados", resultados);

            System.err.println("Erro na rotina completa: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(respostaGeral);
        }
    }

    private Map<String, Object> executarSequenciaParaCanal(String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // 1. Mudar o canal
            System.out.println("1. Mudando canal para: " + canal);
            Map<String, Object> resultadoMudanca = mudarCanal(canal);

            if (!"sucesso".equals(resultadoMudanca.get("status"))) {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Falha ao mudar canal: " + resultadoMudanca.get("mensagem"));
                return resultado;
            }

            resultado.put("mudanca_canal", resultadoMudanca);

            // 2. Aguardar estabilização após mudança de canal
            System.out.println("Aguardando 5 segundos após mudança de canal...");
            Thread.sleep(5000);

            // 3. Chamar AjustarOffSet1
            System.out.println("2. Chamando AjustarOffSet1 para canal " + canal);
            Map<String, Object> resultadoOffset = chamarAjustarOffSet1ViaAPI(canal);

            if (!"sucesso".equals(resultadoOffset.get("status"))) {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Falha no AjustarOffSet1: " + resultadoOffset.get("mensagem"));
                return resultado;
            }

            resultado.put("ajustar_offset", resultadoOffset);
            resultado.put("status", "sucesso");
            resultado.put("mensagem", "Sequência concluída para canal " + canal);

            // 4. Aguardar entre canais (opcional)
            System.out.println("Aguardando 10 segundos antes do próximo canal...");
            Thread.sleep(10000);

        } catch (Exception e) {
            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro na sequência do canal " + canal + ": " + e.getMessage());
        }

        return resultado;
    }

    // Método para chamar via API REST (RECOMENDADO)
    private Map<String, Object> chamarAjustarOffSet1ViaAPI(String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            RestTemplate restTemplate = new RestTemplate();

            // Usar o endpoint /rotina-offset que criamos no AjustarOffSet1
            String url = ajustarOffsetBaseUrl + "/rotina-offset?canal=" + canal;

            System.out.println("Chamando: " + url);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                resultado.put("status", "sucesso");
                resultado.put("resposta", responseBody);
                resultado.put("mensagem", "AjustarOffSet1 executado com sucesso");
            } else {
                resultado.put("status", "erro");
                resultado.put("mensagem", "AjustarOffSet1 retornou erro: " + response.getStatusCode());
            }

        } catch (Exception e) {
            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro ao chamar AjustarOffSet1: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }

    // Método para chamar via injeção de dependência (ALTERNATIVA - se estiverem no mesmo contexto Spring)
    /*
    @Autowired
    private AjustarOffSet1 ajustarOffSet1;

    private Map<String, Object> chamarAjustarOffSet1Direto(String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // Chamar o endpoint usando injeção de dependência
            ResponseEntity<Map<String, Object>> response =
                ajustarOffSet1.rotinaOffset(canal);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                resultado.put("status", "sucesso");
                resultado.put("resposta", responseBody);
            } else {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Erro: " + response.getStatusCode());
            }

        } catch (Exception e) {
            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro: " + e.getMessage());
        }

        return resultado;
    }


    // MÉTODOS DE MUDANÇA DE CANAL (mantidos do seu código)
    private Map<String, Object> mudarCanal(String canal) {
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
            System.out.println("Página acessada");

            // 2. Fazer login
            fazerLogin(driver, wait);
            System.out.println("Login realizado");

            // 3. Acessar Modulator 1
            acessarModulator1(driver, wait);
            System.out.println("Modulator1 acessado");

            // 4. Pegar canal atual (antes)
            String canalAntes = obterCanalAtual(driver, wait);
            System.out.println("Canal atual: " + canalAntes);

            // 5. Clicar para editar o canal
            clicarParaEditarCanal(driver, wait);
            System.out.println("Campo de canal clicado");

            // 6. Aguardar e preencher o diálogo
            Thread.sleep(300);
            preencherNovoCanal(driver, wait, canal);
            System.out.println("Novo canal preenchido: " + canal);

            // 7. Confirmar
            confirmarMudanca(driver, wait);
            System.out.println("Mudança confirmada");

            // 8. Aguardar aplicação
            Thread.sleep(2000);

            // 9. Verificar canal depois
            String canalDepois = obterCanalAtual(driver, wait);
            System.out.println("Canal depois: " + canalDepois);

            // 10. Salvar log
            salvarLogMudancaCanal(canal, canalAntes, canalDepois);

            resultado.put("status", "sucesso");
            resultado.put("canal_antes", canalAntes);
            resultado.put("canal_depois", canalDepois);
            resultado.put("canal_solicitado", canal);

        } catch (Exception e) {
            System.err.println("Erro ao mudar canal: " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Driver finalizado");
            }
        }

        return resultado;
    }

    // MÉTODOS AUXILIARES PARA MUDANÇA DE CANAL
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

    private void acessarModulator1(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator1___']/input")));
        modulator1.click();
        Thread.sleep(300);
    }

    private String obterCanalAtual(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            WebElement canalElement = driver.findElement(
                    By.id("Modulator1_Config_UpConverter_ChannelNumber"));

            String canal = canalElement.getText();
            if (canal == null || canal.trim().isEmpty()) {
                canal = canalElement.getAttribute("value");
            }

            return canal != null ? canal.trim() : "N/A";

        } catch (Exception e) {
            return "N/A";
        }
    }

    private void clicarParaEditarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("Modulator1_Config_UpConverter_ChannelNumber")));

        new org.openqa.selenium.interactions.Actions(driver)
                .doubleClick(canalElement)
                .perform();

        Thread.sleep(300);
    }

    private void preencherNovoCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        WebElement activeElement = driver.switchTo().activeElement();

        if (activeElement.getTagName().equals("input")) {
            activeElement.clear();
            Thread.sleep(300);
            activeElement.sendKeys(canal);
        } else {
            WebElement inputField = driver.findElement(
                    By.xpath("//input[@type='text' or @type='number']"));
            inputField.clear();
            Thread.sleep(300);
            inputField.sendKeys(canal);
        }
    }

    private void confirmarMudanca(WebDriver driver, WebDriverWait wait) throws Exception {
        new org.openqa.selenium.interactions.Actions(driver)
                .sendKeys(Keys.ENTER)
                .perform();

        Thread.sleep(300);

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

    private void salvarLogMudancaCanal(String canal, String antes, String depois) {
        String filePath = System.getProperty("user.dir") + "/logs_rotina_completa.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | [MUDANÇA_CANAL] | Canal: " + canal +
                    " | Antes: " + antes +
                    " | Depois: " + depois + "\n");
            System.out.println("Log de mudança de canal salvo");
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }

    // Endpoint individual para cada canal (opcional)
    @PostMapping("/executar-para-canal")
    public ResponseEntity<Map<String, Object>> executarParaCanal(@RequestParam String canal) {
        Map<String, Object> resultado = executarSequenciaParaCanal(canal);

        if ("sucesso".equals(resultado.get("status"))) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(500).body(resultado);
        }
    }

    // Endpoint para testar apenas mudança de canal
    @PostMapping("/mudar-canal")
    public ResponseEntity<Map<String, Object>> mudarCanalEndpoint(@RequestParam String canal) {
        Map<String, Object> resultado = mudarCanal(canal);

        if ("sucesso".equals(resultado.get("status"))) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(500).body(resultado);
        }
    }
}

 */