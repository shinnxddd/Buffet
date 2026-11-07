package modelo;

import interfaces.Interactuable;

public class Cliente extends Personaje implements Interactuable {

    private final String pedido;        // texto: "un café"
    private final String tipoPedido;    // código del producto que hay que llevarle: "CAFE"
    private final int aspecto;          // qué cara/ropa tiene (lo usa la interfaz)
    private final double pacienciaMax;  // segundos que espera en el mostrador
    private double paciencia;
    private boolean atendido;
    private final String[] respuestas;
    private int preguntas;

    public Cliente(int id, String nombre, String pedido, String tipoPedido, int aspecto,
                   double paciencia, String... respuestas) {
        super(id, nombre);
        this.pedido = pedido;
        this.tipoPedido = tipoPedido;
        this.aspecto = aspecto;
        this.pacienciaMax = paciencia;
        this.paciencia = paciencia;
        this.respuestas = respuestas.length == 0
                ? new String[]{nombre + " no contesta y se queda mirándote fijo."} : respuestas;
    }

    public String getPedido() { return pedido; }
    public String getTipoPedido() { return tipoPedido; }
    public int getAspecto() { return aspecto; }
    public boolean isAtendido() { return atendido; }
    public void marcarAtendido() { this.atendido = true; }
    public double getPacienciaMax() { return pacienciaMax; }
    public double getPacienciaRestante() { return paciencia; }
    public void bajarPaciencia(double segundos) { paciencia = Math.max(0, paciencia - segundos); }

    /** Cada vez que le preguntás responde algo distinto (da la vuelta cuando se le acaban). */
    public String preguntar() {
        return respuestas[preguntas++ % respuestas.length];
    }

    @Override
    public String getTipo() { return "CLIENTE"; }

    @Override
    public void interactuar() {
        System.out.println(nombre + " dice: \"Hola, quiero " + pedido + "\"");
    }
}
