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
public class AjustarOffSetMtx3 {

	// Códigos de cores para console (azul para MTX3)
	private static final String RESET = "\u001B[0m";
	private static final String BLUE = "\u001B[34m";
	private static final String RED = "\u001B[31m";

	// Controle de cancelamento
	private static volatile boolean CANCELAR_OFFSET_MTX3 = false;
	private static Thread threadOffsetAtual = null;

	// Credenciais de login (configuráveis via application.properties)
	@Value("${app.username:admin}")
	private String username;

	@Value("${app.password:admin}")
	private String password;

	// Endpoint principal para executar rotina completa nos 3 canais do MTX3
	@PostMapping("/executar-rotina-completa-mtx3")
	public ResponseEntity<Map<String, Object>> executarRotinaCompleta() {
		Map<String, Object> respostaGeral = new HashMap<>();
		Map<String, Object> resultados = new HashMap<>();
		WebDriver driver = null;

		try {
			// Verifica se já está rodando
			if (threadOffsetAtual != null && threadOffsetAtual.isAlive()) {
				respostaGeral.put("status", "erro");
				respostaGeral.put("mensagem", "Já existe uma rotina em execução no MTX3");
				return ResponseEntity.status(409).body(respostaGeral);
			}

			System.out.println(BLUE + "=== INICIANDO ROTINA COMPLETA MTX3 ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			// Reset flag de cancelamento
			CANCELAR_OFFSET_MTX3 = false;

			// Criar e iniciar thread
			threadOffsetAtual = new Thread(() -> {
				try {
					executarRotinaCompletaThread();
				} catch (Exception e) {
					System.err.println(BLUE + "Erro na thread do MTX3: " + e.getMessage() + RESET);
				} finally {
					threadOffsetAtual = null;
				}
			});

			threadOffsetAtual.setName("MTX3-Offset-Thread");
			threadOffsetAtual.start();

			respostaGeral.put("status", "sucesso");
			respostaGeral.put("mensagem", "Rotina completa MTX3 iniciada em background");
			respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
			respostaGeral.put("pode_cancelar", true);

			return ResponseEntity.ok(respostaGeral);

		} catch (Exception e) {
			respostaGeral.put("status", "erro");
			respostaGeral.put("mensagem", "Erro ao iniciar rotina MTX3: " + e.getMessage());
			return ResponseEntity.status(500).body(respostaGeral);
		}
	}

	// Método que executa a rotina completa em thread separada
	private void executarRotinaCompletaThread() {
		Map<String, Object> resultados = new HashMap<>();
		WebDriver driver = null;

		try {
			System.out.println(BLUE + "=== INICIANDO ROTINA COMPLETA MTX3 EM THREAD ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			// Configurar ChromeDriver
			WebDriverManager.chromedriver().setup();
			ChromeOptions options = new ChromeOptions();
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("--disable-extensions");
			options.addArguments("--disable-gpu");
			options.addArguments("--headless"); // se quiser ver oque está acontecendo comentar essa linha
			options.addArguments("--incognito");
			options.addArguments("--disable-cache");

			driver = new ChromeDriver(options);
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

			// ETAPA 1: LOGIN (apenas uma vez)
			driver.get("http://10.10.103.103/debug/");
			System.out.println(BLUE + "Página acessada" + RESET);
			fazerLogin(driver, wait);
			System.out.println(BLUE + "Login realizado" + RESET);

			// Processar cada canal na sequência: 14, 34, 51
			String[] canais = {"14", "34", "51"};

			for (int i = 0; i < canais.length; i++) {
				// VERIFICA CANCELAMENTO ANTES DE PROCESSAR CADA CANAL
				if (CANCELAR_OFFSET_MTX3 || Thread.currentThread().isInterrupted()) {
					System.out.println(BLUE + "\n[MTX3] Cancelamento solicitado antes do canal " + canais[i] + RESET);
					throw new InterruptedException("Rotina MTX3 cancelada pelo usuário");
				}

				String canal = canais[i];

				System.out.println(BLUE + "\n" + "=".repeat(50) + RESET);
				System.out.println(BLUE + "PROCESSANDO CANAL MTX3: " + canal + RESET);
				System.out.println(BLUE + "=".repeat(50) + RESET);

				// Executar sequência completa para este canal
				Map<String, Object> resultadoCanal = processarCanalCompleto(driver, wait, canal);
				resultados.put("canal_" + canal, resultadoCanal);

				// Se houver falha em um canal, interromper toda a rotina
				if (!"sucesso".equals(resultadoCanal.get("status"))) {
					throw new RuntimeException("Falha no canal MTX3 " + canal + ": " + resultadoCanal.get("mensagem"));
				}

				// Aguardar entre canais (exceto após o último)
				if (i < canais.length - 1) {
					System.out.println(BLUE + "\nAguardando 10 segundos antes do próximo canal..." + RESET);
					if (!aguardarComCancelamento(10000)) {
						throw new InterruptedException("Cancelamento durante espera entre canais");
					}
				}
			}

			System.out.println(BLUE + "\n=== ROTINA COMPLETA MTX3 FINALIZADA COM SUCESSO ===" + RESET);
			System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);

		} catch (InterruptedException e) {
			System.out.println(BLUE + "\n=== ROTINA MTX3 CANCELADA PELO USUÁRIO ===" + RESET);
			System.out.println(BLUE + "Hora de cancelamento: " + LocalDateTime.now() + RESET);
			Thread.currentThread().interrupt();

		} catch (Exception e) {
			System.err.println(BLUE + "\n=== ERRO NA ROTINA MTX3 ===" + RESET);
			System.err.println("Erro: " + e.getMessage());
			e.printStackTrace();

		} finally {
			// Garantir que o driver seja fechado mesmo em caso de erro
			if (driver != null) {
				try {
					driver.quit();
					System.out.println(BLUE + "Driver MTX3 finalizado" + RESET);
				} catch (Exception e) {
					System.err.println("Erro ao finalizar driver MTX3: " + e.getMessage());
				}
			}

			// Reset flag de cancelamento
			CANCELAR_OFFSET_MTX3 = false;
		}
	}
	// Processa um canal individualmente com todas as etapas
	private Map<String, Object> processarCanalCompleto(WebDriver driver, WebDriverWait wait, String canal) {
		Map<String, Object> resultado = new HashMap<>();

		try {
			// ========== ETAPA 1: MUDAR CANAL ==========
			System.out.println(BLUE + "\n[ETAPA 1] Mudando para canal: " + canal + RESET);
			String canalAntes = mudarCanal(driver, wait, canal);

			// VERIFICA CANCELAMENTO
			if (CANCELAR_OFFSET_MTX3 || Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Cancelamento solicitado durante mudança de canal");
			}

			if (!aguardarComCancelamento(2000)) {
				throw new InterruptedException("Cancelamento durante espera após mudar canal");
			}

			// ========== ETAPA 2: AJUSTAR OFFSET (parte inicial) ==========
			System.out.println(BLUE + "\n[ETAPA 2] Configurando offset e potência" + RESET);

			// 2.1. Desligar o MTX3
			System.out.println(BLUE + "  2.1. Desligando MTX3" + RESET);
			desligarMTX3(driver, wait);

			// 2.2. Mudar Thershold para 500
			System.out.println(BLUE + "  2.2. Configurando thershold para 500" + RESET);
			configurarThershold(driver, wait, "500");

			// 2.3. Mudar potência para 486
			System.out.println(BLUE + "  2.3. Configurando potência para 486" + RESET);
			configurarPotencia(driver, wait, "486");

			// 2.4. Ligar o MTX3
			System.out.println(BLUE + "  2.4. Ligando MTX3" + RESET);
			ligarMTX3(driver, wait);

			// 2.5. Esperar 1 minuto
			System.out.println(BLUE + "  2.5. Aguardando 1 minuto para estabilização..." + RESET);
			if (!aguardarComCancelamento(60000)) {
				throw new InterruptedException("Cancelamento durante estabilização");
			}

			// ========== ETAPA 3: VERIFICAR E AJUSTAR DINAMICAMENTE ==========
			System.out.println(BLUE + "\n[ETAPA 3] Verificando e ajustando dinamicamente" + RESET);

			// 2.6. Checa o canal (apenas para confirmar)
			String canalAtual = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual: " + canalAtual + RESET);

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
			System.out.println(BLUE + "\n[ETAPA 4] Coletando resultados finais" + RESET);

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
			System.out.println(BLUE + "\n[ETAPA 6] FAZENDO CHECAGEM FINAL" + RESET);

			Map<String, Object> buscaResultado = chamarBuscaAutomatica();
			resultado.put("busca_automatica", buscaResultado);

			// ========== ETAPA 7: DESLIGAR MTX3 ==========
			System.out.println(BLUE + "\n[ETAPA 7] Desligando MTX3" + RESET);
			desligarMTX3(driver, wait);

		} catch (InterruptedException e) {
			System.out.println(BLUE + "[MTX3] Processamento do canal " + canal + " cancelado" + RESET);
			resultado.put("status", "cancelado");
			resultado.put("mensagem", "Processamento do canal " + canal + " cancelado pelo usuário");
			Thread.currentThread().interrupt();

		} catch (Exception e) {
			System.err.println("Erro no processamento do canal " + canal + ": " + e.getMessage());
			e.printStackTrace();

			resultado.put("status", "erro");
			resultado.put("mensagem", "Erro no canal " + canal + ": " + e.getMessage());
		}

		return resultado;
	}

	// Função para executar ajuste dinâmico por canal COM VERIFICAÇÃO DE CANCELAMENTO
	private Map<String, Object> executarAjusteDinamicoPorCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
		Map<String, Object> resultado = new HashMap<>();

		// LER OFFSET ATUAL DO EQUIPAMENTO MTX3
		int offsetAtual = lerOffsetAtual(driver, wait);
		System.out.println(BLUE + "  Offset inicial lido do equipamento MTX3: " + offsetAtual + RESET);
		resultado.put("offset_inicial", offsetAtual);

		int iteracoes = 0;
		int maxIteracoes = 50;
		int maxAjustes = 10;
		int ajustesFeitos = 0;
		int offsetMaximo = -25;

		int correnteMinima, correnteMaximaErro;

		// Definir limites de corrente para cada canal do MTX3
		switch (canal) {
			case "14":
				correnteMinima = 70;
				correnteMaximaErro = 73;
				System.out.println(BLUE + "  Parâmetros para canal 14 do MTX3: 70-73 A" + RESET);
				break;
			case "34":
				correnteMinima = 65;
				correnteMaximaErro = 68;
				System.out.println(BLUE + "  Parâmetros para canal 34 do MTX3: 65-68 A" + RESET);
				break;
			case "51":
				correnteMinima = 60;
				correnteMaximaErro = 63;
				System.out.println(BLUE + "  Parâmetros para canal 51 do MTX3: 60-63 A" + RESET);
				break;
			default:
				throw new Exception("Canal não suportado no MTX3: " + canal);
		}

		// Loop principal de ajuste
		while (iteracoes < maxIteracoes) {
			// VERIFICA CANCELAMENTO A CADA ITERAÇÃO
			if (CANCELAR_OFFSET_MTX3 || Thread.currentThread().isInterrupted()) {
				System.out.println(BLUE + "  [MTX3] Cancelamento solicitado durante ajuste dinâmico" + RESET);
				throw new InterruptedException("Ajuste dinâmico cancelado pelo usuário");
			}

			iteracoes++;
			System.out.println(BLUE + "\n  --- Loop " + iteracoes + " | Offset: " + offsetAtual + " | Ajustes: " + ajustesFeitos + "/" + maxAjustes + " ---" + RESET);

			String correnteStr = verificarCorrente(driver, wait);
			double correnteDouble = Double.parseDouble(correnteStr);
			int corrente = (int) correnteDouble;
			System.out.println(BLUE + "    Corrente atual MTX3: " + corrente + " A" + RESET);

			// Verificação especial se corrente for zero
			if (corrente == 0) {
				System.out.println(BLUE + "    Corrente = 0 A. Verificando potência e corrente completa do MTX3..." + RESET);

				try {
					String resultadoCompleto = verificarPotenciaEcorrente(driver, wait);
					System.out.println(BLUE + "    Resultado da verificação completa: " + resultadoCompleto + RESET);
				} catch (Exception e) {
					if (e.getMessage() != null && e.getMessage().contains("EQUIPAMENTO DESLIGADO")) {
						System.out.println(BLUE + "    " + e.getMessage() + RESET);
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
				System.out.println(BLUE + "    Corrente atingiu o mínimo (" + correnteMinima + " A)" + RESET);

				System.out.println(BLUE + "    Aguardando 10 segundos para verificação final..." + RESET);
				if (!aguardarComCancelamento(10000)) {
					throw new InterruptedException("Cancelamento durante verificação final");
				}

				// Verificar novamente após espera
				correnteStr = verificarCorrente(driver, wait);
				correnteDouble = Double.parseDouble(correnteStr);
				corrente = (int) correnteDouble;
				System.out.println(BLUE + "    Corrente após 10s: " + corrente + " A" + RESET);

				// Se corrente ultrapassou o máximo permitido
				if (corrente > correnteMaximaErro) {
					ajustesFeitos++;

					// Verificar limites de segurança
					if (ajustesFeitos > maxAjustes) {
						String erroFinal = "ERRO MTX3: Máximo de ajustes (" + maxAjustes + ") atingido. Corrente ainda alta: " + corrente + " A";
						System.err.println("    " + erroFinal);
						resultado.put("status", "erro");
						resultado.put("mensagem", erroFinal);
						resultado.put("iteracoes", iteracoes);
						resultado.put("ajustes_feitos", ajustesFeitos);
						return resultado;
					}

					if (offsetAtual + 5 > offsetMaximo) {
						String erroFinal = "ERRO MTX3: Offset máximo (" + offsetMaximo + ") atingido. Corrente ainda alta: " + corrente + " A";
						System.err.println("    " + erroFinal);
						resultado.put("status", "erro");
						resultado.put("mensagem", erroFinal);
						resultado.put("iteracoes", iteracoes);
						resultado.put("ajustes_feitos", ajustesFeitos);
						return resultado;
					}

					// Aumentar offset para reduzir corrente
					String erroMsg = "Corrente MTX3 " + corrente + " A > limite máximo " + correnteMaximaErro + " A. Ajustando offset...";
					System.err.println("    " + erroMsg + " (Ajuste #" + ajustesFeitos + ")");

					offsetAtual += 5;

					// Registrar ajuste
					if (!resultado.containsKey("ajustes")) {
						resultado.put("ajustes", new ArrayList<String>());
					}
					((List<String>) resultado.get("ajustes")).add(erroMsg + " Offset ajustado para: " + offsetAtual);

					System.out.println(BLUE + "    Aplicando novo offset " + offsetAtual + " no MTX3..." + RESET);
					configurarOffset(driver, wait, String.valueOf(offsetAtual));

					System.out.println(BLUE + "    Aguardando 10 segundos para estabilização..." + RESET);
					if (!aguardarComCancelamento(10000)) {
						throw new InterruptedException("Cancelamento durante estabilização após ajuste");
					}

					continue;
				}

				// Sucesso: corrente dentro dos limites
				System.out.println(BLUE + "    SUCESSO MTX3: Corrente " + corrente + " A dentro dos limites (" +
						correnteMinima + "-" + correnteMaximaErro + " A)" + RESET);
				resultado.put("status", "sucesso");
				resultado.put("mensagem", "Corrente MTX3 ajustada corretamente");
				resultado.put("corrente_final", corrente);
				resultado.put("offset_final", offsetAtual);
				resultado.put("iteracoes", iteracoes);
				resultado.put("ajustes_feitos", ajustesFeitos);
				return resultado;
			}

			// Corrente abaixo do mínimo: diminuir offset
			System.out.println(BLUE + "    Corrente abaixo do mínimo (" + correnteMinima + " A)" + RESET);

			offsetAtual--;
			System.out.println(BLUE + "    Reduzindo offset MTX3 para: " + offsetAtual + RESET);

			// Verificar limite mínimo de offset
			if (offsetAtual < -50) {
				String erroMsg = "ERRO MTX3: Offset chegou a -50 e corrente ainda não atingiu " + correnteMinima + " A";
				System.err.println("    " + erroMsg);
				resultado.put("status", "erro");
				resultado.put("mensagem", erroMsg);
				resultado.put("iteracoes", iteracoes);
				resultado.put("ajustes_feitos", ajustesFeitos);
				return resultado;
			}

			System.out.println(BLUE + "    Aplicando novo offset " + offsetAtual + " no MTX3..." + RESET);
			configurarOffset(driver, wait, String.valueOf(offsetAtual));

			System.out.println(BLUE + "    Aguardando 10 segundos para estabilização..." + RESET);
			if (!aguardarComCancelamento(10000)) {
				throw new InterruptedException("Cancelamento durante estabilização");
			}
		}

		// Se atingiu máximo de iterações
		String erroMsg = "ERRO MTX3: Máximo de " + maxIteracoes + " iterações atingido";
		System.err.println("  " + erroMsg);
		resultado.put("status", "erro");
		resultado.put("mensagem", erroMsg);
		resultado.put("iteracoes", iteracoes);
		resultado.put("ajustes_feitos", ajustesFeitos);
		return resultado;
	}

	// Endpoint para executar ajuste em um canal específico do MTX3
	@PostMapping("/executar-offset-canal-mtx3")
	public ResponseEntity<Map<String, Object>> executaroffsetcanalmtx3(@RequestParam String canal) {
		Map<String, Object> resposta = new HashMap<>();
		WebDriver driver = null;

		try {
			// Verifica se já está rodando
			if (threadOffsetAtual != null && threadOffsetAtual.isAlive()) {
				resposta.put("status", "erro");
				resposta.put("mensagem", "Já existe uma operação em execução no MTX3");
				return ResponseEntity.status(409).body(resposta);
			}

			System.out.println(BLUE + "=== INICIANDO AJUSTE OFFSET PARA CANAL " + canal + " DO MTX3 ===" + RESET);

			// Reset flag de cancelamento
			CANCELAR_OFFSET_MTX3 = false;

			// Criar e iniciar thread
			threadOffsetAtual = new Thread(() -> {
				try {
					executarOffsetCanalThread(canal);
				} catch (Exception e) {
					System.err.println(BLUE + "Erro na thread do offset MTX3: " + e.getMessage() + RESET);
				} finally {
					threadOffsetAtual = null;
				}
			});

			threadOffsetAtual.setName("MTX3-Offset-Canal-" + canal);
			threadOffsetAtual.start();

			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Ajuste offset para canal " + canal + " do MTX3 iniciado em background");
			resposta.put("hora_inicio", LocalDateTime.now().toString());
			resposta.put("pode_cancelar", true);

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao iniciar ajuste offset MTX3: " + e.getMessage());
			return ResponseEntity.status(500).body(resposta);
		}
	}

	// Método que executa o offset do canal em thread separada
	private void executarOffsetCanalThread(String canal) {
		WebDriver driver = null;

		try {
			System.out.println(BLUE + "=== INICIANDO AJUSTE OFFSET EM THREAD PARA CANAL " + canal + " DO MTX3 ===" + RESET);

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

			// Processar canal
			Map<String, Object> resultado = processarCanalCompleto(driver, wait, canal);

			if ("sucesso".equals(resultado.get("status"))) {
				System.out.println(BLUE + "=== AJUSTE OFFSET FINALIZADO COM SUCESSO PARA CANAL " + canal + " DO MTX3 ===" + RESET);
			} else if ("cancelado".equals(resultado.get("status"))) {
				System.out.println(BLUE + "=== AJUSTE OFFSET CANCELADO PARA CANAL " + canal + " DO MTX3 ===" + RESET);
			} else {
				System.err.println("=== ERRO NO AJUSTE OFFSET PARA CANAL " + canal + " DO MTX3 ===");
			}

		} catch (InterruptedException e) {
			System.out.println(BLUE + "[MTX3] Thread interrompida - Cancelamento concluído" + RESET);
			// Não faz nada, apenas loga
		} catch (Exception e) {
			System.err.println("Erro na thread do offset MTX3: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (driver != null) {
				try {
					// Tenta fechar de forma mais agressiva se houver cancelamento
					if (CANCELAR_OFFSET_MTX3) {
						System.out.println(BLUE + "[MTX3] Fechando driver após cancelamento" + RESET);
						try {
							driver.close(); // Tenta close primeiro
						} catch (Exception e) {
							// Ignora erros no close
						}
					}

					driver.quit();
					System.out.println(BLUE + "Driver MTX3 finalizado" + RESET);
				} catch (Exception e) {
					// Ignora erros ao fechar driver durante cancelamento
					if (!CANCELAR_OFFSET_MTX3) {
						System.err.println("Erro ao finalizar driver MTX3: " + e.getMessage());
					}
				}
			}

			// Reset flag de cancelamento
			CANCELAR_OFFSET_MTX3 = false;
		}
	}

	// Endpoint para cancelar offset
	@PostMapping("/cancelar-offset-mtx3")
	public ResponseEntity<Map<String, Object>> cancelarOffsetMtx3() {
		try {
			System.out.println(BLUE + "\n=== SOLICITAÇÃO DE CANCELAMENTO DO OFFSET MTX3 RECEBIDA ===" + RESET);
			System.out.println(BLUE + "Hora: " + LocalDateTime.now() + RESET);

			CANCELAR_OFFSET_MTX3 = true;

			// Interrompe thread se estiver rodando
			if (threadOffsetAtual != null && threadOffsetAtual.isAlive()) {
				// Dá um pequeno tempo para a thread perceber a flag antes de interromper
				Thread.sleep(100);
				threadOffsetAtual.interrupt();
				System.out.println(RED + "Thread de offset MTX3 interrompida" + RESET);
			}

			Map<String, Object> resposta = new HashMap<>();
			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Offset MTX3 cancelado com sucesso");
			resposta.put("hora", LocalDateTime.now().toString());
			resposta.put("thread_ativa", threadOffsetAtual != null && threadOffsetAtual.isAlive());

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			System.err.println("Erro ao cancelar offset MTX3: " + e.getMessage());

			Map<String, Object> resposta = new HashMap<>();
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao cancelar offset MTX3: " + e.getMessage());
			return ResponseEntity.status(500).body(resposta);
		}
	}

	private boolean aguardarComCancelamento(long millis) {
		long intervalo = 1000; // Verifica a cada 1 segundo
		long tempoRestante = millis;

		while (tempoRestante > 0) {
			if (CANCELAR_OFFSET_MTX3) {
				System.out.println(BLUE + "[MTX3] Cancelamento solicitado durante espera" + RESET);
				return false;
			}

			try {
				long tempoAguardar = Math.min(intervalo, tempoRestante);
				Thread.sleep(tempoAguardar);
				tempoRestante -= tempoAguardar;
			} catch (InterruptedException e) {
				// Se a thread for interrompida, verifica se foi por cancelamento
				if (CANCELAR_OFFSET_MTX3) {
					System.out.println(BLUE + "[MTX3] Thread interrompida por cancelamento" + RESET);
					return false;
				}
				// Se não foi cancelamento, re-lança a exceção
				throw new RuntimeException(e);
			}
		}

		return true;
	}

	// Método para chamar a checagem final
	private Map chamarBuscaAutomatica() {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String url = "http://localhost:8087/executar-manualmente";
			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				System.out.println(BLUE + "  Busca automática executada com sucesso" + RESET);
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

	// Extrai valor numérico de strings como "Modulator3.mtrMainCurr = 39"
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

	// Lê o offset atual do equipamento (MTX3)
	private int lerOffsetAtual(WebDriver driver, WebDriverWait wait) throws Exception {
		try {
			System.out.println(BLUE + "  Lendo offset atual do equipamento MTX3..." + RESET);

			WebElement internal3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Internal3___']/input")));
			internal3.click();
			Thread.sleep(300);

			WebElement offsetElement = encontrarElementoComTentativas(wait,
					"Internal3.power.offset",
					"Internal3_power_offset");

			if (offsetElement == null) {
				offsetElement = wait.until(ExpectedConditions.elementToBeClickable(
						By.xpath("//*[contains(@id, 'offset')]")));
			}

			String textoOffset = offsetElement.getText().trim();
			System.out.println(BLUE + "    Texto do offset atual MTX3: " + textoOffset + RESET);

			double offsetValor = extrairValorNumerico(textoOffset);
			System.out.println(BLUE + "    Offset atual MTX3 extraído: " + (int)offsetValor + RESET);

			return (int) offsetValor;

		} catch (Exception e) {
			System.err.println("Erro ao ler offset atual do MTX3: " + e.getMessage());
			return 0; // Retorna 0 como fallback
		}
	}

	// Altera o canal do equipamento MTX3
	private String mudarCanal(WebDriver driver, WebDriverWait wait, String canal) throws Exception {
		try {
			// Navegar para Modulator3
			WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Modulator3___']/input")));
			modulator3.click();
			Thread.sleep(300);

			// Verificar canal atual
			String canalAntes = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal atual MTX3: " + canalAntes + RESET);

			// Se já estiver no canal correto, retornar
			if (canalAntes.equals(canal)) {
				System.out.println(BLUE + "  MTX3 já está no canal " + canal + RESET);
				return canalAntes;
			}

			// Localizar elemento do canal
			WebElement canalElement = wait.until(ExpectedConditions.elementToBeClickable(
					By.id("Modulator3_Config_UpConverter_ChannelNumber")));

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
				throw new Exception("Não foi possível mudar o canal do MTX3");
			}

			Thread.sleep(2000);

			// Verificar se o canal foi alterado
			String canalDepois = verificarCanal(driver, wait);
			System.out.println(BLUE + "  Canal MTX3 configurado: " + canalDepois + RESET);

			int tentativas = 0;
			while (!canalDepois.equals(canal) && tentativas < 3) {
				System.out.println(BLUE + "  Canal MTX3 não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
				Thread.sleep(1000);
				canalDepois = verificarCanal(driver, wait);
				tentativas++;
			}

			if (!canalDepois.equals(canal)) {
				System.err.println("  AVISO: Canal MTX3 não mudou corretamente. Esperado: " + canal + ", Lido: " + canalDepois);
			}

			return canalAntes;

		} catch (Exception e) {
			System.err.println("  Erro ao mudar canal do MTX3: " + e.getMessage());
			throw e;
		}
	}

	// Desliga o MTX3 configurando RfMasterOn para 2
	private void desligarMTX3(WebDriver driver, WebDriverWait wait) throws Exception {
		// VERIFICA CANCELAMENTO ANTES DE COMEÇAR
		if (CANCELAR_OFFSET_MTX3) {
			System.out.println(BLUE + "[MTX3] Cancelamento solicitado antes de desligar MTX3" + RESET);
			throw new InterruptedException("Cancelamento solicitado");
		}

		try {
			WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='PowerAmplifier3___']/input")));
			powerAmplifier3.click();

			// VERIFICA CANCELAMENTO
			if (CANCELAR_OFFSET_MTX3 || Thread.currentThread().isInterrupted()) {
				System.out.println(BLUE + "[MTX3] Cancelamento durante clique" + RESET);
				throw new InterruptedException("Cancelamento durante operação");
			}

			Thread.sleep(300);

			WebElement rfMasterOn = encontrarElementoComTentativas(wait,
					"PowerAmplifier3.Config.RfMasterOn",
					"PowerAmplifier3_Config_RfMasterOn");

			if (rfMasterOn == null) {
				rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
						By.xpath("//*[contains(@id, 'RfMasterOn')]")));
			}

			// VERIFICA CANCELAMENTO ANTES DE CONFIGURAR
			if (CANCELAR_OFFSET_MTX3 || Thread.currentThread().isInterrupted()) {
				System.out.println(BLUE + "[MTX3] Cancelamento antes de configurar RfMasterOn" + RESET);
				throw new InterruptedException("Cancelamento solicitado");
			}

			if (!configurarValor(driver, rfMasterOn, "2")) {
				throw new Exception("Falha ao desligar RfMasterOn do MTX3");
			}

			System.out.println(BLUE + "  MTX3 desligado (RfMasterOn = 2)" + RESET);

			// VERIFICA CANCELAMENTO APÓS OPERAÇÃO
			if (CANCELAR_OFFSET_MTX3 || Thread.currentThread().isInterrupted()) {
				System.out.println(BLUE + "[MTX3] Cancelamento após desligar MTX3" + RESET);
				throw new InterruptedException("Cancelamento solicitado");
			}

		} catch (WebDriverException e) {
			// Se houver erro de comunicação com o browser (provavelmente porque foi fechado)
			if (e.getMessage().contains("Error communicating with the remote browser") ||
					e.getMessage().contains("It may have died")) {
				System.out.println(BLUE + "[MTX3] Browser foi fechado durante operação" + RESET);
				throw new InterruptedException("Browser fechado durante cancelamento");
			}
			throw e;
		}
	}

