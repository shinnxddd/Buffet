package modelo;

public class Cliente extends NPC {

    private String pedido;
    private boolean atendido;

    public Cliente(int id, String nombre, String pedido) {
        this(id, nombre, pedido, false);
    }

    public Cliente(int id, String nombre, String pedido, boolean atendido) {
        super(id, nombre, "CLIENTE", "Hola, quiero " + pedido, false);
        this.pedido = pedido;
        this.atendido = atendido;
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
}
