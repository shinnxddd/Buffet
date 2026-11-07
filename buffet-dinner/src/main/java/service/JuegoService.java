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
 * Reglas del juego. La interfaz le pasa lo que hace el jugador y recibe el mensaje resultante.
 *  - Los productos y las herramientas se llevan en las manos (un objeto a la vez).
 *  - Los clientes esperan con paciencia limitada y hay que entregarles lo que pidieron.
 *  - Las tareas necesitan la herramienta correcta; las estaciones sucias no se pueden usar.
 *  - Al cerrar la jornada empieza otro día con más clientes; con la cordura baja se pierde la partida.
 */
public class JuegoService {

    public static final int UMBRAL_CORDURA_DIA = 30;   // por debajo de esto, pasar de día es perder

    private final Jugador jugador;
    private final Inventario manos = new Inventario(1);
    private List<Cliente> clientes;
    private List<Tarea> tareas;
    private Cliente enMostrador;
    private boolean derrota;
    private String motivoDerrota = "";
    private double acumuladoTablero;

    public JuegoService(Jugador jugador) {
        this.jugador = jugador;
        generarDia();
    }

    private void generarDia() {
        clientes = GeneradorDia.clientes(jugador.getDiaActual());
        tareas = GeneradorDia.tareas(jugador.getDiaActual());
        manos.getObjetos().clear();
        enMostrador = null;
        acumuladoTablero = 0;
    }

    // ---------- consultas ----------

    public Jugador getJugador() { return jugador; }
    public int getDia() { return jugador.getDiaActual(); }
    public boolean isDerrota() { return derrota; }
    public String getMotivoDerrota() { return motivoDerrota; }
    public int totalClientes() { return clientes.size(); }
    public int totalTareas() { return tareas.size(); }
    public int clientesAtendidos() { return (int) clientes.stream().filter(Cliente::isAtendido).count(); }
    public int tareasHechas() { return (int) tareas.stream().filter(Tarea::isCompletada).count(); }
    public boolean hayClientePendiente() { return clientes.stream().anyMatch(c -> !c.isAtendido()); }
    public boolean hayTareaPendiente() { return tareas.stream().anyMatch(t -> !t.isCompletada()); }
    public boolean turnoTerminado() { return !hayClientePendiente() && !hayTareaPendiente(); }

    public boolean hayTareaPendiente(Lugar lugar) {
        return tareas.stream().anyMatch(t -> t.getLugar() == lugar && !t.isCompletada());
    }

    public Cliente proximoCliente() {
        return clientes.stream().filter(c -> !c.isAtendido()).findFirst().orElse(null);
    }

    /** Lo que lleva en las manos, o null si las tiene vacías. */
    public Objeto enMano() {
        return manos.getObjetos().isEmpty() ? null : manos.getObjetos().get(0);
    }

    // ---------- tiempo ----------

    /** Hace correr el reloj. Devuelve un aviso si pasó algo (por ejemplo, un cliente se cansó de esperar). */
    public String pasarTiempo(double segundos) {
        if (derrota) {
            return null;
        }
        String evento = null;
        if (enMostrador != null && !enMostrador.isAtendido()) {
            enMostrador.bajarPaciencia(segundos * (jugador.getCordura() <= 50 ? 1.5 : 1.0));
            if (enMostrador.getPacienciaRestante() <= 0) {
                Cliente c = enMostrador;
                c.marcarAtendido();
                enMostrador = null;
                ajustarCordura(-8);
                evento = cap(c.getNombre()) + " se cansó de esperar y se fue furioso.";
            }
        }
        if (hayTareaPendiente(Lugar.TABLERO)) {   // las chispas desgastan los nervios
            acumuladoTablero += segundos;
            if (acumuladoTablero >= 20) {
                acumuladoTablero = 0;
                ajustarCordura(-1);
            }
        }
        return evento;
    }

    // ---------- clientes ----------

    /** La interfaz avisa cuando el cliente llegó al mostrador: desde ahí empieza a correr su paciencia. */
    public void clienteEnMostrador(Cliente cliente) {
        this.enMostrador = cliente;
    }

    public Cliente esperarCliente() throws InteraccionInvalidaException {
        Cliente c = proximoCliente();
        if (c == null) {
            throw new InteraccionInvalidaException("Ya atendiste a todos los clientes. Por ahora no viene nadie más.");
        }
        return c;
    }

    public String entregar(Cliente cliente) throws PedidoInvalidoException {
        if (cliente.isAtendido()) {
            throw new PedidoInvalidoException("Ese cliente ya fue atendido.");
        }
        Objeto o = enMano();
        if (o == null) {
            throw new PedidoInvalidoException("Tenés las manos vacías. " + cap(cliente.getNombre())
                    + " pidió " + cliente.getPedido() + ": prepará el pedido y traelo.");
        }
        if (!o.getTipoObjeto().equals(cliente.getTipoPedido())) {
            throw new PedidoInvalidoException("Eso no es lo que pidió: quiere " + cliente.getPedido()
                    + " y vos llevás " + o.getNombre() + ".");
        }
        manos.quitar(o);
        cliente.marcarAtendido();
        enMostrador = null;
        ajustarCordura(cliente.getPacienciaRestante() > cliente.getPacienciaMax() * 0.5 ? 8 : 5);
        return "Le diste " + cliente.getPedido() + " " + aQuien(cliente) + ". Se va con una sonrisa.";
    }

    public String rechazar(Cliente cliente) {
        cliente.marcarAtendido();
        enMostrador = null;
        ajustarCordura(-5);
        return cliente.getNombre() + " se va de mal humor.";
    }

    public String preguntar(Cliente cliente) {
        return cliente.preguntar();
    }

    // ---------- manos: productos y herramientas ----------

