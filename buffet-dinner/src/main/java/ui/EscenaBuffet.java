package ui;

import modelo.Cliente;
import modelo.Lugar;
import modelo.Objeto;
import modelo.Sala;
import service.Catalogo;
import service.JuegoService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static ui.Dibujo.*;

/**
 * El buffet en 2D: tres salas unidas por puertas en los bordes, personajes, objetos que se llevan en las manos,
 * luces que dependen de la cordura y animaciones. Es sólo vista y entrada: las reglas viven en JuegoService.
 */
public class EscenaBuffet extends JPanel {

    /** Lugares con los que se puede interactuar (con E). Los que tienen {@code lugar} son rincones con una tarea. */
    public enum Zona {
        CLIENTE(Sala.SALON, "Atender en el mostrador", new Rectangle(330, 250, 240, 60), null),
        CASILLEROS(Sala.SALON, "Abrir los casilleros", new Rectangle(20, 255, 140, 50), null),
        TABLERO(Sala.SALON, "Revisar el tablero de luz", new Rectangle(165, 255, 100, 50), Lugar.TABLERO),
        VITRINA(Sala.SALON, "Mirar la vitrina de alfajores", new Rectangle(590, 255, 125, 50), null),
        BANO(Sala.SALON, "Revisar el baño", new Rectangle(745, 255, 110, 50), Lugar.BANO),
        MESAS(Sala.SALON, "Limpiar las mesas", new Rectangle(270, 350, 330, 115), Lugar.MESAS),
        BASURA(Sala.SALON, "Cambiar la bolsa de basura", new Rectangle(600, 425, 95, 80), Lugar.BASURA),
        HELADERA(Sala.COCINA, "Abrir la heladera", new Rectangle(50, 255, 130, 50), Lugar.HELADERA),
        CAFETERA(Sala.COCINA, "Usar la cafetera", new Rectangle(190, 255, 100, 50), Lugar.CAFETERA),
        PLANCHA(Sala.COCINA, "Usar la plancha", new Rectangle(360, 255, 200, 50), Lugar.PLANCHA),
        LIMPIEZA(Sala.ALMACEN, "Mirar el estante de limpieza", new Rectangle(100, 255, 280, 50), null),
        REPUESTOS(Sala.ALMACEN, "Mirar el estante de repuestos", new Rectangle(440, 255, 340, 50), null);

        final Sala sala;
        final String etiqueta;
        final Rectangle area;   // en coordenadas de los pies del personaje
        final Lugar lugar;      // null = no es una tarea

        Zona(Sala sala, String etiqueta, Rectangle area, Lugar lugar) {
            this.sala = sala;
            this.etiqueta = etiqueta;
            this.area = area;
            this.lugar = lugar;
        }
    }

    public record Opcion(String texto, Runnable accion) { }

    private record Puerta(Sala desde, Rectangle area, Sala hacia, double x, double y, int mira) { }

    private static final class Flotante {
        final String texto; final Color color; final double x; double y; int vida = 80;
        Flotante(String texto, Color color, double x, double y) { this.texto = texto; this.color = color; this.x = x; this.y = y; }
    }

    private enum EstadoCliente { AUSENTE, LLEGA, ESPERA, SE_VA }

    private static final int W = Salas.W, H = Salas.H;
    private static final Rectangle CAMINABLE = new Rectangle(30, 255, 840, 240);
    private static final double VELOCIDAD = 3.4, PASO_FIJO = 1.0 / 60;
    private static final int MOSTRADOR_X = 450, PUERTA_X = 930;

    private static final Rectangle MESA_1 = new Rectangle(290, 398, 120, 30), MESA_2 = new Rectangle(490, 398, 120, 30);
    private static final Rectangle TACHO = new Rectangle(628, 452, 44, 30);
    private static final Rectangle ISLA = new Rectangle(320, 398, 260, 34);
    private static final Rectangle CAJAS = new Rectangle(742, 400, 106, 32);
    private static final Rectangle BARRIL = new Rectangle(610, 418, 50, 26);

    private static final List<Puerta> PUERTAS = List.of(
            new Puerta(Sala.SALON, new Rectangle(0, 262, 46, 238), Sala.COCINA, 810, 380, -1),
            new Puerta(Sala.SALON, new Rectangle(854, 262, 46, 238), Sala.ALMACEN, 90, 380, 1),
            new Puerta(Sala.COCINA, new Rectangle(854, 262, 46, 238), Sala.SALON, 90, 380, 1),
            new Puerta(Sala.ALMACEN, new Rectangle(0, 262, 46, 238), Sala.SALON, 810, 380, -1));

    private static final Point2D[] BURBUJAS = new Point2D[Lugar.values().length];
    private static final Point2D[] DESTELLOS = new Point2D[Lugar.values().length];
    static {
        put(Lugar.BANO, 805, 78, 805, 262);
        put(Lugar.MESAS, 450, 352, 450, 408);
        put(Lugar.BASURA, 650, 392, 650, 452);
        put(Lugar.TABLERO, 207, 74, 207, 135);
        put(Lugar.PLANCHA, 465, 134, 465, 195);
        put(Lugar.CAFETERA, 244, 94, 244, 170);
        put(Lugar.HELADERA, 114, 56, 114, 170);
    }

    private static void put(Lugar l, double bx, double by, double dx, double dy) {
        BURBUJAS[l.ordinal()] = new Point2D.Double(bx, by);
        DESTELLOS[l.ordinal()] = new Point2D.Double(dx, dy);
    }

