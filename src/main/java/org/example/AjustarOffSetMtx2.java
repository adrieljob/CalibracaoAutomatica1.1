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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AjustarOffSetMtx2 {

    // Códigos de cores para console (amarelo para MTX2)
    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";

    // Credenciais de login (configuráveis via application.properties)
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
            System.out.println(YELLOW + "=== INICIANDO ROTINA COMPLETA MTX2 ===" + RESET);
            System.out.println(YELLOW + "Hora de início: " + LocalDateTime.now() + RESET);

            // Configurar ChromeDriver UMA VEZ para toda a rotina
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-gpu");
            options.addArguments("--headless");// se quiser ver oque está acontecendo comentar essa linha
            options.addArguments("--incognito");
            options.addArguments("--disable-cache");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // ETAPA 1: LOGIN (apenas uma vez)
            driver.get("http://10.10.103.103/debug/");
            System.out.println(YELLOW + "Página acessada" + RESET);
            fazerLogin(driver, wait);
            System.out.println(YELLOW + "Login realizado" + RESET);

            // Processar cada canal na sequência: 14, 34, 51
            String[] canais = {"14", "34", "51"};

            for (int i = 0; i < canais.length; i++) {
                String canal = canais[i];

                System.out.println(YELLOW + "\n" + "=".repeat(50) + RESET);
                System.out.println(YELLOW + "PROCESSANDO CANAL MTX2: " + canal + RESET);
                System.out.println(YELLOW + "=".repeat(50) + RESET);

                // Executar sequência completa para este canal
                Map<String, Object> resultadoCanal = processarCanalCompleto(driver, wait, canal);
                resultados.put("canal_" + canal, resultadoCanal);

                // Se houver falha em um canal, interromper toda a rotina
                if (!"sucesso".equals(resultadoCanal.get("status"))) {
                    throw new RuntimeException("Falha no canal MTX2 " + canal + ": " + resultadoCanal.get("mensagem"));
                }

                // Aguardar entre canais (exceto após o último)
                if (i < canais.length - 1) {
                    System.out.println(YELLOW + "\nAguardando 10 segundos antes do próximo canal..." + RESET);
                    Thread.sleep(10000);
                }
            }

            // Resposta final - CONVERTENDO LocalDateTime para String
            respostaGeral.put("status", "sucesso");
            respostaGeral.put("mensagem", "Rotina completa MTX2 executada com sucesso");
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
            respostaGeral.put("hora_fim", LocalDateTime.now().toString());
            respostaGeral.put("resultados", resultados);

            System.out.println(YELLOW + "\n=== ROTINA COMPLETA MTX2 FINALIZADA ===" + RESET);
            System.out.println(YELLOW + "Hora de fim: " + LocalDateTime.now() + RESET);

            return ResponseEntity.ok(respostaGeral);

        } catch (Exception e) {
            // Preparar resposta de erro
            respostaGeral.put("status", "erro");
            respostaGeral.put("mensagem", "Erro na rotina completa MTX2: " + e.getMessage());
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
            respostaGeral.put("resultados", resultados);

            System.err.println("Erro na rotina completa MTX2: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(respostaGeral);
        } finally {
            // Garantir que o driver seja fechado mesmo em caso de erro
            if (driver != null) {
                driver.quit();
                System.out.println(YELLOW + "Driver finalizado" + RESET);
            }
        }
    }

    // Processa um canal individualmente com todas as etapas
    private Map<String, Object> processarCanalCompleto(WebDriver driver, WebDriverWait wait, String canal) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // ========== ETAPA 1: MUDAR CANAL ==========
            System.out.println(YELLOW + "\n[ETAPA 1] Mudando para canal: " + canal + RESET);
            String canalAntes = mudarCanal(driver, wait, canal);
            Thread.sleep(2000);

            // ========== ETAPA 2: AJUSTAR OFFSET (parte inicial) ==========
            System.out.println(YELLOW + "\n[ETAPA 2] Configurando offset e potência" + RESET);

            // 2.1. Desligar o MTX1
            System.out.println(YELLOW + "  2.1. Desligando MTX1" + RESET);
            desligarMTX2(driver, wait);

            // 2.2. Mudar Thershold para 500
            System.out.println(YELLOW + "  2.2. Configurando thershold para 500" + RESET);
            configurarThershold(driver, wait, "500");

            // 2.3. Mudar potência para 486
            System.out.println(YELLOW + "  2.3. Configurando potência para 486" + RESET);
            configurarPotencia(driver, wait, "486");

            // 2.4. Ligar o MTX1
            System.out.println(YELLOW + "  2.4. Ligando MTX1" + RESET);
            ligarMTX2(driver, wait);

            // 2.5. Esperar 5 minutos
            System.out.println(YELLOW + "  2.5. Aguardando 1 minutos para estabilização..." + RESET);
            Thread.sleep(60000);

            // ========== ETAPA 3: VERIFICAR E AJUSTAR DINAMICAMENTE ==========
            System.out.println(YELLOW + "\n[ETAPA 3] Verificando e ajustando dinamicamente" + RESET);

            // 2.6. Checa o canal (apenas para confirmar)
            String canalAtual = verificarCanal(driver, wait);
            System.out.println(YELLOW + "  Canal atual: " + canalAtual + RESET);

            // Verificação FLEXÍVEL do canal
            if (!canalAtual.equals(canal) && !canalAtual.contains(canal)) {
                System.err.println("  AVISO: Canal lido (" + canalAtual + ") diferente do esperado (" + canal + ")");
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
            resultado.put("offset_inicial", resultadoAjuste.get("offset_inicial"));

			// ========== ETAPA 6: FAZER CHECAGEM FINAL DE VALORES ==========
			System.out.println(YELLOW + "\n[ETAPA 6] FAZENDO CHECAGEM FINAL" + RESET);

			Map<String, Object> buscaResultado = chamarBuscaAutomatica();
			resultado.put("busca_automatica", buscaResultado);

			// ========== ETAPA 7: DESLIGAR MTX2 ==========
			System.out.println(YELLOW + "\n[ETAPA 7] Desligando MTX2" + RESET);
			desligarMTX2(driver, wait);


		} catch (Exception e) {
            System.err.println("Erro no processamento do canal " + canal + ": " + e.getMessage());
            e.printStackTrace();

            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro no canal " + canal + ": " + e.getMessage());

            // Mesmo com erro, tentar cancelar
            try {
                chamarFuncaoCancelamento(canal, resultado);
            } catch (Exception ex) {
                System.err.println("Erro no cancelamento após falha: " + ex.getMessage());
            }
        }

        return resultado;
    }

	// Método para chamar a checagem final
	private Map chamarBuscaAutomatica() {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String url = "http://localhost:8087/executar-manualmente";
			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				System.out.println(YELLOW + "  Busca automática executada com sucesso" + RESET);
				return response.getBody();
			}
		} catch (Exception e) {
			System.err.println("  Erro na busca automática: " + e.getMessage());
		}

		Map<String, Object> erro = new HashMap<>();
		erro.put("status", "aviso");
		erro.put("mensagem", "Busca automática não executada");
		return erro;
	}

    // Método para chamar o cancelamento após processar um canal
    private void chamarFuncaoCancelamento(String canal, Map<String, Object> resultadoCanal) {
        try {
            System.out.println(YELLOW + "\n=== CHAMANDO CANCELAMENTO PARA CANAL " + canal + " ===" + RESET);

            // Preparar dados do cancelamento
            Map<String, Object> dadosCancelamento = new HashMap<>();
            dadosCancelamento.put("canal", canal);
            dadosCancelamento.put("hora_processamento", LocalDateTime.now().toString());
            dadosCancelamento.put("corrente_final", resultadoCanal.get("corrente_final"));
            dadosCancelamento.put("offset_final", resultadoCanal.get("offset_final"));
            dadosCancelamento.put("potencia_final", resultadoCanal.get("potencia_final"));
            dadosCancelamento.put("status_processamento", resultadoCanal.get("status"));

            // Chamar o endpoint de cancelamento (simulação)
            Map<String, Object> respostaCancelamento = new HashMap<>();
            respostaCancelamento.put("status", "sucesso");
            respostaCancelamento.put("mensagem", "Cancelamento chamado para canal " + canal);
            respostaCancelamento.put("hora_cancelamento", LocalDateTime.now().toString());
            respostaCancelamento.put("dados_canal", dadosCancelamento);

            // Log do cancelamento
            System.out.println(YELLOW + "Cancelamento executado: " + respostaCancelamento + RESET);

            // Salvar log específico do cancelamento
            salvarLogCancelamento(canal, respostaCancelamento);

        } catch (Exception e) {
            System.err.println("Erro ao chamar cancelamento para canal " + canal + ": " + e.getMessage());
        }
    }

    // Método para salvar log do cancelamento
    private void salvarLogCancelamento(String canal, Map<String, Object> respostaCancelamento) {
        String filePath = System.getProperty("user.dir") + "/logs_cancelamento.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | Canal: " + canal +
                    " | Status: " + respostaCancelamento.get("status") +
                    " | Hora: " + respostaCancelamento.get("hora_cancelamento") +
                    " | Mensagem: " + respostaCancelamento.get("mensagem") + "\n");
            System.out.println(YELLOW + "  Log de cancelamento salvo em: " + filePath + RESET);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log de cancelamento: " + e.getMessage());
        }
    }

    // Extrai valor numérico de strings como "Modulator2.mtrMainCurr = 39"
    private double extrairValorNumerico(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Se já for um número direto, parse direto
            if (texto.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(texto);
            }

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

    // função principal
    private Map<String, Object> executarAjusteDinamicoPorCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        Map<String, Object> resultado = new HashMap<>();

        // LER OFFSET ATUAL DO EQUIPAMENTO MTX2
        int offsetAtual = lerOffsetAtual(driver, wait);
        System.out.println(YELLOW + "  Offset inicial lido do equipamento MTX2: " + offsetAtual + RESET);
        resultado.put("offset_inicial", offsetAtual);

        int iteracoes = 0;
        int maxIteracoes = 50;
        int maxAjustes = 10;
        int ajustesFeitos = 0;
        int offsetMaximo = -25;

        int correnteMinima, correnteMaximaErro;

        // Definir limites de corrente para cada canal do MTX2
        switch (canal) {
            case "14":
                correnteMinima = 70;
                correnteMaximaErro = 73;
                System.out.println(YELLOW + "  Parâmetros para canal 14 do MTX2: 70-73 A" + RESET);
                break;
            case "34":
                correnteMinima = 65;
                correnteMaximaErro = 68;
                System.out.println(YELLOW + "  Parâmetros para canal 34 do MTX2: 65-68 A" + RESET);
                break;
            case "51":
                correnteMinima = 60;
                correnteMaximaErro = 63;
                System.out.println(YELLOW + "  Parâmetros para canal 51 do MTX2: 60-63 A" + RESET);
                break;
            default:
                throw new Exception("Canal não suportado no MTX2: " + canal);
        }

        // Loop principal de ajuste
        while (iteracoes < maxIteracoes) {
            iteracoes++;
            System.out.println(YELLOW + "\n  --- Loop " + iteracoes + " | Offset: " + offsetAtual + " | Ajustes: " + ajustesFeitos + "/" + maxAjustes + " ---" + RESET);

            String correnteStr = verificarCorrente(driver, wait);
            double correnteDouble = Double.parseDouble(correnteStr);
            int corrente = (int) correnteDouble;
            System.out.println(YELLOW + "    Corrente atual MTX2: " + corrente + " A" + RESET);

            // Verificação especial se corrente for zero
            if (corrente == 0) {
                System.out.println(YELLOW + "    Corrente = 0 A. Verificando potência e corrente completa do MTX2..." + RESET);

                try {
                    String resultadoCompleto = verificarPotenciaEcorrente(driver, wait);
                    System.out.println(YELLOW + "    Resultado da verificação completa: " + resultadoCompleto + RESET);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("EQUIPAMENTO DESLIGADO")) {
                        System.out.println(YELLOW + "    " + e.getMessage() + RESET);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", e.getMessage());
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        resultado.put("equipamento_desligado", true);
                        return resultado;
                    } else {
                        throw e;
                    }
                }
            }

            // Se corrente atingiu o mínimo desejado
            if (corrente >= correnteMinima) {
                System.out.println(YELLOW + "    Corrente atingiu o mínimo (" + correnteMinima + " A)" + RESET);

                System.out.println(YELLOW + "    Aguardando 10 segundos para verificação final..." + RESET);
                Thread.sleep(10000);

                // Verificar novamente após espera
                correnteStr = verificarCorrente(driver, wait);
                correnteDouble = Double.parseDouble(correnteStr);
                corrente = (int) correnteDouble;
                System.out.println(YELLOW + "    Corrente após 20s: " + corrente + " A" + RESET);

                // Se corrente ultrapassou o máximo permitido
                if (corrente > correnteMaximaErro) {
                    ajustesFeitos++;

                    // Verificar limites de segurança
                    if (ajustesFeitos > maxAjustes) {
                        String erroFinal = "ERRO MTX2: Máximo de ajustes (" + maxAjustes + ") atingido. Corrente ainda alta: " + corrente + " A";
                        System.err.println("    " + erroFinal);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", erroFinal);
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        return resultado;
                    }

                    if (offsetAtual + 5 > offsetMaximo) {
                        String erroFinal = "ERRO MTX2: Offset máximo (" + offsetMaximo + ") atingido. Corrente ainda alta: " + corrente + " A";
                        System.err.println("    " + erroFinal);
                        resultado.put("status", "erro");
                        resultado.put("mensagem", erroFinal);
                        resultado.put("iteracoes", iteracoes);
                        resultado.put("ajustes_feitos", ajustesFeitos);
                        return resultado;
                    }

                    // Aumentar offset para reduzir corrente
                    String erroMsg = "Corrente MTX2 " + corrente + " A > limite máximo " + correnteMaximaErro + " A. Ajustando offset...";
                    System.err.println("    " + erroMsg + " (Ajuste #" + ajustesFeitos + ")");

                    offsetAtual += 5;

                    // Registrar ajuste
                    if (!resultado.containsKey("ajustes")) {
                        resultado.put("ajustes", new ArrayList<String>());
                    }
                    ((List<String>) resultado.get("ajustes")).add(erroMsg + " Offset ajustado para: " + offsetAtual);

                    System.out.println(YELLOW + "    Aplicando novo offset " + offsetAtual + " no MTX2..." + RESET);
                    configurarOffset(driver, wait, String.valueOf(offsetAtual));

                    System.out.println(YELLOW + "    Aguardando 10 segundos para estabilização..." + RESET);
                    Thread.sleep(10000);

                    continue;
                }

                // Sucesso: corrente dentro dos limites
                System.out.println(YELLOW + "    SUCESSO MTX2: Corrente " + corrente + " A dentro dos limites (" +
                        correnteMinima + "-" + correnteMaximaErro + " A)" + RESET);
                resultado.put("status", "sucesso");
                resultado.put("mensagem", "Corrente MTX2 ajustada corretamente");
                resultado.put("corrente_final", corrente);
                resultado.put("offset_final", offsetAtual);
                resultado.put("iteracoes", iteracoes);
                resultado.put("ajustes_feitos", ajustesFeitos);
                return resultado;
            }

            // Corrente abaixo do mínimo: diminuir offset
            System.out.println(YELLOW + "    Corrente abaixo do mínimo (" + correnteMinima + " A)" + RESET);

            offsetAtual--;
            System.out.println(YELLOW + "    Reduzindo offset MTX2 para: " + offsetAtual + RESET);

            // Verificar limite mínimo de offset
            if (offsetAtual < -50) {
                String erroMsg = "ERRO MTX2: Offset chegou a -50 e corrente ainda não atingiu " + correnteMinima + " A";
                System.err.println("    " + erroMsg);
                resultado.put("status", "erro");
                resultado.put("mensagem", erroMsg);
                resultado.put("iteracoes", iteracoes);
                resultado.put("ajustes_feitos", ajustesFeitos);
                return resultado;
            }

            System.out.println(YELLOW + "    Aplicando novo offset " + offsetAtual + " no MTX2..." + RESET);
            configurarOffset(driver, wait, String.valueOf(offsetAtual));

            System.out.println(YELLOW + "    Aguardando 10 segundos para estabilização..." + RESET);
            Thread.sleep(10000);
        }

        // Se atingiu máximo de iterações
        String erroMsg = "ERRO MTX2: Máximo de " + maxIteracoes + " iterações atingido";
        System.err.println("  " + erroMsg);
        resultado.put("status", "erro");
        resultado.put("mensagem", erroMsg);
        resultado.put("iteracoes", iteracoes);
        resultado.put("ajustes_feitos", ajustesFeitos);
        return resultado;
    }

    // Lê o offset atual do equipamento (MTX2)
    private int lerOffsetAtual(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            System.out.println(YELLOW + "  Lendo offset atual do equipamento MTX2..." + RESET);

            WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Internal2___']/input")));
            internal2.click();
            Thread.sleep(300);

            WebElement offsetElement = encontrarElementoComTentativas(wait,
                    "Internal2.power.offset",
                    "Internal2_power_offset");

            if (offsetElement == null) {
                offsetElement = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[contains(@id, 'offset')]")));
            }

            String textoOffset = offsetElement.getText().trim();
            System.out.println(YELLOW + "    Texto do offset atual MTX2: " + textoOffset + RESET);

            double offsetValor = extrairValorNumerico(textoOffset);
            System.out.println(YELLOW + "    Offset atual MTX2 extraído: " + (int)offsetValor + RESET);

            return (int) offsetValor;

        } catch (Exception e) {
            System.err.println("Erro ao ler offset atual do MTX2: " + e.getMessage());
            return 0; // Retorna 0 como fallback
        }
    }

    // Altera o canal do equipamento MTX2
    private String mudarCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
        try {
            // Navegar para Modulator2
            WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Modulator2___']/input")));
            modulator2.click();
            Thread.sleep(300);

            // Verificar canal atual
            String canalAntes = verificarCanal(driver, wait);
            System.out.println(YELLOW + "  Canal atual MTX2: " + canalAntes + RESET);

            // Se já estiver no canal correto, retornar
            if (canalAntes.equals(canal)) {
                System.out.println(YELLOW + "  MTX2 já está no canal " + canal + RESET);
                return canalAntes;
            }

            // Localizar elemento do canal
            WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("Modulator2_Config_UpConverter_ChannelNumber")));

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

            // Estratégia 2: Selecionar e deletar
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

            // Estratégia 3: JavaScript
            if (!sucesso) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = arguments[1];", canalElement, canal);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].dispatchEvent(new Event('change'));", canalElement);
                    sucesso = true;
                } catch (Exception e) {
                    System.err.println("  Estratégia 3 falhou: " + e.getMessage());
                }
            }

            if (!sucesso) {
                throw new Exception("Não foi possível mudar o canal do MTX2");
            }

            Thread.sleep(2000);

            // Verificar se o canal foi alterado
            String canalDepois = verificarCanal(driver, wait);
            System.out.println(YELLOW + "  Canal MTX2 configurado: " + canalDepois + RESET);

            int tentativas = 0;
            while (!canalDepois.equals(canal) && tentativas < 3) {
                System.out.println(YELLOW + "  Canal MTX2 não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
                Thread.sleep(1000);
                canalDepois = verificarCanal(driver, wait);
                tentativas++;
            }

            if (!canalDepois.equals(canal)) {
                System.err.println("  AVISO: Canal MTX2 não mudou corretamente. Esperado: " + canal + ", Lido: " + canalDepois);
            }

            return canalAntes;

        } catch (Exception e) {
            System.err.println("  Erro ao mudar canal do MTX2: " + e.getMessage());
            throw e;
        }
    }

    // Desliga o MTX2 configurando RfMasterOn para 2
    private void desligarMTX2(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar elemento RfMasterOn
        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.RfMasterOn",
                "PowerAmplifier2_Config_RfMasterOn");

        if (rfMasterOn == null) {
            rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'RfMasterOn')]")));
        }

        // Configurar para 2 (desligado)
        if (!configurarValor(driver, rfMasterOn, "2")) {
            throw new Exception("Falha ao desligar RfMasterOn do MTX2");
        }

        System.out.println(YELLOW + "  MTX2 desligado (RfMasterOn = 2)" + RESET);
        Thread.sleep(300);
    }

    // Liga o MTX2 configurando RfMasterOn para 1
    private void ligarMTX2(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar elemento RfMasterOn
        WebElement rfMasterOn = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.RfMasterOn",
                "PowerAmplifier2_Config_RfMasterOn");

        if (rfMasterOn == null) {
            rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'RfMasterOn')]")));
        }

        // Configurar para 1 (ligado)
        if (!configurarValor(driver, rfMasterOn, "1")) {
            throw new Exception("Falha ao ligar RfMasterOn do MTX2");
        }

        System.out.println(YELLOW + "  MTX2 ligado (RfMasterOn = 1)" + RESET);
        Thread.sleep(300);
    }

    // Realiza login na interface web
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

    // Configura threshold de proteção do MTX2
    private void configurarThershold(WebDriver driver, WebDriverWait wait, String valorThershold) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        internal2.click();
        Thread.sleep(300);

        // Encontrar elemento ForwardHigh (threshold)
        WebElement offset = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.Threshold.ForwardHigh",
                "PowerAmplifier2_Config_Threshold_ForwardHigh");

        if (offset == null) {
            offset = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'ForwardHigh ')]")));
        }

        if (!configurarValor(driver, offset, valorThershold)) {
            throw new Exception("Falha ao configurar thershold do MTX2 para " + valorThershold);
        }

        System.out.println(YELLOW + "  Thershold MTX2 configurado: " + valorThershold + RESET);
        Thread.sleep(300);
    }

    // Configura offset de potência do MTX2
    private void configurarOffset(WebDriver driver, WebDriverWait wait, String valorOffset) throws Exception {
        // Navegar para Internal2
        WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Internal2___']/input")));
        internal2.click();
        Thread.sleep(300);

        // Encontrar elemento de offset
        WebElement offset = encontrarElementoComTentativas(wait,
                "Internal2.power.offset",
                "Internal2_power_offset");

        if (offset == null) {
            offset = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@id, 'offset')]")));
        }

        if (!configurarValor(driver, offset, valorOffset)) {
            throw new Exception("Falha ao configurar offset do MTX2 para " + valorOffset);
        }

        System.out.println(YELLOW + "  Offset MTX2 configurado: " + valorOffset + RESET);
        Thread.sleep(300);
    }

    // Configura potência de saída do MTX2
    private void configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Encontrar elemento OutputPower
        WebElement outputPower = encontrarElementoComTentativas(wait,
                "PowerAmplifier2.Config.OutputPower",
                "PowerAmplifier2_Config_OutputPower");

        if (outputPower == null) {
            outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(text(), 'OutputPower')]")));
        }

        if (!configurarValor(driver, outputPower, potencia)) {
            throw new Exception("Falha ao configurar potência do MTX2");
        }

        System.out.println(YELLOW + "  Potência MTX2 configurada: " + potencia + RESET);
        Thread.sleep(300);
    }

    // Método genérico para configurar valores em campos de entrada do MTX2
    private boolean configurarValor(WebDriver driver, WebElement elemento, String novoValor) {
        try {
            // Tentar double click
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                        .doubleClick(elemento)
                        .perform();
                Thread.sleep(300);
            } catch (Exception e) {
                elemento.click();
                Thread.sleep(300);
            }

            Thread.sleep(500);

            // Obter elemento ativo para edição
            WebElement activeElement = driver.switchTo().activeElement();

            if (activeElement.getTagName().equals("input") ||
                    activeElement.getTagName().equals("textarea") ||
                    activeElement.getAttribute("type").equals("text")) {

                // Limpar campo
                activeElement.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                activeElement.sendKeys(Keys.DELETE);
                Thread.sleep(300);

                // Inserir novo valor
                activeElement.sendKeys(novoValor);
                Thread.sleep(300);

                // Confirmar com Enter
                activeElement.sendKeys(Keys.ENTER);
                Thread.sleep(500);

                return true;
            }

            // Fallback com JavaScript
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].value = arguments[1];", elemento, novoValor);
                Thread.sleep(300);
                js.executeScript("arguments[0].dispatchEvent(new Event('change'));", elemento);
                Thread.sleep(300);
                return true;
            } catch (Exception e) {
                System.err.println("JavaScript também falhou: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Erro ao configurar valor no MTX2: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Lê potência atual do equipamento MTX2
    private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para PowerAmplifier2
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        // Localizar elemento de potência
        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier2_Status_ForwardPower")));

        String textoCompleto = potenciaElement.getText().trim();
        double potenciaValor = extrairValorNumerico(textoCompleto);

        return String.valueOf(potenciaValor);
    }

    // Lê corrente atual do equipamento MTX2
    private String verificarCorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Navegar para Modulator2
        WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator2___']/input")));
        modulator2.click();
        Thread.sleep(300);

        // Localizar elemento de corrente
        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator2_mtrMainCurr")));

        String textoCompleto = correnteElement.getText().trim();

        System.out.println(YELLOW + "    Texto completo da corrente MTX2: " + textoCompleto + RESET);

        double correnteValor = extrairValorNumerico(textoCompleto);

        System.out.println(YELLOW + "    Corrente MTX2 extraída: " + correnteValor + " A" + RESET);

        return String.valueOf(correnteValor);
    }

    // Lê canal atual do equipamento MTX2
    private String verificarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
        try {
            // Navegar para Modulator2
            WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Modulator2___']/input")));
            modulator2.click();
            Thread.sleep(300);

            // Localizar elemento do canal
            WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("Modulator2_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);

            // Tentar diferentes estratégias para obter o valor
            String canalTexto = canalElement.getText().trim();
            System.out.println(YELLOW + "    Texto do elemento canal MTX2: " + canalTexto + RESET);

            if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator2.Config.UpConverter.ChannelNumber")) {
                canalTexto = canalElement.getAttribute("value");
                System.out.println(YELLOW + "    Value attribute do canal MTX2: " + canalTexto + RESET);
            }

            if (canalTexto == null || canalTexto.isEmpty()) {
                canalTexto = canalElement.getAttribute("innerText");
                System.out.println(YELLOW + "    InnerText do canal MTX2: " + canalTexto + RESET);
            }

            if (canalTexto != null && !canalTexto.isEmpty()) {
                // Processar texto (pode conter "=")
                if (canalTexto.contains("=")) {
                    String[] partes = canalTexto.split("=");
                    if (partes.length > 1) {
                        canalTexto = partes[1].trim();
                    }
                }

                // Extrair apenas números
                String numeros = canalTexto.replaceAll("[^0-9]", "").trim();

                if (!numeros.isEmpty()) {
                    System.out.println(YELLOW + "    Canal MTX2 extraído: " + numeros + RESET);
                    return numeros;
                }
            }

            System.out.println(YELLOW + "    Não conseguiu extrair canal MTX2, retornando N/A" + RESET);
            return "N/A";

        } catch (Exception e) {
            System.err.println("    Erro ao verificar canal MTX2: " + e.getMessage());
            return "N/A";
        }
    }

    // Verifica ambos os valores para detectar equipamento desligado
    private String verificarPotenciaEcorrente(WebDriver driver, WebDriverWait wait) throws Exception {
        // Verificar potência
        WebElement powerAmplifier2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmplifier2.click();
        Thread.sleep(300);

        WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("PowerAmplifier2_Status_ForwardPower")));

        String textoPotencia = potenciaElement.getText().trim();
        double potenciaValor = extrairValorNumerico(textoPotencia);

        System.out.println(YELLOW + "    Potência MTX2 extraída: " + potenciaValor + " W" + RESET);

        // Verificar corrente
        WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[@link='Modulator2___']/input")));
        modulator2.click();
        Thread.sleep(300);

        WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Modulator2_mtrMainCurr")));

        String textoCorrente = correnteElement.getText().trim();
        System.out.println(YELLOW + "    Texto completo da corrente MTX2: " + textoCorrente + RESET);

        double correnteValor = extrairValorNumerico(textoCorrente);
        System.out.println(YELLOW + "    Corrente MTX2 extraída: " + correnteValor + " A" + RESET);

        // Detectar equipamento desligado
        if (correnteValor == 0 && potenciaValor == 0) {
            throw new Exception("EQUIPAMENTO MTX2 DESLIGADO - Corrente: " + correnteValor +
                    " A, Potência: " + potenciaValor + " W");
        }

        if (correnteValor == 0) {
            System.out.println(YELLOW + "    Corrente MTX2 zero, retornando valor da potência" + RESET);
            return "Potência MTX2: " + potenciaValor + " W";
        }

        return "Potência MTX2: " + potenciaValor + " W, Corrente MTX2: " + correnteValor + " A";
    }

    // Tenta encontrar elemento usando múltiplos IDs possíveis
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
        String filePath = System.getProperty("user.dir") + "/logs_rotina_completa_mtx2.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
            writer.write(LocalDateTime.now() +
                    " | MTX2 | Canal: " + canal +
                    " | Antes: " + canalAntes +
                    " | Depois: " + canalDepois +
                    " | Potência: " + potencia +
                    " | Corrente: " + corrente +
                    " | Offset: " + offset +
                    " | Iterações: " + resultadoAjuste.get("iteracoes") +
                    " | Status: " + resultadoAjuste.get("status") + "\n");
            System.out.println(YELLOW + "  Log MTX2 salvo em: " + filePath + RESET);
        } catch (Exception e) {
            System.err.println("Erro ao salvar log MTX2: " + e.getMessage());
        }
    }

    // debugar cada canal em especifico
    private void debugElemento(WebElement elemento, String nome) {
        try {
            System.out.println(YELLOW + "\n=== DEBUG MTX2 " + nome + " ===" + RESET);
            System.out.println(YELLOW + "Tag: " + elemento.getTagName() + RESET);
            System.out.println(YELLOW + "Text: " + elemento.getText() + RESET);
            System.out.println(YELLOW + "Value attr: " + elemento.getAttribute("value") + RESET);
            System.out.println(YELLOW + "InnerText: " + elemento.getAttribute("innerText") + RESET);
            System.out.println(YELLOW + "OuterHTML: " + elemento.getAttribute("outerHTML") + RESET);
            System.out.println(YELLOW + "Displayed: " + elemento.isDisplayed() + RESET);
            System.out.println(YELLOW + "Enabled: " + elemento.isEnabled() + RESET);
            System.out.println(YELLOW + "==================\n" + RESET);
        } catch (Exception e) {
            System.err.println("Erro no debug MTX2: " + e.getMessage());
        }
    }

    // Endpoint para configurar offset manualmente no MTX2
    @PostMapping("/configurar-offset-mtx2")
    public ResponseEntity<Map<String, Object>> configurarOffsetMtx2(@RequestParam String offset) {
        Map<String, Object> resposta = new HashMap<>();
        WebDriver driver = null;

        System.out.println(YELLOW + "=== CONFIGURANDO OFFSET PARA MTX2 ===" + RESET);
        System.out.println(YELLOW + "Valor solicitado: " + offset + RESET);

        try {
            // Validar offset
            int offsetInt;
            try {
                offsetInt = Integer.parseInt(offset);
                if (offsetInt < -32768 || offsetInt > 32767) {
                    throw new NumberFormatException("Fora do intervalo permitido");
                }
            } catch (NumberFormatException e) {
                resposta.put("status", "erro");
                resposta.put("mensagem", "Valor de offset inválido! Use números entre -32768 e 32767");
                return ResponseEntity.badRequest().body(resposta);
            }

            // Configurar driver
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-gpu");
            options.addArguments("--headless");
            options.addArguments("--incognito");
            options.addArguments("--disable-cache");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Login
            System.out.println(YELLOW + "Acessando página de debug..." + RESET);
            driver.get("http://10.10.103.103/debug/");

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
            System.out.println(YELLOW + "Login realizado" + RESET);
            Thread.sleep(1500);

            // Navegar para offset
            System.out.println(YELLOW + "Navegando para Internal2..." + RESET);
            WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//label[@link='Internal2___']/input")));
            internal2.click();
            Thread.sleep(500);

            System.out.println(YELLOW + "Procurando campo de offset..." + RESET);

            WebElement offsetElement = null;
            String[] offsetSelectors = {
                    "//*[contains(@id, 'Internal2.power.offset')]",
                    "//*[contains(@id, 'Internal2_power_offset')]",
                    "//*[contains(@name, 'PowerOffset')]",
                    "//button[contains(text(), 'Power Offset')]",
                    "//input[contains(@id, 'offset')]",
                    "//div[contains(text(), 'offset')]/following-sibling::input"
            };

            // Tentar diferentes seletores
            for (String selector : offsetSelectors) {
                try {
                    offsetElement = driver.findElement(By.xpath(selector));
                    if (offsetElement != null && offsetElement.isDisplayed()) {
                        System.out.println(YELLOW + "Offset encontrado com selector: " + selector + RESET);
                        break;
                    }
                } catch (Exception e) {
                    // Continua tentando
                }
            }

            if (offsetElement == null) {
                try {
                    offsetElement = driver.findElement(By.xpath("//*[contains(text(), 'offset') or contains(@id, 'offset')]"));
                } catch (Exception e) {
                    throw new Exception("Não foi possível encontrar o campo de offset do MTX2");
                }
            }

            System.out.println(YELLOW + "Elemento encontrado - Tag: " + offsetElement.getTagName() + RESET);
            System.out.println(YELLOW + "ID: " + offsetElement.getAttribute("id") + RESET);
            System.out.println(YELLOW + "Nome: " + offsetElement.getAttribute("name") + RESET);
            System.out.println(YELLOW + "Texto: " + offsetElement.getText() + RESET);

            System.out.println(YELLOW + "Configurando offset para " + offset + "..." + RESET);

            boolean sucesso = false;

            // Estratégia 1: Double click
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                        .doubleClick(offsetElement)
                        .perform();
                Thread.sleep(500);

                WebElement activeElement = driver.switchTo().activeElement();
                if (activeElement.getTagName().equalsIgnoreCase("input") ||
                        activeElement.getTagName().equalsIgnoreCase("textarea")) {

                    System.out.println(YELLOW + "Campo ativo encontrado para edição" + RESET);

                    activeElement.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    activeElement.sendKeys(Keys.DELETE);
                    Thread.sleep(300);

                    activeElement.sendKeys(offset);
                    Thread.sleep(500);

                    activeElement.sendKeys(Keys.ENTER);
                    Thread.sleep(1000);

                    sucesso = true;
                }
            } catch (Exception e) {
                System.err.println("Estratégia 1 falhou: " + e.getMessage());
            }

            // Estratégia 2: Clicar direto
            if (!sucesso) {
                try {
                    offsetElement.click();
                    Thread.sleep(300);
                    offsetElement.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    offsetElement.sendKeys(Keys.DELETE);
                    offsetElement.sendKeys(offset);
                    Thread.sleep(300);
                    offsetElement.sendKeys(Keys.ENTER);
                    Thread.sleep(1000);
                    sucesso = true;
                } catch (Exception e) {
                    System.err.println("Estratégia 2 falhou: " + e.getMessage());
                }
            }

            // Estratégia 3: JavaScript
            if (!sucesso) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = arguments[1];", offsetElement, offset);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].dispatchEvent(new Event('change'));", offsetElement);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].dispatchEvent(new Event('blur'));", offsetElement);
                    Thread.sleep(1000);
                    sucesso = true;
                } catch (Exception e) {
                    System.err.println("Estratégia 3 falhou: " + e.getMessage());
                }
            }

            if (!sucesso) {
                throw new Exception("Não foi possível configurar o offset do MTX2");
            }

            // Verificar se foi aplicado
            System.out.println(YELLOW + "Verificando offset aplicado..." + RESET);
            Thread.sleep(2000);

            internal2.click();
            Thread.sleep(1000);

            String offsetAtual = "";
            try {
                offsetAtual = offsetElement.getText();
                if (offsetAtual.isEmpty()) {
                    offsetAtual = offsetElement.getAttribute("value");
                }
                if (offsetAtual.isEmpty()) {
                    offsetAtual = offsetElement.getAttribute("innerText");
                }

                if (offsetAtual.contains("=")) {
                    String[] partes = offsetAtual.split("=");
                    if (partes.length > 1) {
                        offsetAtual = partes[1].trim();
                    }
                }

                offsetAtual = offsetAtual.replaceAll("[^0-9-]", "").trim();

                System.out.println(YELLOW + "Offset lido após configuração: " + offsetAtual + RESET);

            } catch (Exception e) {
                System.err.println("Não foi possível ler offset atual do MTX2: " + e.getMessage());
            }

            // Preparar resposta
            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Offset MTX2 configurado com sucesso");
            resposta.put("offset_solicitado", offset);
            resposta.put("offset_aplicado", offsetAtual.isEmpty() ? offset : offsetAtual);
            resposta.put("hora_aplicacao", LocalDateTime.now().toString());

            System.out.println(YELLOW + "=== OFFSET MTX2 CONFIGURADO COM SUCESSO ===" + RESET);

            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            System.err.println("Erro ao configurar offset do MTX2: " + e.getMessage());
            e.printStackTrace();

            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro no MTX2: " + e.getMessage());
            resposta.put("hora_erro", LocalDateTime.now().toString());

            return ResponseEntity.status(500).body(resposta);

        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    System.out.println(YELLOW + "Driver finalizado" + RESET);
                } catch (Exception e) {
                    System.err.println("Erro ao finalizar driver: " + e.getMessage());
                }
            }
        }
    }

    // Endpoint para executar ajuste em um canal específico do MTX2
    @PostMapping("/executar-offset-canal-mtx2")
    public ResponseEntity<Map<String, Object>> executaroffsetcanalmtx2(@RequestParam String canal) {
        Map<String, Object> resposta = new HashMap<>();
        WebDriver driver = null;

        try {
            // Configurar driver
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-gpu");
            options.addArguments("--headless");
            options.addArguments("--incognito");
            options.addArguments("--disable-cache");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Login e processamento
            driver.get("http://10.10.103.103/debug/");
            fazerLogin(driver, wait);

            Map<String, Object> resultado = processarCanalCompleto(driver, wait, canal);
            resposta.putAll(resultado);

            if ("sucesso".equals(resultado.get("status"))) {
                return ResponseEntity.ok(resposta);
            } else {
                return ResponseEntity.status(500).body(resposta);
            }

        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro no MTX2: " + e.getMessage());
            return ResponseEntity.status(500).body(resposta);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    // Endpoint para cancelar operação (apenas simbólico) do MTX2
    @PostMapping("/cancelar-offset-mtx2")
    public ResponseEntity<Map<String, Object>> cancelarOffsetMtx2() {
        Map<String, Object> resposta = new HashMap<>();

        System.out.println(YELLOW + "Solicitação de cancelamento de offset MTX2 recebida" + RESET);

        resposta.put("status", "sucesso");
        resposta.put("mensagem", "Solicitação de cancelamento MTX2 recebida");
        resposta.put("hora_cancelamento", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para ajuste inicial do offset (redireciona para configurarOffsetMtx2)
    @PostMapping("/ajustar-offset-inicial-mtx2")
    public ResponseEntity<Map<String, Object>> ajustarOffsetInicialMtx2(@RequestParam String offset) {
        return configurarOffsetMtx2(offset);
    }
}