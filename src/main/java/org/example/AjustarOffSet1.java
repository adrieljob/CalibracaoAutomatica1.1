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
public class AjustarOffSet1 {

    @Value("${app.username:admin}")
    private String username;

    @Value("${app.password:admin}")
    private String password;

    @PostMapping("/executar-rotina-completa")
    public ResponseEntity<Map<String, Object>> executarRotinaCompleta() {
        Map<String, Object> respostaGeral = new HashMap<>();
        Map<String, Object> resultados = new HashMap<>();
        WebDriver driver = null;

        try {
            System.out.println("=== INICIANDO ROTINA COMPLETA ===");
            System.out.println("Hora de início: " + LocalDateTime.now());

            // Configurar ChromeDriver UMA VEZ para toda a rotina
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

            // ETAPA 1: LOGIN (apenas uma vez)
            driver.get("http://10.10.103.103/debug/");
            System.out.println("Página acessada");
            fazerLogin(driver, wait);
            System.out.println("Login realizado");

            // Processar cada canal
            String[] canais = {"14", "34", "51"};

            for (int i = 0; i < canais.length; i++) {
                String canal = canais[i];

                System.out.println("\n" + "=".repeat(50));
                System.out.println("PROCESSANDO CANAL: " + canal);
                System.out.println("=".repeat(50));

                // Executar sequência completa para este canal
                Map<String, Object> resultadoCanal = processarCanalCompleto(driver, wait, canal);
                resultados.put("canal_" + canal, resultadoCanal);

                if (!"sucesso".equals(resultadoCanal.get("status"))) {
                    throw new RuntimeException("Falha no canal " + canal + ": " + resultadoCanal.get("mensagem"));
                }

                // Aguardar entre canais (exceto após o último)
                if (i < canais.length - 1) {
                    System.out.println("\nAguardando 10 segundos antes do próximo canal...");
                    Thread.sleep(10000);
                }
            }

            // Resposta final - CONVERTENDO LocalDateTime para String
            respostaGeral.put("status", "sucesso");
            respostaGeral.put("mensagem", "Rotina completa executada com sucesso");
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString()); // Convertendo para String
            respostaGeral.put("hora_fim", LocalDateTime.now().toString()); // Convertendo para String
            respostaGeral.put("resultados", resultados);

            System.out.println("\n=== ROTINA COMPLETA FINALIZADA ===");
            System.out.println("Hora de fim: " + LocalDateTime.now());

            return ResponseEntity.ok(respostaGeral);

        } catch (Exception e) {
            respostaGeral.put("status", "erro");
            respostaGeral.put("mensagem", "Erro na rotina completa: " + e.getMessage());
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString()); // Convertendo para String
            respostaGeral.put("resultados", resultados);

