package modelo;

import interfaces.Interactuable;

public class Objeto extends Mundo implements Interactuable {

    private String tipoObjeto;
    private boolean utilizable;
    private int cantidad;

    public Objeto(int id, String nombre, String tipoObjeto, boolean utilizable, int cantidad) {
        super(id, nombre);
        this.tipoObjeto = tipoObjeto;
        this.utilizable = utilizable;
        this.cantidad = cantidad;
    }

    public String getTipoObjeto() {
        return tipoObjeto;
    }

    public boolean isUtilizable() {
        return utilizable;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public String getTipo() {
        return "OBJETO";
    }

    @Override
    public void interactuar() {
        if (!utilizable) {
            System.out.println(nombre + " no se puede usar ahora mismo.");
            return;
        }
        System.out.println("Usaste " + nombre + ".");
    }
}