	// Liga o MTX3 configurando RfMasterOn para 1
	private void ligarMTX3(WebDriver driver, WebDriverWait wait) throws Exception {
		// Navegar para PowerAmplifier3
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		// Encontrar elemento RfMasterOn
		WebElement rfMasterOn = encontrarElementoComTentativas(wait,
				"PowerAmplifier3.Config.RfMasterOn",
				"PowerAmplifier3_Config_RfMasterOn");

		if (rfMasterOn == null) {
			rfMasterOn = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(@id, 'RfMasterOn')]")));
		}

		// Configurar para 1 (ligado)
		if (!configurarValor(driver, rfMasterOn, "1")) {
			throw new Exception("Falha ao ligar RfMasterOn do MTX3");
		}

		System.out.println(BLUE + "  MTX3 ligado (RfMasterOn = 1)" + RESET);
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

	// Configura threshold de proteção do MTX3
	private void configurarThershold(WebDriver driver, WebDriverWait wait, String valorThershold) throws Exception {
		// Navegar para PowerAmplifier3
		WebElement internal3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		internal3.click();
		Thread.sleep(300);

		// Encontrar elemento ForwardHigh (threshold)
		WebElement offset = encontrarElementoComTentativas(wait,
				"PowerAmplifier3.Config.Threshold.ForwardHigh",
				"PowerAmplifier3_Config_Threshold_ForwardHigh");

		if (offset == null) {
			offset = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(@id, 'ForwardHigh ')]")));
		}

