package modelo;

/**
 * Herramienta de limpieza o reparación. Se lleva físicamente en la mano
 * y solo sirve para la tarea del lugar indicado.
 */
public class Herramienta extends Objeto {

    private final Lugar lugar;

    public Herramienta(int id, String nombre, Lugar lugar) {
        super(id, nombre, "HERRAMIENTA", false, 0, null);
        this.lugar = lugar;
    }

    public Lugar getLugar() {
        return lugar;
    }

    public boolean sirvePara(Lugar otro) {
        return lugar == otro;
    }
}
