package ui;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.PersistenciaException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Herramienta;
import modelo.Jugador;
import modelo.Lugar;
import modelo.Objeto;
import modelo.Sala;
import modelo.Tarea;
import service.JuegoService;
import service.PartidaService;
import ui.EscenaBuffet.Zona;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Pantalla completa del juego. Todo lo que el jugador ve —el buffet, el estado, la bitácora,
 * los diálogos y la intro— se dibuja dentro del mismo lienzo de 384x240 y después se escala
 * con un factor entero para que ningún píxel quede deformado.
 */
public class PantallaJuego extends JPanel implements ActionListener {

    private static final int ANCHO = EscenaBuffet.ANCHO;
    private static final int ALTO = EscenaBuffet.ALTO;

    private enum Estado { INTRO, JUEGO, CUADERNO, PAUSA, CERRAR_DIA, CONFIRMAR_SALIDA, DERROTA }

    private enum Tipo { SISTEMA, EXITO, ERROR, CHARLA }

    private record Entrada(String texto, Tipo tipo) { }

    /** Texto de la intro: la primera entrada es el título; el resto son las noches. */
    private static final String[][] GUION = {
            {"BUFFET TERROR", "turno noche"},
            {"NOCHE 1", "El cartel de la calle se apaga y el turno empieza igual."},
            {"NOCHE 2", "Los clientes llegan con hambre. Algunos no tienen ojos."},
            {"NOCHE 3", "Tomá el pedido. Prepará la bandeja. No dejes que te mire."},
            {"TODO LISTO", "El mostrador está en el comedor. El almacén, pasando la cocina."}
    };

    private static final String[] OPCIONES_PAUSA = {"Continuar", "Guardar progreso", "Salir al menú"};
    private static final String[] OPCIONES_SALIR = {"Guardar y salir", "Salir sin guardar", "Cancelar"};
    private static final String[] OPCIONES_FIN = {"Seguir en el buffet", "Pasar al día siguiente", "Guardar y salir"};

    private final JuegoService juego;
    private final PartidaService partidas;
    private final Runnable alSalir;
    private final EscenaBuffet escena;
    private final BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
    private final Timer timer = new Timer(16, this);
    private final Set<Integer> teclas = new HashSet<>();
    private final Deque<Entrada> bitacora = new ArrayDeque<>();

    private Estado estado = Estado.INTRO;
    private int seleccion;
    private int introPaso;
    private long introMillis;
    private boolean introCompleto;
    private String toast = "";
    private int toastTicks;
    private String causaDerrota;
    private boolean confirmarAvance;
    private boolean confirmarSalida;
    private long ultimaInteraccion;
    private boolean avisoCierre;
    private long avisoCierreDesde;

