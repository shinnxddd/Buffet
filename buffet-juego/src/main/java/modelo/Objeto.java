package modelo;

import interfaces.Interactuable;

public class Objeto extends Mundo implements Interactuable {

    private String tipoObjeto;
    private boolean utilizable;
    private int efectoCordura;     // + sube la cordura, - la baja al usarlo
    private String mensajeUso;     // frase extra al usarlo (puede ser null)

    public Objeto(int id, String nombre, String tipoObjeto, boolean utilizable) {
        this(id, nombre, tipoObjeto, utilizable, 0, null);
    }

    public Objeto(int id, String nombre, String tipoObjeto, boolean utilizable, int efectoCordura, String mensajeUso) {
        super(id, nombre);
        this.tipoObjeto = tipoObjeto;
        this.utilizable = utilizable;
        this.efectoCordura = efectoCordura;
        this.mensajeUso = mensajeUso;
    }

    public String getTipoObjeto() {
        return tipoObjeto;
    }

    public boolean isUtilizable() {
        return utilizable;
    }

    public int getEfectoCordura() {
        return efectoCordura;
    }

    public String getMensajeUso() {
        return mensajeUso;
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
