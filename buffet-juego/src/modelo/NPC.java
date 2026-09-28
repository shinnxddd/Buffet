package modelo;

import interfaces.Interactuable;

public class NPC extends Personaje implements Interactuable {

    private String rol;
    private String dialogo;
    private boolean puedeDarTarea;

    public NPC(int id, String nombre, String rol, String dialogo, boolean puedeDarTarea) {
        super(id, nombre);
        this.rol = rol;
        this.dialogo = dialogo;
        this.puedeDarTarea = puedeDarTarea;
    }

    public String getRol() {
        return rol;
    }

    public String getDialogo() {
        return dialogo;
    }

    public boolean isPuedeDarTarea() {
        return puedeDarTarea;
    }

    @Override
    public String getTipo() {
        return "NPC";
    }

    @Override
    public void interactuar() {
        System.out.println(nombre + " dice: \"" + dialogo + "\"");
    }
}
