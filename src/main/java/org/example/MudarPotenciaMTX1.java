package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class MudarPotenciaMTX1 {

    // Mapa de valores pré-programados para potência
    private static final Map<String, String> VALORES_POTENCIA = new HashMap<>();

    static {
        VALORES_POTENCIA.put("300", "1W");
        VALORES_POTENCIA.put("340", "2.5W");
        VALORES_POTENCIA.put("370", "5W");
        VALORES_POTENCIA.put("430", "20W");
        VALORES_POTENCIA.put("486", "67.3W");
    }

    // Variáveis para o loop cíclico
    private static final Map<String, Object> TESTE_STATUS = new ConcurrentHashMap<>();
    private static ExecutorService executorService = null;

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @PostMapping("/mudar-potencia-mtx1")
    public ResponseEntity<Map<String, Object>> mudarPotencia1(@RequestParam String valorPotencia) {
        Map<String, Object> resposta = new HashMap<>();

        if (!VALORES_POTENCIA.containsKey(valorPotencia)) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Valor não programado. Valores permitidos: " +
                    String.join(", ", VALORES_POTENCIA.keySet()));
            resposta.put("valores_programados", VALORES_POTENCIA);
            return ResponseEntity.badRequest().body(resposta);
        }

        try {
            Map<String, Object> resultado = configurarPotenciaMTX1(valorPotencia);

            // Se houve erro no processo interno
            if ("erro".equals(resultado.get("status"))) {
                resposta.put("status", "erro");
                resposta.put("mensagem", resultado.get("mensagem"));
                return ResponseEntity.status(500).body(resposta);
            }

            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Potência do MTX1 configurada com sucesso");
            resposta.put("valor_configurado", valorPotencia);
            resposta.put("potencia_esperada", VALORES_POTENCIA.get(valorPotencia));
            resposta.put("potencia_antes", resultado.get("potencia_antes"));
            resposta.put("potencia_depois", resultado.get("potencia_depois"));

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro ao configurar potência: " + e.getMessage());
            resposta.put("valor_configurado", valorPotencia);

            return ResponseEntity.status(500).body(resposta);
        }
    }

    // Método principal para configurar a potência do MTX1
    private Map<String, Object> configurarPotenciaMTX1(String valorPotencia) {
        String urlBase = "http://10.10.103.103/debug/";
        System.out.println("Configurando potência do MTX1 para DAC: " + valorPotencia +
                " (" + VALORES_POTENCIA.get(valorPotencia) + ")");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        // options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--disable-cache");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);
            System.out.println("Acessando: " + urlBase);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
            campoSenha.clear();
            campoSenha.sendKeys(password);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
            botaoLogin.click();
            System.out.println("Login realizado");

            // Aguardar carregamento
            //Thread.sleep(300);

            // Acessa PowerAmp 1
            WebElement powerAmp1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier1___']/input")));
            powerAmp1.click();
            Thread.sleep(300);
            System.out.println("PowerAmplifier1 selecionado");

            // Pega o valor da potencia ANTES da mudança
            WebElement getPotenciaAntes = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_ForwardPower")));
            String potenciaAntes = getPotenciaAntes.getText().trim();
            System.out.println("Potência ANTES: " + potenciaAntes);

            // *** ETAPA 1: Clica no OutputPower para abrir o diálogo ***
            System.out.println("Clicando em OutputPower...");

            // Tentar diferentes formas de encontrar o OutputPower
            WebElement outputPower = null;
            try {
                // Tentativa 1: Pelo ID exato
                outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("PowerAmplifier1.Config.OutputPower")));
                System.out.println("OutputPower encontrado pelo ID exato");
            } catch (Exception e1) {
                try {
                    // Tentativa 1: Pelo ID com underscore
                    outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                            By.id("PowerAmplifier1_Config_OutputPower")));
                    System.out.println("OutputPower encontrado pelo ID com underscore");
                } catch (Exception e2) {
                    try {
                        // Tentativa 3: Pelo texto
                        outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//*[contains(text(), 'OutputPower')]")));
                        System.out.println("OutputPower encontrado pelo texto");
                    } catch (Exception e3) {
                        resultado.put("status", "erro");
                        resultado.put("mensagem", "OutputPower não encontrado");
                        return resultado;
                    }
                }
            }

            outputPower.click();
            Thread.sleep(300);
            System.out.println("OutputPower clicado - diálogo deve estar aberto");

            // *** ETAPA 2: Encontrar e preencher o campo New Value ***
            System.out.println("Procurando campo New Value...");

            WebElement campoNewValue = null;

            // Estratégia 1: Procurar input ativo (focado)
            try {
                campoNewValue = driver.switchTo().activeElement();
                if (campoNewValue.getTagName().equals("input") || campoNewValue.getTagName().equals("textarea")) {
                    System.out.println("Campo ativo encontrado: " + campoNewValue.getTagName());
                } else {
                    campoNewValue = null;
                }
            } catch (Exception e) {
                System.out.println("Não encontrou campo ativo");
            }

            // Estratégia 2: Procurar por inputs visíveis no diálogo
            if (campoNewValue == null) {
                try {
                    // Primeiro, tentar encontrar o diálogo/modal
                    WebElement dialogo = driver.findElement(
                            By.xpath("//div[contains(@class, 'modal') or contains(@class, 'dialog') or " +
                                    "contains(@style, 'display: block') or @role='dialog']"));

                    // Dentro do diálogo, procurar input
                    campoNewValue = dialogo.findElement(By.tagName("input"));
                    System.out.println("Campo encontrado dentro do diálogo");
                } catch (Exception e) {
                    System.out.println("Não encontrou diálogo específico");
                }
            }

            // Estratégia 3: Procurar por qualquer input visível
            if (campoNewValue == null) {
                try {
                    // Procurar todos os inputs e filtrar os visíveis
                    java.util.List<WebElement> todosInputs = driver.findElements(By.tagName("input"));
                    for (WebElement input : todosInputs) {
                        if (input.isDisplayed() && input.isEnabled()) {
                            // Verificar se é um campo de texto/número
                            String type = input.getAttribute("type");
                            if (type == null || type.equals("text") || type.equals("number")) {
                                campoNewValue = input;
                                System.out.println("Campo visível encontrado: input");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Não encontrou input visível");
                }
            }

            // Estratégia 4: Procurar pelo seu XPath original
            if (campoNewValue == null) {
                try {
                    campoNewValue = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//label[@type='text']/input")));
                    System.out.println("Campo encontrado pelo XPath original");
                } catch (Exception e) {
                    // Tentar variações do XPath
                    try {
                        campoNewValue = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//label[contains(text(), 'New Value')]/input")));
                        System.out.println("Campo encontrado pelo texto 'New Value'");
                    } catch (Exception e2) {
                        try {
                            campoNewValue = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//input[@placeholder='New Value' or contains(@placeholder, 'value')]")));
                            System.out.println("Campo encontrado pelo placeholder");
                        } catch (Exception e3) {
                            resultado.put("status", "erro");
                            resultado.put("mensagem", "Campo New Value não encontrado");
                            return resultado;
                        }
                    }
                }
            }

            // *** ETAPA 3: Digitar o valor da potência ***
            if (campoNewValue != null) {
                System.out.println("Campo New Value encontrado. Digitando valor: " + valorPotencia);

                // Clicar no campo para garantir foco
                campoNewValue.click();
                Thread.sleep(300);

                // Limpar o campo (Ctrl+A + Delete)
                campoNewValue.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                Thread.sleep(300);
                campoNewValue.sendKeys(Keys.DELETE);
                Thread.sleep(300);

                // Digitar o novo valor
                campoNewValue.sendKeys(valorPotencia);
                System.out.println("Valor " + valorPotencia + " digitado no campo New Value");
                Thread.sleep(300);

                // *** ETAPA 4: Confirmar a mudança ***
                System.out.println("Confirmando a mudança...");

                // Método 1: Pressionar Enter
                campoNewValue.sendKeys(Keys.ENTER);
                Thread.sleep(300);

                // Método 2: Procurar e clicar em botão OK/Apply
                try {
                    WebElement botaoOk = driver.findElement(
                            By.xpath("//button[contains(text(), 'OK') or " +
                                    "contains(text(), 'Apply') or " +
                                    "contains(text(), 'Save') or " +
                                    "contains(text(), 'Confirm')]"));
                    if (botaoOk.isDisplayed() && botaoOk.isEnabled()) {
                        botaoOk.click();
                        System.out.println("Botão OK/Apply clicado");
                        Thread.sleep(300);
                    }
                } catch (Exception e) {
                    System.out.println("Botão OK não encontrado, usando apenas Enter");
                }

                // Método 3: Tentar clicar fora do campo para confirmar
                try {
                    // Clicar em algum lugar da página para sair do campo
                    driver.findElement(By.tagName("body")).click();
                    Thread.sleep(300);
                } catch (Exception e) {
                    // Ignorar
                }

            } else {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Não foi possível encontrar o campo New Value");
                return resultado;
            }

            // Aguardar aplicação da mudança
            System.out.println("Aguardando aplicação da mudança...");
            Thread.sleep(300);

            // Recarregar a página para ver mudanças (opcional)
            try {
                driver.navigate().refresh();
                Thread.sleep(300);

                // Re-selecionar PowerAmplifier1 após refresh
                powerAmp1 = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[@link='PowerAmplifier1___']/input")));
                powerAmp1.click();
                Thread.sleep(300);
            } catch (Exception e) {
                System.out.println("Não foi possível recarregar: " + e.getMessage());
            }

            // Pega o valor da potencia DEPOIS da mudança
            WebElement getPotenciaDepois = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_ForwardPower")));
            String potenciaDepois = getPotenciaDepois.getText().trim();
            System.out.println("Potência DEPOIS: " + potenciaDepois);

            // Verificar também o valor configurado atual
            try {
                WebElement campoConfigurado = driver.findElement(By.id("PowerAmplifier1_Config_OutputPower"));
                String valorAtual = campoConfigurado.getText();
                if (valorAtual == null || valorAtual.isEmpty()) {
                    valorAtual = campoConfigurado.getAttribute("value");
                }
                System.out.println("Valor configurado atual: " + valorAtual);
            } catch (Exception e) {
                System.out.println("Não foi possível verificar valor configurado atual");
            }

            // Registrar resultados
            resultado.put("status", "sucesso");
            resultado.put("potencia_antes", potenciaAntes);
            resultado.put("potencia_depois", potenciaDepois);
            resultado.put("valor_configurado", valorPotencia);
            resultado.put("potencia_esperada", VALORES_POTENCIA.get(valorPotencia));

            // Salvar log
            String filePath = System.getProperty("user.dir") + "/potencia_mtx1.txt";
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
                writer.write(LocalDateTime.now() + " | DAC: " + valorPotencia +
                        " | Esperado: " + VALORES_POTENCIA.get(valorPotencia) +
                        " | Antes: " + potenciaAntes +
                        " | Depois: " + potenciaDepois + "\n");
                System.out.println("Log salvo: " + filePath);
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
                System.out.println("Driver finalizado");
            }
        }

        return resultado;
    }

    // Loop pelas potencias
    @PostMapping("/testar-todas-potencias1")
    public ResponseEntity<Map<String, Object>> testarTodasPotencias1() {
        Map<String, Object> resposta = new HashMap<>();

        // Verificar se já está em execução
        if (executorService != null && !executorService.isShutdown()) {
            resposta.put("status", "ja_em_execucao");
            resposta.put("mensagem", "Loop cíclico já está em execução");
            resposta.put("ultima_potencia", TESTE_STATUS.get("potencia_atual"));
            resposta.put("ciclo_atual", TESTE_STATUS.get("ciclo_atual"));
            return ResponseEntity.ok(resposta);
        }

        // Resetar status
        TESTE_STATUS.clear();
        TESTE_STATUS.put("status_geral", "executando");
        TESTE_STATUS.put("inicio", LocalDateTime.now().toString()); // CORRIGIDO
        TESTE_STATUS.put("ciclo_atual", 1);
        TESTE_STATUS.put("modo", "loop_ciclico_5min");
        TESTE_STATUS.put("duracao_por_potencia", "5 minutos");

        // Ordem das potências para o ciclo
        List<String> ordemPotencia = Arrays.asList("300", "340", "370", "430", "486");
        TESTE_STATUS.put("ordem_potencias", ordemPotencia);

        // Criar executor com thread única
        executorService = Executors.newSingleThreadExecutor();

        executorService.submit(() -> {
            try {
                System.out.println("=== INICIANDO LOOP CÍCLICO DE POTÊNCIAS MTX1 ===");
                System.out.println("Data/Hora: " + LocalDateTime.now());
                System.out.println("Modo: Loop cíclico de 5 minutos por potência");
                System.out.println("Ordem: " + ordemPotencia);
                System.out.println("Duração por potência: 5 minutos");
                System.out.println("Ciclo completo: 25 minutos (5 potências x 5 minutos)");

                int ciclo = 1;

                // Loop infinito (até ser cancelado)
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("CICLO " + ciclo);
                    System.out.println("=".repeat(60));

                    for (int i = 0; i < ordemPotencia.size(); i++) {
                        // Verificar se foi solicitado cancelamento
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("Loop interrompido por cancelamento");
                            return;
                        }

                        String valorDAC = ordemPotencia.get(i);
                        String potencia = VALORES_POTENCIA.get(valorDAC);
                        int posicao = i + 1;

                        // Atualizar status
                        TESTE_STATUS.put("ciclo_atual", ciclo);
                        TESTE_STATUS.put("posicao_ciclo", posicao + "/" + ordemPotencia.size());
                        TESTE_STATUS.put("valor_atual", valorDAC);
                        TESTE_STATUS.put("potencia_atual", potencia);
                        TESTE_STATUS.put("inicio_potencia", LocalDateTime.now().toString()); // CORRIGIDO
                        TESTE_STATUS.put("proxima_troca", LocalDateTime.now().plusMinutes(5).toString()); // CORRIGIDO

                        System.out.println("\n" + "-".repeat(50));
                        System.out.println("CICLO " + ciclo + " - POTÊNCIA " + posicao + "/" + ordemPotencia.size());
                        System.out.println("Aplicando: " + potencia + " (DAC: " + valorDAC + ")");
                        System.out.println("Início: " + LocalDateTime.now());
                        System.out.println("Próxima troca: " + LocalDateTime.now().plusMinutes(5));
                        System.out.println("-".repeat(50));

                        // Aplicar a potência
                        try {
                            Map<String, Object> resultado = configurarPotenciaMTX1(valorDAC);

                            if ("sucesso".equals(resultado.get("status"))) {
                                System.out.println(potencia + " aplicada com sucesso");
                                System.out.println("Potência antes: " + resultado.get("potencia_antes"));
                                System.out.println("Potência depois: " + resultado.get("potencia_depois"));
                            } else {
                                System.out.println("Falha ao aplicar " + potencia + ": " + resultado.get("mensagem"));
                            }


                        } catch (Exception e) {
                            System.err.println("ERRO ao aplicar " + potencia + ": " + e.getMessage());
                            e.printStackTrace();
                        }

                        // Aguardar 5 minutos (300000 milissegundos)
                        System.out.println("\nAguardando 1 minuto...");

                        // Aguardar em blocos de 1 minuto para permitir cancelamento mais rápido
                        for (int minuto = 1; minuto <= 1; minuto++) {
                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println("Aguardar interrompido no minuto " + minuto);
                                return;
                            }

                            TESTE_STATUS.put("tempo_restante_minutos", 1 - minuto);
                            TESTE_STATUS.put("minuto_atual", minuto + "/5");

                            System.out.println("Minuto " + minuto + "/5 - Próxima troca em " + (1 - minuto) + " minutos");

                            try {
                                Thread.sleep(60000); // 1 minuto
                            } catch (InterruptedException e) {
                                System.out.println("Thread interrompida durante a espera");
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }

                        System.out.println("5 minutos concluídos! Próxima potência...");
                    }

                    // Incrementar ciclo
                    ciclo++;
                    TESTE_STATUS.put("ciclo_atual", ciclo);

                    System.out.println("\nCICLO " + (ciclo-1) + " COMPLETO!");
                    System.out.println("Iniciando próximo ciclo em 10 segundos...");

                    // Pequena pausa entre ciclos
                    Thread.sleep(10000);
                }

            } catch (Exception e) {
                System.err.println("ERRO FATAL no loop cíclico: " + e.getMessage());
                e.printStackTrace();

                TESTE_STATUS.put("status_geral", "erro_fatal");
                TESTE_STATUS.put("erro", e.getMessage());
                TESTE_STATUS.put("fim", LocalDateTime.now().toString()); // CORRIGIDO
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }

                System.out.println("LOOP CÍCLICO FINALIZADO");
                TESTE_STATUS.put("status_geral", "finalizado");
                TESTE_STATUS.put("fim", LocalDateTime.now().toString()); // CORRIGIDO
            }
        });

        resposta.put("status", "loop_iniciado");
        resposta.put("mensagem", "Loop cíclico de potências iniciado");
        resposta.put("modo", "5_minutos_por_potencia");
        resposta.put("ordem", ordemPotencia);
        resposta.put("duracao_ciclo_completo", "25 minutos (5x5)");
        resposta.put("data_hora_inicio", LocalDateTime.now().toString()); // CORRIGIDO
        resposta.put("instrucao_cancelamento", "Use POST /cancelar-loop-potencias1 para parar");

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para cancelar o loop
    @PostMapping("/cancelar-loop-potencias1")
    public ResponseEntity<Map<String, Object>> cancelarLoopPotencias1() {
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
            resposta.put("data_hora_cancelamento", LocalDateTime.now().toString()); // CORRIGIDO
            resposta.put("ciclo_parcial", TESTE_STATUS.get("ciclo_atual"));
            resposta.put("potencia_parcial", TESTE_STATUS.get("potencia_atual"));

            TESTE_STATUS.put("status_geral", "cancelado");
            TESTE_STATUS.put("fim", LocalDateTime.now().toString()); // CORRIGIDO

        } catch (InterruptedException e) {
            resposta.put("status", "erro_cancelamento");
            resposta.put("mensagem", "Erro ao cancelar: " + e.getMessage());
        }

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para ver status atual
    @GetMapping("/status-loop-potencias1")
    public ResponseEntity<Map<String, Object>> getStatusLoopPotencias1() {
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
    @GetMapping("/valores-potencia1")
    public ResponseEntity<Map<String, Object>> getValoresPotencia1() {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "sucesso");
        resposta.put("valores", VALORES_POTENCIA);
        resposta.put("descricao", "DAC -> Potência aproximada (watts)");

        return ResponseEntity.ok(resposta);
    }
}