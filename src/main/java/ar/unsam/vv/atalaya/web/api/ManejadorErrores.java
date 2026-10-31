package ar.unsam.vv.atalaya.web.api;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorRespuesta ilegible(HttpMessageNotReadableException ex) {
        return new ErrorRespuesta("MENSAJE_INVALIDO", "El cuerpo no es un JSON válido para esta operación", null);
    }
}
