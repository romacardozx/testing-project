package ar.unsam.vv.atalaya.ui.paginas;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object de /panel. Todas las búsquedas son por data-test y las esperas por condición
 * (WebDriverWait), nunca con sleep. Cuenta los clics para verificar UI-01 (≤ 2 interacciones).
 */
public class PaginaPanel {

    private final WebDriver driver;
    private final WebDriverWait espera;
    private int clics;

    PaginaPanel(WebDriver driver) {
        this.driver = driver;
        this.espera = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    PaginaPanel esperarCarga() {
        espera.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-test=titulo]")));
        return this;
    }

    public String usuario() {
        return texto(By.cssSelector("[data-test=usuario]"));
    }

    public WebElement filaAlerta(long id) {
        return espera.until(ExpectedConditions.presenceOfElementLocated(alerta(id)));
    }

    public boolean muestraAlerta(long id) {
        return !driver.findElements(alerta(id)).isEmpty();
    }

    public String nivelDeAlerta(long id) {
        return filaAlerta(id).findElement(By.cssSelector("[data-test=nivel]")).getText();
    }

    /** Un clic en Reconocer y espera a que el panel confirme. */
    public PaginaPanel reconocer(long id) {
        filaAlerta(id).findElement(By.cssSelector("[data-test=reconocer]")).click();
        clics++;
        espera.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-test=mensaje]")));
        return this;
    }

    public int clics() {
        return clics;
    }

    public String probabilidad(String tipo) {
        return texto(By.cssSelector("[data-test=estimacion-" + tipo + "] [data-test=probabilidad]"));
    }

    public String nivelEstimado(String tipo) {
        return texto(By.cssSelector("[data-test=estimacion-" + tipo + "] [data-test=nivel]"));
    }

    public List<String> avisos(String magnitud) {
        return driver.findElements(By.cssSelector("[data-test=magnitud-" + magnitud + "] [data-test=aviso]"))
                .stream().map(WebElement::getText).toList();
    }

    public String canal(String magnitud, String canal) {
        return texto(By.cssSelector("[data-test=magnitud-" + magnitud + "] [data-test=canal-" + canal + "]"));
    }

    /** Texto visible de los estados de canal de una magnitud, una línea por renglón de la página. */
    public List<String> lineasDeCanales(String magnitud) {
        String texto = texto(By.cssSelector("[data-test=magnitud-" + magnitud + "] [data-test=estados-canal]"));
        return texto.lines().map(String::strip).filter(l -> !l.isEmpty()).toList();
    }

    private static By alerta(long id) {
        return By.cssSelector("[data-test=alerta-" + id + "]");
    }

    private String texto(By selector) {
        return espera.until(ExpectedConditions.presenceOfElementLocated(selector)).getText();
    }
}
