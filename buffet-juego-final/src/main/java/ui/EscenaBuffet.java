package ui;

import modelo.Cliente;
import modelo.Lugar;
import service.JuegoService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Plano 2D del buffet dibujado con Java2D: el personaje se mueve con WASD / flechas e interactúa con E.
 * Es solo vista: las decisiones y las reglas siguen en JuegoService (la llama PantallaJuego).
 */
public class EscenaBuffet extends JPanel {

    /**
     * Lugares del buffet donde se puede interactuar. Cliente e inventario son las acciones de la consola;
     * el resto son los rincones donde espera una tarea (cada uno con su Lugar).
     */
    public enum Zona {
        CLIENTE("Esperar a que venga un cliente", new Rectangle(330, 250, 240, 60), null),
        TAREA("Limpiar el baño", new Rectangle(45, 370, 175, 135), Lugar.BANO),
        PLANCHA("Limpiar la plancha", new Rectangle(40, 255, 115, 50), Lugar.PLANCHA),
        TABLERO("Revisar el tablero de luz", new Rectangle(165, 255, 100, 50), Lugar.TABLERO),
        MESAS("Limpiar las mesas", new Rectangle(270, 350, 330, 115), Lugar.MESAS),
        CAFETERA("Destapar la cafetera", new Rectangle(590, 255, 125, 50), Lugar.CAFETERA),
        HELADERA("Acomodar la heladera", new Rectangle(745, 255, 120, 45), Lugar.HELADERA),
        BASURA("Sacar la basura", new Rectangle(600, 425, 95, 80), Lugar.BASURA),
        INVENTARIO("Abrir inventario", new Rectangle(700, 425, 170, 80), null);

        final String etiqueta;
        final Rectangle area;   // en coordenadas de los pies del personaje
        final Lugar lugar;      // null = no es una tarea

        Zona(String etiqueta, Rectangle area, Lugar lugar) {
            this.etiqueta = etiqueta;
            this.area = area;
            this.lugar = lugar;
        }
    }

    public record Opcion(String texto, Runnable accion) { }

    private static final class Flotante {
        final String texto; final Color color; final double x; double y; int vida = 70;
        Flotante(String texto, Color color, double x, double y) { this.texto = texto; this.color = color; this.x = x; this.y = y; }
    }

    private enum EstadoCliente { AUSENTE, LLEGA, ESPERA, SE_VA }

    /** Cómo se ve cada cliente (la lógica no sabe de colores: se elige por el id del cliente). */
    private record AspectoCliente(Color ropa, Color pelo, Color piel, double escala, int accesorio) { }

    private static final AspectoCliente[] ASPECTOS = {
            new AspectoCliente(new Color(0x5B7C99), new Color(0x6B4A2F), new Color(0xD9A87C), 1.00, 0),   // un cliente
            new AspectoCliente(new Color(0xEDEBDD), new Color(0x2A1B12), new Color(0xE8C39E), 0.78, 1),   // nena de guardapolvo
            new AspectoCliente(new Color(0xB5483A), new Color(0x3A3A3A), new Color(0xC8956A), 1.10, 2),   // profesor de gimnasia
            new AspectoCliente(new Color(0xD9B73A), new Color(0x6E6E6E), new Color(0xD9A87C), 1.00, 3),   // señor de impermeable
            new AspectoCliente(new Color(0x6B3F5E), new Color(0x9A9A9A), new Color(0xE0B98F), 1.00, 4),   // directora
    };

    /** Dónde flota el "!" de cada tarea pendiente y dónde salen las estrellitas al completarla. */
    private static final Point2D[] BURBUJAS = new Point2D[Lugar.values().length];
    private static final Point2D[] DESTELLOS = new Point2D[Lugar.values().length];
    static {
        BURBUJAS[Lugar.BANO.ordinal()] = new Point2D.Double(140, 334);
        BURBUJAS[Lugar.MESAS.ordinal()] = new Point2D.Double(450, 352);
        BURBUJAS[Lugar.PLANCHA.ordinal()] = new Point2D.Double(95, 122);
        BURBUJAS[Lugar.CAFETERA.ordinal()] = new Point2D.Double(668, 112);
        BURBUJAS[Lugar.HELADERA.ordinal()] = new Point2D.Double(809, 62);
        BURBUJAS[Lugar.BASURA.ordinal()] = new Point2D.Double(650, 392);
        BURBUJAS[Lugar.TABLERO.ordinal()] = new Point2D.Double(207, 64);
        DESTELLOS[Lugar.BANO.ordinal()] = new Point2D.Double(120, 432);
        DESTELLOS[Lugar.MESAS.ordinal()] = new Point2D.Double(450, 408);
        DESTELLOS[Lugar.PLANCHA.ordinal()] = new Point2D.Double(95, 205);
        DESTELLOS[Lugar.CAFETERA.ordinal()] = new Point2D.Double(668, 165);
        DESTELLOS[Lugar.HELADERA.ordinal()] = new Point2D.Double(809, 170);
        DESTELLOS[Lugar.BASURA.ordinal()] = new Point2D.Double(650, 450);
        DESTELLOS[Lugar.TABLERO.ordinal()] = new Point2D.Double(207, 135);
    }

    private static final int W = 900, H = 520;                                   // tamaño lógico del mundo
    private static final Rectangle CAMINABLE = new Rectangle(45, 255, 810, 240);
    private static final Rectangle CASILLEROS = new Rectangle(725, 300, 150, 125);   // sólido: no se puede atravesar
    private static final Rectangle MESA_1 = new Rectangle(290, 398, 120, 30);        // sólidos: se rodean, no se atraviesan
    private static final Rectangle MESA_2 = new Rectangle(490, 398, 120, 30);
    private static final Rectangle TACHO = new Rectangle(628, 452, 44, 30);
    private static final Rectangle[] SOLIDOS = {CASILLEROS, MESA_1, MESA_2, TACHO};
    private static final double VELOCIDAD = 3.4;
    private static final int MOSTRADOR_X = 450, PUERTA_X = 930;
    private static final Color CONTORNO = new Color(0x0E1412);

