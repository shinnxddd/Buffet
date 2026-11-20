package service;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Herramienta;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Lugar;
import modelo.NPC;
import modelo.Objeto;
import modelo.Sala;
import modelo.Tarea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Reglas del juego. La interfaz solo dibuja y llama a estos métodos: acá viven los pedidos,
 * las herramientas, las salas, la cordura y el paso de los días.
 *
 * El turno tiene tres pasos por cliente: tomar el pedido en el mostrador, retirar la bandeja
 * de la estación de cocina que le toca y llevársela. Las tareas, en cambio, piden una
 * herramienta concreta que hay que buscar en el almacén.
 */
public class JuegoService {

    /** Si terminás el día por debajo de este número, perdés la partida. */
    public static final int CORDURA_MINIMA_DIA = 30;
    /** Lo que recuperás al dormir entre jornadas. */
    public static final int RECUPERACION_DIARIA = 8;
    /** Cada cuánto la presión del turno desgasta cordura mientras haya trabajo pendiente. */
    public static final long INTERVALO_PRESION_MS = 12_000;
    /** Con la cordura en este piso o menos, cualquier trabajo pendiente vuelve la presión crítica. */
    public static final int CORDURA_PRESION_CRITICA = 35;
    /** Cuando una sombra te alcanza en el apagón. */
    public static final int DANO_SOMBRA = 15;

    private static final int MAX_CLIENTES = 12;
    private static final int MAX_TAREAS = 7;

    private final Jugador jugador;
    private final Random azar = new Random();
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Tarea> tareas = new ArrayList<>();
    /** Tareas del día que todavía no se mostraron: se activan a medida que entregás pedidos. */
    private final List<Tarea> tareasPendientesDeActivar = new ArrayList<>();
    private final Inventario inventario = new Inventario();
    private final NPC conserje = new NPC(900, "el conserje",
            "Si la luz se va, no corras: el pasillo cambia de lugar.", true);

    private Sala sala = Sala.COMEDOR;
    private Objeto enMano;            // bandeja o herramienta: una sola cosa a la vez
    private boolean perdida;
    private boolean conserjeHablado;
    private String causaPerdida;

    public JuegoService(Jugador jugador) {
        this.jugador = jugador;
        // Red de seguridad: una partida guardada con 0 de cordura quedaría injugable.
        if (jugador.getCordura() <= 0) {
            jugador.setCordura(CORDURA_MINIMA_DIA);
        }
        prepararDia();
    }

    // ---------------------------------------------------------------- estado

