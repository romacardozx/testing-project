package ar.unsam.vv.atalaya.ui;

import java.time.Duration;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

/**
 * Crea el WebDriver según -Dbrowser: htmlunit (por defecto, sin navegador, corre en mvn verify) o
 * chrome (demo de la exposición; con -Dheadless=true sin ventana). Selenium Manager baja el driver.
 *
 * <p>Modo pausa (-Dpausa=true, solo Chrome con ventana): al terminar cada prueba el navegador queda
 * abierto en el estado final, con una franja verde o roja, y el SISTEMA sigue levantado para seguir
 * navegando. Cerrar la ventana continúa con la siguiente prueba (o pasan 10 minutos).
 */
final class Navegador {

    private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(10);

    private Navegador() {
    }

    static WebDriver crear() {
        if (esChrome()) {
            var opciones = new ChromeOptions();
            if (sinVentana()) {
                opciones.addArguments("--headless=new");
            }
            return new ChromeDriver(opciones);
        }
        return new HtmlUnitDriver(false);
    }

    /** Si corresponde, deja el navegador abierto hasta que el usuario cierre la ventana. */
    static void pausarSiCorresponde(WebDriver driver, String prueba, boolean aprobada) {
        if (!Boolean.getBoolean("pausa") || !esChrome() || sinVentana()) {
            return;
        }
        String texto = (aprobada ? "✔ " : "✘ ") + prueba + " · cerrá esta ventana para seguir";
        marcar(driver, texto, aprobada ? "#2e7d32" : "#c62828");
        System.out.println("  ⏸ " + texto);
        Wait<WebDriver> espera = new FluentWait<>(driver)
                .withTimeout(ESPERA_MAXIMA)
                .pollingEvery(Duration.ofMillis(500));
        try {
            espera.until(Navegador::ventanaCerrada);
        } catch (org.openqa.selenium.TimeoutException e) {
            System.out.println("  ⏵ pasaron 10 minutos, sigo");
        }
    }

    private static boolean ventanaCerrada(WebDriver driver) {
        try {
            return driver.getWindowHandles().isEmpty();
        } catch (WebDriverException e) {
            return true;
        }
    }

    private static void marcar(WebDriver driver, String texto, String color) {
        try {
            ((JavascriptExecutor) driver).executeScript("""
                    const franja = document.createElement('div');
                    franja.textContent = arguments[0];
                    franja.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:99999;padding:10px 16px;'
                        + 'font:bold 16px sans-serif;color:#fff;background:' + arguments[1];
                    document.body.prepend(franja);
                    document.body.style.paddingTop = '48px';""", texto, color);
        } catch (WebDriverException e) {
            // Si la página no permite el script, la pausa sigue igual.
        }
    }

    private static boolean esChrome() {
        return "chrome".equalsIgnoreCase(System.getProperty("browser", "htmlunit"));
    }

    private static boolean sinVentana() {
        return Boolean.getBoolean("headless");
    }
}