		if (!configurarValor(driver, offset, valorThershold)) {
			throw new Exception("Falha ao configurar thershold do MTX3 para " + valorThershold);
		}

		System.out.println(BLUE + "  Thershold MTX3 configurado: " + valorThershold + RESET);
		Thread.sleep(300);
	}

	// Configura offset de potência do MTX3
	private void configurarOffset(WebDriver driver, WebDriverWait wait, String valorOffset) throws Exception {
		// Navegar para Internal3
		WebElement internal3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='Internal3___']/input")));
		internal3.click();
		Thread.sleep(300);

		// Encontrar elemento de offset
		WebElement offset = encontrarElementoComTentativas(wait,
				"Internal3.power.offset",
				"Internal3_power_offset");

		if (offset == null) {
			offset = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(@id, 'offset')]")));
		}

		if (!configurarValor(driver, offset, valorOffset)) {
			throw new Exception("Falha ao configurar offset do MTX3 para " + valorOffset);
		}

		System.out.println(BLUE + "  Offset MTX3 configurado: " + valorOffset + RESET);
		Thread.sleep(300);
	}

	// Configura potência de saída do MTX3
	private void configurarPotencia(WebDriver driver, WebDriverWait wait, String potencia) throws Exception {
		// Navegar para PowerAmplifier3
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		// Encontrar elemento OutputPower
		WebElement outputPower = encontrarElementoComTentativas(wait,
				"PowerAmplifier3.Config.OutputPower",
				"PowerAmplifier3_Config_OutputPower");

		if (outputPower == null) {
			outputPower = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//*[contains(text(), 'OutputPower')]")));
		}

		if (!configurarValor(driver, outputPower, potencia)) {
			throw new Exception("Falha ao configurar potência do MTX3");
		}

		System.out.println(BLUE + "  Potência MTX3 configurada: " + potencia + RESET);
		Thread.sleep(300);
	}

	// Método genérico para configurar valores em campos de entrada do MTX3
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
			System.err.println("Erro ao configurar valor no MTX3: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	// Lê potência atual do equipamento MTX3
	private String verificarPotencia(WebDriver driver, WebDriverWait wait) throws Exception {
		// Navegar para PowerAmplifier3
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		// Localizar elemento de potência
		WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("PowerAmplifier3_Status_ForwardPower")));

		String textoCompleto = potenciaElement.getText().trim();
		double potenciaValor = extrairValorNumerico(textoCompleto);

		return String.valueOf(potenciaValor);
	}

	// Lê corrente atual do equipamento MTX3
	private String verificarCorrente(WebDriver driver, WebDriverWait wait) throws Exception {
		// Navegar para Modulator3
		WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='Modulator3___']/input")));
		modulator3.click();
		Thread.sleep(300);

		// Localizar elemento de corrente
		WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("Modulator3_mtrMainCurr")));

		String textoCompleto = correnteElement.getText().trim();

		System.out.println(BLUE + "    Texto completo da corrente MTX3: " + textoCompleto + RESET);

		double correnteValor = extrairValorNumerico(textoCompleto);

		System.out.println(BLUE + "    Corrente MTX3 extraída: " + correnteValor + " A" + RESET);

		return String.valueOf(correnteValor);
	}

	// Lê canal atual do equipamento MTX3
	private String verificarCanal(WebDriver driver, WebDriverWait wait) throws Exception {
		try {
			// Navegar para Modulator3
			WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Modulator3___']/input")));
			modulator3.click();
			Thread.sleep(300);

			// Localizar elemento do canal
			WebElement canalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator3_Config_UpConverter_ChannelNumber")));
			Thread.sleep(300);

			// Tentar diferentes estratégias para obter o valor
			String canalTexto = canalElement.getText().trim();
			System.out.println(BLUE + "    Texto do elemento canal MTX3: " + canalTexto + RESET);

			if (canalTexto == null || canalTexto.isEmpty() || canalTexto.contains("Modulator3.Config.UpConverter.ChannelNumber")) {
				canalTexto = canalElement.getAttribute("value");
				System.out.println(BLUE + "    Value attribute do canal MTX3: " + canalTexto + RESET);
			}

			if (canalTexto == null || canalTexto.isEmpty()) {
				canalTexto = canalElement.getAttribute("innerText");
				System.out.println(BLUE + "    InnerText do canal MTX3: " + canalTexto + RESET);
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
					System.out.println(BLUE + "    Canal MTX3 extraído: " + numeros + RESET);
					return numeros;
				}
			}

			System.out.println(BLUE + "    Não conseguiu extrair canal MTX3, retornando N/A" + RESET);
			return "N/A";

		} catch (Exception e) {
			System.err.println("    Erro ao verificar canal MTX3: " + e.getMessage());
			return "N/A";
		}
	}

	// Verifica ambos os valores para detectar equipamento desligado
	private String verificarPotenciaEcorrente(WebDriver driver, WebDriverWait wait) throws Exception {
		// Verificar potência
		WebElement powerAmplifier3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='PowerAmplifier3___']/input")));
		powerAmplifier3.click();
		Thread.sleep(300);

		WebElement potenciaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("PowerAmplifier3_Status_ForwardPower")));

		String textoPotencia = potenciaElement.getText().trim();
		double potenciaValor = extrairValorNumerico(textoPotencia);

		System.out.println(BLUE + "    Potência MTX3 extraída: " + potenciaValor + " W" + RESET);

		// Verificar corrente
		WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//label[@link='Modulator3___']/input")));
		modulator3.click();
		Thread.sleep(300);

		WebElement correnteElement = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("Modulator3_mtrMainCurr")));

		String textoCorrente = correnteElement.getText().trim();
		System.out.println(BLUE + "    Texto completo da corrente MTX3: " + textoCorrente + RESET);

		double correnteValor = extrairValorNumerico(textoCorrente);
		System.out.println(BLUE + "    Corrente MTX3 extraída: " + correnteValor + " A" + RESET);

		// Detectar equipamento desligado
		if (correnteValor == 0 && potenciaValor == 0) {
			throw new Exception("EQUIPAMENTO MTX3 DESLIGADO - Corrente: " + correnteValor +
					" A, Potência: " + potenciaValor + " W");
		}

		if (correnteValor == 0) {
			System.out.println(BLUE + "    Corrente MTX3 zero, retornando valor da potência" + RESET);
			return "Potência MTX3: " + potenciaValor + " W";
		}

		return "Potência MTX3: " + potenciaValor + " W, Corrente MTX3: " + correnteValor + " A";
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
		String filePath = System.getProperty("user.dir") + "/logs_rotina_completa_mtx3.txt";
		try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
			writer.write(LocalDateTime.now() +
					" | MTX3 | Canal: " + canal +
					" | Antes: " + canalAntes +
					" | Depois: " + canalDepois +
					" | Potência: " + potencia +
					" | Corrente: " + corrente +
					" | Offset: " + offset +
					" | Iterações: " + resultadoAjuste.get("iteracoes") +
					" | Status: " + resultadoAjuste.get("status") + "\n");
			System.out.println(BLUE + "  Log MTX3 salvo em: " + filePath + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao salvar log MTX3: " + e.getMessage());
		}
	}

	// Endpoint para configurar offset manualmente no MTX3
	@PostMapping("/configurar-offset-mtx3")
	public ResponseEntity<Map<String, Object>> configurarOffsetMtx3(@RequestParam String offset) {
		Map<String, Object> resposta = new HashMap<>();
		WebDriver driver = null;

		System.out.println(BLUE + "=== CONFIGURANDO OFFSET PARA MTX3 ===" + RESET);
		System.out.println(BLUE + "Valor solicitado: " + offset + RESET);

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
			System.out.println(BLUE + "Acessando página de debug..." + RESET);
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
			System.out.println(BLUE + "Login realizado" + RESET);
			Thread.sleep(1500);

			// Navegar para offset
			System.out.println(BLUE + "Navegando para Internal3..." + RESET);
			WebElement internal3 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Internal3___']/input")));
			internal3.click();
			Thread.sleep(500);

			System.out.println(BLUE + "Procurando campo de offset..." + RESET);

			WebElement offsetElement = null;
			String[] offsetSelectors = {
					"//*[contains(@id, 'Internal3.power.offset')]",
					"//*[contains(@id, 'Internal3_power_offset')]",
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
						System.out.println(BLUE + "Offset encontrado com selector: " + selector + RESET);
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
					throw new Exception("Não foi possível encontrar o campo de offset do MTX3");
				}
			}

			System.out.println(BLUE + "Elemento encontrado - Tag: " + offsetElement.getTagName() + RESET);
			System.out.println(BLUE + "ID: " + offsetElement.getAttribute("id") + RESET);
			System.out.println(BLUE + "Nome: " + offsetElement.getAttribute("name") + RESET);
			System.out.println(BLUE + "Texto: " + offsetElement.getText() + RESET);

			System.out.println(BLUE + "Configurando offset para " + offset + "..." + RESET);

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

					System.out.println(BLUE + "Campo ativo encontrado para edição" + RESET);

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
				throw new Exception("Não foi possível configurar o offset do MTX3");
			}

			// Verificar se foi aplicado
			System.out.println(BLUE + "Verificando offset aplicado..." + RESET);
			Thread.sleep(2000);

			internal3.click();
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

				System.out.println(BLUE + "Offset lido após configuração: " + offsetAtual + RESET);

			} catch (Exception e) {
				System.err.println("Não foi possível ler offset atual do MTX3: " + e.getMessage());
			}

			// Preparar resposta
			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Offset MTX3 configurado com sucesso");
			resposta.put("offset_solicitado", offset);
			resposta.put("offset_aplicado", offsetAtual.isEmpty() ? offset : offsetAtual);
			resposta.put("hora_aplicacao", LocalDateTime.now().toString());

			System.out.println(BLUE + "=== OFFSET MTX3 CONFIGURADO COM SUCESSO ===" + RESET);

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			System.err.println("Erro ao configurar offset do MTX3: " + e.getMessage());
			e.printStackTrace();

			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro no MTX3: " + e.getMessage());
			resposta.put("hora_erro", LocalDateTime.now().toString());

			return ResponseEntity.status(500).body(resposta);

		} finally {
			if (driver != null) {
				try {
					driver.quit();
					System.out.println(BLUE + "Driver finalizado" + RESET);
				} catch (Exception e) {
					System.err.println("Erro ao finalizar driver: " + e.getMessage());
				}
			}
		}
	}

	// Endpoint para ajuste inicial do offset (redireciona para configurarOffsetMtx3)
	@PostMapping("/ajustar-offset-inicial-mtx3")
	public ResponseEntity<Map<String, Object>> ajustarOffsetInicialMtx3(@RequestParam String offset) {
		return configurarOffsetMtx3(offset);
	}
}