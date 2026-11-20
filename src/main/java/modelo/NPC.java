package modelo;

public class NPC extends Personaje {

    private String dialogo;
    private boolean puedeDarTarea;

    public NPC(int id, String nombre, String dialogo, boolean puedeDarTarea) {
        super(id, nombre);
        this.dialogo = dialogo;
        this.puedeDarTarea = puedeDarTarea;
    }

    public String getDialogo() {
        return dialogo;
    }

    public boolean isPuedeDarTarea() {
        return puedeDarTarea;
    }
}
