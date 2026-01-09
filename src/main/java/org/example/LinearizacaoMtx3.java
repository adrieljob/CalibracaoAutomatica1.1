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
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class LinearizacaoMtx3 {

	private static final String RESET = "\u001B[0m";
	private static final String BLUE = "\u001B[34m";
	private static final String RED = "\u001B[31m";

	// Controle de cancelamento
	private static volatile boolean CANCELAR_LINEARIZACAO_MTX3 = false;
	private static Thread threadLinearizacaoAtual = null;

	// Status
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

		try {
			// Verifica se já está rodando
			if (threadLinearizacaoAtual != null && threadLinearizacaoAtual.isAlive()) {
				respostaGeral.put("status", "erro");
				respostaGeral.put("mensagem", "Já existe uma linearização em execução no MTX3");
				return ResponseEntity.status(409).body(respostaGeral);
			}

			System.out.println(BLUE + "=== INICIANDO ROTINA DE LINEARIZAÇÃO MTX3 ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			// Reset flag de cancelamento
			CANCELAR_LINEARIZACAO_MTX3 = false;

			// Criar e iniciar thread
			threadLinearizacaoAtual = new Thread(() -> {
				try {
					executarRotinaLinearizacaoThread();
				} catch (Exception e) {
					System.err.println(RED + "Erro na thread da linearização MTX3: " + e.getMessage() + RESET);
				} finally {
					threadLinearizacaoAtual = null;
					linearizacaoAtivaMtx3 = false;
				}
			});

			threadLinearizacaoAtual.setName("MTX3-Linearizacao-Thread");
			threadLinearizacaoAtual.start();

			respostaGeral.put("status", "sucesso");
			respostaGeral.put("mensagem", "Rotina de linearização MTX3 iniciada em background");
			respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
			respostaGeral.put("pode_cancelar", true);

			return ResponseEntity.ok(respostaGeral);

		} catch (Exception e) {
			respostaGeral.put("status", "erro");
			respostaGeral.put("mensagem", "Erro ao iniciar rotina de linearização MTX3: " + e.getMessage());
			return ResponseEntity.status(500).body(respostaGeral);
		}
	}

	// Método que executa a linearização em thread separada
	private void executarRotinaLinearizacaoThread() {
		WebDriver driver = null;

		try {
			// Configurar driver
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

			// Login
			driver.get("http://10.10.103.103/debug/");
			System.out.println(BLUE + "Página acessada" + RESET);
			fazerLogin(driver, wait);
			System.out.println(BLUE + "Login realizado" + RESET);

			// Processo completo
			Map<String, Object> resultado = processoCompleto(driver, wait);

			if ("sucesso".equals(resultado.get("status")) || "parcial".equals(resultado.get("status"))) {
				System.out.println(BLUE + "\n=== ROTINA DE LINEARIZAÇÃO MTX3 FINALIZADA ===" + RESET);
				System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);
			} else if ("cancelado".equals(resultado.get("status"))) {
				System.out.println(RED + "\n=== ROTINA DE LINEARIZAÇÃO MTX3 CANCELADA ===" + RESET);
				System.out.println(RED + "Hora de cancelamento: " + LocalDateTime.now() + RESET);
			} else {
				System.err.println("\n=== ERRO NA ROTINA DE LINEARIZAÇÃO MTX3 ===");
			}

		} catch (InterruptedException e) {
			System.out.println(RED + "\n=== LINEARIZAÇÃO MTX3 CANCELADA PELO USUÁRIO ===" + RESET);
			System.out.println(RED + "Hora de cancelamento: " + LocalDateTime.now() + RESET);
			Thread.currentThread().interrupt();

		} catch (Exception e) {
			System.err.println("\n=== ERRO NA ROTINA DE LINEARIZAÇÃO MTX3 ===");
			System.err.println("Erro: " + e.getMessage());
			e.printStackTrace();

		} finally {
			// Fechar driver
			if (driver != null) {
				try {
					driver.quit();
					System.out.println(BLUE + "Driver finalizado" + RESET);
				} catch (Exception e) {
					System.err.println("Erro ao finalizar driver: " + e.getMessage());
				}
			}

			// Reset flags
			CANCELAR_LINEARIZACAO_MTX3 = false;
			linearizacaoAtivaMtx3 = false;
		}
	}

	@GetMapping("/status-linearizacao-mtx3")
	public ResponseEntity<Map<String, Object>> getStatusLinearizacaoMtx3() {
		Map<String, Object> resposta = new HashMap<>();

		resposta.put("ativo", linearizacaoAtivaMtx3);
		resposta.put("cancelamento_solicitado", CANCELAR_LINEARIZACAO_MTX3);
		resposta.put("hora_inicio", horaInicioLinearizacaoMtx3 != null ? horaInicioLinearizacaoMtx3.toString() : null);
		resposta.put("detalhes", statusLinearizacaoMtx3);
		resposta.put("ultima_atualizacao", LocalDateTime.now().toString());
		resposta.put("thread_ativa", threadLinearizacaoAtual != null && threadLinearizacaoAtual.isAlive());

		// Verificar timeout (30 minutos para linearização)
		if (linearizacaoAtivaMtx3 && horaInicioLinearizacaoMtx3 != null) {
			Duration duracao = Duration.between(horaInicioLinearizacaoMtx3, LocalDateTime.now());
			if (duracao.toMinutes() > 30) {
				CANCELAR_LINEARIZACAO_MTX3 = true;
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
			// ========== ETAPA 1: VERIFICAR CANAL ATUAL ==========
			System.out.println(BLUE + "\nVerificando canal atual" + RESET);

			// VERIFICA CANCELAMENTO
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Linearização cancelada antes de verificar canal");
			}

			String canalAtual = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual: " + canalAtual + RESET);

			// Guardar o canal atual para usar no cancelamento
			resultado.put("canal_atual", canalAtual);

			// ========== ETAPA 2: INFORMAÇÕES INICIAIS ==========
			System.out.println(BLUE + "\nColetando informações iniciais" + RESET);

			// VERIFICA CANCELAMENTO
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Linearização cancelada durante coleta de informações");
			}

			String potenciaInicial = verificarPotencia(driver, wait);
			String temperaturaInicial = verificarTemperatura(driver, wait);
			System.out.println(BLUE + "  Potência inicial: " + potenciaInicial + "W" + RESET);
			System.out.println(BLUE + "  Temperatura inicial: " + temperaturaInicial + "°C" + RESET);

			// ========== ETAPA 3: LOOP DE POTÊNCIAS ==========
			int[] potencias = {300, 340, 370, 430, 483}; // menor pro maior
			int ultimaPotenciaProcessada = 0;
			boolean todasPotenciasConcluidas = true;
			int tentativasEstabilizacao = 10; // Número máximo de tentativas
			int tempoEspera = 180000; // 180 segundos
			int margemTolerancia = 1; // ±1°C

			statusLinearizacaoMtx3.put("etapa", "loop_potencias");
			statusLinearizacaoMtx3.put("total_potencias", potencias.length);
			statusLinearizacaoMtx3.put("potencia_atual_index", 0);

			for (int i = 0; i < potencias.length; i++) {
				// VERIFICA CANCELAMENTO ANTES DE CADA POTÊNCIA
				if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Linearização cancelada durante processamento de potências");
				}

				int potenciaAtual = potencias[i];
				ultimaPotenciaProcessada = potenciaAtual;

				// Atualiza status
				statusLinearizacaoMtx3.put("potencia_atual", potenciaAtual);
				statusLinearizacaoMtx3.put("potencia_atual_index", i + 1);

				System.out.println(BLUE + "\n[ETAPA 1] Desligando MTX3 para iniciar sequência" + RESET);
				desligarMTX3(driver, wait);

				System.out.println(BLUE + "\n[ETAPA 2] Processando potência: " + potenciaAtual + "W" + RESET);
				System.out.println(BLUE + "  Progresso: " + (i + 1) + "/" + potencias.length + RESET);

				// 2.1. Configurar nova potência
				System.out.println(BLUE + "  2.1. Configurando potência para " + potenciaAtual + "W" + RESET);
				configurarPotencia(driver, wait, String.valueOf(potenciaAtual));

				// VERIFICA CANCELAMENTO
				if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Linearização cancelada durante configuração de potência");
				}

				// 2.2. Ligar o MTX3
				System.out.println(BLUE + "  2.2. Ligando MTX3" + RESET);
				ligarMTX3(driver, wait);

				// VERIFICA CANCELAMENTO
				if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Linearização cancelada após ligar MTX3");
				}

				// 2.3. AGUARDAR ESTABILIZAÇÃO DA TEMPERATURA
				statusLinearizacaoMtx3.put("etapa", "estabilizacao_temperatura");
				boolean temperaturaEstabilizada = false;
				String temperaturaAnterior = "";
				String temperaturaAtual = "";

				for (int tentativa = 1; tentativa <= tentativasEstabilizacao; tentativa++) {
					// VERIFICA CANCELAMENTO A CADA TENTATIVA
					if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
						throw new InterruptedException("Linearização cancelada durante estabilização de temperatura");
					}

					statusLinearizacaoMtx3.put("tentativa_estabilizacao", tentativa);

					System.out.println(BLUE + "\n  Tentativa " + tentativa + "/" + tentativasEstabilizacao + " de estabilização" + RESET);

					// Primeira medição
					System.out.println(BLUE + "    Primeira medição de temperatura..." + RESET);
					temperaturaAnterior = verificarTemperatura(driver, wait);
					System.out.println(BLUE + "    Temperatura inicial: " + temperaturaAnterior + "°C" + RESET);

					// Aguardar com verificação de cancelamento
					System.out.println(BLUE + "    Aguardando " + (tempoEspera/1000) + " segundos..." + RESET);
					if (!aguardarComCancelamento(tempoEspera)) {
						throw new InterruptedException("Linearização cancelada durante espera por estabilização");
					}

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
				}

				// 2.4. VERIFICAR SE TEMPERATURA ESTABILIZOU
				if (temperaturaEstabilizada) {
					System.out.println(BLUE + "  Potência " + potenciaAtual + "W processada com sucesso" + RESET);

					if (i < potencias.length - 1) {
						// Aguardar 3 segundos entre potências
						System.out.println(BLUE + "  Aguardando 3 segundos antes da próxima potência..." + RESET);
						if (!aguardarComCancelamento(3000)) {
							throw new InterruptedException("Linearização cancelada durante espera entre potências");
						}
					} else {
						System.out.println(BLUE + "  Última potência concluída com sucesso!" + RESET);
					}
				} else {
					System.out.println(BLUE + "  Temperatura não estabilizou após " + tentativasEstabilizacao + " tentativas" + RESET);
					System.out.println(BLUE + "  Interrompendo sequência na potência " + potenciaAtual + "W" + RESET);
					todasPotenciasConcluidas = false;
					break;
				}
			}

			// VERIFICA CANCELAMENTO ANTES DE COLETAR RESULTADOS
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Linearização cancelada antes de coletar resultados");
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

		} catch (InterruptedException e) {
			System.out.println(RED + "[MTX3] Linearização interrompida pelo usuário" + RESET);
			resultado.put("status", "cancelado");
			resultado.put("mensagem", "Linearização cancelada pelo usuário");
			resultado.put("hora_cancelamento", LocalDateTime.now().toString());
			Thread.currentThread().interrupt();

		} catch (Exception e) {
			System.err.println("Erro no processamento do MTX3: " + e.getMessage());
			e.printStackTrace();

			resultado.put("status", "erro");
			resultado.put("mensagem", "Erro no MTX3: " + e.getMessage());
		} finally {
			linearizacaoAtivaMtx3 = false;
			statusLinearizacaoMtx3.put("status", resultado.get("status"));
			statusLinearizacaoMtx3.put("fim", LocalDateTime.now().toString());
		}

		return resultado;
	}

	// Método auxiliar para aguardar com verificação de cancelamento
	private boolean aguardarComCancelamento(long millis) throws InterruptedException {
		long intervalo = 1000; // Verifica a cada 1 segundo
		long tempoRestante = millis;

		while (tempoRestante > 0) {
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				System.out.println(RED + "[MTX3] Cancelamento detectado durante espera" + RESET);
				return false;
			}

			try {
				long tempoAguardar = Math.min(intervalo, tempoRestante);
				Thread.sleep(tempoAguardar);
				tempoRestante -= tempoAguardar;
			} catch (InterruptedException e) {
				System.out.println(RED + "[MTX3] Thread interrompida durante espera" + RESET);
				Thread.currentThread().interrupt();
				return false;
			}
		}

		return true;
	}

	@PostMapping("/executar-linearizacao-canal-mtx3")
	public ResponseEntity<Map<String, Object>> executarLinearizacaoCanal(@RequestParam String canal) {
		Map<String, Object> resposta = new HashMap<>();

		try {
			// Verifica se já está rodando
			if (threadLinearizacaoAtual != null && threadLinearizacaoAtual.isAlive()) {
				resposta.put("status", "erro");
				resposta.put("mensagem", "Já existe uma linearização em execução no MTX3");
				return ResponseEntity.status(409).body(resposta);
			}

			System.out.println(BLUE + "=== INICIANDO LINEARIZAÇÃO PARA CANAL: " + canal + " ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			// Reset flag de cancelamento
			CANCELAR_LINEARIZACAO_MTX3 = false;

			// Criar e iniciar thread
			threadLinearizacaoAtual = new Thread(() -> {
				try {
					executarLinearizacaoCanalThread(canal);
				} catch (Exception e) {
					System.err.println(RED + "Erro na thread da linearização MTX3: " + e.getMessage() + RESET);
				} finally {
					threadLinearizacaoAtual = null;
				}
			});

			threadLinearizacaoAtual.setName("MTX3-Linearizacao-Canal-" + canal);
			threadLinearizacaoAtual.start();

			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Linearização para canal " + canal + " iniciada em background");
			resposta.put("hora_inicio", LocalDateTime.now().toString());
			resposta.put("pode_cancelar", true);

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao iniciar linearização MTX3: " + e.getMessage());
			return ResponseEntity.status(500).body(resposta);
		}
	}

	// Método que executa a linearização do canal em thread separada
	private void executarLinearizacaoCanalThread(String canal) {
		WebDriver driver = null;

		try {
			System.out.println(BLUE + "=== INICIANDO LINEARIZAÇÃO EM THREAD PARA CANAL " + canal + " DO MTX3 ===" + RESET);

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

			// Login
			driver.get("http://10.10.103.103/debug/");
			System.out.println(BLUE + "Página acessada" + RESET);
			fazerLogin(driver, wait);
			System.out.println(BLUE + "Login realizado" + RESET);

			// Processo de linearização
			Map<String, Object> resultado = processoLinearizacaoParaCanal(driver, wait, canal);

			if ("sucesso".equals(resultado.get("status")) || "parcial".equals(resultado.get("status"))) {
				System.out.println(BLUE + "\n=== LINEARIZAÇÃO CONCLUÍDA PARA CANAL: " + canal + " ===" + RESET);
				System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);
			} else if ("cancelado".equals(resultado.get("status"))) {
				System.out.println(RED + "\n=== LINEARIZAÇÃO CANCELADA PARA CANAL: " + canal + " ===" + RESET);
				System.out.println(RED + "Hora de cancelamento: " + LocalDateTime.now() + RESET);
			} else {
				System.err.println("\n=== ERRO NA LINEARIZAÇÃO PARA CANAL: " + canal + " ===");
			}

		} catch (Exception e) {
			System.err.println("Erro na thread da linearização MTX3: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (driver != null) {
				try {
					driver.quit();
					System.out.println(BLUE + "Driver finalizado" + RESET);
				} catch (Exception e) {
					System.err.println("Erro ao finalizar driver: " + e.getMessage());
				}
			}

			// Reset flag de cancelamento
			CANCELAR_LINEARIZACAO_MTX3 = false;
		}
	}

	@PostMapping("/cancelar-linearizacao-mtx3")
	public ResponseEntity<Map<String, Object>> cancelarLinearizacao() {
		try {
			System.out.println(RED + "\n=== SOLICITAÇÃO DE CANCELAMENTO DA LINEARIZAÇÃO MTX3 RECEBIDA ===" + RESET);
			System.out.println(RED + "Hora: " + LocalDateTime.now() + RESET);

			CANCELAR_LINEARIZACAO_MTX3 = true;

			// Interrompe thread se estiver rodando
			if (threadLinearizacaoAtual != null && threadLinearizacaoAtual.isAlive()) {
				threadLinearizacaoAtual.interrupt();
				System.out.println(RED + "Thread de linearização MTX3 interrompida" + RESET);
			}

			Map<String, Object> resposta = new HashMap<>();
			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Linearização MTX3 cancelada com sucesso");
			resposta.put("hora_cancelamento", LocalDateTime.now().toString());
			resposta.put("thread_ativa", threadLinearizacaoAtual != null && threadLinearizacaoAtual.isAlive());

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			System.err.println("Erro ao cancelar linearização MTX3: " + e.getMessage());

			Map<String, Object> resposta = new HashMap<>();
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao cancelar linearização MTX3: " + e.getMessage());
			return ResponseEntity.status(500).body(resposta);
		}
	}

	// Métodos auxiliares (mantenha os mesmos da sua versão original, apenas adicionando verificações de cancelamento onde houver Thread.sleep)

	// verificar o valor da potencia
	private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();

		// VERIFICA CANCELAMENTO
		if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Cancelamento durante verificação de potência");
		}

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante espera após click");
		}

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

		// VERIFICA CANCELAMENTO
		if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Cancelamento durante verificação de temperatura");
		}

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante espera após click");
		}

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

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante desligamento");
		}

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

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento após desligar MTX3");
		}
	}

	// ligar mtx3
	private void ligarMTX3(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante ligamento");
		}

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

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento após ligar MTX3");
		}
	}

	// fazer login
	private void fazerLogin(WebDriver driver, WebDriverWait wait) throws Exception {
		WebElement usernameField = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//input[@type='text']")));
		usernameField.clear();
		usernameField.sendKeys(username);

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante login");
		}

		WebElement passwordField = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//input[@type='password']")));
		passwordField.clear();
		passwordField.sendKeys(password);

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante login");
		}

		WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//button[text()='Sign In']")));
		loginButton.click();

		if (!aguardarComCancelamento(500)) {
			throw new InterruptedException("Cancelamento após login");
		}
	}

	// configurar a potencia
	private void configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento durante configuração de potência");
		}

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

		if (!aguardarComCancelamento(300)) {
			throw new InterruptedException("Cancelamento após configurar potência");
		}
	}

	// configurar os valores
	private boolean configurarValor(WebDriver driver, WebElement elemento, String valor) {
		try {
			elemento.click();

			// VERIFICA CANCELAMENTO
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				return false;
			}

			if (!aguardarComCancelamento(300)) {
				return false;
			}

			WebElement campoInput = driver.switchTo().activeElement();
			if (campoInput.getTagName().equals("input") || campoInput.getTagName().equals("textarea")) {
				campoInput.click();

				if (!aguardarComCancelamento(300)) {
					return false;
				}

				campoInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
				campoInput.sendKeys(Keys.DELETE);
				campoInput.sendKeys(valor);

				if (!aguardarComCancelamento(300)) {
					return false;
				}

				campoInput.sendKeys(Keys.ENTER);

				if (!aguardarComCancelamento(300)) {
					return false;
				}

				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Erro ao configurar valor: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	// algumas tentativas de envio de informações (comandos SET)
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

	// linearização do canal específico (versão adaptada)
	private Map<String, Object> processoLinearizacaoParaCanal(WebDriver driver, WebDriverWait wait, String canal) {
		Map<String, Object> resultado = new HashMap<>();

		try {
			// ========== ETAPA 0: MUDAR PARA O CANAL ==========
			System.out.println(BLUE + "\n[ETAPA 0] Mudando para canal: " + canal + RESET);

			// VERIFICA CANCELAMENTO
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Cancelamento antes de mudar canal");
			}

			// Primeiro verifica o canal atual
			String canalAtual = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual: " + canalAtual + RESET);

			// Se não for o canal desejado, muda
			if (!canalAtual.equals(canal) && !canalAtual.contains(canal)) {
				System.out.println(BLUE + "  Mudando para canal " + canal + "..." + RESET);
				mudarCanal(driver, wait, canal);

				// Aguarda mudança com verificação de cancelamento
				if (!aguardarComCancelamento(2000)) {
					throw new InterruptedException("Cancelamento após mudar canal");
				}

				canalAtual = verificarCanal(driver, wait);
				System.out.println(BLUE + "  Novo canal: " + canalAtual + RESET);
			}

			// ========== ETAPA 1: INFORMAÇÕES INICIAIS ==========
			System.out.println(BLUE + "\n[ETAPA 1] Coletando informações iniciais" + RESET);

			// VERIFICA CANCELAMENTO
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Cancelamento durante coleta de informações");
			}

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
			List<String> historicoTemperaturas = new ArrayList<>();

			for (int i = 0; i < potencias.length; i++) {
				// VERIFICA CANCELAMENTO ANTES DE CADA POTÊNCIA
				if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
					throw new InterruptedException("Cancelamento durante processamento de potências");
				}

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
				if (!aguardarComCancelamento(60000)) {
					throw new InterruptedException("Cancelamento durante espera por estabilização");
				}

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

			// VERIFICA CANCELAMENTO ANTES DE COLETAR RESULTADOS
			if (CANCELAR_LINEARIZACAO_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Cancelamento antes de coletar resultados");
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

		} catch (InterruptedException e) {
			System.out.println(RED + "[MTX3] Linearização do canal " + canal + " cancelada" + RESET);
			resultado.put("status", "cancelado");
			resultado.put("mensagem", "Linearização do canal " + canal + " cancelada pelo usuário");
			resultado.put("hora_cancelamento", LocalDateTime.now().toString());
			Thread.currentThread().interrupt();

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

			if (!aguardarComCancelamento(300)) {
				throw new InterruptedException("Cancelamento durante verificação de canal");
			}

			WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator3_Config_UpConverter_ChannelNumber")));

			if (!aguardarComCancelamento(300)) {
				throw new InterruptedException("Cancelamento durante espera por elemento do canal");
			}

			String canalTexto = canalElement.getText().trim();
			System.out.println(BLUE + "    Texto do elemento canal: " + canalTexto + RESET);

			if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator3.Config.UpConverter.ChannelNumber")) {
				canalTexto = canalElement.getAttribute("value");
				System.out.println(BLUE + "    Value attribute do canal: " + canalTexto + RESET);
			}

			if (canalTexto == null || canalTexto.isEmpty()) {
				canalTexto = canalElement.getAttribute("innerText");
				System.out.println(BLUE + "    InnerText do canal: " + canalTexto + RESET);
			}

			if (canalTexto != null && !canalTexto.isEmpty()) {
				if (canalTexto.contains("=")) {
					String[] partes = canalTexto.split("=");
					if (partes.length > 1) {
						canalTexto = partes[1].trim();
					}
				}

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

			if (!aguardarComCancelamento(300)) {
				throw new InterruptedException("Cancelamento durante mudança de canal");
			}

			String canalAntes = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual: " + canalAntes + RESET);

			if (canalAntes.equals(canal)) {
				System.out.println(BLUE + "  Já está no canal " + canal + RESET);
				return canalAntes;
			}

			WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
					By.id("Modulator3_Config_UpConverter_ChannelNumber")));

			boolean sucesso = false;

			// Estratégia 1: Double click
			try {
				new org.openqa.selenium.interactions.Actions(driver)
						.doubleClick(canalElement)
						.perform();

				if (!aguardarComCancelamento(300)) {
					throw new InterruptedException("Cancelamento durante mudança de canal");
				}

				WebElement activeElement = driver.switchTo().activeElement();
				if (activeElement.getTagName().equals("input")) {
					activeElement.clear();

					if (!aguardarComCancelamento(300)) {
						throw new InterruptedException("Cancelamento durante limpeza do campo");
					}

					activeElement.sendKeys(canal);

					if (!aguardarComCancelamento(300)) {
						throw new InterruptedException("Cancelamento durante digitação do canal");
					}

					activeElement.sendKeys(Keys.ENTER);
					sucesso = true;
				}
			} catch (Exception e) {
				System.err.println("  Estratégia 1 falhou: " + e.getMessage());
			}

			if (!sucesso) {
				try {
					canalElement.click();

					if (!aguardarComCancelamento(300)) {
						throw new InterruptedException("Cancelamento durante click no campo");
					}

					canalElement.sendKeys(Keys.chord(Keys.CONTROL, "a"));
					canalElement.sendKeys(Keys.DELETE);
					canalElement.sendKeys(canal);

					if (!aguardarComCancelamento(300)) {
						throw new InterruptedException("Cancelamento durante digitação");
					}

					canalElement.sendKeys(Keys.ENTER);
					sucesso = true;
				} catch (Exception e) {
					System.err.println("  Estratégia 2 falhou: " + e.getMessage());
				}
			}

			if (!sucesso) {
				try {
					JavascriptExecutor js = (JavascriptExecutor) driver;
					js.executeScript("arguments[0].value = arguments[1];", canalElement, canal);

					if (!aguardarComCancelamento(300)) {
						throw new InterruptedException("Cancelamento durante execução JavaScript");
					}

					js.executeScript("arguments[0].dispatchEvent(new Event('change'));", canalElement);
					sucesso = true;
				} catch (Exception e) {
					System.err.println("  Estratégia 3 falhou: " + e.getMessage());
				}
			}

			if (!sucesso) {
				throw new Exception("Não foi possível mudar o canal");
			}

			// Aguarda mudança com verificação de cancelamento
			if (!aguardarComCancelamento(2000)) {
				throw new InterruptedException("Cancelamento após mudar canal");
			}

			String canalDepois = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal configurado: " + canalDepois + RESET);

			int tentativas = 0;
			while (!canalDepois.equals(canal) && tentativas < 3) {
				System.out.println(BLUE + "  Canal não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);

				if (!aguardarComCancelamento(1000)) {
					throw new InterruptedException("Cancelamento durante verificação do canal");
				}

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

	// salvar log do canal específico
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
			for (String historicoTemp : historico) {
				writer.write("  " + historicoTemp + "\n");
			}
			System.out.println(BLUE + "  Log salvo em: " + filePath + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao salvar log: " + e.getMessage());
		}
	}
}