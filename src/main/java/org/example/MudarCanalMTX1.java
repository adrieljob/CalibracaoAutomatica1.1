package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class MudarCanalMTX1 {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";

    // Mapa de valores pré-programados para os canais (canal -> Número do Canal)
    private static final Map<String, String> VALORES_CANAL = new HashMap<>();

    static {
        VALORES_CANAL.put("14", "14");
        VALORES_CANAL.put("34", "34");
        VALORES_CANAL.put("51", "51");
    }

    // Variáveis para o loop cíclico
    private static final Map<String, Object> TESTE_STATUS = new ConcurrentHashMap<>();
    private static ExecutorService executorService = null;

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @PostMapping("/mudar-canal-mtx1")
    public ResponseEntity<Map<String, Object>> mudarCanal1(@RequestParam String valorCanal) {
        Map<String, Object> resposta = new HashMap<>();

        if (!VALORES_CANAL.containsKey(valorCanal)) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Valor não programado. Valores permitidos: " +
                    String.join(", ", VALORES_CANAL.keySet()));
            resposta.put("valores_programados", VALORES_CANAL);
            return ResponseEntity.badRequest().body(resposta);
        }

        try {
            Map<String, Object> resultado = configurarCanalMTX1(valorCanal);

            // Se houve erro no processo interno
            if ("erro".equals(resultado.get("status"))) {
                resposta.put("status", "erro");
                resposta.put("mensagem", resultado.get("mensagem"));
                return ResponseEntity.status(500).body(resposta);
            }

            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Canal do MTX2 configurado com sucesso");
            resposta.put("valor_configurado", valorCanal);
            resposta.put("canal_esperado", VALORES_CANAL.get(valorCanal));
            resposta.put("canal_antes", resultado.get("canal_antes"));
            resposta.put("canal_depois", resultado.get("canal_depois"));

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro ao configurar canal: " + e.getMessage());
            resposta.put("valor_configurado", valorCanal);

            return ResponseEntity.status(500).body(resposta);
        }
    }

    // Método principal para configurar o canal do MTX1
    private Map<String, Object> configurarCanalMTX1(String canal) {
        String urlBase = "http://10.10.103.103/debug/";
        String canalEsperado = VALORES_CANAL.get(canal);

        System.out.println(GREEN + "Configurando Canal do MTX1:" + canal +
                " -> Canal " + canalEsperado + RESET);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--disable-cache");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);
            System.out.println(GREEN + "Acessando: " + urlBase + RESET);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
            campoSenha.clear();
            campoSenha.sendKeys(password);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
            botaoLogin.click();
            System.out.println(GREEN + "Login realizado" + RESET);

            Thread.sleep(1000); // Aguardar carregamento

            // Acessa Modulator 1
            WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator1___']/input")));
            modulator1.click();
            Thread.sleep(300);
            System.out.println(GREEN + "Modulator1 selecionado" + RESET);

            // Pega o valor do canal ANTES da mudança
            WebElement getCanalAntes = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("Modulator1_Config_UpConverter_ChannelNumber")));

            String canalAntes = extrairValorNumerico(getCanalAntes.getText().trim());
            System.out.println(GREEN + "Canal ANTES: " + canalAntes + RESET);

            // *** ETAPA 1: Encontrar e clicar no campo ChannelNumber ***
            System.out.println(GREEN + "Localizando campo ChannelNumber..." + RESET);

            WebElement campoChannelNumber = null;

            // Estratégias para encontrar o campo
            String[] seletores = {
                    "//*[@id='Modulator1_Config_UpConverter_ChannelNumber']",
                    "//*[contains(@id, 'ChannelNumber')]",
                    "//*[contains(text(), 'ChannelNumber')]",
                    "//label[contains(text(), 'Channel')]/following-sibling::input"
            };

            for (String seletor : seletores) {
                try {
                    campoChannelNumber = driver.findElement(By.xpath(seletor));
                    if (campoChannelNumber != null && campoChannelNumber.isDisplayed()) {
                        System.out.println(GREEN + "Campo encontrado com seletor: " + seletor + RESET);
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            if (campoChannelNumber == null) {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Campo ChannelNumber não encontrado");
                return resultado;
            }

            // *** ETAPA 2: Configurar o novo valor ***
            System.out.println(GREEN + "Configurando canal para: " + canalEsperado + RESET);

            boolean sucesso = false;

            // Tentativa 1: Double click
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                        .doubleClick(campoChannelNumber)
                        .perform();
                Thread.sleep(300);

                WebElement activeElement = driver.switchTo().activeElement();
                if (activeElement.getTagName().equals("input")) {
                    activeElement.clear();
                    Thread.sleep(300);
                    activeElement.sendKeys(canalEsperado);
                    Thread.sleep(300);
                    activeElement.sendKeys(Keys.ENTER);
                    sucesso = true;
                }
            } catch (Exception e) {
                System.out.println(GREEN + "Estratégia 1 falhou: " + e.getMessage() + RESET);
            }

            // Tentativa 2: Clicar e usar sendKeys
            if (!sucesso) {
                try {
                    campoChannelNumber.click();
                    Thread.sleep(300);
                    campoChannelNumber.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    campoChannelNumber.sendKeys(Keys.DELETE);
                    campoChannelNumber.sendKeys(canalEsperado);
                    Thread.sleep(300);
                    campoChannelNumber.sendKeys(Keys.ENTER);
                    sucesso = true;
                } catch (Exception e) {
                    System.out.println(GREEN + "Estratégia 2 falhou: " + e.getMessage() + RESET);
                }
            }

            // Tentativa 3: JavaScript
            if (!sucesso) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = arguments[1];", campoChannelNumber, canalEsperado);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoChannelNumber);
                    sucesso = true;
                } catch (Exception e) {
                    System.out.println(GREEN + "Estratégia 3 falhou: " + e.getMessage() + RESET);
                }
            }

            if (!sucesso) {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Não foi possível configurar o canal");
                return resultado;
            }

            // Aguardar aplicação da mudança
            Thread.sleep(2000);

            // *** ETAPA 3: Verificar mudança ***
            modulator1.click(); // Recarregar
            Thread.sleep(500);

            // Pega o valor do canal DEPOIS da mudança
            WebElement getCanalDepois = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("Modulator1_Config_UpConverter_ChannelNumber")));

            String canalDepois = extrairValorNumerico(getCanalDepois.getText().trim());
            System.out.println(GREEN + "Canal DEPOIS: " + canalDepois + RESET);

            // Verificar se a mudança foi aplicada
            int tentativas = 0;
            while (!canalDepois.equals(canalEsperado) && tentativas < 3) {
                System.out.println(GREEN + "Canal não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
                Thread.sleep(1000);

                // Recarregar elemento
                modulator1.click();
                Thread.sleep(500);
                getCanalDepois = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.id("Modulator1_Config_UpConverter_ChannelNumber")));
                canalDepois = extrairValorNumerico(getCanalDepois.getText().trim());
                tentativas++;
            }

            if (!canalDepois.equals(canalEsperado)) {
                System.err.println("AVISO: Canal não mudou corretamente. Esperado: " + canalEsperado + ", Lido: " + canalDepois);
            }

            // Registrar resultados
            resultado.put("status", "sucesso");
            resultado.put("canal_antes", canalAntes);
            resultado.put("canal_depois", canalDepois);
            resultado.put("valor_configurado", canal);
            resultado.put("canal_esperado", canalEsperado);

            // Salvar log
            String filePath = System.getProperty("user.dir") + "/canal_mtx1.txt";
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
                writer.write(LocalDateTime.now() + " | Canal: " + canal +
                        " | Canal Esperado: " + canalEsperado +
                        " | Antes: " + canalAntes +
                        " | Depois: " + canalDepois + "\n");
                System.out.println(GREEN + "Log salvo: " + filePath + RESET);
            } catch (java.io.IOException e) {
                System.err.println("Erro ao salvar log: " + e.getMessage());
            }

        } catch (TimeoutException e) {
            System.err.println("Timeout: " + e.getMessage());
            resultado.put("status", "erro");
            resultado.put("mensagem", "Timeout: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println(GREEN + "Driver finalizado" + RESET);
            }
        }

        return resultado;
    }

    // Método auxiliar para extrair valor numérico
    private String extrairValorNumerico(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "0";
        }

        try {
            // Remover tudo que não é número
            String numeros = texto.replaceAll("[^0-9]", "").trim();
            return numeros.isEmpty() ? "0" : numeros;
        } catch (Exception e) {
            return "0";
        }
    }

    // Loop pelos canais
    @PostMapping("/testar-todos-canais1")
    public ResponseEntity<Map<String, Object>> testarTodasCanais1() {
        Map<String, Object> resposta = new HashMap<>();

        // Verificar se já está em execução
        if (executorService != null && !executorService.isShutdown()) {
            resposta.put("status", "ja_em_execucao");
            resposta.put("mensagem", "Loop cíclico já está em execução");
            resposta.put("ultimo_canal", TESTE_STATUS.get("canal_atual"));
            resposta.put("ciclo_atual", TESTE_STATUS.get("ciclo_atual"));
            return ResponseEntity.ok(resposta);
        }

        // Resetar status
        TESTE_STATUS.clear();
        TESTE_STATUS.put("status_geral", "executando");
        TESTE_STATUS.put("inicio", LocalDateTime.now().toString());
        TESTE_STATUS.put("ciclo_atual", 1);
        TESTE_STATUS.put("modo", "loop_ciclico_1min");
        TESTE_STATUS.put("duracao_por_canal", "1 minuto");

        // Ordem dos canais para o ciclo
        List<String> ordemCanais = Arrays.asList("14", "34", "51");
        TESTE_STATUS.put("ordem_canais", ordemCanais);

        // Criar executor com thread única
        executorService = Executors.newSingleThreadExecutor();

        executorService.submit(() -> {
            try {
                System.out.println(GREEN + "=== INICIANDO LOOP CÍCLICO DE CANAIS MTX1 ===" + RESET);
                System.out.println(GREEN + "Data/Hora: " + LocalDateTime.now() + RESET);
                System.out.println(GREEN + "Modo: Loop cíclico de 1 minuto por canal" + RESET);
                System.out.println(GREEN + "Ordem: " + ordemCanais + RESET);
                System.out.println(GREEN + "Duração por canal: 1 minuto" + RESET);
                System.out.println(GREEN + "Ciclo completo: 3 minutos (3 canais x 1 minuto)" + RESET);

                int ciclo = 1;

                // Loop infinito (até ser cancelado)
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println(GREEN + "\n" + "=".repeat(60) + RESET);
                    System.out.println(GREEN + "CICLO " + ciclo + RESET);
                    System.out.println(GREEN + "=".repeat(60) + RESET);

                    for (int i = 0; i < ordemCanais.size(); i++) {
                        // Verificar se foi solicitado cancelamento
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println(GREEN + "Loop interrompido por cancelamento" + RESET);
                            return;
                        }

                        String valorCanal = ordemCanais.get(i);
                        String canalEsperado = VALORES_CANAL.get(valorCanal);
                        int posicao = i + 1;

                        // Atualizar status
                        TESTE_STATUS.put("ciclo_atual", ciclo);
                        TESTE_STATUS.put("posicao_ciclo", posicao + "/" + ordemCanais.size());
                        TESTE_STATUS.put("valor_atual", valorCanal);
                        TESTE_STATUS.put("canal_atual", canalEsperado);
                        TESTE_STATUS.put("inicio_canal", LocalDateTime.now().toString());
                        TESTE_STATUS.put("proxima_troca", LocalDateTime.now().plusMinutes(1).toString());

                        System.out.println(GREEN + "\n" + "-".repeat(50) + RESET);
                        System.out.println(GREEN + "CICLO " + ciclo + " - CANAL " + posicao + "/" + ordemCanais.size() + RESET);
                        System.out.println(GREEN + "Configurando: Canal " + canalEsperado + " (Canal: " + valorCanal + ")" + RESET);
                        System.out.println(GREEN + "Início: " + LocalDateTime.now() + RESET);
                        System.out.println(GREEN + "Próxima troca: " + LocalDateTime.now().plusMinutes(1) + RESET);
                        System.out.println(GREEN + "-".repeat(50) + RESET);

                        // Aplicar o canal
                        try {
                            Map<String, Object> resultado = configurarCanalMTX1(valorCanal);

                            if ("sucesso".equals(resultado.get("status"))) {
                                System.out.println(GREEN + "Canal " + canalEsperado + " aplicado com sucesso" + RESET);
                                System.out.println(GREEN + "Canal antes: " + resultado.get("canal_antes") + RESET);
                                System.out.println(GREEN + "Canal depois: " + resultado.get("canal_depois") + RESET);
                            } else {
                                System.out.println(GREEN + "Falha ao aplicar canal " + canalEsperado + ": " + resultado.get("mensagem") + RESET);
                            }

                        } catch (Exception e) {
                            System.err.println("ERRO ao aplicar canal " + canalEsperado + ": " + e.getMessage());
                            e.printStackTrace();
                        }

                        // Aguardar 1 minuto
                        System.out.println(GREEN + "\nAguardando 1 minuto..." + RESET);

                        // Aguardar em blocos de 10 segundos para permitir cancelamento mais rápido
                        for (int segundo = 1; segundo <= 6; segundo++) { // 6 x 10s = 60s
                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println(GREEN + "Aguardar interrompido" + RESET);
                                return;
                            }

                            TESTE_STATUS.put("tempo_restante_segundos", (6 - segundo) * 10);
                            TESTE_STATUS.put("segundo_atual", segundo + "/6");

                            System.out.println(GREEN + "Aguardando " + ((6 - segundo) * 10) + " segundos restantes" + RESET);

                            try {
                                Thread.sleep(10000); // 10 segundos
                            } catch (InterruptedException e) {
                                System.out.println(GREEN + "Thread interrompida durante a espera" + RESET);
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }

                        System.out.println(GREEN + "1 minuto concluído! Próximo canal..." + RESET);
                    }

                    // Incrementar ciclo
                    ciclo++;
                    TESTE_STATUS.put("ciclo_atual", ciclo);

                    System.out.println(GREEN + "\nCICLO " + (ciclo-1) + " COMPLETO!" + RESET);
                    System.out.println(GREEN + "Iniciando próximo ciclo em 10 segundos..." + RESET);

                    // Pequena pausa entre ciclos
                    Thread.sleep(10000);
                }

            } catch (Exception e) {
                System.err.println("ERRO FATAL no loop cíclico: " + e.getMessage());
                e.printStackTrace();

                TESTE_STATUS.put("status_geral", "erro_fatal");
                TESTE_STATUS.put("erro", e.getMessage());
                TESTE_STATUS.put("fim", LocalDateTime.now().toString());
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }

                System.out.println(GREEN + "LOOP CÍCLICO FINALIZADO" + RESET);
                TESTE_STATUS.put("status_geral", "finalizado");
                TESTE_STATUS.put("fim", LocalDateTime.now().toString());
            }
        });

        resposta.put("status", "loop_iniciado");
        resposta.put("mensagem", "Loop cíclico de canais iniciado");
        resposta.put("modo", "1_minuto_por_canal");
        resposta.put("ordem", ordemCanais);
        resposta.put("duracao_ciclo_completo", "3 minutos (3x1)");
        resposta.put("data_hora_inicio", LocalDateTime.now().toString());
        resposta.put("instrucao_cancelamento", "Use POST /cancelar-loop-canais1 para parar");

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para cancelar o loop
    @PostMapping("/cancelar-loop-canais1")
    public ResponseEntity<Map<String, Object>> cancelarLoopCanais1() {
        Map<String, Object> resposta = new HashMap<>();

        if (executorService == null || executorService.isShutdown()) {
            resposta.put("status", "nao_em_execucao");
            resposta.put("mensagem", "Nenhum loop está em execução");
            return ResponseEntity.ok(resposta);
        }

        // Marcar como cancelando
        TESTE_STATUS.put("status_geral", "cancelando");

        try {
            // Enviar interrupção para todas as threads
            executorService.shutdownNow();

            // Aguardar término
            boolean terminado = executorService.awaitTermination(10, TimeUnit.SECONDS);

            resposta.put("status", "cancelado");
            resposta.put("mensagem", "Loop cancelado com sucesso");
            resposta.put("finalizado_completamente", terminado);
            resposta.put("data_hora_cancelamento", LocalDateTime.now().toString());
            resposta.put("ciclo_parcial", TESTE_STATUS.get("ciclo_atual"));
            resposta.put("canal_parcial", TESTE_STATUS.get("canal_atual"));

            TESTE_STATUS.put("status_geral", "cancelado");
            TESTE_STATUS.put("fim", LocalDateTime.now().toString());

        } catch (InterruptedException e) {
            resposta.put("status", "erro_cancelamento");
            resposta.put("mensagem", "Erro ao cancelar: " + e.getMessage());
        }

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para ver status atual
    @GetMapping("/status-loop-canais1")
    public ResponseEntity<Map<String, Object>> getStatusLoopCanais1() {
        Map<String, Object> resposta = new HashMap<>(TESTE_STATUS);

        if (executorService == null || executorService.isShutdown()) {
            resposta.put("loop_ativo", false);
            if (!resposta.containsKey("status_geral")) {
                resposta.put("status_geral", "parado");
            }
        } else {
            resposta.put("loop_ativo", true);
        }

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para ver valores disponíveis
    @GetMapping("/valores-canal1")
    public ResponseEntity<Map<String, Object>> getValoresCanal1() {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "sucesso");
        resposta.put("valores", VALORES_CANAL);
        resposta.put("descricao", "Canal -> Número do Canal");

        return ResponseEntity.ok(resposta);
    }
}