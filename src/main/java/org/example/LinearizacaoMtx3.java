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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class LinearizacaoMtx3 {

	private static final String RESET = "\u001B[0m";
	private static final String BLUE = "\u001B[34m";
	private static boolean linearizacaoAtivaMtx3 = false;
	private static LocalDateTime horaInicioLinearizacaoMtx3 = null;
	private static Map<String, Object> statusLinearizacaoMtx3 = new ConcurrentHashMap<>();

	@Value("${app.username:admin}")
	private String username;

	@Value("${app.password:admin}")
	private String password;

	@PostMapping("/executar-rotina-linearizacao-mtx3")
	public ResponseEntity<Map<String, Object>> executarRotinaLinearizacaomTX3() {
		Map<String, Object> respostaGeral = new HashMap<>();
		WebDriver driver = null;

		try {
			System.out.println(BLUE + "=== INICIANDO ROTINA DE LINEARIZAÇÃO MTX3 ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			WebDriverManager.chromedriver().setup();
			ChromeOptions options = new ChromeOptions();
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("--disable-extensions");
			options.addArguments("--disable-gpu");
			options.addArguments("--headless"); // Descomente para modo headless
			options.addArguments("--incognito");
			options.addArguments("--disable-cache");

			driver = new ChromeDriver(options);
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

			driver.get("http://10.10.103.103/debug/");
			System.out.println(BLUE + "Página acessada" + RESET);
			fazerLogin(driver, wait);
			System.out.println(BLUE + "Login realizado" + RESET);

			// Processo completo para MTX3
			Map<String, Object> resultado = processoCompleto(driver, wait);

			// Resposta final
			respostaGeral.put("status", "sucesso");
			respostaGeral.put("mensagem", "Rotina de linearização MTX3 executada com sucesso");
			respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
			respostaGeral.put("hora_fim", LocalDateTime.now().toString());
			respostaGeral.put("resultados", resultado);

			System.out.println(BLUE + "\n=== ROTINA DE LINEARIZAÇÃO MTX3 FINALIZADA ===" + RESET);
			System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);

			return ResponseEntity.ok(respostaGeral);

		} catch (Exception e) {
			respostaGeral.put("status", "erro");
			respostaGeral.put("mensagem", "Erro na rotina de linearização MTX3: " + e.getMessage());
			respostaGeral.put("hora_inicio", LocalDateTime.now().toString());

			System.err.println("Erro na rotina de linearização MTX3: " + e.getMessage());
			e.printStackTrace();

			return ResponseEntity.status(500).body(respostaGeral);
		} finally {
			if (driver != null) {
				driver.quit();
				System.out.println(BLUE + "Driver finalizado" + RESET);
			}
		}
	}

	@GetMapping("/status-linearizacao-mtx3")
	public ResponseEntity<Map<String, Object>> getStatusLinearizacaoMtx3() {
		Map<String, Object> resposta = new HashMap<>();

		resposta.put("ativo", linearizacaoAtivaMtx3);
		resposta.put("hora_inicio", horaInicioLinearizacaoMtx3 != null ? horaInicioLinearizacaoMtx3.toString() : null);
		resposta.put("detalhes", statusLinearizacaoMtx3);
		resposta.put("ultima_atualizacao", LocalDateTime.now().toString());

		// Verificar timeout (30 minutos para linearização)
		if (linearizacaoAtivaMtx3 && horaInicioLinearizacaoMtx3 != null) {
			Duration duracao = Duration.between(horaInicioLinearizacaoMtx3, LocalDateTime.now());
			if (duracao.toMinutes() > 30) {
				linearizacaoAtivaMtx3 = false;
				resposta.put("ativo", false);
				resposta.put("mensagem", "Processo expirado");
			}
		}

		return ResponseEntity.ok(resposta);
	}

	// função para linearização
	private Map<String, Object> processoCompleto(WebDriver driver, WebDriverWait wait) {
		Map<String, Object> resultado = new HashMap<>();

		// MARCA COMO ATIVO
		linearizacaoAtivaMtx3 = true;
		horaInicioLinearizacaoMtx3 = LocalDateTime.now();
		statusLinearizacaoMtx3.clear();
		statusLinearizacaoMtx3.put("status", "executando");
		statusLinearizacaoMtx3.put("inicio", horaInicioLinearizacaoMtx3.toString());
		statusLinearizacaoMtx3.put("etapa", "iniciando");

		try {

			try {
				// ========== ETAPA 1: VERIFICAR CANAL ATUAL ==========
				System.out.println(BLUE + "\nVerificando canal atual" + RESET);
				String canalAtual = verificarCanal(driver, wait);
				System.out.println(BLUE + "  Canal atual: " + canalAtual + RESET);

				// Guardar o canal atual para usar no cancelamento
				resultado.put("canal_atual", canalAtual);

				// ========== ETAPA 2: INFORMAÇÕES INICIAIS ==========
				System.out.println(BLUE + "\nColetando informações iniciais" + RESET);
				String potenciaInicial = verificarPotencia(driver, wait);
				String temperaturaInicial = verificarTemperatura(driver, wait);
				System.out.println(BLUE + "  Potência inicial: " + potenciaInicial + "W" + RESET);
				System.out.println(BLUE + "  Temperatura inicial: " + temperaturaInicial + "°C" + RESET);

				// ========== ETAPA 3: LOOP DE POTÊNCIAS ==========
				//int[] potencias = {483, 430, 370, 340, 300}; // maior pro menor
				int[] potencias = {300, 340, 370, 430, 483}; // menor pro maior
				int ultimaPotenciaProcessada = 0;
				boolean todasPotenciasConcluidas = true;
				int tentativasEstabilizacao = 10; // Número máximo de tentativas
				int tempoEspera = 180000; // 180 segundos
				int margemTolerancia = 1; // ±1°C

				for (int i = 0; i < potencias.length; i++) {
					int potenciaAtual = potencias[i];
					ultimaPotenciaProcessada = potenciaAtual;

					System.out.println(BLUE + "\n[ETAPA 1] Desligando MTX3 para iniciar sequência" + RESET);
					desligarMTX3(driver, wait);

					System.out.println(BLUE + "\n[ETAPA 2] Processando potência: " + potenciaAtual + "W" + RESET);
					System.out.println(BLUE + "  Progresso: " + (i + 1) + "/" + potencias.length + RESET);

					// 2.1. Configurar nova potência
					System.out.println(BLUE + "  2.1. Configurando potência para " + potenciaAtual + "W" + RESET);
					configurarPotencia(driver, wait, String.valueOf(potenciaAtual));

					// 2.2. Ligar o MTX3
					System.out.println(BLUE + "  2.2. Ligando MTX3" + RESET);
					ligarMTX3(driver, wait);

					// 2.3. AGUARDAR ESTABILIZAÇÃO DA TEMPERATURA
					boolean temperaturaEstabilizada = false;
					String temperaturaAnterior = "";
					String temperaturaAtual = "";

					for (int tentativa = 1; tentativa <= tentativasEstabilizacao; tentativa++) {
						System.out.println(BLUE + "\n  Tentativa " + tentativa + "/" + tentativasEstabilizacao + " de estabilização" + RESET);

						// Primeira medição
						System.out.println(BLUE + "    Primeira medição de temperatura..." + RESET);
						temperaturaAnterior = verificarTemperatura(driver, wait);
						System.out.println(BLUE + "    Temperatura inicial: " + temperaturaAnterior + "°C" + RESET);

						// Aguardar
						System.out.println(BLUE + "    Aguardando " + (tempoEspera/1000) + " segundos..." + RESET);
						Thread.sleep(tempoEspera);

						// Segunda medição
						System.out.println(BLUE + "    Segunda medição de temperatura..." + RESET);
						temperaturaAtual = verificarTemperatura(driver, wait);
						System.out.println(BLUE + "    Temperatura final: " + temperaturaAtual + "°C" + RESET);

						try {
							// Converter para números APÓS ter os valores
							int tempAnteriorNum = Integer.parseInt(temperaturaAnterior);
							int tempAtualNum = Integer.parseInt(temperaturaAtual);

							// Calcular diferença
							int diferenca = Math.abs(tempAnteriorNum - tempAtualNum);

							// Verificar estabilização
							if (temperaturaAnterior.equals(temperaturaAtual) || diferenca <= margemTolerancia) {
								temperaturaEstabilizada = true;
								System.out.println(BLUE + "    Temperatura estabilizada em " + temperaturaAtual + "°C\n" + RESET);
								if (diferenca > 0) {
									System.out.println(BLUE + "    Diferença: " + diferenca + "°C (≤ " + margemTolerancia + "°C)\n" + RESET);
								}
								break;
							} else {
								System.out.println(BLUE + "    Temperatura não estabilizada: " + temperaturaAnterior + "°C → " + temperaturaAtual + "°C" + RESET);
								System.out.println(BLUE + "    Diferença: " + diferenca + "°C (> " + margemTolerancia + "°C)" + RESET);
							}
						} catch (NumberFormatException e) {
							// Se não conseguir converter, usar comparação de strings
							System.err.println("    Erro ao converter temperatura para número: " + e.getMessage());
							if (temperaturaAnterior.equals(temperaturaAtual)) {
								temperaturaEstabilizada = true;
								System.out.println(BLUE + "    Temperatura estabilizada em " + temperaturaAtual + "°C" + RESET);
								break;
							} else {
								System.out.println(BLUE + "    Temperatura não estabilizada: " + temperaturaAnterior + "°C ≠ " + temperaturaAtual + "°C" + RESET);
							}
						}

                    /* Se não for a última tentativa, aguardar mais
                    if (tentativa < tentativasEstabilizacao && !temperaturaEstabilizada) {
                        System.out.println(BLUE + "    Aguardando mais " + (tempoEspera/1000) + " segundos..." + RESET);
                        Thread.sleep(tempoEspera);
                    }

                     */
					}

					// 2.4. VERIFICAR SE TEMPERATURA ESTABILIZOU
					if (temperaturaEstabilizada) {
						System.out.println(BLUE + "  Potência " + potenciaAtual + "W processada com sucesso" + RESET);

						if (i < potencias.length - 1) {
							//System.out.println(BLUE + "  2.5. Desligando MTX3 para próxima potência" + RESET);
							//desligarMTX3(driver, wait);
							Thread.sleep(3000); // Aguardar 3 segundos entre potências
						} else {
							System.out.println(BLUE + "  Última potência concluída com sucesso!" + RESET);
						}
					} else {
						System.out.println(BLUE + "  Temperatura não estabilizou após " + tentativasEstabilizacao + " tentativas" + RESET);
						System.out.println(BLUE + "  Interrompendo sequência na potência " + potenciaAtual + "W" + RESET);
						todasPotenciasConcluidas = false;

						// Desligar MTX3 antes de sair
						//desligarMTX3(driver, wait);
						break;
					}
				}

				// ========== ETAPA 3: RESULTADOS FINAIS ==========
				System.out.println(BLUE + "\n[ETAPA 3] Coletando resultados finais" + RESET);

				String potenciaFinal = verificarPotencia(driver, wait);
				String temperaturaFinal = verificarTemperatura(driver, wait);

				// ========== ETAPA 4: SALVAR LOG ==========
				System.out.println(BLUE + "\n[ETAPA 4] Salvando log" + RESET);
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
				System.err.println("Erro no processamento do MTX3: " + e.getMessage());
				e.printStackTrace();

				resultado.put("status", "erro");
				resultado.put("mensagem", "Erro no MTX3: " + e.getMessage());
			}
			return resultado;

		} catch (Exception e) {
			linearizacaoAtivaMtx3 = false;
			statusLinearizacaoMtx3.put("status", "erro");
			throw e;
		} finally {
			linearizacaoAtivaMtx3 = false;
			statusLinearizacaoMtx3.put("status", "finalizado");
			statusLinearizacaoMtx3.put("fim", LocalDateTime.now().toString());
		}
	}

	// Método para chamar o cancelamento após processar um canal
	private void chamarFuncaoCancelamento(String canal, Map<String, Object> resultadoCanal) {
		try {
			System.out.println(BLUE + "\n=== CHAMANDO CANCELAMENTO PARA CANAL " + canal + " ===" + RESET);

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
			System.out.println(BLUE + "Cancelamento executado: " + respostaCancelamento + RESET);

			// Salvar log específico do cancelamento
			salvarLogCancelamento(canal, respostaCancelamento);

		} catch (Exception e) {
			System.err.println("Erro ao chamar cancelamento para canal " + canal + ": " + e.getMessage());
		}
	}

	// Método para salvar log do cancelamento
	private void salvarLogCancelamento(String canal, Map<String, Object> respostaCancelamento) {
		String filePath = System.getProperty("user.dir") + "/logs_cancelamento_mtx3.txt";
		try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
			writer.write(LocalDateTime.now() +
					" | Canal: " + canal +
					" | Status: " + respostaCancelamento.get("status") +
					" | Hora: " + respostaCancelamento.get("hora_cancelamento") +
					" | Mensagem: " + respostaCancelamento.get("mensagem") + "\n");
			System.out.println(BLUE + "  Log de cancelamento salvo em: " + filePath + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao salvar log de cancelamento: " + e.getMessage());
		}
	}

	// verificar o valor da potencia
	private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		// CORREÇÃO: Mudar de OutputPower para ForwardPowerdBm
		WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("PowerAmplifier3_Status_ForwardPowerdBm")));

		String potenciaText = potenciaElement.getText().trim();
		double potencia = extrairValorNumerico(potenciaText);

		return String.valueOf((int) potencia);
	}

	// verificar os valores das temperaturas
	private String verificarTemperatura(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		WebElement tempElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("PowerAmplifier3_Status_Temperature")));

		String tempText = tempElement.getText().trim();
		double temperatura = extrairValorNumerico(tempText);

		return String.valueOf((int) temperatura);
	}

	// salvar as informações
	private void salvarLogLinearizacao(String potenciaInicial, String potenciaFinal, String tempInicial, String tempFinal, int ultimaPotencia, boolean concluido) {
		String filePath = System.getProperty("user.dir") + "/log_linearizacao_mtx3.txt";
		try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
			writer.write(LocalDateTime.now() +
					" | P_Inicial: " + potenciaInicial + "W" +
					" | P_Final: " + potenciaFinal + "W" +
					" | T_Inicial: " + tempInicial + "°C" +
					" | T_Final: " + tempFinal + "°C" +
					" | Ultima_Potencia: " + ultimaPotencia + "W" +
					" | Status: " + (concluido ? "COMPLETO" : "INTERROMPIDO") + "\n");
			System.out.println(BLUE + "  Log salvo em: " + filePath + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao salvar log: " + e.getMessage());
		}
	}

	// desligar mtx3
	private void desligarMTX3(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		WebElement rfMasterOn = encontrarElementoComTentativas(wait,
				"PowerAmplifier3.Config.RfMasterOn",
				"PowerAmplifier3_Config_RfMasterOn");

		if (rfMasterOn == null) {
			rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(@id, 'RfMasterOn')]")));
		}

		if (!configurarValor(driver, rfMasterOn, "2")) {
			throw new Exception("Falha ao desligar RfMasterOn");
		}

		System.out.println(BLUE + "  MTX3 desligado (RfMasterOn = 2)" + RESET);
		Thread.sleep(300);
	}

	// ligar mtx3
	private void ligarMTX3(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		WebElement rfMasterOn = encontrarElementoComTentativas(wait,
				"PowerAmplifier3.Config.RfMasterOn",
				"PowerAmplifier3_Config_RfMasterOn");

		if (rfMasterOn == null) {
			rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(@id, 'RfMasterOn')]")));
		}

		if (!configurarValor(driver, rfMasterOn, "1")) {
			throw new Exception("Falha ao ligar RfMasterOn");
		}

		System.out.println(BLUE + "  MTX3 ligado (RfMasterOn = 1)" + RESET);
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
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		// Encontrar OutputPower (este é o elemento de CONFIGURAÇÃO, não de STATUS)
		WebElement outputPower = encontrarElementoComTentativas(wait,
				"PowerAmplifier3.Config.OutputPower",
				"PowerAmplifier3_Config_OutputPower");

		if (outputPower == null) {
			outputPower = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(text(), 'OutputPower')]")));
		}

		if (!configurarValor(driver, outputPower, potencia)) {
			throw new Exception("Falha ao configurar potência");
		}

		System.out.println(BLUE + "  Potência configurada: " + potencia + "W" + RESET);
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

			// Se tiver formato "Modulator3.mtrMainCurr = 39"
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
			System.out.println(BLUE + "\n[ETAPA 0] Mudando para canal: " + canal + RESET);

			// Primeiro verifica o canal atual
			String canalAtual = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual: " + canalAtual + RESET);

			// Se não for o canal desejado, muda
			if (!canalAtual.equals(canal) && !canalAtual.contains(canal)) {
				System.out.println(BLUE + "  Mudando para canal " + canal + "..." + RESET);
				// Chama a função para mudar canal (você precisa implementar ou usar a existente)
				mudarCanal(driver, wait, canal);
				Thread.sleep(2000);
				canalAtual = verificarCanal(driver, wait);
				System.out.println(BLUE + "  Novo canal: " + canalAtual + RESET);
			}

			// ========== ETAPA 1: INFORMAÇÕES INICIAIS ==========
			System.out.println(BLUE + "\n[ETAPA 1] Coletando informações iniciais" + RESET);
			String potenciaInicial = verificarPotencia(driver, wait);
			String temperaturaInicial = verificarTemperatura(driver, wait);
			System.out.println(BLUE + "  Potência inicial: " + potenciaInicial + "W" + RESET);
			System.out.println(BLUE + "  Temperatura inicial: " + temperaturaInicial + "°C" + RESET);

			// ========== ETAPA 2: DESLIGAR MTX3 PARA COMEÇAR ==========
			System.out.println(BLUE + "\n[ETAPA 2] Desligando MTX3 para iniciar sequência" + RESET);
			desligarMTX3(driver, wait);

			// ========== ETAPA 3: LOOP DE POTÊNCIAS ==========
			int[] potencias = {483, 430, 370, 340, 300};
			int ultimaPotenciaProcessada = 0;
			boolean todasPotenciasConcluidas = true;
			List<String> historicoTemperaturas = new  ArrayList<>();

			for (int i = 0; i < potencias.length; i++) {
				int potenciaAtual = potencias[i];
				ultimaPotenciaProcessada = potenciaAtual;

				System.out.println(BLUE + "\n[ETAPA 3] Processando potência: " + potenciaAtual + "W" + RESET);
				System.out.println(BLUE + "  Progresso: " + (i + 1) + "/" + potencias.length + RESET);
				System.out.println(BLUE + "  Canal: " + canalAtual + RESET);

				// 3.1. Configurar nova potência
				System.out.println(BLUE + "  3.1. Configurando potência para " + potenciaAtual + "W" + RESET);
				configurarPotencia(driver, wait, String.valueOf(potenciaAtual));

				// 3.2. Ligar o MTX3
				System.out.println(BLUE + "  3.2. Ligando MTX3" + RESET);
				ligarMTX3(driver, wait);

				// 3.3. Primeira medição de temperatura
				System.out.println(BLUE + "  3.3. Primeira medição de temperatura..." + RESET);
				String temperaturaAntes = verificarTemperatura(driver, wait);
				System.out.println(BLUE + "    Temperatura inicial: " + temperaturaAntes + "°C" + RESET);

				// 3.4. Aguardar 1 minuto
				System.out.println(BLUE + "  3.4. Aguardando 1 minuto para estabilização..." + RESET);
				Thread.sleep(60000);

				// 3.5. Segunda medição de temperatura
				System.out.println(BLUE + "  3.5. Segunda medição de temperatura..." + RESET);
				String temperaturaDepois = verificarTemperatura(driver, wait);
				System.out.println(BLUE + "    Temperatura final: " + temperaturaDepois + "°C" + RESET);

				// Registrar no histórico
				historicoTemperaturas.add("Potência " + potenciaAtual + "W: " +
						temperaturaAntes + "°C → " + temperaturaDepois + "°C");

				// 3.6. Verificar se temperaturas são iguais
				if (temperaturaAntes.equals(temperaturaDepois)) {
					System.out.println(BLUE + "  Temperatura estabilizada em " + temperaturaAntes + "°C" + RESET);

					// Se não for a última potência, desligar para próxima
					if (i < potencias.length - 1) {
						System.out.println(BLUE + "  3.6. Desligando MTX3 para próxima potência" + RESET);
						desligarMTX3(driver, wait);
					} else {
						System.out.println(BLUE + "  Última potência concluída com sucesso!" + RESET);
					}
				} else {
					System.out.println(BLUE + "  Temperatura não estabilizada: " + temperaturaAntes + "°C ≠ " + temperaturaDepois + "°C" + RESET);
					System.out.println(BLUE + "  Interrompendo sequência na potência " + potenciaAtual + "W" + RESET);
					todasPotenciasConcluidas = false;

					// Desligar MTX3 antes de sair
					desligarMTX3(driver, wait);
					break;
				}
			}

			// ========== ETAPA 4: RESULTADOS FINAIS ==========
			System.out.println(BLUE + "\n[ETAPA 4] Coletando resultados finais" + RESET);

			String potenciaFinal = verificarPotencia(driver, wait);
			String temperaturaFinal = verificarTemperatura(driver, wait);
			String canalFinal = verificarCanal(driver, wait);

			// ========== ETAPA 5: SALVAR LOG ==========
			System.out.println(BLUE + "\n[ETAPA 5] Salvando log" + RESET);
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

			// ========== ETAPA 6: FAZER CHECAGEM FINAL DE VALORES ==========
			System.out.println(BLUE + "\n[ETAPA 6] FAZENDO CHECAGEM FINAL" + RESET);

			// faz a checagem final
			Map<String, Object> buscaResultado = chamarBuscaAutomatica();
			resultado.put("busca_automatica", buscaResultado);

		} catch (Exception e) {
			System.err.println("Erro no processamento do canal " + canal + ": " + e.getMessage());
			e.printStackTrace();

			resultado.put("status", "erro");
			resultado.put("mensagem", "Erro no canal " + canal + ": " + e.getMessage());
			resultado.put("canal", canal);
		}

		return resultado;
	}

	// Método para chamar a checagem final
	private Map<String, Object> chamarBuscaAutomatica() {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String url = "http://localhost:8087/executar-manualmente";
			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				return response.getBody();
			} else {
				Map<String, Object> erro = new HashMap<>();
				erro.put("status", "erro");
				erro.put("mensagem", "Falha na busca automática");
				return erro;
			}
		} catch (Exception e) {
			Map<String, Object> erro = new HashMap<>();
			erro.put("status", "erro");
			erro.put("mensagem", "Exceção na busca automática: " + e.getMessage());
			return erro;
		}
	}

	// verificar canal
	private String verificarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
		try {
			// Navegar para Modulator3
			WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Modulator3___']/input")));
			modulator3.click();
			Thread.sleep(300);

			// Tentar várias estratégias para pegar o valor do canal
			WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator3_Config_UpConverter_ChannelNumber")));
			Thread.sleep(300);

			// Estratégia 1: Pegar o texto diretamente
			String canalTexto = canalElement.getText().trim();
			System.out.println(BLUE + "    Texto do elemento canal: " + canalTexto + RESET);

			// Estratégia 2: Se não conseguir, pegar o value attribute
			if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator3.Config.UpConverter.ChannelNumber")) {
				canalTexto = canalElement.getAttribute("value");
				System.out.println(BLUE + "    Value attribute do canal: " + canalTexto + RESET);
			}

			// Estratégia 3: Se ainda não, tentar innerText
			if (canalTexto == null || canalTexto.isEmpty()) {
				canalTexto = canalElement.getAttribute("innerText");
				System.out.println(BLUE + "    InnerText do canal: " + canalTexto + RESET);
			}

			// Extrair números do texto (pode vir como "Modulator3.Config.UpConverter.ChannelNumber = 14")
			if (canalTexto != null && !canalTexto.isEmpty()) {
				// Se tiver formato "Modulator3.Config.UpConverter.ChannelNumber = 14"
				if (canalTexto.contains("=")) {
					String[] partes = canalTexto.split("=");
					if (partes.length > 1) {
						canalTexto = partes[1].trim();
					}
				}

				// Remover qualquer caractere não numérico
				String numeros = canalTexto.replaceAll("[^0-9]", "").trim();

				if (!numeros.isEmpty()) {
					System.out.println(BLUE + "    Canal extraído: " + numeros + RESET);
					return numeros;
				}
			}

			System.out.println(BLUE + "    Não conseguiu extrair canal, retornando N/A" + RESET);
			return "N/A";

		} catch (Exception e) {
			System.err.println("    Erro ao verificar canal: " + e.getMessage());
			return "N/A";
		}
	}

	// mudar o canal  14 - 34 - 51
	private String mudarCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
		try {
			// Acessar Modulator 3
			WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Modulator3___']/input")));
			modulator3.click();
			Thread.sleep(300);

			// Pegar canal atual (antes) - usando o método corrigido
			String canalAntes = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual: " + canalAntes + RESET);

			// Se já estiver no canal correto, retornar
			if (canalAntes.equals(canal)) {
				System.out.println(BLUE + "  Já está no canal " + canal + RESET);
				return canalAntes;
			}

			// Clicar para editar o canal
			WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
					By.id("Modulator3_Config_UpConverter_ChannelNumber")));

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
			System.out.println(BLUE + "  Canal configurado: " + canalDepois + RESET);

			// Verificar se realmente mudou
			int tentativas = 0;
			while (!canalDepois.equals(canal) && tentativas < 3) {
				System.out.println(BLUE + "  Canal não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
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
		String filePath = System.getProperty("user.dir") + "/log_linearizacao_canal_" + canalSolicitado + "_mtx3.txt";
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
			System.out.println(BLUE + "  Log salvo em: " + filePath + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao salvar log: " + e.getMessage());
		}
	}

	@PostMapping("/executar-linearizacao-canal-mtx3")
	public ResponseEntity<Map<String, Object>> executarLinearizacaoCanal(@RequestParam String canal) {
		Map<String, Object> resposta = new HashMap<>();
		WebDriver driver = null;

		try {
			System.out.println(BLUE + "=== INICIANDO LINEARIZAÇÃO PARA CANAL: " + canal + " ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

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

			driver.get("http://10.10.103.103/debug/");
			System.out.println(BLUE + "Página acessada" + RESET);
			fazerLogin(driver, wait);
			System.out.println(BLUE + "Login realizado" + RESET);

			// Processo de linearização para o canal específico
			//Map<String, Object> resultado = processoLinearizacaoParaCanal(driver, wait, canal);
			Map<String, Object> resultado = processoCompleto(driver, wait);


			// Resposta final
			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Linearização para canal " + canal + " executada com sucesso");
			resposta.put("hora_inicio", LocalDateTime.now().toString());
			resposta.put("hora_fim", LocalDateTime.now().toString());
			resposta.put("detalhes", resultado);

			System.out.println(BLUE + "\n=== LINEARIZAÇÃO CONCLUÍDA PARA CANAL: " + canal + " ===" + RESET);
			System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);

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
				System.out.println(BLUE + "Driver finalizado" + RESET);
			}
		}
	}

	@PostMapping("/cancelar-linearizacao-mtx3")
	public ResponseEntity<Map<String, Object>> cancelarLinearizacao() {
		Map<String, Object> resposta = new HashMap<>();

		System.out.println(BLUE + "Solicitação de cancelamento de linearização MTX3 recebida" + RESET);

		resposta.put("status", "sucesso");
		resposta.put("mensagem", "Solicitação de cancelamento recebida");
		resposta.put("hora_cancelamento", LocalDateTime.now().toString());

		return ResponseEntity.ok(resposta);
	}

}