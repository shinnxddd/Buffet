package modelo;

import interfaces.Interactuable;

public class Cliente extends Personaje implements Interactuable {

    private String pedido;
    private boolean atendido;
    private final String[] respuestas;   // lo que contesta cuando le preguntás "por qué"
    private int preguntas;

    public Cliente(int id, String nombre, String pedido) {
        this(id, nombre, pedido, nombre + " no contesta y se queda mirándote fijo.");
    }

    public Cliente(int id, String nombre, String pedido, String... respuestas) {
        super(id, nombre);
        this.pedido = pedido;
        this.atendido = false;
        this.respuestas = respuestas;
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

    /** Cada vez que le preguntás, responde algo distinto (da la vuelta cuando se le acaban). */
    public String preguntar() {
        return respuestas[preguntas++ % respuestas.length];
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
