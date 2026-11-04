package ui;

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

    /** Lugares del buffet donde se puede interactuar (equivalen a las 3 opciones del menú de consola). */
    public enum Zona {
        CLIENTE("Esperar a que venga un cliente", new Rectangle(330, 250, 240, 60)),
        TAREA("Realizar tareas", new Rectangle(45, 370, 175, 135)),
        INVENTARIO("Abrir inventario", new Rectangle(700, 425, 170, 80));

        final String etiqueta;
        final Rectangle area;   // en coordenadas de los pies del personaje

        Zona(String etiqueta, Rectangle area) {
            this.etiqueta = etiqueta;
            this.area = area;
        }
    }

    public record Opcion(String texto, Runnable accion) { }

    private static final class Flotante {
        final String texto; final Color color; final double x; double y; int vida = 70;
        Flotante(String texto, Color color, double x, double y) { this.texto = texto; this.color = color; this.x = x; this.y = y; }
    }

    private enum EstadoCliente { AUSENTE, LLEGA, ESPERA, SE_VA }

    private static final int W = 900, H = 520;                                   // tamaño lógico del mundo
    private static final Rectangle CAMINABLE = new Rectangle(45, 255, 810, 240);
    private static final Rectangle CASILLEROS = new Rectangle(725, 300, 150, 125);   // sólido: no se puede atravesar
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
    private int mirada = 0, cuadro, sacudida, brillo, avisoVida;
    private boolean camina;
    private Zona activa;

    private EstadoCliente estadoCliente = EstadoCliente.AUSENTE;
    private double clienteX = PUERTA_X, pasoCliente;
    private Boolean clienteContento;          // null = neutro
    private boolean tareaPrevia;

    private String dialogoTitulo = "", dialogoCuerpo = "", aviso = "";
    private Opcion[] opciones;                // null = sin diálogo abierto
    private Rectangle[] zonasOpcion = new Rectangle[0];
    private int opcionResaltada = -1;
    private double escala = 1, desdeX, desdeY;

    public EscenaBuffet(JuegoService juego, Consumer<Zona> alInteractuar) {
        this.juego = juego;
        this.alInteractuar = alInteractuar;
        this.tareaPrevia = juego.hayTareaPendiente();
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
            alInteractuar.accept(activa);
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
        boolean pendiente = juego.hayTareaPendiente();
        if (tareaPrevia && !pendiente) {
            brillo = 90;
        }
        tareaPrevia = pendiente;
        if (brillo > 0) brillo--;
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
        if (!CASILLEROS.contains(nx, py)) px = nx;   // cada eje por separado: se puede deslizar a lo largo de la pared
        if (!CASILLEROS.contains(px, ny)) py = ny;
        if (dx != 0) {
            mirada = (int) Math.signum(dx);
        }
        paso += 0.28;
    }

    private void actualizarCliente() {
        boolean hay = juego.hayClientePendiente();
        switch (estadoCliente) {
            case AUSENTE:
                if (hay) {
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
                if (!hay) {
                    estadoCliente = EstadoCliente.SE_VA;
                }
                break;
            case ESPERA:
                if (!hay) {
                    estadoCliente = EstadoCliente.SE_VA;
                }
                break;
            case SE_VA:
                clienteX += 3;
                pasoCliente += 0.25;
                if (clienteX > PUERTA_X) {
                    estadoCliente = EstadoCliente.AUSENTE;
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
        pintarTarea(g);
        pintarCasilleros(g);
        persona(g, px, py, mirada, camina ? paso : 0, camina,
                new Color(0x3F6B5B), new Color(0xD9A441), new Color(0x3A2A1E), true, null, new Color(0xE8C39E));
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
        g.setColor(new Color(0xC4513F));
        g.fill(new Ellipse2D.Double(650, 148, 8, 8));
        forma(g, new Rectangle2D.Double(662, 170, 12, 20), new Color(0x1E2226));
        for (int x : new int[]{230, 268}) {
            forma(g, new RoundRectangle2D.Double(x, 168, 20, 20, 6, 6), new Color(0xEDEBDD));
        }
    }

    private void pintarTarea(Graphics2D g) {
        // puerta del baño en la pared izquierda
        forma(g, new RoundRectangle2D.Double(-6, 295, 42, 130, 6, 6), new Color(0x5B6E66));
        textoCentrado(g, "Baño", Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO_SUAVE, 18, 288);
        boolean pendiente = juego.hayTareaPendiente();
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
        if (pendiente) {
            burbujaAdmiracion(g, 140, 334);
        }
        if (brillo > 0) {
            float a = brillo / 90f;
            for (int i = 0; i < 5; i++) {
                double ang = cuadro * 0.05 + i * 1.26;
                estrella(g, 120 + Math.cos(ang) * 40, 432 + Math.sin(ang) * 14 - (90 - brillo) * 0.15, 7 + i % 3, a);
            }
        }
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
        if (estadoCliente == EstadoCliente.AUSENTE) {
            return;
        }
        boolean camina = estadoCliente != EstadoCliente.ESPERA;
        int dir = estadoCliente == EstadoCliente.LLEGA ? -1 : estadoCliente == EstadoCliente.SE_VA ? 1 : 0;
        persona(g, clienteX, 190, dir, pasoCliente, camina,
                new Color(0x5B7C99), null, new Color(0x6B4A2F), false, clienteContento, new Color(0xD9A87C));
        if (estadoCliente == EstadoCliente.ESPERA && opciones == null) {
            burbujaAdmiracion(g, clienteX, 72);
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
        if (cordura <= 30 && cuadro % 150 < 9 && (cuadro / 2) % 2 == 0) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, 0, W, H);
        }
    }

    private void pintarIndicacion(Graphics2D g) {
        Font f = Tema.CUERPO.deriveFont(Font.BOLD, 15f);
        FontMetrics fm = g.getFontMetrics(f);
        int ancho = fm.stringWidth(activa.etiqueta) + 62;
        double x = Math.max(ancho / 2.0 + 10, Math.min(W - ancho / 2.0 - 10, px)), y = py - 150;
        forma(g, new RoundRectangle2D.Double(x - ancho / 2.0, y, ancho, 34, 14, 14), new Color(14, 20, 17, 235));
        g.setColor(Tema.ACENTO);
        g.fill(new RoundRectangle2D.Double(x - ancho / 2.0 + 6, y + 6, 22, 22, 8, 8));
        textoCentrado(g, "E", f, Tema.SOBRE_ACENTO, x - ancho / 2.0 + 17, y + 23);
        g.setFont(f);
        g.setColor(Tema.TEXTO);
        g.drawString(activa.etiqueta, (float) (x - ancho / 2.0 + 38), (float) (y + 23));
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
