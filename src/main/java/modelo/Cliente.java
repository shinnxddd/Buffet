package modelo;

public class Cliente extends Personaje {

    private String pedido;
    private boolean pedidoTomado;        // ya le anotaste lo que quiere
    private boolean atendido;            // ya le entregaste la bandeja

    public Cliente(int id, String nombre, String pedido) {
        super(id, nombre);
        this.pedido = pedido;
    }

    public String getPedido() {
        return pedido;
    }

    /** Todavía está esperando que le tomen el pedido. */
    public boolean isPedidoTomado() {
        return pedidoTomado;
    }

    public void marcarPedidoTomado() {
        this.pedidoTomado = true;
    }

    public boolean isAtendido() {
        return atendido;
    }

    public void marcarAtendido() {
        this.atendido = true;
    }
}