    private final JuegoService juego;
    private final Consumer<Zona> alInteractuar;
    private final javax.swing.Timer reloj = new javax.swing.Timer(16, e -> tick());
    private final Set<Integer> teclas = new HashSet<>();
    private final List<Flotante> flotantes = new ArrayList<>();

    private static final double PASO_FIJO = 1.0 / 60;   // la lógica avanza a 60 pasos por segundo en cualquier PC
    private long ultimoTick = System.nanoTime();
    private double acumulado;

    private double px = 450, py = 340, paso;
    private int mirada = 0, cuadro, sacudida, avisoVida;
    private boolean camina;
    private Zona activa;

    private EstadoCliente estadoCliente = EstadoCliente.AUSENTE;
    private Cliente clienteEnEscena;          // el cliente que se ve caminando / esperando
    private double clienteX = PUERTA_X, pasoCliente;
    private Boolean clienteContento;          // null = neutro
    private final boolean[] pendiente = new boolean[Lugar.values().length];   // para detectar "se acaba de completar"
    private final int[] brillo = new int[Lugar.values().length];              // estrellitas por lugar

    private String dialogoTitulo = "", dialogoCuerpo = "", aviso = "";
    private Opcion[] opciones;                // null = sin diálogo abierto
    private Rectangle[] zonasOpcion = new Rectangle[0];
    private int opcionResaltada = -1;
    private double escala = 1, desdeX, desdeY;

