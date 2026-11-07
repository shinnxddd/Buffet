package modelo;

/** Rincones del buffet donde puede haber una tarea pendiente. */
public enum Lugar {
    BANO(Sala.SALON), MESAS(Sala.SALON), BASURA(Sala.SALON), TABLERO(Sala.SALON),
    PLANCHA(Sala.COCINA), CAFETERA(Sala.COCINA), HELADERA(Sala.COCINA);

    private final Sala sala;

    Lugar(Sala sala) {
        this.sala = sala;
    }

    public Sala getSala() {
        return sala;
    }
}
