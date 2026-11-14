package excepciones;

/** Falla al hablar con la base de datos. Envuelve el SQLException para que la UI no dependa de java.sql. */
public class PersistenciaException extends Exception {
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
