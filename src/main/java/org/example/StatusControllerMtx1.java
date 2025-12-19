/*import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class StatusControllerMtx1 {

    // Armazena o status das rotinas
    private static Map<String, String> statusRotinas = new ConcurrentHashMap<>();

    @GetMapping("/status-procedimento-mtx1")
    public ResponseEntity<Map<String, Object>> getStatusProcedimentoMtx1() {
        Map<String, Object> resposta = new HashMap<>();
        String status = statusRotinas.getOrDefault("procedimento-mtx1", "inativo");

        resposta.put("status", status);
        resposta.put("mensagem", "Status do procedimento MTX1");
        resposta.put("hora_consulta", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/status-offset-mtx1")
    public ResponseEntity<Map<String, Object>> getStatusOffsetMtx1() {
        Map<String, Object> resposta = new HashMap<>();
        String status = statusRotinas.getOrDefault("offset-mtx1", "inativo");

        resposta.put("status", status);
        resposta.put("mensagem", "Status do offset MTX1");
        resposta.put("hora_consulta", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/status-linearizacao-mtx1")
    public ResponseEntity<Map<String, Object>> getStatusLinearizacaoMtx1() {
        Map<String, Object> resposta = new HashMap<>();
        String status = statusRotinas.getOrDefault("linearizacao-mtx1", "inativo");

        resposta.put("status", status);
        resposta.put("mensagem", "Status da linearização MTX1");
        resposta.put("hora_consulta", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    // Método para atualizar status (chamar no início e fim das rotinas)
    public static void atualizarStatusRotina(String chave, String status) {
        statusRotinas.put(chave, status);
    }
}

 */