    public EscenaBuffet(JuegoService juego, Consumer<Zona> alInteractuar) {
        this.juego = juego;
        this.alInteractuar = alInteractuar;
        for (Lugar l : Lugar.values()) {
            pendiente[l.ordinal()] = juego.hayTareaPendiente(l);
        }
        setBackground(Tema.FONDO);
        setFocusable(true);
        setPreferredSize(new Dimension(W, H));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                teclaPresionada(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                teclas.remove(e.getKeyCode());
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                teclas.clear();
            }
        });
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                opcionResaltada = opcionEn(e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int i = opcionEn(e.getPoint());
                if (i >= 0) {
                    opciones[i].accion().run();
                }
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        // La animación corre solo mientras la escena se ve en pantalla.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    ultimoTick = System.nanoTime();
                    reloj.start();
                    requestFocusInWindow();
                } else {
                    reloj.stop();
                }
            }
        });
    }

    // ---------- API para PantallaJuego ----------

    public void mostrarDialogo(String titulo, String cuerpo, Opcion... nuevas) {
        dialogoTitulo = titulo;
        dialogoCuerpo = cuerpo;
        opciones = nuevas;
        opcionResaltada = -1;
        teclas.clear();
    }

    public void cerrarDialogo() {
        opciones = null;
    }

    public void aviso(String mensaje) {
        aviso = mensaje;
        avisoVida = 220;
    }

    public void reaccionCliente(boolean contento) {
        clienteContento = contento;
    }

    public void efectoCordura(int delta) {
        flotantes.add(new Flotante("Cordura " + (delta > 0 ? "+" : "") + delta,
                delta > 0 ? Tema.BIEN : Tema.PELIGRO, Math.min(px + 75, W - 70), py - 60));
        if (delta < 0) {
            sacudida = 20;
        }
    }

    // ---------- entrada ----------

    private void teclaPresionada(int k) {
        if (opciones != null) {
            int i = k - KeyEvent.VK_1;
            if (i >= 0 && i < opciones.length) {
                opciones[i].accion().run();
            }
            return;
        }
        boolean nueva = teclas.add(k);
        if (k == KeyEvent.VK_E && nueva && activa != null) {
            boolean clienteEnCamino = activa == Zona.CLIENTE && juego.hayClientePendiente()
                    && (estadoCliente == EstadoCliente.AUSENTE || estadoCliente == EstadoCliente.SE_VA);
            if (clienteEnCamino) {
                aviso("Ya viene el próximo cliente...");
            } else {
                alInteractuar.accept(activa);
            }
        }
    }

    private boolean pulsada(int... codigos) {
        for (int c : codigos) {
            if (teclas.contains(c)) {
                return true;
            }
        }
        return false;
    }

    // ---------- lógica de cada cuadro ----------

    /** Corre cada ~16 ms, pero avanza el juego en pasos de tiempo fijo: la velocidad no depende del hardware. */
    private void tick() {
        long ahora = System.nanoTime();
        acumulado += Math.min(0.1, (ahora - ultimoTick) / 1e9);
        ultimoTick = ahora;
        while (acumulado >= PASO_FIJO) {
            avanzar();
            acumulado -= PASO_FIJO;
        }
        repaint();
    }

    private void avanzar() {
        cuadro++;
        if (opciones == null) {
            mover();
        }
        activa = null;
        for (Zona z : Zona.values()) {
            if (z.area.contains(px, py)) {
                activa = z;
            }
        }
        actualizarCliente();
        for (Lugar l : Lugar.values()) {
            int i = l.ordinal();
            boolean hay = juego.hayTareaPendiente(l);
            if (pendiente[i] && !hay) {
                brillo[i] = 90;   // recién la completaste: estrellitas en ese rincón
            }
            pendiente[i] = hay;
            if (brillo[i] > 0) brillo[i]--;
        }
        if (sacudida > 0) sacudida--;
        if (avisoVida > 0) avisoVida--;
        flotantes.removeIf(f -> --f.vida <= 0);
        flotantes.forEach(f -> f.y -= 0.7);
    }

    private void mover() {
        double dx = 0, dy = 0;
        if (pulsada(KeyEvent.VK_A, KeyEvent.VK_LEFT)) dx -= 1;
        if (pulsada(KeyEvent.VK_D, KeyEvent.VK_RIGHT)) dx += 1;
        if (pulsada(KeyEvent.VK_W, KeyEvent.VK_UP)) dy -= 1;
        if (pulsada(KeyEvent.VK_S, KeyEvent.VK_DOWN)) dy += 1;
        camina = dx != 0 || dy != 0;
        if (!camina) {
            return;
        }
        double largo = Math.hypot(dx, dy);
        double nx = Math.max(CAMINABLE.x, Math.min(CAMINABLE.x + CAMINABLE.width, px + dx / largo * VELOCIDAD));
        double ny = Math.max(CAMINABLE.y, Math.min(CAMINABLE.y + CAMINABLE.height, py + dy / largo * VELOCIDAD));
        if (!chocaConSolido(nx, py)) px = nx;   // cada eje por separado: se puede deslizar a lo largo de la pared
        if (!chocaConSolido(px, ny)) py = ny;
        if (dx != 0) {
            mirada = (int) Math.signum(dx);
        }
        paso += 0.28;
    }

    private static boolean chocaConSolido(double x, double y) {
        for (Rectangle r : SOLIDOS) {
            if (r.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private void actualizarCliente() {
        switch (estadoCliente) {
            case AUSENTE:
                Cliente proximo = juego.proximoCliente();
                if (proximo != null) {
                    clienteEnEscena = proximo;
                    estadoCliente = EstadoCliente.LLEGA;
                    clienteX = PUERTA_X;
                    clienteContento = null;
                }
                break;
            case LLEGA:
                clienteX -= 2.6;
                pasoCliente += 0.25;
                if (clienteX <= MOSTRADOR_X) {
                    clienteX = MOSTRADOR_X;
                    estadoCliente = EstadoCliente.ESPERA;
                }
                if (clienteEnEscena.isAtendido()) {
                    estadoCliente = EstadoCliente.SE_VA;
                }
                break;
            case ESPERA:
                if (clienteEnEscena.isAtendido()) {
                    estadoCliente = EstadoCliente.SE_VA;
                }
                break;
            case SE_VA:
                clienteX += 3;
                pasoCliente += 0.25;
                if (clienteX > PUERTA_X) {
                    estadoCliente = EstadoCliente.AUSENTE;   // si queda alguien en la fila, entra el siguiente
                }
                break;
        }
        // Si el jugador interactúa mientras el cliente aún camina, se planta en el mostrador.
        if (opciones != null && estadoCliente == EstadoCliente.LLEGA) {
            clienteX = MOSTRADOR_X;
            estadoCliente = EstadoCliente.ESPERA;
        }
    }

    // ---------- dibujo ----------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        escala = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        desdeX = (getWidth() - W * escala) / 2;
        desdeY = (getHeight() - H * escala) / 2;
        g.translate(desdeX, desdeY);
        g.scale(escala, escala);
        g.clip(new Rectangle(0, 0, W, H));
        if (sacudida > 0) {
            g.translate(Math.sin(cuadro * 2.3) * sacudida * 0.35, Math.cos(cuadro * 1.7) * sacudida * 0.25);
        }

        pintarSalon(g);
        pintarCliente(g);
        pintarMostrador(g);
        pintarPlancha(g);
        pintarTablero(g);
        pintarHeladera(g);
        pintarTarea(g);
        pintarCasilleros(g);
        pintarObjetosYJugador(g);
        pintarBurbujas(g);
        pintarDestellos(g);
        flotantes.forEach(f -> textoConContorno(g, f.texto, Tema.SUBTITULO.deriveFont(20f), f.color, f.x, f.y, true));
        pintarAmbiente(g);
        if (opciones != null) {
            pintarDialogo(g);
        } else {
            if (activa != null) pintarIndicacion(g);
            if (avisoVida > 0) pintarAviso(g);
        }
        g.dispose();
    }

    private static void forma(Graphics2D g, Shape s, Color relleno) {
        g.setColor(relleno);
        g.fill(s);
        g.setColor(CONTORNO);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    private static void baldosas(Graphics2D g, int x, int y, int w, int h, int t, Color a, Color b) {
        for (int i = 0; i * t < w; i++) {
            for (int j = 0; j * t < h; j++) {
                g.setColor((i + j) % 2 == 0 ? a : b);
                g.fillRect(x + i * t, y + j * t, Math.min(t, w - i * t), Math.min(t, h - j * t));
            }
        }
    }

    private void pintarSalon(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x2E4039), 0, 110, new Color(0x24322D)));
        g.fillRect(0, 0, W, 110);
        g.setColor(new Color(0x1B2622));
        g.fillRect(0, 100, W, 10);
        baldosas(g, 0, 110, W, 75, 36, new Color(0x3B4B45), new Color(0x344340));
        baldosas(g, 0, 235, W, 285, 45, new Color(0x4A5A53), new Color(0x435249));

        // ventanas de noche
        for (int x : new int[]{90, 700}) {
            forma(g, new Rectangle2D.Double(x, 18, 110, 66), new Color(0x13222D));
            g.setColor(CONTORNO);
            g.drawLine(x + 55, 18, x + 55, 84);
            g.drawLine(x, 51, x + 110, 51);
        }
        // algo mira desde la ventana cuando la cordura baja
        if (juego.getJugador().getCordura() <= 50 && cuadro % 420 < 260) {
            g.setColor(new Color(0x05080A));
            g.fill(new Ellipse2D.Double(122, 34, 46, 50));
            g.setColor(new Color(0xEDEBDD));
            g.fill(new Ellipse2D.Double(134, 48, 8, 6));
            g.fill(new Ellipse2D.Double(150, 48, 8, 6));
        }
        // pizarra del menú
        forma(g, new RoundRectangle2D.Double(330, 12, 240, 50, 8, 8), new Color(0x1F2D28));
        g.setColor(new Color(0x8A6A3B));
        g.setStroke(new BasicStroke(4));
        g.draw(new RoundRectangle2D.Double(330, 12, 240, 50, 8, 8));
        textoCentrado(g, "BUFFET", Tema.ESCENA.deriveFont(26f), Tema.TEXTO, 450, 46);
    }

    private void pintarMostrador(Graphics2D g) {
        forma(g, new RoundRectangle2D.Double(150, 185, 600, 28, 10, 10), new Color(0xC79A55));
        forma(g, new Rectangle2D.Double(158, 213, 584, 32), new Color(0x92693A));
        textoCentrado(g, "Mostrador", Tema.CUERPO.deriveFont(Font.BOLD, 14f), new Color(0xF0DDB4), 450, 234);
        // máquina de café y tazas
        forma(g, new RoundRectangle2D.Double(640, 140, 56, 50, 8, 8), new Color(0x3A3F45));
        boolean tapada = juego.hayTareaPendiente(Lugar.CAFETERA);
        g.setColor(tapada ? Tema.PELIGRO : Tema.BIEN);   // luz roja = hay que destaparla
        g.fill(new Ellipse2D.Double(650, 148, 8, 8));
        forma(g, new Rectangle2D.Double(662, 170, 12, 20), new Color(0x1E2226));
        if (tapada) {
            // gotea algo oscuro sobre el mostrador
            g.setColor(new Color(0x2A1A12));
            g.fill(new Ellipse2D.Double(652, 200, 32, 7));
            double caida = (cuadro * 1.4) % 24;
            g.fill(new Ellipse2D.Double(666, 192 + caida, 5, 8));
        } else {
            // vapor de café recién hecho
            for (int i = 0; i < 2; i++) {
                double t = (cuadro * 0.7 + i * 30) % 60;
                g.setColor(new Color(235, 235, 225, (int) (120 * (1 - t / 60))));
                g.fill(new Ellipse2D.Double(660 + i * 12 + Math.sin((cuadro + i * 20) * 0.08) * 4, 134 - t * 0.5, 9, 9));
            }
        }
        for (int x : new int[]{230, 268}) {
            forma(g, new RoundRectangle2D.Double(x, 168, 20, 20, 6, 6), new Color(0xEDEBDD));
        }
    }

    private void pintarTarea(Graphics2D g) {
        // puerta del baño en la pared izquierda
        forma(g, new RoundRectangle2D.Double(-6, 295, 42, 130, 6, 6), new Color(0x5B6E66));
        textoCentrado(g, "Baño", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, 18, 288);
        boolean pendiente = juego.hayTareaPendiente(Lugar.BANO);
        if (pendiente) {
            g.setColor(new Color(63, 107, 115, 180));
            g.fill(new Ellipse2D.Double(64, 418, 112, 38));
            g.setColor(new Color(150, 200, 205, 120));
            g.fill(new Ellipse2D.Double(86, 424, 40, 10));
        }
        // balde y trapeador
        g.setColor(CONTORNO);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(196, 372, 210, 424));
        forma(g, new Ellipse2D.Double(198, 418, 26, 12), new Color(0xB8B29A));
        Path2D balde = new Path2D.Double();
        balde.moveTo(176, 408); balde.lineTo(220, 408); balde.lineTo(214, 444); balde.lineTo(182, 444); balde.closePath();
        forma(g, balde, new Color(0xD9A441));
        textoCentrado(g, "Tareas", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, 140, 372);
    }

    private void pintarCasilleros(Graphics2D g) {
        for (int i = 0; i < 3; i++) {
            double x = 735 + i * 45;
            forma(g, new Rectangle2D.Double(x, 300, 45, 125), new Color(i % 2 == 0 ? 0x566B62 : 0x4D6058));
            g.setColor(CONTORNO);
            g.setStroke(new BasicStroke(2f));
            for (int r = 0; r < 3; r++) {
                g.draw(new Line2D.Double(x + 12, 315 + r * 7, x + 33, 315 + r * 7));
            }
            g.setColor(new Color(0xD9A441));
            g.fill(new Ellipse2D.Double(x + 30, 370, 7, 7));
        }
        textoCentrado(g, "Inventario", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, 802, 292);
    }

    private void pintarCliente(Graphics2D g) {
        if (estadoCliente == EstadoCliente.AUSENTE || clienteEnEscena == null) {
            return;
        }
        AspectoCliente a = ASPECTOS[Math.floorMod(clienteEnEscena.getId() - 1, ASPECTOS.length)];
        boolean camina = estadoCliente != EstadoCliente.ESPERA;
        int dir = estadoCliente == EstadoCliente.LLEGA ? -1 : estadoCliente == EstadoCliente.SE_VA ? 1 : 0;
        double rebote = camina ? Math.abs(Math.sin(pasoCliente)) * 3 : 0;

        Graphics2D gc = (Graphics2D) g.create();
        gc.translate(clienteX, 190);
        gc.scale(a.escala(), a.escala());
        gc.translate(-clienteX, -190);
        if (a.accesorio() == 3) {
            charcoDelSenor(gc, clienteX, 190);
        }
        persona(gc, clienteX, 190, dir, pasoCliente, camina,
                a.ropa(), null, a.pelo(), false, clienteContento, a.piel());
        accesorioCliente(gc, a.accesorio(), clienteX, 190 - rebote);
        gc.dispose();

        if (estadoCliente == EstadoCliente.ESPERA && opciones == null) {
            burbujaAdmiracion(g, clienteX, 190 - 101 * a.escala() - 17);
        }
    }

    private void charcoDelSenor(Graphics2D g, double x, double y) {
        g.setColor(new Color(60, 100, 130, 150));
        g.fill(new Ellipse2D.Double(x - 36, y - 9, 72, 18));
        g.setColor(new Color(150, 195, 225, 110));
        g.fill(new Ellipse2D.Double(x - 18, y - 5, 22, 6));
    }

    /** Detalles que distinguen a cada cliente: moños, gorra, sombrero de lluvia, anteojos. */
    private void accesorioCliente(Graphics2D g, int tipo, double x, double y) {
        switch (tipo) {
            case 1:   // la nena: trenzas y moños rosas
                forma(g, new RoundRectangle2D.Double(x - 27, y - 84, 8, 28, 6, 6), new Color(0x2A1B12));
                forma(g, new RoundRectangle2D.Double(x + 19, y - 84, 8, 28, 6, 6), new Color(0x2A1B12));
                forma(g, new Ellipse2D.Double(x - 27, y - 100, 14, 11), new Color(0xE58FB0));
                forma(g, new Ellipse2D.Double(x + 13, y - 100, 14, 11), new Color(0xE58FB0));
                break;
            case 2:   // el profesor: gorra azul y silbato
                forma(g, new RoundRectangle2D.Double(x - 19, y - 112, 38, 16, 9, 9), new Color(0x2F5E8A));
                forma(g, new RoundRectangle2D.Double(x - 24, y - 100, 48, 6, 4, 4), new Color(0x244B6E));
                g.setColor(CONTORNO);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Line2D.Double(x - 7, y - 62, x, y - 46));
                g.draw(new Line2D.Double(x + 7, y - 62, x, y - 46));
                forma(g, new Ellipse2D.Double(x - 5, y - 48, 10, 8), new Color(0xE6E6E6));
                break;
            case 3:   // el señor: sombrero de lluvia y gotas que caen sin parar
                forma(g, new RoundRectangle2D.Double(x - 19, y - 112, 38, 18, 10, 10), new Color(0xD9B73A));
                forma(g, new RoundRectangle2D.Double(x - 27, y - 100, 54, 6, 4, 4), new Color(0xC0A02E));
                for (int i = 0; i < 3; i++) {
                    double caida = (cuadro * 1.8 + i * 17) % 52;
                    g.setColor(new Color(110, 160, 200, (int) (230 * (1 - caida / 52))));
                    g.fill(new Ellipse2D.Double(x - 15 + i * 15, y - 50 + caida, 4, 7));
                }
                break;
            case 4:   // la directora: rodete y anteojos
                forma(g, new Ellipse2D.Double(x - 9, y - 116, 18, 16), new Color(0x9A9A9A));
                g.setColor(CONTORNO);
                g.setStroke(new BasicStroke(2.2f));
                g.draw(new Ellipse2D.Double(x - 15, y - 90, 14, 14));
                g.draw(new Ellipse2D.Double(x + 1, y - 90, 14, 14));
                g.draw(new Line2D.Double(x - 1, y - 83, x + 1, y - 83));
                break;
            default:
                break;
        }
    }

    /** Personaje estilo "flash": contorno grueso, colores planos y caminata con balanceo. */
    private void persona(Graphics2D g, double x, double y, int mira, double paso, boolean camina,
                         Color ropa, Color delantal, Color pelo, boolean gorro, Boolean contento, Color piel) {
        double rebote = camina ? Math.abs(Math.sin(paso)) * 3 : 0;
        double osc = camina ? Math.sin(paso) * 7 : 0;
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(x - 22, y - 7, 44, 14));
        y -= rebote;
        Color pantalon = new Color(0x2C3540);
        forma(g, new RoundRectangle2D.Double(x - 13 + osc * 0.5, y - 26, 10, 26, 5, 5), pantalon);
        forma(g, new RoundRectangle2D.Double(x + 3 - osc * 0.5, y - 26, 10, 26, 5, 5), pantalon);
        forma(g, new RoundRectangle2D.Double(x - 17, y - 62, 34, 40, 12, 12), ropa);
        if (delantal != null) {
            forma(g, new RoundRectangle2D.Double(x - 12, y - 54, 24, 32, 6, 6), delantal);
        }
        forma(g, new Ellipse2D.Double(x - 25, y - 54 + osc * 0.5, 11, 22), ropa);
        forma(g, new Ellipse2D.Double(x + 14, y - 54 - osc * 0.5, 11, 22), ropa);
        forma(g, new Ellipse2D.Double(x - 18, y - 98, 36, 38), piel);
        g.setColor(pelo);
        g.fill(new Arc2D.Double(x - 19, y - 101, 38, 32, 0, 180, Arc2D.CHORD));
        g.setColor(CONTORNO);
        g.draw(new Arc2D.Double(x - 19, y - 101, 38, 32, 0, 180, Arc2D.CHORD));
        if (gorro) {
            forma(g, new RoundRectangle2D.Double(x - 15, y - 116, 30, 18, 8, 8), new Color(0xEDEBDD));
            forma(g, new Rectangle2D.Double(x - 18, y - 103, 36, 6), new Color(0xEDEBDD));
        }
        for (int s = -1; s <= 1; s += 2) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x + s * 7 - 4 + mira * 1.5, y - 85, 8, 10));
            g.setColor(CONTORNO);
            g.fill(new Ellipse2D.Double(x + s * 7 - 2 + mira * 3, y - 82, 4, 5));
        }
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(CONTORNO);
        if (contento == null) {
            g.draw(new Line2D.Double(x - 4, y - 68, x + 4, y - 68));
        } else if (contento) {
            g.draw(new Arc2D.Double(x - 6, y - 74, 12, 9, 180, 180, Arc2D.OPEN));
        } else {
            g.draw(new Arc2D.Double(x - 6, y - 68, 12, 9, 0, 180, Arc2D.OPEN));
        }
    }

    // ---------- rincones con tarea ----------

    /** Mesas, tacho y personaje se ordenan por la altura de sus pies: el que está más abajo tapa al de atrás. */
    private void pintarObjetosYJugador(Graphics2D g) {
        record Pieza(double y, Runnable dibujo) { }
        List<Pieza> piezas = new ArrayList<>();
        boolean mesasSucias = juego.hayTareaPendiente(Lugar.MESAS);
        piezas.add(new Pieza(MESA_1.y + MESA_1.height, () -> pintarMesa(g, MESA_1.x, mesasSucias, 0)));
        piezas.add(new Pieza(MESA_2.y + MESA_2.height, () -> pintarMesa(g, MESA_2.x, mesasSucias, 1)));
        piezas.add(new Pieza(TACHO.y + TACHO.height + 2, () -> pintarTacho(g)));
        piezas.add(new Pieza(py, () -> persona(g, px, py, mirada, camina ? paso : 0, camina,
                new Color(0x3F6B5B), new Color(0xD9A441), new Color(0x3A2A1E), true, null, new Color(0xE8C39E))));
        piezas.sort((a, b) -> Double.compare(a.y(), b.y()));
        piezas.forEach(p -> p.dibujo().run());
    }

    private void pintarMesa(Graphics2D g, int x, boolean sucia, int variante) {
        forma(g, new Rectangle2D.Double(x + 10, 408, 9, 30), new Color(0x6B4A2F));
        forma(g, new Rectangle2D.Double(x + 101, 408, 9, 30), new Color(0x6B4A2F));
        forma(g, new RoundRectangle2D.Double(x, 390, 120, 20, 10, 10), new Color(0xC79A55));
        g.setColor(new Color(0xE0B873));
        g.fill(new RoundRectangle2D.Double(x + 6, 393, 108, 5, 4, 4));
        if (sucia) {
            g.setColor(new Color(0x4A3322));
            g.fill(new Ellipse2D.Double(x + 18, 396, 26, 8));
            g.fill(new Ellipse2D.Double(x + 62, 399, 32, 7));
            g.setColor(new Color(0x7A3B2E));
            g.fill(new Ellipse2D.Double(x + 44, 393, 12, 5));
            for (int i = 0; i < 6; i++) {            // migas
                g.setColor(new Color(0xE0C98A));
                g.fill(new Ellipse2D.Double(x + 12 + i * 17, 403 - (i % 2) * 2, 4, 3));
            }
            // plato con restos y un vaso tirado
            forma(g, new Ellipse2D.Double(x + 78 - variante * 50, 381, 30, 10), new Color(0xEDEBDD));
            g.setColor(new Color(0xB5483A));
            g.fill(new Ellipse2D.Double(x + 86 - variante * 50, 382, 12, 5));
            forma(g, new RoundRectangle2D.Double(x + 22 + variante * 42, 396, 18, 8, 4, 4), new Color(0xA9C7CC));
            // moscas
            g.setColor(CONTORNO);
            for (int i = 0; i < 2; i++) {
                double a = cuadro * 0.11 + i * 3 + variante;
                g.fill(new Ellipse2D.Double(x + 60 + Math.cos(a) * 24, 370 + Math.sin(a * 1.3) * 8, 3, 3));
            }
        } else {
            forma(g, new Rectangle2D.Double(x + 52, 378, 16, 13), new Color(0xEDEBDD));   // servilletero
            g.setColor(new Color(255, 255, 255, 110));
            g.fill(new RoundRectangle2D.Double(x + 14, 394, 40, 3, 3, 3));
        }
        if (variante == 0) {
            textoCentrado(g, "Mesas", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, x + 120 + 40, 380);
        }
    }

    private void pintarTacho(Graphics2D g) {
        boolean llena = juego.hayTareaPendiente(Lugar.BASURA);
        Path2D cuerpo = new Path2D.Double();
        cuerpo.moveTo(630, 438); cuerpo.lineTo(670, 438); cuerpo.lineTo(665, 484); cuerpo.lineTo(635, 484); cuerpo.closePath();
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(624, 478, 52, 12));
        if (llena) {
            forma(g, new Ellipse2D.Double(623, 414, 54, 34), new Color(0x1F2A26));      // bolsa rebalsando
            g.setColor(new Color(255, 255, 255, 40));
            g.fill(new Ellipse2D.Double(633, 420, 14, 8));
        }
        forma(g, cuerpo, new Color(0x5B6E66));
        g.setColor(CONTORNO);
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 3; i++) {
            g.draw(new Line2D.Double(641 + i * 8, 446, 640 + i * 9, 478));
        }
        if (llena) {
            for (int i = 0; i < 3; i++) {   // moscas dando vueltas
                double a = cuadro * 0.17 + i * 2.1;
                g.fill(new Ellipse2D.Double(650 + Math.cos(a) * 26 - 1.5, 410 + Math.sin(a * 1.4) * 12, 3.5, 3.5));
            }
        } else {
            forma(g, new RoundRectangle2D.Double(626, 430, 48, 10, 6, 6), new Color(0x7B8E86));   // tapa cerrada
        }
        textoCentrado(g, "Basura", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, 650, 502);
    }

    private void pintarPlancha(Graphics2D g) {
        boolean sucia = juego.hayTareaPendiente(Lugar.PLANCHA);
        forma(g, new RoundRectangle2D.Double(50, 170, 90, 75, 8, 8), new Color(0x6C7773));
        forma(g, new RoundRectangle2D.Double(46, 160, 98, 16, 6, 6), sucia ? new Color(0x2B2522) : new Color(0xC3CDC9));
        for (int i = 0; i < 3; i++) {
            forma(g, new Ellipse2D.Double(63 + i * 24, 186, 12, 12), new Color(0xD9A441));
        }
        if (sucia) {
            g.setColor(new Color(0x2B2522));
            g.fill(new Ellipse2D.Double(60, 204, 30, 10));
            g.fill(new Ellipse2D.Double(98, 216, 26, 9));
            g.setColor(new Color(0x4A3322));
            g.fill(new Ellipse2D.Double(80, 160, 22, 7));
            for (int i = 0; i < 3; i++) {   // humo que sube
                double t = (cuadro * 0.6 + i * 22) % 66;
                g.setColor(new Color(150, 150, 145, (int) (140 * (1 - t / 66))));
                g.fill(new Ellipse2D.Double(70 + i * 20 + Math.sin((cuadro + i * 30) * 0.06) * 7, 150 - t, 14 + t * 0.15, 14 + t * 0.15));
            }
        } else {
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(new RoundRectangle2D.Double(54, 163, 50, 4, 4, 4));
            g.fill(new RoundRectangle2D.Double(58, 172, 4, 60, 4, 4));
        }
        textoCentrado(g, "Plancha", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO, 95, 236);
    }

    private void pintarTablero(Graphics2D g) {
        boolean roto = juego.hayTareaPendiente(Lugar.TABLERO);
        g.setColor(CONTORNO);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(207, 160, 207, 184));
        forma(g, new RoundRectangle2D.Double(186, 104, 42, 56, 6, 6), new Color(0x5B6E66));
        forma(g, new Rectangle2D.Double(192, 110, 30, 30), new Color(0x2C3A35));
        for (int i = 0; i < 3; i++) {   // fusibles (uno quemado cuando está roto)
            g.setColor(roto && i == 1 ? new Color(0x2B2522) : new Color(0xD9A441));
            g.fill(new RoundRectangle2D.Double(196 + i * 9, 116, 6, 18, 3, 3));
        }
        boolean luz = !roto || cuadro % 40 < 20;
        g.setColor(roto ? (luz ? Tema.PELIGRO : new Color(0x4A2A25)) : Tema.BIEN);
        g.fill(new Ellipse2D.Double(203, 145, 8, 8));
        if (roto && cuadro % 110 < 10) {   // chispas
            Path2D chispa = new Path2D.Double();
            chispa.moveTo(228, 122); chispa.lineTo(238, 116); chispa.lineTo(234, 124); chispa.lineTo(246, 118);
            g.setColor(new Color(0xFFE27A));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(chispa);
        }
        textoCentrado(g, "Tablero", Tema.CUERPO.deriveFont(Font.BOLD, 12f), Tema.TEXTO_SUAVE, 207, 98);
    }

    private void pintarHeladera(Graphics2D g) {
        boolean desordenada = juego.hayTareaPendiente(Lugar.HELADERA);
        if (desordenada) {
            // abierta: se ve el interior con luz fría y la puerta ladeada
            forma(g, new RoundRectangle2D.Double(768, 98, 82, 148, 10, 10), new Color(0x10201F));
            g.setPaint(new GradientPaint(0, 98, new Color(0x3A5E6B), 0, 246, new Color(0x10201F)));
            g.fill(new RoundRectangle2D.Double(773, 103, 72, 138, 6, 6));
            g.setColor(new Color(0x8DB7C4));
            for (int y : new int[]{140, 180, 214}) {
                g.fill(new Rectangle2D.Double(774, y, 70, 3));
            }
            Path2D puerta = new Path2D.Double();
            puerta.moveTo(768, 98); puerta.lineTo(746, 110); puerta.lineTo(746, 236); puerta.lineTo(768, 246); puerta.closePath();
            forma(g, puerta, new Color(0xA7B2AE));
            botella(g, 790, 258, -1.4, new Color(0x7FB58A));
            botella(g, 818, 264, 1.2, new Color(0xD9A441));
            botella(g, 840, 257, -0.5, new Color(0xC4513F));
        } else {
            forma(g, new RoundRectangle2D.Double(768, 98, 82, 148, 10, 10), new Color(0xB8C2BE));
            g.setColor(CONTORNO);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Line2D.Double(768, 150, 850, 150));
            forma(g, new RoundRectangle2D.Double(776, 112, 6, 30, 3, 3), new Color(0x4D5A55));
            forma(g, new RoundRectangle2D.Double(776, 160, 6, 44, 3, 3), new Color(0x4D5A55));
            forma(g, new Rectangle2D.Double(800, 170, 12, 12), new Color(0xD9A441));   // imanes
            forma(g, new Rectangle2D.Double(820, 186, 12, 12), new Color(0xC4513F));
            g.setColor(new Color(255, 255, 255, 90));
            g.fill(new RoundRectangle2D.Double(838, 106, 5, 130, 4, 4));
        }
        textoCentrado(g, "Heladera", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, 809, 92);
    }

    private void botella(Graphics2D g, double x, double y, double ang, Color color) {
        Graphics2D b = (Graphics2D) g.create();
        b.translate(x, y);
        b.rotate(ang);
        forma(b, new RoundRectangle2D.Double(-5, -13, 10, 26, 5, 5), color);
        forma(b, new Rectangle2D.Double(-2.5, -18, 5, 6), new Color(0xEDEBDD));
        b.dispose();
    }

    /** El "!" de cada tarea pendiente, flotando sobre su rincón. */
    private void pintarBurbujas(Graphics2D g) {
        for (Lugar l : Lugar.values()) {
            if (juego.hayTareaPendiente(l)) {
                Point2D p = BURBUJAS[l.ordinal()];
                burbujaAdmiracion(g, p.getX(), p.getY());
            }
        }
    }

    /** Estrellitas en el rincón que acabás de dejar impecable. */
    private void pintarDestellos(Graphics2D g) {
        for (Lugar l : Lugar.values()) {
            int b = brillo[l.ordinal()];
            if (b <= 0) {
                continue;
            }
            Point2D c = DESTELLOS[l.ordinal()];
            float a = b / 90f;
            for (int i = 0; i < 5; i++) {
                double ang = cuadro * 0.05 + i * 1.26;
                estrella(g, c.getX() + Math.cos(ang) * 40, c.getY() + Math.sin(ang) * 14 - (90 - b) * 0.15, 7 + i % 3, a);
            }
        }
    }

    private void burbujaAdmiracion(Graphics2D g, double x, double y) {
        double salto = Math.sin(cuadro * 0.15) * 3;
        forma(g, new RoundRectangle2D.Double(x - 14, y - 14 + salto, 28, 30, 10, 10), Color.WHITE);
        Path2D cola = new Path2D.Double();
        cola.moveTo(x - 5, y + 16 + salto); cola.lineTo(x, y + 24 + salto); cola.lineTo(x + 5, y + 16 + salto);
        g.setColor(Color.WHITE);
        g.fill(cola);
        textoCentrado(g, "!", Tema.SUBTITULO.deriveFont(24f), Tema.PELIGRO, x, y + 11 + salto);
    }

    private void estrella(Graphics2D g, double x, double y, double r, float alfa) {
        g.setColor(new Color(1f, 0.9f, 0.45f, Math.max(0f, Math.min(1f, alfa))));
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double rad = i % 2 == 0 ? r : r * 0.35, a = i * Math.PI / 4;
            if (i == 0) p.moveTo(x + Math.cos(a) * rad, y + Math.sin(a) * rad);
            else p.lineTo(x + Math.cos(a) * rad, y + Math.sin(a) * rad);
        }
        p.closePath();
        g.fill(p);
    }

    /** Oscurece y enrojece la sala a medida que baja la cordura; con poca, las luces parpadean. */
    private void pintarAmbiente(Graphics2D g) {
        int cordura = juego.getJugador().getCordura();
        float insano = 1f - Math.max(0, Math.min(100, cordura)) / 100f;
        int oscuridad = (int) ((0.30f + insano * 0.5f) * 255);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W / 2.0, H / 2.0), 560f,
                new float[]{0.4f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, Math.min(255, oscuridad))}));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(150, 20, 20, (int) (insano * 40)));
        g.fillRect(0, 0, W, H);
        boolean tableroRoto = juego.hayTareaPendiente(Lugar.TABLERO);
        if ((cordura <= 30 && cuadro % 150 < 9 && (cuadro / 2) % 2 == 0)
                || (tableroRoto && cuadro % 260 < 7 && (cuadro / 2) % 2 == 0)) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, 0, W, H);
        }
    }

    private void pintarIndicacion(Graphics2D g) {
        Font f = Tema.CUERPO.deriveFont(Font.BOLD, 15f);
        FontMetrics fm = g.getFontMetrics(f);
        boolean yaHecho = activa.lugar != null && !juego.hayTareaPendiente(activa.lugar);
        String etiqueta = yaHecho ? "Acá ya está todo hecho" : activa.etiqueta;
        int ancho = fm.stringWidth(etiqueta) + 62;
        double x = Math.max(ancho / 2.0 + 10, Math.min(W - ancho / 2.0 - 10, px)), y = py - 150;
        forma(g, new RoundRectangle2D.Double(x - ancho / 2.0, y, ancho, 34, 14, 14), new Color(14, 20, 17, 235));
        g.setColor(Tema.ACENTO);
        g.fill(new RoundRectangle2D.Double(x - ancho / 2.0 + 6, y + 6, 22, 22, 8, 8));
        textoCentrado(g, "E", f, Tema.SOBRE_ACENTO, x - ancho / 2.0 + 17, y + 23);
        g.setFont(f);
        g.setColor(Tema.TEXTO);
        g.drawString(etiqueta, (float) (x - ancho / 2.0 + 38), (float) (y + 23));
    }

    private void pintarAviso(Graphics2D g) {
        Font f = Tema.CUERPO_GRANDE;
        FontMetrics fm = g.getFontMetrics(f);
        int ancho = fm.stringWidth(aviso) + 40;
        float alfa = Math.min(1f, avisoVida / 40f);
        g.setComposite(AlphaComposite.SrcOver.derive(alfa));
        forma(g, new RoundRectangle2D.Double((W - ancho) / 2.0, H - 64, ancho, 40, 16, 16), new Color(14, 20, 17, 235));
        g.setFont(f);
        g.setColor(Tema.TEXTO);
        g.drawString(aviso, (W - ancho) / 2f + 20, H - 37);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void pintarDialogo(Graphics2D g) {
        Font fTitulo = Tema.ESCENA.deriveFont(26f), fCuerpo = Tema.CUERPO_GRANDE, fOpcion = Tema.CUERPO.deriveFont(Font.BOLD, 16f);
        int ancho = 800, margen = 24, x = (W - ancho) / 2;
        List<String> titulo = envolver(dialogoTitulo, g.getFontMetrics(fTitulo), ancho - 2 * margen);
        List<String> cuerpo = envolver(dialogoCuerpo, g.getFontMetrics(fCuerpo), ancho - 2 * margen);
        int alto = margen + titulo.size() * 34 + 8 + cuerpo.size() * 25 + 14 + opciones.length * 44 + margen - 8;
        int y = H - 20 - alto;

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, W, H);
        forma(g, new RoundRectangle2D.Double(x, y, ancho, alto, 18, 18), new Color(0x22302C));
        int cy = y + margen + 24;
        g.setFont(fTitulo);
        g.setColor(Tema.ACENTO);
        for (String l : titulo) { g.drawString(l, x + margen, cy); cy += 34; }
        g.setFont(fCuerpo);
        g.setColor(Tema.TEXTO);
        for (String l : cuerpo) { g.drawString(l, x + margen, cy); cy += 25; }

        zonasOpcion = new Rectangle[opciones.length];
        int oy = cy - 8;
        for (int i = 0; i < opciones.length; i++) {
            Rectangle r = new Rectangle(x + margen, oy + i * 44, ancho - 2 * margen, 36);
            zonasOpcion[i] = r;
            forma(g, new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12),
                    i == opcionResaltada ? new Color(0xF0BE5E) : Tema.ACENTO);
            g.setColor(Tema.SOBRE_ACENTO);
            g.fill(new RoundRectangle2D.Double(r.x + 8, r.y + 7, 22, 22, 8, 8));
            textoCentrado(g, String.valueOf(i + 1), fOpcion, Tema.ACENTO, r.x + 19, r.y + 24);
            g.setFont(fOpcion);
            g.setColor(Tema.SOBRE_ACENTO);
            g.drawString(opciones[i].texto(), r.x + 44, r.y + 24);
        }
    }

    private static List<String> envolver(String texto, FontMetrics fm, int ancho) {
        List<String> lineas = new ArrayList<>();
        for (String parrafo : texto.split("\n")) {
            StringBuilder linea = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
                if (fm.stringWidth(prueba) > ancho && linea.length() > 0) {
                    lineas.add(linea.toString());
                    linea = new StringBuilder(palabra);
                } else {
                    linea = new StringBuilder(prueba);
                }
            }
            lineas.add(linea.toString());
        }
        return lineas;
    }

    private static void textoCentrado(Graphics2D g, String t, Font f, Color c, double x, double y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(t, (float) (x - g.getFontMetrics().stringWidth(t) / 2.0), (float) y);
    }

    private static void textoConContorno(Graphics2D g, String t, Font f, Color c, double x, double y, boolean centrado) {
        g.setFont(f);
        double ox = centrado ? x - g.getFontMetrics().stringWidth(t) / 2.0 : x;
        g.setColor(CONTORNO);
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dy = -2; dy <= 2; dy += 2) {
                g.drawString(t, (float) ox + dx, (float) y + dy);
            }
        }
        g.setColor(c);
        g.drawString(t, (float) ox, (float) y);
    }

    private int opcionEn(Point p) {
        if (opciones == null) {
            return -1;
        }
        double mx = (p.x - desdeX) / escala, my = (p.y - desdeY) / escala;
        for (int i = 0; i < zonasOpcion.length; i++) {
            if (zonasOpcion[i].contains(mx, my)) {
                return i;
            }
        }
        return -1;
    }
}
