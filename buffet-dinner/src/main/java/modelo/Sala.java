package modelo;

/** Habitaciones del buffet. Se pasa de una a otra cruzando las puertas de los bordes. */
public enum Sala {
    SALON("El salón"), COCINA("La cocina"), ALMACEN("El almacén");

    private final String nombre;

    Sala(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
