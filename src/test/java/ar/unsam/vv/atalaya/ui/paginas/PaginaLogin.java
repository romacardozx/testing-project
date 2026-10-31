package ar.unsam.vv.atalaya.ui.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Page Object de /login. */
public class PaginaLogin {

    private final WebDriver driver;
    private final String baseUrl;

    public PaginaLogin(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public PaginaLogin abrir() {
        driver.get(baseUrl + "/login");
        return this;
    }

    public PaginaPanel ingresar(String usuario, String clave) {
        driver.findElement(By.cssSelector("[data-test=usuario]")).sendKeys(usuario);
        driver.findElement(By.cssSelector("[data-test=clave]")).sendKeys(clave);
        driver.findElement(By.cssSelector("[data-test=ingresar]")).click();
        return new PaginaPanel(driver).esperarCarga();
    }
}