            System.err.println("Erro na rotina completa: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(respostaGeral);
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Driver finalizado");
            }
        }
    }

    private Map<String, Object> processarCanalCompleto(WebDriver driver, WebDriverWait wait, String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // ========== ETAPA 1: MUDAR CANAL ==========
            System.out.println("\n[ETAPA 1] Mudando para canal: " + canal);
            String canalAntes = mudarCanal(driver, wait, canal);
            Thread.sleep(2000); // Aguardar mudança de canal

            // ========== ETAPA 2: AJUSTAR OFFSET (parte inicial) ==========
            System.out.println("\n[ETAPA 2] Configurando offset e potência");

            // 2.1. Desligar o MTX1
            System.out.println("  2.1. Desligando MTX1");
            desligarMTX1(driver, wait);

            // 2.2. Mudar offset para 15
            System.out.println("  2.2. Configurando offset para 15");
            configurarOffset(driver, wait, "15");

            // 2.3. Mudar potência para 486
            System.out.println("  2.3. Configurando potência para 486");
            configurarPotencia(driver, wait, "486");

            // 2.4. Ligar o MTX1
            System.out.println("  2.4. Ligando MTX1");
            ligarMTX1(driver, wait);

            // 2.5. Esperar 5 minutos
            System.out.println("  2.5. Aguardando 5 minutos para estabilização...");
            Thread.sleep(300000); // 5 minutos = 300000 ms

            // ========== ETAPA 3: VERIFICAR E AJUSTAR DINAMICAMENTE ==========
            System.out.println("\n[ETAPA 3] Verificando e ajustando dinamicamente");

            // 2.6. Checa o canal (apenas para confirmar)
            String canalAtual = verificarCanal(driver, wait);
            System.out.println("  Canal atual: " + canalAtual);

            // Verificação FLEXÍVEL do canal
            if (!canalAtual.equals(canal) && !canalAtual.contains(canal)) {
                System.err.println("  AVISO: Canal lido (" + canalAtual + ") diferente do esperado (" + canal + ")");
                // Não lançar exceção, apenas logar o aviso
            }

            // Executar ajuste dinâmico baseado no canal
            Map<String, Object> resultadoAjuste = executarAjusteDinamicoPorCanal(driver, wait, canal);

            if (!"sucesso".equals(resultadoAjuste.get("status"))) {
                throw new Exception("Falha no ajuste dinâmico: " + resultadoAjuste.get("mensagem"));
            }

            // ========== ETAPA 4: COLETAR RESULTADOS FINAIS ==========
            System.out.println("\n[ETAPA 4] Coletando resultados finais");

            String potenciaFinal = verificarPotencia(driver, wait);
            String correnteFinal = resultadoAjuste.get("corrente_final").toString();
            String offsetFinal = resultadoAjuste.get("offset_final").toString();
            String canalFinal = verificarCanal(driver, wait);

            // ========== ETAPA 5: SALVAR LOG ==========
            salvarLogCanal(canal, canalAntes, canalFinal, potenciaFinal,
                    correnteFinal, offsetFinal, resultadoAjuste);

            // ========== PREPARAR RESPOSTA ==========
            resultado.put("status", "sucesso");
            resultado.put("mensagem", "Canal " + canal + " processado com sucesso");
            resultado.put("canal_antes", canalAntes);
            resultado.put("canal_depois", canalFinal);
            resultado.put("potencia_final", potenciaFinal);
            resultado.put("corrente_final", correnteFinal);
            resultado.put("offset_final", offsetFinal);
            resultado.put("iteracoes", resultadoAjuste.get("iteracoes"));
            resultado.put("offset_inicial", "15");

        } catch (Exception e) {
            System.err.println("Erro no processamento do canal " + canal + ": " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro no canal " + canal + ": " + e.getMessage());
        }

        return resultado;
    }

    private String verificarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Navegar para Modulator1
            WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Modulator1___']/input")));
            modulator1.click();
            Thread.sleep(300);

            // Tentar várias estratégias para pegar o valor do canal
            WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("Modulator1_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);

            // Estratégia 1: Pegar o texto diretamente
            String canalTexto = canalElement.getText().trim();
            System.out.println("    Texto do elemento canal: " + canalTexto);

            // Estratégia 2: Se não conseguir, pegar o value attribute
            if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator1.Config.UpConverter.ChannelNumber")) {
                canalTexto = canalElement.getAttribute("value");
                System.out.println("    Value attribute do canal: " + canalTexto);
            }

            // Estratégia 3: Se ainda não, tentar innerText
            if (canalTexto == null || canalTexto.isEmpty()) {
                canalTexto = canalElement.getAttribute("innerText");
                System.out.println("    InnerText do canal: " + canalTexto);
            }

            // Extrair números do texto (pode vir como "Modulator1.Config.UpConverter.ChannelNumber = 14")
            if (canalTexto != null && !canalTexto.isEmpty()) {
                // Se tiver formato "Modulator1.Config.UpConverter.ChannelNumber = 14"
                if (canalTexto.contains("=")) {
                    String[] partes = canalTexto.split("=");
                    if (partes.length > 1) {
                        canalTexto = partes[1].trim();
                    }
                }

                // Remover qualquer caractere não numérico
                String numeros = canalTexto.replaceAll("[^0-9]", "").trim();

                if (!numeros.isEmpty()) {
                    System.out.println("    Canal extraído: " + numeros);
                    return numeros;
                }
            }

            System.out.println("    Não conseguiu extrair canal, retornando N/A");
            return "N/A";

        } catch (Exception e) {
            System.err.println("    Erro ao verificar canal: " + e.getMessage());
            return "N/A";
        }
    }

    private String verificarCorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para Modulator1
        WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator1___']/input")));
        modulator1.click();
        Thread.sleep(300);

        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator1_mtrMainCurr")));

        String textoCompleto = correnteElement.getText().trim();

        System.out.println("    Texto completo da corrente: " + textoCompleto);

        // Método robusto para extrair apenas os números
        double correnteValor = extrairValorNumerico(textoCompleto);

        System.out.println("    Corrente extraída: " + correnteValor + " mA");

        return String.valueOf(correnteValor);
    }

    private double extrairValorNumerico(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Se já for um número direto, parse direto
            if (texto.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(texto);
            }

            // Se tiver formato "Modulator1.mtrMainCurr = 39"
            if (texto.contains("=")) {
                String[] partes = texto.split("=");
                if (partes.length > 1) {
                    String valorStr = partes[1].trim();
                    // Remove qualquer coisa que não seja número, ponto ou sinal negativo
                    valorStr = valorStr.replaceAll("[^0-9.-]", "");
                    if (!valorStr.isEmpty()) {
                        return Double.parseDouble(valorStr);
                    }
                }
            }

            // Extrai o primeiro número encontrado no texto
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("-?\\d+(\\.\\d+)?");
            java.util.regex.Matcher matcher = pattern.matcher(texto);

            if (matcher.find()) {
                return Double.parseDouble(matcher.group());
            }

            return 0.0;
        } catch (Exception e) {
            System.err.println("Erro ao extrair valor numérico de: '" + texto + "' - " + e.getMessage());
            return 0.0;
        }
    }

    private Map<String, Object> executarAjusteDinamicoPorCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {

        Map<String, Object> resultado = new HashMap<>();
        int offsetAtual = 15; // Começa com 15
        int iteracoes = 0;
        int maxIteracoes = 20; // Prevenir loop infinito

        // Definir parâmetros baseado no canal
        int correnteMinima, correnteMaximaErro;

        switch (canal) {
            case "14":
                correnteMinima = 70;
                correnteMaximaErro = 73;
                System.out.println("  Parâmetros para canal 14: 70-73 mA");
                break;
            case "34":
                correnteMinima = 65;
                correnteMaximaErro = 68;
                System.out.println("  Parâmetros para canal 34: 65-68 mA");
                break;
            case "51":
                correnteMinima = 60;
                correnteMaximaErro = 63;
                System.out.println("  Parâmetros para canal 51: 60-63 mA");
                break;
            default:
                throw new Exception("Canal não suportado: " + canal);
        }

        while (iteracoes < maxIteracoes) {
            iteracoes++;
            System.out.println("\n  --- Loop " + iteracoes + " | Offset: " + offsetAtual + " ---");

            // 2.6.1.1. Checa a corrente
            String correnteStr = verificarCorrente(driver, wait);
            double correnteDouble = Double.parseDouble(correnteStr);
            int corrente = (int) correnteDouble;
            System.out.println("    Corrente atual: " + corrente + " mA");

            // 2.6.1.1.1. Se a corrente >= X (70, 65 ou 60 dependendo do canal)
            if (corrente >= correnteMinima) {
                System.out.println("    Corrente atingiu o mínimo (" + correnteMinima + " mA)");

                // 2.6.1.1.1.1. Espera 10 seg
                System.out.println("    Aguardando 10 segundos para verificação final...");
                Thread.sleep(10000);

                // 2.6.1.1.1.2. Checa a corrente novamente
                correnteStr = verificarCorrente(driver, wait);
                correnteDouble = Double.parseDouble(correnteStr);
                corrente = (int) correnteDouble;
                System.out.println("    Corrente após 10s: " + corrente + " mA");

                // 2.6.1.1.1.2.1. Se a corrente > Y (73, 68 ou 63) → ERRO
                if (corrente > correnteMaximaErro) {
                    String erroMsg = "ERRO: Corrente " + corrente + " mA > limite máximo " + correnteMaximaErro + " mA";
                    System.err.println("    " + erroMsg);
                    resultado.put("status", "erro");
                    resultado.put("mensagem", erroMsg);
                    resultado.put("iteracoes", iteracoes);
                    return resultado;
                }

                // 2.6.1.1.1.2.2. Se corrente >= mínimo e <= máximo → SUCESSO
                System.out.println("    SUCESSO: Corrente " + corrente + " mA dentro dos limites (" +
                        correnteMinima + "-" + correnteMaximaErro + " mA)");
                resultado.put("status", "sucesso");
                resultado.put("mensagem", "Corrente ajustada corretamente");
                resultado.put("corrente_final", corrente);
                resultado.put("offset_final", offsetAtual);
                resultado.put("iteracoes", iteracoes);
                return resultado;
            }

            // 2.6.1.1.2. Se a corrente < X
            System.out.println("    Corrente abaixo do mínimo (" + correnteMinima + " mA)");

            // 2.6.1.1.2.1. Subtrai 1 do offset
            offsetAtual--;
            System.out.println("    Reduzindo offset para: " + offsetAtual);

            if (offsetAtual < 0) {
                String erroMsg = "ERRO: Offset chegou a 0 e corrente ainda não atingiu " + correnteMinima + " mA";
                System.err.println("    " + erroMsg);
                resultado.put("status", "erro");
                resultado.put("mensagem", erroMsg);
                resultado.put("iteracoes", iteracoes);
                return resultado;
            }

            // Aplicar novo offset (precisa desligar/ligar para aplicar)
            System.out.println("    Aplicando novo offset " + offsetAtual + "...");
            desligarMTX1(driver, wait);
            configurarOffset(driver, wait, String.valueOf(offsetAtual));
            ligarMTX1(driver, wait);

            // 2.6.1.1.2.1. Espera 10 seg
            System.out.println("    Aguardando 10 segundos para estabilização...");
            Thread.sleep(10000);

            // O loop continua verificando a corrente novamente
        }

        // Se chegou aqui, atingiu o máximo de iterações
        String erroMsg = "ERRO: Máximo de " + maxIteracoes + " iterações atingido";
        System.err.println("  " + erroMsg);
        resultado.put("status", "erro");
        resultado.put("mensagem", erroMsg);
        resultado.put("iteracoes", iteracoes);
        return resultado;
    }

    private String mudarCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        try {
            // Acessar Modulator 1
            WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Modulator1___']/input")));
            modulator1.click();
            Thread.sleep(300);

            // Pegar canal atual (antes) - usando o método corrigido
            String canalAntes = verificarCanal(driver, wait);
            System.out.println("  Canal atual: " + canalAntes);

            // Se já estiver no canal correto, retornar
            if (canalAntes.equals(canal)) {
                System.out.println("  Já está no canal " + canal);
                return canalAntes;
            }

            // Clicar para editar o canal
            WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("Modulator1_Config_UpConverter_ChannelNumber")));

            // Tentar várias estratégias para mudar o canal
            boolean sucesso = false;

            // Estratégia 1: Double click
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                        .doubleClick(canalElement)
                        .perform();
                Thread.sleep(300);

                WebElement activeElement = driver.switchTo().activeElement();
                if (activeElement.getTagName().equals("input")) {
                    activeElement.clear();
                    Thread.sleep(300);
                    activeElement.sendKeys(canal);
                    Thread.sleep(300);
                    activeElement.sendKeys(Keys.ENTER);
                    sucesso = true;
                }
            } catch (Exception e) {
                System.err.println("  Estratégia 1 falhou: " + e.getMessage());
            }

            // Estratégia 2: Clicar direto e usar sendKeys
            if (!sucesso) {
                try {
                    canalElement.click();
                    Thread.sleep(300);
                    canalElement.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    canalElement.sendKeys(Keys.DELETE);
                    canalElement.sendKeys(canal);
                    Thread.sleep(300);
                    canalElement.sendKeys(Keys.ENTER);
                    sucesso = true;
                } catch (Exception e) {
                    System.err.println("  Estratégia 2 falhou: " + e.getMessage());
                }
            }

            // Estratégia 3: Usar JavaScript
            if (!sucesso) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = arguments[1];", canalElement, canal);
                    Thread.sleep(300);
                    // Simular evento de change
                    js.executeScript("arguments[0].dispatchEvent(new Event('change'));", canalElement);
                    sucesso = true;
                } catch (Exception e) {
                    System.err.println("  Estratégia 3 falhou: " + e.getMessage());
                }
            }

            if (!sucesso) {
                throw new Exception("Não foi possível mudar o canal");
            }

            Thread.sleep(2000); // Aguardar mudança

            // Verificar canal depois
            String canalDepois = verificarCanal(driver, wait);
            System.out.println("  Canal configurado: " + canalDepois);

            // Verificar se realmente mudou
            int tentativas = 0;
            while (!canalDepois.equals(canal) && tentativas < 3) {
                System.out.println("  Canal não mudou corretamente. Tentativa " + (tentativas + 1));
                Thread.sleep(1000);
                canalDepois = verificarCanal(driver, wait);
                tentativas++;
            }

            if (!canalDepois.equals(canal)) {
                System.err.println("  AVISO: Canal não mudou corretamente. Esperado: " + canal + ", Lido: " + canalDepois);
            }

            return canalAntes;

        } catch (Exception e) {
            System.err.println("  Erro ao mudar canal: " + e.getMessage());
            throw e;
        }
    }

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

        System.out.println("  MTX1 desligado (RfMasterOn = 2)");
        Thread.sleep(300);
    }

    private void ligarMTX1(WebDriver driver, WebDriverWait wait) throws Exception {
        // Encontrar RfMasterOn novamente
        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier1.Config.RfMasterOn",
                "PowerAmplifier1_Config_RfMasterOn");

        if (!configurarValor(driver, rfMasterOn, "1")) {
            throw new Exception("Falha ao ligar RfMasterOn");
        }

        System.out.println("  MTX1 ligado (RfMasterOn = 1)");
        Thread.sleep(300);
    }

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

        System.out.println("  Offset configurado: " + valorOffset);
        Thread.sleep(300);
    }

    private void configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
        // Navegar para PowerAmplifier1
        WebElement powerAmplifier1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier1___']/input")));
        powerAmplifier1.click();
        Thread.sleep(300);

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

        System.out.println("  Potência configurada: " + potencia);
        Thread.sleep(300);
    }

    private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier1
        WebElement powerAmplifier1 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier1___']/input")));
        powerAmplifier1.click();
        Thread.sleep(300);

        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier1_Status_ForwardPower")));

        String textoCompleto = potenciaElement.getText().trim();
        double potenciaValor = extrairValorNumerico(textoCompleto);

        return String.valueOf(potenciaValor);
    }

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
        Thread.sleep(500);
    }

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

    private void salvarLogCanal(String canal, String canalAntes, String canalDepois, String potencia, String corrente, String offset, Map<String, Object> resultadoAjuste) {
        String filePath = System.getProperty("user.dir") + "/logs_rotina_completa.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | Canal: " + canal +
                    " | Antes: " + canalAntes +
                    " | Depois: " + canalDepois +
                    " | Potência: " + potencia +
                    " | Corrente: " + corrente +
                    " | Offset: " + offset +
                    " | Iterações: " + resultadoAjuste.get("iteracoes") +
                    " | Status: " + resultadoAjuste.get("status") + "\n");
            System.out.println("  Log salvo em: " + filePath);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }

    private void debugElemento(WebElement elemento, String nome) {
        try {
            System.out.println("\n=== DEBUG " + nome + " ===");
            System.out.println("Tag: " + elemento.getTagName());
            System.out.println("Text: " + elemento.getText());
            System.out.println("Value attr: " + elemento.getAttribute("value"));
            System.out.println("InnerText: " + elemento.getAttribute("innerText"));
            System.out.println("OuterHTML: " + elemento.getAttribute("outerHTML"));
            System.out.println("Displayed: " + elemento.isDisplayed());
            System.out.println("Enabled: " + elemento.isEnabled());
            System.out.println("==================\n");
        } catch (Exception e) {
            System.err.println("Erro no debug: " + e.getMessage());
        }
    }

    @PostMapping("/executar-para-canal")
    public ResponseEntity<Map<String, Object>> executarParaCanal(@RequestParam String canal) {
        Map<String, Object> resposta = new HashMap<>();
        WebDriver driver = null;

        try {
            // Configurar ChromeDriver
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-gpu");
            options.addArguments("--incognito");
            options.addArguments("--disable-cache");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Login
            driver.get("http://10.10.103.103/debug/");
            fazerLogin(driver, wait);

            // Processar canal
            Map<String, Object> resultado = processarCanalCompleto(driver, wait, canal);
            resposta.putAll(resultado);

            if ("sucesso".equals(resultado.get("status"))) {
                return ResponseEntity.ok(resposta);
            } else {
                return ResponseEntity.status(500).body(resposta);
            }

        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro: " + e.getMessage());
            return ResponseEntity.status(500).body(resposta);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}