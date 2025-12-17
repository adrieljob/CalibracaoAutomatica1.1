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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AjustarOffSetMtx2 {

    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";

    @Value("${app.username:admin}")
    private String username;

    @Value("${app.password:admin}")
    private String password;

    // comunicação com o index.html
    @PostMapping("/executar-rotina-completa-mtx2")
    public ResponseEntity<Map<String, Object>> executarRotinaCompleta() {
        Map<String, Object> respostaGeral = new HashMap<>();
        Map<String, Object> resultados = new HashMap<>();
        WebDriver driver = null;

        try {
            System.out.println(YELLOW + "=== INICIANDO ROTINA COMPLETA ===" + RESET);
            System.out.println(YELLOW + "Hora de início: " + LocalDateTime.now() + RESET);

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
            System.out.println(YELLOW + "Página acessada" + RESET);
            fazerLogin(driver, wait);
            System.out.println(YELLOW + "Login realizado" + RESET);

            // Processar cada canal
            String[] canais = {"14", "34", "51"};

            for (int i = 0; i < canais.length; i++) {
                String canal = canais[i];

                System.out.println(YELLOW + "\n" + "=".repeat(50) + RESET);
                System.out.println(YELLOW + "PROCESSANDO CANAL: " + canal + RESET);
                System.out.println(YELLOW + "=".repeat(50) + RESET);

                // Executar sequência completa para este canal
                Map<String, Object> resultadoCanal = processarCanalCompleto(driver, wait, canal);
                resultados.put("canal_" + canal, resultadoCanal);

                if (!"sucesso".equals(resultadoCanal.get("status"))) {
                    throw new RuntimeException("Falha no canal " + canal + ": " + resultadoCanal.get("mensagem"));
                }

                // Aguardar entre canais (exceto após o último)
                if (i < canais.length - 1) {
                    System.out.println(YELLOW + "\nAguardando 10 segundos antes do próximo canal..." + RESET);
                    Thread.sleep(10000);
                }
            }

            // Resposta final - CONVERTENDO LocalDateTime para String
            respostaGeral.put("status", "sucesso");
            respostaGeral.put("mensagem", "Rotina completa executada com sucesso");
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString()); // Convertendo para String
            respostaGeral.put("hora_fim", LocalDateTime.now().toString()); // Convertendo para String
            respostaGeral.put("resultados", resultados);

            System.out.println(YELLOW + "\n=== ROTINA COMPLETA FINALIZADA ===" + RESET);
            System.out.println(YELLOW + "Hora de fim: " + LocalDateTime.now() + RESET);

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
                System.out.println(YELLOW + "Driver finalizado" + RESET);
            }
        }
    }

    // função para ajustar os 3 canais direto
    private Map<String, Object> processarCanalCompleto(WebDriver driver, WebDriverWait wait, String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // ========== ETAPA 1: MUDAR CANAL ==========
            System.out.println(YELLOW + "\n[ETAPA 1] Mudando para canal: " + canal + RESET);
            String canalAntes = mudarCanal(driver, wait, canal);
            Thread.sleep(2000); // Aguardar mudança de canal

            // ========== ETAPA 2: AJUSTAR OFFSET (parte inicial) ==========
            System.out.println(YELLOW + "\n[ETAPA 2] Configurando offset e potência" + RESET);

            // 2.1. Desligar o MTX2
            System.out.println(YELLOW + "  2.1. Desligando MTX2" + RESET);
            desligarMTX2(driver, wait);

            // 2.2. Mudar offset para 0
            System.out.println(YELLOW + "  2.2. Configurando offset para 0" + RESET);
            configurarOffset(driver, wait, "0");

            // 2.3 Mudar Thershold para 500
            System.out.println(YELLOW + "  2.3. Configurando thershold para 500" + RESET);
            configurarThershold(driver, wait, "500");

            // 2.4. Mudar potência para 486
            System.out.println(YELLOW + "  2.4. Configurando potência para 486" + RESET);
            configurarPotencia(driver, wait, "486");

            // 2.5. Ligar o MTX2
            System.out.println(YELLOW + "  2.5. Ligando MTX2" + RESET);
            ligarMTX2(driver, wait);

            // 2.6. Esperar 5 minutos
            System.out.println(YELLOW + "  2.6. Aguardando 1 minutos para estabilização..." + RESET);
            Thread.sleep(60000); // 5 minutos = 300000 ms

            // ========== ETAPA 3: VERIFICAR E AJUSTAR DINAMICAMENTE ==========
            System.out.println(YELLOW + "\n[ETAPA 3] Verificando e ajustando dinamicamente" + RESET);

            // 2.6. Checa o canal (apenas para confirmar)
            String canalAtual = verificarCanal(driver, wait);
            System.out.println(YELLOW + "  Canal atual: " + canalAtual + RESET);

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
            System.out.println(YELLOW + "\n[ETAPA 4] Coletando resultados finais" + RESET);

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
            resultado.put("offset_inicial", "0");

        } catch (Exception e) {
            System.err.println("Erro no processamento do canal " + canal + ": " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro no canal " + canal + ": " + e.getMessage());
        }

        return resultado;
    }

    // separar texto do valor da corrente
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

    // ANTIGO - ta aqui só pra salvar (já que eu apago)
    // função principal
    private Map<String, Object> executarAjusteDinamicoPorCanalAntigo(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        Map<String, Object> resultado = new HashMap<>();
        int offsetAtual = 0; // Começa com 0
        int iteracoes = 0;
        int maxIteracoes = 50; // Prevenir loop infinito

        // Definir parâmetros baseado no canal
        int correnteMinima, correnteMaximaErro;

        switch (canal) {
            case "14":
                correnteMinima = 70;
                correnteMaximaErro = 73;
                System.out.println(YELLOW + "  Parâmetros para canal 14: 70-73 A" + RESET);
                break;
            case "34":
                correnteMinima = 65;
                correnteMaximaErro = 68;
                System.out.println(YELLOW + "  Parâmetros para canal 34: 65-68 A" + RESET);
                break;
            case "51":
                correnteMinima = 60;
                correnteMaximaErro = 63;
                System.out.println(YELLOW + "  Parâmetros para canal 51: 60-63 A" + RESET);
                break;
            default:
                throw new Exception("Canal não suportado: " + canal);
        }

        while (iteracoes < maxIteracoes) {
            iteracoes++;
            System.out.println(YELLOW + "\n  --- Loop " + iteracoes + " | Offset: " + offsetAtual + " ---" + RESET);

            // 2.6.1.1. Checa a corrente
            String correnteStr = verificarCorrente(driver, wait);
            double correnteDouble = Double.parseDouble(correnteStr);
            int corrente = (int) correnteDouble;
            System.out.println(YELLOW + "    Corrente atual: " + corrente + " A" + RESET);

            // 2.6.1.1.1. Se a corrente >= X (70, 65 ou 60 dependendo do canal)
            if (corrente >= correnteMinima) {
                System.out.println(YELLOW + "    Corrente atingiu o mínimo (" + correnteMinima + " A)" + RESET);

                // 2.6.1.1.1.1. Espera 10 seg
                System.out.println(YELLOW + "    Aguardando 20 10000 para verificação final..." + RESET);
                Thread.sleep(10000);

                // 2.6.1.1.1.2. Checa a corrente novamente
                correnteStr = verificarCorrente(driver, wait);
                correnteDouble = Double.parseDouble(correnteStr);
                corrente = (int) correnteDouble;
                System.out.println(YELLOW + "    Corrente após 10s: " + corrente + " A" + RESET);

                // 2.6.1.1.1.2.1. Se a corrente > Y (73, 68 ou 63) → ERRO
                if (corrente > correnteMaximaErro) {
                    String erroMsg = "ERRO: Corrente " + corrente + " A > limite máximo " + correnteMaximaErro + " A";
                    System.err.println("    " + erroMsg);
                    resultado.put("status", "erro");
                    resultado.put("mensagem", erroMsg);
                    resultado.put("iteracoes", iteracoes);
                    return resultado;
                }

                // 2.6.1.1.1.2.1. Se a corrente > Y (73, 68 ou 63) → ERRO
                if (corrente > correnteMaximaErro) {
                    String erroMsg = "Corrente " + corrente + " A > limite máximo " + correnteMaximaErro + " A. Ajustando offset...";
                    System.err.println("    " + erroMsg);

                    // Aumenta o offset em +5 para tentar reduzir a corrente
                    offsetAtual += 5;

                    // Adiciona informação no resultado (opcional)
                    if (!resultado.containsKey("ajustes")) {
                        resultado.put("ajustes", new ArrayList<String>());
                    }
                    ((List<String>) resultado.get("ajustes")).add(erroMsg + " Offset ajustado para: " + offsetAtual);

                    // Continua o loop para testar com novo offset
                    continue;
                }

                // 2.6.1.1.1.2.2. Se corrente >= mínimo e <= máximo → SUCESSO
                System.out.println(YELLOW + "    SUCESSO: Corrente " + corrente + " A dentro dos limites (" +
                        correnteMinima + "-" + correnteMaximaErro + " A)" + RESET);
                resultado.put("status", "sucesso");
                resultado.put("mensagem", "Corrente ajustada corretamente");
                resultado.put("corrente_final", corrente);
                resultado.put("offset_final", offsetAtual);
                resultado.put("iteracoes", iteracoes);
                return resultado;
            }

            // 2.6.1.1.2. Se a corrente < X
            System.out.println(YELLOW + "    Corrente abaixo do mínimo (" + correnteMinima + " A)" + RESET);

            // 2.6.1.1.2.1. Subtrai 1 do offset
            offsetAtual--;
            System.out.println(YELLOW + "    Reduzindo offset para: " + offsetAtual + RESET);

            if (offsetAtual < -100) {
                String erroMsg = "ERRO: Offset chegou a -100 e corrente ainda não atingiu " + correnteMinima + " A";
                System.err.println("    " + erroMsg);
                resultado.put("status", "erro");
                resultado.put("mensagem", erroMsg);
                resultado.put("iteracoes", iteracoes);
                return resultado;
            }

            // Aplicar novo offset (precisa desligar/ligar para aplicar)
            System.out.println(YELLOW + "    Aplicando novo offset " + offsetAtual + "..." + RESET);
            //desligarMTX2(driver, wait);
            configurarOffset(driver, wait, String.valueOf(offsetAtual));
            //ligarMTX2(driver, wait);

            // 2.6.1.1.2.1. Espera 10 seg
            System.out.println(YELLOW + "    Aguardando 10 segundos para estabilização..." + RESET);
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

    // NOVO
    // função principal
    private Map<String, Object> executarAjusteDinamicoPorCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {

        Map<String, Object> resultado = new HashMap<>();
        int offsetAtual = 0; // Começa com 0
        int iteracoes = 0; // Quantos loops
        int maxIteracoes = 50; // Prevenir loop infinito
        int maxAjustes = 10;  // Número máximo de ajustes permitidos
        int ajustesFeitos = 0; // Quantas vezes precisou voltar
        int offsetMaximo = -25; // Limite máximo para offset

        // Definir parâmetros baseado no canal
        int correnteMinima, correnteMaximaErro;

        switch (canal) {
            case "14":
                correnteMinima = 70;
                correnteMaximaErro = 73;
                System.out.println(YELLOW + "  Parâmetros para canal 14: 70-73 A" + RESET);
                break;
            case "34":
                correnteMinima = 65;
                correnteMaximaErro = 68;
                System.out.println(YELLOW + "  Parâmetros para canal 34: 65-68 A" + RESET);
                break;
            case "51":
                correnteMinima = 60;
                correnteMaximaErro = 63;
                System.out.println(YELLOW + "  Parâmetros para canal 51: 60-63 A" + RESET);
                break;
            default:
                throw new Exception("Canal não suportado: " + canal);
        }

        while (iteracoes < maxIteracoes) {
            iteracoes++;
            System.out.println(YELLOW + "\n  --- Loop " + iteracoes + " | Offset: " + offsetAtual + " | Ajustes: " + ajustesFeitos + "/" + maxAjustes + " ---" + RESET);

            // 2.6.1.1. Checa a corrente
            String correnteStr = verificarCorrente(driver, wait);
            double correnteDouble = Double.parseDouble(correnteStr);
            int corrente = (int) correnteDouble;
            System.out.println(YELLOW + "    Corrente atual: " + corrente + " A" + RESET);

            // VERIFICAÇÃO ADICIONAL: Se corrente = 0, checar potência também
            if (corrente == 0) {
                System.out.println(YELLOW + "    Corrente = 0 A. Verificando potência e corrente completa..." + RESET);

                try {
                    // Chama a função que verifica potência E corrente
                    String resultadoCompleto = verificarPotenciaEcorrente(driver, wait);
                    System.out.println(YELLOW + "    Resultado da verificação completa: " + resultadoCompleto + RESET);

                    // Continua com o fluxo normal
                } catch (Exception e) {
                    // Verifica se é a exceção de equipamento desligado
                    if (e.getMessage() != null && e.getMessage().contains("EQUIPAMENTO DESLIGADO")) {
                        System.out.println(YELLOW + "    " + e.getMessage() + RESET);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", e.getMessage());
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        resultado.put("equipamento_desligado", true);
                        return resultado;
                    } else {
                        // Relança outras exceções
                        throw e;
                    }
                }
            }
            /*
            // VERIFICAÇÃO ADICIONAL: Se corrente = 15, checar offset também
            if (canal == 14 && corrente == -15) {
                System.out.println(YELLOW + "    Corrente = -15 A. Verificando potência e corrente completa..." + RESET);

                try {
                    // Chama a função que verifica potência E corrente
                    String resultadoCompleto = verificarOffsetEcorrente(driver, wait);
                    System.out.println(YELLOW + "    Resultado da verificação completa: " + resultadoCompleto + RESET);

                    // Continua com o fluxo normal
                } catch (Exception e) {
                    // Verifica se é a exceção de equipamento desligado
                    if (e.getMessage() != null && e.getMessage().contains("EQUIPAMENTO DESLIGADO")) {
                        System.out.println(YELLOW + "    " + e.getMessage() + RESET);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", e.getMessage());
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        resultado.put("equipamento_desligado", true);
                        return resultado;
                    } else {
                        // Relança outras exceções
                        throw e;
                    }
                }
            }

             */

            // Continua normalmente
            // 2.6.1.1.1. Se a corrente >= X (70, 65 ou 60 dependendo do canal)
            if (corrente >= correnteMinima) {
                System.out.println(YELLOW + "    Corrente atingiu o mínimo (" + correnteMinima + " A)" + RESET);

                // 2.6.1.1.1.1. Espera 10 seg
                System.out.println(YELLOW + "    Aguardando 10 segundos para verificação final..." + RESET);
                Thread.sleep(10000);

                // 2.6.1.1.1.2. Checa a corrente novamente
                correnteStr = verificarCorrente(driver, wait);
                correnteDouble = Double.parseDouble(correnteStr);
                corrente = (int) correnteDouble;
                System.out.println(YELLOW + "    Corrente após 20s: " + corrente + " A" + RESET);

                // 2.6.1.1.1.2.1. Se a corrente > Y (73, 68 ou 63) → AJUSTAR OFFSET (+5)
                if (corrente > correnteMaximaErro) {
                    ajustesFeitos++;

                    // Verifica se atingiu limite máximo de ajustes
                    if (ajustesFeitos > maxAjustes) {
                        String erroFinal = "ERRO: Máximo de ajustes (" + maxAjustes + ") atingido. Corrente ainda alta: " + corrente + " A";
                        System.err.println("    " + erroFinal);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", erroFinal);
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        return resultado;
                    }

                    // Verifica se offset não ultrapassa o máximo
                    if (offsetAtual + 5 > offsetMaximo) {
                        String erroFinal = "ERRO: Offset máximo (" + offsetMaximo + ") atingido. Corrente ainda alta: " + corrente + " A";
                        System.err.println("    " + erroFinal);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", erroFinal);
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        return resultado;
                    }

                    String erroMsg = "Corrente " + corrente + " A > limite máximo " + correnteMaximaErro + " A. Ajustando offset...";
                    System.err.println("    " + erroMsg + " (Ajuste #" + ajustesFeitos + ")");

                    // Aumenta o offset em +5 para tentar reduzir a corrente
                    offsetAtual += 5;

                    // Adiciona informação no resultado (opcional)
                    if (!resultado.containsKey("ajustes")) {
                        resultado.put("ajustes", new ArrayList<String>());
                    }
                    ((List<String>) resultado.get("ajustes")).add(erroMsg + " Offset ajustado para: " + offsetAtual);

                    // Aplicar novo offset
                    System.out.println(YELLOW + "    Aplicando novo offset " + offsetAtual + "..." + RESET);
                    configurarOffset(driver, wait, String.valueOf(offsetAtual));

                    // Espera para estabilização
                    System.out.println(YELLOW + "    Aguardando 10 segundos para estabilização..." + RESET);
                    Thread.sleep(10000);

                    // Volta para testar novamente com o novo offset
                    continue;
                }

                // 2.6.1.1.1.2.2. Se corrente >= mínimo e <= máximo → SUCESSO
                System.out.println(YELLOW + "    SUCESSO: Corrente " + corrente + " A dentro dos limites (" +
                        correnteMinima + "-" + correnteMaximaErro + " A)" + RESET);
                resultado.put("status", "sucesso");
                resultado.put("mensagem", "Corrente ajustada corretamente");
                resultado.put("corrente_final", corrente);
                resultado.put("offset_final", offsetAtual);
                resultado.put("iteracoes", iteracoes);
                resultado.put("ajustes_feitos", ajustesFeitos);
                return resultado;
            }

            // 2.6.1.1.2. Se a corrente < X
            System.out.println(YELLOW + "    Corrente abaixo do mínimo (" + correnteMinima + " A)" + RESET);

            // 2.6.1.1.2.1. Subtrai 1 do offset
            offsetAtual--;
            System.out.println(YELLOW + "    Reduzindo offset para: " + offsetAtual + RESET);

            if (offsetAtual < -50) {
                String erroMsg = "ERRO: Offset chegou a -50 e corrente ainda não atingiu " + correnteMinima + " A";
                System.err.println("    " + erroMsg);
                resultado.put("status", "erro");
                resultado.put("mensagem", erroMsg);
                resultado.put("iteracoes", iteracoes);
                resultado.put("ajustes_feitos", ajustesFeitos);
                return resultado;
            }

            // Aplicar novo offset
            System.out.println(YELLOW + "    Aplicando novo offset " + offsetAtual + "..." + RESET);
            configurarOffset(driver, wait, String.valueOf(offsetAtual));

            // 2.6.1.1.2.1. Espera 20 seg
            System.out.println(YELLOW + "    Aguardando 10 segundos para estabilização..." + RESET);
            Thread.sleep(10000);

            // O loop continua verificando a corrente novamente
        }

        // Se chegou aqui, atingiu o máximo de iterações
        String erroMsg = "ERRO: Máximo de " + maxIteracoes + " iterações atingido";
        System.err.println("  " + erroMsg);
        resultado.put("status", "erro");
        resultado.put("mensagem", erroMsg);
        resultado.put("iteracoes", iteracoes);
        resultado.put("ajustes_feitos", ajustesFeitos);
        return resultado;
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
            System.out.println(YELLOW + "  Canal atual: " + canalAntes + RESET);

            // Se já estiver no canal correto, retornar
            if (canalAntes.equals(canal)) {
                System.out.println(YELLOW + "  Já está no canal " + canal + RESET);
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
            System.out.println(YELLOW + "  Canal configurado: " + canalDepois + RESET);

            // Verificar se realmente mudou
            int tentativas = 0;
            while (!canalDepois.equals(canal) && tentativas < 3) {
                System.out.println(YELLOW + "  Canal não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
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

    // desligar o módulo
    private void desligarMTX2(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar e configurar RfMasterOn para 2 (desligar)
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

        System.out.println(YELLOW + "  MTX2 desligado (RfMasterOn = 2)" + RESET);
        Thread.sleep(300);
    }

    // ligar o mnódulo
    private void ligarMTX2(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar e configurar RfMasterOn para 2 (desligar)
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

        System.out.println(YELLOW + "  MTX2 ligado (RfMasterOn = 1)" + RESET);
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

    // configurar novo valor para o thershold
    private void configurarThershold(WebDriver driver, WebDriverWait wait, String valorThershold) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        internal2.click();
        Thread.sleep(300);

        // Encontrar e configurar ForwardHigh
        WebElement offset = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.Threshold.ForwardHigh",
                "PowerAmplifier2_Config_Threshold_ForwardHigh");

        if (offset == null) {
            offset = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'ForwardHigh ')]")));
        }

        if (!configurarValor(driver, offset, valorThershold)) {
            throw new Exception("Falha ao configurar thershold para " + valorThershold);
        }

        System.out.println(YELLOW + "  Thershold configurado: " + valorThershold + RESET);
        Thread.sleep(300);
    }

    // configurar novo valor para o offset
    private void configurarOffset(WebDriver driver, WebDriverWait wait, String valorOffset) throws Exception {
        // Navegar para Internal2
        WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Internal2___']/input")));
        internal2.click();
        Thread.sleep(300);

        // Encontrar e configurar offset
        WebElement offset = encontrarElementoComTentativas(wait,
                "Internal2.power.offset",
                "Internal2_power_offset");

        if (offset == null) {
            offset = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'offset')]")));
        }

        if (!configurarValor(driver, offset, valorOffset)) {
            throw new Exception("Falha ao configurar offset para " + valorOffset);
        }

        System.out.println(YELLOW + "  Offset configurado: " + valorOffset + RESET);
        Thread.sleep(300);
    }

    // configurar novo valor para a potencia
    private void configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar OutputPower
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

        System.out.println(YELLOW + "  Potência configurada: " + potencia + RESET);
        Thread.sleep(300);
    }

    // configurar novo valor
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

    // verificar a potencia
    private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier2_Status_ForwardPower")));

        String textoCompleto = potenciaElement.getText().trim();
        double potenciaValor = extrairValorNumerico(textoCompleto);

        return String.valueOf(potenciaValor);
    }

    // verificar a corrente
    private String verificarCorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para Modulator2
        WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator2___']/input")));
        modulator2.click();
        Thread.sleep(300);

        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator2_mtrMainCurr")));

        String textoCompleto = correnteElement.getText().trim();

        System.out.println(YELLOW + "    Texto completo da corrente: " + textoCompleto + RESET);

        // Método robusto para extrair apenas os números
        double correnteValor = extrairValorNumerico(textoCompleto);

        System.out.println(YELLOW + "    Corrente extraída: " + correnteValor + " A" + RESET);

        return String.valueOf(correnteValor);
    }

    // verificar o canal
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
            System.out.println(YELLOW + "    Texto do elemento canal: " + canalTexto + RESET);

            // Estratégia 2: Se não conseguir, pegar o value attribute
            if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator2.Config.UpConverter.ChannelNumber")) {
                canalTexto = canalElement.getAttribute("value");
                System.out.println(YELLOW + "    Value attribute do canal: " + canalTexto + RESET);
            }

            // Estratégia 3: Se ainda não, tentar innerText
            if (canalTexto == null || canalTexto.isEmpty()) {
                canalTexto = canalElement.getAttribute("innerText");
                System.out.println(YELLOW + "    InnerText do canal: " + canalTexto + RESET);
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
                    System.out.println(YELLOW + "    Canal extraído: " + numeros + RESET);
                    return numeros;
                }
            }

            System.out.println(YELLOW + "    Não conseguiu extrair canal, retornando N/A" + RESET);
            return "N/A";

        } catch (Exception e) {
            System.err.println("    Erro ao verificar canal: " + e.getMessage());
            return "N/A";
        }
    }

    // verificação de corrente e potencia
    private String verificarPotenciaEcorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2 e verificar potência
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier2_Status_ForwardPower")));

        String textoPotencia = potenciaElement.getText().trim();
        double potenciaValor = extrairValorNumerico(textoPotencia);

        System.out.println(YELLOW + "    Potência extraída: " + potenciaValor + " W" + RESET);

        // Navegar para Modulator2 e verificar corrente
        WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator2___']/input")));
        modulator2.click();
        Thread.sleep(300);

        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator2_mtrMainCurr")));

        String textoCorrente = correnteElement.getText().trim();
        System.out.println(YELLOW + "    Texto completo da corrente: " + textoCorrente + RESET);

        double correnteValor = extrairValorNumerico(textoCorrente);
        System.out.println(YELLOW + "    Corrente extraída: " + correnteValor + " A" + RESET);

        // Verificar se equipamento desligou
        if (correnteValor == 0 && potenciaValor == 0) {
            throw new Exception("EQUIPAMENTO DESLIGADO - Corrente: " + correnteValor +
                    " A, Potência: " + potenciaValor + " W");
        }

        // Se apenas a corrente for 0, retorna a potência
        if (correnteValor == 0) {
            System.out.println(YELLOW + "    Corrente zero, retornando valor da potência" + RESET);
            return "Potência: " + potenciaValor + " W";
        }

        // Caso contrário, retorna ambos os valores
        return "Potência: " + potenciaValor + " W, Corrente: " + correnteValor + " A";
    }

    // verificar caso de erro canal 14
    private String verificarOffsetEcorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para internal2 e verificar o offset
        WebElement Internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Internal2___']/input")));
        Internal2.click();
        Thread.sleep(300);

        WebElement offsetElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Internal2_power_offset")));

        String textoOffset = offsetElement.getText().trim();
        double offsetValor = extrairValorNumerico(textoOffset);

        System.out.println(YELLOW + "    Offset extraído: " + offsetValor + RESET);

        // Navegar para Modulator2 e verificar offset
        // mudar para internal 2 e configurar para o offset
        WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator2___']/input")));
        modulator2.click();
        Thread.sleep(300);

        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator2_mtrMainCurr")));

        String textoCorrente = correnteElement.getText().trim();
        System.out.println(YELLOW + "    Texto completo da corrente: " + textoCorrente + RESET);

        double correnteValor = extrairValorNumerico(textoCorrente);
        System.out.println(YELLOW + "    Corrente extraída: " + correnteValor + " A" + RESET);

        // Verificar se equipamento desligou
        if (correnteValor == 0 && offsetValor == 0) {
            throw new Exception("EQUIPAMENTO DESLIGADO - Corrente: " + correnteValor +
                    " A, Offset: " + offsetValor);
        }

        // Se apenas a corrente for 0, retorna a potência
        if (correnteValor == 0) {
            System.out.println(YELLOW + "    Corrente zero, retornando valor do Offset" + RESET);
            return "Offset: " + offsetValor;
        }

        // Caso contrário, retorna ambos os valores
        return "Offset: " + offsetValor + ", Corrente: " + correnteValor + " A";
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

    // salvar LOG de informações dos canais
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
            System.out.println(YELLOW + "  Log salvo em: " + filePath + RESET);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log: " + e.getMessage());
        }
    }

    // debugar cada canal em especifico
    private void debugElemento(WebElement elemento, String nome) {
        try {
            System.out.println(YELLOW + "\n=== DEBUG " + nome + " ===" + RESET);
            System.out.println(YELLOW + "Tag: " + elemento.getTagName() + RESET);
            System.out.println(YELLOW + "Text: " + elemento.getText() + RESET);
            System.out.println(YELLOW + "Value attr: " + elemento.getAttribute("value") + RESET);
            System.out.println(YELLOW + "InnerText: " + elemento.getAttribute("innerText") + RESET);
            System.out.println(YELLOW + "OuterHTML: " + elemento.getAttribute("outerHTML") + RESET);
            System.out.println(YELLOW + "Displayed: " + elemento.isDisplayed() + RESET);
            System.out.println(YELLOW + "Enabled: " + elemento.isEnabled() + RESET);
            System.out.println(YELLOW + "==================\n" + RESET);
        } catch (Exception e) {
            System.err.println("Erro no debug: " + e.getMessage());
        }
    }

    @PostMapping("/executar-offset-canal-mtx2")
    public ResponseEntity<Map<String, Object>> executaroffsetcanalmtx2(@RequestParam String canal) {
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

    @PostMapping("/cancelar-offset-mtx2")
    public ResponseEntity<Map<String, Object>> cancelarOffsetMtx2() {
        Map<String, Object> resposta = new HashMap<>();

        System.out.println(YELLOW + "Solicitação de cancelamento de offset recebida" + RESET);

        resposta.put("status", "sucesso");
        resposta.put("mensagem", "Solicitação de cancelamento recebida");
        resposta.put("hora_cancelamento", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

}