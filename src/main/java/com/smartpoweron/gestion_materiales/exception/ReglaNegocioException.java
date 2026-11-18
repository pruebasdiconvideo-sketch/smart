package com.smartpoweron.gestion_materiales.exception;

/**
 * Excepción de regla de negocio. Se traduce a HTTP 400 con el mensaje en
 * español (RNF14) gracias al GlobalExceptionHandler.
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) { super(mensaje); }
}
