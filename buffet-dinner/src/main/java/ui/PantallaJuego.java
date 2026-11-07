package ui;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.PersistenciaException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Jugador;
import modelo.Lugar;
import modelo.Objeto;
import modelo.Tarea;
import service.Catalogo;
import service.JuegoService;
import service.PartidaService;
import ui.EscenaBuffet.Opcion;
import ui.EscenaBuffet.Zona;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de juego. El personaje camina por las salas del buffet (EscenaBuffet) y, al interactuar con un lugar,
 * se arma el menú de acciones: atender clientes, preparar productos, buscar herramientas y arreglar cosas.
 */
public class PantallaJuego extends JPanel {

    private final JuegoService juego;
    private final PartidaService partidas;
    private final Runnable alSalir;
    private final EscenaBuffet escena;
    private int corduraPrevia;
    private boolean jornadaAnunciada, derrotaMostrada;

    private final JLabel lblNombre = Tema.etiqueta("", Tema.SUBTITULO, Tema.TEXTO);
    private final JLabel lblDia = Tema.etiqueta("", Tema.CUERPO.deriveFont(Font.BOLD, 15f), Tema.ACENTO);
    private final JLabel lblTurno = Tema.etiqueta("", Tema.CUERPO, Tema.TEXTO_SUAVE);
    private final JLabel lblTareas = Tema.etiqueta("", Tema.CUERPO, Tema.TEXTO);
    private final JProgressBar barra = new JProgressBar(0, 100);
    private final JTextArea txtRegistro = Tema.texto(Tema.CUERPO, Tema.TEXTO_SUAVE);

