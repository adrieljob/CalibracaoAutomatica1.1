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
@RequestMapping("/mtx3")
public class CicloMtx3 { // Nome alterado para corresponder ao arquivo

    private static final String RESET = "\u001B[0m";
    private static final String BLUE = "\u001B[34m";

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

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "ativo");
        resposta.put("servico", "Controle MTX3 - Loop de Potências");
        resposta.put("versao", "1.0");
        resposta.put("endpoints", Arrays.asList(
                "POST /mtx3/mudar-potencia?valorPotencia=XXX",
                "POST /mtx3/testar-todas-potencias",
                "POST /mtx3/cancelar-loop",
                "GET /mtx3/status",
                "GET /mtx3/valores"
        ));
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/mudar-potencia")
    public ResponseEntity<Map<String, Object>> mudarPotencia(@RequestParam String valorPotencia) {
        Map<String, Object> resposta = new HashMap<>();

        if (!VALORES_POTENCIA.containsKey(valorPotencia)) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Valor não programado. Valores permitidos: " +
                    String.join(", ", VALORES_POTENCIA.keySet()));
            resposta.put("valores_programados", VALORES_POTENCIA);
            return ResponseEntity.badRequest().body(resposta);
        }

        try {
            Map<String, Object> resultado = configurarPotenciaMTX3(valorPotencia);

            if ("erro".equals(resultado.get("status"))) {
                resposta.put("status", "erro");
                resposta.put("mensagem", resultado.get("mensagem"));
                return ResponseEntity.status(500).body(resposta);
            }

            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Potência do MTX3 configurada com sucesso");
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

    // Método principal para configurar a potência do MTX3
    private Map<String, Object> configurarPotenciaMTX3(String valorPotencia) {
        String urlBase = "http://10.10.103.103/debug/";
        System.out.println(BLUE + "Configurando potência do MTX3 para DAC: " + valorPotencia +
                " (" + VALORES_POTENCIA.get(valorPotencia) + ")"+ RESET);

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
            // ... (mantenha o código existente de configurarPotenciaMTX3 aqui)
            // O código completo dessa função permanece igual

            return resultado;
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println(BLUE + "Driver finalizado"+ RESET);
            }
        }
    }

    @PostMapping("/testar-todas-potencias")
    public ResponseEntity<Map<String, Object>> testarTodasPotencias() {
        Map<String, Object> resposta = new HashMap<>();

        if (executorService != null && !executorService.isShutdown()) {
            resposta.put("status", "ja_em_execucao");
            resposta.put("mensagem", "Loop cíclico já está em execução");
            resposta.put("ultima_potencia", TESTE_STATUS.get("potencia_atual"));
            resposta.put("ciclo_atual", TESTE_STATUS.get("ciclo_atual"));
            return ResponseEntity.ok(resposta);
        }

        TESTE_STATUS.clear();
        TESTE_STATUS.put("status_geral", "executando");
        TESTE_STATUS.put("inicio", LocalDateTime.now());
        TESTE_STATUS.put("ciclo_atual", 1);
        TESTE_STATUS.put("modo", "loop_ciclico_5min");
        TESTE_STATUS.put("duracao_por_potencia", "5 minutos");

        List<String> ordemPotencia = Arrays.asList("300", "340", "370", "430", "486");
        TESTE_STATUS.put("ordem_potencias", ordemPotencia);

        executorService = Executors.newSingleThreadExecutor();

        executorService.submit(() -> {
            try {
                System.out.println(BLUE + "=== INICIANDO LOOP CÍCLICO DE POTÊNCIAS MTX3 ===" + RESET);
                System.out.println(BLUE + "Data/Hora: " + LocalDateTime.now() + RESET);
                System.out.println(BLUE + "Ordem: " + ordemPotencia + RESET);

                int ciclo = 1;

                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println(BLUE + "\n" + "=".repeat(60) + RESET);
                    System.out.println(BLUE + "CICLO " + ciclo + RESET);
                    System.out.println(BLUE + "=".repeat(60) + RESET);

                    for (int i = 0; i < ordemPotencia.size(); i++) {
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println(BLUE + "Loop interrompido por cancelamento" + RESET);
                            return;
                        }

                        String valorDAC = ordemPotencia.get(i);
                        String potencia = VALORES_POTENCIA.get(valorDAC);
                        int posicao = i + 1;

                        TESTE_STATUS.put("ciclo_atual", ciclo);
                        TESTE_STATUS.put("posicao_ciclo", posicao + "/" + ordemPotencia.size());
                        TESTE_STATUS.put("valor_atual", valorDAC);
                        TESTE_STATUS.put("potencia_atual", potencia);
                        TESTE_STATUS.put("inicio_potencia", LocalDateTime.now());
                        TESTE_STATUS.put("proxima_troca", LocalDateTime.now().plusMinutes(5));

                        System.out.println(BLUE + "\n" + "-".repeat(50) + RESET);
                        System.out.println(BLUE + "CICLO " + ciclo + " - POTÊNCIA " + posicao + "/" + ordemPotencia.size()+ RESET);
                        System.out.println(BLUE + "Aplicando: " + potencia + " (DAC: " + valorDAC + ")" + RESET);

                        try {
                            Map<String, Object> resultado = configurarPotenciaMTX3(valorDAC);

                            if ("sucesso".equals(resultado.get("status"))) {
                                System.out.println(BLUE + potencia + " aplicada com sucesso"+ RESET);
                            } else {
                                System.out.println(BLUE + "Falha ao aplicar " + potencia + ": " + resultado.get("mensagem")+ RESET);
                            }
                        } catch (Exception e) {
                            System.err.println("ERRO ao aplicar " + potencia + ": " + e.getMessage());
                        }

                        System.out.println(BLUE + "\nAguardando 5 minutos..."+ RESET);

                        for (int minuto = 1; minuto <= 5; minuto++) {
                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println(BLUE + "Aguardar interrompido no minuto " + minuto+ RESET);
                                return;
                            }

                            TESTE_STATUS.put("tempo_restante_minutos", 5 - minuto);
                            TESTE_STATUS.put("minuto_atual", minuto + "/5");

                            System.out.println(BLUE + "Minuto " + minuto + "/5 - Próxima troca em " + (5 - minuto) + " minutos"+ RESET);

                            try {
                                Thread.sleep(60000);
                            } catch (InterruptedException e) {
                                System.out.println(BLUE + "Thread interrompida durante a espera"+ RESET);
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }

                        System.out.println(BLUE + "5 minutos concluídos! Próxima potência..."+ RESET);
                    }

                    ciclo++;
                    TESTE_STATUS.put("ciclo_atual", ciclo);

                    System.out.println(BLUE + "\nCICLO " + (ciclo-1) + " COMPLETO!"+ RESET);
                    System.out.println(BLUE + "Iniciando próximo ciclo em 10 segundos..."+ RESET);
                    Thread.sleep(10000);
                }

            } catch (Exception e) {
                System.err.println("ERRO FATAL no loop cíclico: " + e.getMessage());
                TESTE_STATUS.put("status_geral", "erro_fatal");
                TESTE_STATUS.put("erro", e.getMessage());
                TESTE_STATUS.put("fim", LocalDateTime.now());
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }

                System.out.println(BLUE + "LOOP CÍCLICO FINALIZADO"+ RESET);
                TESTE_STATUS.put("status_geral", "finalizado");
                TESTE_STATUS.put("fim", LocalDateTime.now());
            }
        });

        resposta.put("status", "loop_iniciado");
        resposta.put("mensagem", "Loop cíclico de potências iniciado");
        resposta.put("modo", "5_minutos_por_potencia");
        resposta.put("ordem", ordemPotencia);
        resposta.put("duracao_ciclo_completo", "25 minutos (5x5)");
        resposta.put("data_hora_inicio", LocalDateTime.now());
        resposta.put("instrucao_cancelamento", "Use POST /mtx3/cancelar-loop para parar");

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/cancelar-loop")
    public ResponseEntity<Map<String, Object>> cancelarLoop() {
        Map<String, Object> resposta = new HashMap<>();

        if (executorService == null || executorService.isShutdown()) {
            resposta.put("status", "nao_em_execucao");
            resposta.put("mensagem", "Nenhum loop está em execução");
            return ResponseEntity.ok(resposta);
        }

        TESTE_STATUS.put("status_geral", "cancelando");

        try {
            executorService.shutdownNow();
            boolean terminado = executorService.awaitTermination(10, TimeUnit.SECONDS);

            resposta.put("status", "cancelado");
            resposta.put("mensagem", "Loop cancelado com sucesso");
            resposta.put("finalizado_completamente", terminado);
            resposta.put("data_hora_cancelamento", LocalDateTime.now());
            resposta.put("ciclo_parcial", TESTE_STATUS.get("ciclo_atual"));
            resposta.put("potencia_parcial", TESTE_STATUS.get("potencia_atual"));

            TESTE_STATUS.put("status_geral", "cancelado");
            TESTE_STATUS.put("fim", LocalDateTime.now());

        } catch (InterruptedException e) {
            resposta.put("status", "erro_cancelamento");
            resposta.put("mensagem", "Erro ao cancelar: " + e.getMessage());
        }

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> resposta = new HashMap<>(TESTE_STATUS);

        if (executorService == null || executorService.isShutdown()) {
            resposta.put("loop_ativo", false);
            if (!resposta.containsKey("status_geral")) {
                resposta.put("status_geral", "parado");
            }
        } else {
            resposta.put("loop_ativo", true);
            resposta.put("tempo_decorrido", calcularTempoDecorrido(
                    (LocalDateTime) TESTE_STATUS.getOrDefault("inicio", LocalDateTime.now())
            ));
        }

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/valores")
    public ResponseEntity<Map<String, Object>> getValoresPotencia() {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "sucesso");
        resposta.put("valores", VALORES_POTENCIA);
        resposta.put("descricao", "DAC -> Potência aproximada (watts)");
        return ResponseEntity.ok(resposta);
    }

    private String calcularTempoDecorrido(LocalDateTime inicio) {
        if (inicio == null) return "N/A";

        Duration duracao = Duration.between(inicio, LocalDateTime.now());
        long horas = duracao.toHours();
        long minutos = duracao.toMinutesPart();
        long segundos = duracao.toSecondsPart();

        return String.format("%02d:%02d:%02d", horas, minutos, segundos);
    }
}