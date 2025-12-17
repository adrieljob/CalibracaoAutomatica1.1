package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestController
public class ProcedimentoMtx1 {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";

    @Autowired
    private RestTemplate restTemplate;

    // Endpoint para executar ajuste offset + linearização para todos os canais
    @PostMapping("/executar-procedimento-completo-mtx1")
    public ResponseEntity<Map<String, Object>> executarProcedimentoCompletomtx1() {
        Map<String, Object> respostaGeral = new HashMap<>();
        Map<String, Object> resultados = new HashMap<>();

        try {
            System.out.println(GREEN + "=== INICIANDO PROCEDIMENTO COMPLETO MTX1 ===" + RESET);
            System.out.println(GREEN + "Hora de início: " + LocalDateTime.now() + RESET);

            // Sequência de canais
            String[] canais = {"14", "34", "51"};

            for (int i = 0; i < canais.length; i++) {
                String canal = canais[i];

                System.out.println(GREEN + "\n" + "=".repeat(60) + RESET);
                System.out.println(GREEN + "PROCESSANDO CANAL: " + canal + RESET);
                System.out.println(GREEN + "=".repeat(60) + RESET);

                // ========== ETAPA 1: AJUSTE OFFSET ==========
                System.out.println(GREEN + "\n[ETAPA 1] EXECUTANDO AJUSTE OFFSET PARA CANAL " + canal + RESET);

                // Chama o endpoint de ajuste offset com o canal como parâmetro
                Map<String, Object> resultadoAjuste = chamarAjusteOffset(canal);
                resultados.put("canal_" + canal + "_ajuste_offset", resultadoAjuste);

                if (!"sucesso".equals(resultadoAjuste.get("status"))) {
                    System.err.println("✗ Ajuste offset falhou para canal " + canal);
                    resultados.put("canal_" + canal + "_status", "erro_ajuste_offset");
                    // Decida se quer continuar com o próximo canal ou parar
                    // throw new RuntimeException("Falha no ajuste offset canal " + canal);
                } else {
                    System.out.println(GREEN + "✓ Ajuste offset concluído para canal " + canal + RESET);
                }

                // Aguarda entre ajuste offset e linearização
                System.out.println(GREEN + "\nAguardando 30 segundos antes da linearização..." + RESET);
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // ========== ETAPA 2: LINEARIZAÇÃO ==========
                System.out.println(GREEN + "\n[ETAPA 2] EXECUTANDO LINEARIZAÇÃO PARA CANAL " + canal + RESET);

                // Chama o endpoint de linearização com o canal como parâmetro
                Map<String, Object> resultadoLinearizacao = chamarLinearizacao(canal);
                resultados.put("canal_" + canal + "_linearizacao", resultadoLinearizacao);

                String statusLinearizacao = (String) resultadoLinearizacao.get("status");
                if (!"sucesso".equals(statusLinearizacao) && !"parcial".equals(statusLinearizacao)) {
                    System.err.println("✗ Linearização falhou para canal " + canal);
                    resultados.put("canal_" + canal + "_status", "erro_linearizacao");
                } else {
                    System.out.println(GREEN + "✓ Linearização concluída para canal " + canal + RESET);
                    resultados.put("canal_" + canal + "_status", "sucesso");
                }

                // Aguarda entre canais (exceto o último)
                if (i < canais.length - 1) {
                    System.out.println(GREEN + "\n" + "=".repeat(50) + RESET);
                    System.out.println(GREEN + "AGUARDANDO 2 MINUTOS ANTES DO PRÓXIMO CANAL..." + RESET);
                    System.out.println(GREEN + "=".repeat(50) + RESET);
                    try {
                        Thread.sleep(120000); // 2 minutos
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Prepara resposta final
            respostaGeral.put("status", "sucesso");
            respostaGeral.put("mensagem", "Procedimento completo executado para todos os canais");
            respostaGeral.put("hora_inicio", LocalDateTime.now().toString());
            respostaGeral.put("hora_fim", LocalDateTime.now().toString());
            respostaGeral.put("resultados", resultados);
            respostaGeral.put("sequencia_canais", "14 → 34 → 51");
            respostaGeral.put("etapas_por_canal", "Ajuste Offset → Linearização");

            System.out.println(GREEN + "\n=== PROCEDIMENTO COMPLETO FINALIZADO ===" + RESET);
            System.out.println(GREEN + "Hora de fim: " + LocalDateTime.now() + RESET);

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
    @PostMapping("/executar-procedimento-canal-mtx1")
    public ResponseEntity<Map<String, Object>> executarProcedimentoCanalmtx1(@RequestParam String canal) {
        Map<String, Object> resposta = new HashMap<>();

        try {
            System.out.println(GREEN + "=== INICIANDO PROCEDIMENTO PARA CANAL: " + canal + " ===" + RESET);
            System.out.println(GREEN + "Hora de início: " + LocalDateTime.now() + RESET);

            // ========== ETAPA 1: AJUSTE OFFSET ==========
            System.out.println(GREEN + "\n[ETAPA 1] EXECUTANDO AJUSTE OFFSET" + RESET);
            Map<String, Object> resultadoAjuste = chamarAjusteOffset(canal);

            // Aguarda entre etapas
            System.out.println(GREEN + "\nAguardando 30 segundos antes da linearização..." + RESET);
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // ========== ETAPA 2: LINEARIZAÇÃO ==========
            System.out.println(GREEN + "\n[ETAPA 2] EXECUTANDO LINEARIZAÇÃO" + RESET);
            Map<String, Object> resultadoLinearizacao = chamarLinearizacao(canal);

            // Prepara resposta
            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Procedimento executado para canal " + canal);
            resposta.put("hora_inicio", LocalDateTime.now().toString());
            resposta.put("hora_fim", LocalDateTime.now().toString());
            resposta.put("canal", canal);
            resposta.put("ajuste_offset", resultadoAjuste);
            resposta.put("linearizacao", resultadoLinearizacao);

            System.out.println(GREEN + "\n=== PROCEDIMENTO FINALIZADO PARA CANAL: " + canal + " ===" + RESET);
            System.out.println(GREEN + "Hora de fim: " + LocalDateTime.now() + RESET);

            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro no procedimento para canal " + canal + ": " + e.getMessage());
            resposta.put("hora_inicio", LocalDateTime.now().toString());
            resposta.put("canal", canal);

            System.err.println("Erro no procedimento para canal " + canal + ": " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(resposta);
        }
    }

    // Método para chamar o ajuste offset
    private Map<String, Object> chamarAjusteOffset(String canal) {
        try {
            System.out.println(GREEN + "  Chamando endpoint de ajuste offset para canal " + canal + RESET);

            // URL do endpoint existente do AjustarOffSetMtx1
            String url = "http://localhost:8087/executar-offset-canal-mtx1?canal=" + canal;
            // Faz a requisição POST
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resultado = response.getBody();
                System.out.println(GREEN + "  Resposta do ajuste offset: " + resultado.get("status")+ RESET);
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

    // Método para chamar a linearização
    private Map<String, Object> chamarLinearizacao(String canal) {
        try {
            System.out.println(GREEN + "  Chamando endpoint de linearização para canal " + canal + RESET);

            // URL do endpoint existente do LinearizacaoMtx1
            String url = "http://localhost:8087/executar-linearizacao-canal-mtx1?canal=" + canal;

            // Faz a requisição POST
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resultado = response.getBody();
                System.out.println(GREEN + "  Resposta da linearização: " + resultado.get("status")+ RESET);
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
    }

    // Endpoint para cancelar procedimento em andamento
    @PostMapping("/cancelar-procedimento-mtx1")
    public ResponseEntity<Map<String, Object>> cancelarProcedimentomtx1() {
        Map<String, Object> resposta = new HashMap<>();

        try {
            System.out.println(GREEN + "Solicitação de cancelamento de procedimento recebida"+ RESET);

            // Opcional: Chamar endpoints de cancelamento dos serviços
            try {
                restTemplate.postForEntity("http://localhost:8087/cancelar-linearizacao-mtx1", null, Map.class);
            } catch (Exception e) {
                System.err.println("Erro ao cancelar linearização: " + e.getMessage());
            }

            try {
                restTemplate.postForEntity("http://localhost:8087/cancelar-offset-mtx1", null, Map.class);
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

    // Endpoint para verificar status dos serviços
    @GetMapping("/status-procedimento-mtx1")
    public ResponseEntity<Map<String, Object>> verificarStatusmtx1() {
        Map<String, Object> resposta = new HashMap<>();

        resposta.put("status", "disponivel");
        resposta.put("servicos", new String[] {
                "AjustarOffSetMtx1 - Disponível",
                "LinearizacaoMtx1 - Disponível",
                "ProcedimentoMtx1 - Disponível"
        });
        resposta.put("hora_verificacao", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }



}