    public PantallaJuego(JuegoService juego, PartidaService partidas, Runnable alSalir) {
        this.juego = juego;
        this.partidas = partidas;
        this.alSalir = alSalir;
        this.escena = new EscenaBuffet(juego, this::interactuar, this::eventoDelTiempo);
        this.corduraPrevia = juego.getJugador().getCordura();

        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(20, 28, 16, 28));
        setBackground(Tema.FONDO);
        add(armarBarraSuperior(), BorderLayout.NORTH);
        add(armarCentro(), BorderLayout.CENTER);
        add(Tema.etiqueta("Mover: WASD o flechas  ·  Interactuar: E  ·  Decidir: 1, 2, 3 o clic  ·  "
                + "Cruzá las puertas de los bordes  ·  Llevás un objeto por vez en las manos",
                Tema.CUERPO, Tema.TEXTO_SUAVE), BorderLayout.SOUTH);
        anunciarDia();
        actualizarHud();
    }

    // ---------- armado de la pantalla ----------

    private JPanel armarBarraSuperior() {
        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));
        izquierda.add(lblNombre);
        izquierda.add(lblDia);
        izquierda.add(lblTurno);

        barra.setStringPainted(true);
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 28, 0, 28);
        centro.add(barra, c);

        JButton btnGuardar = Tema.primario("Guardar partida");
        JButton btnSalir = Tema.secundario("Salir al menú");
        btnGuardar.setFocusable(false);
        btnSalir.setFocusable(false);
        btnGuardar.addActionListener(e -> guardar());
        btnSalir.addActionListener(e -> {
            if (confirmarSalida()) alSalir.run();
        });
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        derecha.add(lblTareas);
        derecha.add(btnGuardar);
        derecha.add(btnSalir);

        JPanel hud = Tema.tarjeta(new BorderLayout());
        hud.add(izquierda, BorderLayout.WEST);
        hud.add(centro, BorderLayout.CENTER);
        hud.add(derecha, BorderLayout.EAST);
        return hud;
    }

    private JPanel armarCentro() {
        JPanel marco = new JPanel(new BorderLayout());
        marco.setBorder(BorderFactory.createLineBorder(Tema.BORDE, 2));
        marco.add(escena, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(txtRegistro);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        JPanel registro = Tema.tarjeta(new BorderLayout(0, 10));
        registro.setPreferredSize(new Dimension(230, 0));
        registro.add(Tema.etiqueta("Diario del buffet", Tema.CUERPO.deriveFont(Font.BOLD, 15f), Tema.TEXTO), BorderLayout.NORTH);
        registro.add(scroll, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(14, 0));
        centro.setOpaque(false);
        centro.add(marco, BorderLayout.CENTER);
        centro.add(registro, BorderLayout.EAST);
        return centro;
    }

    // ---------- estado visible ----------

    private void actualizarHud() {
        Jugador j = juego.getJugador();
        lblNombre.setText(j.getNombre());
        lblDia.setText("Día " + juego.getDia());
        lblTurno.setText("Clientes " + juego.clientesAtendidos() + "/" + juego.totalClientes()
                + " · Tareas " + juego.tareasHechas() + "/" + juego.totalTareas());
        lblTareas.setText("Tareas totales: " + j.getTareasCompletadas());
        barra.setValue(Math.max(0, Math.min(100, j.getCordura())));
        barra.setString("Cordura " + j.getCordura());
        barra.setForeground(j.getCordura() < JuegoService.UMBRAL_CORDURA_DIA ? Tema.PELIGRO : Tema.BIEN);
        if (j.getCordura() != corduraPrevia) {
            escena.efectoCordura(j.getCordura() - corduraPrevia);
            corduraPrevia = j.getCordura();
        }
        if (juego.isDerrota() && !derrotaMostrada) {
            derrotaMostrada = true;
            mostrarDerrota();
        }
    }

    private void anunciarDia() {
        log("Día " + juego.getDia() + ": hoy vienen " + juego.totalClientes() + " clientes y hay "
                + juego.totalTareas() + " tareas. Llevales lo que piden y arreglá lo que esté roto.");
        jornadaAnunciada = false;
    }

    /** Cierra el diálogo y muestra (y registra) el resultado de la última decisión. */
    private void volver(String mensaje) {
        escena.cerrarDialogo();
        if (mensaje != null) {
            log(mensaje);
            escena.aviso(capitalizar(mensaje));
        }
        actualizarHud();
        escena.requestFocusInWindow();
        if (mensaje != null && !derrotaMostrada && !jornadaAnunciada && juego.turnoTerminado()) {
            jornadaAnunciada = true;
            mostrarFinDeJornada();
        }
    }

    private void error(Exception e) {
        Dialogos.error(this, e);
        escena.requestFocusInWindow();
    }

    private void log(String mensaje) {
        txtRegistro.append("• " + capitalizar(mensaje) + "\n\n");
        txtRegistro.setCaretPosition(txtRegistro.getDocument().getLength());
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Lo que pasa solo con el tiempo (un cliente se cansó de esperar). */
    private void eventoDelTiempo(String mensaje) {
        escena.reaccionCliente(false);
        log(mensaje);
        escena.aviso(mensaje);
        actualizarHud();
    }

    // ---------- interacción física -> menú de acciones ----------

    private void interactuar(Zona zona) {
        switch (zona) {
            case CLIENTE:
                atenderCliente();
                break;
            case CASILLEROS:
                abrirCasilleros();
                break;
            case MESAS:
            case BANO:
            case BASURA:
            case TABLERO:
                tarea(zona.lugar);
                break;
            case VITRINA:
                menu("VITRINA DE ALFAJORES", "Alfajores de dulce de leche, recién llegados.", null,
                        producto("Agarrar un alfajor", "ALFAJOR", null, 0.8, "Agarrando..."));
                break;
            case CAFETERA:
                menu("CAFETERA", "Para hacer café, la cafetera tiene que estar destapada.", Lugar.CAFETERA,
                        producto("Preparar un café", "CAFE", Lugar.CAFETERA, 2.6, "Preparando café..."));
                break;
            case PLANCHA:
                menu("PLANCHA", "Acá se hacen los tostados y las milanesas.", Lugar.PLANCHA,
                        producto("Hacer un tostado de jamón y queso", "TOSTADO", Lugar.PLANCHA, 3.2, "Tostando..."),
                        producto("Armar un sándwich de milanesa", "MILANESA", Lugar.PLANCHA, 4.0, "Friendo la milanesa..."));
                break;
            case HELADERA:
                menu("HELADERA", "Bebidas frías para los clientes.", Lugar.HELADERA,
                        producto("Sacar una botella de agua", "AGUA", Lugar.HELADERA, 1.0, "Buscando..."));
                break;
            case LIMPIEZA:
                menu("ESTANTE DE LIMPIEZA", "Todo lo que hace falta para limpiar. Agarrá una cosa por vez.", null,
                        producto("Agarrar el trapeador", "TRAPEADOR", null, 0.8, "Agarrando..."),
                        producto("Agarrar un trapo limpio", "TRAPO", null, 0.8, "Agarrando..."),
                        producto("Agarrar una rasqueta", "RASQUETA", null, 0.8, "Agarrando..."),
                        producto("Agarrar una bolsa de basura nueva", "BOLSA", null, 0.8, "Agarrando..."));
                break;
            case REPUESTOS:
                menu("ESTANTE DE REPUESTOS", "Repuestos y herramientas. Agarrá una cosa por vez.", null,
                        producto("Agarrar un destapador", "DESTAPADOR", null, 0.8, "Agarrando..."),
                        producto("Agarrar un fusible", "FUSIBLE", null, 0.8, "Agarrando..."),
                        producto("Agarrar una caja de bebidas", "CAJA", null, 1.0, "Cargando la caja..."));
                break;
        }
    }

    /** Menú de una estación o estante. Si el rincón tiene una tarea pendiente, se ofrece arreglarlo. */
    private void menu(String titulo, String cuerpo, Lugar lugar, Opcion... acciones) {
        List<Opcion> opciones = new ArrayList<>(List.of(acciones));
        String texto = cuerpo + manosTexto();
        if (lugar != null && juego.hayTareaPendiente(lugar)) {
            try {
                Tarea t = juego.realizarTarea(lugar);
                texto = capitalizar(t.getDescripcion()) + ". Está inutilizable hasta que lo arregles." + manosTexto();
                opciones.add(new Opcion("Arreglarlo (necesitás " + Catalogo.nombre(t.getHerramienta()) + ")", () -> tarea(lugar)));
            } catch (TareaNoDisponibleException e) {
                // no debería pasar: recién preguntamos si había una pendiente
            }
        }
        opciones.add(new Opcion("Cerrar", () -> volver(null)));
        escena.mostrarDialogo(titulo, texto, opciones.toArray(new Opcion[0]));
    }

    private String manosTexto() {
        Objeto o = juego.enMano();
        return o == null ? "\nTenés las manos vacías." : "\nLlevás en las manos: " + o.getNombre() + ".";
    }

    /** Opción que prepara un producto o agarra una herramienta: se valida, se espera la barra y recién ahí aparece en las manos. */
    private Opcion producto(String texto, String tipo, Lugar estacion, double segundos, String etiqueta) {
        return new Opcion(texto, () -> {
            try {
                juego.validarPreparacion(estacion);
                escena.cerrarDialogo();
                escena.trabajar(etiqueta, segundos, () -> terminarProducto(tipo));
            } catch (InteraccionInvalidaException | ObjetoNoDisponibleException e) {
                error(e);
                volver(null);
            }
        });
    }

    private void terminarProducto(String tipo) {
        try {
            volver(juego.agarrar(tipo));
        } catch (ObjetoNoDisponibleException e) {
            error(e);
            volver(null);
        }
    }

    // ---------- clientes ----------

    private void atenderCliente() {
        try {
            Cliente c = juego.esperarCliente();
            escena.mostrarDialogo(c.getNombre().toUpperCase() + " !!!!!!!!!!!!",
                    capitalizar(c.getNombre()) + " dice: \"Hola, quiero " + c.getPedido() + "\"" + manosTexto(),
                    new Opcion(capitalizar("entregar " + c.getPedido()), () -> entregar(c)),
                    new Opcion(capitalizar("no dar " + c.getPedido()), () -> rechazar(c)),
                    new Opcion("Preguntar por qué", () -> volver(juego.preguntar(c))));
        } catch (InteraccionInvalidaException e) {
            error(e);
        }
    }

    private void entregar(Cliente c) {
        try {
            String mensaje = juego.entregar(c);
            escena.reaccionCliente(true);
            volver(mensaje);
        } catch (PedidoInvalidoException e) {
            error(e);
            volver(null);
        }
    }

    private void rechazar(Cliente c) {
        String mensaje = juego.rechazar(c);
        escena.reaccionCliente(false);
        volver(mensaje);
    }

    // ---------- tareas ----------

    private void tarea(Lugar lugar) {
        try {
            Tarea t = juego.realizarTarea(lugar);
            String herramienta = Catalogo.nombre(t.getHerramienta());
            escena.mostrarDialogo(t.getTitulo(),
                    capitalizar(t.getDescripcion()) + ".\nNecesitás: " + herramienta + "." + manosTexto(),
                    new Opcion("Arreglarlo (" + herramienta + ")", () -> arreglar(t)),
                    new Opcion("Dejarlo para después", () -> volver(juego.posponer(t))),
                    new Opcion("Romperlo todo", () -> volver(juego.romper(t))));
        } catch (TareaNoDisponibleException e) {
            error(e);
        }
    }

    private void arreglar(Tarea t) {
        try {
            juego.validarArreglo(t);
            escena.cerrarDialogo();
            escena.trabajar("Arreglando...", 2.2, () -> terminarArreglo(t));
        } catch (TareaNoDisponibleException e) {
            error(e);
            volver(null);
        }
    }

    private void terminarArreglo(Tarea t) {
        try {
            volver(juego.arreglar(t));
        } catch (TareaNoDisponibleException e) {
            error(e);
            volver(null);
        }
    }

    // ---------- casilleros: dormir, usar, tirar ----------

    private void abrirCasilleros() {
        String estado;
        if (juego.turnoTerminado()) {
            estado = "Terminaste la jornada. Si dormís, mañana vienen más clientes.";
        } else {
            long cp = juego.totalClientes() - juego.clientesAtendidos(), tp = juego.totalTareas() - juego.tareasHechas();
            estado = "Todavía quedan " + cp + " clientes y " + tp + " tareas. Si dormís ahora, tu cordura se resiente.";
        }
        if (juego.getJugador().getCordura() < JuegoService.UMBRAL_CORDURA_DIA) {
            estado += "\nCuidado: con la cordura por debajo de " + JuegoService.UMBRAL_CORDURA_DIA + ", pasar de día es perder.";
        }
        escena.mostrarDialogo("CASILLEROS", estado + manosTexto(),
                new Opcion("Dormir y pasar al día siguiente", this::dormir),
                new Opcion("Usar lo que llevo en las manos", this::usarMano),
                new Opcion("Tirar lo que llevo en las manos", this::tirarMano),
                new Opcion("Cerrar", () -> volver(null)));
    }

    private void usarMano() {
        try {
            volver(juego.usarMano());
        } catch (ObjetoNoDisponibleException | InteraccionInvalidaException e) {
            error(e);
            volver(null);
        }
    }

    private void tirarMano() {
        try {
            volver(juego.tirarMano());
        } catch (InteraccionInvalidaException e) {
            error(e);
            volver(null);
        }
    }

    private void dormir() {
        String mensaje = juego.cerrarJornada();
        escena.cerrarDialogo();
        if (juego.isDerrota()) {
            log(mensaje);
            actualizarHud();
            return;
        }
        log(mensaje);
        escena.nuevoDia("DÍA " + juego.getDia());
        anunciarDia();
        actualizarHud();
        guardarEnSilencio();
        escena.requestFocusInWindow();
    }

    private void mostrarFinDeJornada() {
        escena.mostrarDialogo("JORNADA TERMINADA!!!!!!!!!!!!",
                "Atendiste a todos los clientes y dejaste todo en orden. Tu cordura: "
                        + juego.getJugador().getCordura() + ". Si dormís (casilleros), mañana vendrán más clientes.",
                new Opcion("Dormir y pasar al día siguiente", this::dormir),
                new Opcion("Seguir en el buffet", () -> volver(null)));
    }

    // ---------- derrota ----------

    private void mostrarDerrota() {
        escena.cerrarDialogo();
        escena.mostrarDialogo("TE PERDISTE EN EL BUFFET",
                juego.getMotivoDerrota() + "\nLlegaste hasta el día " + juego.getDia() + ".",
                new Opcion("Empezar de nuevo (día 1)", () -> {
                    juego.reiniciarPartida();
                    derrotaMostrada = false;
                    corduraPrevia = juego.getJugador().getCordura();
                    escena.cerrarDialogo();
                    escena.nuevoDia("DÍA 1");
                    anunciarDia();
                    guardarEnSilencio();
                    actualizarHud();
                    escena.requestFocusInWindow();
                }),
                new Opcion("Salir al menú", () -> {
                    juego.reiniciarPartida();
                    guardarEnSilencio();
                    alSalir.run();
                }));
    }

    // ---------- guardar / salir ----------

    private void guardarEnSilencio() {
        try {
            partidas.guardarProgreso(juego.getJugador());
        } catch (PersistenciaException e) {
            log("No se pudo guardar automáticamente.");
        }
    }

    private boolean guardar() {
        try {
            partidas.guardarProgreso(juego.getJugador());
            log("Partida guardada.");
            escena.aviso("Partida guardada.");
            return true;
        } catch (PersistenciaException e) {
            error(e);
            return false;
        }
    }

    /** Pregunta si guardar antes de salir. Devuelve true si se puede abandonar la partida. */
    public boolean confirmarSalida() {
        int r = JOptionPane.showConfirmDialog(this, "¿Querés guardar la partida antes de salir?",
                "Salir al menú", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        return r != JOptionPane.YES_OPTION || guardar();
    }
}
