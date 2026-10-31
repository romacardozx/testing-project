package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.AlertaInexistenteException;
import ar.unsam.vv.atalaya.aplicacion.ConflictoException;
import ar.unsam.vv.atalaya.aplicacion.RecursoInexistenteException;
import ar.unsam.vv.atalaya.dominio.AlertaYaReconocidaException;
import ar.unsam.vv.atalaya.dominio.ProbabilidadInvalidaException;
import ar.unsam.vv.atalaya.dominio.UmbralesInvalidosException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce excepciones a {@link ErrorRespuesta}. Cada RF suma aquí sus códigos (409, 422...). */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta validacion(MethodArgumentNotValidException ex) {
        var error = ex.getBindingResult().getFieldError();
        String campo = error == null ? null : error.getField();
        String mensaje = error == null ? "Solicitud inválida" : error.getDefaultMessage();
        return new ErrorRespuesta("VALIDACION", mensaje, campo);
    }

    /** JSON mal formado, valor no numérico o enum inexistente (magnitud, canal D...). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta ilegible(HttpMessageNotReadableException ex) {
        String campo = null;
        if (ex.getCause() instanceof JsonMappingException mapeo && !mapeo.getPath().isEmpty()) {
            campo = mapeo.getPath().get(mapeo.getPath().size() - 1).getFieldName();
        }
        String mensaje = campo == null
                ? "El cuerpo no es un JSON válido"
                : "Valor inválido para el campo " + campo;
        return new ErrorRespuesta("MENSAJE_INVALIDO", mensaje, campo);
    }

    @ExceptionHandler(ProbabilidadInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta probabilidadInvalida(ProbabilidadInvalidaException ex) {
        return new ErrorRespuesta("PROBABILIDAD_INVALIDA", ex.getMessage(), "probabilidad");
    }

    @ExceptionHandler(AlertaInexistenteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorRespuesta alertaInexistente(AlertaInexistenteException ex) {
        return new ErrorRespuesta("ALERTA_INEXISTENTE", ex.getMessage(), "id");
    }

    @ExceptionHandler(AlertaYaReconocidaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta yaReconocida(AlertaYaReconocidaException ex) {
        return new ErrorRespuesta("ALERTA_YA_RECONOCIDA", ex.getMessage(), null);
    }

    /**
     * Dos reconocimientos simultáneos (A5): el segundo choca con la versión de la alerta, con un
     * bloqueo de H2 o con la columna única de reconocimiento. En todos los casos, 409.
     */
    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta conflictoConcurrente(RuntimeException ex) {
        return new ErrorRespuesta("CONFLICTO", "Otra operación modificó el recurso al mismo tiempo", null);
    }

    @ExceptionHandler(UmbralesInvalidosException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorRespuesta umbralesInvalidos(UmbralesInvalidosException ex) {
        return new ErrorRespuesta("UMBRALES_INVALIDOS", ex.getMessage(), ex.campo());
    }

    @ExceptionHandler(ConflictoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorRespuesta conflicto(ConflictoException ex) {
        return new ErrorRespuesta(ex.codigo(), ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoInexistenteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorRespuesta inexistente(RecursoInexistenteException ex) {
        return new ErrorRespuesta(ex.codigo(), ex.getMessage(), "id");
    }

    @ExceptionHandler(MagnitudInexistenteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorRespuesta magnitudInexistente(MagnitudInexistenteException ex) {
        return new ErrorRespuesta("MAGNITUD_INEXISTENTE", ex.getMessage(), "magnitud");
    }
}
