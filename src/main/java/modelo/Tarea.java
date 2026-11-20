package modelo;

/**
 * Una tarea del buffet. Cada una vive en un lugar puntual y puede pedir una herramienta
 * que el jugador tiene que llevar físicamente desde el almacén.
 */
public class Tarea {

    private int id;
    private String descripcion;
    private boolean completada;
    private Lugar lugar;
    private String titulo;
    private String mensajeExito;
    private Objeto recompensa;      // objeto que aparece en el inventario al completarla (puede ser null)
    private Herramienta herramienta; // herramienta obligatoria para hacerla (puede ser null)
    private int prioridad;          // 1 = molesta, 2 = urgente, 3 = peligrosa

    public Tarea(int id, String descripcion, Lugar lugar, String titulo, String mensajeExito,
                 Objeto recompensa, Herramienta herramienta, int prioridad) {
        this.id = id;
        this.descripcion = descripcion;
        this.completada = false;
        this.lugar = lugar;
        this.titulo = titulo;
        this.mensajeExito = mensajeExito;
        this.recompensa = recompensa;
        this.herramienta = herramienta;
        this.prioridad = Math.max(1, Math.min(3, prioridad));
    }

    public int getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public Lugar getLugar() {
        return lugar;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensajeExito() {
        return mensajeExito;
    }

    public Objeto getRecompensa() {
        return recompensa;
    }

    public Herramienta getHerramienta() {
        return herramienta;
    }

    public boolean requiereHerramienta() {
        return herramienta != null;
    }

    public int getPrioridad() {
        return prioridad;
    }

    @Override
    public String toString() {
        return titulo + " (" + lugar.getEtiqueta() + ")";
    }
}
