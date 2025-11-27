/*
package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class buscaAutomatica {

@Value("${app.username}")
private String username;

@Value("${app.password}")
private String password;

    @GetMapping("/funcaoAlc")
    public String funcaoAlc(@RequestParam(name = "cidade", required = false) String cidade) {
        System.out.println("=== /funcaoAlc chamado ===");
        System.out.println("Calibração");

        String urlBase = "http://10.10.103.103/login.html";
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        Map<String, Object> resultado = new HashMap<>();

        try {
            driver.get(urlBase);

            // Login
            WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.id("hrs_login_user")));
            campoUsuario.clear();
            campoUsuario.sendKeys(username);

            WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.id("hrs_login_pw")));
            campoSenha.clear();
            campoSenha.sendKeys(password);

            WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.id("hrs_login_loginBtn")));
            botaoLogin.click();

            wait.until(ExpectedConditions.urlContains("mainPage.html"));

            // MTX1
            // 1. Clicar em "Go To"
            WebElement goToBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.btn60x22.link_btn[onclick*='modBtnClick(1)']")));
            goToBtn.click();

            // 2. Clicar em "AMP"
            WebElement buttonAMP1 = wait.until(ExpectedConditions.elementToBeClickable(By.className("smTri_ok")));
            buttonAMP1.click();

            // 3. Pegar Freqência
            WebElement getFrequencia1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("out_freq")));
            String frequencia1 =  getFrequencia1.getText();

            // 4. Pegar o estado do ALC
            WebElement getALC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_alc_stat")));
            Thread.sleep(2000);
            String statusALC1 = getALC1.getText();

            // 5. Pegar o valor do Output DAC
            WebElement getOutputDAC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_out_dac")));
            String outputDAC1 = getOutputDAC1.getText();

            // 6. Pegar o valor do Desired DAC
            WebElement getDesiredDAC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_des_dac")));
            String desiredDAC1 = getDesiredDAC1.getText();

            System.out.println("frequencia1: " + frequencia1);
            System.out.println("Status ALC1: " + statusALC1);
            System.out.println("Output DAC1: " + outputDAC1);
            System.out.println("Desired DAC1: " + desiredDAC1);

            // MTX2
            // 7. Clicar em Home
            WebElement homeBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("firstMenu")));
            homeBtn.click();

            // 8. Clicar no MTX2
            WebElement mtx2 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href=\"javascript:loadModPage('xmtrhome', 2)\"]")));
            mtx2.click();

            // 9. Clicar em "AMP"
            WebElement buttonAMP2 = wait.until(ExpectedConditions.elementToBeClickable(By.className("smTri_ok")));
            buttonAMP2.click();

            // 10. Pegar Freqência
            WebElement getFrequencia2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("out_freq")));
            String frequencia2 =  getFrequencia2.getText();

            // 11. Pegar o estado do ALC
            WebElement getALC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_alc_stat")));
            Thread.sleep(2000);
            String statusALC2 = getALC2.getText();

            // 12. Pegar o valor do Output DAC
            WebElement getOutputDAC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_out_dac")));
            String outputDAC2 = getOutputDAC2.getText();

            // 13. Pegar o valor do Desired DAC
            WebElement getDesiredDAC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_des_dac")));
            String desiredDAC2 = getDesiredDAC2.getText();

            System.out.println("frequencia2: " + frequencia2);
            System.out.println("Status ALC2: " + statusALC2);
            System.out.println("Output DAC2: " + outputDAC2);
            System.out.println("Desired DAC2: " + desiredDAC2);

            // MTX3
            // 14. Clicar em Home
            WebElement homeBtn2 = wait.until(ExpectedConditions.elementToBeClickable(By.id("firstMenu")));
            homeBtn2.click();

            // 15. Clicar no MTX3
            WebElement mtx3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href=\"javascript:loadModPage('xmtrhome', 3)\"]")));
            mtx3.click();

            // 16. Clicar em "AMP"
            WebElement buttonAMP3 = wait.until(ExpectedConditions.elementToBeClickable(By.className("smTri_ok")));
            buttonAMP3.click();

            // 17. Pegar Freqência
            WebElement getFrequencia3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("out_freq")));
            String frequencia3 =  getFrequencia3.getText();

            // 18. Pegar o estado do ALC
            WebElement getALC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_alc_stat")));
            Thread.sleep(2000);
            String statusALC3 = getALC3.getText();

            // 19. Pegar o valor do Output DAC
            WebElement getOutputDAC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_out_dac")));
            String outputDAC3 = getOutputDAC3.getText();

            // 20. Pegar o valor do Desired DAC
            WebElement getDesiredDAC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_des_dac")));
            String desiredDAC3 = getDesiredDAC3.getText();

            System.out.println("frequencia3: " + frequencia3);
            System.out.println("Status ALC3: " + statusALC3);
            System.out.println("Output DAC3: " + outputDAC3);
            System.out.println("Desired DAC3: " + desiredDAC3);

            // MTX4
            // 20. Clicar em Home
            WebElement homeBtn3 = wait.until(ExpectedConditions.elementToBeClickable(By.id("firstMenu")));
            homeBtn3.click();

            // 21. Clicar no MTX4
            WebElement mtx4 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href=\"javascript:loadModPage('xmtrhome', 4)\"]")));
            mtx4.click();

            // 22. Clicar em "AMP"
            WebElement buttonAMP4 = wait.until(ExpectedConditions.elementToBeClickable(By.className("smTri_ok")));
            buttonAMP4.click();

            // 23. Pega o valor da Frequência
            WebElement getFrequencia4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("out_freq")));
            String frequencia4 =  getFrequencia4.getText();

            // 24. Pegar o estado do ALC
            WebElement getALC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_alc_stat")));
            Thread.sleep(2000);
            String statusALC4 = getALC4.getText();

            // 25. Pegar o valor do Output DAC
            WebElement getOutputDAC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_out_dac")));
            String outputDAC4 = getOutputDAC4.getText();

            // 26. Pegar o valor do Desired DAC
            WebElement getDesiredDAC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pa_stat_des_dac")));
            String desiredDAC4 = getDesiredDAC4.getText();

            System.out.println("frequencia4: " + frequencia4);
            System.out.println("Status ALC4: " + statusALC4);
            System.out.println("Output DAC4: " + outputDAC4);
            System.out.println("Desired DAC4: " + desiredDAC4);

            // Colocar os valores no resultado
            resultado.put("status", "Checagem executada com sucesso");
            // MTX1
            resultado.put("frequencia1", frequencia1);
            resultado.put("statusAlc1", statusALC1);
            resultado.put("valorOutputDac1", outputDAC1);
            resultado.put("valorDesiredDac1", desiredDAC1);

            // MTX2
            resultado.put("frequencia2", frequencia2);
            resultado.put("statusAlc2", statusALC2);
            resultado.put("valorOutputDac2", outputDAC2);
            resultado.put("valorDesiredDac2", desiredDAC2);

            // MTX3
            resultado.put("frequencia3", frequencia3);
            resultado.put("statusAlc3", statusALC3);
            resultado.put("valorOutputDac3", outputDAC3);
            resultado.put("valorDesiredDac3", desiredDAC3);

            // MTX4
            resultado.put("frequencia4", frequencia4);
            resultado.put("statusAlc4", statusALC4);
            resultado.put("valorOutputDac4", outputDAC4);
            resultado.put("valorDesiredDac4", desiredDAC4);
            // aqui
            // Salvar os dados em um arquivo .txt NO FORMATO QUE A PLANILHA LÊ
            try (FileWriter writer = new FileWriter("dados.txt")) {

                writer.write("dac|" + frequencia1 + "|" + statusALC1 + "|" + outputDAC1 + "|" + desiredDAC1 + "\n");
                writer.write("dac|" + frequencia2 + "|" + statusALC2 + "|" + outputDAC2 + "|" + desiredDAC2 + "\n");
                writer.write("dac|" + frequencia3 + "|" + statusALC3 + "|" + outputDAC3 + "|" + desiredDAC3 + "\n");
                writer.write("dac|" + frequencia4 + "|" + statusALC4 + "|" + outputDAC4 + "|" + desiredDAC4 + "\n");

                System.out.println("Dados salvos com sucesso em dados.txt (formato compatível com a planilha)");

            } catch (IOException e) {
                System.err.println("Erro ao salvar dados: " + e.getMessage());
            }
            // aqui

            // Salvar os dados em um arquivo .txt
            try (FileWriter writer = new FileWriter("dados.txt")) {

                writer.write("frequencia1: " + frequencia1 + " MHz\n");
                writer.write("Status ALC1: " + statusALC1 + "\n");
                writer.write("Output DAC1: " + outputDAC1 + "\n");
                writer.write("Desired DAC1: " + desiredDAC1 + "\n");
                writer.write("\n");

                writer.write("frequencia2: " + frequencia2 + " MHz\n");
                writer.write("Status ALC2: " + statusALC2 + "\n");
                writer.write("Output DAC2: " + outputDAC2 + "\n");
                writer.write("Desired DAC2: " + desiredDAC2 + "\n");
                writer.write("\n");

                writer.write("frequencia3: " + frequencia3 + " MHz\n");
                writer.write("Status ALC3: " + statusALC3 + "\n");
                writer.write("Output DAC3: " + outputDAC3 + "\n");
                writer.write("Desired DAC3: " + desiredDAC3 + "\n");
                writer.write("\n");

                writer.write("frequencia4: " + frequencia4 + " MHz\n");
                writer.write("Status ALC4: " + statusALC4 + "\n");
                writer.write("Output DAC4: " + outputDAC4 + "\n");
                writer.write("Desired DAC4: " + desiredDAC4 + "\n");
                System.out.println("Dados salvos com sucesso em dados.txt");

            }

        } catch (TimeoutException e) {
            System.err.println("Timeout no /funcaoAlc: " + e.toString());
            resultado.put("status", "Erro: Timeout ao executar checagem");
            resultado.put("erro", e.toString());
        } catch (Exception e) {
            System.err.println("Erro no /funcaoAlc: " + e.toString());
            resultado.put("status", "Erro ao executar checagem");
            resultado.put("erro", e.toString());
        } finally {
            driver.quit();
            System.out.println("Driver finalizado no /funcaoAlc");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(resultado);
        } catch (Exception e) {
            System.err.println("Erro ao gerar JSON no /funcaoAlc: " + e.toString());
            return "{\"erro\":\"Falha ao gerar JSON\"}";
        }
    }

@GetMapping("/funcaoAlcDebug")
public String funcaoAlcDebug(@RequestParam(name = "cidade", required = false) String cidade) {
    System.out.println("=== /funcaoAlc chamado ===");
    System.out.println("Calibração");

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
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    Map<String, Object> resultado = new HashMap<>();

    try {
        driver.get(urlBase);
        // MTX1

        // escreve o usuario (application.properties)
        WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
        campoUsuario.clear();
        campoUsuario.sendKeys(username);

        // escreve a senha (application.properties)
        WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
        campoSenha.clear();
        campoSenha.sendKeys(password);

        // clica no botão de login
        WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
        botaoLogin.click();

        // espera a apagina carregar
        //wait.until(ExpectedConditions.urlContains("page_content.html"));

        // clica em modulador 1
        WebElement modulator1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator1___']/input")));
        modulator1.click();

        // pega o numero do canal
        WebElement getChanelNumber1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Config_UpConverter_ChannelNumber")));
        String chanelNumber1 = getChanelNumber1.getText();

        // pega a frequencia
        WebElement getFrequency1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Status_FrequencyChannel")));
        String frequency1 = getFrequency1.getText();

        // clica em poweramplifier1
        WebElement powerAmp1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier1___']/input")));
        powerAmp1.click();

        // pega o forward dbs
        WebElement getForwardDBM1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Config_OutputPower")));
        String forwardDBM1 =  getForwardDBM1.getText();

        // pega o ALC
        WebElement getALC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_AlcStatus")));
        String statusALC1 = getALC1.getText();

        // pega o output DAC
        WebElement getOutputDAC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_Measures_output_dac")));
        String outputDAC1 = getOutputDAC1.getText();

        // pega o desired DAC
        WebElement getDesiredDAC1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_Measures_desired_dac")));
        String desiredDAC1 = getDesiredDAC1.getText();

        System.out.println("Dados extraídos:");
        System.out.println("MTX1");
        System.out.println("Numero do canal1: " + chanelNumber1);
        System.out.println("Frequencia1: "+ frequency1);
        System.out.println("forwardDBM1: " + forwardDBM1);
        System.out.println("Status ALC1: " + statusALC1);
        System.out.println("Output DAC1: " + outputDAC1);
        System.out.println("Desired DAC1: " + desiredDAC1);

        // MTX2

        // clica em modulador 2
        WebElement modulator2 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator2___']/input")));
        modulator2.click();

        // pega o numero do canal
        WebElement getChanelNumber2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator2_Config_UpConverter_ChannelNumber")));
        String chanelNumber2 = getChanelNumber2.getText();

        // pega a frequencia
        WebElement getFrequency2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator2_Status_FrequencyChannel")));
        String frequency2 = getFrequency2.getText();

        // clica em poweramplifier2
        WebElement powerAmp2= wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier2___']/input")));
        powerAmp2.click();

        // pega o forward dbs
        WebElement getForwardDBM2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Config_OutputPower")));
        String forwardDBM2 =  getForwardDBM2.getText();

        // pega o ALC
        WebElement getALC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_AlcStatus")));
        String statusALC2 = getALC2.getText();

        // pega o output DAC
        WebElement getOutputDAC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_Measures_output_dac")));
        String outputDAC2 = getOutputDAC2.getText();

        // pega o desired DAC
        WebElement getDesiredDAC2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier2_Status_Measures_desired_dac")));
        String desiredDAC2 = getDesiredDAC2.getText();

        System.out.println("MTX2");
        System.out.println("Numero do canal2: " + chanelNumber2);
        System.out.println("Frequencia2: "+ frequency2);
        System.out.println("forwardDBM2: " + forwardDBM2);
        System.out.println("Status ALC2: " + statusALC2);
        System.out.println("Output DAC2: " + outputDAC2);
        System.out.println("Desired DAC2: " + desiredDAC2);

        // MTX3

        // clica em modulador 3
        WebElement modulator3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator3___']/input")));
        modulator3.click();

        // pega o numero do canal
        WebElement getChanelNumber3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator3_Config_UpConverter_ChannelNumber")));
        String chanelNumber3 = getChanelNumber3.getText();

        // pega a frequencia
        WebElement getFrequency3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator3_Status_FrequencyChannel")));
        String frequency3 = getFrequency3.getText();

        // clica em poweramplifier3
        WebElement powerAmp3 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier3___']/input")));
        powerAmp3.click();

        // pega o forward dbs
        WebElement getForwardDBM3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Config_OutputPower")));
        String forwardDBM3 =  getForwardDBM3.getText();

        // pega o ALC
        WebElement getALC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_AlcStatus")));
        String statusALC3 = getALC3.getText();

        // pega o output DAC
        WebElement getOutputDAC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_Measures_output_dac")));
        String outputDAC3 = getOutputDAC3.getText();

        // pega o desired DAC
        WebElement getDesiredDAC3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier3_Status_Measures_desired_dac")));
        String desiredDAC3 = getDesiredDAC3.getText();

        System.out.println("MTX3");
        System.out.println("Numero do canal3: " + chanelNumber3);
        System.out.println("Frequencia3: "+ frequency3);
        System.out.println("forwardDBM3: " + forwardDBM3);
        System.out.println("Status ALC3: " + statusALC3);
        System.out.println("Output DAC3: " + outputDAC3);
        System.out.println("Desired DAC3: " + desiredDAC3);

        // MTX4

        // clica em modulador 4
        WebElement modulator4 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator4___']/input")));
        modulator4.click();

        // pega o numero do canal
        WebElement getChanelNumber4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator4_Config_UpConverter_ChannelNumber")));
        String chanelNumber4 = getChanelNumber4.getText();

        // pega a frequencia
        WebElement getFrequency4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator4_Status_FrequencyChannel")));
        String frequency4 = getFrequency4.getText();

        // clica em poweramplifier4
        WebElement powerAmp4= wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='PowerAmplifier4___']/input")));
        powerAmp4.click();

        // pega o forward dbs
        WebElement getForwardDBM4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Config_OutputPower")));
        String forwardDBM4 = getForwardDBM4.getText();

        // pega o ALC
        WebElement getALC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_AlcStatus")));
        String statusALC4 = getALC4.getText();

        // pega o output DAC
        WebElement getOutputDAC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_Measures_output_dac")));
        String outputDAC4 = getOutputDAC4.getText();

        // pega o desired DAC
        WebElement getDesiredDAC4 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier4_Status_Measures_desired_dac")));
        String desiredDAC4 = getDesiredDAC4.getText();

        System.out.println("MTX4");
        System.out.println("Numero do canal4: " + chanelNumber4);
        System.out.println("Frequencia4: "+ frequency4);
        System.out.println("forwardDBM4: " + forwardDBM4);
        System.out.println("Status ALC4: " + statusALC4);
        System.out.println("Output DAC4: " + outputDAC4);
        System.out.println("Desired DAC4: " + desiredDAC4);


        // Colocar os valores no resultado
        resultado.put("status", "Checagem executada com sucesso");
        resultado.put("MTX1", " ");
        resultado.put("chanelnumber1", chanelNumber1);
        resultado.put("frequency1", frequency1);
        resultado.put("forwardDBM1", forwardDBM1);
        resultado.put("statusAlc1", statusALC1);
        resultado.put("valorOutputDac1", outputDAC1);
        resultado.put("valorDesiredDac1", desiredDAC1);

        resultado.put("MTX2", " ");
        resultado.put("chanelnumber2", chanelNumber2);
        resultado.put("frequency2", frequency2);
        resultado.put("forwardDBM2", forwardDBM2);
        resultado.put("statusAlc2", statusALC2);
        resultado.put("valorOutputDac2", outputDAC2);
        resultado.put("valorDesiredDac2", desiredDAC2);

        resultado.put("MTX3", " ");
        resultado.put("chanelnumber3", chanelNumber3);
        resultado.put("frequency3", frequency3);
        resultado.put("forwardDBM3", forwardDBM3);
        resultado.put("statusAlc3", statusALC3);
        resultado.put("valorOutputDac3", outputDAC3);
        resultado.put("valorDesiredDac3", desiredDAC3);

        resultado.put("MTX4", " ");
        resultado.put("chanelnumber4", chanelNumber4);
        resultado.put("frequency4", frequency4);
        resultado.put("forwardDBM4", forwardDBM4);
        resultado.put("statusAlc4", statusALC4);
        resultado.put("valorOutputDac4", outputDAC4);
        resultado.put("valorDesiredDac4", desiredDAC4);

        // Salvar os dados em um arquivo .txt (caminho melhorado)
        String filePath = System.getProperty("user.dir") + "/dados.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            //   MTX1
            writer.write("chanel number1: "+ chanelNumber1 + "\n");
            writer.write("frequency1: " + frequency1 + "\n");
            writer.write("forwardDBM1: " + forwardDBM1 + " dBM\n");
            writer.write("Status ALC1: " + statusALC1 + "\n");
            writer.write("Output DAC1: " + outputDAC1 + "\n");
            writer.write("Desired DAC1: " + desiredDAC1 + "\n");
            writer.write("\n");

            // MTX2
            writer.write("chanel number2: "+ chanelNumber2 + "\n");
            writer.write("frequency2: " + frequency2 + "\n");
            writer.write("forwardDBM2: " + forwardDBM2 + " dBM\n");
            writer.write("Status ALC2: " + statusALC2 + "\n");
            writer.write("Output DAC2: " + outputDAC2 + "\n");
            writer.write("Desired DAC2: " + desiredDAC2 + "\n");
            writer.write("\n");

            // MTX2
            writer.write("chanel number3: "+ chanelNumber3 + "\n");
            writer.write("frequency3: " + frequency3 + "\n");
            writer.write("forwardDBM3: " + forwardDBM3 + " dBM\n");
            writer.write("Status ALC3: " + statusALC3 + "\n");
            writer.write("Output DAC3: " + outputDAC3 + "\n");
            writer.write("Desired DAC3: " + desiredDAC3 + "\n");
            writer.write("\n");

            // MTX2
            writer.write("chanel number4: "+ chanelNumber4 + "\n");
            writer.write("frequency4: " + frequency4 + "\n");
            writer.write("forwardDBM4: " + forwardDBM4 + " dBM\n");
            writer.write("Status ALC4: " + statusALC4 + "\n");
            writer.write("Output DAC4: " + outputDAC4 + "\n");
            writer.write("Desired DAC4: " + desiredDAC4 + "\n");
            writer.write("\n");

            System.out.println("Dados salvos em: " + filePath);
        } catch (IOException e) {
            System.err.println("Erro ao salvar dados: " + e.getMessage());
            resultado.put("erroArquivo", e.getMessage());
        }

    } catch (TimeoutException e) {
        System.err.println("Timeout no /funcaoAlc: " + e.toString());
        resultado.put("status", "Erro: Timeout ao executar checagem");
        resultado.put("erro", e.toString());
    } catch (Exception e) {
        System.err.println("Erro no /funcaoAlc: " + e.toString());
        resultado.put("status", "Erro ao executar checagem");
        resultado.put("erro", e.toString());
    } finally {
        driver.quit();
        System.out.println("Driver finalizado no /funcaoAlc");
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(resultado);
    } catch (Exception e) {
        System.err.println("Erro ao gerar JSON no /funcaoAlc: " + e.toString());
        return "{\"erro\":\"Falha ao gerar JSON\"}";
    }
}

@GetMapping("/funcaoAlcTeste")
public String funcaoAlcTeste(@RequestParam(name = "cidade", required = false) String cidade) {
    System.out.println("=== /funcaoAlcTeste chamado ===");
    System.out.println("Calibração");

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
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    Map<String, Object> resultado = new HashMap<>();

    try {
        driver.get(urlBase);

        WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
        campoUsuario.clear();
        campoUsuario.sendKeys(username);

        WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
        campoSenha.clear();
        campoSenha.sendKeys(password);

        WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
        botaoLogin.click();

        //wait.until(ExpectedConditions.urlContains("page_content.html"));
        // ele clica onde ele não ve checar isso


        // 1. clica ana aba do modulador
        WebElement botaoModulator1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[text()='EdiEti1']")));
        botaoModulator1.click();

        // 2. clica no numero do canal
        WebElement getChanelNumber = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Config_UpConverter_ChannelNumber")));
        String chanelNumber =  getChanelNumber.getText();

        // 3. clica na frequencia
        WebElement getFrequency = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Config_UpConverter_ChannelFrequency")));
        String frequency =  getFrequency.getText();

        System.out.println("Dados extraídos:");
        System.out.println("Numero do canal: " + chanelNumber);
        System.out.println("Frequencia: " + frequency);

        // Colocar os valores no resultado
        resultado.put("status", "Checagem executada com sucesso");
        resultado.put("Numero do canal", chanelNumber);
        resultado.put("Frequencia", frequency);

        String filePath = System.getProperty("user.dir") + "/teste.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Numero do canal: " + chanelNumber + " \n");
            writer.write("Frequencia: " + frequency + " \n");
            writer.write("\n");
            System.out.println("Dados salvos em: " + filePath);
        } catch (IOException e) {
            System.err.println("Erro ao salvar dados: " + e.getMessage());
            resultado.put("erroArquivo", e.getMessage());
        }

    } catch (TimeoutException e) {
        System.err.println("Timeout no /funcaoAlcTeste: " + e.toString());
        resultado.put("status", "Erro: Timeout ao executar checagem");
        resultado.put("erro", e.toString());
    } catch (Exception e) {
        System.err.println("Erro no /funcaoAlcTeste: " + e.toString());
        resultado.put("status", "Erro ao executar checagem");
        resultado.put("erro", e.toString());
    } finally {
        driver.quit();
        System.out.println("Driver finalizado no /funcaoAlcTeste");
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(resultado);
    } catch (Exception e) {
        System.err.println("Erro ao gerar JSON no /funcaoAlcTeste: " + e.toString());
        return "{\"erro\":\"Falha ao gerar JSON\"}";
    }
}

@GetMapping("/funcaoAlcTeste1")
public String funcaoAlcTeste1(@RequestParam(name = "cidade", required = false) String cidade) {
    System.out.println("=== /funcaoAlcTeste chamado ===");
    System.out.println("Calibração");

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
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    Map<String, Object> resultado = new HashMap<>();

    try {
        driver.get(urlBase);

        WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='text']")));
        campoUsuario.clear();
        campoUsuario.sendKeys(username);

        WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='password']")));
        campoSenha.clear();
        campoSenha.sendKeys(password);

        WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Sign In']")));
        botaoLogin.click();

        // Novo passo: Clique no radio button para Modulator1
        WebElement radioModulator1 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[@link='Modulator1___']/input")));
        radioModulator1.click();
        System.out.println("Radio button Modulator1 selecionado.");

        // 2. Clica no numero do canal
        WebElement getChanelNumber = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Config_UpConverter_ChannelNumber")));
        String chanelNumber = getChanelNumber.getText();

        // 3. Clica na frequencia
        WebElement getFrequency = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Modulator1_Config_UpConverter_ChannelFrequency")));
        String frequency = getFrequency.getText();

        System.out.println("Dados extraídos:");
        System.out.println("Numero do canal: " + chanelNumber);
        System.out.println("Frequencia: " + frequency);

        // Colocar os valores no resultado
        resultado.put("status", "Checagem executada com sucesso");
        resultado.put("Numero do canal", chanelNumber);
        resultado.put("Frequencia", frequency);

        String filePath = System.getProperty("user.dir") + "/teste.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Numero do canal: " + chanelNumber + " \n");
            writer.write("Frequencia: " + frequency + " \n");
            writer.write("\n");
            System.out.println("Dados salvos em: " + filePath);
        } catch (IOException e) {
            System.err.println("Erro ao salvar dados: " + e.getMessage());
            resultado.put("erroArquivo", e.getMessage());
        }

    } catch (TimeoutException e) {
        System.err.println("Timeout no /funcaoAlcTeste: " + e.toString());
        resultado.put("status", "Erro: Timeout ao executar checagem");
        resultado.put("erro", e.toString());
    } catch (Exception e) {
        System.err.println("Erro no /funcaoAlcTeste: " + e.toString());
        resultado.put("status", "Erro ao executar checagem");
        resultado.put("erro", e.toString());
    } finally {
        driver.quit();
        System.out.println("Driver finalizado no /funcaoAlcTeste");
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(resultado);
    } catch (Exception e) {
        System.err.println("Erro ao gerar JSON no /funcaoAlcTeste: " + e.toString());
        return "{\"erro\":\"Falha ao gerar JSON\"}";
    }
}

@GetMapping("/funcaoAlterarDAC")
public String funcaoAlterarDAC(@RequestParam(name = "novoDesiredDac", required = false) String novoDesiredDac) {
    System.out.println("=== /funcaoAlterarDAC chamado ===");
    System.out.println("Alteração de DAC no debug");

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
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    Map<String, Object> resultado = new HashMap<>();

    try {
        driver.get(urlBase);

        // Login (corrigido: assumindo classes em vez de IDs inválidos)
        WebElement campoUsuario = wait.until(ExpectedConditions.elementToBeClickable(By.className("username")));
        campoUsuario.clear();
        campoUsuario.sendKeys(username);

        WebElement campoSenha = wait.until(ExpectedConditions.elementToBeClickable(By.className("password")));
        campoSenha.clear();
        campoSenha.sendKeys(password);

        WebElement botaoLogin = wait.until(ExpectedConditions.elementToBeClickable(By.className("input"))); // Assumindo botão com class="input"
        botaoLogin.click();

        wait.until(ExpectedConditions.urlContains("debug")); // Ajuste se a URL muda

        // 1. Navegar para seção de debug
        WebElement debugBtn = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("PowerAmplifier1___"))); // Corrigido
        System.out.println("Clicando em PowerAmplifier1___...");
        debugBtn.click();

        // 2. Output DAC
        WebElement getOutputDAC = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_Measures_output_dac")));
        String outputDAC = getOutputDAC.getText();

        // 3. Desired DAC
        WebElement getDesiredDAC = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("PowerAmplifier1_Status_Measures_desired_dac")));
        String desiredDACAtual = getDesiredDAC.getText();

        // Colocar os valores no resultado
        resultado.put("status", "Operação no debug executada com sucesso");
        resultado.put("valorOutputDac", outputDAC);
        resultado.put("valorDesiredDacAtual", desiredDACAtual);

    } catch (TimeoutException e) {
        System.err.println("Timeout no /funcaoAlterarDAC: " + e.toString());
        resultado.put("status", "Erro: Timeout ao executar alteração");
        resultado.put("erro", e.toString());
    } catch (Exception e) {
        System.err.println("Erro no /funcaoAlterarDAC: " + e.toString());
        resultado.put("status", "Erro ao executar alteração");
        resultado.put("erro", e.toString());
    } finally {
        driver.quit();
        System.out.println("Driver finalizado no /funcaoAlterarDAC");
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(resultado);
    } catch (Exception e) {
        System.err.println("Erro ao gerar JSON no /funcaoAlterarDAC: " + e.toString());
        return "{\"erro\":\"Falha ao gerar JSON\"}";
    }
}
}
*/
