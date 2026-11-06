package service;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Lugar;
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

    /** ¿Queda algo por hacer en ese lugar del buffet? */
    public boolean hayTareaPendiente(Lugar lugar) {
        return tareas.stream().anyMatch(t -> t.getLugar() == lugar && !t.isCompletada());
    }

    /** El cliente que sigue en la fila, o null si ya atendiste a todos. */
    public Cliente proximoCliente() {
        return clientes.stream().filter(c -> !c.isAtendido()).findFirst().orElse(null);
    }

    // ---------- progreso del turno ----------

    public int totalClientes() {
        return clientes.size();
    }

    public int clientesAtendidos() {
        return (int) clientes.stream().filter(Cliente::isAtendido).count();
    }

    public int totalTareas() {
        return tareas.size();
    }

    public int tareasHechas() {
        return (int) tareas.stream().filter(Tarea::isCompletada).count();
    }

    /** Todos los clientes atendidos y todas las tareas hechas. */
    public boolean turnoTerminado() {
        return !hayClientePendiente() && !hayTareaPendiente();
    }

    /** Opción 1 del menú: el próximo cliente sin atender. */
    public Cliente esperarCliente() throws InteraccionInvalidaException {
        return clientes.stream()
                .filter(c -> !c.isAtendido())
                .findFirst()
                .orElseThrow(() -> new InteraccionInvalidaException(clientes.isEmpty()
                        ? "No vino ningún cliente todavía."
                        : "Ya atendiste a todos los clientes. Por ahora no viene nadie más."));
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
                return "Le diste " + cliente.getPedido() + " " + aQuien(cliente) + ". Se va con una sonrisa.";
            case CLIENTE_NO_DAR:
                cliente.marcarAtendido();
                jugador.setCordura(jugador.getCordura() - 5);
                return cliente.getNombre() + " se va de mal humor.";
            case CLIENTE_PREGUNTAR:
                return cliente.preguntar();
            default:
                throw new InteraccionInvalidaException("Esa opción no existe.");
        }
    }

    /** "a el profesor" no existe: la contracción es "al profesor". */
    private static String aQuien(Cliente cliente) {
        String n = cliente.getNombre();
        return n.startsWith("el ") ? "al " + n.substring(3) : "a " + n;
    }

    /** Opción 2 del menú: la primera tarea sin completar. */
    public Tarea realizarTarea() throws TareaNoDisponibleException {
        return tareas.stream()
                .filter(t -> !t.isCompletada())
                .findFirst()
                .orElseThrow(() -> new TareaNoDisponibleException("No hay tareas pendientes."));
    }

    /** Tarea pendiente en un lugar puntual del buffet (cada rincón tiene la suya). */
    public Tarea realizarTarea(Lugar lugar) throws TareaNoDisponibleException {
        return tareas.stream()
                .filter(t -> t.getLugar() == lugar && !t.isCompletada())
                .findFirst()
                .orElseThrow(() -> new TareaNoDisponibleException("Acá ya no queda nada por hacer."));
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
                Objeto premio = tarea.getRecompensa();
                if (premio == null) {
                    return tarea.getMensajeExito();
                }
                inventario.agregar(premio);
                return tarea.getMensajeExito() + " Guardaste " + premio.getNombre() + " en el inventario.";
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

    /**
     * Mismo comportamiento que Objeto.interactuar(), pero informando por excepción/mensaje.
     * Los objetos usables se gastan: cambian la cordura (para bien o para mal) y salen del inventario.
     */
    public String usarObjeto(Objeto objeto) throws ObjetoNoDisponibleException {
        if (!objeto.isUtilizable()) {
            throw new ObjetoNoDisponibleException(objeto.getNombre() + " no se puede usar ahora mismo.");
        }
        int cordura = jugador.getCordura();
        int efecto = objeto.getEfectoCordura();
        if (efecto > 0) {
            jugador.setCordura(Math.max(cordura, Math.min(100, cordura + efecto)));   // cura hasta 100, sin pasarse
        } else if (efecto < 0) {
            jugador.setCordura(cordura + efecto);
        }
        inventario.quitar(objeto);
        String extra = objeto.getMensajeUso() == null ? "" : " " + objeto.getMensajeUso();
        return "Usaste " + objeto.getNombre() + "." + extra;
    }
}
