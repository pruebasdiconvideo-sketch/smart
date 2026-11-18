package com.smartpoweron.gestion_materiales.exception;

/**
 * Excepción de autenticación. Se traduce a HTTP 401 Unauthorized con el
 * mensaje en español (RNF14, PF-02, PF-03).
 */
public class AutenticacionException extends RuntimeException {
    public AutenticacionException(String mensaje) { super(mensaje); }
}
