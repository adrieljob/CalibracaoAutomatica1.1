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

	private static final Map<String, String> VALORES_CANAL = new HashMap<>();
	static {
		VALORES_CANAL.put("14", "14");
		VALORES_CANAL.put("34", "34");
		VALORES_CANAL.put("51", "51");
	}

	private static final Map<String, String> VALORES_ESPERADOS_MTX1 = new HashMap<>();
	static {
		VALORES_ESPERADOS_MTX1.put("14", "114");
		VALORES_ESPERADOS_MTX1.put("34", "134");
		VALORES_ESPERADOS_MTX1.put("51", "151");
	}

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

			if ("erro".equals(resultado.get("status"))) {
				resposta.put("status", "erro");
				resposta.put("mensagem", resultado.get("mensagem"));
				return ResponseEntity.status(500).body(resposta);
			}

			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Canal do MTX1 configurado com sucesso");
			resposta.put("valor_configurado", valorCanal);
			resposta.put("valor_real_configurado", resultado.get("valor_real_configurado"));
			resposta.put("canal_esperado_leitura", resultado.get("canal_esperado_leitura"));
			resposta.put("canal_antes", resultado.get("canal_antes"));
			resposta.put("canal_depois", resultado.get("canal_depois"));
			resposta.put("configuracao_correta", resultado.get("configuracao_correta"));

			return ResponseEntity.ok(resposta);
		} catch (Exception e) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao configurar canal: " + e.getMessage());
			resposta.put("valor_configurado", valorCanal);

			return ResponseEntity.status(500).body(resposta);
		}
	}

	private Map<String, Object> configurarCanalMTX1(String canalEntrada) {
		String valorRealParaConfigurar = VALORES_CANAL.get(canalEntrada);
		String valorEsperadoNaLeitura = VALORES_ESPERADOS_MTX1.get(canalEntrada);
		String urlBase = "http://10.10.103.103/debug/";

		System.out.println(GREEN + "Configurando Canal do MTX1:" + RESET);
		System.out.println(GREEN + "  Entrada: " + canalEntrada + RESET);
		System.out.println(GREEN + "  Valor para configurar: " + valorRealParaConfigurar + RESET);
		System.out.println(GREEN + "  Valor esperado na leitura: " + valorEsperadoNaLeitura + RESET);

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

			WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
			campoUsuario.clear();
			campoUsuario.sendKeys(username);

			WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
			campoSenha.clear();
			campoSenha.sendKeys(password);

			WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
			botaoLogin.click();
			System.out.println(GREEN + "Login realizado" + RESET);

			Thread.sleep(1000);

			WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Modulator1___']/input")));
			modulator1.click();
			Thread.sleep(300);
			System.out.println(GREEN + "Modulator1 selecionado" + RESET);

			WebElement getCanalAntes = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator1_Config_UpConverter_ChannelNumber")));

			String canalAntes = extrairValorNumerico(getCanalAntes.getText().trim());
			System.out.println(GREEN + "Canal ANTES: " + canalAntes + RESET);

			System.out.println(GREEN + "Localizando campo ChannelNumber..." + RESET);

			WebElement campoChannelNumber = null;
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

			System.out.println(GREEN + "Configurando canal para: " + valorRealParaConfigurar + RESET);

			boolean sucesso = false;

			try {
				new org.openqa.selenium.interactions.Actions(driver)
						.doubleClick(campoChannelNumber)
						.perform();
				Thread.sleep(300);

				WebElement activeElement = driver.switchTo().activeElement();
				if (activeElement.getTagName().equals("input")) {
					activeElement.clear();
					Thread.sleep(300);
					activeElement.sendKeys(valorRealParaConfigurar);
					Thread.sleep(300);
					activeElement.sendKeys(Keys.ENTER);
					sucesso = true;
				}
			} catch (Exception e) {
				System.out.println(GREEN + "Estratégia 1 falhou: " + e.getMessage() + RESET);
			}

			if (!sucesso) {
				try {
					campoChannelNumber.click();
					Thread.sleep(300);
					campoChannelNumber.sendKeys(Keys.chord(Keys.CONTROL, "a"));
					campoChannelNumber.sendKeys(Keys.DELETE);
					campoChannelNumber.sendKeys(valorRealParaConfigurar);
					Thread.sleep(300);
					campoChannelNumber.sendKeys(Keys.ENTER);
					sucesso = true;
				} catch (Exception e) {
					System.out.println(GREEN + "Estratégia 2 falhou: " + e.getMessage() + RESET);
				}
			}

			if (!sucesso) {
				try {
					JavascriptExecutor js = (JavascriptExecutor) driver;
					js.executeScript("arguments[0].value = arguments[1];", campoChannelNumber, valorRealParaConfigurar);
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

			Thread.sleep(2000);

			modulator1.click();
			Thread.sleep(500);

			WebElement getCanalDepois = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator1_Config_UpConverter_ChannelNumber")));

			String canalDepois = extrairValorNumerico(getCanalDepois.getText().trim());
			System.out.println(GREEN + "Canal DEPOIS: " + canalDepois + RESET);

			int tentativas = 0;
			while (!canalDepois.equals(valorEsperadoNaLeitura) && tentativas < 3) {
				System.out.println(GREEN + "Canal não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
				System.out.println(GREEN + "Esperado: " + valorEsperadoNaLeitura + ", Lido: " + canalDepois + RESET);

				Thread.sleep(1000);

				modulator1.click();
				Thread.sleep(500);
				getCanalDepois = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.id("Modulator1_Config_UpConverter_ChannelNumber")));
				canalDepois = extrairValorNumerico(getCanalDepois.getText().trim());
				tentativas++;
			}

			boolean configuracaoCorreta = canalDepois.equals(valorEsperadoNaLeitura);

			if (!configuracaoCorreta) {
				System.err.println("AVISO: Canal não mudou corretamente.");
				System.err.println("  Configuramos: " + valorRealParaConfigurar);
				System.err.println("  Esperávamos ler: " + valorEsperadoNaLeitura);
				System.err.println("  Lemos: " + canalDepois);
			} else {
				System.out.println(GREEN + "✓ Canal configurado corretamente!" + RESET);
				System.out.println(GREEN + "  Configuramos: " + valorRealParaConfigurar + RESET);
				System.out.println(GREEN + "  Leitura esperada: " + valorEsperadoNaLeitura + RESET);
				System.out.println(GREEN + "  Leitura obtida: " + canalDepois + RESET);
			}

			resultado.put("status", "sucesso");
			resultado.put("canal_antes", canalAntes);
			resultado.put("canal_depois", canalDepois);
			resultado.put("valor_configurado", canalEntrada);
			resultado.put("valor_real_configurado", valorRealParaConfigurar);
			resultado.put("canal_esperado_leitura", valorEsperadoNaLeitura);
			resultado.put("configuracao_correta", configuracaoCorreta);

			String filePath = System.getProperty("user.dir") + "/canal_mtx1.txt";
			try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
				writer.write(LocalDateTime.now() +
						" | Entrada: " + canalEntrada +
						" | Configurado: " + valorRealParaConfigurar +
						" | Esperado: " + valorEsperadoNaLeitura +
						" | Antes: " + canalAntes +
						" | Depois: " + canalDepois +
						" | Correto: " + (configuracaoCorreta ? "SIM" : "NÃO") + "\n");
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

	private String extrairValorNumerico(String texto) {
		if (texto == null || texto.trim().isEmpty()) {
			return "0";
		}

		try {
			String numeros = texto.replaceAll("[^0-9]", "").trim();
			return numeros.isEmpty() ? "0" : numeros;
		} catch (Exception e) {
			return "0";
		}
	}

	@PostMapping("/testar-todos-canais1")
	public ResponseEntity<Map<String, Object>> testarTodasCanais1() {
		Map<String, Object> resposta = new HashMap<>();

		if (executorService != null && !executorService.isShutdown()) {
			resposta.put("status", "ja_em_execucao");
			resposta.put("mensagem", "Loop cíclico já está em execução");
			resposta.put("ultimo_canal", TESTE_STATUS.get("canal_atual"));
			resposta.put("ciclo_atual", TESTE_STATUS.get("ciclo_atual"));
			return ResponseEntity.ok(resposta);
		}

		TESTE_STATUS.clear();
		TESTE_STATUS.put("status_geral", "executando");
		TESTE_STATUS.put("inicio", LocalDateTime.now().toString());
		TESTE_STATUS.put("ciclo_atual", 1);
		TESTE_STATUS.put("modo", "loop_ciclico_1min");

		List<String> ordemCanais = Arrays.asList("14", "34", "51");
		TESTE_STATUS.put("ordem_canais", ordemCanais);

		executorService = Executors.newSingleThreadExecutor();

		executorService.submit(() -> {
			try {
				System.out.println(GREEN + "=== INICIANDO LOOP CÍCLICO DE CANAIS MTX1 ===" + RESET);
				System.out.println(GREEN + "Data/Hora: " + LocalDateTime.now() + RESET);
				System.out.println(GREEN + "Ordem: " + ordemCanais + RESET);
				System.out.println(GREEN + "NOTA: MTX1 adiciona prefixo '1' aos canais" + RESET);
				System.out.println(GREEN + "Ex: Configura '14' → Lê '114'" + RESET);

				int ciclo = 1;

				while (!Thread.currentThread().isInterrupted()) {
					System.out.println(GREEN + "\n" + "=".repeat(60) + RESET);
					System.out.println(GREEN + "CICLO " + ciclo + RESET);
					System.out.println(GREEN + "=".repeat(60) + RESET);

					for (int i = 0; i < ordemCanais.size(); i++) {
						if (Thread.currentThread().isInterrupted()) {
							System.out.println(GREEN + "Loop interrompido por cancelamento" + RESET);
							return;
						}

						String valorCanal = ordemCanais.get(i);
						String valorParaConfigurar = VALORES_CANAL.get(valorCanal);
						String valorEsperadoLeitura = VALORES_ESPERADOS_MTX1.get(valorCanal);
						int posicao = i + 1;

						TESTE_STATUS.put("ciclo_atual", ciclo);
						TESTE_STATUS.put("posicao_ciclo", posicao + "/" + ordemCanais.size());
						TESTE_STATUS.put("valor_atual", valorCanal);
						TESTE_STATUS.put("valor_configurar", valorParaConfigurar);
						TESTE_STATUS.put("valor_esperado_leitura", valorEsperadoLeitura);
						TESTE_STATUS.put("inicio_canal", LocalDateTime.now().toString());

						System.out.println(GREEN + "\n" + "-".repeat(50) + RESET);
						System.out.println(GREEN + "CICLO " + ciclo + " - CANAL " + posicao + "/" + ordemCanais.size() + RESET);
						System.out.println(GREEN + "Configurar: " + valorParaConfigurar + RESET);
						System.out.println(GREEN + "Esperar ler: " + valorEsperadoLeitura + RESET);
						System.out.println(GREEN + "-".repeat(50) + RESET);

						try {
							Map<String, Object> resultado = configurarCanalMTX1(valorCanal);

							if ("sucesso".equals(resultado.get("status"))) {
								System.out.println(GREEN + "Configuração realizada" + RESET);
								System.out.println(GREEN + "Correto: " + resultado.get("configuracao_correta") + RESET);
							} else {
								System.out.println(GREEN + "Falha: " + resultado.get("mensagem") + RESET);
							}

						} catch (Exception e) {
							System.err.println("ERRO: " + e.getMessage());
						}

						System.out.println(GREEN + "\nAguardando 1 minuto..." + RESET);

						for (int segundo = 1; segundo <= 6; segundo++) {
							if (Thread.currentThread().isInterrupted()) {
								System.out.println(GREEN + "Aguardar interrompido" + RESET);
								return;
							}

							TESTE_STATUS.put("tempo_restante_segundos", (6 - segundo) * 10);
							System.out.println(GREEN + "Aguardando " + ((6 - segundo) * 10) + " segundos restantes" + RESET);

							try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								return;
							}
						}
					}

					ciclo++;
					TESTE_STATUS.put("ciclo_atual", ciclo);
					Thread.sleep(10000);
				}

			} catch (Exception e) {
				System.err.println("ERRO FATAL: " + e.getMessage());
				TESTE_STATUS.put("status_geral", "erro_fatal");
				TESTE_STATUS.put("erro", e.getMessage());
				TESTE_STATUS.put("fim", LocalDateTime.now().toString());
			} finally {
				if (executorService != null) {
					executorService.shutdown();
				}
				TESTE_STATUS.put("status_geral", "finalizado");
				TESTE_STATUS.put("fim", LocalDateTime.now().toString());
			}
		});

		resposta.put("status", "loop_iniciado");
		resposta.put("mensagem", "Loop cíclico de canais iniciado");
		resposta.put("nota", "MTX1 adiciona prefixo '1' (ex: 14 → 114)");
		resposta.put("valores_configurar", VALORES_CANAL);
		resposta.put("valores_esperados_leitura", VALORES_ESPERADOS_MTX1);
		resposta.put("data_hora_inicio", LocalDateTime.now().toString());

		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/cancelar-loop-canais1")
	public ResponseEntity<Map<String, Object>> cancelarLoopCanais1() {
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
			resposta.put("data_hora_cancelamento", LocalDateTime.now().toString());

			TESTE_STATUS.put("status_geral", "cancelado");
			TESTE_STATUS.put("fim", LocalDateTime.now().toString());

		} catch (InterruptedException e) {
			resposta.put("status", "erro_cancelamento");
			resposta.put("mensagem", "Erro ao cancelar: " + e.getMessage());
		}

		return ResponseEntity.ok(resposta);
	}

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

	@GetMapping("/valores-canal1")
	public ResponseEntity<Map<String, Object>> getValoresCanal1() {
		Map<String, Object> resposta = new HashMap<>();
		resposta.put("status", "sucesso");
		resposta.put("valores_configurar", VALORES_CANAL);
		resposta.put("valores_esperados_leitura", VALORES_ESPERADOS_MTX1);
		resposta.put("nota", "MTX1 adiciona prefixo '1' aos canais configurados");

		return ResponseEntity.ok(resposta);
	}
}