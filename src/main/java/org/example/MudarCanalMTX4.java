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
public class MudarCanalMTX4 {

	private static final String RESET = "\u001B[0m";
	private static final String MAGENTA = "\u001B[35m";


	private static final Map<String, String> VALORES_CANAL = new HashMap<>();
	static {
		VALORES_CANAL.put("14", "14");
		VALORES_CANAL.put("34", "34");
		VALORES_CANAL.put("51", "51");
	}

	private static final Map<String, String> VALORES_ESPERADOS_MTX4 = new HashMap<>();
	static {
		VALORES_ESPERADOS_MTX4.put("14", "414");
		VALORES_ESPERADOS_MTX4.put("34", "434");
		VALORES_ESPERADOS_MTX4.put("51", "451");
	}

	private static final Map<String, Object> TESTE_STATUS = new ConcurrentHashMap<>();
	private static ExecutorService executorService = null;

	@Value("${app.username}")
	private String username;

	@Value("${app.password}")
	private String password;

	@PostMapping("/mudar-canal-mtx4")
	public ResponseEntity<Map<String, Object>> mudarCanal4(@RequestParam String valorCanal) {
		Map<String, Object> resposta = new HashMap<>();

		if (!VALORES_CANAL.containsKey(valorCanal)) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Valor não programado. Valores permitidos: " +
					String.join(", ", VALORES_CANAL.keySet()));
			resposta.put("valores_programados", VALORES_CANAL);
			return ResponseEntity.badRequest().body(resposta);
		}

		try {
			Map<String, Object> resultado = configurarCanalMTX4(valorCanal);

			if ("erro".equals(resultado.get("status"))) {
				resposta.put("status", "erro");
				resposta.put("mensagem", resultado.get("mensagem"));
				return ResponseEntity.status(500).body(resposta);
			}

			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Canal do MTX4 configurado com sucesso");
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

	private Map<String, Object> configurarCanalMTX4(String canalEntrada) {
		String valorRealParaConfigurar = VALORES_CANAL.get(canalEntrada);
		String valorEsperadoNaLeitura = VALORES_ESPERADOS_MTX4.get(canalEntrada);
		String urlBase = "http://10.10.103.103/debug/";

		System.out.println(MAGENTA + "Configurando Canal do MTX4:" + RESET);
		System.out.println(MAGENTA + "  Entrada: " + canalEntrada + RESET);
		System.out.println(MAGENTA + "  Valor para configurar: " + valorRealParaConfigurar + RESET);
		System.out.println(MAGENTA + "  Valor esperado na leitura: " + valorEsperadoNaLeitura + RESET);

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
			System.out.println(MAGENTA + "Acessando: " + urlBase + RESET);

			WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
			campoUsuario.clear();
			campoUsuario.sendKeys(username);

			WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
			campoSenha.clear();
			campoSenha.sendKeys(password);

			WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
			botaoLogin.click();
			System.out.println(MAGENTA + "Login realizado" + RESET);

			Thread.sleep(1000);

			WebElement modulator4 = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//label[@link='Modulator4___']/input")));
			modulator4.click();
			Thread.sleep(300);
			System.out.println(MAGENTA + "Modulator4 selecionado" + RESET);

			WebElement getCanalAntes = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator4_Config_UpConverter_ChannelNumber")));

			String canalAntes = extrairValorNumerico(getCanalAntes.getText().trim());
			System.out.println(MAGENTA + "Canal ANTES: " + canalAntes + RESET);

			System.out.println(MAGENTA + "Localizando campo ChannelNumber..." + RESET);

			WebElement campoChannelNumber = null;
			String[] seletores = {
					"//*[@id='Modulator4_Config_UpConverter_ChannelNumber']",
					"//*[contains(@id, 'ChannelNumber')]",
					"//*[contains(text(), 'ChannelNumber')]",
					"//label[contains(text(), 'Channel')]/following-sibling::input"
			};

			for (String seletor : seletores) {
				try {
					campoChannelNumber = driver.findElement(By.xpath(seletor));
					if (campoChannelNumber != null && campoChannelNumber.isDisplayed()) {
						System.out.println(MAGENTA + "Campo encontrado com seletor: " + seletor + RESET);
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

			System.out.println(MAGENTA + "Configurando canal para: " + valorRealParaConfigurar + RESET);

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
				System.out.println(MAGENTA + "Estratégia 1 falhou: " + e.getMessage() + RESET);
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
					System.out.println(MAGENTA + "Estratégia 2 falhou: " + e.getMessage() + RESET);
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
					System.out.println(MAGENTA + "Estratégia 3 falhou: " + e.getMessage() + RESET);
				}
			}

			if (!sucesso) {
				resultado.put("status", "erro");
				resultado.put("mensagem", "Não foi possível configurar o canal");
				return resultado;
			}

			Thread.sleep(2000);

			modulator4.click();
			Thread.sleep(500);

			WebElement getCanalDepois = wait.until(ExpectedConditions.presenceOfElementLocated(
					By.id("Modulator4_Config_UpConverter_ChannelNumber")));

			String canalDepois = extrairValorNumerico(getCanalDepois.getText().trim());
			System.out.println(MAGENTA + "Canal DEPOIS: " + canalDepois + RESET);

			int tentativas = 0;
			while (!canalDepois.equals(valorEsperadoNaLeitura) && tentativas < 3) {
				System.out.println(MAGENTA + "Canal não mudou corretamente. Tentativa " + (tentativas + 1) + RESET);
				System.out.println(MAGENTA + "Esperado: " + valorEsperadoNaLeitura + ", Lido: " + canalDepois + RESET);

				Thread.sleep(1000);

				modulator4.click();
				Thread.sleep(500);
				getCanalDepois = wait.until(ExpectedConditions.presenceOfElementLocated(
						By.id("Modulator4_Config_UpConverter_ChannelNumber")));
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
				System.out.println(MAGENTA + "✓ Canal configurado corretamente!" + RESET);
				System.out.println(MAGENTA + "  Configuramos: " + valorRealParaConfigurar + RESET);
				System.out.println(MAGENTA + "  Leitura esperada: " + valorEsperadoNaLeitura + RESET);
				System.out.println(MAGENTA + "  Leitura obtida: " + canalDepois + RESET);
			}

			resultado.put("status", "sucesso");
			resultado.put("canal_antes", canalAntes);
			resultado.put("canal_depois", canalDepois);
			resultado.put("valor_configurado", canalEntrada);
			resultado.put("valor_real_configurado", valorRealParaConfigurar);
			resultado.put("canal_esperado_leitura", valorEsperadoNaLeitura);
			resultado.put("configuracao_correta", configuracaoCorreta);

			String filePath = System.getProperty("user.dir") + "/canal_mtx4.txt";
			try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
				writer.write(LocalDateTime.now() +
						" | Entrada: " + canalEntrada +
						" | Configurado: " + valorRealParaConfigurar +
						" | Esperado: " + valorEsperadoNaLeitura +
						" | Antes: " + canalAntes +
						" | Depois: " + canalDepois +
						" | Correto: " + (configuracaoCorreta ? "SIM" : "NÃO") + "\n");
				System.out.println(MAGENTA + "Log salvo: " + filePath + RESET);
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
				System.out.println(MAGENTA + "Driver finalizado" + RESET);
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

	@PostMapping("/testar-todos-canais4")
	public ResponseEntity<Map<String, Object>> testarTodasCanais4() {
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
				System.out.println(MAGENTA + "=== INICIANDO LOOP CÍCLICO DE CANAIS MTX4 ===" + RESET);
				System.out.println(MAGENTA + "Data/Hora: " + LocalDateTime.now() + RESET);
				System.out.println(MAGENTA + "Ordem: " + ordemCanais + RESET);
				System.out.println(MAGENTA + "NOTA: MTX4 adiciona prefixo '4' aos canais" + RESET);
				System.out.println(MAGENTA + "Ex: Configura '14' → Lê '414'" + RESET);

				int ciclo = 1;

				while (!Thread.currentThread().isInterrupted()) {
					System.out.println(MAGENTA + "\n" + "=".repeat(60) + RESET);
					System.out.println(MAGENTA + "CICLO " + ciclo + RESET);
					System.out.println(MAGENTA + "=".repeat(60) + RESET);

					for (int i = 0; i < ordemCanais.size(); i++) {
						if (Thread.currentThread().isInterrupted()) {
							System.out.println(MAGENTA + "Loop interrompido por cancelamento" + RESET);
							return;
						}

						String valorCanal = ordemCanais.get(i);
						String valorParaConfigurar = VALORES_CANAL.get(valorCanal);
						String valorEsperadoLeitura = VALORES_ESPERADOS_MTX4.get(valorCanal);
						int posicao = i + 1;

						TESTE_STATUS.put("ciclo_atual", ciclo);
						TESTE_STATUS.put("posicao_ciclo", posicao + "/" + ordemCanais.size());
						TESTE_STATUS.put("valor_atual", valorCanal);
						TESTE_STATUS.put("valor_configurar", valorParaConfigurar);
						TESTE_STATUS.put("valor_esperado_leitura", valorEsperadoLeitura);
						TESTE_STATUS.put("inicio_canal", LocalDateTime.now().toString());

						System.out.println(MAGENTA + "\n" + "-".repeat(50) + RESET);
						System.out.println(MAGENTA + "CICLO " + ciclo + " - CANAL " + posicao + "/" + ordemCanais.size() + RESET);
						System.out.println(MAGENTA + "Configurar: " + valorParaConfigurar + RESET);
						System.out.println(MAGENTA + "Esperar ler: " + valorEsperadoLeitura + RESET);
						System.out.println(MAGENTA + "-".repeat(50) + RESET);

						try {
							Map<String, Object> resultado = configurarCanalMTX4(valorCanal);

							if ("sucesso".equals(resultado.get("status"))) {
								System.out.println(MAGENTA + "Configuração realizada" + RESET);
								System.out.println(MAGENTA + "Correto: " + resultado.get("configuracao_correta") + RESET);
							} else {
								System.out.println(MAGENTA + "Falha: " + resultado.get("mensagem") + RESET);
							}

						} catch (Exception e) {
							System.err.println("ERRO: " + e.getMessage());
						}

						System.out.println(MAGENTA + "\nAguardando 1 minuto..." + RESET);

						for (int segundo = 1; segundo <= 6; segundo++) {
							if (Thread.currentThread().isInterrupted()) {
								System.out.println(MAGENTA + "Aguardar interrompido" + RESET);
								return;
							}

							TESTE_STATUS.put("tempo_restante_segundos", (6 - segundo) * 10);
							System.out.println(MAGENTA + "Aguardando " + ((6 - segundo) * 10) + " segundos restantes" + RESET);

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
		resposta.put("nota", "MTX4 adiciona prefixo '4' (ex: 14 → 414)");
		resposta.put("valores_configurar", VALORES_CANAL);
		resposta.put("valores_esperados_leitura", VALORES_ESPERADOS_MTX4);
		resposta.put("data_hora_inicio", LocalDateTime.now().toString());

		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/cancelar-loop-canais4")
	public ResponseEntity<Map<String, Object>> cancelarLoopCanais4() {
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

	@GetMapping("/status-loop-canais4")
	public ResponseEntity<Map<String, Object>> getStatusLoopCanais4() {
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

	@GetMapping("/valores-canal4")
	public ResponseEntity<Map<String, Object>> getValoresCanal4() {
		Map<String, Object> resposta = new HashMap<>();
		resposta.put("status", "sucesso");
		resposta.put("valores_configurar", VALORES_CANAL);
		resposta.put("valores_esperados_leitura", VALORES_ESPERADOS_MTX4);
		resposta.put("nota", "MTX4 adiciona prefixo '4' aos canais configurados");

		return ResponseEntity.ok(resposta);
	}
}