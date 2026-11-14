package service;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Herramienta;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Lugar;
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
 * El turno tiene tres pasos por cliente: tomar el pedido en el mostrador, preparar la bandeja
 * en la cocina y llevársela. Las tareas, en cambio, piden una herramienta concreta que hay
 * que buscar en el almacén.
 */
public class JuegoService {

    /** Si terminás el día por debajo de este número, perdés la partida. */
    public static final int CORDURA_MINIMA_DIA = 30;
    /** Lo que recuperás al dormir entre jornadas. */
    public static final int RECUPERACION_DIARIA = 8;

    private static final int MAX_CLIENTES = 12;
    private static final int MAX_TAREAS = 7;

    private final Jugador jugador;
    private final Random azar = new Random();
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Tarea> tareas = new ArrayList<>();
    private final Inventario inventario = new Inventario();

    private Sala sala = Sala.COMEDOR;
    private Objeto enMano;            // bandeja o herramienta: una sola cosa a la vez
    private boolean perdida;
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

    public List<Cliente> getClientes() {
        return Collections.unmodifiableList(clientes);
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

    public int totalTareas() {
        return tareas.size();
    }

    public int tareasHechas() {
        return (int) tareas.stream().filter(Tarea::isCompletada).count();
    }

    /** Ya no queda nada por atender ni por limpiar en la jornada. */
    public boolean turnoTerminado() {
        return !hayClientePendiente() && !hayTareaPendiente();
    }

    /** Frase corta para la barra de objetivos de la interfaz. */
    public String getPista() {
        Cliente c = clienteActual();
        if (c != null) {
            if (!c.isPedidoTomado()) {
                return "Tomale el pedido a " + c.getNombre() + " en el mostrador.";
            }
            if (tieneBandejaPara(c)) {
                return "Llevale la bandeja con " + c.getPedido() + " a " + c.getNombre() + ".";
            }
            return "Prepará " + c.getPedido() + " en la mesada de la cocina.";
        }
        Tarea t = tareas.stream().filter(x -> !x.isCompletada()).findFirst().orElse(null);
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
            throw new InteraccionInvalidaException("Ya le tomaste el pedido a " + c.getNombre()
                    + ". Ahora hay que prepararlo.");
        }
        drenaje();
        c.marcarPedidoTomado();
        return c.getNombre() + " pide " + c.getPedido()
                + ". Anotado. Andá a la cocina y prepará la bandeja.";
    }

    /** Segundo paso: armar la bandeja en la cocina. */
    public String prepararPedido() throws InteraccionInvalidaException {
        if (sala != Sala.COCINA) {
            throw new InteraccionInvalidaException("Las bandejas se preparan en la cocina.");
        }
        Cliente c = clienteActual();
        if (c == null) {
            throw new InteraccionInvalidaException("No hay ningún pedido pendiente en el mostrador.");
        }
        if (!c.isPedidoTomado()) {
            throw new InteraccionInvalidaException("Todavía no le tomaste el pedido a " + c.getNombre() + ".");
        }
        if (enMano != null) {
            if (esBandejaPara(enMano, c)) {
                return "Ya tenés la bandeja con " + c.getPedido() + ". Llevala al mostrador.";
            }
            throw new InteraccionInvalidaException("Tenés las manos ocupadas con " + enMano.getNombre()
                    + ". Soltá eso con Q antes de preparar.");
        }
        drenaje();
        enMano = new Objeto(1000 + c.getId(), "una bandeja con " + c.getPedido(), "PEDIDO", false, 0, null);
        return "Preparaste una bandeja con " + c.getPedido() + ". No la dejes enfriar.";
    }