    public PantallaJuego(JuegoService juego, PartidaService partidas, Runnable alSalir) {
        this.juego = juego;
        this.partidas = partidas;
        this.alSalir = alSalir;
        this.escena = new EscenaBuffet(juego);
        this.escena.setAlCambiarSala(this::cambiarSala);
        this.introMillis = System.currentTimeMillis();

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setBackground(new Color(0x060908));
        setPreferredSize(new Dimension(ANCHO * 3, ALTO * 3));
        setLayout(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                teclaPresionada(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                teclas.remove(e.getKeyCode());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                teclas.clear();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    // ------------------------------------------------------------------ ciclo

    @Override
    public void actionPerformed(ActionEvent e) {
        long ahora = System.currentTimeMillis();

        if (estado == Estado.JUEGO) {
            escena.actualizar(teclas, false);
            if (juego.isPerdida()) {
                derrota(juego.getCausaPerdida());
            } else if (juego.turnoTerminado() && !avisoCierre) {
                avisoCierre = true;
                avisoCierreDesde = ahora;
                mostrarToast("Jornada completa. ENTER para cerrar la caja.", Tipo.SISTEMA);
            } else if (avisoCierre && ahora - avisoCierreDesde > 2400) {
                abrirCierreDeJornada();
            }
        } else {
            escena.actualizar(Set.of(), true);
        }

        if (estado == Estado.INTRO && !introCompleto && introPaso > 0 && introPaso < GUION.length - 1) {
            String cuerpo = GUION[introPaso][1];
            if (ahora - introMillis > cuerpo.length() * 24L + 400) {
                introCompleto = true;
            }
        }

        if (toastTicks > 0) {
            toastTicks--;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        dibujarLienzo();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setColor(new Color(0x060908));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int escala = Math.max(1, Math.min(getWidth() / ANCHO, getHeight() / ALTO));
        int ancho = ANCHO * escala;
        int alto = ALTO * escala;
        int x = (getWidth() - ancho) / 2;
        int y = (getHeight() - alto) / 2;
        g2.drawImage(lienzo, x, y, ancho, alto, null);
        g2.setColor(new Color(0x24312B));
        g2.drawRect(x - 1, y - 1, ancho + 1, alto + 1);
        g2.dispose();
    }

    private void dibujarLienzo() {
        Graphics2D g = lienzo.createGraphics();
        Pixel.preparar(g);
        g.setColor(Pixel.NEGRO);
        g.fillRect(0, 0, ANCHO, ALTO);

        escena.pintar(g);

        switch (estado) {
            case INTRO -> dibujarIntro(g);
            case JUEGO -> dibujarHud(g);
            case CUADERNO -> {
                dibujarHud(g);
                dibujarCuaderno(g);
            }
            case PAUSA -> {
                dibujarHud(g);
                dibujarPausa(g);
            }
            case CONFIRMAR_SALIDA -> {
                dibujarHud(g);
                dibujarConfirmarSalida(g);
            }
            case CERRAR_DIA -> {
                dibujarHud(g);
                dibujarFinDeDia(g);
            }
            case DERROTA -> {
                dibujarHud(g);
                dibujarDerrota(g);
            }
            default -> dibujarHud(g);
        }
        g.dispose();
    }

    // ------------------------------------------------------------------ intro

    private void dibujarIntro(Graphics2D g) {
        g.setColor(new Color(4, 7, 8, 184));
        g.fillRect(0, 0, ANCHO, ALTO);

        // lluvia sobre el vidrio
        for (int i = 0; i < 54; i++) {
            int x = (i * 71 + tickLluvia(i)) % ANCHO;
            int y = (i * 47 + tickLluvia(i) * 2) % ALTO;
            g.setColor(new Color(120, 160, 166, 28 + (i % 3) * 10));
            g.fillRect(x, y, 1, 3 + (i % 2));
        }

        long t = System.currentTimeMillis() - introMillis;
        if (introPaso == 0) {
            dibujarTitulo(g);
        } else if (introPaso < GUION.length - 1) {
            dibujarNoche(g, t);
        } else {
            dibujarEntrada(g);
        }

        // marco del cine y pie
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, ANCHO, 12);
        g.fillRect(0, ALTO - 12, ANCHO, 12);
        Pixel.textoCentrado(g, "ENTER continuar  ·  ESC salir", ANCHO / 2, ALTO - 4, Pixel.T_CHICO,
                Pixel.GRIS_OSCURO);
    }

    private static int tickLluvia(int i) {
        return (int) ((System.currentTimeMillis() / 28 + i * 7) % 400);
    }

    private void dibujarTitulo(Graphics2D g) {
        int parpadeo = (int) (System.currentTimeMillis() / 90) % 23;
        Color crema = parpadeo == 0 ? new Color(0x8E8878) : Pixel.CREMA;
        Color rojo = parpadeo == 0 ? new Color(0x6E231F) : Pixel.ROJO_CLARO;

        // bandeja con un ojo servido
        int x = ANCHO / 2;
        dibujarBandeja(g, x - 22, 48);
        Pixel.elipse(g, x - 10, 54, 20, 13, new Color(0xF1EAD8));
        Pixel.elipse(g, x - 5, 57, 10, 8, new Color(0x7FB58A));
        Pixel.elipse(g, x - 2, 59, 4, 4, new Color(0x120F0C));
        g.setColor(new Color(0xC4513F));
        g.fillRect(x - 12, 58, 2, 1);
        g.fillRect(x + 10, 58, 2, 1);

        Pixel.textoCentrado(g, "BUFFET", ANCHO / 2 + 2, 104, 36, new Color(0x000000), true);
        Pixel.textoCentrado(g, "BUFFET", ANCHO / 2, 102, 36, crema, true);
        Pixel.textoCentrado(g, "TERROR", ANCHO / 2 + 2, 140, 36, new Color(0x000000), true);
        Pixel.textoCentrado(g, "TERROR", ANCHO / 2, 138, 36, rojo, true);

        Pixel.textoCentrado(g, "T U R N O   N O C H E", ANCHO / 2, 160, Pixel.T_CUERPO, Pixel.MOSTAZA);

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            Pixel.textoCentrado(g, "PRESIONÁ ENTER", ANCHO / 2, 198, Pixel.T_CUERPO, Pixel.MOSTAZA_CLARA, true);
        }
        Pixel.textoCentrado(g, "un turno. una bandeja. nadie se va sin comer.",
                ANCHO / 2, 218, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    private void dibujarNoche(Graphics2D g, long t) {
        String[] noche = GUION[introPaso];
        Pixel.textoCentrado(g, noche[0], ANCHO / 2, 88, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        g.setColor(new Color(0x51695C));
        g.fillRect(ANCHO / 2 - 58, 96, 116, 1);

        List<String> lineas = Pixel.envolver(g, noche[1], 300, Pixel.T_CUERPO);
        int visibles = introCompleto ? Integer.MAX_VALUE : (int) ((t - 250) / 24);
        visibles = Math.max(0, visibles);
        int y = 118;
        for (String linea : lineas) {
            if (visibles <= 0) {
                break;
            }
            String trozo = linea.substring(0, Math.min(linea.length(), visibles));
            visibles -= linea.length();
            Pixel.textoCentrado(g, trozo, ANCHO / 2, y, Pixel.T_CUERPO, Pixel.CREMA);
            y += Pixel.altoLinea(Pixel.T_CUERPO) + 2;
        }

        if (introCompleto && (System.currentTimeMillis() / 420) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER", ANCHO / 2, 186, Pixel.T_CUERPO, Pixel.MOSTAZA_CLARA, true);
        }
    }

    private void dibujarEntrada(Graphics2D g) {
        Pixel.textoCentrado(g, "TODO LISTO", ANCHO / 2, 74, Pixel.T_TITULO, Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "El buffet abre en un rato.", ANCHO / 2, 88, Pixel.T_CHICO, Pixel.GRIS);

        Pixel.panelTenue(g, 48, 104, 288, 84, 190, Pixel.BORDE);
        Pixel.texto(g, "WASD / flechas   moverte", 60, 122, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "E / ENTER        interactuar", 60, 136, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "I                usar la mochila", 60, 150, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "Q                soltar lo que tenés", 60, 164, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "TAB              cuaderno de turno", 60, 178, Pixel.T_CUERPO, Pixel.CREMA);

        Pixel.textoCentrado(g, "Cruzá las puertas de los bordes para cambiar de sala.",
                ANCHO / 2, 204, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        if ((System.currentTimeMillis() / 420) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER para entrar", ANCHO / 2, 226, Pixel.T_CUERPO,
                    Pixel.MOSTAZA_CLARA, true);
        }
    }

    private void avanzarIntro() {
        long ahora = System.currentTimeMillis();
        if (introPaso == 0) {
            introPaso = 1;
            introMillis = ahora;
            introCompleto = false;
            return;
        }
        if (introPaso == GUION.length - 1) {
            empezarTurno();
            return;
        }
        if (!introCompleto) {
            introCompleto = true;
            return;
        }
        introPaso++;
        introMillis = ahora;
        introCompleto = introPaso == GUION.length - 1;
    }

    private void empezarTurno() {
        estado = Estado.JUEGO;
        seleccion = 0;
        confirmarAvance = false;
        avisoCierre = false;
        escena.setSala(juego.getSala());
        escena.reiniciarPosicion();
        bitacora.clear();
        Jugador j = juego.getJugador();
        registrar("Día " + j.getDiaActual() + ". " + j.getNombre() + ", el buffet abre.", Tipo.SISTEMA);
        registrar("Tomá el pedido del mostrador y preparalo en la cocina.", Tipo.CHARLA);
        mostrarToast("DÍA " + j.getDiaActual() + "  ·  " + juego.totalClientes()
                + " clientes  ·  " + juego.totalTareas() + " tareas", Tipo.SISTEMA);
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------ HUD

    private void dibujarHud(Graphics2D g) {
        Jugador j = juego.getJugador();
        int cordura = Math.max(0, Math.min(100, j.getCordura()));
        Color colorCordura = cordura < JuegoService.CORDURA_MINIMA_DIA ? Pixel.ROJO
                : cordura < 55 ? Pixel.MOSTAZA : Pixel.VERDE;

        // placa del día y la sala
        Pixel.panelTenue(g, 4, 4, 120, 22, 180, Pixel.BORDE);
        Pixel.texto(g, "DÍA " + j.getDiaActual(), 9, 14, Pixel.T_CUERPO, Pixel.MOSTAZA, true);
        Pixel.texto(g, juego.getSala().getNombre().toUpperCase(), 9, 23, Pixel.T_CHICO, Pixel.GRIS);

        // pedidos y tareas
        Pixel.panelTenue(g, 128, 4, 124, 22, 180, Pixel.BORDE);
        Pixel.texto(g, "PEDIDOS", 133, 13, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoDerecha(g, juego.clientesAtendidos() + "/" + juego.totalClientes(),
                247, 13, Pixel.T_CUERPO, Pixel.CREMA, true);
        Pixel.texto(g, "TAREAS", 133, 23, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoDerecha(g, juego.tareasHechas() + "/" + juego.totalTareas(),
                247, 23, Pixel.T_CUERPO, Pixel.CREMA, true);

        // cordura
        Pixel.panelTenue(g, 256, 4, 124, 22, 180, Pixel.BORDE);
        Pixel.texto(g, "CORDURA", 260, 12, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoDerecha(g, String.valueOf(cordura), 376, 13, Pixel.T_CUERPO, colorCordura, true);
        Pixel.barra(g, 260, 16, 112, 6, cordura, 100, colorCordura, new Color(0x1B2621), Pixel.BORDE);

        // objeto en la mano
        Objeto enMano = juego.getEnMano();
        if (enMano != null) {
            Pixel.panelTenue(g, 256, 30, 124, 30, 180, Pixel.MOSTAZA);
            Pixel.texto(g, "EN MANO", 260, 38, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
            dibujarIcono(g, enMano, 260, 42);
            Pixel.texto(g, Pixel.recortar(g, enMano.getNombre(), 92, Pixel.T_CHICO, false),
                    275, 44, Pixel.T_CHICO, Pixel.CREMA);
            if (enMano instanceof Herramienta h) {
                Pixel.texto(g, "PARA " + h.getLugar().getEtiqueta().toUpperCase(),
                        275, 54, Pixel.T_CHICO, Pixel.MOSTAZA);
            } else {
                Pixel.texto(g, "PEDIDO", 275, 54, Pixel.T_CHICO, Pixel.MOSTAZA);
            }
        }

        // aviso de interacción, pegado a la cabeza del jugador
        Zona zona = escena.zonaCercana();
        if (zona != null) {
            String texto = "[E] " + zona.etiqueta();
            boolean listo = true;
            if (zona.id().startsWith("tarea:")) {
                Lugar lugar = Lugar.valueOf(zona.id().substring(6));
                Tarea tarea = juego.tareaPendienteEn(lugar);
                if (tarea != null && tarea.requiereHerramienta() && !juego.tieneHerramientaPara(lugar)) {
                    listo = false;
                    texto = "[E] " + zona.etiqueta() + "  ·  falta " + tarea.getHerramienta().getNombre();
                }
            }
            int ancho = Math.min(356, Pixel.ancho(g, texto, Pixel.T_CUERPO, true) + 20);
            int x = Math.max(4, Math.min(ANCHO - ancho - 4, (int) Math.round(escena.getX()) - ancho / 2));
            int y = Math.max(58, Math.min(172, (int) Math.round(escena.getY()) - 33));
            Pixel.panelTenue(g, x, y, ancho, 15, 215, listo ? Pixel.MOSTAZA : Pixel.ROJO);
            Pixel.textoCentrado(g, texto, x + ancho / 2, y + 11, Pixel.T_CUERPO,
                    listo ? Pixel.CREMA : Pixel.ROJO_CLARO, true);
        }

        // objetivo del turno, arriba a la izquierda para no tapar al jugador
        Pixel.panelTenue(g, 4, 30, 248, 23, 165, Pixel.BORDE);
        Pixel.texto(g, "OBJETIVO", 9, 40, Pixel.T_CHICO, Pixel.MOSTAZA, true);
        List<String> pista = Pixel.envolver(g, juego.getPista(), 188, Pixel.T_CHICO);
        for (int i = 0; i < Math.min(2, pista.size()); i++) {
            String linea = i == 1 ? Pixel.recortar(g, pista.get(i), 188, Pixel.T_CHICO, false) : pista.get(i);
            Pixel.texto(g, linea, 57, 40 + i * 9, Pixel.T_CHICO, Pixel.CREMA);
        }

        // bitácora (las dos últimas entradas) con una franja tenue para que se lea
        Pixel.panelTenue(g, 0, 211, ANCHO, 29, 110, null);
        int n = Math.min(2, bitacora.size());
        int i = 0;
        for (java.util.Iterator<Entrada> it = bitacora.descendingIterator(); it.hasNext() && i < n; i++) {
            Entrada entrada = it.next();
            int y = 218 + (n - 1 - i) * 9;
            Color color = switch (entrada.tipo()) {
                case SISTEMA -> Pixel.MOSTAZA_CLARA;
                case EXITO -> Pixel.VERDE;
                case ERROR -> Pixel.ROJO_CLARO;
                case CHARLA -> Pixel.GRIS;
            };
            String linea = Pixel.recortar(g, "› " + entrada.texto(), 300, Pixel.T_CHICO, false);
            Pixel.texto(g, linea, 8, y, Pixel.T_CHICO, color);
        }

        Pixel.texto(g, "WASD mover · E usar · I/Q mochila", 8, 236, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoDerecha(g, "TAB cuaderno · ESC pausa", 376, 236, Pixel.T_CHICO, Pixel.GRIS_OSCURO);

        // aviso de jornada terminada
        if (estado == Estado.JUEGO && juego.turnoTerminado() && (System.currentTimeMillis() / 450) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER para cerrar la caja", ANCHO / 2, 170, Pixel.T_CUERPO,
                    Pixel.MOSTAZA_CLARA, true);
        }

        dibujarToast(g);
    }

    private void dibujarToast(Graphics2D g) {
        if (toastTicks <= 0 || toast == null || toast.isBlank()) {
            return;
        }
        int ancho = Math.min(330, Pixel.ancho(g, toast, Pixel.T_CUERPO, true) + 24);
        int x = (ANCHO - ancho) / 2;
        Pixel.panelTenue(g, x, 62, ancho, 17, 220, Pixel.MOSTAZA);
        Pixel.textoCentrado(g, toast, ANCHO / 2, 74, Pixel.T_CUERPO, Pixel.CREMA, true);
    }

    // ------------------------------------------------------------------ cuaderno

    private List<Objeto> inventario() {
        return juego.abrirInventario();
    }

    private void dibujarCuaderno(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 18, 12, 348, 216, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.panelTenue(g, 21, 15, 342, 30, 90, null);
        Pixel.textoCentrado(g, "CUADERNO DE TURNO", ANCHO / 2, 33, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "DÍA " + juego.getJugador().getDiaActual() + "  ·  "
                + juego.getSala().getNombre().toUpperCase(), ANCHO / 2, 43, Pixel.T_CHICO, Pixel.GRIS);
        g.setColor(Pixel.BORDE);
        g.fillRect(24, 49, 336, 1);

        // columna izquierda: clientes y tareas
        Pixel.texto(g, "PEDIDOS", 30, 64, Pixel.T_CUERPO, Pixel.MOSTAZA, true);
        Cliente c = juego.clienteActual();
        int y = 78;
        if (c == null) {
            Pixel.texto(g, "Mostrador vacío.", 30, y, Pixel.T_CHICO, Pixel.GRIS);
        } else {
            Pixel.texto(g, c.getNombre(), 30, y, Pixel.T_CUERPO, Pixel.CREMA, true);
            y += 10;
            for (String linea : Pixel.envolver(g, c.getPedido(), 150, Pixel.T_CHICO)) {
                Pixel.texto(g, linea, 30, y, Pixel.T_CHICO, Pixel.GRIS);
                y += 8;
            }
            if (!c.isPedidoTomado()) {
                Pixel.texto(g, "· pedido sin anotar", 30, y, Pixel.T_CHICO, Pixel.ROJO_CLARO);
            } else if (juego.tieneBandejaPara(c)) {
                Pixel.texto(g, "· bandeja lista", 30, y, Pixel.T_CHICO, Pixel.VERDE);
            } else {
                Pixel.texto(g, "· falta prepararlo", 30, y, Pixel.T_CHICO, Pixel.MOSTAZA);
            }
            y += 10;
        }

        Pixel.texto(g, "TAREAS", 30, Math.max(y + 4, 118), Pixel.T_CUERPO, Pixel.MOSTAZA, true);
        y = Math.max(y + 16, 130);
        List<Tarea> pendientes = new ArrayList<>();
        for (Tarea t : juego.getTareas()) {
            if (!t.isCompletada()) {
                pendientes.add(t);
            }
        }
        if (pendientes.isEmpty()) {
            Pixel.texto(g, "Todo limpio. Por ahora.", 30, y, Pixel.T_CHICO, Pixel.GRIS);
        } else {
            int maximo = 4;
            for (int i = 0; i < Math.min(maximo, pendientes.size()); i++) {
                Tarea t = pendientes.get(i);
                String titulo = Pixel.recortar(g, t.getTitulo(), 150, Pixel.T_CHICO, false);
                Pixel.texto(g, "· " + titulo, 30, y, Pixel.T_CHICO, Pixel.CREMA);
                y += 8;
                String detalle = t.getLugar().getSala().getNombre() + " · " + t.getLugar().getEtiqueta();
                if (t.requiereHerramienta()) {
                    detalle += " · " + t.getHerramienta().getNombre();
                }
                Pixel.texto(g, "  " + Pixel.recortar(g, detalle, 148, 6, false), 30, y, 6, Pixel.GRIS_OSCURO);
                y += 12;
            }
            if (pendientes.size() > maximo) {
                Pixel.texto(g, "+" + (pendientes.size() - maximo) + " tareas más", 30, y, 6, Pixel.GRIS_OSCURO);
            }
        }

        // columna derecha: mochila
        int x = 200;
        Pixel.texto(g, "MOCHILA", x, 64, Pixel.T_CUERPO, Pixel.MOSTAZA, true);
        Objeto enMano = juego.getEnMano();
        Pixel.texto(g, "En mano: " + (enMano == null ? "nada" : Pixel.recortar(g, enMano.getNombre(), 120, 6, false)),
                x, 76, 6, Pixel.GRIS);
        g.setColor(Pixel.BORDE);
        g.fillRect(x, 81, 156, 1);

        List<Objeto> objetos = inventario();
        if (objetos.isEmpty()) {
            Pixel.texto(g, "Vacía. Limpiá tareas para", x, 94, 6, Pixel.GRIS);
            Pixel.texto(g, "conseguir cosas.", x, 102, 6, Pixel.GRIS);
        } else {
            int filas = Math.min(6, objetos.size());
            if (seleccion >= objetos.size()) {
                seleccion = Math.max(0, objetos.size() - 1);
            }
            for (int i = 0; i < filas; i++) {
                Objeto o = objetos.get(i);
                int fy = 88 + i * 16;
                boolean elegido = i == seleccion;
                if (elegido) {
                    Pixel.panelTenue(g, x - 3, fy - 1, 158, 15, 170, Pixel.MOSTAZA);
                }
                dibujarIcono(g, o, x, fy + 1);
                String nombre = Pixel.recortar(g, o.getNombre(), 100, 6, elegido);
                Pixel.texto(g, nombre, x + 15, fy + 8, 6, elegido ? Pixel.CREMA : Pixel.GRIS, elegido);
                String etiqueta = o instanceof Herramienta ? "EQUIPAR" : (o.isUtilizable() ? "USAR" : "—");
                Pixel.textoDerecha(g, etiqueta, x + 150, fy + 8, 6, Pixel.MOSTAZA);
            }
            if (objetos.size() > filas) {
                Pixel.texto(g, "+" + (objetos.size() - filas) + " más", x, 88 + filas * 16 + 2, 6, Pixel.GRIS_OSCURO);
            }
        }

        Pixel.textoCentrado(g, "↑↓ elegir  ·  ENTER usar/equipar  ·  TAB cerrar",
                ANCHO / 2, 218, 6, Pixel.GRIS);
    }

    private void dibujarPausa(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 108, 66, 168, 108, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.textoCentrado(g, "PAUSA", ANCHO / 2, 88, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        for (int i = 0; i < OPCIONES_PAUSA.length; i++) {
            Pixel.boton(g, 120, 102 + i * 20, 144, 16, OPCIONES_PAUSA[i], i == seleccion,
                    i == 2, Pixel.MOSTAZA);
        }
        Pixel.textoCentrado(g, "ESC para volver", ANCHO / 2, 168, 6, Pixel.GRIS_OSCURO);
    }

    private void dibujarConfirmarSalida(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 48, 62, 288, 120, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.textoCentrado(g, "SALIR DE LA PARTIDA", ANCHO / 2, 84, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "¿Querés guardar el progreso antes de volver?",
                ANCHO / 2, 100, 6, Pixel.GRIS);
        for (int i = 0; i < OPCIONES_SALIR.length; i++) {
            Pixel.boton(g, 62, 110 + i * 20, 260, 16, OPCIONES_SALIR[i], i == seleccion,
                    i == 1, Pixel.MOSTAZA);
        }
        Pixel.textoCentrado(g, "ENTER confirmar  ·  ESC cancelar", ANCHO / 2, 174, 6, Pixel.GRIS_OSCURO);
    }

    // ------------------------------------------------------------------ fin de día

    private void dibujarFinDeDia(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 24, 14, 336, 212, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.textoCentrado(g, "SE TERMINÓ LA JORNADA", ANCHO / 2, 36, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "DÍA " + juego.getJugador().getDiaActual() + "  ·  "
                + juego.getJugador().getNombre(), ANCHO / 2, 47, 6, Pixel.GRIS);

        int[] valores = {juego.clientesAtendidos(), juego.tareasHechas(), juego.getJugador().getDiaActual()};
        String[] titulos = {"CLIENTES", "TAREAS", "DÍA"};
        String[] maximos = {"/" + juego.totalClientes(), "/" + juego.totalTareas(), ""};
        for (int i = 0; i < 3; i++) {
            int x = 40 + i * 104;
            Pixel.panelTenue(g, x, 56, 96, 30, 120, Pixel.BORDE);
            Pixel.textoCentrado(g, valores[i] + maximos[i], x + 48, 74, Pixel.T_SUBTITULO,
                    i == 2 ? Pixel.MOSTAZA : Pixel.CREMA, true);
            Pixel.textoCentrado(g, titulos[i], x + 48, 83, 6, Pixel.GRIS_OSCURO);
        }

        int cordura = juego.getJugador().getCordura();
        Color color = cordura < JuegoService.CORDURA_MINIMA_DIA ? Pixel.ROJO
                : cordura < 55 ? Pixel.MOSTAZA : Pixel.VERDE;
        Pixel.texto(g, "CORDURA AL CERRAR", 40, 101, 6, Pixel.GRIS_OSCURO);
        Pixel.texto(g, String.valueOf(cordura), 40, 118, Pixel.T_TITULO, color, true);
        Pixel.barra(g, 76, 105, 236, 8, cordura, 100, color, new Color(0x1B2621), Pixel.BORDE);
        if (cordura < JuegoService.CORDURA_MINIMA_DIA) {
            Pixel.texto(g, "Estás por debajo del mínimo (" + JuegoService.CORDURA_MINIMA_DIA
                    + "). Si pasás así, perdés.", 76, 119, 6, Pixel.ROJO_CLARO);
        } else {
            Pixel.texto(g, "Al dormir recuperás " + JuegoService.RECUPERACION_DIARIA
                    + " de cordura.", 76, 119, 6, Pixel.GRIS);
        }

        List<Objeto> consumibles = new ArrayList<>();
        for (Objeto o : inventario()) {
            if (o.isUtilizable()) {
                consumibles.add(o);
            }
        }

        Pixel.texto(g, "CONSUMIBLES", 40, 136, 6, Pixel.MOSTAZA, true);
        if (consumibles.isEmpty()) {
            Pixel.texto(g, "No te queda nada. Vas a cerrar así.", 40, 150, 6, Pixel.GRIS_OSCURO);
        } else {
            for (int i = 0; i < Math.min(4, consumibles.size()); i++) {
                Objeto o = consumibles.get(i);
                boolean elegido = i == seleccion;
                int fy = 141 + i * 15;
                if (elegido) {
                    Pixel.panelTenue(g, 36, fy - 1, 152, 14, 175, Pixel.MOSTAZA);
                }
                dibujarIcono(g, o, 40, fy + 1);
                String nombre = Pixel.recortar(g, o.getNombre(), 92, 6, elegido);
                Pixel.texto(g, nombre, 55, fy + 8, 6, elegido ? Pixel.CREMA : Pixel.GRIS, elegido);
                if (o.getEfectoCordura() != 0) {
                    Pixel.textoDerecha(g, (o.getEfectoCordura() > 0 ? "+" : "") + o.getEfectoCordura(),
                            184, fy + 8, 6, Pixel.VERDE);
                }
            }
        }

        // decisiones
        Pixel.texto(g, "DECISIÓN", 208, 136, 6, Pixel.MOSTAZA, true);
        int base = consumibles.size();
        for (int i = 0; i < OPCIONES_FIN.length; i++) {
            String texto = OPCIONES_FIN[i];
            boolean peligro = false;
            if (i == 1) {
                if (confirmarAvance) {
                    texto = "CONFIRMAR Y ARRIESGAR";
                    peligro = true;
                } else if (cordura < JuegoService.CORDURA_MINIMA_DIA) {
                    texto = "Pasar (peligro)";
                    peligro = true;
                }
            }
            Pixel.boton(g, 204, 142 + i * 20, 146, 16, texto, seleccion == base + i, peligro, Pixel.MOSTAZA);
        }
        Pixel.texto(g, "↑↓ elegir  ·  ENTER confirmar", 204, 208, 6, Pixel.GRIS_OSCURO);
        Pixel.texto(g, "ESC volver", 204, 217, 6, Pixel.GRIS_OSCURO);
    }

    private void dibujarDerrota(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 32, 40, 320, 172, new Color(0x120C0B), new Color(0x6E231F));
        Pixel.textoCentrado(g, "PERDISTE", ANCHO / 2, 78, Pixel.T_GRANDE, Pixel.ROJO_CLARO, true);
        String causa = causaDerrota == null
                ? "La noche se cerró sobre el buffet y ya no hubo salida."
                : causaDerrota;
        List<String> lineas = Pixel.envolver(g, causa, 280, Pixel.T_CUERPO);
        int y = 100;
        for (int i = 0; i < Math.min(4, lineas.size()); i++) {
            Pixel.textoCentrado(g, lineas.get(i), ANCHO / 2, y, Pixel.T_CUERPO, Pixel.CREMA);
            y += 11;
        }
        Pixel.textoCentrado(g, "Clientes atendidos: " + juego.clientesAtendidos()
                + "  ·  Tareas: " + juego.tareasHechas(), ANCHO / 2, 158, 6, Pixel.GRIS);
        Pixel.textoCentrado(g, "Días sobrevividos: " + Math.max(0, juego.getJugador().getDiaActual() - 1),
                ANCHO / 2, 168, 6, Pixel.GRIS);
        if ((System.currentTimeMillis() / 450) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER para volver al menú", ANCHO / 2, 196, Pixel.T_CUERPO,
                    Pixel.MOSTAZA_CLARA, true);
        }
    }

    private void oscurecer(Graphics2D g) {
        g.setColor(new Color(3, 5, 5, 185));
        g.fillRect(0, 0, ANCHO, ALTO);
    }

    // ------------------------------------------------------------------ teclado

    private void teclaPresionada(KeyEvent e) {
        int codigo = e.getKeyCode();
        if (esMovimiento(codigo)) {
            teclas.add(codigo);
        }

        switch (estado) {
            case INTRO -> {
                if (codigo == KeyEvent.VK_ENTER || codigo == KeyEvent.VK_SPACE) {
                    avanzarIntro();
                } else if (codigo == KeyEvent.VK_ESCAPE) {
                    alSalir.run();
                }
            }
            case JUEGO -> jugarTecla(codigo);
            case CUADERNO -> cuadernoTecla(codigo);
            case PAUSA -> pausaTecla(codigo);
            case CONFIRMAR_SALIDA -> salirTecla(codigo);
            case CERRAR_DIA -> finDiaTecla(codigo);
            case DERROTA -> {
                if (codigo == KeyEvent.VK_ENTER || codigo == KeyEvent.VK_ESCAPE) {
                    alSalir.run();
                }
            }
            default -> { }
        }
    }

    private void jugarTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_E -> interactuar();
            case KeyEvent.VK_ENTER -> {
                if (juego.turnoTerminado()) {
                    abrirCierreDeJornada();
                } else if (escena.zonaCercana() != null) {
                    interactuar();
                }
            }
            case KeyEvent.VK_I -> usarRapido();
            case KeyEvent.VK_Q -> soltar();
            case KeyEvent.VK_TAB -> abrirCuaderno();
            case KeyEvent.VK_ESCAPE -> {
                seleccion = 0;
                estado = Estado.PAUSA;
            }
            default -> { }
        }
    }

    private void cuadernoTecla(int codigo) {
        List<Objeto> objetos = inventario();
        switch (codigo) {
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_TAB -> {
                estado = Estado.JUEGO;
                seleccion = 0;
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                if (!objetos.isEmpty()) {
                    seleccion = (seleccion + objetos.size() - 1) % objetos.size();
                }
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                if (!objetos.isEmpty()) {
                    seleccion = (seleccion + 1) % objetos.size();
                }
            }
            case KeyEvent.VK_ENTER, KeyEvent.VK_E -> {
                if (!objetos.isEmpty()) {
                    Objeto elegido = objetos.get(Math.min(seleccion, objetos.size() - 1));
                    usarObjeto(elegido);
                }
            }
            default -> { }
        }
    }

    private void pausaTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> estado = Estado.JUEGO;
            case KeyEvent.VK_UP, KeyEvent.VK_W -> seleccion = (seleccion + OPCIONES_PAUSA.length - 1)
                    % OPCIONES_PAUSA.length;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> seleccion = (seleccion + 1) % OPCIONES_PAUSA.length;
            case KeyEvent.VK_ENTER -> {
                if (seleccion == 0) {
                    estado = Estado.JUEGO;
                } else if (seleccion == 1) {
                    if (guardarSilencioso()) {
                        mostrarToast("Progreso guardado.", Tipo.EXITO);
                    }
                } else {
                    seleccion = 0;
                    estado = Estado.CONFIRMAR_SALIDA;
                }
            }
            default -> { }
        }
    }

    private void salirTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> estado = Estado.PAUSA;
            case KeyEvent.VK_UP, KeyEvent.VK_W -> seleccion = (seleccion + OPCIONES_SALIR.length - 1)
                    % OPCIONES_SALIR.length;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> seleccion = (seleccion + 1) % OPCIONES_SALIR.length;
            case KeyEvent.VK_ENTER -> {
                if (seleccion == 0) {
                    if (guardarSilencioso()) {
                        alSalir.run();
                    }
                } else if (seleccion == 1) {
                    alSalir.run();
                } else {
                    estado = Estado.PAUSA;
                }
            }
            default -> { }
        }
    }

    private void finDiaTecla(int codigo) {
        List<Objeto> consumibles = new ArrayList<>();
        for (Objeto o : inventario()) {
            if (o.isUtilizable()) {
                consumibles.add(o);
            }
        }
        int total = consumibles.size() + OPCIONES_FIN.length;
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> {
                estado = Estado.JUEGO;
                confirmarAvance = false;
                seleccion = 0;
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W -> seleccion = (seleccion + total - 1) % total;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> seleccion = (seleccion + 1) % total;
            case KeyEvent.VK_ENTER -> {
                if (seleccion < consumibles.size()) {
                    usarObjeto(consumibles.get(seleccion));
                    seleccion = Math.min(seleccion, Math.max(0, consumibles.size() - 1));
                } else {
                    int opcion = seleccion - consumibles.size();
                    if (opcion == 0) {
                        estado = Estado.JUEGO;
                        confirmarAvance = false;
                        seleccion = 0;
                    } else if (opcion == 1) {
                        if (juego.getJugador().getCordura() < JuegoService.CORDURA_MINIMA_DIA
                                && !confirmarAvance) {
                            confirmarAvance = true;
                            mostrarToast("Si pasás así, perdés. ENTER otra vez para arriesgarte.", Tipo.ERROR);
                        } else {
                            avanzarDia();
                        }
                    } else {
                        if (guardarSilencioso()) {
                            alSalir.run();
                        }
                    }
                }
            }
            default -> { }
        }
    }

    private static boolean esMovimiento(int codigo) {
        return codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_A
                || codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_D
                || codigo == KeyEvent.VK_UP || codigo == KeyEvent.VK_DOWN
                || codigo == KeyEvent.VK_LEFT || codigo == KeyEvent.VK_RIGHT;
    }

    // ------------------------------------------------------------------ acciones

    private void interactuar() {
        if (estado != Estado.JUEGO) {
            return;
        }
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaInteraccion < 220) {
            return;
        }
        ultimaInteraccion = ahora;

        Zona zona = escena.zonaCercana();
        if (zona == null) {
            mostrarToast("No hay nada al alcance.", Tipo.CHARLA);
            return;
        }

        try {
            String mensaje = switch (prefijo(zona.id())) {
                case "cliente" -> atenderMostrador();
                case "preparar" -> juego.prepararPedido();
                case "tarea" -> juego.usarHerramientaEnTarea(Lugar.valueOf(zona.id().substring(6)));
                case "herramienta" -> juego.recogerHerramienta(Lugar.valueOf(zona.id().substring(12)));
                default -> "No pasa nada.";
            };
            registrar(mensaje, Tipo.CHARLA);
        } catch (InteraccionInvalidaException | TareaNoDisponibleException | PedidoInvalidoException e) {
            registrar(e.getMessage(), Tipo.ERROR);
            mostrarToast(e.getMessage(), Tipo.ERROR);
            avisarError(e);
        }
        verificarFin();
    }

    private static String prefijo(String id) {
        int corte = id.indexOf(':');
        return corte < 0 ? id : id.substring(0, corte);
    }

    private String atenderMostrador() throws InteraccionInvalidaException, PedidoInvalidoException {
        Cliente c = juego.clienteActual();
        if (c == null) {
            return "No queda nadie en el mostrador. Podés cerrar la jornada.";
        }
        if (!c.isPedidoTomado()) {
            return juego.tomarPedido();
        }
        // Siempre pasa por el Service: si la bandeja no es la correcta,
        // PedidoInvalidoException viaja hasta la UI y se muestra con JOptionPane.
        return juego.entregarPedido();
    }

    private void usarRapido() {
        try {
            registrar(juego.usarLoPrimero(), Tipo.EXITO);
        } catch (ObjetoNoDisponibleException | InteraccionInvalidaException e) {
            registrar(e.getMessage(), Tipo.ERROR);
            mostrarToast(e.getMessage(), Tipo.ERROR);
            avisarError(e);
        }
        verificarFin();
    }

    private void usarObjeto(Objeto objeto) {
        try {
            registrar(juego.usarObjeto(objeto), Tipo.EXITO);
        } catch (ObjetoNoDisponibleException | InteraccionInvalidaException e) {
            registrar(e.getMessage(), Tipo.ERROR);
            mostrarToast(e.getMessage(), Tipo.ERROR);
            avisarError(e);
        }
        verificarFin();
    }

    private void soltar() {
        try {
            registrar(juego.soltarEnMano(), Tipo.CHARLA);
        } catch (ObjetoNoDisponibleException e) {
            mostrarToast(e.getMessage(), Tipo.ERROR);
            avisarError(e);
        }
        verificarFin();
    }

    private void abrirCuaderno() {
        seleccion = 0;
        estado = Estado.CUADERNO;
    }

    private void cambiarSala(Sala sala) {
        registrar("Entraste a " + sala.getNombre() + ". " + sala.getDescripcion(), Tipo.CHARLA);
        mostrarToast(sala.getNombre().toUpperCase(), Tipo.SISTEMA);
    }

    private void verificarFin() {
        if (estado != Estado.JUEGO) {
            return;
        }
        if (juego.isPerdida()) {
            derrota(juego.getCausaPerdida());
        }
    }

    private void derrota(String causa) {
        if (estado == Estado.DERROTA) {
            return;
        }
        causaDerrota = causa;
        estado = Estado.DERROTA;
        seleccion = 0;
        teclas.clear();
        // No se guarda la derrota: al volver al menú se retoma la última partida guardada.
    }

    private void abrirCierreDeJornada() {
        if (!juego.turnoTerminado()) {
            mostrarToast("Todavía queda trabajo: " + juego.getPista(), Tipo.ERROR);
            return;
        }
        seleccion = 0;
        confirmarAvance = false;
        estado = Estado.CERRAR_DIA;
        teclas.clear();
    }

    private void avanzarDia() {
        JuegoService.ResultadoDia resultado = juego.avanzarDia();
        if (resultado.perdio()) {
            derrota(resultado.detalle());
            return;
        }
        if (!resultado.avanzo()) {
            registrar(resultado.detalle(), Tipo.ERROR);
            mostrarToast(resultado.detalle(), Tipo.ERROR);
            estado = Estado.JUEGO;
            return;
        }
        guardarSilencioso();
        estado = Estado.JUEGO;
        seleccion = 0;
        confirmarAvance = false;
        avisoCierre = false;
        escena.setSala(juego.getSala());
        escena.reiniciarPosicion();
        bitacora.clear();
        registrar(resultado.detalle(), Tipo.SISTEMA);
        mostrarToast("DÍA " + juego.getJugador().getDiaActual() + "  ·  "
                + juego.totalClientes() + " clientes  ·  " + juego.totalTareas() + " tareas", Tipo.SISTEMA);
    }

    // ------------------------------------------------------------------ guardado y salida

    private boolean guardarSilencioso() {
        try {
            partidas.guardarProgreso(juego.getJugador());
            return true;
        } catch (PersistenciaException e) {
            mostrarToast("No se pudo guardar: " + e.getMessage(), Tipo.ERROR);
            avisarError(e);
            return false;
        }
    }

    /** Lo llama VentanaPrincipal al cerrar la ventana. Guarda el avance y deja cerrar igual. */
    public boolean confirmarSalida() {
        if (estado != Estado.DERROTA) {
            guardarSilencioso();
        }
        return true;
    }

    // ------------------------------------------------------------------ utilidades

    private void registrar(String texto, Tipo tipo) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        bitacora.addLast(new Entrada(texto, tipo));
        while (bitacora.size() > 40) {
            bitacora.removeFirst();
        }
    }

    /**
     * Muestra el cuadro de diálogo de la excepción y detiene el reloj mientras está abierto:
     * un JOptionPane modal comparte el EDT y, si no paráramos el Timer, la partida seguiría corriendo detrás.
     */
    private void avisarError(Exception e) {
        boolean corriendo = timer.isRunning();
        timer.stop();
        try {
            Dialogos.error(this, e);
        } finally {
            if (corriendo && isDisplayable()) {
                timer.start();
            }
        }
    }

    private void mostrarToast(String texto, Tipo tipo) {
        this.toast = texto;
        this.toastTicks = tipo == Tipo.ERROR ? 260 : 190;
    }

    private static void dibujarIcono(Graphics2D g, Objeto objeto, int x, int y) {
        if (objeto instanceof Herramienta h) {
            EscenaBuffet.dibujarHerramienta(g, h.getLugar(), x, y);
            return;
        }
        if ("PEDIDO".equals(objeto.getTipoObjeto())) {
            dibujarBandeja(g, x, y + 2);
            return;
        }
        EscenaBuffet.dibujarObjeto(g, objeto, x, y);
    }

    /** Bandeja de metal con un ojo: el icono del juego. */
    private static void dibujarBandeja(Graphics2D g, int x, int y) {
        Pixel.elipse(g, x + 2, y + 6, 20, 7, new Color(0x1A211E));
        Pixel.elipse(g, x + 3, y + 5, 18, 6, new Color(0x8A9691));
        Pixel.elipse(g, x + 4, y + 5, 16, 4, new Color(0xC3CCC6));
        Pixel.elipse(g, x + 7, y + 1, 10, 7, new Color(0xF1EAD8));
        Pixel.elipse(g, x + 9, y + 2, 6, 5, new Color(0x7FB58A));
        Pixel.elipse(g, x + 11, y + 3, 2, 3, new Color(0x120F0C));
    }
}
