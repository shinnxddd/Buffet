package modelo;

/** Algo que arreglar o limpiar en un rincón del buffet. Se resuelve llevando la herramienta correcta en las manos. */
public class Tarea {

    private final int id;
    private final String descripcion;
    private final int dia;
    private boolean completada;
    private final Lugar lugar;
    private final String titulo;
    private final String mensajeExito;
    private final String herramienta;   // tipo de objeto que hace falta (ver service.Catalogo)

    public Tarea(int id, String descripcion, int dia, boolean completada, Lugar lugar,
                 String titulo, String mensajeExito, String herramienta) {
        this.id = id;
        this.descripcion = descripcion;
        this.dia = dia;
        this.completada = completada;
        this.lugar = lugar;
        this.titulo = titulo;
        this.mensajeExito = mensajeExito;
        this.herramienta = herramienta;
    }

    public int getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public int getDia() { return dia; }
    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }
    public Lugar getLugar() { return lugar; }
    public String getTitulo() { return titulo; }
    public String getMensajeExito() { return mensajeExito; }
    public String getHerramienta() { return herramienta; }
}
