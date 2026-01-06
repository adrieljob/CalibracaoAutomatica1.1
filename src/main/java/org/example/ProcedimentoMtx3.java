package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ProcedimentoMtx3 {

	private static final String RESET = "\u001B[0m";
	private static final String BLUE = "\u001B[34m";
	private static boolean procedimentoAtivoMtx3 = false;
	private static LocalDateTime horaInicioProcedimentoMtx3 = null;
	private static Map<String, Object> statusProcedimentoMtx3 = new ConcurrentHashMap<>();

	@Autowired
	private RestTemplate restTemplate;

	@GetMapping("/status-procedimento-mtx3")
	public ResponseEntity<Map<String, Object>> getStatusProcedimentoMtx3() {
		Map<String, Object> resposta = new HashMap<>();

		resposta.put("ativo", procedimentoAtivoMtx3);
		resposta.put("hora_inicio", horaInicioProcedimentoMtx3 != null ? horaInicioProcedimentoMtx3.toString() : null);
		resposta.put("progresso", statusProcedimentoMtx3);
		resposta.put("ultima_atualizacao", LocalDateTime.now().toString());

		// Verificar timeout (2 horas para procedimento completo)
		if (procedimentoAtivoMtx3 && horaInicioProcedimentoMtx3 != null) {
			Duration duracao = Duration.between(horaInicioProcedimentoMtx3, LocalDateTime.now());
			if (duracao.toHours() > 2) {
				procedimentoAtivoMtx3 = false;
				resposta.put("ativo", false);
				resposta.put("mensagem", "Processo expirado");
			}
		}

		return ResponseEntity.ok(resposta);
	}

	// Endpoint para executar ajuste offset + linearização para todos os canais
	@PostMapping("/executar-procedimento-completo-mtx3")
	public ResponseEntity<Map<String, Object>> executarProcedimentoCompletomtx3() {
		Map<String, Object> respostaGeral = new HashMap<>();
		Map<String, Object> resultados = new HashMap<>();

		try {
			System.out.println(BLUE + "=== INICIANDO PROCEDIMENTO COMPLETO MTX3 ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			// Sequência de canais
			String[] canais = {"14", "34", "51"};

			for (int i = 0; i < canais.length; i++) {
				String canal = canais[i];

				System.out.println(BLUE + "\n" + "=".repeat(60) + RESET);
				System.out.println(BLUE + "PROCESSANDO CANAL: " + canal + RESET);
				System.out.println(BLUE + "=".repeat(60) + RESET);

				// ========== ETAPA 1: AJUSTE OFFSET ==========
				System.out.println(BLUE + "\n[ETAPA 1] EXECUTANDO AJUSTE OFFSET PARA CANAL " + canal + RESET);

				// Chama o endpoint de ajuste offset com o canal como parâmetro
				Map<String, Object> resultadoAjuste = chamarAjusteOffset(canal);
				resultados.put("canal_" + canal + "_ajuste_offset", resultadoAjuste);

				if (!"sucesso".equals(resultadoAjuste.get("status"))) {
					System.err.println("✗ Ajuste offset falhou para canal " + canal);
					resultados.put("canal_" + canal + "_status", "erro_ajuste_offset");
					// Decida se quer continuar com o próximo canal ou parar
					// throw new RuntimeException("Falha no ajuste offset canal " + canal);
				} else {
					System.out.println(BLUE + "Ajuste offset concluído para canal " + canal + RESET);
				}

				// Aguarda entre ajuste offset e linearização
				System.out.println(BLUE + "\nAguardando 30 segundos antes da linearização..." + RESET);
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

				// ========== ETAPA 2: LINEARIZAÇÃO ==========
				System.out.println(BLUE + "\n[ETAPA 2] EXECUTANDO LINEARIZAÇÃO PARA CANAL " + canal + RESET);

				// Chama o endpoint de linearização com o canal como parâmetro
				Map<String, Object> resultadoLinearizacao = chamarLinearizacao(canal);
				resultados.put("canal_" + canal + "_linearizacao", resultadoLinearizacao);

				String statusLinearizacao = (String) resultadoLinearizacao.get("status");
				if (!"sucesso".equals(statusLinearizacao) && !"parcial".equals(statusLinearizacao)) {
					System.err.println("✗ Linearização falhou para canal " + canal);
					resultados.put("canal_" + canal + "_status", "erro_linearizacao");
				} else {
					System.out.println(BLUE + "✓ Linearização concluída para canal " + canal + RESET);
					resultados.put("canal_" + canal + "_status", "sucesso");
				}

				// Aguarda entre linearização e checagem final
				System.out.println(BLUE + "\nAguardando 15 segundos antes da checagem final..." + RESET);
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

				// ========== ETAPA 3: CHECAGEM FINAL ==========
				System.out.println(BLUE + "\n[ETAPA 3] EXECUTANDO CHECAGEM FINAL PARA CANAL " + canal + RESET);
				Map<String, Object> resultadoChecagem = chamarChecagem();
				resultados.put("canal_" + canal + "_checagem_final", resultadoChecagem);

				// Verifica se a checagem foi bem-sucedida
				String statusChecagem = (String) resultadoChecagem.get("status");
				if ("sucesso".equals(statusChecagem)) {
					System.out.println(BLUE + "✓ Checagem final concluída para canal " + canal + RESET);
					// Atualiza o status geral do canal
					resultados.put("canal_" + canal + "_status", "sucesso_completo");
				} else {
					System.err.println("✗ Checagem final apresentou problemas para canal " + canal);
					resultados.put("canal_" + canal + "_status", "aviso_checagem");
				}

				// Aguarda entre canais (exceto o último)
				if (i < canais.length - 1) {
					System.out.println(BLUE + "\n" + "=".repeat(50) + RESET);
					System.out.println(BLUE + "AGUARDANDO 30 SEGUNDOS ANTES DO PRÓXIMO CANAL..." + RESET);
					System.out.println(BLUE + "=".repeat(50) + RESET);
					try {
						Thread.sleep(30000); // 30 segundos
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			// Executa uma checagem final geral após todos os canais
			System.out.println(BLUE + "\n" + "=".repeat(60) + RESET);
			System.out.println(BLUE + "EXECUTANDO CHECAGEM FINAL GERAL APÓS TODOS OS CANAIS" + RESET);
			System.out.println(BLUE + "=".repeat(60) + RESET);

			Map<String, Object> checagemFinalGeral = chamarChecagem();
			resultados.put("checagem_final_geral", checagemFinalGeral);

			// Prepara resposta final
			respostaGeral.put("status", "sucesso");
			respostaGeral.put("mensagem", "Procedimento completo executado para todos os canais");
			respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
			respostaGeral.put("hora_fim", LocalDateTime.now().toString());
			respostaGeral.put("resultados", resultados);
			respostaGeral.put("sequencia_canais", "14 → 34 → 51");
			respostaGeral.put("etapas_por_canal", "Ajuste Offset → Linearização → Checagem Final");

			System.out.println(BLUE + "\n=== PROCEDIMENTO COMPLETO FINALIZADO ===" + RESET);
			System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);

			return ResponseEntity.ok(respostaGeral);

		} catch (Exception e) {
			respostaGeral.put("status", "erro");
			respostaGeral.put("mensagem", "Erro no procedimento completo: " + e.getMessage());
			respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
			respostaGeral.put("resultados", resultados);

			System.err.println("Erro no procedimento completo: " + e.getMessage());
			e.printStackTrace();

			return ResponseEntity.status(500).body(respostaGeral);
		}
	}

	// Endpoint para executar apenas para um canal específico
	@PostMapping("/executar-procedimento-canal-mtx3")
	public ResponseEntity<Map<String, Object>> executarProcedimentoCanalmtx3(@RequestParam String canal) {
		Map<String, Object> resposta = new HashMap<>();

		// MARCA COMO ATIVO
		procedimentoAtivoMtx3 = true;
		horaInicioProcedimentoMtx3 = LocalDateTime.now();
		statusProcedimentoMtx3.clear();
		statusProcedimentoMtx3.put("canal", canal);
		statusProcedimentoMtx3.put("status", "executando");
		statusProcedimentoMtx3.put("etapa", "ajuste_offset");

		try {
			System.out.println(BLUE + "=== INICIANDO PROCEDIMENTO PARA CANAL: " + canal + " ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			// ========== ETAPA 1: AJUSTE OFFSET ==========
			System.out.println(BLUE + "\n[ETAPA 1] EXECUTANDO AJUSTE OFFSET" + RESET);
			Map<String, Object> resultadoAjuste = chamarAjusteOffset(canal);

			// Aguarda entre etapas
			System.out.println(BLUE + "\nAguardando 30 segundos antes da linearização..." + RESET);
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// ========== ETAPA 2: LINEARIZAÇÃO ==========
			System.out.println(BLUE + "\n[ETAPA 2] EXECUTANDO LINEARIZAÇÃO" + RESET);
			Map<String, Object> resultadoLinearizacao = chamarLinearizacao(canal);

			// Aguarda antes da checagem final
			System.out.println(BLUE + "\nAguardando 15 segundos antes da checagem final..." + RESET);
			try {
				Thread.sleep(15000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// ========== ETAPA 3: CHECAGEM FINAL ==========
			System.out.println(BLUE + "\n[ETAPA 3] EXECUTANDO CHECAGEM FINAL" + RESET);
			Map<String, Object> resultadoChecagem = chamarChecagem();

			// Prepara resposta ÚNICA com todas as etapas
			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Procedimento executado para canal " + canal);
			resposta.put("hora_inicio", horaInicioProcedimentoMtx3.toString());
			resposta.put("hora_fim", LocalDateTime.now().toString());
			resposta.put("canal", canal);
			resposta.put("ajuste_offset", resultadoAjuste);
			resposta.put("linearizacao", resultadoLinearizacao);
			resposta.put("checagem_final", resultadoChecagem); // Note: sem espaço no nome

			System.out.println(BLUE + "\n=== PROCEDIMENTO FINALIZADO PARA CANAL: " + canal + " ===" + RESET);
			System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro no procedimento para canal " + canal + ": " + e.getMessage());
			resposta.put("hora_inicio", horaInicioProcedimentoMtx3 != null ? horaInicioProcedimentoMtx3.toString() : LocalDateTime.now().toString());
			resposta.put("canal", canal);

			System.err.println("Erro no procedimento para canal " + canal + ": " + e.getMessage());
			e.printStackTrace();

			return ResponseEntity.status(500).body(resposta);
		} finally {
			procedimentoAtivoMtx3 = false;
			statusProcedimentoMtx3.put("status", "finalizado");
		}
	}

	// Método para chamar o ajuste offset
	private Map<String, Object> chamarAjusteOffset(String canal) {
		try {
			statusProcedimentoMtx3.put("etapa", "ajuste_offset");
			statusProcedimentoMtx3.put("canal", canal);
			try {
				System.out.println(BLUE + "  Chamando endpoint de ajuste offset para canal " + canal + RESET);

				// URL do endpoint existente do AjustarOffSetMtx3
				String url = "http://localhost:8087/executar-offset-canal-mtx3?canal=" + canal;
				// Faz a requisição POST
				ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

				Map<String, Object> resultado;
				if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
					resultado = response.getBody();
					System.out.println(BLUE + "  Resposta do ajuste offset: " + resultado.get("status") + RESET);
					return resultado;
				} else {
					throw new RuntimeException("Falha na comunicação com serviço de ajuste offset");
				}

			} catch (Exception e) {
				System.err.println("  Erro ao chamar ajuste offset: " + e.getMessage());
				Map<String, Object> erro = new HashMap<>();
				erro.put("status", "erro");
				erro.put("mensagem", "Falha ao chamar ajuste offset: " + e.getMessage());
				erro.put("canal", canal);
				return erro;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Método para chamar a linearização
	private Map<String, Object> chamarLinearizacao(String canal) {
		try {
			statusProcedimentoMtx3.put("etapa", "linearizacao");
			try {
				System.out.println(BLUE + "  Chamando endpoint de linearização para canal " + canal + RESET);

				// URL do endpoint existente do LinearizacaoMtx3
				String url = "http://localhost:8087/executar-linearizacao-canal-mtx3?canal=" + canal;

				// Faz a requisição POST
				ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

				Map<String, Object> resultado;
				if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
					resultado = response.getBody();
					System.out.println(BLUE + "  Resposta da linearização: " + resultado.get("status") + RESET);
					return resultado;
				} else {
					throw new RuntimeException("Falha na comunicação com serviço de linearização");
				}

			} catch (Exception e) {
				System.err.println("  Erro ao chamar linearização: " + e.getMessage());
				Map<String, Object> erro = new HashMap<>();
				erro.put("status", "erro");
				erro.put("mensagem", "Falha ao chamar linearização: " + e.getMessage());
				erro.put("canal", canal);
				return erro;
			}
		} finally {

		}
	}

	// Método para fazer uma checagem depois das mudanças
	private Map<String, Object> chamarChecagem() {
		try {
			statusProcedimentoMtx3.put("etapa", "checagem_final");

			System.out.println(BLUE + "  Chamando endpoint de checagem final" + RESET);

			// CORREÇÃO: URL correta
			String url = "http://localhost:8087/executar-manualmente";

			// Faz a requisição POST
			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			Map<String, Object> resultado;
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				resultado = response.getBody();
				System.out.println(BLUE + "  Resposta da checagem final: " + resultado.get("status") + RESET);

				// Adiciona timestamp à resposta
				resultado.put("timestamp_checagem", LocalDateTime.now().toString());
				return resultado;
			} else {
				throw new RuntimeException("Falha na comunicação com serviço de checagem final. Status: " + response.getStatusCode());
			}

		} catch (Exception e) {
			System.err.println("  Erro ao chamar checagem final: " + e.getMessage());
			Map<String, Object> erro = new HashMap<>();
			erro.put("status", "erro");
			erro.put("mensagem", "Falha ao chamar checagem final: " + e.getMessage());
			erro.put("timestamp", LocalDateTime.now().toString());
			return erro;
		}
	}

	// Endpoint para cancelar procedimento em andamento
	@PostMapping("/cancelar-procedimento-mtx3")
	public ResponseEntity<Map<String, Object>> cancelarProcedimentomtx3() {
		Map<String, Object> resposta = new HashMap<>();

		try {
			System.out.println(BLUE + "Solicitação de cancelamento de procedimento recebida"+ RESET);

			// Opcional: Chamar endpoints de cancelamento dos serviços
			try {
				restTemplate.postForEntity("http://localhost:8087/cancelar-linearizacao-mtx3", null, Map.class);
			} catch (Exception e) {
				System.err.println("Erro ao cancelar linearização: " + e.getMessage());
			}

			try {
				restTemplate.postForEntity("http://localhost:8087/cancelar-offset-mtx3", null, Map.class);
			} catch (Exception e) {
				System.err.println("Erro ao cancelar linearização: " + e.getMessage());
			}

			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Solicitação de cancelamento recebida");
			resposta.put("hora_cancelamento", LocalDateTime.now().toString());

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao processar cancelamento: " + e.getMessage());
			return ResponseEntity.status(500).body(resposta);
		}
	}
}