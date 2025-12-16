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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LinearizacaoMtx2 {

    @Value("${app.username:admin}")
    private String username;

    @Value("${app.password:admin}")
    private String password;

    @PostMapping("/executar-rotina-linearizacao")
    public ResponseEntity<Map<String, Object>> executarRotinaLinearizacao() {
        Map<String, Object> respostaGeral = new HashMap<>();
        WebDriver driver = null;

        try {
            System.out.println("=== INICIANDO ROTINA DE LINEARIZAÇÃO ===");
            System.out.println("Hora de início: " + LocalDateTime.now());

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

            driver.get("http://10.10.103.103/debug/");
            System.out.println("Página acessada");
            fazerLogin(driver, wait);
            System.out.println("Login realizado");

            // Processo completo para MTX2
            Map<String, Object> resultado = processoCompleto(driver, wait);

            // Resposta final
            respostaGeral.put("status", "sucesso");
            respostaGeral.put("mensagem", "Rotina de linearização executada com sucesso");
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
            respostaGeral.put("hora_fim", LocalDateTime.now().toString());
            respostaGeral.put("resultados", resultado);

            System.out.println("\n=== ROTINA DE LINEARIZAÇÃO FINALIZADA ===");
            System.out.println("Hora de fim: " + LocalDateTime.now());

            return ResponseEntity.ok(respostaGeral);

        } catch (Exception e) {
            respostaGeral.put("status", "erro");
            respostaGeral.put("mensagem", "Erro na rotina de linearização: " + e.getMessage());
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString());

            System.err.println("Erro na rotina de linearização: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(respostaGeral);
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Driver finalizado");
            }
        }
    }

    // função para linearização
    private Map<String, Object> processoCompleto(WebDriver driver, WebDriverWait wait) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // ========== ETAPA 0: INFORMAÇÕES INICIAIS ==========
            System.out.println("\n[ETAPA 0] Coletando informações iniciais");
            String potenciaInicial = verificarPotencia(driver, wait);
            String temperaturaInicial = verificarTemperatura(driver, wait);
            System.out.println("  Potência inicial: " + potenciaInicial + "W");
            System.out.println("  Temperatura inicial: " + temperaturaInicial + "°C");

            // ========== ETAPA 1: DESLIGAR MTX2 PARA COMEÇAR ==========
            System.out.println("\n[ETAPA 1] Desligando MTX2 para iniciar sequência");
            desligarMTX2(driver, wait);

            // ========== ETAPA 2: LOOP DE POTÊNCIAS ==========
            int[] potencias = {300, 340, 370, 430, 483}; // menor pro maior
            int ultimaPotenciaProcessada = 0;
            boolean todasPotenciasConcluidas = true;
            int tentativasEstabilizacao = 10; // Número máximo de tentativas
            int tempoEspera = 300000; // 300 segundos
            int margemTolerancia = 1; // ±1°C

            for (int i = 0; i < potencias.length; i++) {
                int potenciaAtual = potencias[i];
                ultimaPotenciaProcessada = potenciaAtual;

                System.out.println("\n[ETAPA 2] Processando potência: " + potenciaAtual + "W");
                System.out.println("  Progresso: " + (i + 1) + "/" + potencias.length);

                // 2.1. Configurar nova potência
                System.out.println("  2.1. Configurando potência para " + potenciaAtual + "W");
                configurarPotencia(driver, wait, String.valueOf(potenciaAtual));

                // 2.2. Ligar o MTX2
                System.out.println("  2.2. Ligando MTX2");
                ligarMTX2(driver, wait);

                // 2.3. AGUARDAR ESTABILIZAÇÃO DA TEMPERATURA
                boolean temperaturaEstabilizada = false;
                String temperaturaAnterior = "";
                String temperaturaAtual = "";

                for (int tentativa = 1; tentativa <= tentativasEstabilizacao; tentativa++) {
                    System.out.println("\n  Tentativa " + tentativa + "/" + tentativasEstabilizacao + " de estabilização");

                    // Primeira medição
                    System.out.println("    Primeira medição de temperatura...");
                    temperaturaAnterior = verificarTemperatura(driver, wait);
                    System.out.println("    Temperatura inicial: " + temperaturaAnterior + "°C");

                    // Aguardar
                    System.out.println("    Aguardando " + (tempoEspera/1000) + " segundos...");
                    Thread.sleep(tempoEspera);

                    // Segunda medição
                    System.out.println("    Segunda medição de temperatura...");
                    temperaturaAtual = verificarTemperatura(driver, wait);
                    System.out.println("    Temperatura final: " + temperaturaAtual + "°C");

                    try {
                        // Converter para números APÓS ter os valores
                        int tempAnteriorNum = Integer.parseInt(temperaturaAnterior);
                        int tempAtualNum = Integer.parseInt(temperaturaAtual);

                        // Calcular diferença
                        int diferenca = Math.abs(tempAnteriorNum - tempAtualNum);

                        // Verificar estabilização
                        if (temperaturaAnterior.equals(temperaturaAtual) || diferenca <= margemTolerancia) {
                            temperaturaEstabilizada = true;
                            System.out.println("    Temperatura estabilizada em " + temperaturaAtual + "°C\n");
                            if (diferenca > 0) {
                                System.out.println("    Diferença: " + diferenca + "°C (≤ " + margemTolerancia + "°C)\n");
                            }
                            break;
                        } else {
                            System.out.println("    Temperatura não estabilizada: " +
                                    temperaturaAnterior + "°C → " + temperaturaAtual + "°C");
                            System.out.println("    Diferença: " + diferenca + "°C (> " + margemTolerancia + "°C)");
                        }
                    } catch (NumberFormatException e) {
                        // Se não conseguir converter, usar comparação de strings
                        System.err.println("    Erro ao converter temperatura para número: " + e.getMessage());
                        if (temperaturaAnterior.equals(temperaturaAtual)) {
                            temperaturaEstabilizada = true;
                            System.out.println("    Temperatura estabilizada em " + temperaturaAtual + "°C");
                            break;
                        } else {
                            System.out.println("    Temperatura não estabilizada: " +
                                    temperaturaAnterior + "°C ≠ " + temperaturaAtual + "°C");
                        }
                    }

                    // Se não for a última tentativa, aguardar mais
                    if (tentativa < tentativasEstabilizacao && !temperaturaEstabilizada) {
                        System.out.println("    Aguardando mais " + (tempoEspera/1000) + " segundos...");
                        Thread.sleep(tempoEspera);
                    }
                }

                // 2.4. VERIFICAR SE TEMPERATURA ESTABILIZOU
                if (temperaturaEstabilizada) {
                    System.out.println("  Potência " + potenciaAtual + "W processada com sucesso");

                    // Se não for a última potência, desligar para próxima
                    if (i < potencias.length - 1) {
                        System.out.println("  2.5. Desligando MTX2 para próxima potência");
                        desligarMTX2(driver, wait);
                        Thread.sleep(3000); // Aguardar 3 segundos entre potências
                    } else {
                        System.out.println("  Última potência concluída com sucesso!");
                    }
                } else {
                    System.out.println("  Temperatura não estabilizou após " + tentativasEstabilizacao + " tentativas");
                    System.out.println("  Interrompendo sequência na potência " + potenciaAtual + "W");
                    todasPotenciasConcluidas = false;

                    // Desligar MTX2 antes de sair
                    desligarMTX2(driver, wait);
                    break;
                }
            }

            // ========== ETAPA 3: RESULTADOS FINAIS ==========
            System.out.println("\n[ETAPA 3] Coletando resultados finais");

            String potenciaFinal = verificarPotencia(driver, wait);
            String temperaturaFinal = verificarTemperatura(driver, wait);

            // ========== ETAPA 4: SALVAR LOG ==========
            System.out.println("\n[ETAPA 4] Salvando log");
            salvarLogLinearizacao(potenciaInicial, potenciaFinal, temperaturaInicial,
                    temperaturaFinal, ultimaPotenciaProcessada, todasPotenciasConcluidas);

            // ========== PREPARAR RESPOSTA ==========
            resultado.put("status", todasPotenciasConcluidas ? "sucesso" : "parcial");
            resultado.put("mensagem", todasPotenciasConcluidas ?
                    "Sequência de potências concluída com sucesso" :
                    "Sequência interrompida na potência " + ultimaPotenciaProcessada + "W (temperatura não estabilizou)");
            resultado.put("potencia_inicial", potenciaInicial);
            resultado.put("potencia_final", potenciaFinal);
            resultado.put("ultima_potencia_processada", ultimaPotenciaProcessada);
            resultado.put("todas_potencias_concluidas", todasPotenciasConcluidas);
            resultado.put("temperatura_inicial", temperaturaInicial);
            resultado.put("temperatura_final", temperaturaFinal);
            resultado.put("sequencia_potencias", "300 → 340 → 370 → 430 → 483");

        } catch (Exception e) {
            System.err.println("Erro no processamento do MTX2: " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro no MTX2: " + e.getMessage());
        }

        return resultado;
    }

    // verificar o valor da potencia
    private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // CORREÇÃO: Mudar de OutputPower para ForwardPowerdBm
        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier2_Status_ForwardPowerdBm")));

        String potenciaText = potenciaElement.getText().trim();
        double potencia = extrairValorNumerico(potenciaText);

        return String.valueOf((int) potencia);
    }

    // verificar os valores das temperaturas
    private String verificarTemperatura(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        WebElement tempElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier2_Status_Temperature")));

        String tempText = tempElement.getText().trim();
        double temperatura = extrairValorNumerico(tempText);

        return String.valueOf((int) temperatura);
    }

    // salvar as informações
    private void salvarLogLinearizacao(String potenciaInicial, String potenciaFinal, String tempInicial, String tempFinal, int ultimaPotencia, boolean concluido) {
        String filePath = System.getProperty("user.dir") + "/log_linearizacao_mtx2.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | P_Inicial: " + potenciaInicial + "W" +
                    " | P_Final: " + potenciaFinal + "W" +
                    " | T_Inicial: " + tempInicial + "°C" +
                    " | T_Final: " + tempFinal + "°C" +
                    " | Ultima_Potencia: " + ultimaPotencia + "W" +
                    " | Status: " + (concluido ? "COMPLETO" : "INTERROMPIDO") + "\n");
            System.out.println("  Log salvo em: " + filePath);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }

    // desligar mtx2
    private void desligarMTX2(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.RfMasterOn",
                "PowerAmplifier2_Config_RfMasterOn");

        if (rfMasterOn == null) {
            rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'RfMasterOn')]")));
        }

        if (!configurarValor(driver, rfMasterOn, "2")) {
            throw new Exception("Falha ao desligar RfMasterOn");
        }

        System.out.println("  MTX2 desligado (RfMasterOn = 2)");
        Thread.sleep(300);
    }

    // ligar mtx2
    private void ligarMTX2(WebDriver driver, WebDriverWait wait) throws Exception {
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.RfMasterOn",
                "PowerAmplifier2_Config_RfMasterOn");

        if (rfMasterOn == null) {
            rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'RfMasterOn')]")));
        }

        if (!configurarValor(driver, rfMasterOn, "1")) {
            throw new Exception("Falha ao ligar RfMasterOn");
        }

        System.out.println("  MTX2 ligado (RfMasterOn = 1)");
        Thread.sleep(300);
    }

    // fazer login
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

    // configurar a potencia
    private void configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar OutputPower (este é o elemento de CONFIGURAÇÃO, não de STATUS)
        WebElement outputPower = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.OutputPower",
                "PowerAmplifier2_Config_OutputPower");

        if (outputPower == null) {
            outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(text(), 'OutputPower')]")));
        }

        if (!configurarValor(driver, outputPower, potencia)) {
            throw new Exception("Falha ao configurar potência");
        }

        System.out.println("  Potência configurada: " + potencia + "W");
        Thread.sleep(300);
    }

    // configurar os valores
    private boolean configurarValor(WebDriver driver, WebElement elemento, String valor) {
        try {
            elemento.click();
            Thread.sleep(300);

            WebElement campoInput = driver.switchTo().activeElement();
            if (campoInput.getTagName().equals("input") || campoInput.getTagName().equals("textarea")) {
                campoInput.click();
                Thread.sleep(300);
                campoInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                campoInput.sendKeys(Keys.DELETE);
                campoInput.sendKeys(valor);
                Thread.sleep(300);
                campoInput.sendKeys(Keys.ENTER);
                Thread.sleep(300);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao configurar valor: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // algumas trentaativas de envio de informações (comandos SET)
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

    // extrair os valores
    private double extrairValorNumerico(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Se já for um número direto, parse direto
            if (texto.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(texto);
            }

            // Se tiver formato "Modulator2.mtrMainCurr = 39"
            if (texto.contains("=")) {
                String[] partes = texto.split("=");
                if (partes.length > 1) {
                    String valorStr = partes[1].trim();
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

    // linearização do canal específico
    private Map<String, Object> processoLinearizacaoParaCanal(WebDriver driver, WebDriverWait wait, String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // ========== ETAPA 0: MUDAR PARA O CANAL ==========
            System.out.println("\n[ETAPA 0] Mudando para canal: " + canal);

            // Primeiro verifica o canal atual
            String canalAtual = verificarCanal(driver, wait);
            System.out.println("  Canal atual: " + canalAtual);

            // Se não for o canal desejado, muda
            if (!canalAtual.equals(canal) && !canalAtual.contains(canal)) {
                System.out.println("  Mudando para canal " + canal + "...");
                // Chama a função para mudar canal (você precisa implementar ou usar a existente)
                mudarCanal(driver, wait, canal);
                Thread.sleep(2000);
                canalAtual = verificarCanal(driver, wait);
                System.out.println("  Novo canal: " + canalAtual);
            }

            // ========== ETAPA 1: INFORMAÇÕES INICIAIS ==========
            System.out.println("\n[ETAPA 1] Coletando informações iniciais");
            String potenciaInicial = verificarPotencia(driver, wait);
            String temperaturaInicial = verificarTemperatura(driver, wait);
            System.out.println("  Potência inicial: " + potenciaInicial + "W");
            System.out.println("  Temperatura inicial: " + temperaturaInicial + "°C");

            // ========== ETAPA 2: DESLIGAR MTX2 PARA COMEÇAR ==========
            System.out.println("\n[ETAPA 2] Desligando MTX2 para iniciar sequência");
            desligarMTX2(driver, wait);

            // ========== ETAPA 3: LOOP DE POTÊNCIAS ==========
            int[] potencias = {483, 430, 370, 340, 300};
            int ultimaPotenciaProcessada = 0;
            boolean todasPotenciasConcluidas = true;
            List<String> historicoTemperaturas = new  ArrayList<>();

            for (int i = 0; i < potencias.length; i++) {
                int potenciaAtual = potencias[i];
                ultimaPotenciaProcessada = potenciaAtual;

                System.out.println("\n[ETAPA 3] Processando potência: " + potenciaAtual + "W");
                System.out.println("  Progresso: " + (i + 1) + "/" + potencias.length);
                System.out.println("  Canal: " + canalAtual);

                // 3.1. Configurar nova potência
                System.out.println("  3.1. Configurando potência para " + potenciaAtual + "W");
                configurarPotencia(driver, wait, String.valueOf(potenciaAtual));

                // 3.2. Ligar o MTX2
                System.out.println("  3.2. Ligando MTX2");
                ligarMTX2(driver, wait);

                // 3.3. Primeira medição de temperatura
                System.out.println("  3.3. Primeira medição de temperatura...");
                String temperaturaAntes = verificarTemperatura(driver, wait);
                System.out.println("    Temperatura inicial: " + temperaturaAntes + "°C");

                // 3.4. Aguardar 1 minuto
                System.out.println("  3.4. Aguardando 1 minuto para estabilização...");
                Thread.sleep(60000);

                // 3.5. Segunda medição de temperatura
                System.out.println("  3.5. Segunda medição de temperatura...");
                String temperaturaDepois = verificarTemperatura(driver, wait);
                System.out.println("    Temperatura final: " + temperaturaDepois + "°C");

                // Registrar no histórico
                historicoTemperaturas.add("Potência " + potenciaAtual + "W: " +
                        temperaturaAntes + "°C → " + temperaturaDepois + "°C");

                // 3.6. Verificar se temperaturas são iguais
                if (temperaturaAntes.equals(temperaturaDepois)) {
                    System.out.println("  Temperatura estabilizada em " + temperaturaAntes + "°C");

                    // Se não for a última potência, desligar para próxima
                    if (i < potencias.length - 1) {
                        System.out.println("  3.6. Desligando MTX2 para próxima potência");
                        desligarMTX2(driver, wait);
                    } else {
                        System.out.println("  Última potência concluída com sucesso!");
                    }
                } else {
                    System.out.println("  Temperatura não estabilizada: " +
                            temperaturaAntes + "°C ≠ " + temperaturaDepois + "°C");
                    System.out.println("  Interrompendo sequência na potência " + potenciaAtual + "W");
                    todasPotenciasConcluidas = false;

                    // Desligar MTX2 antes de sair
                    desligarMTX2(driver, wait);
                    break;
                }
            }

            // ========== ETAPA 4: RESULTADOS FINAIS ==========
            System.out.println("\n[ETAPA 4] Coletando resultados finais");

            String potenciaFinal = verificarPotencia(driver, wait);
            String temperaturaFinal = verificarTemperatura(driver, wait);
            String canalFinal = verificarCanal(driver, wait);

            // ========== ETAPA 5: SALVAR LOG ==========
            System.out.println("\n[ETAPA 5] Salvando log");
            salvarLogLinearizacaoCanal(canal, canalAtual, canalFinal,
                    potenciaInicial, potenciaFinal,
                    temperaturaInicial, temperaturaFinal,
                    ultimaPotenciaProcessada, todasPotenciasConcluidas,
                    historicoTemperaturas);

            // ========== PREPARAR RESPOSTA ==========
            resultado.put("status", todasPotenciasConcluidas ? "sucesso" : "parcial");
            resultado.put("mensagem", todasPotenciasConcluidas ?
                    "Linearização concluída para todas as potências no canal " + canal :
                    "Linearização interrompida na potência " + ultimaPotenciaProcessada + "W no canal " + canal);
            resultado.put("canal", canal);
            resultado.put("canal_antes", canalAtual);
            resultado.put("canal_depois", canalFinal);
            resultado.put("potencia_inicial", potenciaInicial);
            resultado.put("potencia_final", potenciaFinal);
            resultado.put("ultima_potencia_processada", ultimaPotenciaProcessada);
            resultado.put("todas_potencias_concluidas", todasPotenciasConcluidas);
            resultado.put("temperatura_inicial", temperaturaInicial);
            resultado.put("temperatura_final", temperaturaFinal);
            resultado.put("historico_temperaturas", historicoTemperaturas);
            resultado.put("sequencia_potencias", "483 → 430 → 370 → 340 → 300");

        } catch (Exception e) {
            System.err.println("Erro no processamento do canal " + canal + ": " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro no canal " + canal + ": " + e.getMessage());
            resultado.put("canal", canal);
        }

        return resultado;
    }

    // verificar canal
    private String verificarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Navegar para Modulator2
            WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Modulator2___']/input")));
            modulator2.click();
            Thread.sleep(300);

            // Tentar várias estratégias para pegar o valor do canal
            WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("Modulator2_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);

            // Estratégia 1: Pegar o texto diretamente
            String canalTexto = canalElement.getText().trim();
            System.out.println("    Texto do elemento canal: " + canalTexto);

            // Estratégia 2: Se não conseguir, pegar o value attribute
            if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator2.Config.UpConverter.ChannelNumber")) {
                canalTexto = canalElement.getAttribute("value");
                System.out.println("    Value attribute do canal: " + canalTexto);
            }

            // Estratégia 3: Se ainda não, tentar innerText
            if (canalTexto == null || canalTexto.isEmpty()) {
                canalTexto = canalElement.getAttribute("innerText");
                System.out.println("    InnerText do canal: " + canalTexto);
            }

            // Extrair números do texto (pode vir como "Modulator2.Config.UpConverter.ChannelNumber = 14")
            if (canalTexto != null && !canalTexto.isEmpty()) {
                // Se tiver formato "Modulator2.Config.UpConverter.ChannelNumber = 14"
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

    // mudar o canal  14 - 34 - 51
    private String mudarCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        try {
            // Acessar Modulator 2
            WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Modulator2___']/input")));
            modulator2.click();
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
                    By.id("Modulator2_Config_UpConverter_ChannelNumber")));

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

    // salvar log do canalespecífico
    private void salvarLogLinearizacaoCanal(String canalSolicitado, String canalAntes, String canalDepois, String potenciaInicial, String potenciaFinal, String tempInicial, String tempFinal, int ultimaPotencia, boolean concluido, List<String> historico) {
        String filePath = System.getProperty("user.dir") + "/log_linearizacao_canal_" + canalSolicitado + "_mtx2.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write("CANAL SOLICITADO: " + canalSolicitado + " | Data: " + LocalDateTime.now() + "\n");
            writer.write("Canal antes: " + canalAntes + "\n");
            writer.write("Canal depois: " + canalDepois + "\n");
            writer.write("Potência inicial: " + potenciaInicial + "W\n");
            writer.write("Potência final: " + potenciaFinal + "W\n");
            writer.write("Última potência processada: " + ultimaPotencia + "W\n");
            writer.write("Temperatura inicial: " + tempInicial + "°C\n");
            writer.write("Temperatura final: " + tempFinal + "°C\n");
            writer.write("Status: " + (concluido ? "COMPLETO" : "INTERROMPIDO") + "\n");
            writer.write("HISTÓRICO DE TEMPERATURAS:\n");
            System.out.println("  Log salvo em: " + filePath);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }

    @PostMapping("/executar-linearizacao-para-canal")
    public ResponseEntity<Map<String, Object>> executarLinearizacaoParaCanal(@RequestParam String canal) {
        Map<String, Object> resposta = new HashMap<>();
        WebDriver driver = null;

        try {
            System.out.println("=== INICIANDO LINEARIZAÇÃO PARA CANAL: " + canal + " ===");
            System.out.println("Hora de início: " + LocalDateTime.now());

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

            driver.get("http://10.10.103.103/debug/");
            System.out.println("Página acessada");
            fazerLogin(driver, wait);
            System.out.println("Login realizado");

            // Processo de linearização para o canal específico
            //Map<String, Object> resultado = processoLinearizacaoParaCanal(driver, wait, canal);
            Map<String, Object> resultado = processoCompleto(driver, wait);


            // Resposta final
            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Linearização para canal " + canal + " executada com sucesso");
            resposta.put("hora_inicio", LocalDateTime.now().toString());
            resposta.put("hora_fim", LocalDateTime.now().toString());
            resposta.put("detalhes", resultado);

            System.out.println("\n=== LINEARIZAÇÃO CONCLUÍDA PARA CANAL: " + canal + " ===");
            System.out.println("Hora de fim: " + LocalDateTime.now());

            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro na linearização para canal " + canal + ": " + e.getMessage());
            resposta.put("hora_inicio", LocalDateTime.now().toString());

            System.err.println("Erro na linearização para canal " + canal + ": " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(resposta);
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Driver finalizado");
            }
        }
    }

    @PostMapping("/cancelar-linearizacao")
    public ResponseEntity<Map<String, Object>> cancelarLinearizacao() {
        Map<String, Object> resposta = new HashMap<>();

        System.out.println("Solicitação de cancelamento de linearização recebida");

        resposta.put("status", "sucesso");
        resposta.put("mensagem", "Solicitação de cancelamento recebida");
        resposta.put("hora_cancelamento", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }
}