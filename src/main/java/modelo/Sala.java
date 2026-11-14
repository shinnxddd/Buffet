package modelo;

/** Las salas del buffet. Cada una tiene su propia puerta y su propio clima. */
public enum Sala {
    COMEDOR("Comedor", "Mesas, clientes y el mostrador. El corazón del buffet."),
    COCINA("Cocina", "Plancha, cafetera y heladera. Acá se prepara lo que piden."),
    BANO("Baño", "Un charco que nunca se termina de secar."),
    ALMACEN("Almacén", "Herramientas, cajas y todo lo que hace falta para limpiar.");

    private final String nombre;
    private final String descripcion;

    Sala(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
