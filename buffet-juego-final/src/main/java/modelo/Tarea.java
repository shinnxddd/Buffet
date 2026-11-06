package modelo;

public class Tarea {

    private int id;
    private String descripcion;
    private int dia;
    private boolean completada;
    private Lugar lugar;
    private String titulo;
    private String mensajeExito;
    private Objeto recompensa;   // objeto que aparece en el inventario al completarla (puede ser null)

    /** Tarea simple, como la versión original: sin lugar propio ni recompensa (queda en el baño). */
    public Tarea(int id, String descripcion, int dia, boolean completada) {
        this(id, descripcion, dia, completada, Lugar.BANO,
                "TAREAAAAAAAAAAAA!!!!!!!!!!!!!!!!!!!!!!", "Listo, tarea completada.", null);
    }

    public Tarea(int id, String descripcion, int dia, boolean completada, Lugar lugar,
                 String titulo, String mensajeExito, Objeto recompensa) {
        this.id = id;
        this.descripcion = descripcion;
        this.dia = dia;
        this.completada = completada;
        this.lugar = lugar;
        this.titulo = titulo;
        this.mensajeExito = mensajeExito;
        this.recompensa = recompensa;
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

    public String getTitulo() {
        return titulo;
    }

    public String getMensajeExito() {
        return mensajeExito;
    }

    public Objeto getRecompensa() {
        return recompensa;
    }
}
