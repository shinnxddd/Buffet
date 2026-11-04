package service;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Objeto;
import modelo.Tarea;

import java.util.List;

/**
 * Reglas del juego. Es la misma lógica de la versión de consola (ver referencia-consola/),
 * pero en vez de leer con Scanner e imprimir con System.out, recibe la decisión del jugador
 * y devuelve el mensaje resultante para que la interfaz lo muestre.
 */
public class JuegoService {

    // Decisiones posibles ante un cliente (antes: opciones "1", "2", "3")
    public static final int CLIENTE_DAR = 1;
    public static final int CLIENTE_NO_DAR = 2;
    public static final int CLIENTE_PREGUNTAR = 3;

    // Decisiones posibles ante una tarea (antes: opciones "1", "2", "3")
    public static final int TAREA_HACER = 1;
    public static final int TAREA_POSPONER = 2;
    public static final int TAREA_ROMPER = 3;

    private final Jugador jugador;
    private final List<Cliente> clientes;
    private final List<Tarea> tareas;
    private final Inventario inventario;

    public JuegoService(Jugador jugador, List<Cliente> clientes, List<Tarea> tareas, Inventario inventario) {
        this.jugador = jugador;
        this.clientes = clientes;
        this.tareas = tareas;
        this.inventario = inventario;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public boolean hayClientePendiente() {
        return clientes.stream().anyMatch(c -> !c.isAtendido());
    }

    public boolean hayTareaPendiente() {
        return tareas.stream().anyMatch(t -> !t.isCompletada());
    }

    /** Opción 1 del menú: el próximo cliente sin atender. */
    public Cliente esperarCliente() throws InteraccionInvalidaException {
        return clientes.stream()
                .filter(c -> !c.isAtendido())
                .findFirst()
                .orElseThrow(() -> new InteraccionInvalidaException("No vino ningún cliente todavía."));
    }

    public String responderCliente(Cliente cliente, int opcion)
            throws InteraccionInvalidaException, PedidoInvalidoException {
        switch (opcion) {
            case CLIENTE_DAR:
                if (cliente.isAtendido()) {
                    throw new PedidoInvalidoException("Ese cliente ya fue atendido.");
                }
                cliente.marcarAtendido();
                jugador.setCordura(jugador.getCordura() + 5);
                return "Le diste " + cliente.getPedido() + " a " + cliente.getNombre() + ". Se va contento.";
            case CLIENTE_NO_DAR:
                cliente.marcarAtendido();
                jugador.setCordura(jugador.getCordura() - 5);
                return cliente.getNombre() + " se va enojado.";
            case CLIENTE_PREGUNTAR:
                return cliente.getNombre() + " no contesta y se queda mirándote fijo.";
            default:
                throw new InteraccionInvalidaException("Esa opción no existe.");
        }
    }

    /** Opción 2 del menú: la primera tarea sin completar. */
    public Tarea realizarTarea() throws TareaNoDisponibleException {
        return tareas.stream()
                .filter(t -> !t.isCompletada())
                .findFirst()
                .orElseThrow(() -> new TareaNoDisponibleException("No hay tareas pendientes."));
    }

    public String responderTarea(Tarea tarea, int opcion)
            throws TareaNoDisponibleException, InteraccionInvalidaException {
        switch (opcion) {
            case TAREA_HACER:
                if (tarea.isCompletada()) {
                    throw new TareaNoDisponibleException("Esa tarea ya estaba hecha.");
                }
                tarea.setCompletada(true);
                jugador.sumarTareaCompletada();
                return "Listo, tarea completada.";
            case TAREA_POSPONER:
                return "La dejás para después.";
            case TAREA_ROMPER:
                jugador.setCordura(jugador.getCordura() - 15);
                return "Rompiste todo. Tu cordura baja.";
            default:
                throw new InteraccionInvalidaException("Esa opción no existe.");
        }
    }

    /** Opción 3 del menú: los objetos del inventario (lista vacía = "(vacío)"). */
    public List<Objeto> abrirInventario() {
        return inventario.getObjetos();
    }

    /** Mismo comportamiento que Objeto.interactuar(), pero informando por excepción/mensaje. */
    public String usarObjeto(Objeto objeto) throws ObjetoNoDisponibleException {
        if (!objeto.isUtilizable()) {
            throw new ObjetoNoDisponibleException(objeto.getNombre() + " no se puede usar ahora mismo.");
        }
        return "Usaste " + objeto.getNombre() + ".";
    }
}
