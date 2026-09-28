package modelo;

public class Tarea {

    private int id;
    private String descripcion;
    private int dia;
    private boolean completada;

    public Tarea(int id, String descripcion, int dia, boolean completada) {
        this.id = id;
        this.descripcion = descripcion;
        this.dia = dia;
        this.completada = completada;
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
}
