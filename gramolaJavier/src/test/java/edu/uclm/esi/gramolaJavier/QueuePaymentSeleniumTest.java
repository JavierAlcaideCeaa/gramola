package edu.uclm.esi.gramolaJavier;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import edu.uclm.esi.gramolaJavier.Dao.QueuePaymentTransactionDao;
import edu.uclm.esi.gramolaJavier.models.QueuePaymentTransaction;

import java.time.Duration;
import java.util.List;

/**
 * Tests de Selenium para los escenarios de pago de cola
 * 
 * ESCENARIO 1: Cliente busca canción, paga correctamente y se encola
 * ESCENARIO 2: Cliente busca canción, pone mal los datos de pago y se produce error
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class QueuePaymentSeleniumTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    
    @Autowired
    private QueuePaymentTransactionDao queuePaymentTransactionDao;
    
    private static final String BASE_URL = "http://127.0.0.1:4200";
    private static final String TEST_EMAIL = "alcaidejavier6@gmail.com";
    private static final String TEST_PASSWORD = "120230";
    private static final String CUSTOMER_NAME = "Javier Test";
    
    @BeforeAll
    public static void setupClass() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }
    
    @AfterAll
    public static void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * ESCENARIO 1: Pago exitoso
     * 
     * 1. Login con cuenta de test
     * 2. Buscar una canción
     * 3. Hacer clic en "Añadir" (2.99€)
     * 4. Ingresar nombre del cliente
     * 5. Ingresar datos de tarjeta correctos
     * 6. Confirmar pago
     * 7. Verificar en BD que el pago se confirmó
     * 8. Verificar que la canción está guardada en la lista del backend
     */
    @Test
    @Order(1)
    public void testPaymentSuccess() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🧪 ESCENARIO 1: PAGO EXITOSO");
        System.out.println("═══════════════════════════════════════════════════════");
        
        // Paso 1: Login
        System.out.println("1️⃣ Navegando al login...");
        driver.get(BASE_URL + "/login");
        Thread.sleep(2000);
        
        WebElement emailInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='email']"))
        );
        WebElement passwordInput = driver.findElement(By.cssSelector("input[type='password']"));
        
        emailInput.sendKeys(TEST_EMAIL);
        passwordInput.sendKeys(TEST_PASSWORD);
        
        WebElement loginButton = driver.findElement(By.cssSelector("button.login-button"));
        loginButton.click();
        
        System.out.println("✅ Login realizado");
        Thread.sleep(3000);
        
        // Paso 2: Buscar canción
        System.out.println("2️⃣ Buscando canción...");
        WebElement searchButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("button.fab-button"))
        );
        searchButton.click();
        Thread.sleep(1000);
        
        WebElement searchInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".search-input"))
        );
        searchInput.sendKeys("Hola Perdida");
        Thread.sleep(2000);
        
        System.out.println("✅ Canción buscada");
        
        // Paso 3: Hacer clic en "Añadir" (2.99€)
        System.out.println("3️⃣ Haciendo clic en Añadir...");
        WebElement addButton = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Añadir') and contains(text(), '2.99')]")
            )
        );
        addButton.click();
        Thread.sleep(2000);
        
        System.out.println("✅ Modal de pago abierto");
        
        // Paso 4: Ingresar nombre del cliente
        System.out.println("4️⃣ Ingresando nombre del cliente...");
        WebElement nameInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input.name-input, input[placeholder*='nombre']")
            )
        );
        nameInput.clear();
        nameInput.sendKeys(CUSTOMER_NAME);
        Thread.sleep(1000);
        
        System.out.println("✅ Nombre ingresado: " + CUSTOMER_NAME);
        
        // Paso 5: Hacer clic en "Pagar"
        System.out.println("5️⃣ Haciendo clic en Pagar...");
        WebElement payButton = driver.findElement(
            By.xpath("//button[contains(text(), 'Pagar') and contains(text(), '2.99')]")
        );
        payButton.click();
        Thread.sleep(3000);
        
        // Paso 6: Ingresar datos de tarjeta (Stripe test card)
        System.out.println("6️⃣ Ingresando datos de tarjeta...");
        
        // Cambiar al iframe de Stripe
        driver.switchTo().frame(driver.findElement(By.cssSelector("iframe[name^='__privateStripeFrame']")));
        
        WebElement cardNumber = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.name("cardnumber"))
        );
        cardNumber.sendKeys("4242424242424242"); // Tarjeta de prueba Stripe
        
        WebElement expiry = driver.findElement(By.name("exp-date"));
        expiry.sendKeys("1228"); // 12/28
        
        WebElement cvc = driver.findElement(By.name("cvc"));
        cvc.sendKeys("123");
        
        // Volver al contenido principal
        driver.switchTo().defaultContent();
        Thread.sleep(1000);
        
        System.out.println("✅ Datos de tarjeta ingresados");
        
        // Paso 7: Confirmar pago
        System.out.println("7️⃣ Confirmando pago...");
        WebElement confirmButton = driver.findElement(
            By.xpath("//button[contains(text(), 'Confirmar Pago') or contains(text(), '✅')]")
        );
        confirmButton.click();
        
        // Esperar a que se procese el pago
        Thread.sleep(8000);
        
        System.out.println("✅ Pago procesado");
        
        // Paso 8: Verificar en base de datos
        System.out.println("8️⃣ Verificando en base de datos...");
        Thread.sleep(2000);
        
        List<QueuePaymentTransaction> transactions = 
            queuePaymentTransactionDao.findByEmailAndStatus(TEST_EMAIL, "completed");
        
        Assertions.assertFalse(transactions.isEmpty(), 
            "❌ No se encontró ninguna transacción completada en la BD");
        
        QueuePaymentTransaction lastTransaction = transactions.get(transactions.size() - 1);
        
        Assertions.assertEquals("completed", lastTransaction.getStatus(), 
            "❌ El estado de la transacción no es 'completed'");
        
        Assertions.assertEquals(CUSTOMER_NAME, lastTransaction.getCustomerName(), 
            "❌ El nombre del cliente no coincide");
        
        Assertions.assertNotNull(lastTransaction.getTrackName(), 
            "❌ No se guardó el nombre de la canción");
        
        Assertions.assertNotNull(lastTransaction.getArtistName(), 
            "❌ No se guardó el nombre del artista");
        
        Assertions.assertEquals(299L, lastTransaction.getPriceCents(), 
            "❌ El precio no es correcto");
        
        System.out.println("✅ Transacción verificada en BD:");
        System.out.println("   - ID: " + lastTransaction.getId());
        System.out.println("   - Estado: " + lastTransaction.getStatus());
        System.out.println("   - Cliente: " + lastTransaction.getCustomerName());
        System.out.println("   - Canción: " + lastTransaction.getTrackName());
        System.out.println("   - Artista: " + lastTransaction.getArtistName());
        System.out.println("   - Precio: " + (lastTransaction.getPriceCents() / 100.0) + "€");
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("✅ ESCENARIO 1 COMPLETADO EXITOSAMENTE");
        System.out.println("═══════════════════════════════════════════════════════");
    }
    
    /**
     * ESCENARIO 2: Pago con error (datos incorrectos)
     * 
     * 1. Login con cuenta de test
     * 2. Buscar una canción
     * 3. Hacer clic en "Añadir"
     * 4. Ingresar nombre del cliente
     * 5. Ingresar datos de tarjeta INCORRECTOS
     * 6. Verificar que aparece mensaje de error
     * 7. Verificar que NO se guardó en BD como "completed"
     */
    @Test
    @Order(2)
    public void testPaymentError() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🧪 ESCENARIO 2: PAGO CON ERROR");
        System.out.println("═══════════════════════════════════════════════════════");
        
        // Contar transacciones antes del test
        long transactionsBeforeTest = queuePaymentTransactionDao.count();
        
        // Paso 1: Volver al dashboard (ya estamos logueados)
        System.out.println("1️⃣ Navegando al dashboard...");
        driver.get(BASE_URL + "/dashboard");
        Thread.sleep(3000);
        
        // Paso 2: Buscar canción
        System.out.println("2️⃣ Buscando canción...");
        WebElement searchButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("button.fab-button"))
        );
        searchButton.click();
        Thread.sleep(1000);
        
        WebElement searchInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".search-input"))
        );
        searchInput.sendKeys("Sweet");
        Thread.sleep(2000);
        
        System.out.println("✅ Canción buscada");
        
        // Paso 3: Hacer clic en "Añadir"
        System.out.println("3️⃣ Haciendo clic en Añadir...");
        WebElement addButton = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Añadir') and contains(text(), '2.99')]")
            )
        );
        addButton.click();
        Thread.sleep(2000);
        
        // Paso 4: Ingresar nombre del cliente
        System.out.println("4️⃣ Ingresando nombre del cliente...");
        WebElement nameInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input.name-input, input[placeholder*='nombre']")
            )
        );
        nameInput.clear();
        nameInput.sendKeys("Cliente Error");
        Thread.sleep(1000);
        
        // Paso 5: Hacer clic en "Pagar"
        System.out.println("5️⃣ Haciendo clic en Pagar...");
        WebElement payButton = driver.findElement(
            By.xpath("//button[contains(text(), 'Pagar')]")
        );
        payButton.click();
        Thread.sleep(3000);
        
        // Paso 6: Ingresar datos de tarjeta INCORRECTOS
        System.out.println("6️⃣ Ingresando datos de tarjeta INCORRECTOS...");
        
        driver.switchTo().frame(driver.findElement(By.cssSelector("iframe[name^='__privateStripeFrame']")));
        
        WebElement cardNumber = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.name("cardnumber"))
        );
        cardNumber.sendKeys("4000000000000002"); // Tarjeta que siempre falla en Stripe
        
        WebElement expiry = driver.findElement(By.name("exp-date"));
        expiry.sendKeys("1228");
        
        WebElement cvc = driver.findElement(By.name("cvc"));
        cvc.sendKeys("123");
        
        driver.switchTo().defaultContent();
        Thread.sleep(1000);
        
        System.out.println("✅ Datos incorrectos ingresados");
        
        // Paso 7: Intentar confirmar pago
        System.out.println("7️⃣ Intentando confirmar pago...");
        WebElement confirmButton = driver.findElement(
            By.xpath("//button[contains(text(), 'Confirmar Pago') or contains(text(), '✅')]")
        );
        confirmButton.click();
        
        // Esperar a que aparezca el error
        Thread.sleep(5000);
        
        System.out.println("✅ Pago rechazado (esperado)");
        
        // Paso 8: Verificar que apareció mensaje de error
        System.out.println("8️⃣ Verificando mensaje de error...");
        
        // Buscar mensaje de error en el DOM
        boolean errorFound = false;
        try {
            WebElement errorMessage = driver.findElement(
                By.xpath("//*[contains(text(), 'Error') or contains(text(), 'rechazada') or contains(text(), 'declined')]")
            );
            errorFound = errorMessage.isDisplayed();
        } catch (Exception e) {
            // También podría ser un alert
            try {
                driver.switchTo().alert();
                errorFound = true;
                driver.switchTo().alert().accept();
            } catch (Exception ex) {
                // No hay alert
            }
        }
        
        Assertions.assertTrue(errorFound, 
            "❌ No se mostró mensaje de error al usuario");
        
        System.out.println("✅ Mensaje de error mostrado");
        
        // Paso 9: Verificar que NO se creó transacción "completed" en BD
        System.out.println("9️⃣ Verificando que NO se guardó en BD como 'completed'...");
        Thread.sleep(2000);
        
        long transactionsAfterTest = queuePaymentTransactionDao.count();
        
        // Puede haber creado una transacción "pending" o "failed", pero NO "completed"
        List<QueuePaymentTransaction> completedTransactions = 
            queuePaymentTransactionDao.findByEmailAndStatus(TEST_EMAIL, "completed");
        
        long completedCountBefore = transactionsBeforeTest;
        long completedCountAfter = completedTransactions.size();
        
        Assertions.assertEquals(completedCountBefore, completedCountAfter, 
            "❌ Se creó una transacción 'completed' cuando no debería");
        
        System.out.println("✅ No se creó transacción 'completed' (correcto)");
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("✅ ESCENARIO 2 COMPLETADO EXITOSAMENTE");
        System.out.println("═══════════════════════════════════════════════════════");
    }
}
