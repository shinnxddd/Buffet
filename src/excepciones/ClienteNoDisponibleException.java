package excepciones;

public class ClienteNoDisponibleException extends Exception {
    public ClienteNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
