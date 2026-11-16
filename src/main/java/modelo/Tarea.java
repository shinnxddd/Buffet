package modelo;

/**
 * Una tarea del buffet. Cada una vive en un lugar puntual y puede pedir una herramienta
 * que el jugador tiene que llevar físicamente desde el almacén.
 */
public class Tarea {

    private int id;
    private String descripcion;
    private int dia;
    private boolean completada;
    private Lugar lugar;
    private String titulo;
    private String mensajeExito;
    private Objeto recompensa;      // objeto que aparece en el inventario al completarla (puede ser null)
    private Herramienta herramienta; // herramienta obligatoria para hacerla (puede ser null)
    private int prioridad;          // 1 = molesta, 2 = urgente, 3 = peligrosa
    private String consecuencia;    // qué pasa si queda pendiente

    /** Tarea simple, como la versión original: sin lugar propio ni recompensa (queda en el baño). */
    public Tarea(int id, String descripcion, int dia, boolean completada) {
        this(id, descripcion, dia, completada, Lugar.BANO,
                "Tarea pendiente", "Listo, tarea completada.", null, null, 1,
                "El buffet se hunde un poco más.");
    }

    public Tarea(int id, String descripcion, int dia, boolean completada, Lugar lugar,
                 String titulo, String mensajeExito, Objeto recompensa) {
        this(id, descripcion, dia, completada, lugar, titulo, mensajeExito, recompensa, null, 1,
                "El buffet se hunde un poco más.");
    }

    public Tarea(int id, String descripcion, int dia, boolean completada, Lugar lugar,
                 String titulo, String mensajeExito, Objeto recompensa, Herramienta herramienta) {
        this(id, descripcion, dia, completada, lugar, titulo, mensajeExito, recompensa, herramienta, 1,
                "El buffet se hunde un poco más.");
    }

    public Tarea(int id, String descripcion, int dia, boolean completada, Lugar lugar,
                 String titulo, String mensajeExito, Objeto recompensa, Herramienta herramienta,
                 int prioridad, String consecuencia) {
        this.id = id;
        this.descripcion = descripcion;
        this.dia = dia;
        this.completada = completada;
        this.lugar = lugar;
        this.titulo = titulo;
        this.mensajeExito = mensajeExito;
        this.recompensa = recompensa;
        this.herramienta = herramienta;
        this.prioridad = Math.max(1, Math.min(3, prioridad));
        this.consecuencia = consecuencia;
    }

    public int getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getDia() {
        return dia;
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

    public Sala getSala() {
        return lugar.getSala();
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

    public String getConsecuencia() {
        return consecuencia;
    }

    @Override
    public String toString() {
        return titulo + " (" + lugar.getEtiqueta() + ")";
    }
}