    private final JuegoService juego;
    private final Consumer<Zona> alInteractuar;
    private final Consumer<String> alEvento;
    private final javax.swing.Timer reloj = new javax.swing.Timer(16, e -> tick());
    private final Set<Integer> teclas = new HashSet<>();
    private final List<Flotante> flotantes = new ArrayList<>();
    private final Map<Sala, BufferedImage> fondos = new EnumMap<>(Sala.class);
    private final Map<Sala, BufferedImage> frentes = new EnumMap<>(Sala.class);
    private final BufferedImage luzImg = lienzo(W / 2, H / 2, true);
    private final BufferedImage grano = crearGrano();
    private final double[][] polvo = new double[34][4];

    private long ultimoTick = System.nanoTime();
    private double acumulado;

    private Sala sala = Sala.SALON;
    private double px = 450, py = 340, paso;
    private int mirada = 1, cuadro, sacudida, avisoVida, bannerVida;
    private String bannerTexto = "";
    private boolean camina;
    private Zona activa;

    // transición entre salas / días
    private int transFase, transT;
    private String transTitulo;
    private Runnable transAccion;

    // trabajo con barra de progreso (preparar algo, agarrar una herramienta, arreglar)
    private double trabajoRestante, trabajoTotal;
    private String trabajoEtiqueta = "";
    private Runnable trabajoFin;

    private EstadoCliente estadoCliente = EstadoCliente.AUSENTE;
    private Cliente clienteEnEscena;
    private double clienteX = PUERTA_X, pasoCliente;
    private Boolean clienteContento;
    private final boolean[] pendiente = new boolean[Lugar.values().length];
    private final int[] brillo = new int[Lugar.values().length];

    private String dialogoTitulo = "", dialogoCuerpo = "", aviso = "";
    private Opcion[] opciones;
    private Rectangle[] zonasOpcion = new Rectangle[0];
    private int opcionResaltada = -1;
    private double escala = 1, desdeX, desdeY;

    public EscenaBuffet(JuegoService juego, Consumer<Zona> alInteractuar, Consumer<String> alEvento) {
        this.juego = juego;
        this.alInteractuar = alInteractuar;
        this.alEvento = alEvento;
        Random r = new Random(3);
        for (double[] p : polvo) {
            p[0] = r.nextDouble() * W; p[1] = r.nextDouble() * H; p[2] = (r.nextDouble() - 0.5) * 0.25; p[3] = r.nextDouble() * 6.28;
        }
        resetEstado();
        setBackground(Tema.FONDO);
        setFocusable(true);
        setPreferredSize(new Dimension(W, H));

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { teclaPresionada(e.getKeyCode()); }
            @Override public void keyReleased(KeyEvent e) { teclas.remove(e.getKeyCode()); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { teclas.clear(); }
        });
        MouseAdapter raton = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { opcionResaltada = opcionEn(e.getPoint()); }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int i = opcionEn(e.getPoint());
                if (i >= 0) opciones[i].accion().run();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
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

    private static BufferedImage crearGrano() {
        BufferedImage img = lienzo(128, 128, true);
        Random r = new Random(11);
        for (int x = 0; x < 128; x++) for (int y = 0; y < 128; y++) {
            int a = r.nextInt(26);
            img.setRGB(x, y, (a << 24) | (r.nextBoolean() ? 0xFFFFFF : 0x000000));
        }
        return img;
    }

    /** Vuelve todo al estado de "empieza el día": salón, personaje al centro, sin cliente. */
    private void resetEstado() {
        sala = Sala.SALON;
        px = 450; py = 340; mirada = 1;
        estadoCliente = EstadoCliente.AUSENTE;
        clienteEnEscena = null;
        clienteContento = null;
        trabajoFin = null; trabajoRestante = 0;
        flotantes.clear();
        for (Lugar l : Lugar.values()) {
            pendiente[l.ordinal()] = juego.hayTareaPendiente(l);
            brillo[l.ordinal()] = 0;
        }
    }

    // ---------- API para PantallaJuego ----------

    public void mostrarDialogo(String titulo, String cuerpo, Opcion... nuevas) {
        dialogoTitulo = titulo; dialogoCuerpo = cuerpo; opciones = nuevas; opcionResaltada = -1; teclas.clear();
    }

    public void cerrarDialogo() { opciones = null; }

    public void aviso(String mensaje) { aviso = mensaje; avisoVida = 230; }

    public void reaccionCliente(boolean contento) { clienteContento = contento; }

    public void efectoCordura(int delta) {
        flotantes.add(new Flotante("Cordura " + (delta > 0 ? "+" : "") + delta, delta > 0 ? Tema.BIEN : Tema.PELIGRO,
                Math.min(Math.max(px, 90), W - 90), py - 130));
        if (delta < 0) sacudida = 20;
    }

    /** Barra de progreso sobre el personaje; al terminar se ejecuta {@code alTerminar}. */
    public void trabajar(String etiqueta, double segundos, Runnable alTerminar) {
        trabajoEtiqueta = etiqueta; trabajoTotal = segundos; trabajoRestante = segundos; trabajoFin = alTerminar; teclas.clear();
    }

    /** Fundido a negro, título de pantalla (por ejemplo "DÍA 2") y vuelta al salón con todo reiniciado. */
    public void nuevoDia(String titulo) {
        iniciarTransicion(titulo, this::resetEstado);
    }

    public boolean ocupada() { return trabajoFin != null || transFase != 0; }

    private void iniciarTransicion(String titulo, Runnable alCambiar) {
        transFase = 1; transT = 0; transTitulo = titulo; transAccion = alCambiar; teclas.clear();
    }

    // ---------- entrada ----------

