package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MudarPotenciaMTX4 {

    // Mapa de valores pré-programados para potência
    private static final Map<String, String> VALORES_POTENCIA = new HashMap<>();

    static {
        VALORES_POTENCIA.put("300", "1W");
        VALORES_POTENCIA.put("340", "2.5W");
        VALORES_POTENCIA.put("370", "5W");
        VALORES_POTENCIA.put("430", "10W");
        VALORES_POTENCIA.put("486", "72.4W");
    }

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @PostMapping("/mudar-potencia-mtx4")
    public ResponseEntity<Map<String, Object>> mudarPotencia4(@RequestParam String valorPotencia) {
        Map<String, Object> resposta = new HashMap<>();

        if (!VALORES_POTENCIA.containsKey(valorPotencia)) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Valor não programado. Valores permitidos: " +
                    String.join(", ", VALORES_POTENCIA.keySet()));
            resposta.put("valores_programados", VALORES_POTENCIA);
            return ResponseEntity.badRequest().body(resposta);
        }

        try {
            Map<String, Object> resultado = configurarPotenciaMTX4(valorPotencia);

            // Se houve erro no processo interno
            if ("erro".equals(resultado.get("status"))) {
                resposta.put("status", "erro");
                resposta.put("mensagem", resultado.get("mensagem"));
                return ResponseEntity.status(500).body(resposta);
            }

            resposta.put("status", "sucesso");
            resposta.put("mensagem", "Potência do MTX4 configurada com sucesso");
            resposta.put("valor_configurado", valorPotencia);
            resposta.put("potencia_esperada", VALORES_POTENCIA.get(valorPotencia));
            resposta.put("potencia_antes", resultado.get("potencia_antes"));
            resposta.put("potencia_depois", resultado.get("potencia_depois"));

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            resposta.put("status", "erro");
            resposta.put("mensagem", "Erro ao configurar potência: " + e.getMessage());
            resposta.put("valor_configurado", valorPotencia);

            return ResponseEntity.status(500).body(resposta);
        }
    }

    // Método principal para configurar a potência do MTX4
    private Map<String, Object> configurarPotenciaMTX4(String valorPotencia) {
        String urlBase = "http://10.10.103.103/debug/";
        System.out.println("Configurando potência do MTX4 para DAC: " + valorPotencia +
                " (" + VALORES_POTENCIA.get(valorPotencia) + ")");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        // options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--disable-cache");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);
            System.out.println("Acessando: " + urlBase);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
            campoSenha.clear();
            campoSenha.sendKeys(password);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
            botaoLogin.click();
            System.out.println("Login realizado");

            // Aguardar carregamento
            Thread.sleep(300);

            // Acessa PowerAmp 4
            WebElement powerAmp4 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier4___']/input")));
            powerAmp4.click();
            Thread.sleep(300);
            System.out.println("PowerAmplifier4 selecionado");

            // Pega o valor da potencia ANTES da mudança
            WebElement getPotenciaAntes = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_ForwardPower")));
            String potenciaAntes = getPotenciaAntes.getText().trim();
            System.out.println("Potência ANTES: " + potenciaAntes);

            // *** ETAPA 1: Clica no OutputPower para abrir o diálogo ***
            System.out.println("Clicando em OutputPower...");

            // Tentar diferentes formas de encontrar o OutputPower
            WebElement outputPower = null;
            try {
                // Tentativa 1: Pelo ID exato
                outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("PowerAmplifier4.Config.OutputPower")));
                System.out.println("OutputPower encontrado pelo ID exato");
            } catch (Exception e1) {
                try {
                    // Tentativa 2: Pelo ID com underscore
                    outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                            By.id("PowerAmplifier4_Config_OutputPower")));
                    System.out.println("OutputPower encontrado pelo ID com underscore");
                } catch (Exception e2) {
                    try {
                        // Tentativa 3: Pelo texto
                        outputPower = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//*[contains(text(), 'OutputPower')]")));
                        System.out.println("OutputPower encontrado pelo texto");
                    } catch (Exception e3) {
                        resultado.put("status", "erro");
                        resultado.put("mensagem", "OutputPower não encontrado");
                        return resultado;
                    }
                }
            }

            outputPower.click();
            Thread.sleep(300);
            System.out.println("OutputPower clicado - diálogo deve estar aberto");

            // *** ETAPA 2: Encontrar e preencher o campo New Value ***
            System.out.println("Procurando campo New Value...");

            WebElement campoNewValue = null;

            // Estratégia 1: Procurar input ativo (focado)
            try {
                campoNewValue = driver.switchTo().activeElement();
                if (campoNewValue.getTagName().equals("input") || campoNewValue.getTagName().equals("textarea")) {
                    System.out.println("Campo ativo encontrado: " + campoNewValue.getTagName());
                } else {
                    campoNewValue = null;
                }
            } catch (Exception e) {
                System.out.println("Não encontrou campo ativo");
            }

            // Estratégia 2: Procurar por inputs visíveis no diálogo
            if (campoNewValue == null) {
                try {
                    // Primeiro, tentar encontrar o diálogo/modal
                    WebElement dialogo = driver.findElement(
                            By.xpath("//div[contains(@class, 'modal') or contains(@class, 'dialog') or " +
                                    "contains(@style, 'display: block') or @role='dialog']"));

                    // Dentro do diálogo, procurar input
                    campoNewValue = dialogo.findElement(By.tagName("input"));
                    System.out.println("Campo encontrado dentro do diálogo");
                } catch (Exception e) {
                    System.out.println("Não encontrou diálogo específico");
                }
            }

            // Estratégia 3: Procurar por qualquer input visível
            if (campoNewValue == null) {
                try {
                    // Procurar todos os inputs e filtrar os visíveis
                    java.util.List<WebElement> todosInputs = driver.findElements(By.tagName("input"));
                    for (WebElement input : todosInputs) {
                        if (input.isDisplayed() && input.isEnabled()) {
                            // Verificar se é um campo de texto/número
                            String type = input.getAttribute("type");
                            if (type == null || type.equals("text") || type.equals("number")) {
                                campoNewValue = input;
                                System.out.println("Campo visível encontrado: input");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Não encontrou input visível");
                }
            }

            // Estratégia 4: Procurar pelo seu XPath original
            if (campoNewValue == null) {
                try {
                    campoNewValue = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//label[@type='text']/input")));
                    System.out.println("Campo encontrado pelo XPath original");
                } catch (Exception e) {
                    // Tentar variações do XPath
                    try {
                        campoNewValue = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//label[contains(text(), 'New Value')]/input")));
                        System.out.println("Campo encontrado pelo texto 'New Value'");
                    } catch (Exception e2) {
                        try {
                            campoNewValue = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//input[@placeholder='New Value' or contains(@placeholder, 'value')]")));
                            System.out.println("Campo encontrado pelo placeholder");
                        } catch (Exception e3) {
                            resultado.put("status", "erro");
                            resultado.put("mensagem", "Campo New Value não encontrado");
                            return resultado;
                        }
                    }
                }
            }

            // *** ETAPA 3: Digitar o valor da potência ***
            if (campoNewValue != null) {
                System.out.println("Campo New Value encontrado. Digitando valor: " + valorPotencia);

                // Clicar no campo para garantir foco
                campoNewValue.click();
                Thread.sleep(300);

                // Limpar o campo (Ctrl+A + Delete)
                campoNewValue.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                Thread.sleep(300);
                campoNewValue.sendKeys(Keys.DELETE);
                Thread.sleep(300);

                // Digitar o novo valor
                campoNewValue.sendKeys(valorPotencia);
                System.out.println("Valor " + valorPotencia + " digitado no campo New Value");
                Thread.sleep(300);

                // *** ETAPA 4: Confirmar a mudança ***
                System.out.println("Confirmando a mudança...");

                // Método 1: Pressionar Enter
                campoNewValue.sendKeys(Keys.ENTER);
                Thread.sleep(300);

                // Método 2: Procurar e clicar em botão OK/Apply
                try {
                    WebElement botaoOk = driver.findElement(
                            By.xpath("//button[contains(text(), 'OK') or " +
                                    "contains(text(), 'Apply') or " +
                                    "contains(text(), 'Save') or " +
                                    "contains(text(), 'Confirm')]"));
                    if (botaoOk.isDisplayed() && botaoOk.isEnabled()) {
                        botaoOk.click();
                        System.out.println("Botão OK/Apply clicado");
                        Thread.sleep(300);
                    }
                } catch (Exception e) {
                    System.out.println("Botão OK não encontrado, usando apenas Enter");
                }

                // Método 3: Tentar clicar fora do campo para confirmar
                try {
                    // Clicar em algum lugar da página para sair do campo
                    driver.findElement(By.tagName("body")).click();
                    Thread.sleep(300);
                } catch (Exception e) {
                    // Ignorar
                }

            } else {
                resultado.put("status", "erro");
                resultado.put("mensagem", "Não foi possível encontrar o campo New Value");
                return resultado;
            }

            // Aguardar aplicação da mudança
            System.out.println("Aguardando aplicação da mudança...");
            Thread.sleep(300);

            // Recarregar a página para ver mudanças (opcional)
            try {
                driver.navigate().refresh();
                Thread.sleep(300);

                // Re-selecionar PowerAmplifier4 após refresh
                powerAmp4 = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[@link='PowerAmplifier4___']/input")));
                powerAmp4.click();
                Thread.sleep(300);
            } catch (Exception e) {
                System.out.println("Não foi possível recarregar: " + e.getMessage());
            }

            // Pega o valor da potencia DEPOIS da mudança
            WebElement getPotenciaDepois = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_ForwardPower")));
            String potenciaDepois = getPotenciaDepois.getText().trim();
            System.out.println("Potência DEPOIS: " + potenciaDepois);

            // Verificar também o valor configurado atual
            try {
                WebElement campoConfigurado = driver.findElement(By.id("PowerAmplifier4_Config_OutputPower"));
                String valorAtual = campoConfigurado.getText();
                if (valorAtual == null || valorAtual.isEmpty()) {
                    valorAtual = campoConfigurado.getAttribute("value");
                }
                System.out.println("Valor configurado atual: " + valorAtual);
            } catch (Exception e) {
                System.out.println("Não foi possível verificar valor configurado atual");
            }

            // Registrar resultados
            resultado.put("status", "sucesso");
            resultado.put("potencia_antes", potenciaAntes);
            resultado.put("potencia_depois", potenciaDepois);
            resultado.put("valor_configurado", valorPotencia);
            resultado.put("potencia_esperada", VALORES_POTENCIA.get(valorPotencia));

            // Salvar log
            String filePath = System.getProperty("user.dir") + "/potencia_mtx4.txt";
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
                writer.write(LocalDateTime.now() + " | DAC: " + valorPotencia +
                        " | Esperado: " + VALORES_POTENCIA.get(valorPotencia) +
                        " | Antes: " + potenciaAntes +
                        " | Depois: " + potenciaDepois + "\n");
                System.out.println("Log salvo: " + filePath);
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
                System.out.println("Driver finalizado");
            }
        }

        return resultado;
    }

    // Método auxiliar para debug
    private void debugElementosPagina4(WebDriver driver) {
        System.out.println("\n=== DEBUG: Elementos da Página ===");

        // Contar elementos
        java.util.List<WebElement> todosInputs = driver.findElements(By.tagName("input"));
        System.out.println("Total de inputs: " + todosInputs.size());

        java.util.List<WebElement> todosBotoes = driver.findElements(By.tagName("button"));
        System.out.println("Total de botões: " + todosBotoes.size());

        // Mostrar inputs visíveis
        System.out.println("\n--- Inputs Visíveis ---");
        for (int i = 0; i < Math.min(todosInputs.size(), 10); i++) {
            WebElement input = todosInputs.get(i);
            if (input.isDisplayed()) {
                System.out.println("Input " + i + ": " +
                        "ID='" + input.getAttribute("id") + "', " +
                        "Name='" + input.getAttribute("name") + "', " +
                        "Type='" + input.getAttribute("type") + "', " +
                        "Value='" + input.getAttribute("value") + "'");
            }
        }

        // Mostrar botões visíveis
        System.out.println("\n--- Botões Visíveis ---");
        for (int i = 0; i < Math.min(todosBotoes.size(), 10); i++) {
            WebElement botao = todosBotoes.get(i);
            if (botao.isDisplayed()) {
                System.out.println("Botão " + i + ": " +
                        "Texto='" + botao.getText() + "', " +
                        "ID='" + botao.getAttribute("id") + "', " +
                        "Class='" + botao.getAttribute("class") + "'");
            }
        }

        System.out.println("=== FIM DEBUG ===\n");
    }

    // Endpoint para testar todas as potências em sequência
    @PostMapping("/testar-todas-potencias4")
    public ResponseEntity<Map<String, Object>> testarTodasPotencias4() {
        Map<String, Object> resposta = new HashMap<>();

        new Thread(() -> {
            System.out.println("=== INICIANDO TESTE DE TODAS AS POTÊNCIAS ===");

            for (String valor : VALORES_POTENCIA.keySet()) {
                try {
                    System.out.println("\n>>> Testando: " + valor + " (" + VALORES_POTENCIA.get(valor) + ")");

                    Map<String, Object> resultado = configurarPotenciaMTX4(valor);
                    System.out.println("Resultado: " + resultado);

                    Thread.sleep(300);

                } catch (Exception e) {
                    System.err.println("Erro em " + valor + ": " + e.getMessage());
                }
            }

            System.out.println("=== TESTE CONCLUÍDO ===");
        }).start();

        resposta.put("status", "teste_iniciado");
        resposta.put("mensagem", "Teste de todas as potências iniciado");
        resposta.put("total_potencias", VALORES_POTENCIA.size());

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para testar sequência específica
    @PostMapping("/testar-sequencia-potencias4")
    public ResponseEntity<Map<String, Object>> testarSequencia4(@RequestParam String sequencia) {
        Map<String, Object> resposta = new HashMap<>();

        String[] valores = sequencia.split(",");

        new Thread(() -> {
            System.out.println("=== INICIANDO SEQUÊNCIA PERSONALIZADA ===");

            for (int i = 0; i < valores.length; i++) {
                String valor = valores[i].trim();
                try {
                    System.out.println("Teste " + (i+1) + "/" + valores.length +
                            ": " + valor + " (" + VALORES_POTENCIA.get(valor) + ")");

                    configurarPotenciaMTX4(valor);
                    Thread.sleep(300);

                } catch (Exception e) {
                    System.err.println("Erro em " + valor + ": " + e.getMessage());
                }
            }

            System.out.println("=== SEQUÊNCIA CONCLUÍDA ===");
        }).start();

        resposta.put("status", "sequencia_iniciada");
        resposta.put("mensagem", "Sequência iniciada em background");
        resposta.put("total_valores", valores.length);

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para ver valores disponíveis
    @GetMapping("/valores-potencia4")
    public ResponseEntity<Map<String, Object>> getValoresPotencia4() {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", "sucesso");
        resposta.put("valores", VALORES_POTENCIA);
        resposta.put("descricao", "DAC -> Potência aproximada (watts)");

        return ResponseEntity.ok(resposta);
    }
}