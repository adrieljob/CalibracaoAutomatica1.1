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
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class ProcedimentoMtx3 {

	private static final String RESET = "\u001B[0m";
	private static final String BLUE = "\u001B[34m";
	private static final String RED = "\u001B[31m";
	private static final String YELLOW = "\u001B[33m";

	// Controle simplificado com AtomicBoolean
	private static final AtomicBoolean CANCELAMENTO_SOLICITADO = new AtomicBoolean(false);
	private static final AtomicBoolean PROCEDIMENTO_ATIVO = new AtomicBoolean(false);
	private static LocalDateTime horaInicioProcedimentoMtx3 = null;
	private static final Map<String, Object> STATUS_PROCEDIMENTO_MTX3 = new ConcurrentHashMap<>();

	// Thread do procedimento atual
	private static Thread threadProcedimentoAtual = null;

	@Autowired
	private RestTemplate restTemplate;

	@GetMapping("/status-procedimento-mtx3")
	public ResponseEntity<Map<String, Object>> getStatusProcedimentoMtx3() {
		Map<String, Object> resposta = new HashMap<>();

		resposta.put("ativo", PROCEDIMENTO_ATIVO.get());
		resposta.put("cancelamento_solicitado", CANCELAMENTO_SOLICITADO.get());
		resposta.put("hora_inicio", horaInicioProcedimentoMtx3 != null ? horaInicioProcedimentoMtx3.toString() : null);
		resposta.put("progresso", STATUS_PROCEDIMENTO_MTX3);
		resposta.put("ultima_atualizacao", LocalDateTime.now().toString());
		resposta.put("thread_ativa", threadProcedimentoAtual != null && threadProcedimentoAtual.isAlive());

		// Verificar timeout (2 horas para procedimento completo)
		if (PROCEDIMENTO_ATIVO.get() && horaInicioProcedimentoMtx3 != null) {
			Duration duracao = Duration.between(horaInicioProcedimentoMtx3, LocalDateTime.now());
			if (duracao.toHours() > 2) {
				cancelarProcedimento("Processo expirado por timeout");
				resposta.put("ativo", false);
				resposta.put("mensagem", "Processo expirado");
			}
		}

		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/executar-procedimento-completo-mtx3")
	public ResponseEntity<Map<String, Object>> executarProcedimentoCompletomtx3() {
		Map<String, Object> respostaGeral = new HashMap<>();

		if (PROCEDIMENTO_ATIVO.get()) {
			respostaGeral.put("status", "erro");
			respostaGeral.put("mensagem", "Já existe um procedimento em andamento");
			return ResponseEntity.status(409).body(respostaGeral);
		}

		// Reset flags
		CANCELAMENTO_SOLICITADO.set(false);
		PROCEDIMENTO_ATIVO.set(true);
		horaInicioProcedimentoMtx3 = LocalDateTime.now();
		STATUS_PROCEDIMENTO_MTX3.clear();
		STATUS_PROCEDIMENTO_MTX3.put("status", "iniciando");
		STATUS_PROCEDIMENTO_MTX3.put("etapa", "preparacao");

		// Cria e inicia nova thread
		threadProcedimentoAtual = new Thread(() -> {
			try {
				executarProcedimentoCompleto();
			} catch (Exception e) {
				System.err.println(RED + "Erro na thread do procedimento: " + e.getMessage() + RESET);
			} finally {
				PROCEDIMENTO_ATIVO.set(false);
			}
		});

		threadProcedimentoAtual.setName("Procedimento-MTX3");
		threadProcedimentoAtual.start();

		respostaGeral.put("status", "sucesso");
		respostaGeral.put("mensagem", "Procedimento iniciado em background");
		respostaGeral.put("hora_inicio", horaInicioProcedimentoMtx3.toString());
		respostaGeral.put("pode_cancelar", true);
		respostaGeral.put("thread_id", threadProcedimentoAtual.getId());

		return ResponseEntity.ok(respostaGeral);
	}

	private void executarProcedimentoCompleto() {
		try {
			System.out.println(BLUE + "=== INICIANDO PROCEDIMENTO COMPLETO MTX3 ===" + RESET);
			System.out.println(BLUE + "Hora de início: " + LocalDateTime.now() + RESET);

			STATUS_PROCEDIMENTO_MTX3.put("status", "executando");

			String[] canais = {"14", "34", "51"};

			for (int i = 0; i < canais.length; i++) {
				// VERIFICA CANCELAMENTO A CADA ITERAÇÃO
				if (CANCELAMENTO_SOLICITADO.get()) {
					System.out.println(YELLOW + "\n[INTERROMPENDO] Cancelamento solicitado durante processamento" + RESET);
					throw new InterruptedException("Procedimento cancelado pelo usuário");
				}

				String canal = canais[i];

				System.out.println(BLUE + "\n" + "=".repeat(60) + RESET);
				System.out.println(BLUE + "PROCESSANDO CANAL: " + canal + RESET);
				System.out.println(BLUE + "=".repeat(60) + RESET);

				STATUS_PROCEDIMENTO_MTX3.put("canal_atual", canal);

				// ========== ETAPA 1: AJUSTE OFFSET ==========
				if (CANCELAMENTO_SOLICITADO.get()) {
					throw new InterruptedException("Cancelamento solicitado antes do ajuste offset");
				}

				System.out.println(BLUE + "\n[ETAPA 1] EXECUTANDO AJUSTE OFFSET PARA CANAL " + canal + RESET);
				STATUS_PROCEDIMENTO_MTX3.put("etapa", "ajuste_offset");

				// CHAMA AJUSTE OFFSET COM VERIFICAÇÃO DE CANCELAMENTO
				Map<String, Object> resultadoAjuste = chamarAjusteOffsetComCancelamento(canal);

				// VERIFICA CANCELAMENTO APÓS CHAMADA
				if (CANCELAMENTO_SOLICITADO.get()) {
					throw new InterruptedException("Cancelamento solicitado após ajuste offset");
				}

				// Aguarda 30 segundos com verificação de cancelamento
				System.out.println(BLUE + "\nAguardando 30 segundos antes da linearização..." + RESET);
				if (!aguardarComVerificacao(30000)) {
					throw new InterruptedException("Cancelamento durante espera após ajuste offset");
				}

				// ========== ETAPA 2: LINEARIZAÇÃO ==========
				if (CANCELAMENTO_SOLICITADO.get()) {
					throw new InterruptedException("Cancelamento solicitado antes da linearização");
				}

				System.out.println(BLUE + "\n[ETAPA 2] EXECUTANDO LINEARIZAÇÃO PARA CANAL " + canal + RESET);
				STATUS_PROCEDIMENTO_MTX3.put("etapa", "linearizacao");

				// CHAMA LINEARIZAÇÃO COM VERIFICAÇÃO DE CANCELAMENTO
				Map<String, Object> resultadoLinearizacao = chamarLinearizacaoComCancelamento(canal);

				if (CANCELAMENTO_SOLICITADO.get()) {
					throw new InterruptedException("Cancelamento solicitado após linearização");
				}

				// Aguarda 15 segundos com verificação de cancelamento
				System.out.println(BLUE + "\nAguardando 15 segundos antes da checagem final..." + RESET);
				if (!aguardarComVerificacao(15000)) {
					throw new InterruptedException("Cancelamento durante espera após linearização");
				}

				// ========== ETAPA 3: CHECAGEM FINAL ==========
				if (CANCELAMENTO_SOLICITADO.get()) {
					throw new InterruptedException("Cancelamento solicitado antes da checagem final");
				}

				System.out.println(BLUE + "\n[ETAPA 3] EXECUTANDO CHECAGEM FINAL PARA CANAL " + canal + RESET);
				STATUS_PROCEDIMENTO_MTX3.put("etapa", "checagem_final");

				Map<String, Object> resultadoChecagem = chamarChecagem();

				// Aguarda entre canais (exceto o último)
				if (i < canais.length - 1) {
					if (CANCELAMENTO_SOLICITADO.get()) {
						throw new InterruptedException("Cancelamento solicitado durante pausa entre canais");
					}

					System.out.println(BLUE + "\n" + "=".repeat(50) + RESET);
					System.out.println(BLUE + "AGUARDANDO 30 SEGUNDOS ANTES DO PRÓXIMO CANAL..." + RESET);
					System.out.println(BLUE + "=".repeat(50) + RESET);

					if (!aguardarComVerificacao(30000)) {
						throw new InterruptedException("Cancelamento durante espera entre canais");
					}
				}
			}

			// Se chegou aqui sem cancelamento, procedimento completo
			System.out.println(BLUE + "\n=== PROCEDIMENTO COMPLETO FINALIZADO COM SUCESSO ===" + RESET);
			System.out.println(BLUE + "Hora de fim: " + LocalDateTime.now() + RESET);

			STATUS_PROCEDIMENTO_MTX3.put("status", "finalizado_sucesso");
			STATUS_PROCEDIMENTO_MTX3.put("hora_fim", LocalDateTime.now().toString());

		} catch (InterruptedException e) {
			System.out.println(RED + "\n=== PROCEDIMENTO CANCELADO PELO USUÁRIO ===" + RESET);
			System.out.println(RED + "Hora de cancelamento: " + LocalDateTime.now() + RESET);

			STATUS_PROCEDIMENTO_MTX3.put("status", "cancelado");
			STATUS_PROCEDIMENTO_MTX3.put("mensagem", "Procedimento interrompido pelo usuário");
			STATUS_PROCEDIMENTO_MTX3.put("hora_cancelamento", LocalDateTime.now().toString());

			// Interrompe a thread atual
			Thread.currentThread().interrupt();

		} catch (Exception e) {
			System.err.println(RED + "\n=== ERRO NO PROCEDIMENTO ===" + RESET);
			System.err.println("Erro: " + e.getMessage());
			e.printStackTrace();

			STATUS_PROCEDIMENTO_MTX3.put("status", "erro");
			STATUS_PROCEDIMENTO_MTX3.put("mensagem", "Erro: " + e.getMessage());
			STATUS_PROCEDIMENTO_MTX3.put("hora_erro", LocalDateTime.now().toString());

		} finally {
			PROCEDIMENTO_ATIVO.set(false);
			STATUS_PROCEDIMENTO_MTX3.put("ativo", false);

			// Tenta cancelar procedimentos nos serviços externos
			cancelarProcedimentosExternos();

			// Limpa a referência da thread
			threadProcedimentoAtual = null;
		}
	}

	// Método auxiliar para aguardar com verificação de cancelamento
	private boolean aguardarComVerificacao(long millis) {
		long intervalo = 1000; // Verifica a cada 1 segundo
		long tempoRestante = millis;

		while (tempoRestante > 0) {
			if (CANCELAMENTO_SOLICITADO.get() || Thread.currentThread().isInterrupted()) {
				System.out.println(YELLOW + "[INFO] Cancelamento detectado durante espera" + RESET);
				return false;
			}

			try {
				long tempoAguardar = Math.min(intervalo, tempoRestante);
				Thread.sleep(tempoAguardar);
				tempoRestante -= tempoAguardar;
			} catch (InterruptedException e) {
				System.out.println(YELLOW + "[INFO] Thread interrompida durante espera" + RESET);
				Thread.currentThread().interrupt();
				return false;
			}
		}

		return true;
	}

	// Método para chamar ajuste offset com verificação de cancelamento
	private Map<String, Object> chamarAjusteOffsetComCancelamento(String canal) {
		if (CANCELAMENTO_SOLICITADO.get()) {
			Map<String, Object> cancelado = new HashMap<>();
			cancelado.put("status", "cancelado");
			cancelado.put("mensagem", "Operação cancelada antes da execução");
			return cancelado;
		}

		try {
			System.out.println(BLUE + "  Chamando endpoint de ajuste offset para canal " + canal + RESET);

			String url = "http://localhost:8087/executar-offset-canal-mtx3?canal=" + canal;

			// Configura timeout menor para poder interromper mais rápido
			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			// Aguarda um pouco para dar tempo do serviço iniciar
			Thread.sleep(2000);

			// Verifica se foi cancelado DURANTE a chamada
			if (CANCELAMENTO_SOLICITADO.get()) {
				// Aguarda um pouco antes de tentar cancelar
				Thread.sleep(1000);

				// Tenta cancelar no serviço também
				try {
					restTemplate.postForEntity("http://localhost:8087/cancelar-offset-mtx3", null, Map.class);
					System.out.println(RED + "[INFO] Cancelamento de offset solicitado durante chamada" + RESET);
				} catch (Exception e) {
					System.err.println("Erro ao cancelar offset: " + e.getMessage());
				}

				Map<String, Object> cancelado = new HashMap<>();
				cancelado.put("status", "cancelado");
				cancelado.put("mensagem", "Offset cancelado durante execução");
				return cancelado;
			}

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
	}

	// Método para chamar linearização com verificação de cancelamento
	private Map<String, Object> chamarLinearizacaoComCancelamento(String canal) {
		if (CANCELAMENTO_SOLICITADO.get()) {
			Map<String, Object> cancelado = new HashMap<>();
			cancelado.put("status", "cancelado");
			cancelado.put("mensagem", "Operação cancelada antes da execução");
			return cancelado;
		}

		STATUS_PROCEDIMENTO_MTX3.put("etapa", "linearizacao");

		try {
			System.out.println(BLUE + "  Chamando endpoint de linearização para canal " + canal + RESET);

			// ANTES de chamar o serviço, verifica se há cancelamento
			if (CANCELAMENTO_SOLICITADO.get()) {
				throw new InterruptedException("Cancelamento solicitado antes da chamada HTTP");
			}

			String url = "http://localhost:8087/executar-linearizacao-canal-mtx3?canal=" + canal;

			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			if (CANCELAMENTO_SOLICITADO.get()) {
				// Se cancelado durante a chamada, tenta cancelar no serviço também
				try {
					restTemplate.postForEntity("http://localhost:8087/cancelar-linearizacao-mtx3", null, Map.class);
				} catch (Exception e) {
					System.err.println("Erro ao cancelar linearização: " + e.getMessage());
				}
				throw new InterruptedException("Cancelamento solicitado durante chamada HTTP");
			}

			Map<String, Object> resultado;
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				resultado = response.getBody();
				System.out.println(BLUE + "  Resposta da linearização: " + resultado.get("status") + RESET);
				return resultado;
			} else {
				throw new RuntimeException("Falha na comunicação com serviço de linearização");
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Map<String, Object> cancelado = new HashMap<>();
			cancelado.put("status", "cancelado");
			cancelado.put("mensagem", "Linearização cancelada: " + e.getMessage());
			return cancelado;

		} catch (Exception e) {
			System.err.println("  Erro ao chamar linearização: " + e.getMessage());
			Map<String, Object> erro = new HashMap<>();
			erro.put("status", "erro");
			erro.put("mensagem", "Falha ao chamar linearização: " + e.getMessage());
			erro.put("canal", canal);
			return erro;
		}
	}

	// Método para fazer uma checagem
	private Map<String, Object> chamarChecagem() {
		try {
			STATUS_PROCEDIMENTO_MTX3.put("etapa", "checagem_final");

			System.out.println(BLUE + "  Chamando endpoint de checagem final" + RESET);

			String url = "http://localhost:8087/executar-manualmente";

			ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

			Map<String, Object> resultado;
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				resultado = response.getBody();
				System.out.println(BLUE + "  Resposta da checagem final: " + resultado.get("status") + RESET);

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

	// Método para cancelar procedimentos externos
	private void cancelarProcedimentosExternos() {
		System.out.println(RED + "[INFO] Cancelando procedimentos em serviços externos..." + RESET);

		try {
			restTemplate.postForEntity("http://localhost:8087/cancelar-linearizacao-mtx3", null, Map.class);
			System.out.println(RED + "[INFO] Cancelamento de linearização solicitado" + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao cancelar linearização: " + e.getMessage());
		}

		try {
			restTemplate.postForEntity("http://localhost:8087/cancelar-offset-mtx3", null, Map.class);
			System.out.println(RED + "[INFO] Cancelamento de offset solicitado" + RESET);
		} catch (Exception e) {
			System.err.println("Erro ao cancelar offset: " + e.getMessage());
		}
	}

	// Método para cancelar procedimento
	private void cancelarProcedimento(String motivo) {
		CANCELAMENTO_SOLICITADO.set(true);
		System.out.println(RED + "\n[INFO] Iniciando cancelamento: " + motivo + RESET);

		// Interrompe a thread do procedimento se estiver em execução
		if (threadProcedimentoAtual != null && threadProcedimentoAtual.isAlive()) {
			threadProcedimentoAtual.interrupt();
			System.out.println(RED + "[INFO] Thread do procedimento interrompida" + RESET);
		}

		// Cancela procedimentos externos imediatamente
		cancelarProcedimentosExternos();
	}

	// Endpoint para cancelar procedimento em andamento
	@PostMapping("/cancelar-procedimento-mtx3")
	public ResponseEntity<Map<String, Object>> cancelarProcedimentomtx3() {
		Map<String, Object> resposta = new HashMap<>();

		if (!PROCEDIMENTO_ATIVO.get()) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Nenhum procedimento está em andamento");
			return ResponseEntity.status(400).body(resposta);
		}

		try {
			System.out.println(RED + "\n=== SOLICITAÇÃO DE CANCELAMENTO RECEBIDA ===" + RESET);
			System.out.println(RED + "Hora: " + LocalDateTime.now() + RESET);

			// Marca para cancelamento
			cancelarProcedimento("Cancelamento solicitado via API");

			resposta.put("status", "sucesso");
			resposta.put("mensagem", "Cancelamento do procedimento solicitado com sucesso");
			resposta.put("hora_cancelamento", LocalDateTime.now().toString());
			resposta.put("procedimento_em_andamento", true);
			resposta.put("thread_interrompida", threadProcedimentoAtual != null && threadProcedimentoAtual.isInterrupted());

			return ResponseEntity.ok(resposta);

		} catch (Exception e) {
			resposta.put("status", "erro");
			resposta.put("mensagem", "Erro ao processar cancelamento: " + e.getMessage());
			return ResponseEntity.status(500).body(resposta);
		}
	}

	// IMPORTANTE: Você também precisa modificar os serviços de offset e linearização
	// para que eles também suportem cancelamento. Adicione isso nos arquivos AjustarOffSetMtx3.java e LinearizacaoMtx3.java:

    /*
    // NO ARQUIVO AjustarOffSetMtx3.java:
    private static volatile boolean CANCELAR_OFFSET = false;

    @PostMapping("/cancelar-offset-mtx3")
    public ResponseEntity<Map<String, Object>> cancelarOffsetMtx3() {
        CANCELAR_OFFSET = true;
        // Interrompe qualquer operação em andamento
        if (threadOffsetAtual != null && threadOffsetAtual.isAlive()) {
            threadOffsetAtual.interrupt();
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "sucesso");
        resposta.put("mensagem", "Offset cancelado");
        return ResponseEntity.ok(resposta);
    }

    // E no método de ajuste offset, adicione verificações:
    if (CANCELAR_OFFSET) {
        System.out.println("Offset cancelado pelo usuário");
        return; // ou lance uma exceção
    }
    */

    /*
    // NO ARQUIVO LinearizacaoMtx3.java:
    private static volatile boolean CANCELAR_LINEARIZACAO = false;

    @PostMapping("/cancelar-linearizacao-mtx3")
    public ResponseEntity<Map<String, Object>> cancelarLinearizacaoMtx3() {
        CANCELAR_LINEARIZACAO = true;
        // Interrompe qualquer operação em andamento
        if (threadLinearizacaoAtual != null && threadLinearizacaoAtual.isAlive()) {
            threadLinearizacaoAtual.interrupt();
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "sucesso");
        resposta.put("mensagem", "Linearização cancelada");
        return ResponseEntity.ok(resposta);
    }

    // E no método de linearização, adicione verificações:
    if (CANCELAR_LINEARIZACAO) {
        System.out.println("Linearização cancelada pelo usuário");
        return; // ou lance uma exceção
    }
    */
}