    /** Tercer paso: entregar la bandeja correcta al cliente del mostrador. */
    public String entregarPedido() throws InteraccionInvalidaException {
        if (sala != Sala.COMEDOR) {
            throw new InteraccionInvalidaException("El cliente te espera en el mostrador del comedor.");
        }
        Cliente c = clienteActual();
        if (c == null) {
            throw new InteraccionInvalidaException("No hay nadie esperando en el mostrador.");
        }
        if (!c.isPedidoTomado()) {
            throw new InteraccionInvalidaException("Primero tomale el pedido a " + c.getNombre() + ".");
        }
        if (!esBandejaPara(enMano, c)) {
            throw new InteraccionInvalidaException("Necesitás la bandeja con " + c.getPedido()
                    + ". Preparala en la cocina.");
        }
        drenaje();
        c.marcarAtendido();
        enMano = null;
        sumarCordura(6);
        return "Le entregaste " + c.getPedido() + " " + aQuien(c) + " y se fue conforme. +5 de cordura.";
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
     * Intenta hacer la tarea pendiente de un lugar. La herramienta se lleva puesta:
     * no se gasta, pero tiene que ser la que corresponde.
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
                throw new TareaNoDisponibleException("Para " + tarea.getTitulo() + " necesitás "
                        + requerida.getNombre() + ". Está en el almacén.");
            }
            Herramienta enManoHerramienta = (Herramienta) enMano;
            if (!enManoHerramienta.sirvePara(lugar)) {
                throw new TareaNoDisponibleException("Eso no sirve acá. Necesitás "
                        + requerida.getNombre() + " para " + lugar.getEtiqueta() + ".");
            }
        }

        drenaje();
        tarea.setCompletada(true);
        jugador.sumarTareaCompletada();
        sumarCordura(4);

        String mensaje = tarea.getMensajeExito();
        Objeto premio = tarea.getRecompensa();
        if (premio != null) {
            inventario.agregar(premio);
            mensaje += " Encontraste " + premio.getNombre() + " y lo guardaste en la mochila.";
        }
        return mensaje + " +3 de cordura.";
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
                    + ". Soltala con Q antes de agarrar una herramienta.");
        }
        drenaje();
        enMano = nueva;
        return "Agarraste " + nueva.getNombre() + ". Ahora andá a " + lugar.getEtiqueta() + ".";
    }

    public boolean tieneHerramientaPara(Lugar lugar) {
        return enMano instanceof Herramienta && ((Herramienta) enMano).sirvePara(lugar);
    }

    /** El nombre de la herramienta que pide un lugar (aunque no la tengas). */
    public static String nombreHerramienta(Lugar lugar) {
        return herramientaPara(lugar).getNombre();
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
        restarCordura(2);
        return "Tiraste " + soltado.getNombre() + " a la basura. -2 de cordura.";
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
            throw new ObjetoNoDisponibleException("No tenés nada usable en la mochila.");
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
                        + tareas.size() + " tareas.");
    }

    /** Cada interacción desgasta un poco. Con el tablero roto, el doble. */
    private void drenaje() {
        restarCordura(hayTareaPendiente(Lugar.TABLERO) ? 2 : 1);
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
        tareas.clear();
        int dia = Math.max(1, jugador.getDiaActual());

        int cantidadClientes = Math.min(MAX_CLIENTES, 4 + dia);
        List<Perfil> perfiles = new ArrayList<>(List.of(PERFILES));
        Collections.shuffle(perfiles, azar);
        for (int i = 0; i < cantidadClientes; i++) {
            Perfil p = perfiles.get(i % perfiles.size());
            clientes.add(new Cliente(i + 1, p.nombre(), p.pedido(), p.respuestas()));
        }

        int cantidadTareas = Math.min(MAX_TAREAS, 3 + dia);
        List<Lugar> lugares = new ArrayList<>(List.of(Lugar.values()));
        Collections.shuffle(lugares, azar);
        for (int i = 0; i < cantidadTareas; i++) {
            tareas.add(crearTarea(lugares.get(i), dia, i + 1));
        }
    }

    private Tarea crearTarea(Lugar lugar, int dia, int numero) {
        int id = dia * 100 + numero;
        Herramienta herramienta = herramientaPara(lugar);
        switch (lugar) {
            case BANO:
                return new Tarea(id, "el baño está sucio y el piso brilla de una forma rara", dia, false, lugar,
                        "El baño no se seca", "Trapeaste el piso. El agua volvió a aparecer detrás tuyo.",
                        new Objeto(701, "un jabón de hotel", "JABON", true, 5,
                                "Te lavaste la cara. Por un segundo el espejo te devolvió tu reflejo."),
                        herramienta);
            case MESAS:
                return new Tarea(id, "las mesas quedaron pegajosas y con restos de comida", dia, false, lugar,
                        "Mesas pegajosas", "Dejaste las mesas relucientes. Debajo de un plato había una propina.",
                        new Objeto(702, "una propina", "MONEDA", true, 6,
                                "Te compraste un alfajor con la propina. Sabe a plástico."),
                        herramienta);
            case BASURA:
                return new Tarea(id, "la bolsa de basura está por explotar y hay moscas por todos lados", dia, false, lugar,
                        "La basura vive", "Barriste la basura al patio. No mires hacia atrás.",
                        new Objeto(703, "una nota arrugada", "NOTA", true, -6,
                                "Dice: \"NO LE DES NADA AL ÚLTIMO CLIENTE\". La letra es tuya."),
                        herramienta);
            case TABLERO:
                return new Tarea(id, "el tablero de luz chispea y las luces parpadean solas", dia, false, lugar,
                        "Cortocircuito", "Cambiaste el fusible. La luz vuelve a ser estable. Había una linterna en el cajón.",
                        new Objeto(704, "una linterna", "HERRAMIENTA", true, 7,
                                "Alumbra rincones donde antes no te animabas a mirar."),
                        herramienta);
            case PLANCHA:
                return new Tarea(id, "la plancha está llena de grasa quemada y larga un humo raro", dia, false, lugar,
                        "La plancha humea", "Raspaste la plancha hasta que quedó como espejo. Algo brilló en la grasa.",
                        new Objeto(705, "una llave oxidada", "LLAVE", false),
                        herramienta);
            case CAFETERA:
                return new Tarea(id, "la cafetera gotea algo oscuro que no parece café", dia, false, lugar,
                        "Goteo negro", "Ajustaste la cafetera y salió un café perfecto.",
                        new Objeto(706, "un café recién hecho", "BEBIDA", true, 10,
                                "Está caliente y por un rato todo parece normal."),
                        herramienta);
            case HELADERA:
            default:
                return new Tarea(id, "la heladera quedó abierta y hay bebidas tiradas por el piso", dia, false, lugar,
                        "La heladera no cierra", "Acomodaste todo y cerraste la heladera. Se salvó una gaseosa.",
                        new Objeto(707, "una gaseosa fría", "BEBIDA", true, 9, "Qué alivio."),
                        herramienta);
        }
    }

    // ---------------------------------------------------------------- datos fijos

    private record Perfil(String nombre, String pedido, String[] respuestas) { }

    private static final Perfil[] PERFILES = {
            new Perfil("un cliente", "un café", new String[]{
                    "Un cliente no contesta y se queda mirándote fijo.",
                    "Un cliente parpadea una sola vez. Tarda demasiado en volver a abrir los ojos."}),
            new Perfil("una nena de guardapolvo", "un alfajor", new String[]{
                    "La nena dice que su mamá la espera afuera. Afuera no hay nadie.",
                    "La nena susurra: \"Vos también te vas a quedar acá\"."}),
            new Perfil("el profesor de gimnasia", "una botella de agua", new String[]{
                    "El profesor dice que hace años que corre y no logra salir del patio.",
                    "El profesor te muestra el cronómetro: marca la hora de mañana."}),
            new Perfil("un señor de impermeable", "un tostado de jamón y queso", new String[]{
                    "El señor gotea sobre el mostrador. Afuera no llueve.",
                    "El señor dice que ya vino ayer. Y anteayer. Y el año que viene."}),
            new Perfil("la directora", "un sándwich de milanesa", new String[]{
                    "La directora te recuerda que el buffet no cierra nunca.",
                    "La directora anota algo en su libreta. Es tu nombre, repetido cientos de veces."}),
            new Perfil("la portera", "un té con dos de azúcar", new String[]{
                    "La portera dice que el piso del baño nunca se seca.",
                    "La portera te pide que no mires el espejo después de las once."}),
            new Perfil("un chico con mochila", "una empanada de carne", new String[]{
                    "El chico dice que su mochila pesa cada vez más.",
                    "El chico abre la mochila. Adentro hay otra mochila."}),
            new Perfil("una mujer con velo", "un flan casero", new String[]{
                    "La mujer no tiene cara, pero igual sonríe.",
                    "La mujer te pregunta si este es el buffet de la escuela donde ibas."}),
            new Perfil("el kiosquero", "un vaso de leche", new String[]{
                    "El kiosquero dice que hace turno doble desde 1998.",
                    "El kiosquero te cobra con monedas que todavía no existen."}),
            new Perfil("una señora mayor", "un pan dulce", new String[]{
                    "La señora dice que el pan dulce era de su hijo.",
                    "La señora te llama por el nombre de su hijo. Coincide con el tuyo."}),
            new Perfil("un nene descalzo", "una porción de torta", new String[]{
                    "El nene mira la torta y no parpadea.",
                    "El nene pregunta si podés guardarle un pedazo para siempre."}),
            new Perfil("el auxiliar de turno", "una milanesa con puré", new String[]{
                    "El auxiliar dice que hoy renunció tres veces.",
                    "El auxiliar te pide que no apagues la luz del pasillo."}),
            new Perfil("una alumna repetidora", "un chocolate caliente", new String[]{
                    "La alumna dice que ya cursó este día antes.",
                    "La alumna te pregunta qué día es. No sabés qué contestar."}),
            new Perfil("el último cliente", "lo que sea", new String[]{
                    "El último cliente no tiene pedido. Solo espera.",
                    "El último cliente sonríe y dice: \"Esta vez tampoco te vas\"."}),
            new Perfil("un hombre de traje", "una gaseosa", new String[]{
                    "El hombre de traje dice que viene a auditar el buffet.",
                    "El hombre de traje te ofrece un contrato. No tiene letra chica: no tiene letra."}),
            new Perfil("una gemela", "otra gaseosa", new String[]{
                    "La gemela dice que su hermana ya vino. Las dos tienen el mismo nombre.",
                    "La gemela te guiña un ojo. La otra también."})
    };
}
