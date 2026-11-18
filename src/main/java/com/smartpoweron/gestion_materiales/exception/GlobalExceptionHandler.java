package com.smartpoweron.gestion_materiales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas HTTP en español (RNF14, RNF11):
 *   - ReglaNegocioException  -> 400 Bad Request con el mensaje de la regla.
 *   - Recurso no encontrado  -> 404 Not Found.
 *   - Validación de bean     -> 400 con la lista de errores por campo.
 *   - Cualquier otra         -> 500 "Error interno del servidor".
 *
 * El cuerpo de la respuesta siempre es JSON con las claves {error, mensaje}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Regla de negocio", ex.getMessage());
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<Map<String, Object>> autenticacion(AutenticacionException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estadoIlegal(IllegalStateException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Operación inválida", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errores.put(fe.getField(), fe.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "Revise los campos del formulario.", errores);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraintViolation(jakarta.validation.ConstraintViolationException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.toLowerCase().contains("duplicate")) {
            return respuesta(HttpStatus.BAD_REQUEST, "Dato duplicado",
                    "El valor ingresado ya existe en el sistema.");
        }
        return respuesta(HttpStatus.BAD_REQUEST, "Integridad de datos",
                "La operación viola una restricción de la base de datos.");
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> recursoNoEncontrado(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        // Recurso estático inexistente (favicon.ico, etc.) -> 404, no 500.
        return respuesta(HttpStatus.NOT_FOUND, "No encontrado",
                "El recurso '" + ex.getResourcePath() + "' no existe.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception ex) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, String mensaje) {
        return respuesta(status, error, mensaje, null);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, String mensaje, Map<String, String> errores) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("mensaje", mensaje);
        if (errores != null) body.put("errores", errores);
        return ResponseEntity.status(status).body(body);
    }
}