    private void teclaPresionada(int k) {
        if (opciones != null) {
            int i = k - KeyEvent.VK_1;
            if (i >= 0 && i < opciones.length) opciones[i].accion().run();
            return;
        }
        if (ocupada()) return;
        boolean nueva = teclas.add(k);
        if (k == KeyEvent.VK_E && nueva && activa != null) {
            boolean enCamino = activa == Zona.CLIENTE && juego.hayClientePendiente()
                    && (estadoCliente == EstadoCliente.AUSENTE || estadoCliente == EstadoCliente.SE_VA);
            if (enCamino) aviso("Ya viene el próximo cliente...");
            else alInteractuar.accept(activa);
        }
    }

    private boolean pulsada(int... codigos) {
        for (int c : codigos) if (teclas.contains(c)) return true;
        return false;
    }

    // ---------- lógica ----------

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
        actualizarTransicion();
        boolean libre = opciones == null && transFase == 0;
        if (libre && trabajoFin == null) mover(); else camina = false;
        if (libre && trabajoFin != null) {
            trabajoRestante -= PASO_FIJO;
            if (trabajoRestante <= 0) {
                Runnable fin = trabajoFin;
                trabajoFin = null;
                fin.run();
            }
        }
        activa = null;
        if (libre && trabajoFin == null) {
            for (Zona z : Zona.values()) if (z.sala == sala && z.area.contains(px, py)) activa = z;
            comprobarPuertas();
        }
        actualizarCliente();
        if (libre && alEvento != null) {
            String ev = juego.pasarTiempo(PASO_FIJO);
            if (ev != null) alEvento.accept(ev);
        }
        for (Lugar l : Lugar.values()) {
            int i = l.ordinal();
            boolean hay = juego.hayTareaPendiente(l);
            if (pendiente[i] && !hay) brillo[i] = 90;
            pendiente[i] = hay;
            if (brillo[i] > 0) brillo[i]--;
        }
        if (sacudida > 0) sacudida--;
        if (avisoVida > 0) avisoVida--;
        if (bannerVida > 0) bannerVida--;
        flotantes.removeIf(f -> --f.vida <= 0);
        flotantes.forEach(f -> f.y -= 0.6);
        for (double[] p : polvo) {
            p[3] += 0.02;
            p[0] += p[2] + Math.sin(p[3]) * 0.12;
            p[1] -= 0.06;
            if (p[1] < 0) p[1] = H;
            if (p[0] < 0) p[0] = W;
            if (p[0] > W) p[0] = 0;
        }
    }

    private void actualizarTransicion() {
        if (transFase == 0) return;
        transT++;
        if (transFase == 1 && transT >= 22) {
            if (transAccion != null) transAccion.run();
            transFase = 2; transT = 0;
        } else if (transFase == 2 && transT >= (transTitulo != null ? 80 : 3)) {
            transFase = 3; transT = 0;
        } else if (transFase == 3 && transT >= 22) {
            transFase = 0; transTitulo = null;
        }
    }

    private void comprobarPuertas() {
        for (Puerta p : PUERTAS) {
            if (p.desde() == sala && p.area().contains(px, py)) {
                iniciarTransicion(null, () -> {
                    sala = p.hacia();
                    px = p.x(); py = p.y(); mirada = p.mira();
                    bannerTexto = sala.getNombre(); bannerVida = 140;
                });
                return;
            }
        }
    }

    private void mover() {
        double dx = 0, dy = 0;
        if (pulsada(KeyEvent.VK_A, KeyEvent.VK_LEFT)) dx -= 1;
        if (pulsada(KeyEvent.VK_D, KeyEvent.VK_RIGHT)) dx += 1;
        if (pulsada(KeyEvent.VK_W, KeyEvent.VK_UP)) dy -= 1;
        if (pulsada(KeyEvent.VK_S, KeyEvent.VK_DOWN)) dy += 1;
        camina = dx != 0 || dy != 0;
        if (!camina) return;
        double largo = Math.hypot(dx, dy);
        double nx = Math.max(CAMINABLE.x, Math.min(CAMINABLE.x + CAMINABLE.width, px + dx / largo * VELOCIDAD));
        double ny = Math.max(CAMINABLE.y, Math.min(CAMINABLE.y + CAMINABLE.height, py + dy / largo * VELOCIDAD));
        if (!choca(nx, py)) px = nx;   // cada eje por separado: se desliza a lo largo de los muebles
        if (!choca(px, ny)) py = ny;
        mirada = dx != 0 ? (int) Math.signum(dx) : 0;
        paso += 0.28;
    }

    private Rectangle[] solidos() {
        switch (sala) {
            case SALON: return new Rectangle[]{MESA_1, MESA_2, TACHO};
            case COCINA: return new Rectangle[]{ISLA};
            default: return new Rectangle[]{CAJAS, BARRIL};
        }
    }

    private boolean choca(double x, double y) {
        for (Rectangle r : solidos()) if (r.contains(x, y)) return true;
        return false;
    }

    private void actualizarCliente() {
        switch (estadoCliente) {
            case AUSENTE:
                Cliente proximo = juego.proximoCliente();
                if (proximo != null) {
                    clienteEnEscena = proximo; estadoCliente = EstadoCliente.LLEGA; clienteX = PUERTA_X; clienteContento = null;
                }
                break;
            case LLEGA:
                clienteX -= 2.6; pasoCliente += 0.25;
                if (clienteX <= MOSTRADOR_X) llegoAlMostrador();
                if (clienteEnEscena.isAtendido()) estadoCliente = EstadoCliente.SE_VA;
                break;
            case ESPERA:
                if (clienteEnEscena.isAtendido()) estadoCliente = EstadoCliente.SE_VA;
                break;
            case SE_VA:
                clienteX += 3; pasoCliente += 0.25;
                if (clienteX > PUERTA_X) estadoCliente = EstadoCliente.AUSENTE;
                break;
        }
        if (opciones != null && estadoCliente == EstadoCliente.LLEGA) llegoAlMostrador();
    }

    private void llegoAlMostrador() {
        clienteX = MOSTRADOR_X;
        estadoCliente = EstadoCliente.ESPERA;
        juego.clienteEnMostrador(clienteEnEscena);   // desde acá corre su paciencia
    }

    // ---------- dibujo ----------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (getWidth() == 0 || getHeight() == 0) return;
        Graphics2D g = calidad((Graphics2D) g0.create());
        escala = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        desdeX = (getWidth() - W * escala) / 2;
        desdeY = (getHeight() - H * escala) / 2;
        g.translate(desdeX, desdeY);
        g.scale(escala, escala);
        g.clip(new Rectangle(0, 0, W, H));

        Graphics2D mundo = (Graphics2D) g.create();
        if (sacudida > 0) mundo.translate(Math.sin(cuadro * 2.3) * sacudida * 0.3, Math.cos(cuadro * 1.7) * sacudida * 0.22);
        mundo.drawImage(fondos.computeIfAbsent(sala, Salas::fondo), 0, 0, W, H, null);
        pintarEstados(mundo);
        if (sala == Sala.SALON) pintarCliente(mundo);
        BufferedImage frente = frentes.computeIfAbsent(sala, s -> Optional.ofNullable(Salas.frente(s)).orElse(VACIO));
        if (frente != VACIO) mundo.drawImage(frente, 0, 0, W, H, null);
        pintarObjetosYJugador(mundo);
        pintarDestellos(mundo);
        flotantes.forEach(f -> textoSombra(mundo, f.texto, Tema.SUBTITULO.deriveFont(20f), f.color, f.x, f.y,
                Math.min(1f, f.vida / 25f)));
        pintarAlucinaciones(mundo);
        pintarLuz(mundo);
        pintarBurbujas(mundo);
        pintarBurbujaCliente(mundo);
        mundo.dispose();

        pintarHud(g);
        if (opciones != null) {
            pintarDialogo(g);
        } else {
            if (activa != null) pintarIndicacion(g);
            if (trabajoFin != null) pintarTrabajo(g);
            if (avisoVida > 0) pintarAviso(g);
        }
        pintarTransicion(g);
        g.dispose();
    }

    private static final BufferedImage VACIO = lienzo(1, 1, true);

    private void pintarEstados(Graphics2D g) {
        switch (sala) {
            case SALON: pintarEstadoSalon(g); break;
            case COCINA: pintarEstadoCocina(g); break;
            default: break;
        }
    }

    private void pintarEstadoSalon(Graphics2D g) {
        // día en la pizarra, escrito con tiza
        Font tiza = Tema.ESCENA.deriveFont(Font.BOLD, 19f);
        texto(g, "BUFFET", tiza, new Color(0xE9E4D0), 372, 40);
        texto(g, "Día " + juego.getDia(), tiza, new Color(0xE9E4D0), 532, 40);
        // tablero de luz: LED y chispas
        boolean roto = juego.hayTareaPendiente(Lugar.TABLERO);
        boolean luz = !roto || cuadro % 40 < 20;
        g.setColor(roto ? (luz ? Tema.PELIGRO : new Color(0x4A2A25)) : Tema.BIEN);
        g.fill(new Ellipse2D.Double(203, 146, 8, 8));
        if (roto) {
            g.setColor(new Color(0x2B2522));
            g.fill(new Rectangle2D.Double(205, 116, 6, 18));
        }
        if (roto && cuadro % 110 < 10) {
            Path2D chispa = new Path2D.Double();
            chispa.moveTo(228, 122); chispa.lineTo(238, 116); chispa.lineTo(234, 124); chispa.lineTo(246, 118);
            g.setColor(new Color(0xFFE27A));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(chispa);
            brillo(g, 238, 120, 30, new Color(0xFFE27A), 120);
        }
        // baño: el charco se desparrama por debajo de la puerta
        if (juego.hayTareaPendiente(Lugar.BANO)) {
            double r = 1 + Math.sin(cuadro * 0.05) * 0.03;
            suave(g, mancha(805, 246, 78 * r, 17, 800), new Color(55, 105, 125, 175));
            suave(g, mancha(795, 244, 40, 8, 801), new Color(150, 205, 220, 90));
            g.setColor(new Color(220, 245, 250, (int) (110 - (cuadro % 60) * 1.7)));
            g.setStroke(new BasicStroke(1.4f));
            double rr = 8 + (cuadro % 60) * 0.45;
            g.draw(new Ellipse2D.Double(826 - rr, 250 - rr * 0.3, rr * 2, rr * 0.6));
        }
    }

    private void pintarEstadoCocina(Graphics2D g) {
        // cafetera
        boolean tapada = juego.hayTareaPendiente(Lugar.CAFETERA);
        g.setColor(tapada ? Tema.PELIGRO : Tema.BIEN);
        g.fill(new Ellipse2D.Double(258, 137, 7, 7));
        if (tapada) {
            suave(g, mancha(246, 190, 18, 4, 810), new Color(0x2A1A12));
            double caida = (cuadro * 1.3) % 14;
            suave(g, new Ellipse2D.Double(243, 182 + caida * 0.2, 5, 7 + caida * 0.2), new Color(0x2A1A12));
        } else {
            for (int i = 0; i < 2; i++) {
                double t = (cuadro * 0.7 + i * 30) % 60;
                g.setColor(new Color(240, 240, 230, (int) (110 * (1 - t / 60))));
                g.fill(new Ellipse2D.Double(236 + i * 12 + Math.sin((cuadro + i * 20) * 0.08) * 4, 118 - t * 0.5, 9, 9));
            }
        }
        // plancha
        if (juego.hayTareaPendiente(Lugar.PLANCHA)) {
            suave(g, mancha(430, 162, 40, 7, 820), new Color(0x1E1A17));
            suave(g, mancha(510, 164, 26, 5, 821), new Color(0x1E1A17));
            for (int i = 0; i < 3; i++) {
                double t = (cuadro * 0.6 + i * 22) % 70;
                g.setColor(new Color(150, 150, 145, (int) (130 * (1 - t / 70))));
                g.fill(new Ellipse2D.Double(420 + i * 40 + Math.sin((cuadro + i * 30) * 0.06) * 7, 148 - t, 14 + t * 0.15, 14 + t * 0.15));
            }
        } else {
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(new RoundRectangle2D.Double(366, 155, 90, 3, 3, 3));
        }
        // heladera abierta, con la puerta ladeada y bebidas por el piso
        if (juego.hayTareaPendiente(Lugar.HELADERA)) {
            forma(g, organica(58, 84, 112, 160, 10, 830), new Color(0x10201F), 2.4f);
            g.setPaint(new GradientPaint(0, 86, new Color(0x4C7684), 0, 244, new Color(0x10201F)));
            g.fill(organica(64, 90, 100, 148, 6, 831));
            g.setColor(new Color(0x9CC9D6));
            for (int y : new int[]{130, 172, 208}) g.fill(new RoundRectangle2D.Double(64, y, 100, 3, 2, 2));
            Path2D puerta = new Path2D.Double();
            puerta.moveTo(58, 84); puerta.quadTo(48, 88, 34, 98); puerta.lineTo(34, 234); puerta.quadTo(48, 242, 58, 246); puerta.closePath();
            forma(g, puerta, new Color(0xA7B2AE), 2.4f);
            botella(g, 74, 262, -1.4, new Color(0x7FB58A));
            botella(g, 112, 270, 1.2, new Color(0xD9A441));
            botella(g, 150, 262, -0.5, new Color(0xC4513F));
            brillo(g, 114, 170, 70, new Color(0x9CD3E4), 60);
        }
    }

    private void botella(Graphics2D g, double x, double y, double ang, Color color) {
        Graphics2D b = (Graphics2D) g.create();
        b.translate(x, y);
        b.rotate(ang);
        forma(b, organica(-5, -13, 10, 26, 5, (long) x), color, 1.8f);
        forma(b, organica(-2.5, -18, 5, 6, 2, (long) y), new Color(0xEDEBDD), 1.4f);
        b.dispose();
    }

    private void pintarCliente(Graphics2D g) {
        if (estadoCliente == EstadoCliente.AUSENTE || clienteEnEscena == null) return;
        Figura.Aspecto a = Figura.CLIENTES[Math.floorMod(clienteEnEscena.getAspecto(), Figura.CLIENTES.length)];
        boolean camina = estadoCliente != EstadoCliente.ESPERA;
        int dir = estadoCliente == EstadoCliente.LLEGA ? -1 : estadoCliente == EstadoCliente.SE_VA ? 1 : 0;
        Figura.dibujar(g, clienteX, 190, dir, pasoCliente, camina, a, clienteContento, false, null, cuadro);
    }

    /** El pedido flota por encima de la oscuridad para que siempre se lea. */
    private void pintarBurbujaCliente(Graphics2D g) {
        if (sala == Sala.SALON && estadoCliente == EstadoCliente.ESPERA && opciones == null
                && clienteEnEscena != null && !clienteEnEscena.isAtendido()) {
            Figura.Aspecto a = Figura.CLIENTES[Math.floorMod(clienteEnEscena.getAspecto(), Figura.CLIENTES.length)];
            burbujaPedido(g, clienteX, 190 - 118 * a.escala() - 22, clienteEnEscena);
        }
    }

    private void burbujaPedido(Graphics2D g, double x, double y, Cliente c) {
        double salto = Math.sin(cuadro * 0.12) * 2;
        y += salto;
        double frac = Math.max(0, c.getPacienciaRestante() / c.getPacienciaMax());
        sombra(g, x, y + 22, 22, 5, 70);
        Path2D cola = new Path2D.Double();
        cola.moveTo(x - 6, y + 18); cola.lineTo(x, y + 28); cola.lineTo(x + 6, y + 18); cola.closePath();
        forma(g, cola, Color.WHITE, 2f);
        forma(g, new Ellipse2D.Double(x - 21, y - 21, 42, 42), new Color(0xFBF6E6), 2.4f);
        Figura.icono(g, c.getTipoPedido(), x, y, 1.05);
        Color anillo = mezcla(Tema.PELIGRO, Tema.BIEN, (float) frac);
        g.setColor(anillo);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(x - 25, y - 25, 50, 50, 90, 360 * frac, Arc2D.OPEN));
    }

    private void pintarObjetosYJugador(Graphics2D g) {
        record Pieza(double y, Runnable dibujo) { }
        List<Pieza> piezas = new ArrayList<>();
        switch (sala) {
            case SALON:
                boolean sucias = juego.hayTareaPendiente(Lugar.MESAS);
                piezas.add(new Pieza(436, () -> Salas.mesa(g, MESA_1.x, sucias, 0)));
                piezas.add(new Pieza(436, () -> Salas.mesa(g, MESA_2.x, sucias, 1)));
                piezas.add(new Pieza(486, () -> pintarTacho(g)));
                break;
            case COCINA:
                piezas.add(new Pieza(440, () -> Salas.isla(g, ISLA)));
                break;
            default:
                piezas.add(new Pieza(432, () -> Salas.cajas(g, new Rectangle(740, 360, 110, 70))));
                piezas.add(new Pieza(446, () -> Salas.barril(g, new Rectangle(610, 405, 50, 35))));
                break;
        }
        Objeto mano = juego.enMano();
        String tipo = mano == null ? null : mano.getTipoObjeto();
        double fase = trabajoFin != null ? cuadro * 0.35 : paso;
        piezas.add(new Pieza(py, () -> Figura.dibujar(g, px, py, mirada, fase, camina || trabajoFin != null, Figura.JUGADOR,
                null, true, tipo, cuadro)));
        piezas.sort(Comparator.comparingDouble(Pieza::y));
        piezas.forEach(p -> p.dibujo().run());
    }

    private void pintarTacho(Graphics2D g) {
        boolean llena = juego.hayTareaPendiente(Lugar.BASURA);
        sombra(g, 650, 486, 32, 8, 120);
        if (llena) {
            Path2D bolsa = new Path2D.Double();
            bolsa.moveTo(626, 440); bolsa.curveTo(612, 408, 640, 396, 650, 404); bolsa.curveTo(662, 396, 690, 410, 674, 440); bolsa.closePath();
            forma(g, bolsa, new Color(0x26332E), 2f);
            suave(g, new Ellipse2D.Double(634, 412, 10, 18), new Color(255, 255, 255, 40));
        }
        Path2D cuerpo = new Path2D.Double();
        cuerpo.moveTo(630, 438); cuerpo.quadTo(650, 434, 670, 438); cuerpo.lineTo(665, 484); cuerpo.quadTo(650, 488, 635, 484); cuerpo.closePath();
        forma(g, cuerpo, new Color(0x5B6E66), 2.2f);
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) g.draw(new Line2D.Double(641 + i * 8, 447, 640 + i * 9, 477));
        if (llena) {
            for (int i = 0; i < 3; i++) {
                double a = cuadro * 0.17 + i * 2.1;
                suave(g, new Ellipse2D.Double(650 + Math.cos(a) * 26 - 1.5, 408 + Math.sin(a * 1.4) * 12, 3.5, 3.5), TRAZO);
            }
        } else {
            forma(g, organica(626, 430, 48, 10, 5, 840), new Color(0x7B8E86), 1.8f);
        }
        textoIzq(g, "Basura", Tema.CUERPO.deriveFont(Font.BOLD, 12f), new Color(0xB8C5B8), 630, 504);
    }

    private void pintarBurbujas(Graphics2D g) {
        for (Lugar l : Lugar.values()) {
            if (l.getSala() == sala && juego.hayTareaPendiente(l)) {
                Point2D p = BURBUJAS[l.ordinal()];
                burbujaAdmiracion(g, p.getX(), p.getY());
            }
        }
    }

    private void burbujaAdmiracion(Graphics2D g, double x, double y) {
        double salto = Math.sin(cuadro * 0.15 + x) * 3;
        Path2D cola = new Path2D.Double();
        cola.moveTo(x - 5, y + 14 + salto); cola.lineTo(x, y + 23 + salto); cola.lineTo(x + 5, y + 14 + salto); cola.closePath();
        forma(g, cola, Color.WHITE, 2f);
        forma(g, organica(x - 14, y - 15 + salto, 28, 31, 11, (long) x), Color.WHITE, 2.4f);
        texto(g, "!", Tema.SUBTITULO.deriveFont(24f), Tema.PELIGRO, x, y + 11 + salto);
    }

    private void pintarDestellos(Graphics2D g) {
        for (Lugar l : Lugar.values()) {
            int b = brillo[l.ordinal()];
            if (b <= 0 || l.getSala() != sala) continue;
            Point2D c = DESTELLOS[l.ordinal()];
            float a = b / 90f;
            for (int i = 0; i < 6; i++) {
                double ang = cuadro * 0.05 + i * 1.05;
                estrella(g, c.getX() + Math.cos(ang) * 44, c.getY() + Math.sin(ang) * 16 - (90 - b) * 0.15, 6 + i % 3, a);
            }
        }
    }

    private void estrella(Graphics2D g, double x, double y, double r, float alfa) {
        g.setColor(new Color(1f, 0.9f, 0.45f, Math.max(0f, Math.min(1f, alfa))));
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double rad = i % 2 == 0 ? r : r * 0.32, a = i * Math.PI / 4;
            if (i == 0) p.moveTo(x + Math.cos(a) * rad, y + Math.sin(a) * rad); else p.lineTo(x + Math.cos(a) * rad, y + Math.sin(a) * rad);
        }
        p.closePath();
        g.fill(p);
    }

    /** Con la cordura baja la sala empieza a mostrar cosas que no están. */
    private void pintarAlucinaciones(Graphics2D g) {
        int cordura = juego.getJugador().getCordura();
        if (sala == Sala.SALON && cordura <= 50 && cuadro % 420 < 260) {   // algo mira desde la ventana
            g.setColor(new Color(0x05080A));
            g.fill(new Ellipse2D.Double(122, 34, 46, 52));
            suave(g, new Ellipse2D.Double(134, 49, 8, 6), new Color(0xEDEBDD));
            suave(g, new Ellipse2D.Double(150, 49, 8, 6), new Color(0xEDEBDD));
        }
        int t = cuadro % 840;
        if (cordura <= 40 && t < 260) {   // una silueta alta, quieta, que aparece y se disuelve
            float a = (float) Math.sin(Math.PI * t / 260.0) * 0.6f;
            double x = sala == Sala.SALON ? 820 : sala == Sala.COCINA ? 700 : 640, y = 300;
            g.setColor(new Color(4, 6, 8, (int) (a * 255)));
            g.fill(new Ellipse2D.Double(x - 14, y - 128, 28, 34));
            Path2D c = new Path2D.Double();
            c.moveTo(x - 18, y - 94); c.quadTo(x - 24, y - 40, x - 12, y); c.lineTo(x + 12, y); c.quadTo(x + 24, y - 40, x + 18, y - 94); c.closePath();
            g.fill(c);
            g.setColor(new Color(235, 235, 220, (int) (a * 255)));
            g.fill(new Ellipse2D.Double(x - 8, y - 116, 4, 5));
            g.fill(new Ellipse2D.Double(x + 4, y - 116, 4, 5));
        }
    }

    // ---------- luz, atmósfera ----------

    private double[][] luces() {
        switch (sala) {
            case SALON: return new double[][]{{230, 40, 250}, {450, 40, 260}, {670, 40, 250}};
            case COCINA: return new double[][]{{300, 40, 280}, {600, 40, 280}, {110, 200, 120}};
            default: return new double[][]{{410, 40, 330}};
        }
    }

    private void pintarLuz(Graphics2D g) {
        int cordura = juego.getJugador().getCordura();
        float insano = 1f - Math.max(0, Math.min(100, cordura)) / 100f;
        int oscuridad = (int) Math.min(235, 84 + insano * 130);
        boolean fallo = (cordura <= 30 && cuadro % 150 < 9 && (cuadro / 2) % 2 == 0)
                || (juego.hayTareaPendiente(Lugar.TABLERO) && cuadro % 260 < 7 && (cuadro / 2) % 2 == 0);
        double intensidad = fallo ? 0.12 : 1.0;

        Graphics2D l = luzImg.createGraphics();
        l.setComposite(AlphaComposite.Clear);
        l.fillRect(0, 0, W / 2, H / 2);
        l.setComposite(AlphaComposite.SrcOver);
        l.setColor(new Color(5, 7, 12, oscuridad));
        l.fillRect(0, 0, W / 2, H / 2);
        l.setComposite(AlphaComposite.DstOut);
        for (double[] f : luces()) {
            double r = f[2] * (1 + Math.sin(cuadro * 0.03 + f[0]) * 0.015);
            agujero(l, f[0] / 2, f[1] / 2 + 20, r / 2, (int) (235 * intensidad));
        }
        agujero(l, px / 2, (py - 50) / 2, (130 + cordura * 1.0) / 2, 190);   // el halo que acompaña al personaje
        l.dispose();
        g.drawImage(luzImg, 0, 0, W, H, null);

        g.setPaint(new RadialGradientPaint(new Point2D.Double(W / 2.0, H / 2.0), 560f, new float[]{0.45f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(150, 20, 20, (int) (insano * 55)));
        g.fillRect(0, 0, W, H);
        for (double[] p : polvo) {   // polvo flotando en la luz
            g.setColor(new Color(255, 245, 220, 25 + (int) (30 * (0.5 + 0.5 * Math.sin(p[3] * 2)))));
            g.fill(new Ellipse2D.Double(p[0], p[1], 2, 2));
        }
        g.setPaint(new TexturePaint(grano, new Rectangle2D.Double((cuadro / 3 * 37) % 128, (cuadro / 3 * 59) % 128, 128, 128)));
        g.fillRect(0, 0, W, H);
    }

    private static void agujero(Graphics2D l, double cx, double cy, double r, int alfa) {
        l.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r, new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(0, 0, 0, alfa), new Color(0, 0, 0, (int) (alfa * 0.5)), new Color(0, 0, 0, 0)}));
        l.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    // ---------- interfaz dentro del juego ----------

    private void pintarHud(Graphics2D g) {
        Objeto mano = juego.enMano();
        Shape panel = organica(14, H - 70, 214, 56, 14, 900);
        forma(g, panel, new Color(0x22302C), 2.4f);
        forma(g, organica(22, H - 62, 40, 40, 10, 901), new Color(0x2E4039), 2f);
        if (mano != null) {
            Figura.icono(g, mano.getTipoObjeto(), 42, H - 42, 1.15);
        }
        textoIzq(g, "EN LAS MANOS", Tema.CUERPO.deriveFont(Font.BOLD, 10f), Tema.TEXTO_SUAVE, 72, H - 48);
        String nombre = mano == null ? "(nada)" : mano.getNombre();
        Font f = Tema.CUERPO.deriveFont(Font.BOLD, 13f);
        g.setFont(f);
        while (g.getFontMetrics().stringWidth(nombre) > 150 && nombre.length() > 4) nombre = nombre.substring(0, nombre.length() - 2);
        textoIzq(g, nombre, f, mano == null ? Tema.TEXTO_SUAVE : Tema.TEXTO, 72, H - 28);

        if (bannerVida > 0 && transFase == 0) {
            float a = Math.min(1f, bannerVida / 30f);
            Composite viejo = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(a));
            texto(g, bannerTexto, Tema.ESCENA.deriveFont(Font.BOLD, 30f), new Color(0xF0E6C8), W / 2.0, H - 96);
            g.setComposite(viejo);
        }
    }

    private void pintarTransicion(Graphics2D g) {
        if (transFase == 0) return;
        float a = transFase == 1 ? transT / 22f : transFase == 2 ? 1f : 1f - transT / 22f;
        g.setColor(new Color(3, 4, 6, (int) (255 * Math.max(0, Math.min(1, a)))));
        g.fillRect(-4, -4, W + 8, H + 8);
        if (transTitulo != null && transFase != 1) {
            float t = transFase == 2 ? Math.min(1f, transT / 20f) : a;
            Composite viejo = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, t))));
            texto(g, transTitulo, Tema.TITULO.deriveFont(54f), new Color(0xE9D9A8), W / 2.0, H / 2.0 + 14);
            g.setComposite(viejo);
        }
    }

    private void textoSombra(Graphics2D g, String t, Font f, Color c, double x, double y, float a) {
        Composite viejo = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, a))));
        texto(g, t, f, c, x, y);
        g.setComposite(viejo);
    }

    private void pintarIndicacion(Graphics2D g) {
        Font f = Tema.CUERPO.deriveFont(Font.BOLD, 15f);
        FontMetrics fm = g.getFontMetrics(f);
        boolean yaHecho = activa.lugar != null && !juego.hayTareaPendiente(activa.lugar)
                && activa != Zona.CAFETERA && activa != Zona.PLANCHA && activa != Zona.HELADERA;
        String etiqueta = yaHecho ? "Ya está todo en orden" : activa.etiqueta;
        int ancho = fm.stringWidth(etiqueta) + 62;
        double x = Math.max(ancho / 2.0 + 10, Math.min(W - ancho / 2.0 - 10, px)), y = Math.max(8, py - 160);
        forma(g, organica(x - ancho / 2.0, y, ancho, 34, 14, 910), new Color(0x151D1A), 2.2f);
        forma(g, organica(x - ancho / 2.0 + 6, y + 6, 22, 22, 7, 911), Tema.ACENTO, 1.8f);
        texto(g, "E", f, Tema.SOBRE_ACENTO, x - ancho / 2.0 + 17, y + 23);
        textoIzq(g, etiqueta, f, Tema.TEXTO, x - ancho / 2.0 + 38, y + 23);
    }

    private void pintarTrabajo(Graphics2D g) {
        double frac = 1 - trabajoRestante / Math.max(0.01, trabajoTotal);
        double x = Math.max(70, Math.min(W - 70, px)), y = py - 150;
        forma(g, organica(x - 62, y, 124, 40, 12, 920), new Color(0x151D1A), 2.2f);
        g.setColor(new Color(0x2E4039));
        g.fill(organica(x - 54, y + 24, 108, 8, 4, 921));
        g.setColor(Tema.ACENTO);
        g.fill(organica(x - 54, y + 24, Math.max(6, 108 * frac), 8, 4, 922));
        texto(g, trabajoEtiqueta, Tema.CUERPO.deriveFont(Font.BOLD, 13f), Tema.TEXTO, x, y + 18);
    }

    private void pintarAviso(Graphics2D g) {
        Font f = Tema.CUERPO_GRANDE;
        FontMetrics fm = g.getFontMetrics(f);
        List<String> lineas = envolver(aviso, fm, 640);
        int ancho = 0;
        for (String l : lineas) ancho = Math.max(ancho, fm.stringWidth(l));
        ancho += 44;
        int alto = 18 + lineas.size() * 24;
        float alfa = Math.min(1f, avisoVida / 40f);
        Composite viejo = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alfa));
        double y0 = H - 82 - alto + 24;
        forma(g, organica((W - ancho) / 2.0, y0, ancho, alto, 16, 930), new Color(0x151D1A), 2.2f);
        for (int i = 0; i < lineas.size(); i++) textoIzq(g, lineas.get(i), f, Tema.TEXTO, (W - ancho) / 2.0 + 22, y0 + 26 + i * 24);
        g.setComposite(viejo);
    }

    private void pintarDialogo(Graphics2D g) {
        Font fTitulo = Tema.ESCENA.deriveFont(26f), fCuerpo = Tema.CUERPO_GRANDE, fOpcion = Tema.CUERPO.deriveFont(Font.BOLD, 16f);
        int ancho = 800, margen = 26, x = (W - ancho) / 2;
        List<String> titulo = envolver(dialogoTitulo, g.getFontMetrics(fTitulo), ancho - 2 * margen);
        List<String> cuerpo = envolver(dialogoCuerpo, g.getFontMetrics(fCuerpo), ancho - 2 * margen);
        int alto = margen + titulo.size() * 34 + 8 + cuerpo.size() * 25 + 14 + opciones.length * 44 + margen - 8;
        int y = H - 20 - alto;

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(0, 0, W, H);
        sombra(g, W / 2.0, y + alto + 6, ancho / 2.0, 14, 120);
        forma(g, organica(x, y, ancho, alto, 20, 940), new Color(0x2A3A34), 3f);
        g.setColor(new Color(0xB98A3A));   // filete interior
        g.setStroke(new BasicStroke(1.6f));
        g.draw(organica(x + 6, y + 6, ancho - 12, alto - 12, 16, 941));
        int cy = y + margen + 24;
        for (String l : titulo) { textoIzq(g, l, fTitulo, Tema.ACENTO, x + margen, cy); cy += 34; }
        for (String l : cuerpo) { textoIzq(g, l, fCuerpo, Tema.TEXTO, x + margen, cy); cy += 25; }

        zonasOpcion = new Rectangle[opciones.length];
        int oy = cy - 8;
        for (int i = 0; i < opciones.length; i++) {
            Rectangle r = new Rectangle(x + margen, oy + i * 44, ancho - 2 * margen, 36);
            zonasOpcion[i] = r;
            forma(g, organica(r.x, r.y, r.width, r.height, 13, 950 + i), i == opcionResaltada ? new Color(0xF0BE5E) : Tema.ACENTO, 2.2f);
            forma(g, new Ellipse2D.Double(r.x + 7, r.y + 7, 22, 22), Tema.SOBRE_ACENTO, 1.6f);
            texto(g, String.valueOf(i + 1), fOpcion, Tema.ACENTO, r.x + 18, r.y + 23);
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

    private int opcionEn(Point p) {
        if (opciones == null) return -1;
        double mx = (p.x - desdeX) / escala, my = (p.y - desdeY) / escala;
        for (int i = 0; i < zonasOpcion.length; i++) if (zonasOpcion[i].contains(mx, my)) return i;
        return -1;
    }
}
