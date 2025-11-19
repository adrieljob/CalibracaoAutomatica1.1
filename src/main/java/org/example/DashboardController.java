/*
package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController { // Renomeado de DashboardController_orgn

    @GetMapping("/dashboard/{idCidade}")
    public String acessarDashboard(@PathVariable("idCidade") String idCidade) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        Map<String, Object> resultadoFinal = new HashMap<>();

        try {
            String urlBase = "https://pdb.nemesys.cloud/dashboards/";
            driver.get(urlBase);
            driver.manage().window().maximize();

            // --- LOGIN ---
            WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("#nemesys\\.username")
            ));
            WebElement campoSenha = driver.findElement(By.cssSelector("#nemesys\\.password"));
            WebElement botaoLogin = driver.findElement(By.cssSelector(
                    "body > div > div.relative.h-screen.w-screen.overflow-hidden > div > div > form > div.card-actions.justify-center"
            ));
            campoUsuario.sendKeys("foccus.ead");
            campoSenha.sendKeys("foccus123");
            botaoLogin.click();

            // --- Espera redirecionamento/login ---
            Thread.sleep(3000); // ou usar wait.until para elemento do dashboard

            // --- Acessa dashboard da cidade ---
            String urlCidade = urlBase + idCidade;
            driver.get(urlCidade);

            WebElement tituloDashboard = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("#dashboard-nav-title > h1")
            ));
            resultadoFinal.put("cidade", tituloDashboard.getText());

            // --- Mapa para todos os MTXs ---
            Map<String, Object> mtxsMap = new HashMap<>();

            // --- Adiciona todos os MTXs ---
            adicionarMetricasMTX(driver, mtxsMap, "MTX1",
                    new String[]{"#grid-stack-item-7a4ce326-9795-4f4a-b4b9-91c802ae2fdb",
                            "#grid-stack-item-13db5c5d-195a-4684-84ca-2c57741a364e"},
                    new String[][]{
                            {"Potência Direta MTX1", "Pot. Refletida MTX1", "MER MTX1", "Resumo Falhas MTX1", "Conexão LAN MTX1"},
                            {"Conexão LAN MUX", "RX Sat 1 Lock MTX1", "RX Sat 1 C/N MTX1", "RX Sat 2 Lock MTX1", "RX Sat 2 C/N MTX1"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX2",
                    new String[]{"#grid-stack-item-d8befd9f-cb47-4e2e-a71e-9ff325f6f2bf",
                            "#grid-stack-item-05cd22c6-1b83-4039-8a57-3eb3dd6c9897"},
                    new String[][]{
                            {"Potência Direta MTX2", "Pot. Refletida MTX2", "MER MTX2", "Resumo Falhas MTX2", "Conexão LAN MTX2"},
                            {"Input Lock MTX2", "Input C/N MTX2"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX3",
                    new String[]{"#grid-stack-item-b0fa60b1-18e7-41ad-9468-88e5c2640534",
                            "#grid-stack-item-4160c3ba-2b72-40d7-8083-b3800e73c112"},
                    new String[][]{
                            {"Potência Direta MTX3", "Pot. Refletida MTX3", "MER MTX3", "Resumo Falhas MTX3", "Conexão LAN MTX3"},
                            {"Input Lock MTX3", "Input C/N MTX3"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX4",
                    new String[]{"#grid-stack-item-7307ecbe-7944-48e4-a1cf-c15c06a95d44",
                            "#grid-stack-item-65823c44-b822-4be3-96e9-0d774f5a8d6e"},
                    new String[][]{
                            {"Potência Direta MTX4", "Pot. Refletida MTX4", "MER MTX4", "Resumo Falhas MTX4", "Conexão LAN MTX4"},
                            {"Input Lock MTX4", "Input C/N MTX4"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX5",
                    new String[]{"#grid-stack-item-9ac63e71-cb7c-4285-b063-e7984576a76f",
                            "#grid-stack-item-bd064d32-6274-4bdb-8428-e3853c118aae"},
                    new String[][]{
                            {"Potência Direta MTX5", "Pot. Refletida MTX5", "MER MTX5", "Resumo Falhas MTX5", "Conexão LAN MTX5"},
                            {"Input Lock MTX5", "Input C/N MTX5"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX6",
                    new String[]{"#grid-stack-item-e64ff831-6cc9-448f-91c9-14503bd5a2f3",
                            "#grid-stack-item-7903de0e-57ed-4f35-8c67-30cec3e1117f"},
                    new String[][]{
                            {"Potência Direta MTX6", "Pot. Refletida MTX6", "MER MTX6", "Resumo Falhas MTX6", "Conexão LAN MTX6"},
                            {"Input Lock MTX6", "Input C/N MTX6"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX7",
                    new String[]{"#grid-stack-item-2838a358-cea1-468b-87f8-8a77e2b1a88e",
                            "#grid-stack-item-10f1fd95-ad5e-49c3-87fe-288dcab4119f"},
                    new String[][]{
                            {"Potência Direta MTX7", "Pot. Refletida MTX7", "MER MTX7", "Resumo Falhas MTX7", "Conexão LAN MTX7"},
                            {"Input Lock MTX7", "Input C/N MTX7"}
                    });

            adicionarMetricasMTX(driver, mtxsMap, "MTX8",
                    new String[]{"#grid-stack-item-c0e7a9ea-5dd5-4fe7-9dec-915d968cdbe9",
                            "#grid-stack-item-1f0f1d85-0961-4803-acbd-509a37c028c4"},
                    new String[][]{
                            {"Potência Direta MTX8", "Pot. Refletida MTX8", "MER MTX8", "Resumo Falhas MTX8", "Conexão LAN MTX8"},
                            {"Input Lock MTX8", "Input C/N MTX8"}
                    });

            resultadoFinal.put("MTXs", mtxsMap);

            // --- Outros grupos ---
            Map<String, Object> outrosMap = new HashMap<>();
            adicionarMetricasMTX(driver, outrosMap, "Nobreak",
                    new String[]{"#grid-stack-item-3fd16ae8-5757-4034-a1c3-5bdde4d21173"},
                    new String[][]{
                            {"Operação Nobreak", "Comunicação Nobreak", "Tensão Bateria Nobreak", "Potência Consumida"}
                    });

            adicionarMetricasMTX(driver, outrosMap, "Temperatura",
                    new String[]{"#grid-stack-item-7c6a9fe3-6659-43c8-85d1-74c21544578a"},
                    new String[][]{
                            {"Temperatura Shelter", "Ar Condicionado 01", "Ar Condicionado 02", "Tensão Bateria FLEX", "Taxa de Perda de Pacotes"}
                    });

            adicionarMetricasMTX(driver, outrosMap, "Segurança",
                    new String[]{"#grid-stack-item-27772b90-58eb-45e7-a910-7a86bd191524"},
                    new String[][]{
                            {"Rack Porta Frontal", "Rack Porta Traseira", "Sensor Movimento", "Tensão Bateria FLEX", "Taxa de Perda de Pacotes"}
                    });

            resultadoFinal.put("Outros", outrosMap);


        } catch (Exception e) {
            e.printStackTrace();
            resultadoFinal.put("erro", e.toString());
        } finally {
            driver.quit();
        }

        // --- Formata JSON bonito ---
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(resultadoFinal);
        } catch (Exception e) {
            return "{\"erro\":\"Falha ao formatar JSON\"}";
        }
    }

    // --- Método genérico para adicionar métricas de um MTX ---
    private void adicionarMetricasMTX(WebDriver driver, Map<String, Object> mtxsMap,
                                      String nomeMTX, String[] idsContainers, String[][] metricasPorContainer) {
        Map<String, String> mtx = new HashMap<>();
        for (int i = 0; i < idsContainers.length; i++) {
            mtx.putAll(pegarMetricas(driver, idsContainers[i], metricasPorContainer[i]));
        }
        mtxsMap.put(nomeMTX, mtx);
    }

    // --- Método para pegar métricas de um container ---
    private Map<String, String> pegarMetricas(WebDriver driver, String containerId, String[] metricas) {
        Map<String, String> dados = new HashMap<>();
        try {
            WebElement container = driver.findElement(By.cssSelector(containerId));

            for (String metrica : metricas) {
                try {
                    WebElement celula = container.findElement(
                            By.xpath(".//td[preceding-sibling::th/a[text()='" + metrica + "']]")
                    );
                    int tentativas = 0;
                    while (celula.getText().isEmpty() && tentativas < 5) {
                        Thread.sleep(1000);
                        tentativas++;
                    }
                    dados.put(metrica, celula.getText().isEmpty() ? "N/A" : celula.getText());
                } catch (Exception e) {
                    dados.put(metrica, "N/A");
                }
            }
        } catch (Exception e) {
            for (String metrica : metricas) {
                dados.put(metrica, "N/A");
            }
        }
        return dados;
    }
}

 */