    /** Chequea que se pueda preparar algo en una estación (limpia) y que haya lugar en las manos. */
    public void validarPreparacion(Lugar estacion) throws InteraccionInvalidaException, ObjetoNoDisponibleException {
        if (estacion != null && hayTareaPendiente(estacion)) {
            Tarea t = tareas.stream().filter(x -> x.getLugar() == estacion && !x.isCompletada()).findFirst().get();
            throw new InteraccionInvalidaException("Así no se puede usar: " + t.getDescripcion() + ". Arreglalo primero.");
        }
        validarManosLibres();
    }

    public void validarManosLibres() throws ObjetoNoDisponibleException {
        if (manos.isLleno()) {
            throw new ObjetoNoDisponibleException("Ya tenés las manos ocupadas con " + enMano().getNombre()
                    + ". Entregalo, usalo o tiralo en los casilleros.");
        }
    }

    /** Pone en las manos un producto recién preparado o una herramienta del estante. */
    public String agarrar(String tipo) throws ObjetoNoDisponibleException {
        validarManosLibres();
        Objeto o = Catalogo.crear(tipo);
        manos.agregar(o);
        return (o.isUtilizable() ? "Preparaste " : "Agarraste ") + o.getNombre() + ".";
    }

    /** Usar lo que llevás (por ejemplo, tomarte el café): se gasta y cambia la cordura. */
    public String usarMano() throws ObjetoNoDisponibleException, InteraccionInvalidaException {
        Objeto o = enMano();
        if (o == null) {
            throw new InteraccionInvalidaException("No llevás nada en las manos.");
        }
        if (!o.isUtilizable()) {
            throw new ObjetoNoDisponibleException(cap(o.getNombre()) + " no se puede usar ahora mismo.");
        }
        manos.quitar(o);
        ajustarCordura(o.getEfectoCordura());
        return "Usaste " + o.getNombre() + ". " + o.getMensajeUso();
    }

    public String tirarMano() throws InteraccionInvalidaException {
        Objeto o = enMano();
        if (o == null) {
            throw new InteraccionInvalidaException("No llevás nada en las manos.");
        }
        manos.quitar(o);
        return "Tiraste " + o.getNombre() + ".";
    }

    // ---------- tareas ----------

    public Tarea realizarTarea(Lugar lugar) throws TareaNoDisponibleException {
        return tareas.stream().filter(t -> t.getLugar() == lugar && !t.isCompletada()).findFirst()
                .orElseThrow(() -> new TareaNoDisponibleException("Acá ya no queda nada por hacer."));
    }

    public void validarArreglo(Tarea tarea) throws TareaNoDisponibleException {
        if (tarea.isCompletada()) {
            throw new TareaNoDisponibleException("Esa tarea ya estaba hecha.");
        }
        Objeto o = enMano();
        if (o == null || !o.getTipoObjeto().equals(tarea.getHerramienta())) {
            throw new TareaNoDisponibleException("Para esto necesitás " + Catalogo.nombre(tarea.getHerramienta())
                    + (o == null ? ". Buscalo en el almacén y traelo en las manos."
                    : ", pero llevás " + o.getNombre() + ". Buscá lo correcto en el almacén."));
        }
    }

    public String arreglar(Tarea tarea) throws TareaNoDisponibleException {
        validarArreglo(tarea);
        manos.quitar(enMano());   // la herramienta se gasta
        tarea.setCompletada(true);
        jugador.sumarTareaCompletada();
        ajustarCordura(3);
        return tarea.getMensajeExito();
    }

    public String posponer(Tarea tarea) {
        return "Lo dejás para después.";
    }

    public String romper(Tarea tarea) {
        ajustarCordura(-15);
        return "Rompiste todo. Tu cordura baja.";
    }

    // ---------- días ----------

    /** Termina la jornada y pasa al día siguiente. Dejar cosas sin hacer le cuesta cordura; con muy poca, se pierde. */
    public String cerrarJornada() {
        long clientesPendientes = clientes.stream().filter(c -> !c.isAtendido()).count();
        long tareasPendientes = tareas.stream().filter(t -> !t.isCompletada()).count();
        String castigo = "";
        if (clientesPendientes > 0 || tareasPendientes > 0) {
            ajustarCordura((int) -(clientesPendientes * 10 + tareasPendientes * 5));
            castigo = "Dejaste " + clientesPendientes + " clientes y " + tareasPendientes + " tareas sin hacer. ";
        }
        if (!derrota && jugador.getCordura() < UMBRAL_CORDURA_DIA) {
            perder("Pasaste de día con la cordura por el piso (" + jugador.getCordura()
                    + "). Cuando amaneció, ya no eras vos quien atendía el buffet.");
        }
        if (derrota) {
            return motivoDerrota;
        }
        jugador.setDiaActual(jugador.getDiaActual() + 1);
        ajustarCordura(15);   // dormir un poco ayuda
        generarDia();
        return castigo + "Dormiste unas horas. Empieza el día " + jugador.getDiaActual() + ".";
    }

    public void reiniciarPartida() {
        jugador.reiniciar();
        derrota = false;
        motivoDerrota = "";
        generarDia();
    }

    // ---------- internos ----------

    private void ajustarCordura(int delta) {
        int c = Math.max(0, Math.min(100, jugador.getCordura() + delta));
        jugador.setCordura(c);
        if (c <= 0) {
            perder("Tu cordura llegó a cero. El buffet te tragó.");
        }
    }

    private void perder(String motivo) {
        derrota = true;
        motivoDerrota = motivo;
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String aQuien(Cliente cliente) {
        String n = cliente.getNombre();
        return n.startsWith("el ") ? "al " + n.substring(3) : "a " + n;
    }
}
