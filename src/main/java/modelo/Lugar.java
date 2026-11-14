package modelo;

/** Lugares del buffet donde puede haber una tarea pendiente. */
public enum Lugar {
    BANO("el baño", Sala.BANO),
    MESAS("las mesas", Sala.COMEDOR),
    BASURA("la basura", Sala.COMEDOR),
    TABLERO("el tablero de luz", Sala.COMEDOR),
    PLANCHA("la plancha", Sala.COCINA),
    CAFETERA("la cafetera", Sala.COCINA),
    HELADERA("la heladera", Sala.COCINA);

    private final String etiqueta;
    private final Sala sala;

    Lugar(String etiqueta, Sala sala) {
        this.etiqueta = etiqueta;
        this.sala = sala;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Sala getSala() {
        return sala;
    }
}
