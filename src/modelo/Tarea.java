package modelo;

import interfaces.Interactuable;


public class Tarea extends Mundo implements Interactuable {

    private String descripcion;
    private String tipoTarea;
    private int dia;
    private boolean completada;

    public Tarea(int id, String descripcion, String tipoTarea, int dia, boolean completada) {
        super(id, descripcion);
        this.descripcion = descripcion;
        this.tipoTarea = tipoTarea;
        this.dia = dia;
        this.completada = completada;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getTipoTarea() {
        return tipoTarea;
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

    @Override
    public String getTipo() {
        return "TAREA";
    }

    @Override
    public void interactuar() {
        System.out.println("Tarea (" + tipoTarea + ", día " + dia + "): " + descripcion
                + (completada ? " [hecha]" : " [pendiente]"));
    }
}