    public Jugador getJugador() {
        return jugador;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Objeto getEnMano() {
        return enMano;
    }

    public List<Objeto> abrirInventario() {
        return inventario.getObjetos();
    }

    public List<Tarea> getTareas() {
        return Collections.unmodifiableList(tareas);
    }

    public boolean isPerdida() {
        return perdida;
    }

    public String getCausaPerdida() {
        return causaPerdida;
    }

    // ---------------------------------------------------------------- presión

    /**
     * Peso del trabajo que sigue abierto: cada cliente esperando suma uno y cada tarea
     * pendiente suma su prioridad. La presión no se guarda en la base: se deriva del estado.
     */
    public int presionPendiente() {
        int pendientes = (int) clientes.stream().filter(c -> !c.isAtendido()).count();
        int peso = tareas.stream().filter(t -> !t.isCompletada())
                .mapToInt(Tarea::getPrioridad).sum();
        return pendientes + peso;
    }

    /**
     * 0 = tranquilo, 1 = leve, 2 = tensa, 3 = alta, 4 = crítica.
     * Con poca cordura cualquier trabajo abierto se vuelve crítica: el turno ya no se aguanta.
     */
    public int nivelPresion() {
        int peso = presionPendiente();
        if (peso <= 0) {
            return 0;
        }
        if (jugador.getCordura() <= CORDURA_PRESION_CRITICA) {
            return 4;
        }
        if (peso <= 3) {
            return 1;
        }
        if (peso <= 7) {
            return 2;
        }
        if (peso <= 12) {
            return 3;
        }
        return 4;
    }

    public String getPresionEtiqueta() {
        return switch (nivelPresion()) {
            case 1 -> "LEVE";
            case 2 -> "TENSA";
            case 3 -> "ALTA";
            case 4 -> "CRÍTICA";
            default -> "TRANQUILA";
        };
    }

    /**
     * Latido pasivo de la jornada. La interfaz lo llama cada {@link #INTERVALO_PRESION_MS}
     * solo mientras se está jugando. Devuelve el daño aplicado (0 si no había presión).
     */
    public int latidoPresion() {
        if (perdida || turnoTerminado()) {
            return 0;
        }
        int nivel = nivelPresion();
        if (nivel <= 0) {
            return 0;
        }
        int dano = nivel >= 4 ? 4 : 2;
        restarCordura(dano);
        return dano;
    }

    // ---------------------------------------------------------------- progreso

    public Cliente clienteActual() {
        return clientes.stream().filter(c -> !c.isAtendido()).findFirst().orElse(null);
    }

    public boolean hayClientePendiente() {
        return clientes.stream().anyMatch(c -> !c.isAtendido());
    }

    public boolean hayTareaPendiente() {
        return tareas.stream().anyMatch(t -> !t.isCompletada());
    }

    public boolean hayTareaPendiente(Lugar lugar) {
        return tareaPendienteEn(lugar) != null;
    }

    public Tarea tareaPendienteEn(Lugar lugar) {
        return tareas.stream()
                .filter(t -> t.getLugar() == lugar && !t.isCompletada())
                .findFirst().orElse(null);
    }

    public int totalClientes() {
        return clientes.size();
    }

    public int clientesAtendidos() {
        return (int) clientes.stream().filter(Cliente::isAtendido).count();
    }

    /** Incluye las tareas del día que todavía esperan su turno para activarse. */
    public int totalTareas() {
        return tareas.size() + tareasPendientesDeActivar.size();
    }

    public int tareasHechas() {
        return (int) tareas.stream().filter(Tarea::isCompletada).count();
    }

    public int tareasPorActivar() {
        return tareasPendientesDeActivar.size();
    }

    /**
     * Pasa la próxima tarea de la cola a la sala. Se llama una vez por pedido entregado:
     * el turno se va complicando a medida que avanza, no de golpe al empezar.
     */
    public Tarea activarSiguienteTarea() {
        if (tareasPendientesDeActivar.isEmpty()) {
            return null;
        }
        Tarea siguiente = tareasPendientesDeActivar.remove(0);
        tareas.add(siguiente);
        return siguiente;
    }

    /** Ya no queda nada por atender ni por limpiar en la jornada. */
    public boolean turnoTerminado() {
        return !hayClientePendiente() && !hayTareaPendiente() && tareasPendientesDeActivar.isEmpty();
    }

    /** La tarea pendiente más peligrosa; la usa el HUD y el conserje. */
    public Tarea tareaMasUrgente() {
        return tareas.stream()
                .filter(t -> !t.isCompletada())
                .max((a, b) -> {
                    int cmp = Integer.compare(a.getPrioridad(), b.getPrioridad());
                    return cmp != 0 ? cmp : Integer.compare(b.getId(), a.getId());
                })
                .orElse(null);
    }

    /** Frase corta para la barra de objetivos de la interfaz. */
    public String getPista() {
        Tarea tablero = tareaPendienteEn(Lugar.TABLERO);
        if (tablero != null) {
            if (tieneHerramientaPara(Lugar.TABLERO)) {
                return "Sin luz. Cambiá el fusible en el tablero del comedor.";
            }
            if (tieneHerramientaGuardada(Lugar.TABLERO)) {
                return "Tenés el fusible en la mochila. Equipalo con I.";
            }
            return "Sin luz. Buscá un fusible en el almacén.";
        }
        Cliente c = clienteActual();
        if (c != null) {
            if (!c.isPedidoTomado()) {
                return "Tomale el pedido " + aQuien(c) + " en el mostrador.";
            }
            if (tieneBandejaPara(c)) {
                return "Llevale la bandeja con " + c.getPedido() + " " + aQuien(c) + ".";
            }
            Lugar estacion = estacionPara(c);
            Tarea enEstacion = tareaPendienteEn(estacion);
            if (enEstacion != null) {
                if (tieneHerramientaPara(estacion)) {
                    return "La " + nombreEstacion(estacion) + " está rota: arreglala ahora que tenés "
                            + herramientaPara(estacion).getNombre() + ".";
                }
                if (tieneHerramientaGuardada(estacion)) {
                    return "Para la " + nombreEstacion(estacion) + " necesitás "
                            + herramientaPara(estacion).getNombre() + ". Equipala con I.";
                }
                return "La " + nombreEstacion(estacion) + " no anda. Buscá "
                        + herramientaPara(estacion).getNombre() + " en el almacén.";
            }
            return "Retirá " + c.getPedido() + " de la " + nombreEstacion(estacion) + ".";
        }
        Tarea t = tareaMasUrgente();
        if (t != null) {
            String extra = "";
            if (t.requiereHerramienta() && !tieneHerramientaPara(t.getLugar())) {
                extra = " (necesitás " + t.getHerramienta().getNombre() + ")";
            }
            return "Andá a " + t.getLugar().getEtiqueta() + " en " + t.getLugar().getSala().getNombre()
                    + extra + ".";
        }
        return "Turno terminado. Cerrá la caja y pasá el día.";
    }

    // ---------------------------------------------------------------- clientes

    /** Primer paso: anotar lo que quiere el cliente que está en el mostrador. */
    public String tomarPedido() throws InteraccionInvalidaException {
        if (sala != Sala.COMEDOR) {
            throw new InteraccionInvalidaException("El mostrador está en el comedor.");
        }
        Cliente c = clienteActual();
        if (c == null) {
            throw new InteraccionInvalidaException("No hay nadie esperando en el mostrador.");
        }
        if (c.isPedidoTomado()) {
            throw new InteraccionInvalidaException("Ya le tomaste el pedido " + aQuien(c)
                    + ". Ahora retirá la bandeja en la cocina.");
        }
        drenaje();
        c.marcarPedidoTomado();
        return c.getNombre() + " pide " + c.getPedido()
                + ". Anotado. Retirá la bandeja de " + nombreEstacion(estacionPara(c)) + ".";
    }

    /**
     * Interacción con una estación de cocina. Si está rota, se repara con su herramienta;
     * si está limpia, el pedido se retira directamente de ahí, sin pasar por otra barra.
     */
    public String interactuarEstacion(Lugar estacion)
            throws InteraccionInvalidaException, TareaNoDisponibleException {
        if (estacion.getSala() != sala) {
            throw new InteraccionInvalidaException("Esa estación no está en esta sala.");
        }
        // Si la estación todavía esperaba su turno en la cola, se revela al interactuar.
        activarTareaDeEstacion(estacion);
        if (tareaPendienteEn(estacion) != null) {
            return usarHerramientaEnTarea(estacion);
        }
        return prepararEnEstacion(estacion);
    }

    /** Segundo paso: retirar la bandeja de la estación que corresponde al pedido. */
    public String prepararEnEstacion(Lugar estacion) throws InteraccionInvalidaException {
        if (sala != Sala.COCINA) {
            throw new InteraccionInvalidaException("Las bandejas se retiran en la cocina.");
        }
        Cliente c = clienteActual();
        if (c == null) {
            throw new InteraccionInvalidaException("No hay ningún pedido pendiente en el mostrador.");
        }
        if (!c.isPedidoTomado()) {
            throw new InteraccionInvalidaException("Todavía no le tomaste el pedido " + aQuien(c) + ".");
        }
        if (estacionPara(c) != estacion) {
            throw new InteraccionInvalidaException("Ese pedido no se retira de " + nombreEstacion(estacion)
                    + ": corresponde a " + nombreEstacion(estacionPara(c)) + ".");
        }
        if (esBandejaPara(enMano, c)) {
            return "Ya tenés la bandeja con " + c.getPedido() + ". Llevala al mostrador.";
        }
        if (enMano != null) {
            throw new InteraccionInvalidaException("Tenés las manos ocupadas con " + enMano.getNombre()
                    + ". Soltá eso con Q antes de retirar la bandeja.");
        }
        // Si la estación todavía no se rompió a la vista, se adelanta su tarea: primero
        // se repara y recién después se puede retirar el pedido.
        activarTareaDeEstacion(estacion);
        Tarea pendiente = tareaPendienteEn(estacion);
        if (pendiente != null) {
            throw new InteraccionInvalidaException(pendiente.getTitulo()
                    + ". Usá " + herramientaPara(estacion).getNombre()
                    + " para arreglarla y volvé a intentar.");
        }
        drenaje();
        enMano = new Objeto(1000 + c.getId(), "una bandeja con " + c.getPedido(), "PEDIDO", false, 0, null);
        return "Retiraste una bandeja con " + c.getPedido() + " de la " + nombreEstacion(estacion)
                + ". No la dejes enfriar.";
    }

    /**
     * La estación que necesita cada pedido. Las bebidas y lo frío salen de la heladera,
     * el café y las infusiones de la cafetera, y todo lo que se cocina de la plancha.
     */
    public static Lugar estacionPara(Cliente cliente) {
        String pedido = cliente.getPedido().toLowerCase();
        if (pedido.contains("café") || pedido.contains("cafe")
                || pedido.contains("té") || pedido.contains("chocolate")) {
            return Lugar.CAFETERA;
        }
        if (pedido.contains("agua") || pedido.contains("gaseosa") || pedido.contains("leche")
                || pedido.contains("alfajor") || pedido.contains("flan")
                || pedido.contains("torta") || pedido.contains("pan dulce")) {
            return Lugar.HELADERA;
        }
        return Lugar.PLANCHA;
    }

    /** Nombre en minúsculas de la estación, para armar los mensajes. */
    public static String nombreEstacion(Lugar estacion) {
        return switch (estacion) {
            case PLANCHA -> "plancha";
            case CAFETERA -> "cafetera";
            case HELADERA -> "heladera";
            default -> estacion.getEtiqueta().toLowerCase();
        };
    }

    /**
     * Si la tarea de esa estación todavía estaba en la cola, la adelanta: el pedido la
     * necesita ya y no tiene sentido dejar al jugador trabado esperando otra entrega.
     */
    private void activarTareaDeEstacion(Lugar estacion) {
        if (tareaPendienteEn(estacion) != null) {
            return;
        }
        for (int i = 0; i < tareasPendientesDeActivar.size(); i++) {
            if (tareasPendientesDeActivar.get(i).getLugar() == estacion) {
                tareas.add(tareasPendientesDeActivar.remove(i));
                return;
            }
        }
    }

    /** Tercer paso: entregar la bandeja correcta al cliente del mostrador. */
    public String entregarPedido() throws InteraccionInvalidaException, PedidoInvalidoException {
        if (sala != Sala.COMEDOR) {
            throw new InteraccionInvalidaException("El cliente te espera en el mostrador del comedor.");
        }
        Cliente c = clienteActual();
        if (c == null) {
            throw new InteraccionInvalidaException("No hay nadie esperando en el mostrador.");
        }
        if (!c.isPedidoTomado()) {
            throw new InteraccionInvalidaException("Primero tomale el pedido " + aQuien(c) + ".");
        }
        if (!esBandejaPara(enMano, c)) {
            throw new PedidoInvalidoException("Necesitás la bandeja con " + c.getPedido()
                    + ". Retirala de " + nombreEstacion(estacionPara(c)) + ".");
        }
        drenaje();
        c.marcarAtendido();
        enMano = null;
        sumarCordura(6);
        String mensaje = "Le entregaste " + c.getPedido() + " " + aQuien(c) + " y se fue conforme. +6 de cordura.";
        Tarea nueva = activarSiguienteTarea();
        if (nueva != null) {
            mensaje += " Algo más para atender: " + nueva.getDescripcion() + ".";
        }
        return mensaje;
    }

    public boolean tieneBandejaPara(Cliente c) {
        return esBandejaPara(enMano, c);
    }

    private boolean esBandejaPara(Objeto objeto, Cliente c) {
        return objeto != null
                && "PEDIDO".equals(objeto.getTipoObjeto())
                && objeto.getNombre().equals("una bandeja con " + c.getPedido());
    }

    /** "a el profesor" no existe: la contracción es "al profesor". */
    private static String aQuien(Cliente cliente) {
        String n = cliente.getNombre();
        return n.startsWith("el ") ? "al " + n.substring(3) : "a " + n;
    }

    // ---------------------------------------------------------------- tareas

    /**
     * Intenta hacer la tarea pendiente de un lugar. La herramienta tiene que ser la que
     * corresponde y se consume al usarla: si hace falta de nuevo, se busca en el almacén.
     */
    public String usarHerramientaEnTarea(Lugar lugar) throws TareaNoDisponibleException {
        if (lugar.getSala() != sala) {
            throw new TareaNoDisponibleException("Ese lugar no está en esta sala.");
        }
        Tarea tarea = tareaPendienteEn(lugar);
        if (tarea == null) {
            throw new TareaNoDisponibleException("Acá ya no queda nada por hacer.");
        }
        Herramienta requerida = tarea.getHerramienta();
        if (requerida != null) {
            if (!(enMano instanceof Herramienta)) {
                throw new TareaNoDisponibleException("Necesitás " + requerida.getNombre()
                        + " para arreglar " + lugar.getEtiqueta() + ". Está en el almacén.");
            }
            Herramienta enManoHerramienta = (Herramienta) enMano;
            if (!enManoHerramienta.sirvePara(lugar)) {
                throw new TareaNoDisponibleException("Esa herramienta no sirve para "
                        + lugar.getEtiqueta() + ". Necesitás " + requerida.getNombre() + ".");
            }
        }

        drenaje();
        tarea.setCompletada(true);
        jugador.sumarTareaCompletada();
        int recuperacion = 2 + tarea.getPrioridad();
        sumarCordura(recuperacion);

        String mensaje = tarea.getMensajeExito();
        if (requerida != null) {
            // La herramienta se gastó en la tarea: desaparece de la mano y de la mochila.
            // Para la próxima hay que volver al estante del almacén a buscarla.
            enMano = null;
            inventario.getObjetos().removeIf(o -> o instanceof Herramienta h && h.sirvePara(lugar));
        }
        Objeto premio = tarea.getRecompensa();
        if (premio != null) {
            inventario.agregar(premio);
            mensaje += " Encontraste " + premio.getNombre() + " y lo guardaste en la mochila.";
        }
        return mensaje + " +" + recuperacion + " de cordura.";
    }

    /** Se lleva una herramienta del estante del almacén (queda en la mano). */
    public String recogerHerramienta(Lugar lugar) throws InteraccionInvalidaException {
        if (sala != Sala.ALMACEN) {
            throw new InteraccionInvalidaException("Las herramientas están en el almacén.");
        }
        Herramienta nueva = herramientaPara(lugar);

        if (enMano instanceof Herramienta) {
            Herramienta actual = (Herramienta) enMano;
            if (actual.getLugar() == lugar) {
                return "Ya tenés " + actual.getNombre() + " en la mano.";
            }
            drenaje();
            inventario.agregar(actual);
            enMano = nueva;
            return "Guardaste " + actual.getNombre() + " en la mochila y agarraste " + nueva.getNombre()
                    + ". Ahora andá a " + lugar.getEtiqueta() + ".";
        }
        if (enMano != null) {
            throw new InteraccionInvalidaException("Tenés las manos ocupadas con " + enMano.getNombre()
                    + ". Soltá eso con Q antes de agarrar una herramienta.");
        }
        drenaje();
        enMano = nueva;
        return "Agarraste " + nueva.getNombre() + ". Ahora andá a " + lugar.getEtiqueta() + ".";
    }

    public boolean tieneHerramientaPara(Lugar lugar) {
        return enMano instanceof Herramienta && ((Herramienta) enMano).sirvePara(lugar);
    }

    /** La herramienta está en la mochila, todavía sin equipar. */
    public boolean tieneHerramientaGuardada(Lugar lugar) {
        return inventario.getObjetos().stream()
                .anyMatch(o -> o instanceof Herramienta && ((Herramienta) o).sirvePara(lugar));
    }

    /** La linterna del tablero sirve para atravesar un apagón. */
    public boolean tieneLinterna() {
        if (enMano != null && enMano.getNombre().toLowerCase().contains("linterna")) {
            return true;
        }
        return inventario.getObjetos().stream()
                .anyMatch(o -> o.getNombre().toLowerCase().contains("linterna"));
    }

    /**
     * El conserje del almacén es el único que habla del apagón sin gritar. Da una pista
     * sobre la tarea más urgente y, una vez por día, un poco de calma.
     */
    public String hablarConConserje() throws InteraccionInvalidaException {
        if (sala != Sala.ALMACEN) {
            throw new InteraccionInvalidaException("El conserje espera junto a los estantes del almacén.");
        }
        Tarea urgente = tareaMasUrgente();
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("\"").append(conserje.getDialogo()).append("\"");
        if (conserje.isPuedeDarTarea() && urgente != null) {
            mensaje.append(" Lo más urgente: ").append(urgente.getDescripcion())
                    .append(". Está en ").append(urgente.getLugar().getEtiqueta()).append(".");
        } else if (conserje.isPuedeDarTarea()) {
            mensaje.append(turnoTerminado()
                    ? " No queda nada pendiente. Cerrá la caja y andá a dormir."
                    : " No queda ninguna tarea pendiente. Terminá los pedidos que faltan.");
        }
        if (!conserjeHablado) {
            conserjeHablado = true;
            sumarCordura(2);
            mensaje.append(" Te quedás un momento en silencio. +2 de cordura.");
        }
        return mensaje.toString();
    }

    public static Herramienta herramientaPara(Lugar lugar) {
        String nombre;
        switch (lugar) {
            case BANO: nombre = "un trapeador"; break;
            case MESAS: nombre = "una esponja"; break;
            case BASURA: nombre = "una escoba"; break;
            case TABLERO: nombre = "un fusible"; break;
            case PLANCHA: nombre = "una espátula"; break;
            case CAFETERA: nombre = "una llave inglesa"; break;
            case HELADERA: nombre = "unos guantes de goma"; break;
            default: nombre = "una herramienta"; break;
        }
        return new Herramienta(400 + lugar.ordinal(), nombre, lugar);
    }

    // ---------------------------------------------------------------- objetos

    /** Suelta lo que tenés en la mano. Las herramientas vuelven a la mochila; las bandejas se tiran. */
    public String soltarEnMano() throws ObjetoNoDisponibleException {
        if (enMano == null) {
            throw new ObjetoNoDisponibleException("No tenés nada en la mano.");
        }
        Objeto soltado = enMano;
        enMano = null;
        if (soltado instanceof Herramienta) {
            inventario.agregar(soltado);
            return "Guardaste " + soltado.getNombre() + " en la mochila.";
        }
        restarCordura(4);
        return "Tiraste " + soltado.getNombre() + " a la basura. -4 de cordura.";
    }

    /**
     * Usa un objeto de la mochila. Las herramientas se equipan en la mano; los consumibles
     * cambian la cordura y desaparecen.
     */
    public String usarObjeto(Objeto objeto) throws ObjetoNoDisponibleException, InteraccionInvalidaException {
        if (!inventario.getObjetos().contains(objeto)) {
            throw new ObjetoNoDisponibleException("Ese objeto ya no está en la mochila.");
        }
        if (objeto instanceof Herramienta) {
            if (enMano != null && !(enMano instanceof Herramienta)) {
                throw new InteraccionInvalidaException("Tenés las manos ocupadas con " + enMano.getNombre()
                        + ". Soltala con Q primero.");
            }
            if (enMano != null) {
                inventario.agregar(enMano);
            }
            inventario.quitar(objeto);
            enMano = objeto;
            return "Te colgaste " + objeto.getNombre() + " en el cinturón.";
        }
        if (!objeto.isUtilizable()) {
            throw new ObjetoNoDisponibleException(objeto.getNombre() + " no se puede usar ahora mismo.");
        }
        int cordura = jugador.getCordura();
        int efecto = objeto.getEfectoCordura();
        if (efecto > 0) {
            jugador.setCordura(Math.min(100, cordura + efecto));
        } else if (efecto < 0) {
            restarCordura(-efecto);
        }
        inventario.quitar(objeto);
        String extra = objeto.getMensajeUso() == null ? "" : " " + objeto.getMensajeUso();
        return "Usaste " + objeto.getNombre() + "." + extra;
    }

    /** Atajo de la tecla I: usa el primer consumible; si no hay, equipa la primera herramienta. */
    public String usarLoPrimero() throws ObjetoNoDisponibleException, InteraccionInvalidaException {
        Objeto elegido = inventario.getObjetos().stream()
                .filter(Objeto::isUtilizable)
                .findFirst()
                .orElseGet(() -> inventario.getObjetos().stream()
                        .filter(o -> o instanceof Herramienta)
                        .findFirst().orElse(null));
        if (elegido == null) {
            throw new ObjetoNoDisponibleException("No tenés ningún objeto que puedas usar en la mochila.");
        }
        return usarObjeto(elegido);
    }

    // ---------------------------------------------------------------- cordura y días

    /** Resultado de cerrar la jornada. */
    public record ResultadoDia(boolean avanzo, boolean perdio, String titulo, String detalle) { }

    /**
     * Cierra el turno y arranca el día siguiente. Si la cordura quedó por debajo del mínimo,
     * la partida termina: el buffet no perdona dos noches seguidas.
     */
    public ResultadoDia avanzarDia() {
        if (perdida) {
            return new ResultadoDia(false, true, "La partida terminó", causaPerdida);
        }
        if (!turnoTerminado()) {
            return new ResultadoDia(false, false, "Todavía queda trabajo",
                    "Atendé a todos los clientes y terminá las tareas antes de cerrar la caja.");
        }
        int cordura = jugador.getCordura();
        if (cordura < CORDURA_MINIMA_DIA) {
            perdida = true;
            causaPerdida = "Terminaste el día con " + cordura + " de cordura. La noche te encontró solo "
                    + "en el buffet y no volviste a abrir los ojos.";
            return new ResultadoDia(true, true, "Noche del día " + jugador.getDiaActual(), causaPerdida);
        }

        int diaAnterior = jugador.getDiaActual();
        jugador.setDiaActual(diaAnterior + 1);
        jugador.setCordura(Math.min(100, cordura + RECUPERACION_DIARIA));
        prepararDia();
        sala = Sala.COMEDOR;
        enMano = null;
        return new ResultadoDia(true, false, "Día " + jugador.getDiaActual(),
                "Dormiste poco, pero sobreviviste. Recuperaste " + RECUPERACION_DIARIA
                        + " de cordura. Hoy hay " + clientes.size() + " clientes y "
                        + totalTareas() + " tareas.");
    }

    /**
     * Una sombra del apagón te alcanza. No hace daño físico: te saca cordura y te deja
     * el pulso temblando. La interfaz la usa como confirmación del golpe.
     */
    public String golpeSombra() {
        restarCordura(DANO_SOMBRA);
        return "Una sombra te rozó la nuca. -" + DANO_SOMBRA + " de cordura.";
    }

    /** Cada interacción desgasta un poco. Con el tablero roto, el doble. */
    private void drenaje() {
        restarCordura(hayTareaPendiente(Lugar.TABLERO) ? 4 : 2);
    }

    private void sumarCordura(int cantidad) {
        jugador.setCordura(Math.min(100, jugador.getCordura() + cantidad));
    }

    private void restarCordura(int cantidad) {
        jugador.setCordura(Math.max(0, jugador.getCordura() - cantidad));
        if (jugador.getCordura() <= 0 && !perdida) {
            perdida = true;
            causaPerdida = "Tu cordura llegó a cero. Las luces del buffet se apagaron una por una "
                    + "y ya no supiste cuál era la puerta de salida.";
        }
    }

    // ---------------------------------------------------------------- armado del día

    private void prepararDia() {
        clientes.clear();
        int dia = Math.max(1, jugador.getDiaActual());
        conserjeHablado = false;

        int cantidadClientes = Math.min(MAX_CLIENTES, 4 + dia);
        List<Perfil> perfiles = new ArrayList<>(List.of(PERFILES));
        Collections.shuffle(perfiles, azar);
        for (int i = 0; i < cantidadClientes; i++) {
            Perfil p = perfiles.get(i % perfiles.size());
            clientes.add(new Cliente(i + 1, p.nombre(), p.pedido()));
        }

        // Las tareas no se borran al dormir: lo que quedó pendiente sigue activo y presiona.
        tareas.removeIf(Tarea::isCompletada);
        tareasPendientesDeActivar.clear();
        int objetivo = Math.min(MAX_TAREAS, 2 + (dia + 1) / 2);

        List<Tarea> nuevas = new ArrayList<>();
        // El apagón abre la noche: el tablero es la primera tarea que se activa.
        if (dia == 1 && !hayTareaPendiente(Lugar.TABLERO)) {
            nuevas.add(crearTarea(Lugar.TABLERO, dia, siguienteNumero(dia, nuevas)));
        }
        // No todas las noches se rompe la cocina entera: cada día se eligen al azar una,
        // dos o tres estaciones. Las que ya venían rotas de la noche anterior se respetan.
        List<Lugar> estaciones = new ArrayList<>(List.of(Lugar.PLANCHA, Lugar.CAFETERA, Lugar.HELADERA));
        Collections.shuffle(estaciones, azar);
        int estacionesRotas = 1 + azar.nextInt(3);
        for (int i = 0; i < estacionesRotas; i++) {
            Lugar estacion = estaciones.get(i);
            boolean yaExiste = tareas.stream().anyMatch(t -> t.getLugar() == estacion)
                    || nuevas.stream().anyMatch(t -> t.getLugar() == estacion);
            if (!yaExiste) {
                nuevas.add(crearTarea(estacion, dia, siguienteNumero(dia, nuevas)));
            }
        }
        List<Lugar> libres = new ArrayList<>();
        for (Lugar lugar : Lugar.values()) {
            boolean ocupado = tareas.stream().anyMatch(t -> t.getLugar() == lugar)
                    || nuevas.stream().anyMatch(t -> t.getLugar() == lugar);
            if (!ocupado) {
                libres.add(lugar);
            }
        }
        Collections.shuffle(libres, azar);
        for (Lugar lugar : libres) {
            if (tareas.size() + nuevas.size() >= objetivo) {
                break;
            }
            nuevas.add(crearTarea(lugar, dia, siguienteNumero(dia, nuevas)));
        }
        // Ninguna tarea nueva arranca activa: cada pedido entregado habilita la siguiente.
        tareasPendientesDeActivar.addAll(nuevas);
    }

    /** Los ids no se repiten aunque una tarea vieja siga pendiente o esperando en la cola. */
    private int siguienteNumero(int dia, List<Tarea> nuevas) {
        for (int numero = 1; numero < 100; numero++) {
            final int candidato = numero;
            boolean usado = tareas.stream().anyMatch(t -> t.getId() == dia * 100 + candidato)
                    || nuevas.stream().anyMatch(t -> t.getId() == dia * 100 + candidato);
            if (!usado) {
                return candidato;
            }
        }
        return 1;
    }

    private Tarea crearTarea(Lugar lugar, int dia, int numero) {
        int id = dia * 100 + numero;
        Herramienta herramienta = herramientaPara(lugar);
        switch (lugar) {
            case BANO:
                return new Tarea(id, "el piso del baño vuelve a llenarse de agua", lugar,
                        "El baño no se seca",
                        "Trapeaste el piso. El agua vuelve a aparecer detrás tuyo, pero el espejo te deja pasar.",
                        new Objeto(701, "un jabón de hotel", "JABON", true, 5,
                                "Te lavaste la cara. Por un segundo el espejo te devolvió tu reflejo."),
                        herramienta, 1);
            case MESAS:
                return new Tarea(id, "las mesas quedaron pegajosas y con restos de comida", lugar,
                        "Mesas pegajosas",
                        "Dejaste las mesas relucientes. Debajo de un plato había una propina.",
                        new Objeto(702, "una propina", "MONEDA", true, 6,
                                "Te compraste un alfajor con la propina. Sabe a plástico."),
                        herramienta, 2);
            case BASURA:
                return new Tarea(id, "la bolsa de basura está por explotar y hay moscas por todos lados", lugar,
                        "La basura vive",
                        "Barriste la basura al patio. No mires hacia atrás.",
                        new Objeto(703, "una nota arrugada", "NOTA", true, -10,
                                "Dice: \"NO LE DES NADA AL ÚLTIMO CLIENTE\". La letra es tuya."),
                        herramienta, 2);
            case TABLERO:
                return new Tarea(id, "el tablero de luz chispea y las luces parpadean solas", lugar,
                        "Cortocircuito",
                        "Cambiaste el fusible. La luz vuelve a ser estable. Había una linterna en el cajón.",
                        new Objeto(704, "una linterna", "HERRAMIENTA", true, 7,
                                "Alumbra rincones donde antes no te animabas a mirar."),
                        herramienta, 3);
            case PLANCHA:
                return new Tarea(id, "la plancha está llena de grasa quemada y larga un humo raro", lugar,
                        "La plancha humea",
                        "Raspaste la plancha hasta que quedó como espejo. Algo brilló en la grasa.",
                        new Objeto(705, "una llave oxidada", "LLAVE", false),
                        herramienta, 2);
            case CAFETERA:
                return new Tarea(id, "la cafetera gotea algo oscuro que no parece café", lugar,
                        "Goteo negro",
                        "Ajustaste la cafetera y salió un café perfecto.",
                        new Objeto(706, "un café recién hecho", "BEBIDA", true, 10,
                                "Está caliente y por un rato todo parece normal."),
                        herramienta, 2);
            case HELADERA:
            default:
                return new Tarea(id, "la heladera quedó abierta y hay bebidas tiradas por el piso", lugar,
                        "La heladera no cierra",
                        "Acomodaste todo y cerraste la heladera. Se salvó una gaseosa.",
                        new Objeto(707, "una gaseosa fría", "BEBIDA", true, 9, "Qué alivio."),
                        herramienta, 2);
        }
    }

    // ---------------------------------------------------------------- datos fijos

    private record Perfil(String nombre, String pedido) { }

    private static final Perfil[] PERFILES = {
            new Perfil("un cliente", "un café"),
            new Perfil("una nena de guardapolvo", "un alfajor"),
            new Perfil("el profesor de gimnasia", "una botella de agua"),
            new Perfil("un señor de impermeable", "un tostado de jamón y queso"),
            new Perfil("la directora", "un sándwich de milanesa"),
            new Perfil("la portera", "un té con dos de azúcar"),
            new Perfil("un chico con mochila", "una empanada de carne"),
            new Perfil("una mujer con velo", "un flan casero"),
            new Perfil("el kiosquero", "un vaso de leche"),
            new Perfil("una señora mayor", "un pan dulce"),
            new Perfil("un nene descalzo", "una porción de torta"),
            new Perfil("el auxiliar de turno", "una milanesa con puré"),
            new Perfil("una alumna repetidora", "un chocolate caliente"),
            new Perfil("el último cliente", "lo que sea"),
            new Perfil("un hombre de traje", "una gaseosa"),
            new Perfil("una gemela", "otra gaseosa")
    };
}
