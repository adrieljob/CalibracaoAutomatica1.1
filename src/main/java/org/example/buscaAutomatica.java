package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.PostConstruct; // Usando jakarta (Java 11+)
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
@EnableScheduling
@RestController
public class buscaAutomatica {

    @Value("${app.username}")
    private String username;

    @Value("${app.password}")
    private String password;

    @Autowired
    private TaskScheduler taskScheduler;

    private ScheduledFuture<?> tarefaAgendada;
    private long intervaloAtual = 60000; // padrão 1 minuto
    private boolean agendamentoAtivo = true;

    // Inicia o agendamento ao iniciar a aplicação
    @PostConstruct
    public void iniciarAgendamento() {
        agendarTarefa(intervaloAtual);
    }

    // Método para agendar/reagendar a tarefa
    private void agendarTarefa(long intervaloMs) {
        // Cancela a tarefa anterior se existir
        if (tarefaAgendada != null) {
            tarefaAgendada.cancel(false);
        }

        if (agendamentoAtivo) {
            // Cria um trigger periódico com o intervalo especificado
            PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(intervaloMs));
            trigger.setFixedRate(true); // Executa em intervalo fixo

            // Agenda a nova tarefa
            tarefaAgendada = taskScheduler.schedule(this::executarColeta, trigger);

            System.out.println("Tarefa agendada com intervalo de " + intervaloMs + "ms");
            this.intervaloAtual = intervaloMs;
        }
    }

    // Método que será executado periodicamente
    private void executarColeta() {
        System.out.println("=== Execução agendada iniciada ===");
        System.out.println("Intervalo atual: " + intervaloAtual + "ms");

        try {
            String resultado = coletarDadosESalvar();
            System.out.println("Coleta executada com sucesso");
        } catch (Exception e) {
            System.err.println("Erro na execução agendada: " + e.getMessage());
        }
    }

    // Endpoint para alterar o intervalo DINAMICAMENTE
    @PostMapping("/configurar-intervalo")
    public ResponseEntity<Map<String, Object>> configurarIntervalo(@RequestParam long milissegundos) {
        Map<String, Object> resposta = new HashMap<>();

        if (milissegundos < 2000) { // Mínimo de 2 segundos
            resposta.put("erro", "Intervalo mínimo é 2000ms (2 segundos)");
            return ResponseEntity.badRequest().body(resposta);
        }

        // Atualiza o intervalo e reage a tarefa
        this.intervaloAtual = milissegundos;
        agendarTarefa(milissegundos);

        resposta.put("mensagem", "Intervalo configurado para " + milissegundos + "ms");
        resposta.put("intervalo", milissegundos);
        resposta.put("legivel", formatarIntervalo(milissegundos));

        return ResponseEntity.ok(resposta);
    }

    // Endpoint para pausar/retomar
    @PostMapping("/toggle-agendamento")
    public ResponseEntity<Map<String, Object>> toggleAgendamento(@RequestParam boolean ativo) {
        Map<String, Object> resposta = new HashMap<>();

        this.agendamentoAtivo = ativo;

        if (ativo) {
            // Se está ativando, agenda a tarefa
            agendarTarefa(this.intervaloAtual);
            resposta.put("mensagem", "Agendamento ativado");
        } else {
            // Se está desativando, cancela a tarefa
            if (tarefaAgendada != null) {
                tarefaAgendada.cancel(false);
                tarefaAgendada = null;
            }
            resposta.put("mensagem", "Agendamento desativado");
        }

        resposta.put("ativo", ativo);
        return ResponseEntity.ok(resposta);
    }

    // Endpoint com opções pré-definidas
    @PostMapping("/configurar-intervalo-predefinido")
    public ResponseEntity<Map<String, Object>> configurarIntervaloPredefinido(@RequestParam String opcao) {
        Map<String, Long> opcoes = new HashMap<>();
        opcoes.put("30s", 30000L);
        opcoes.put("1min", 60000L);
        opcoes.put("2min", 120000L);
        opcoes.put("3min", 180000L);
        opcoes.put("5min", 300000L);
        opcoes.put("10min", 600000L);
        opcoes.put("15min", 900000L);
        opcoes.put("30min", 1800000L);
        opcoes.put("1h", 3600000L);
        opcoes.put("6h", 21600000L);
        opcoes.put("12h", 43200000L);
        opcoes.put("Disabled", 0L);

        Map<String, Object> resposta = new HashMap<>();

        if (opcoes.containsKey(opcao)) {
            this.intervaloAtual = opcoes.get(opcao);
            agendarTarefa(opcoes.get(opcao));

            resposta.put("mensagem", "Intervalo configurado para " + opcao);
            resposta.put("opcao", opcao);
            resposta.put("intervalo", opcoes.get(opcao));
            resposta.put("legivel", formatarIntervalo(opcoes.get(opcao)));

            return ResponseEntity.ok(resposta);
        } else {
            resposta.put("erro", "Opção inválida");
            resposta.put("opcoes_validas", opcoes.keySet());
            return ResponseEntity.badRequest().body(resposta);
        }
    }

    // Endpoint para executar manualmente
    @PostMapping("/executar-manualmente")
    public ResponseEntity<Map<String, Object>> executarManualmente() {
        Map<String, Object> resposta = new HashMap<>();

        try {
            System.out.println("=== Execução manual solicitada ===");
            String resultado = coletarDadosESalvar();

            resposta.put("mensagem", "Execução manual realizada com sucesso");
            resposta.put("status", "sucesso");

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            resposta.put("erro", "Falha na execução manual: " + e.getMessage());
            resposta.put("status", "erro");

            return ResponseEntity.status(500).body(resposta);
        }
    }

    // Endpoint para obter status atual
    @GetMapping("/status-agendamento")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> resposta = new HashMap<>();

        boolean tarefaAtiva = tarefaAgendada != null && !tarefaAgendada.isCancelled();

        resposta.put("agendamentoAtivo", agendamentoAtivo && tarefaAtiva);
        resposta.put("intervaloAtual", intervaloAtual);
        resposta.put("intervaloLegivel", formatarIntervalo(intervaloAtual));
        resposta.put("username", username != null ? "configurado" : "não configurado");

        return ResponseEntity.ok(resposta);
    }

    private String formatarIntervalo(long ms) {
        if (ms < 60000) {
            return (ms / 1000) + " segundos";
        } else if (ms < 3600000) {
            return (ms / 60000) + " minutos";
        } else {
            return (ms / 3600000) + " horas";
        }
    }

    // Endpoint de debug (opcional)
    @GetMapping("/funcaoAlcDebug")
    public String funcaoAlcDebug() {
        System.out.println("=== /funcaoAlcDebug chamado ===");
        return "Calibração - Sistema operacional";
    }

    private String coletarDadosESalvar() {
        String urlBase = "http://10.10.103.103/debug/";
        System.out.println("URL que será acessada: " + urlBase);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        // options.addArguments("--headless"); // Desativado para exibir o que está sendo feito
        options.addArguments("--incognito");
        options.addArguments("--disable-cache");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
            campoSenha.clear();
            campoSenha.sendKeys(password);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
            botaoLogin.click();

            // MTX1
            // clica em modulador
            WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator1___']/input")));
            Thread.sleep(300);
            modulator1.click();

            // pega o numero do canal
            WebElement getChanelNumber1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);
            String chanelNumber1 = getChanelNumber1.getText().trim();

            // pega a qfrequencia
            WebElement getFrequency1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Status_FrequencyChannel")));
            Thread.sleep(300);
            String frequency1 = getFrequency1.getText().trim();

            // clica em powerAmplifier
            WebElement powerAmp1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier1___']/input")));
            Thread.sleep(300);
            powerAmp1.click();

            // pega a potencia em dbm
            WebElement getForwardDBM1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Config_OutputPower")));
            Thread.sleep(300);
            String forwardDBM1 = getForwardDBM1.getText().trim();

            // pega o status ALC
            WebElement getALC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_AlcStatus")));
            Thread.sleep(300);
            String statusALC1 = getALC1.getText().trim();

            // pega o DAC output
            WebElement getOutputDAC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_Measures_output_dac")));
            Thread.sleep(300);
            String outputDAC1 = getOutputDAC1.getText().trim();

            // pega o DAC disered
            WebElement getDesiredDAC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_Measures_desired_dac")));
            Thread.sleep(300);
            String desiredDAC1 = getDesiredDAC1.getText().trim();

            // pega a potencia
            WebElement getPotencia1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_ForwardPower")));
            Thread.sleep(300);
            String potencia1 = getPotencia1.getText().trim();

            // pega a potencia refletida
            WebElement getRefletida1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_ReflectedPower")));
            Thread.sleep(300);
            String refletida1 = getRefletida1.getText().trim();

            // pega a  temperatura
            WebElement getTemperatura1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_FanControl_ActualTemperature")));
            Thread.sleep(300);
            String temperatura1 = getTemperatura1.getText().trim();

            // pega a FAN 1
            WebElement getFan1MTX1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_FanControl_Fan1Speed")));
            Thread.sleep(300);
            String fan1MTX1 = getFan1MTX1.getText().trim();

            // pega a FAN 2
            WebElement getFan2MTX1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_FanControl_Fan2Speed")));
            Thread.sleep(300);
            String fan2MTX1 = getFan2MTX1.getText().trim();

            // clica em INTERNAL
            WebElement internal1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Internal1___']/input")));
            Thread.sleep(300);
            internal1.click();

            // pega o OFFSET
            WebElement getOffSetMtx1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Internal1_power_offset")));
            Thread.sleep(300);
            String offSetMtx1 = getOffSetMtx1.getText().trim();


            System.out.println("Dados extraídos:");
            System.out.println("MTX1");
            System.out.println("Numero do canal1: " + chanelNumber1);
            System.out.println("Frequencia1: " + frequency1);
            System.out.println("forwardDBM1: " + forwardDBM1);
            System.out.println("Status ALC1: " + statusALC1);
            System.out.println("Output DAC1: " + outputDAC1);
            System.out.println("Desired DAC1: " + desiredDAC1);
            System.out.println("Potencia Direta1: " + potencia1);
            System.out.println("Potencia Refletida1: " + refletida1);
            System.out.println("Temperatura1: " + temperatura1);
            System.out.println("FAN1 MTX1: " + fan1MTX1);
            System.out.println("FAN2 MTX1: " + fan2MTX1);
            System.out.println("offSetMtx1: " + offSetMtx1);

            // MTX2
            WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator2___']/input")));
            Thread.sleep(300);
            modulator2.click();

            WebElement getChanelNumber2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator2_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);
            String chanelNumber2 = getChanelNumber2.getText().trim();

            WebElement getFrequency2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator2_Status_FrequencyChannel")));
            Thread.sleep(300);
            String frequency2 = getFrequency2.getText().trim();

            WebElement powerAmp2 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier2___']/input")));
            Thread.sleep(300);
            powerAmp2.click();

            WebElement getForwardDBM2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Config_OutputPower")));
            Thread.sleep(300);
            String forwardDBM2 = getForwardDBM2.getText().trim();

            WebElement getALC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_AlcStatus")));
            Thread.sleep(300);
            String statusALC2 = getALC2.getText().trim();

            WebElement getOutputDAC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_Measures_output_dac")));
            Thread.sleep(300);
            String outputDAC2 = getOutputDAC2.getText().trim();

            WebElement getDesiredDAC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_Measures_desired_dac")));
            Thread.sleep(300);
            String desiredDAC2 = getDesiredDAC2.getText().trim();

            WebElement getPotencia2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_ForwardPower")));
            Thread.sleep(300);
            String potencia2 = getPotencia2.getText().trim();

            WebElement getRefletida2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_ReflectedPower")));
            Thread.sleep(300);
            String refletida2 = getRefletida2.getText().trim();

            WebElement getTemperatura2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_FanControl_ActualTemperature")));
            Thread.sleep(300);
            String temperatura2 = getTemperatura2.getText().trim();

            WebElement getFan1MTX2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_FanControl_Fan1Speed")));
            Thread.sleep(300);
            String fan1MTX2 = getFan1MTX2.getText().trim();

            WebElement getFan2MTX2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_FanControl_Fan2Speed")));
            Thread.sleep(300);
            String fan2MTX2 = getFan2MTX2.getText().trim();

            // clica em INTERNAL
            WebElement internal2 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Internal2___']/input")));
            Thread.sleep(300);
            internal2.click();

            // pega o OFFSET
            WebElement getOffSetMtx2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Internal2_power_offset")));
            Thread.sleep(300);
            String offSetMtx2 = getOffSetMtx2.getText().trim();

            System.out.println("MTX2");
            System.out.println("Numero do canal2: " + chanelNumber2);
            System.out.println("Frequencia2: " + frequency2);
            System.out.println("forwardDBM2: " + forwardDBM2);
            System.out.println("Status ALC2: " + statusALC2);
            System.out.println("Output DAC2: " + outputDAC2);
            System.out.println("Desired DAC2: " + desiredDAC2);
            System.out.println("Potencia Direta2: " + potencia2);
            System.out.println("Potencia Refletida2: " + refletida2);
            System.out.println("Temperatura2: " + temperatura2);
            System.out.println("FAN1 MTX2: " + fan1MTX2);
            System.out.println("FAN2 MTX2: " + fan2MTX2);
            System.out.println("offSetMtx2: " + offSetMtx2);

            // MTX3
            WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator3___']/input")));
            modulator3.click();

            WebElement getChanelNumber3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator3_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);
            String chanelNumber3 = getChanelNumber3.getText().trim();

            WebElement getFrequency3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator3_Status_FrequencyChannel")));
            Thread.sleep(300);
            String frequency3 = getFrequency3.getText().trim();

            WebElement powerAmp3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier3___']/input")));
            Thread.sleep(300);
            powerAmp3.click();

            WebElement getForwardDBM3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Config_OutputPower")));
            Thread.sleep(300);
            String forwardDBM3 = getForwardDBM3.getText().trim();

            WebElement getALC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_AlcStatus")));
            Thread.sleep(300);
            String statusALC3 = getALC3.getText().trim();

            WebElement getOutputDAC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_Measures_output_dac")));
            Thread.sleep(300);
            String outputDAC3 = getOutputDAC3.getText().trim();

            WebElement getDesiredDAC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_Measures_desired_dac")));
            Thread.sleep(300);
            String desiredDAC3 = getDesiredDAC3.getText().trim();

            WebElement getPotencia3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_ForwardPower")));
            Thread.sleep(300);
            String potencia3 = getPotencia3.getText().trim();

            WebElement getRefletida3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_ReflectedPower")));
            Thread.sleep(300);
            String refletida3 = getRefletida3.getText().trim();

            WebElement getTemperatura3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_FanControl_ActualTemperature")));
            Thread.sleep(300);
            String temperatura3 = getTemperatura3.getText().trim();

            WebElement getFan1MTX3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_FanControl_Fan1Speed")));
            Thread.sleep(300);
            String fan1MTX3 = getFan1MTX3.getText().trim();

            WebElement getFan2MTX3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_FanControl_Fan2Speed")));
            Thread.sleep(300);
            String fan2MTX3 = getFan2MTX3.getText().trim();

            // clica em INTERNAL
            WebElement internal3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Internal3___']/input")));
            Thread.sleep(300);
            internal3.click();

            // pega o OFFSET
            WebElement getOffSetMtx3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Internal3_power_offset")));
            Thread.sleep(300);
            String offSetMtx3 = getOffSetMtx3.getText().trim();

            System.out.println("MTX3");
            System.out.println("Numero do canal3: " + chanelNumber3);
            System.out.println("Frequencia3: " + frequency3);
            System.out.println("forwardDBM3: " + forwardDBM3);
            System.out.println("Status ALC3: " + statusALC3);
            System.out.println("Output DAC3: " + outputDAC3);
            System.out.println("Desired DAC3: " + desiredDAC3);
            System.out.println("Potencia Direta3: " + potencia3);
            System.out.println("Potencia Refletida3: " + refletida3);
            System.out.println("Temperatura3: " + temperatura3);
            System.out.println("FAN1 MTX3: " + fan1MTX3);
            System.out.println("FAN2 MTX3: " + fan2MTX3);
            System.out.println("offSetMtx3: " + offSetMtx3);

            // MTX4
            WebElement modulator4 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator4___']/input")));
            Thread.sleep(300);
            modulator4.click();

            WebElement getChanelNumber4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator4_Config_UpConverter_ChannelNumber")));
            Thread.sleep(300);
            String chanelNumber4 = getChanelNumber4.getText().trim();

            WebElement getFrequency4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator4_Status_FrequencyChannel")));
            Thread.sleep(300);
            String frequency4 = getFrequency4.getText().trim();

            WebElement powerAmp4 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier4___']/input")));
            Thread.sleep(300);
            powerAmp4.click();

            WebElement getForwardDBM4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Config_OutputPower")));
            Thread.sleep(300);
            String forwardDBM4 = getForwardDBM4.getText().trim();

            WebElement getALC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_AlcStatus")));
            Thread.sleep(300);
            String statusALC4 = getALC4.getText().trim();

            WebElement getOutputDAC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_Measures_output_dac")));
            Thread.sleep(300);
            String outputDAC4 = getOutputDAC4.getText().trim();

            WebElement getDesiredDAC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_Measures_desired_dac")));
            Thread.sleep(300);
            String desiredDAC4 = getDesiredDAC4.getText().trim();

            WebElement getPotencia4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_ForwardPower")));
            Thread.sleep(300);
            String potencia4 = getPotencia4.getText().trim();

            WebElement getRefletida4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_ReflectedPower")));
            Thread.sleep(300);
            String refletida4 = getRefletida4.getText().trim();

            WebElement getTemperatura4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_FanControl_ActualTemperature")));
            Thread.sleep(300);
            String temperatura4 = getTemperatura4.getText().trim();

            WebElement getFan1MTX4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_FanControl_Fan1Speed")));
            Thread.sleep(300);
            String fan1MTX4 = getFan1MTX4.getText().trim();

            WebElement getFan2MTX4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_FanControl_Fan2Speed")));
            Thread.sleep(300);
            String fan2MTX4 = getFan2MTX4.getText().trim();

            // clica em INTERNAL
            WebElement internal4 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Internal4___']/input")));
            Thread.sleep(300);
            internal4.click();

            // pega o OFFSET
            WebElement getOffSetMtx4= wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Internal4_power_offset")));
            Thread.sleep(300);
            String offSetMtx4 = getOffSetMtx4.getText().trim();

            System.out.println("MTX4");
            System.out.println("Numero do canal4: " + chanelNumber4);
            System.out.println("Frequencia4: " + frequency4);
            System.out.println("forwardDBM4: " + forwardDBM4);
            System.out.println("Status ALC4: " + statusALC4);
            System.out.println("Output DAC4: " + outputDAC4);
            System.out.println("Desired DAC4: " + desiredDAC4);
            System.out.println("Potencia Direta4: " + potencia4);
            System.out.println("Potencia Refletida4: " + refletida4);
            System.out.println("Temperatura4: " + temperatura4);
            System.out.println("FAN1 MTX4: " + fan1MTX4);
            System.out.println("FAN2 MTX4: " + fan2MTX4);
            System.out.println("offSetMtx4: " + offSetMtx4);

            // Colocar os valores no resultado
            resultado.put("status", "Checagem executada com sucesso");
            resultado.put("chanelnumber1", chanelNumber1);
            resultado.put("frequency1", frequency1);
            resultado.put("forwardDBM1", forwardDBM1);
            resultado.put("statusAlc1", statusALC1);
            resultado.put("valorOutputDac1", outputDAC1);
            resultado.put("valorDesiredDac1", desiredDAC1);
            resultado.put("potecia1", potencia1);
            resultado.put("refletida1", refletida1);
            resultado.put("temperatura1", temperatura1);
            resultado.put("fan1MTX1", fan1MTX1);
            resultado.put("fan2MTX1", fan2MTX1);
            resultado.put("offSetMtx1", offSetMtx1);

            resultado.put("chanelnumber2", chanelNumber2);
            resultado.put("frequency2", frequency2);
            resultado.put("forwardDBM2", forwardDBM2);
            resultado.put("statusAlc2", statusALC2);
            resultado.put("valorOutputDac2", outputDAC2);
            resultado.put("valorDesiredDac2", desiredDAC2);
            resultado.put("potecia2", potencia2);
            resultado.put("refletida2", refletida2);
            resultado.put("temperatura2", temperatura2);
            resultado.put("fan1MTX2", fan1MTX2);
            resultado.put("fan2MTX2", fan2MTX2);
            resultado.put("offSetMtx2", offSetMtx2);

            resultado.put("chanelnumber3", chanelNumber3);
            resultado.put("frequency3", frequency3);
            resultado.put("forwardDBM3", forwardDBM3);
            resultado.put("statusAlc3", statusALC3);
            resultado.put("valorOutputDac3", outputDAC3);
            resultado.put("valorDesiredDac3", desiredDAC3);
            resultado.put("potecia3", potencia3);
            resultado.put("refletida3", refletida3);
            resultado.put("temperatura3", temperatura3);
            resultado.put("fan1MTX3", fan1MTX3);
            resultado.put("fan2MTX3", fan2MTX3);
            resultado.put("offSetMtx3", offSetMtx3);

            resultado.put("chanelnumber4", chanelNumber4);
            resultado.put("frequency4", frequency4);
            resultado.put("forwardDBM4", forwardDBM4);
            resultado.put("statusAlc4", statusALC4);
            resultado.put("valorOutputDac4", outputDAC4);
            resultado.put("valorDesiredDac4", desiredDAC4);
            resultado.put("potecia4", potencia4);
            resultado.put("refletida4", refletida4);
            resultado.put("temperatura4", temperatura4);
            resultado.put("fan1MTX4", fan1MTX4);
            resultado.put("fan2MTX4", fan2MTX4);
            resultado.put("offSetMtx4", offSetMtx4);

            // Salvar os dados em um arquivo .txt
            String filePath = System.getProperty("user.dir") + "/dados.txt";
            try (FileWriter writer = new FileWriter(filePath)) {
                // MTX1
                writer.write("chanel number1: " + chanelNumber1 + "\n");
                writer.write("frequency1: " + frequency1 + "\n");
                writer.write("forwardDBM1: " + forwardDBM1 + " dBM\n");
                writer.write("Status ALC1: " + statusALC1 + "\n");
                writer.write("Output DAC1: " + outputDAC1 + "\n");
                writer.write("Desired DAC1: " + desiredDAC1 + "\n");
                writer.write("Potencia Direta1: " + potencia1 + "\n");
                writer.write("Potencia Refletida1: " + refletida1 + "\n");
                writer.write("Temperatura1: " + temperatura1 + "\n");
                writer.write("FAN1 MTX1: " + fan1MTX1 + "\n");
                writer.write("FAN2 MTX1: " + fan2MTX1 + "\n");
                writer.write("offSetMtx1: " + offSetMtx1 + "\n");
                writer.write("\n");

                // MTX2
                writer.write("chanel number2: " + chanelNumber2 + "\n");
                writer.write("frequency2: " + frequency2 + "\n");
                writer.write("forwardDBM2: " + forwardDBM2 + " dBM\n");
                writer.write("Status ALC2: " + statusALC2 + "\n");
                writer.write("Output DAC2: " + outputDAC2 + "\n");
                writer.write("Desired DAC2: " + desiredDAC2 + "\n");
                writer.write("Potencia Direta2: " + potencia2 + "\n");
                writer.write("Potencia Refletida2: " + refletida2 + "\n");
                writer.write("Temperatura2: " + temperatura2 + "\n");
                writer.write("FAN1 MTX2: " + fan1MTX2 + "\n");
                writer.write("FAN2 MTX2: " + fan2MTX2 + "\n");
                writer.write("offSetMtx2: " + offSetMtx2 + "\n");
                writer.write("\n");

                // MTX3
                writer.write("chanel number3: " + chanelNumber3 + "\n");
                writer.write("frequency3: " + frequency3 + "\n");
                writer.write("forwardDBM3: " + forwardDBM3 + " dBM\n");
                writer.write("Status ALC3: " + statusALC3 + "\n");
                writer.write("Output DAC3: " + outputDAC3 + "\n");
                writer.write("Desired DAC3: " + desiredDAC3 + "\n");
                writer.write("Potencia Direta3: " + potencia3 + "\n");
                writer.write("Potencia Refletida3: " + refletida3 + "\n");
                writer.write("Temperatura3: " + temperatura3 + "\n");
                writer.write("FAN1 MTX3: " + fan1MTX3 + "\n");
                writer.write("FAN2 MTX3: " + fan2MTX3 + "\n");
                writer.write("offSetMtx3: " + offSetMtx3 + "\n");
                writer.write("\n");

                // MTX4
                writer.write("chanel number4: " + chanelNumber4 + "\n");
                writer.write("frequency4: " + frequency4 + "\n");
                writer.write("forwardDBM4: " + forwardDBM4 + " dBM\n");
                writer.write("Status ALC4: " + statusALC4 + "\n");
                writer.write("Output DAC4: " + outputDAC4 + "\n");
                writer.write("Desired DAC4: " + desiredDAC4 + "\n");
                writer.write("Potencia Direta4: " + potencia4 + "\n");
                writer.write("Potencia Refletida4: " + refletida4 + "\n");
                writer.write("Temperatura4: " + temperatura4 + "\n");
                writer.write("FAN1 MTX4: " + fan1MTX4 + "\n");
                writer.write("FAN2 MTX4: " + fan2MTX4 + "\n");
                writer.write("offSetMtx4: " + offSetMtx4 + "\n");
                writer.write("\n");

                System.out.println("Dados salvos em: " + filePath);
            } catch (IOException e) {
                System.err.println("Erro ao salvar dados: " + e.getMessage());
                resultado.put("erroArquivo", e.getMessage());
            }

        } catch (TimeoutException e) {
            System.err.println("Timeout: " + e.toString());
            resultado.put("status", "Erro: Timeout ao executar checagem");
            resultado.put("erro", e.toString());
        } catch (Exception e) {
            System.err.println("Erro: " + e.toString());
            resultado.put("status", "Erro ao executar checagem");
            resultado.put("erro", e.toString());
        } finally {
            driver.quit();
            System.out.println("Driver finalizado");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(resultado);
        } catch (Exception e) {
            System.err.println("Erro ao gerar JSON: " + e.toString());
            return "{\"erro\":\"Falha ao gerar JSON\"}";
        }
    }

    @GetMapping("/dados.txt")
    public ResponseEntity<Resource> servirDadosTxt() {
        try {
            Path filePath = Paths.get(System.getProperty("user.dir")).resolve("dados.txt");
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .body(resource);
            } else {
                System.out.println("Arquivo dados.txt não encontrado: " + filePath);
                // Cria um arquivo vazio temporário
                try (FileWriter writer = new FileWriter(filePath.toFile())) {
                    writer.write("Aguardando primeira execução...\n");
                }
                Resource newResource = new UrlResource(filePath.toUri());
                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain; charset=UTF-8")
                        .body(newResource);
            }
        } catch (Exception e) {
            System.err.println("Erro ao servir dados.txt: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}