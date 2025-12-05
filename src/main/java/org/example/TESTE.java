package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TESTE {

    @Value("${app.username:admin}")
    private String username;

    @Value("${app.password:admin}")
    private String password;

    @PostMapping("/configurar-mtx1")
    public ResponseEntity<Map<String, Object>> configurarOffsetPotenciaMTX1(
            @RequestParam(defaultValue = "486") String potencia,
            @RequestParam(required = false) String canal) {

        Map<String, Object> resposta = new HashMap<>();

        try {
            System.out.println("=== Iniciando configuração MTX1 ===");
            System.out.println("Potência solicitada: " + potencia);
            if (canal != null) {
                System.out.println("Canal informado: " + canal);
            }

            // Executar a automação
            Map<String, Object> resultado = executarConfiguracaoMTX1(potencia, canal);

            if ("sucesso".equals(resultado.get("status"))) {
                resposta.put("status", "sucesso");
                resposta.put("mensagem", "Configuração MTX1 aplicada com sucesso");
                resposta.put("potencia_configurada", potencia);
                resposta.put("potencia_antes", resultado.get("potencia_antes"));
                resposta.put("potencia_depois", resultado.get("potencia_depois"));
                resposta.put("offset_final", resultado.get("offset_final"));
                resposta.put("corrente_final", resultado.get("corrente_final"));
                resposta.put("iteracoes", resultado.get("iteracoes"));

                return ResponseEntity.ok(resposta);
            } else {
                resposta.put("status", "erro");
                resposta.put("mensagem", resultado.get("mensagem"));
                resposta.put("potencia", potencia);
                if (canal != null) resposta.put("canal", canal);

                return ResponseEntity.status(500).body(resposta);
            }

        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro inesperado: " + e.getMessage());
            resposta.put("potencia", potencia);
            if (canal != null) resposta.put("canal", canal);

            return ResponseEntity.status(500).body(resposta);
        }
    }

    private Map<String, Object> executarConfiguracaoMTX1(String potencia, String canalParam) {
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

            // 1. Acessar a página e fazer login
            driver.get("http://10.10.103.103/debug/");
            System.out.println("Página acessada");

            fazerLogin(driver, wait);
            System.out.println("Login realizado");

            // 2. Obter canal atual (se não foi informado)
            String canal = canalParam;
            if (canal == null || canal.isEmpty()) {
                canal = verificarCanal(driver, wait);
                System.out.println("Canal detectado: " + canal);
            }

            // Validar se é um dos canais suportados
            if (!canal.equals("14") && !canal.equals("34") && !canal.equals("51")) {
                throw new Exception("Canal " + canal + " não suportado. Canais válidos: 14, 34, 51");
            }

            // 3. Desligar MTX1
            System.out.println("\n=== ETAPA 1: DESLIGANDO MTX1 ===");
            desligarMTX1(driver, wait);

            // 4. Configurar Offset inicial (15) e Potência (486)
            System.out.println("\n=== ETAPA 2: CONFIGURAÇÃO INICIAL ===");
            String potenciaAntes = configurarPotencia(driver, wait, potencia);
            configurarOffset(driver, wait, "15"); // Offset inicial sempre 15

            // 5. Ligar MTX1
            System.out.println("\n=== ETAPA 3: LIGANDO MTX1 ===");
            ligarMTX1(driver, wait);

            // Aguardar estabilização inicial
            Thread.sleep(5000);

            // 6. Executar rotina de ajuste de offset baseado na corrente
            System.out.println("\n=== ETAPA 4: AJUSTE DINÂMICO DO OFFSET ===");
            Map<String, Object> ajusteResultado = ajustarOffsetDinamico(driver, wait, canal);

            // 7. Verificar resultado final
            Thread.sleep(3000);
            String potenciaDepois = verificarPotencia(driver, wait);
            String correnteFinal = verificarCorrente(driver, wait);
            String canalFinal = verificarCanal(driver, wait);
            String offsetFinal = verificarOffset(driver, wait);

            // 8. Salvar log
            salvarLogCompleto(canal, potencia, potenciaAntes, potenciaDepois,
                    offsetFinal, correnteFinal, ajusteResultado);

            // Preparar resultado
            resultado.put("status", "sucesso");
            resultado.put("potencia_antes", potenciaAntes);
            resultado.put("potencia_depois", potenciaDepois);
            resultado.put("offset_final", offsetFinal);
            resultado.put("corrente_final", correnteFinal);
            resultado.put("canal_final", canalFinal);
            resultado.put("iteracoes", ajusteResultado.get("iteracoes"));
            resultado.put("offset_inicial", "15");

        } catch (Exception e) {
            System.err.println("Erro na execução: " + e.getMessage());
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

    private Map<String, Object> ajustarOffsetDinamico(WebDriver driver, WebDriverWait wait, String canal)
            throws Exception {

        Map<String, Object> resultado = new HashMap<>();
        int offsetAtual = 15; // Offset inicial
        int iteracoes = 0;
        int maxIteracoes = 20; // Prevenir loop infinito

        // Definir parâmetros baseado no canal
        int correnteMinima;
        int correnteMaximaErro;

        switch (canal) {
            case "14":
                correnteMinima = 70;
                correnteMaximaErro = 73;
                break;
            case "34":
                correnteMinima = 65;
                correnteMaximaErro = 68;
                break;
            case "51":
                correnteMinima = 60;
                correnteMaximaErro = 63;
                break;
            default:
                throw new Exception("Canal não suportado: " + canal);
        }

        System.out.println("Parâmetros para canal " + canal + ":");
        System.out.println("  Corrente mínima: " + correnteMinima + " mA");
        System.out.println("  Corrente máxima (erro): " + correnteMaximaErro + " mA");

        while (iteracoes < maxIteracoes) {
            iteracoes++;
            System.out.println("\n--- Iteração " + iteracoes + " ---");

            // 1. Verificar corrente atual
            Thread.sleep(10000); // 10 segundos
            String correnteStr = verificarCorrente(driver, wait);
            int corrente = (int) Double.parseDouble(correnteStr.replace(" mA", "").trim());

            System.out.println("Offset atual: " + offsetAtual);
            System.out.println("Corrente atual: " + corrente + " mA");

            // 2. Verificar se corrente atingiu o mínimo
            if (corrente >= correnteMinima) {
                System.out.println("Corrente atingiu o mínimo (" + correnteMinima + " mA)");

                // Verificar se não ultrapassou o limite máximo
                if (corrente > correnteMaximaErro) {
                    String erroMsg = "ERRO: Corrente " + corrente + " mA > " + correnteMaximaErro + " mA";
                    System.err.println(erroMsg);
                    resultado.put("status", "erro");
                    resultado.put("mensagem", erroMsg);
                    resultado.put("iteracoes", iteracoes);
                    return resultado;
                }

                // Sucesso: corrente dentro dos limites
                System.out.println("Sucesso: Corrente " + corrente + " mA está dentro dos limites");
                resultado.put("status", "sucesso");
                resultado.put("iteracoes", iteracoes);
                resultado.put("corrente_final", corrente);
                resultado.put("offset_final", offsetAtual);
                return resultado;
            }

            // 3. Se não atingiu, diminuir offset em 1
            System.out.println("Corrente abaixo do mínimo. Reduzindo offset...");
            offsetAtual--;

            if (offsetAtual < 0) {
                String erroMsg = "ERRO: Offset chegou a 0 e corrente ainda não atingiu " + correnteMinima + " mA";
                System.err.println(erroMsg);
                resultado.put("status", "erro");
                resultado.put("mensagem", erroMsg);
                resultado.put("iteracoes", iteracoes);
                return resultado;
            }

            // 4. Aplicar novo offset
            // Primeiro desligar MTX1
            desligarMTX1(driver, wait);

            // Configurar novo offset
            configurarOffset(driver, wait, String.valueOf(offsetAtual));

            // Ligar MTX1 novamente
            ligarMTX1(driver, wait);

            System.out.println("Novo offset aplicado: " + offsetAtual);
        }

        // Se chegou aqui, atingiu o máximo de iterações
        String erroMsg = "ERRO: Máximo de " + maxIteracoes + " iterações atingido";
        System.err.println(erroMsg);
        resultado.put("status", "erro");
        resultado.put("mensagem", erroMsg);
        resultado.put("iteracoes", iteracoes);
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

    // DESLIGAR MTX1
    private void desligarMTX1(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier1
        WebElement powerAmplifier1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier1___']/input")));
        powerAmplifier1.click();
        Thread.sleep(300);

        // Encontrar e configurar RfMasterOn para 2 (desligar)
        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier1.Config.RfMasterOn",
                "PowerAmplifier1_Config_RfMasterOn");

        if (rfMasterOn == null) {
            rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'RfMasterOn')]")));
        }

        if (!configurarValor(driver, rfMasterOn, "2")) {
            throw new Exception("Falha ao desligar RfMasterOn");
        }

        System.out.println("MTX1 desligado (RfMasterOn = 2)");
        Thread.sleep(300);
    }

    // LIGAR MTX1
    private void ligarMTX1(WebDriver driver, WebDriverWait wait) throws Exception {
        // Encontrar RfMasterOn novamente
        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier1.Config.RfMasterOn",
                "PowerAmplifier1_Config_RfMasterOn");

        if (!configurarValor(driver, rfMasterOn, "1")) {
            throw new Exception("Falha ao ligar RfMasterOn");
        }

        System.out.println("MTX1 ligado (RfMasterOn = 1)");
        Thread.sleep(300);
    }

    // CONFIGURAR OFFSET
    private void configurarOffset(WebDriver driver, WebDriverWait wait, String valorOffset) throws Exception {
        // Navegar para Internal1
        WebElement internal1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Internal1___']/input")));
        internal1.click();
        Thread.sleep(300);

        // Encontrar e configurar offset
        WebElement offset = encontrarElementoComTentativas(wait,
                "Internal1.power.offset",
                "Internal1_power_offset");

        if (offset == null) {
            offset = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'offset')]")));
        }

        if (!configurarValor(driver, offset, valorOffset)) {
            throw new Exception("Falha ao configurar offset para " + valorOffset);
        }

        System.out.println("Offset configurado para: " + valorOffset);
        Thread.sleep(300);
    }

    // CONFIGURAR POTÊNCIA
    private String configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
        // Navegar para PowerAmplifier1
        WebElement powerAmplifier1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier1___']/input")));
        powerAmplifier1.click();
        Thread.sleep(300);

        // Ler potência atual
        WebElement potenciaAtualElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier1_Status_ForwardPower")));
        String potenciaAntes = potenciaAtualElement.getText().trim();
        System.out.println("Potência atual: " + potenciaAntes);

        // Encontrar OutputPower
        WebElement outputPower = encontrarElementoComTentativas(wait,
                "PowerAmplifier1.Config.OutputPower",
                "PowerAmplifier1_Config_OutputPower");

        if (outputPower == null) {
            outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(text(), 'OutputPower')]")));
        }

        if (!configurarValor(driver, outputPower, potencia)) {
            throw new Exception("Falha ao configurar potência");
        }

        System.out.println("Potência configurada para: " + potencia);
        Thread.sleep(300);

        return potenciaAntes;
    }

    // VERIFICAR OFFSET (nova função)
    private String verificarOffset(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para Internal1
        WebElement internal1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Internal1___']/input")));
        internal1.click();
        Thread.sleep(300);

        // Encontrar elemento do offset
        WebElement offsetElement = encontrarElementoComTentativas(wait,
                "Internal1.power.offset",
                "Internal1_power_offset");

        if (offsetElement == null) {
            offsetElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(@id, 'offset')]")));
        }

        String offset = offsetElement.getText().trim();
        if (offset.isEmpty()) {
            offset = offsetElement.getAttribute("value");
        }

        System.out.println("Offset atual: " + offset);
        return offset != null ? offset.trim() : "N/A";
    }

    // VERIFICAR POTÊNCIA
    private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier1
        WebElement powerAmplifier1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier1___']/input")));
        powerAmplifier1.click();
        Thread.sleep(300);

        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier1_Status_ForwardPower")));
        String potencia = potenciaElement.getText().trim();

        System.out.println("Potência: " + potencia);
        return potencia;
    }

    // VERIFICAR CANAL
    private String verificarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para Modulator1
        WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator1___']/input")));
        modulator1.click();
        Thread.sleep(300);

        WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator1_Config_UpConverter_ChannelNumber")));
        String canal = canalElement.getText().trim();
        if (canal.isEmpty()) {
            canal = canalElement.getAttribute("value");
        }

        System.out.println("Canal: " + canal);
        return canal != null ? canal.trim() : "N/A";
    }

    // VERIFICAR CORRENTE
    private String verificarCorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para Modulator1
        WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator1___']/input")));
        modulator1.click();
        Thread.sleep(300);

        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator1_mtrMainCurr")));
        String corrente = correnteElement.getText().trim();

        System.out.println("Corrente: " + corrente);
        return corrente;
    }

    // CONFIGURAR VALOR
    private boolean configurarValor(WebDriver driver, WebElement elemento, String novoValor) {
        try {
            elemento.click();
            Thread.sleep(300);

            WebElement campoInput = driver.switchTo().activeElement();
            if (campoInput.getTagName().equals("input") || campoInput.getTagName().equals("textarea")) {
                campoInput.click();
                Thread.sleep(300);
                campoInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                campoInput.sendKeys(Keys.DELETE);
                campoInput.sendKeys(novoValor);
                Thread.sleep(300);
                campoInput.sendKeys(Keys.ENTER);
                Thread.sleep(300);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erro ao configurar valor: " + e.getMessage());
        }
        return false;
    }

    // ENCONTRAR ELEMENTO
    private WebElement encontrarElementoComTentativas(WebDriverWait wait, String... ids) {
        for (String id : ids) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
            } catch (Exception e) {
                // Continua tentando o próximo ID
            }
        }
        return null;
    }

    // SALVAR LOG COMPLETO
    private void salvarLogCompleto(String canal, String potenciaConfigurada, String potenciaAntes, String potenciaDepois, String offsetFinal, String correnteFinal, Map<String, Object> ajusteResultado) {
        String filePath = System.getProperty("user.dir") + "/logs_ajuste_offset.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | Canal: " + canal +
                    " | Potência Config: " + potenciaConfigurada +
                    " | Antes: " + potenciaAntes +
                    " | Depois: " + potenciaDepois +
                    " | Offset Final: " + offsetFinal +
                    " | Corrente Final: " + correnteFinal +
                    " | Iterações: " + ajusteResultado.get("iteracoes") +
                    " | Status: " + ajusteResultado.get("status") + "\n");
            System.out.println("Log salvo em: " + filePath);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }

    @PostMapping("/rotina-offset")
    public ResponseEntity<Map<String, Object>> rotinaOffset(@RequestParam String canal) {
        // Este endpoint será chamado pela RotinaPrincipal
        // Potência fixa em 486 conforme especificado
        return configurarOffsetPotenciaMTX1("486", canal);
    }
}