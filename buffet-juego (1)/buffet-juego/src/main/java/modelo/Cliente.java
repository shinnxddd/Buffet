package modelo;

import interfaces.Interactuable;

public class Cliente extends Personaje implements Interactuable {

    private String pedido;
    private boolean atendido;

    public Cliente(int id, String nombre, String pedido) {
        super(id, nombre);
        this.pedido = pedido;
        this.atendido = false;
    }

    public String getPedido() {
        return pedido;
    }

    public boolean isAtendido() {
        return atendido;
    }

    public void marcarAtendido() {
        this.atendido = true;
    }

    @Override
    public String getTipo() {
        return "CLIENTE";
    }

    @Override
    public void interactuar() {
        System.out.println(nombre + " dice: \"Hola, quiero " + pedido + "\"");
    }
}
