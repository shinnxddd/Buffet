package ui;

import modelo.Cliente;
import modelo.Herramienta;
import modelo.Lugar;
import modelo.Objeto;
import modelo.Sala;
import modelo.Tarea;
import service.JuegoService;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * El buffet dibujado en pixel art sobre un lienzo interno de 384x240. Se
 * encarga del movimiento,
 * las puertas, las partículas y la iluminación. La interfaz y los diálogos
 * viven en PantallaJuego.
 */
public class EscenaBuffet {

    public record Zona(String id, String etiqueta, Sala sala, Rectangle area) {
    }

    public static final int ANCHO = 384;
    public static final int ALTO = 240;

    private static final int HORIZONTE = 84;
    /**
     * Caminando se llega cómodo a cualquier sala; con Shift se cruza el buffet
     * corriendo.
     */
    private static final double VELOCIDAD = 1.7;
    private static final double VELOCIDAD_CORRER = 2.9;
    /** La carrera gasta aguante; sin aguante se vuelve a caminar. */
    private static final double STAMINA_MAXIMA = 100;
    private static final double GASTE_STAMINA = 0.55;
    private static final double REGENERACION_STAMINA = 0.10;
    private static final int ESPERA_STAMINA = 45;
    private static final double DISTANCIA = 28;
    /** Los pies pueden llegar hasta el borde inferior del comedor. */
    private static final int PISO_MAXIMO = ALTO - 3;

    private static final int SOMBRAS_APAGON = 3;
    private static final double VELOCIDAD_SOMBRA = 0.38;
    private static final double DISTANCIA_GOLPE = 11;
    private static final int ESPERA_GOLPE = 90;

    private static final Color PARED = new Color(0x22352D);
    private static final Color PARED_CLARA = new Color(0x2C4339);
    private static final Color PARED_OSCURA = new Color(0x1A2923);
    private static final Color ZOCALO = new Color(0x4A3524);
    private static final Color ZOCALO_CLARO = new Color(0x6B4E32);
    private static final Color PISO_A = new Color(0x3A3B33);
    private static final Color PISO_B = new Color(0x2F3029);
    private static final Color MADERA = new Color(0x6B4E32);
    private static final Color MADERA_CLARA = new Color(0x8A6844);
    private static final Color MADERA_OSCURA = new Color(0x3E2C1D);
    private static final Color METAL = new Color(0x7C8A85);
    private static final Color METAL_CLARO = new Color(0xABB7B0);
    private static final Color METAL_OSCURO = new Color(0x4C5953);
    private static final Color CREMA = new Color(0xE8E4D4);
    private static final Color MOSTAZA = new Color(0xD9A441);
    private static final Color ROJO = new Color(0x8E2F2A);
    private static final Color ROJO_CLARO = new Color(0xB84A3C);
    private static final Color PIEL = new Color(0xC99A6B);
    private static final Color PIEL_OSCURA = new Color(0x8A6246);
    private static final Color NEGRO = new Color(0x070A09);

    private static final Map<Sala, List<Zona>> ZONAS = new EnumMap<>(Sala.class);
    private static final Map<Sala, List<Rectangle>> OBSTACULOS = new EnumMap<>(Sala.class);

    static {
        ZONAS.put(Sala.COMEDOR, List.of(
                new Zona("cliente", "Atender el mostrador", Sala.COMEDOR, new Rectangle(136, 140, 116, 34)),
                new Zona("tarea:MESAS", "Limpiar las mesas", Sala.COMEDOR, new Rectangle(48, 140, 116, 62)),
                new Zona("tarea:BASURA", "Sacar la basura", Sala.COMEDOR, new Rectangle(292, 148, 56, 50)),
                new Zona("tarea:TABLERO", "Revisar el tablero", Sala.COMEDOR, new Rectangle(18, 70, 44, 46))));

        ZONAS.put(Sala.BANO, List.of(
                new Zona("tarea:BANO", "Trapear el baño", Sala.BANO, new Rectangle(126, 130, 130, 76))));

        // Cada estación es la única salida del pedido que le corresponde: primero se
        // repara
        // si está rota y después se retira la bandeja directamente de ella.
        ZONAS.put(Sala.COCINA, List.of(
                new Zona("estacion:PLANCHA", "Plancha", Sala.COCINA, new Rectangle(24, 98, 90, 38)),
                new Zona("estacion:CAFETERA", "Cafetera", Sala.COCINA, new Rectangle(142, 64, 58, 38)),
                new Zona("estacion:HELADERA", "Heladera", Sala.COCINA, new Rectangle(324, 58, 52, 90))));

        ZONAS.put(Sala.ALMACEN, List.of(
                new Zona("herramienta:BANO", "Agarrar el trapeador", Sala.ALMACEN, new Rectangle(30, 50, 58, 50)),
                new Zona("herramienta:MESAS", "Agarrar la esponja", Sala.ALMACEN, new Rectangle(102, 50, 58, 50)),
                new Zona("herramienta:BASURA", "Agarrar la escoba", Sala.ALMACEN, new Rectangle(174, 50, 58, 50)),
                new Zona("herramienta:TABLERO", "Agarrar el fusible", Sala.ALMACEN, new Rectangle(246, 50, 58, 50)),
                new Zona("herramienta:PLANCHA", "Agarrar la espátula", Sala.ALMACEN, new Rectangle(30, 124, 58, 50)),
                new Zona("herramienta:CAFETERA", "Agarrar la llave inglesa", Sala.ALMACEN,
                        new Rectangle(102, 124, 58, 50)),
                new Zona("herramienta:HELADERA", "Agarrar los guantes", Sala.ALMACEN, new Rectangle(174, 124, 58, 50)),
                new Zona("npc", "Hablar con el conserje", Sala.ALMACEN, new Rectangle(304, 178, 64, 36))));

        // Huellas de los muebles: el jugador no puede atravesarlos. Son rectángulos
        // algo más chicos que el dibujo para que siempre quede un pasillo claro.
        OBSTACULOS.put(Sala.COMEDOR, List.of(
                new Rectangle(124, 120, 136, 38), // mostrador
                new Rectangle(76, 146, 32, 22), // mesa grande
                new Rectangle(134, 178, 32, 24), // mesa chica
                new Rectangle(306, 148, 22, 26))); // basura

        OBSTACULOS.put(Sala.BANO, List.of(
                new Rectangle(20, 42, 168, 58), // cabinas hasta el piso
                new Rectangle(220, 68, 82, 22))); // lavatorios

        OBSTACULOS.put(Sala.COCINA, List.of(
                new Rectangle(44, 62, 92, 34), // estufa
                new Rectangle(24, 104, 86, 16), // plancha
                new Rectangle(150, 80, 44, 20), // cafetera
                new Rectangle(228, 66, 92, 32), // mesada de preparación
                new Rectangle(324, 60, 48, 90))); // heladera

        OBSTACULOS.put(Sala.ALMACEN, List.of(
                new Rectangle(28, 48, 62, 50), new Rectangle(100, 48, 62, 50),
                new Rectangle(172, 48, 62, 50), new Rectangle(244, 48, 62, 50),
                new Rectangle(28, 122, 62, 52), new Rectangle(100, 122, 62, 52),
                new Rectangle(172, 122, 62, 52),
                new Rectangle(242, 124, 34, 24), new Rectangle(290, 126, 24, 18),
                new Rectangle(314, 122, 26, 18),
                new Rectangle(324, 190, 16, 14))); // conserje
    }

    private final JuegoService juego;
    private final Random azar = new Random(13);
    private final List<Particula> particulas = new ArrayList<>();
    private final List<Sombra> sombras = new ArrayList<>();

    private Sala sala;
    private double x;
    private double y;
    private int mirada = 1;
    private double animacion;
    private boolean caminando;
    private boolean corriendo;
    private double stamina = STAMINA_MAXIMA;
    private boolean agotado;
    private int esperaStamina;
    private int tick;
    private double fundido = 1;
    private int espera = 30;
    private int esperaGolpe;
    private double golpeFlash;
    private int golpes;
    private BufferedImage mascaraLuz;
    private int[] pixelesLuz;

    public EscenaBuffet(JuegoService juego) {
        this.juego = juego;
        this.sala = juego.getSala();
        ubicarEntrada(sala, null);
        for (int i = 0; i < 46; i++) {
            particulas.add(nuevaParticula());
        }
    }

    // ------------------------------------------------------------------ API

    public Sala getSala() {
        return sala;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setSala(Sala nueva) {
        if (nueva == null) {
            return;
        }
        this.sala = nueva;
        juego.setSala(nueva);
        ubicarEntrada(nueva, null);
        fundido = 1;
        espera = 30;
        reiniciarSombras();
    }

    public void reiniciarPosicion() {
        ubicarEntrada(sala, null);
        fundido = 1;
        espera = 30;
        stamina = STAMINA_MAXIMA;
        agotado = false;
        esperaStamina = 0;
        reiniciarSombras();
    }

    /** Las sombras no cruzan puertas con vos: te esperan del otro lado. */
    private void reiniciarSombras() {
        sombras.clear();
        esperaGolpe = 50;
        golpeFlash = 0;
    }

    public void actualizar(Set<Integer> teclas, boolean bloqueado) {
        tick++;
        if (fundido > 0) {
            fundido = Math.max(0, fundido - 0.07);
        }
        if (espera > 0) {
            espera--;
        }

        double dx = 0;
        double dy = 0;
        if (!bloqueado && espera < 20) {
            if (teclas.contains(java.awt.event.KeyEvent.VK_A)
                    || teclas.contains(java.awt.event.KeyEvent.VK_LEFT))
                dx--;
            if (teclas.contains(java.awt.event.KeyEvent.VK_D)
                    || teclas.contains(java.awt.event.KeyEvent.VK_RIGHT))
                dx++;
            if (teclas.contains(java.awt.event.KeyEvent.VK_W)
                    || teclas.contains(java.awt.event.KeyEvent.VK_UP))
                dy--;
            if (teclas.contains(java.awt.event.KeyEvent.VK_S)
                    || teclas.contains(java.awt.event.KeyEvent.VK_DOWN))
                dy++;
        }

        caminando = dx != 0 || dy != 0;
        boolean quiereCorrer = caminando && teclas.contains(java.awt.event.KeyEvent.VK_SHIFT);
        if (!quiereCorrer) {
            agotado = false;
        }
        corriendo = quiereCorrer && !agotado && stamina > 0;
        if (corriendo) {
            stamina = Math.max(0, stamina - GASTE_STAMINA);
            if (stamina <= 0) {
                // Se quedó sin aguante: vuelve a caminar y espera un momento antes de
                // recuperar.
                agotado = true;
                corriendo = false;
                esperaStamina = ESPERA_STAMINA;
            }
        } else if (esperaStamina > 0) {
            esperaStamina--;
        } else {
            stamina = Math.min(STAMINA_MAXIMA, stamina + REGENERACION_STAMINA);
        }
        if (caminando) {
            double largo = Math.hypot(dx, dy);
            dx /= largo;
            dy /= largo;
            if (dx != 0) {
                mirada = dx > 0 ? 1 : -1;
            }
            double velocidad = corriendo ? VELOCIDAD_CORRER : VELOCIDAD;
            int yMinimo = pisoMinimo();
            double nx = Math.max(14, Math.min(ANCHO - 14, x + dx * velocidad));
            double ny = Math.max(yMinimo, Math.min(PISO_MAXIMO, y + dy * velocidad));
            // Resolvemos cada eje por separado para poder deslizarse contra los muebles.
            if (!bloquea(nx, y)) {
                x = nx;
            }
            if (!bloquea(x, ny)) {
                y = ny;
            }
            animacion += corriendo ? 0.30 : 0.18;
        }

        int yMinimo = pisoMinimo();
        x = Math.max(14, Math.min(ANCHO - 14, x));
        y = Math.max(yMinimo, Math.min(PISO_MAXIMO, y));

        if (!bloqueado && espera <= 0 && fundido < 0.4) {
            revisarPuertas();
        }
        actualizarParticulas();
        actualizarSombras(bloqueado);
        if (golpeFlash > 0) {
            golpeFlash = Math.max(0, golpeFlash - 0.055);
        }
    }

    public boolean isCorriendo() {
        return corriendo;
    }

    /** Aguante restante para correr, de 0 a 100. */
    public double getStamina() {
        return stamina;
    }

    public double getStaminaMaxima() {
        return STAMINA_MAXIMA;
    }

    /** Cantidad acumulada de golpes de sombra, para que el HUD pueda destellar. */
    public int getGolpes() {
        return golpes;
    }

    /** Los pies solo pisan el piso de cada sala, nunca la pared. */
    private int pisoMinimo() {
        return switch (sala) {
            case BANO -> 96;
            case COCINA -> 98;
            case ALMACEN -> 100;
            default -> 94;
        };
    }

    // ------------------------------------------------------------------ sombras
    // del apagón

    /**
     * El apagón dura lo que tarde el tablero: mientras tanto, las sombras caminan.
     */
    private boolean apagon() {
        return juego.hayTareaPendiente(Lugar.TABLERO);
    }

    private void actualizarSombras(boolean bloqueado) {
        if (!apagon()) {
            if (!sombras.isEmpty()) {
                sombras.clear();
            }
            esperaGolpe = Math.max(0, esperaGolpe - 1);
            return;
        }
        // Con un menú abierto el turno no corre: las sombras esperan.
        if (bloqueado) {
            return;
        }
        if (sombras.isEmpty()) {
            sembrarSombras();
        }
        if (esperaGolpe > 0) {
            esperaGolpe--;
        }

        for (Sombra s : sombras) {
            double dx = x - s.x;
            double dy = (y - 6) - s.y;
            double distancia = Math.hypot(dx, dy);
            if (distancia > 0.001) {
                s.x += dx / distancia * VELOCIDAD_SOMBRA;
                s.y += dy / distancia * VELOCIDAD_SOMBRA;
            }
            // Deriva leve: nunca caminan en una línea perfecta.
            s.fase += 0.05;
            s.x += Math.cos(s.fase * 1.7) * 0.06;
            s.y += Math.sin(s.fase * 1.3) * 0.04;
            s.x = Math.max(12, Math.min(ANCHO - 12, s.x));
            s.y = Math.max(pisoMinimo() - 4, Math.min(PISO_MAXIMO, s.y));

            if (esperaGolpe <= 0 && distancia < DISTANCIA_GOLPE) {
                esperaGolpe = ESPERA_GOLPE;
                golpeFlash = 1;
                golpes++;
                juego.golpeSombra();
                // Retrocede tras el susto: no te atrapa, te desgasta.
                double empuje = Math.max(1, distancia);
                s.x -= dx / empuje * 24;
                s.y -= dy / empuje * 24;
            }
        }
    }

    /**
     * Tres sombras lejos del jugador para que el apagón se sienta poblado, no
     * injusto.
     */
    private void sembrarSombras() {
        sombras.clear();
        for (int i = 0; i < SOMBRAS_APAGON; i++) {
            Sombra s = new Sombra();
            int intentos = 0;
            do {
                s.x = 36 + azar.nextDouble() * (ANCHO - 72);
                s.y = pisoMinimo() + azar.nextDouble() * (PISO_MAXIMO - pisoMinimo());
                intentos++;
            } while (Math.hypot(s.x - x, s.y - y) < 38 && intentos < 24);
            s.fase = azar.nextDouble() * Math.PI * 2;
            sombras.add(s);
        }
        esperaGolpe = 60; // margen para reaccionar al primer parpadeo
    }

    private static final class Sombra {
        double x;
        double y;
        double fase;
    }

    /**
     * La zona accionable más cercana. Una estación solo aparece si está rota o si
     * de ahí
     * sale el pedido actual; las tareas, solo mientras siguen pendientes.
     */
    public Zona zonaCercana() {
        List<Zona> zonas = ZONAS.get(sala);
        if (zonas == null) {
            return null;
        }
        Zona cercana = null;
        double mejor = Double.MAX_VALUE;
        for (Zona z : zonas) {
            if (!zonaActiva(z)) {
                continue;
            }
            Rectangle r = z.area();
            double cx = Math.max(r.x, Math.min(x, r.x + r.width));
            double cy = Math.max(r.y, Math.min(y, r.y + r.height));
            double distancia = Math.hypot(x - cx, y - cy);
            if (distancia <= DISTANCIA && distancia < mejor) {
                mejor = distancia;
                cercana = z;
            }
        }
        return cercana;
    }

    private boolean zonaActiva(Zona z) {
        String id = z.id();
        if (id.equals("cliente")) {
            Cliente c = juego.clienteActual();
            return c != null && (!c.isPedidoTomado() || juego.tieneBandejaPara(c));
        }
        if (id.startsWith("estacion:")) {
            Lugar estacion = Lugar.valueOf(id.substring("estacion:".length()));
            if (juego.hayTareaPendiente(estacion)) {
                return true;
            }
            Cliente c = juego.clienteActual();
            return c != null && c.isPedidoTomado()
                    && JuegoService.estacionPara(c) == estacion
                    && !juego.tieneBandejaPara(c)
                    && juego.getEnMano() == null;
        }
        if (id.startsWith("tarea:")) {
            return juego.hayTareaPendiente(Lugar.valueOf(id.substring("tarea:".length())));
        }
        return true;
    }

    /**
     * Evita que el jugador se meta dentro de un mueble. El punto de apoyo son los
     * pies.
     */
    private boolean bloquea(double nx, double ny) {
        List<Rectangle> obstaculos = OBSTACULOS.get(sala);
        if (obstaculos == null) {
            return false;
        }
        for (Rectangle r : obstaculos) {
            if (nx > r.x - 4 && nx < r.x + r.width + 4
                    && ny > r.y - 1 && ny < r.y + r.height + 2) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ puertas

    private void revisarPuertas() {
        boolean franja = y > 92 && y < 166;
        switch (sala) {
            case COMEDOR:
                if (franja && x < 20) {
                    irA(Sala.BANO);
                } else if (franja && x > ANCHO - 20) {
                    irA(Sala.COCINA);
                }
                break;
            case BANO:
                if (franja && x > ANCHO - 20) {
                    irA(Sala.COMEDOR);
                }
                break;
            case COCINA:
                if (franja && x < 20) {
                    irA(Sala.COMEDOR);
                } else if (x > 166 && x < 222 && y > 200) {
                    irA(Sala.ALMACEN);
                }
                break;
            case ALMACEN:
                if (x > 166 && x < 222 && y > 200) {
                    irA(Sala.COCINA);
                }
                break;
            default:
                break;
        }
    }

    private void irA(Sala destino) {
        Sala origen = sala;
        sala = destino;
        juego.setSala(destino);
        ubicarEntrada(destino, origen);
        fundido = 1;
        espera = 30;
        reiniciarSombras();
    }

    private void ubicarEntrada(Sala destino, Sala origen) {
        switch (destino) {
            case BANO:
                x = ANCHO - 30;
                y = 128;
                break;
            case COCINA:
                if (origen == Sala.ALMACEN) {
                    x = 192;
                    y = 190;
                } else {
                    x = 30;
                    y = 128;
                }
                break;
            case ALMACEN:
                x = 192;
                y = 190;
                break;
            case COMEDOR:
            default:
                if (origen == Sala.BANO) {
                    x = 30;
                    y = 128;
                } else if (origen == Sala.COCINA) {
                    x = ANCHO - 30;
                    y = 128;
                } else {
                    x = 192;
                    y = 184;
                }
                break;
        }
    }

    // ------------------------------------------------------------------ partículas

    private static final class Particula {
        double x;
        double y;
        double vx;
        double vy;
        int vida;
        int tipo;
    }

    private Particula nuevaParticula() {
        Particula p = new Particula();
        reiniciarParticula(p);
        return p;
    }

    private void reiniciarParticula(Particula p) {
        p.x = 8 + azar.nextDouble() * (ANCHO - 16);
        p.y = 40 + azar.nextDouble() * (ALTO - 50);
        p.vx = (azar.nextDouble() - 0.5) * 0.12;
        p.vy = -0.04 - azar.nextDouble() * 0.1;
        p.vida = 160 + azar.nextInt(500);
        p.tipo = 0;
        if (sala == Sala.COCINA && azar.nextInt(3) == 0) {
            p.x = 118 + azar.nextDouble() * 60;
            p.y = 92;
            p.vy = -0.12 - azar.nextDouble() * 0.1;
            p.tipo = 1;
        } else if (sala == Sala.BANO && azar.nextInt(4) == 0) {
            p.x = 150 + azar.nextDouble() * 90;
            p.y = 110 + azar.nextDouble() * 40;
            p.vy = 0.1 + azar.nextDouble() * 0.1;
            p.tipo = 2;
        }
    }

    private void actualizarParticulas() {
        for (Particula p : particulas) {
            p.x += p.vx;
            p.y += p.vy;
            p.vida--;
            if (p.vida <= 0 || p.x < -4 || p.x > ANCHO + 4 || p.y < 30 || p.y > ALTO) {
                reiniciarParticula(p);
            }
        }
    }

    // ------------------------------------------------------------------ pintado

    public void pintar(Graphics2D g) {
        Pixel.preparar(g);
        g.setColor(NEGRO);
        g.fillRect(0, 0, ANCHO, ALTO);

        switch (sala) {
            case COMEDOR:
                dibujarComedor(g);
                break;
            case BANO:
                dibujarBano(g);
                break;
            case COCINA:
                dibujarCocina(g);
                break;
            case ALMACEN:
                dibujarAlmacen(g);
                break;
            default:
                break;
        }

        dibujarParticulas(g);
        dibujarSombras(g, false);
        dibujarJugador(g);
        dibujarSombras(g, true);
        pintarLuz(g);
        dibujarOjosSombras(g);
        dibujarMarcadores(g);
        pintarCordura(g);

        if (golpeFlash > 0) {
            g.setColor(new Color(122, 12, 16, (int) Math.round(golpeFlash * 76)));
            g.fillRect(0, 0, ANCHO, ALTO);
        }

        if (fundido > 0) {
            g.setColor(new Color(0, 0, 0, (int) Math.round(fundido * 255)));
            g.fillRect(0, 0, ANCHO, ALTO);
        }
    }

    // ------------------------------------------------------------------ comedor

    private void dibujarComedor(Graphics2D g) {
        pared(g, HORIZONTE, PARED, PARED_OSCURA, PARED_CLARA);
        piso(g, HORIZONTE + 4, PISO_A, PISO_B, new Color(0x1A1B17));
        alfombra(g, 44, 146, 200, 62);
        luzPiso(g, 148, 96, 74, 48, new Color(0xFFE6A0));
        luzPiso(g, 286, 100, 52, 30, new Color(0xFFE6A0));

        // ventana con noche y lluvia
        ventana(g, 58, 14, 46, 30);

        // cartel del menú
        cartelMenu(g, 214, 12, 62, 34);

        // póster de mascota perdida
        poster(g, 292, 14, 34, 40);

        // tablero de luz
        tablero(g, 18, 66);

        // mesas y sillas (la zona de limpieza queda sobre la mesa grande)
        mesa(g, 92, 156);
        silla(g, 68, 170, true);
        silla(g, 116, 170, true);
        mesa(g, 150, 188);
        silla(g, 128, 198, true);
        silla(g, 172, 198, true);

        // basura
        basura(g, 316, 154);

        // clientes detrás del mostrador
        dibujarClientes(g);

        // mostrador
        mostrador(g);

        // puertas laterales
        puertaLateral(g, true, "BAÑO");
        puertaLateral(g, false, "COCINA");

        // lámpara del techo
        lampara(g, 148, 16);
        lampara(g, 286, 22);
    }

    private void dibujarClientes(Graphics2D g) {
        int pendientes = juego.totalClientes() - juego.clientesAtendidos();
        int dibujados = Math.min(3, pendientes);
        int[] lugares = { 158, 192, 226 };
        for (int i = 0; i < dibujados; i++) {
            int cx = lugares[i] + (int) Math.round(Math.sin((tick + i * 37) * 0.03) * 1.5);
            figuraCliente(g, cx, 132, i);
        }
    }

    private void mostrador(Graphics2D g) {
        // Mueble en medio del salón: los clientes quedan detrás y el jugador atiende
        // desde adelante.
        caja(g, 124, 124, 136, 26, 8, MADERA, MADERA_CLARA, MADERA_OSCURA);
        r(g, 124, 124, 136, 4, METAL_CLARO);
        r(g, 126, 128, 132, 2, METAL);
        r(g, 124, 132, 136, 1, new Color(0x2E1F14));
        r(g, 128, 145, 128, 1, new Color(0x3A2818));
        r(g, 128, 149, 128, 1, new Color(0x2A1C12));

        // Vitrina chica, caja registradora y bandeja sobre la tapa.
        caja(g, 132, 112, 34, 6, 6, new Color(0x3A4A43), new Color(0x52665C), new Color(0x26322D));
        r(g, 136, 114, 26, 4, new Color(0x9FB7A9, true));
        r(g, 136, 114, 26, 1, new Color(0xD9E4DA));
        r(g, 232, 106, 22, 18, new Color(0x2C3530));
        r(g, 234, 108, 18, 7, new Color(0x6C8C75));
        r(g, 236, 110, 14, 2, new Color(0xB6D4AC));
        r(g, 232, 120, 22, 4, new Color(0x171D1A));
        r(g, 204, 118, 18, 2, METAL);
        r(g, 207, 114, 12, 4, new Color(0xCBD5CB));
        r(g, 209, 112, 8, 2, new Color(0xE7EEE4));
        r(g, 215, 111, 2, 1, MOSTAZA);

        // Zócalo y cartel de la caja.
        r(g, 124, 152, 136, 2, new Color(0x2A1C12));
        r(g, 130, 137, 124, 3, new Color(0x4A3422));
        Pixel.textoDisplay(g, "CAJA", 178, 143, 6, MOSTAZA, true);
    }

    private void mesa(Graphics2D g, int cx, int cy) {
        sombra(g, cx - 15, cy + 6, 30, 7);
        r(g, cx - 9, cy + 1, 2, 9, MADERA_OSCURA);
        r(g, cx + 7, cy + 1, 2, 9, MADERA_OSCURA);
        Pixel.elipse(g, cx - 15, cy - 8, 30, 14, new Color(0x2A1C12));
        Pixel.elipse(g, cx - 14, cy - 7, 28, 12, MADERA_OSCURA);
        Pixel.elipse(g, cx - 13, cy - 7, 26, 10, MADERA);
        Pixel.elipse(g, cx - 11, cy - 7, 22, 7, MADERA_CLARA);
        r(g, cx - 14, cy + 2, 28, 1, new Color(0x3A2818));
        r(g, cx - 8, cy - 4, 5, 1, new Color(0x5A3F26));
        r(g, cx + 2, cy - 2, 3, 1, new Color(0x4C3521));
        // restos de comida
        r(g, cx - 3, cy - 5, 1, 1, new Color(0xB8A46A));
        r(g, cx + 5, cy - 4, 1, 1, new Color(0x8C4B42));
    }

    private void silla(Graphics2D g, int cx, int cy, boolean frente) {
        sombra(g, cx - 6, cy + 3, 12, 4);
        r(g, cx - 4, cy - 4, 8, 3, MADERA_OSCURA);
        r(g, cx - 3, cy - 3, 6, 2, MADERA);
        r(g, cx - 4, cy - 8, 8, 2, MADERA_OSCURA);
        r(g, cx - 3, cy - 7, 6, 1, MADERA);
        r(g, cx - 4, cy - 1, 2, 4, MADERA_OSCURA);
        r(g, cx + 2, cy - 1, 2, 4, MADERA_OSCURA);
        r(g, cx - 4, cy, 1, 2, MADERA_CLARA);
    }

    private void basura(Graphics2D g, int cx, int cy) {
        sombra(g, cx - 8, cy + 12, 18, 5);
        r(g, cx - 7, cy, 14, 16, new Color(0x4C5A52));
        r(g, cx - 6, cy + 1, 3, 13, new Color(0x6C7C72));
        r(g, cx + 2, cy + 1, 3, 13, new Color(0x39463F));
        r(g, cx - 9, cy - 3, 18, 4, new Color(0x2D3832));
        r(g, cx - 8, cy - 4, 16, 2, new Color(0x7E8E84));
        r(g, cx - 2, cy - 6, 4, 2, new Color(0x2D3832));
        // moscas
        int fase = (tick / 3) % 4;
        r(g, cx + 7 + fase, cy - 7, 1, 1, new Color(0x2A2A24));
        r(g, cx - 9, cy - 5 + (fase % 2), 1, 1, new Color(0x2A2A24));
        r(g, cx + 3, cy - 9 - (fase % 2), 1, 1, new Color(0x2A2A24));
        if (juego.hayTareaPendiente(Lugar.BASURA)) {
            r(g, cx - 10, cy + 14, 2, 1, new Color(0xB8A46A));
            r(g, cx + 10, cy + 15, 2, 1, new Color(0x8C4B42));
        }
    }

    private void tablero(Graphics2D g, int x, int y) {
        r(g, x, y, 24, 22, new Color(0x2F3A34));
        r(g, x + 1, y + 1, 22, 20, new Color(0x4A5A50));
        r(g, x + 3, y + 3, 18, 2, new Color(0x1C2520));
        for (int i = 0; i < 4; i++) {
            r(g, x + 4 + i * 4, y + 8, 2, 4, new Color(0x1C2520));
            r(g, x + 4 + i * 4, y + 8, 1, 2, new Color(0xBFC9BC));
        }
        r(g, x + 4, y + 15, 16, 1, new Color(0x1C2520));
        if (juego.hayTareaPendiente(Lugar.TABLERO)) {
            int chispa = (tick / 2) % 6;
            if (chispa < 3) {
                r(g, x + 17, y + 15, 2, 2, new Color(0xFFE08A));
                r(g, x + 19, y + 13, 1, 1, new Color(0xFFF2BC));
            }
        } else {
            r(g, x + 17, y + 15, 2, 1, new Color(0x86B8A8));
        }
    }

    private void cartelMenu(Graphics2D g, int x, int y, int w, int h) {
        r(g, x + 1, y + 1, w, h, new Color(0, 0, 0, 100));
        r(g, x, y, w, h, new Color(0x262019));
        r(g, x + 1, y + 1, w - 2, h - 2, new Color(0x3A3022));
        r(g, x + 1, y + 1, w - 2, 1, new Color(0x6B4E32));
        Pixel.textoDisplay(g, "MENU", x + 5, y + 9, 8, MOSTAZA, true);
        r(g, x + 5, y + 12, w - 10, 1, new Color(0x6B4E32));
        for (int i = 0; i < 3; i++) {
            r(g, x + 5, y + 16 + i * 5, 18, 1, new Color(0x8C7A5C));
            r(g, x + 30, y + 16 + i * 5, 5, 1, new Color(0x8C7A5C));
            r(g, x + 38, y + 16 + i * 5, 3, 1, MOSTAZA);
        }
        r(g, x + 5, y + 31, w - 10, 1, new Color(0x1E1812));
    }

    private void poster(Graphics2D g, int x, int y, int w, int h) {
        r(g, x + 1, y + 1, w, h, new Color(0, 0, 0, 90));
        r(g, x, y, w, h, new Color(0xB8A98D));
        r(g, x + 1, y + 1, w - 2, h - 2, new Color(0xD6C9AA));
        r(g, x + 3, y + 3, w - 6, 1, new Color(0x8C7A5C));
        r(g, x + 3, y + 5, w - 6, 13, new Color(0x8E9E92));
        // silueta de una nena
        r(g, x + 14, y + 7, 6, 5, new Color(0x4A5A50));
        r(g, x + 13, y + 12, 8, 5, new Color(0x4A5A50));
        r(g, x + 15, y + 8, 1, 1, new Color(0xE8E4D4));
        r(g, x + 18, y + 8, 1, 1, new Color(0xE8E4D4));
        Pixel.textoDisplay(g, "PERDIDA", x + 4, y + 25, 6, new Color(0x5A3428), true);
        r(g, x + 4, y + 29, w - 8, 1, new Color(0x8C7A5C));
        r(g, x + 4, y + 32, w - 12, 1, new Color(0x8C7A5C));
        r(g, x + 4, y + 35, w - 10, 1, new Color(0x8C7A5C));
    }

    // ------------------------------------------------------------------ baño

    private void dibujarBano(Graphics2D g) {
        // pared de azulejos chicos
        gradiente(g, 0, 0, ANCHO, HORIZONTE, new Color(0x243B33), new Color(0x2C4A3F));
        for (int yy = 4; yy < HORIZONTE - 4; yy += 6) {
            r(g, 0, yy, ANCHO, 1, new Color(0x1C302A));
            for (int xx = (yy / 6 % 2) * 6; xx < ANCHO; xx += 12) {
                r(g, xx, yy, 1, 6, new Color(0x1C302A));
            }
        }
        r(g, 0, HORIZONTE - 4, ANCHO, 4, new Color(0x1B2E28));
        r(g, 0, HORIZONTE, ANCHO, 2, new Color(0x5E7A6C));
        piso(g, HORIZONTE + 2, new Color(0x2A3732), new Color(0x222E2A), new Color(0x17211E));
        luzPiso(g, 194, 92, 62, 42, new Color(0xCFEBDD));
        luzPiso(g, 262, 100, 40, 26, new Color(0xCFEBDD));

        // puerta de regreso
        puertaLateral(g, false, "COMEDOR");

        // inodoros y puertas de cabina
        cabina(g, 24, 44, 48);
        cabina(g, 80, 44, 48);
        cabina(g, 136, 44, 48);

        // lavatorios y espejos
        lavatorio(g, 232, 76);
        lavatorio(g, 286, 76);
        espejo(g, 222, 22, 34, 42, true);
        espejo(g, 278, 22, 34, 42, false);

        // piso mojado, balde y trapeador
        charco(g, 150, 160, 42, 13);
        charco(g, 208, 186, 30, 9);
        charco(g, 118, 196, 24, 7);
        balde(g, 102, 170);

        // grafiti
        Pixel.textoDisplay(g, "NO MIRES", 158, 34, 8, new Color(0x7E2A26), true);

        // luz fluorescente
        r(g, 172, 4, 44, 3, new Color(0xD8E6D5));
        r(g, 174, 7, 40, 1, new Color(0x86B8A8));
        if (juego.hayTareaPendiente(Lugar.BANO)) {
            r(g, 156, 140, 3, 1, new Color(0x6C8C75));
            r(g, 214, 150, 2, 1, new Color(0x6C8C75));
        }
    }

    private void cabina(Graphics2D g, int x, int y, int w) {
        // La cabina baja de la pared hasta el piso: así no queda flotando en el
        // horizonte.
        sombra(g, x + 2, y + 52, w - 4, 10);
        r(g, x, y, w, 46, new Color(0x334B41));
        r(g, x + 2, y + 2, w - 4, 42, new Color(0x253A32));
        r(g, x + 4, y + 4, w - 8, 38, new Color(0x1B2C27));
        // puerta entreabierta con luz del otro lado
        r(g, x + 5, y + 6, w - 12, 34, new Color(0x3E5A4C));
        r(g, x + 6, y + 7, w - 14, 32, new Color(0x294238));
        r(g, x + 8, y + 9, 1, 28, new Color(0x536F60));
        r(g, x + w - 8, y + 23, 1, 2, new Color(0xB8C4B6));
        // madera que apoya en el piso
        r(g, x + 3, y + 46, 3, 10, new Color(0x2A3B33));
        r(g, x + w - 6, y + 46, 3, 10, new Color(0x2A3B33));
        r(g, x + 2, y + 55, 5, 2, new Color(0x16231E));
        r(g, x + w - 7, y + 55, 5, 2, new Color(0x16231E));
        r(g, x + 1, y + 57, w - 2, 2, new Color(0, 0, 0, 90));
    }

    private void lavatorio(Graphics2D g, int cx, int y) {
        sombra(g, cx - 12, y + 10, 24, 5);
        r(g, cx - 10, y, 20, 9, new Color(0xB8C8BD));
        r(g, cx - 9, y + 1, 18, 6, new Color(0xD6E0D4));
        r(g, cx - 7, y + 2, 14, 4, new Color(0x8EA49A));
        r(g, cx - 2, y - 4, 4, 5, METAL);
        r(g, cx - 1, y - 5, 2, 1, new Color(0xE0E8DE));
        r(g, cx - 8, y + 9, 2, 6, new Color(0x7E9488));
        r(g, cx + 6, y + 9, 2, 6, new Color(0x7E9488));
        // agua
        r(g, cx - 1, y + 6, 1, 2, new Color(0x86B8A8));
    }

    private void espejo(Graphics2D g, int x, int y, int w, int h, boolean roto) {
        r(g, x, y, w, h, new Color(0x1B2C27));
        r(g, x + 2, y + 2, w - 4, h - 4, new Color(0x405D50));
        gradiente(g, x + 3, y + 3, w - 6, h - 6, new Color(0x6C8C7C), new Color(0x2A4037));
        r(g, x + 4, y + 4, 6, 2, new Color(0x9FB7A9));
        if (roto) {
            // grieta y una mano del otro lado
            r(g, x + 14, y + 10, 1, 9, new Color(0x1B2C27));
            r(g, x + 15, y + 18, 4, 1, new Color(0x1B2C27));
            r(g, x + 15, y + 19, 1, 4, new Color(0x1B2C27));
            r(g, x + 22, y + 20, 5, 2, new Color(0x1B2C27));
            r(g, x + 23, y + 18, 2, 2, new Color(0x15231F));
        } else {
            r(g, x + 20, y + 14, 2, 1, new Color(0x15231F));
            r(g, x + 24, y + 14, 2, 1, new Color(0x15231F));
            r(g, x + 22, y + 17, 2, 1, new Color(0x15231F));
        }
        r(g, x, y + h, w, 2, new Color(0x0E1915));
    }

    private void charco(Graphics2D g, int x, int y, int w, int h) {
        Pixel.elipse(g, x, y, w, h, new Color(0x162A25));
        Pixel.elipse(g, x + 2, y + 1, w - 5, h - 3, new Color(0x2E5047));
        Pixel.elipse(g, x + 6, y + 2, w - 15, h - 6, new Color(0x24453C));
        g.setColor(new Color(0x6C9A8A));
        g.fillRect(x + 5, y + 2, 4, 1);
        g.fillRect(x + w - 10, y + h - 3, 3, 1);
    }

    private void balde(Graphics2D g, int cx, int cy) {
        sombra(g, cx - 8, cy + 9, 16, 4);
        r(g, cx - 7, cy - 5, 14, 13, new Color(0x8A7A4A));
        r(g, cx - 6, cy - 4, 3, 11, new Color(0xB0A064));
        r(g, cx - 8, cy - 7, 16, 3, new Color(0x5E5534));
        r(g, cx - 2, cy - 8, 4, 1, new Color(0x3A3422));
        // trapeador apoyado
        r(g, cx + 9, cy - 16, 1, 20, new Color(0x6B4E32));
        r(g, cx + 7, cy + 3, 5, 5, new Color(0x7E8E84));
        r(g, cx + 6, cy + 7, 7, 2, new Color(0x9FB0A4));
    }

    // ------------------------------------------------------------------ cocina

    private void dibujarCocina(Graphics2D g) {
        // pared de acero con paneles
        gradiente(g, 0, 0, ANCHO, HORIZONTE, new Color(0x2A3732), new Color(0x384741));
        for (int x = 0; x < ANCHO; x += 32) {
            r(g, x, 0, 1, HORIZONTE - 4, new Color(0x202B27));
            r(g, x + 1, 0, 1, HORIZONTE - 4, new Color(0x52645A));
        }
        r(g, 0, HORIZONTE - 4, ANCHO, 4, new Color(0x1D2824));
        r(g, 0, HORIZONTE, ANCHO, 2, new Color(0x6A7C70));
        piso(g, HORIZONTE + 2, new Color(0x30322B), new Color(0x262821), new Color(0x1A1C18));
        luzPiso(g, 92, 96, 62, 44, new Color(0xFFE6A0));
        luzPiso(g, 300, 96, 54, 34, new Color(0xFFE6A0));
        luzPiso(g, 192, 150, 40, 30, new Color(0xFFD68C));

        // campana extractora, arriba y lejos de los muebles
        r(g, 40, 8, 144, 8, new Color(0x4A5952));
        r(g, 44, 16, 136, 5, new Color(0x63746A));
        r(g, 120, 0, 20, 8, new Color(0x3A4741));
        for (int i = 0; i < 5; i++) {
            r(g, 48 + i * 26, 16, 18, 1, new Color(0x2A3732));
        }

        // estufa: 44..136 x 62..96, sin invadir la plancha
        caja(g, 44, 62, 92, 24, 10, new Color(0x3A4741), new Color(0x5A6A60), new Color(0x26302C));
        r(g, 46, 64, 88, 6, new Color(0x242C28));
        for (int i = 0; i < 4; i++) {
            r(g, 50 + i * 21, 65, 14, 4, new Color(0x59675F));
            r(g, 52 + i * 21, 66, 10, 1, new Color(0x8A3E2A));
        }
        r(g, 44, 76, 92, 1, new Color(0x6B7A70));
        for (int i = 0; i < 3; i++) {
            r(g, 56 + i * 24, 82, 6, 5, new Color(0x2A3531));
            r(g, 57 + i * 24, 83, 4, 3, METAL_CLARO);
        }
        r(g, 46, 92, 88, 2, METAL_OSCURO);

        // plancha / freidora: 24..110 x 104..120
        caja(g, 24, 104, 86, 10, 6, new Color(0x3C4B44), new Color(0x5C6E63), new Color(0x27322D));
        r(g, 30, 106, 74, 3, new Color(0x1A211E));
        r(g, 30, 107, 74, 1, new Color(0x8A6A3A));
        for (int i = 0; i < 5; i++) {
            r(g, 36 + i * 13, 109, 7, 1, new Color(0x2A2A22));
        }
        r(g, 34, 114, 66, 1, new Color(0x2A3531));
        if (juego.hayTareaPendiente(Lugar.PLANCHA)) {
            r(g, 58, 101, 1, 2, new Color(0x6E7E74));
            r(g, 63, 98, 2, 1, new Color(0x8A9A90));
        }

        // cafetera: 150..194, separada de la estufa y de la mesada
        caja(g, 150, 84, 44, 10, 6, new Color(0x3A4741), new Color(0x5C6E63), new Color(0x26302C));
        r(g, 160, 66, 24, 3, new Color(0x2A2E2A));
        r(g, 162, 69, 20, 13, new Color(0x2A2E2A));
        r(g, 164, 71, 16, 7, new Color(0x8A3E32));
        r(g, 167, 72, 10, 1, new Color(0xC4513F));
        r(g, 166, 84, 12, 8, new Color(0xB8C4B6, true));
        r(g, 168, 88, 8, 3, new Color(0x3A2620));
        r(g, 184, 86, 5, 8, new Color(0x1A211E));
        r(g, 185, 88, 3, 2, new Color(0xD9A441));
        r(g, 157, 80, 3, 6, new Color(0x59675F));
        if (juego.hayTareaPendiente(Lugar.CAFETERA)) {
            r(g, 172, 98, 1, 3, new Color(0x5A3428));
            r(g, 173, 101, 1, 1, new Color(0x5A3428));
        }

        // mesada de preparación: 228..320 x 66..98
        caja(g, 228, 66, 92, 24, 8, new Color(0x596B60), new Color(0x8A9E90), new Color(0x3A4741));
        r(g, 230, 64, 88, 3, new Color(0xC4D0C6));
        r(g, 240, 58, 18, 3, METAL);
        r(g, 242, 55, 14, 3, new Color(0xD9E2D6));
        r(g, 270, 57, 12, 5, new Color(0x8E2F2A));
        r(g, 284, 58, 4, 1, new Color(0xD9A441));
        r(g, 298, 52, 1, 12, METAL);
        r(g, 294, 52, 9, 2, METAL_CLARO);

        // heladera: 324..372 x 60..150
        caja(g, 324, 60, 48, 82, 8, new Color(0x84928A), new Color(0xB0BCB4), new Color(0x5A6A60));
        r(g, 326, 62, 44, 1, new Color(0xD6DED4));
        r(g, 326, 92, 44, 1, new Color(0x4A5952));
        r(g, 366, 74, 2, 12, new Color(0x4A5952));
        r(g, 366, 100, 2, 20, new Color(0x4A5952));
        r(g, 332, 68, 4, 3, MOSTAZA);
        r(g, 342, 70, 2, 2, new Color(0xC4513F));
        r(g, 334, 110, 3, 1, new Color(0xC8D2C8));
        r(g, 335, 111, 1, 3, new Color(0xC8D2C8));
        if (juego.hayTareaPendiente(Lugar.HELADERA)) {
            r(g, 348, 132, 8, 1, new Color(0x5A3428));
            r(g, 350, 134, 4, 1, new Color(0x5A3428));
        }

        // ollas colgadas entre la campana y la mesada
        r(g, 196, 18, 1, 14, new Color(0x2A3732));
        r(g, 188, 30, 18, 3, new Color(0x7C8A85));
        r(g, 190, 33, 14, 7, new Color(0x4C5953));
        r(g, 198, 27, 1, 3, new Color(0x2A3732));
        r(g, 214, 22, 1, 12, new Color(0x2A3732));
        r(g, 208, 32, 14, 2, new Color(0x8A9691));

        // puertas y lámparas
        puertaLateral(g, true, "COMEDOR");
        puertaAbajo(g, "ALMAC\u00c9N");
        lampara(g, 210, 8);
        lampara(g, 304, 10);
    }

    // ------------------------------------------------------------------ almacén

    private void dibujarAlmacen(Graphics2D g) {
        // pared de ladrillo gastado
        gradiente(g, 0, 0, ANCHO, 66, new Color(0x2A2118), new Color(0x3A2C1E));
        for (int yy = 6; yy < 64; yy += 8) {
            r(g, 0, yy, ANCHO, 1, new Color(0x241C14));
            int off = (yy / 8 % 2) * 8;
            for (int xx = off; xx < ANCHO; xx += 16) {
                r(g, xx, yy, 1, 8, new Color(0x241C14));
            }
        }
        r(g, 0, 64, ANCHO, 3, new Color(0x5A4430));
        r(g, 0, 67, ANCHO, 2, new Color(0x1C150F));
        piso(g, 69, new Color(0x2A241C), new Color(0x221D16), new Color(0x17130E));
        luzPiso(g, 192, 74, 60, 36, new Color(0xFFD68C));
        desperdicios(g);

        // estantes de arriba
        estante(g, 30, 52, 58);
        estante(g, 102, 52, 58);
        estante(g, 174, 52, 58);
        estante(g, 246, 52, 58);
        // estantes de abajo
        estante(g, 30, 126, 58);
        estante(g, 102, 126, 58);
        estante(g, 174, 126, 58);

        // herramientas sobre los estantes
        dibujarHerramienta(g, Lugar.BANO, 50, 60);
        dibujarHerramienta(g, Lugar.MESAS, 122, 60);
        dibujarHerramienta(g, Lugar.BASURA, 194, 60);
        dibujarHerramienta(g, Lugar.TABLERO, 266, 60);
        dibujarHerramienta(g, Lugar.PLANCHA, 50, 134);
        dibujarHerramienta(g, Lugar.CAFETERA, 122, 134);
        dibujarHerramienta(g, Lugar.HELADERA, 194, 134);

        // cajas y bidones en el piso
        caja(g, 246, 130, 26, 18, 6, new Color(0x6B4E32), new Color(0x8A6844), new Color(0x4A3422));
        r(g, 250, 134, 18, 1, new Color(0xB08A5A));
        r(g, 252, 138, 14, 1, new Color(0x3E2C1D));
        caja(g, 292, 132, 20, 14, 5, new Color(0x5A4430), new Color(0x7A5C3C), new Color(0x3E2C1D));
        caja(g, 318, 128, 18, 12, 5, new Color(0x4A5A50), new Color(0x6A7C70), new Color(0x2E3A34));
        r(g, 252, 152, 10, 8, new Color(0x4A5A50));
        r(g, 254, 150, 6, 2, new Color(0x2E3A34));
        r(g, 300, 120, 1, 12, METAL);
        r(g, 296, 132, 9, 1, METAL);

        // telarañas
        r(g, 4, 4, 1, 12, new Color(0x6A7C70, true));
        r(g, 4, 4, 12, 1, new Color(0x6A7C70, true));
        r(g, 8, 8, 1, 6, new Color(0x6A7C70, true));
        r(g, 12, 12, 6, 1, new Color(0x6A7C70, true));

        // escotilla en el piso: única salida al pasillo de la cocina
        puertaAbajo(g, "COCINA");
        // bombilla
        r(g, 190, 0, 1, 20, new Color(0x2A2118));
        r(g, 186, 20, 9, 4, new Color(0x4A3A28));
        r(g, 188, 22, 5, 3, new Color(0xFFE6A0));
        if (juego.hayTareaPendiente(Lugar.TABLERO)) {
            r(g, 189, 25, 1, 2, new Color(0xB88A3A));
        }

        dibujarConserje(g);
    }

    /**
     * El conserje es el ancla humana del almacén y la única luz durante un apagón.
     */
    private void dibujarConserje(Graphics2D g) {
        int x = 330;
        int pies = 204;
        int bob = (tick / 22) % 2;
        sombra(g, x - 9, pies - 2, 19, 5);

        // botas y pantalón
        r(g, x - 6, pies - 8, 4, 8, new Color(0x1A211E));
        r(g, x + 2, pies - 8, 4, 8, new Color(0x1A211E));
        r(g, x - 7, pies - 2, 5, 2, new Color(0x090C0A));
        r(g, x + 2, pies - 2, 5, 2, new Color(0x090C0A));

        // abrigo largo con botones
        r(g, x - 7, pies - 19 + bob, 14, 12, new Color(0x24352D));
        r(g, x - 7, pies - 19 + bob, 3, 12, new Color(0x32463C));
        r(g, x + 2, pies - 19 + bob, 5, 12, new Color(0x1B2923));
        r(g, x - 1, pies - 17 + bob, 1, 8, new Color(0x0E1713));
        r(g, x - 1, pies - 15 + bob, 1, 1, new Color(0xB08A5A));
        r(g, x - 1, pies - 11 + bob, 1, 1, new Color(0xB08A5A));

        // manos y candelabro
        r(g, x - 9, pies - 13 + bob, 2, 4, PIEL_OSCURA);
        r(g, x + 7, pies - 13 + bob, 2, 4, PIEL_OSCURA);
        r(g, x + 8, pies - 18 + bob, 3, 7, new Color(0xD8D2C0));
        r(g, x + 9, pies - 20 + bob, 1, 2, new Color(0xFFE6A0));
        r(g, x + 8, pies - 21 + bob, 3, 1, new Color(0xF0C56B));

        // cabeza, barba y gorra
        r(g, x - 5, pies - 28 + bob, 11, 10, new Color(0x0A0E0C));
        r(g, x - 4, pies - 27 + bob, 9, 8, PIEL);
        r(g, x - 4, pies - 22 + bob, 9, 3, new Color(0xC8C4B4));
        r(g, x - 3, pies - 20 + bob, 7, 1, new Color(0x9DAFA4));
        r(g, x - 5, pies - 29 + bob, 12, 3, new Color(0x1B2923));
        r(g, x - 6, pies - 27 + bob, 14, 2, new Color(0x32463C));
        r(g, x - 4, pies - 26 + bob, 2, 1, new Color(0xB08A5A));
        // ojos cansados
        r(g, x - 2, pies - 24 + bob, 1, 1, new Color(0x1A211E));
        r(g, x + 2, pies - 24 + bob, 1, 1, new Color(0x1A211E));
    }

    // ------------------------------------------------------------------ marcadores

    /**
     * Señal flotante sobre cada tarea pendiente; se dibuja después de la luz para
     * no perderse.
     */
    private void dibujarMarcadores(Graphics2D g) {
        switch (sala) {
            case COMEDOR -> {
                marcador(g, 30, 60, Lugar.TABLERO);
                marcador(g, 92, 138, Lugar.MESAS);
                marcador(g, 316, 140, Lugar.BASURA);
            }
            case BANO -> marcador(g, 150, 126, Lugar.BANO);
            case COCINA -> {
                marcador(g, 60, 96, Lugar.PLANCHA);
                marcador(g, 172, 62, Lugar.CAFETERA);
                marcador(g, 348, 50, Lugar.HELADERA);
            }
            default -> {
            }
        }
    }

    private void marcador(Graphics2D g, int x, int y, Lugar lugar) {
        if (!juego.hayTareaPendiente(lugar)) {
            return;
        }
        Tarea tarea = juego.tareaPendienteEn(lugar);
        Color color = tarea != null && tarea.getPrioridad() >= 3 ? ROJO_CLARO : MOSTAZA;
        int bob = (tick / 12) % 2;
        int yy = y - bob;
        r(g, x - 1, yy - 2, 3, 1, NEGRO);
        r(g, x - 2, yy - 1, 5, 1, color);
        r(g, x - 1, yy, 3, 1, NEGRO);
        r(g, x, yy + 1, 1, 1, NEGRO);
    }

    private void estante(Graphics2D g, int x, int y, int w) {
        r(g, x + 1, y + 1, w, 44, new Color(0, 0, 0, 90));
        r(g, x, y, w, 44, new Color(0x3E2C1D));
        r(g, x + 2, y + 2, w - 4, 40, new Color(0x2A1E14));
        r(g, x + 2, y + 20, w - 4, 2, new Color(0x6B4E32));
        r(g, x + 2, y + 2, w - 4, 2, new Color(0x6B4E32));
        // mercadería
        for (int i = 0; i < 3; i++) {
            r(g, x + 6 + i * 16, y + 6, 11, 12, i % 2 == 0
                    ? new Color(0x5A4430)
                    : new Color(0x4A5A50));
            r(g, x + 7 + i * 16, y + 7, 9, 1, new Color(0x8A6844));
            r(g, x + 7 + i * 16, y + 10, 9, 1, new Color(0x2E2A22));
        }
        r(g, x + 6, y + 24, 14, 12, new Color(0x4A5A50));
        r(g, x + 7, y + 25, 12, 2, new Color(0x6A7C70));
        r(g, x + 24, y + 24, 12, 12, new Color(0x5A4430));
        r(g, x + 25, y + 25, 10, 2, new Color(0x8A6844));
        r(g, x + 40, y + 26, 8, 8, new Color(0x6C8C75));
        r(g, x + 42, y + 24, 4, 2, new Color(0x4A5A50));
    }

    // ------------------------------------------------------------------ piezas
    // comunes

    private void pared(Graphics2D g, int alto, Color base, Color oscura, Color franja) {
        gradiente(g, 0, 0, ANCHO, alto - 8, mezclar(base, oscura, 0.6), base);
        for (int x = 0; x < ANCHO; x += 8) {
            r(g, x, 0, 1, alto - 8, franja);
        }
        r(g, 0, 0, ANCHO, 3, new Color(0, 0, 0, 120));
        r(g, 0, alto - 8, ANCHO, 4, ZOCALO);
        r(g, 0, alto - 9, ANCHO, 1, ZOCALO_CLARO);
        r(g, 0, alto - 4, ANCHO, 2, new Color(0x241A10));
        r(g, 0, alto - 2, ANCHO, 2, new Color(0x0D1210));
    }

    private void piso(Graphics2D g, int desdeY, Color a, Color b, Color junta) {
        int y = desdeY;
        int fila = 0;
        while (y < ALTO) {
            // Las baldosas se agrandan hacia abajo: da perspectiva sin dibujar contornos
            // por todos lados.
            int alto = Math.min(14, 6 + fila * 2);
            int ancho = Math.min(34, 18 + fila * 2);
            int off = (fila % 2 == 0) ? 0 : -(ancho / 2);
            for (int x = off; x < ANCHO; x += ancho) {
                int columna = Math.floorDiv(x, ancho);
                Color base = ((columna + fila) & 1) == 0 ? a : b;
                g.setColor(base);
                g.fillRect(x, y, ancho, alto);
                if ((columna + fila) % 3 == 0) {
                    g.setColor(mezclar(base, Color.WHITE, 0.05));
                    g.fillRect(x + 1, y, ancho - 2, 1);
                }
                // Junta en L: solo el borde inferior y el derecho.
                g.setColor(junta);
                g.fillRect(x, y + alto - 1, ancho, 1);
                g.fillRect(x + ancho - 1, y, 1, alto);
            }
            y += alto;
            fila++;
        }
        // manchas y brillos sueltos
        for (int i = 0; i < 18; i++) {
            int sx = 10 + (i * 79) % (ANCHO - 20);
            int sy = desdeY + 10 + (i * 53) % (ALTO - desdeY - 16);
            r(g, sx, sy, 1 + (i % 3), 1, new Color(0, 0, 0, 55));
        }
    }

    /**
     * Charco de luz sobre las baldosas: filas que se angostan y se apagan con
     * tramado.
     */
    private void luzPiso(Graphics2D g, int cx, int y, int ancho, int alto, Color color) {
        for (int i = 0; i < alto; i++) {
            int anchoFila = ancho - i * 2;
            if (anchoFila <= 2) {
                break;
            }
            int alfa = Math.max(0, 30 - i / 2);
            if (alfa == 0) {
                break;
            }
            if ((i & 1) == 1) {
                continue;
            }
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alfa));
            g.fillRect(cx - anchoFila / 2, y + i, anchoFila, 1);
        }
    }

    /**
     * Alfombra gastada del comedor: borde oscuro, guardas y rombos descoloridos.
     */
    private void alfombra(Graphics2D g, int x, int y, int w, int h) {
        r(g, x - 1, y - 1, w + 2, h + 2, new Color(0x120E0B));
        r(g, x, y, w, h, new Color(0x3E1B18));
        r(g, x + 2, y + 2, w - 4, h - 4, new Color(0x52241F));
        r(g, x + 3, y + 3, w - 6, 1, new Color(0x6E3028));
        r(g, x + 3, y + h - 4, w - 6, 1, new Color(0x2C1210));
        r(g, x + 3, y + 3, 1, h - 6, new Color(0x6E3028));
        r(g, x + w - 4, y + 3, 1, h - 6, new Color(0x2C1210));
        for (int i = 0; i < 5; i++) {
            int mx = x + 20 + i * 38;
            for (int j = 0; j < 3; j++) {
                int yy = y + 15 + j * 11;
                r(g, mx - 2, yy, 5, 1, new Color(0x8A4A32));
                r(g, mx - 1, yy + 1, 3, 1, new Color(0x7A3A2A));
                r(g, mx, yy + 2, 1, 1, new Color(0x8A4A32));
            }
        }
        // desgaste y pelusas
        for (int i = 0; i < 14; i++) {
            int sx = x + 4 + (i * 31) % (w - 8);
            int sy = y + 4 + (i * 19) % (h - 8);
            r(g, sx, sy, 1 + (i % 3), 1, new Color(0x2E1210));
        }
    }

    /** Virutas, clavos y polvo del almacén repartidos por el piso. */
    private void desperdicios(Graphics2D g) {
        for (int i = 0; i < 20; i++) {
            int sx = 8 + (i * 67) % (ANCHO - 16);
            int sy = 96 + (i * 41) % 102;
            int largo = 1 + (i % 3);
            r(g, sx, sy, largo, 1, new Color(0x6B5A3E));
            if (i % 4 == 0) {
                r(g, sx + 2, sy + 1, 1, 1, new Color(0x8A7452));
            }
        }
    }

    private void gradiente(Graphics2D g, int x, int y, int w, int h, Color arriba, Color abajo) {
        int pasos = Math.max(2, h / 6);
        for (int i = 0; i < pasos; i++) {
            g.setColor(mezclar(arriba, abajo, (double) i / (pasos - 1)));
            g.fillRect(x, y + i * h / pasos, w, h / pasos + 1);
        }
    }

    private void ventana(Graphics2D g, int x, int y, int w, int h) {
        r(g, x - 2, y - 2, w + 4, h + 4, new Color(0x1A140E));
        r(g, x, y, w, h, new Color(0x0B1720));
        gradiente(g, x, y, w, h, new Color(0x122634), new Color(0x0A141B));
        // luna
        Pixel.elipse(g, x + 28, y + 4, 10, 10, new Color(0xC8D2C0));
        Pixel.elipse(g, x + 30, y + 5, 7, 8, new Color(0xE4EBDD));
        g.setColor(new Color(0x8A9A8A));
        g.fillRect(x + 33, y + 7, 2, 2);
        g.fillRect(x + 31, y + 10, 1, 1);
        // lluvia
        for (int i = 0; i < 12; i++) {
            int rx = x + 3 + (i * 13 + tick / 3) % (w - 6);
            int ry = y + 3 + (i * 7 + tick) % (h - 5);
            r(g, rx, ry, 1, 3, new Color(0x6C8C9A, true));
        }
        // marco
        r(g, x + w / 2, y, 2, h, new Color(0x4A3524));
        r(g, x, y + h / 2, w, 2, new Color(0x4A3524));
        r(g, x - 1, y - 1, w + 2, 1, new Color(0x6B4E32));
        r(g, x - 1, y + h, w + 2, 1, new Color(0x2E1F14));
    }

    private void puertaLateral(Graphics2D g, boolean izquierda, String etiqueta) {
        int x = izquierda ? 0 : ANCHO - 20;
        // marco
        r(g, x, 86, 20, 84, new Color(0x3E2C1D));
        r(g, x + 2, 88, 16, 80, new Color(0x0A0F0D));
        r(g, x + 3, 89, 1, 78, new Color(0x22352D));
        r(g, x + 2, 88, 16, 3, new Color(0x6B4E32));
        r(g, x + 2, 165, 16, 3, new Color(0x2A1C12));
        // luz que entra
        g.setColor(new Color(217, 164, 65, 30));
        g.fillRect(izquierda ? 14 : ANCHO - 34, 92, 20, 72);
        // cartel
        int cartelX = izquierda ? 2 : ANCHO - 40;
        r(g, cartelX, 72, 38, 11, new Color(0x17110C));
        r(g, cartelX + 1, 73, 36, 9, new Color(0x3A3022));
        r(g, cartelX + 1, 73, 36, 1, new Color(0x6B4E32));
        Pixel.texto(g, etiqueta, cartelX + 3, 81, Pixel.T_CHICO, MOSTAZA);
        // flecha
        Pixel.textoDisplay(g, izquierda ? "<" : ">", izquierda ? 8 : ANCHO - 16, 98, 8, MOSTAZA, true);
    }

    private void puertaAbajo(Graphics2D g, String etiqueta) {
        // Escotilla en el piso, siempre por encima de la franja del HUD.
        r(g, 166, 188, 52, 18, new Color(0x3E2C1D));
        r(g, 168, 190, 48, 14, new Color(0x070A09));
        r(g, 168, 190, 48, 2, new Color(0x6B4E32));
        for (int i = 0; i < 4; i++) {
            r(g, 170 + i * 12, 192, 2, 10, new Color(0x2A3732));
        }
        r(g, 168, 204, 48, 1, new Color(0x2A1C12));
        r(g, 162, 172, 60, 13, new Color(0x17110C));
        r(g, 163, 173, 58, 11, new Color(0x3A3022));
        r(g, 163, 173, 58, 1, new Color(0x6B4E32));
        Pixel.textoCentrado(g, etiqueta, 192, 182, Pixel.T_CHICO, MOSTAZA);
        Pixel.textoCentrado(g, "v", 192, 203, Pixel.T_CHICO, MOSTAZA);
    }

    private void lampara(Graphics2D g, int cx, int cy) {
        r(g, cx, 0, 1, cy, new Color(0x1A211E));
        r(g, cx - 8, cy, 17, 2, new Color(0x3A4741));
        r(g, cx - 6, cy + 2, 13, 2, new Color(0x5A6A60));
        r(g, cx - 3, cy + 4, 7, 2, new Color(0xFFE6A0));
        r(g, cx - 2, cy + 6, 5, 1, new Color(0xD9A441));
        // halo dither
        for (int i = 3; i >= 1; i--) {
            Pixel.elipse(g, cx - 9 - i * 5, cy + 4 - i * 2, 19 + i * 10, 7 + i * 5,
                    new Color(255, 214, 140, 12 + i * 3));
        }
    }

    private void sombra(Graphics2D g, int x, int y, int w, int h) {
        Pixel.elipse(g, x, y, w, h, new Color(0, 0, 0, 70));
        Pixel.elipse(g, x + 1, y + 1, Math.max(1, w - 3), Math.max(1, h - 2), new Color(0, 0, 0, 55));
    }

    private void caja(Graphics2D g, int x, int y, int w, int h, int alto,
            Color frente, Color tapa, Color lado) {
        sombra(g, x - 2, y + alto + h - 4, w + 6, 7);
        r(g, x, y, w, alto, tapa);
        r(g, x, y + alto, w, h, frente);
        r(g, x + w, y + 2, 3, alto + h - 2, lado);
        r(g, x, y, w, 1, mezclar(tapa, Color.WHITE, 0.22));
        r(g, x, y + alto, w, 1, mezclar(frente, Color.BLACK, 0.25));
        r(g, x, y + alto + h - 1, w, 1, mezclar(frente, Color.BLACK, 0.45));
    }

    private static void r(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.fillRect(x, y, w, h);
    }

    private static Color mezclar(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // ------------------------------------------------------------------ personajes

    /**
     * Siluetas del apagón. Se dibujan antes de la máscara de luz; su propio halo y
     * sus ojos
     * las separan del negro para que el jugador pueda esquivarlas.
     */
    private void dibujarSombras(Graphics2D g, boolean delante) {
        for (Sombra s : sombras) {
            // Las que están más abajo pisan al jugador; las de arriba quedan detrás.
            if ((s.y > y) != delante) {
                continue;
            }
            int sx = (int) Math.round(s.x);
            int sy = (int) Math.round(s.y);

            // aura apenas visible sobre el negro del apagón
            Pixel.elipse(g, sx - 13, sy - 34, 27, 36, new Color(38, 24, 58, 44));
            Pixel.elipse(g, sx - 9, sy - 30, 19, 31, new Color(22, 14, 36, 90));

            // silueta: cabeza redonda y cuerpo que se deshilacha
            Pixel.elipse(g, sx - 6, sy - 29, 13, 13, new Color(6, 5, 10, 244));
            r(g, sx - 5, sy - 20, 11, 14, new Color(6, 5, 10, 240));
            r(g, sx - 6, sy - 16, 2, 9, new Color(6, 5, 10, 220));
            r(g, sx + 5, sy - 15, 2, 8, new Color(6, 5, 10, 220));
            for (int i = 0; i < 3; i++) {
                int ondula = (int) Math.round(Math.sin(tick * 0.09 + s.fase + i) * 2);
                r(g, sx - 4 + i * 3 + ondula, sy - 7, 2, 7 - i, new Color(6, 5, 10, 200));
            }
            // borde violáceo tenue
            r(g, sx - 6, sy - 27, 1, 9, new Color(64, 40, 92, 90));
            r(g, sx + 5, sy - 28, 1, 10, new Color(64, 40, 92, 90));
        }
    }

    /**
     * Los ojos van después de la máscara: son lo único que atraviesa la oscuridad.
     */
    private void dibujarOjosSombras(Graphics2D g) {
        for (Sombra s : sombras) {
            int sx = (int) Math.round(s.x);
            int sy = (int) Math.round(s.y);
            int brillo = 175 + (int) (Math.sin(tick * 0.14 + s.fase) * 45);
            g.setColor(new Color(150, 255, 205, 28));
            g.fillRect(sx - 6, sy - 28, 13, 7);
            g.setColor(new Color(brillo, 255, 208, 230));
            g.fillRect(sx - 4, sy - 25, 2, 2);
            g.fillRect(sx + 2, sy - 25, 2, 2);
        }
    }

    private void dibujarJugador(Graphics2D g) {
        int px = (int) Math.round(x);
        int py = (int) Math.round(y);
        int dir = mirada;
        int frame = caminando ? (int) (animacion) % 2 : 0;
        int bob = caminando && frame == 1 ? -1 : 0;

        sombra(g, px - 5, py - 2, 11, 4);

        if (juego.getJugador().getCordura() < 30) {
            // reflejo que no siempre está donde debería
            for (int dx = -1; dx <= 1; dx++) {
                if ((tick / 7 + dx) % 5 == 0) {
                    dibujarCuerpo(g, px + dx * 2 - 5, py + bob - 17, dir, frame, true);
                }
            }
        }
        dibujarCuerpo(g, px - 5, py + bob - 17, dir, frame, false);
        dibujarObjetoEnMano(g, px, py + bob - 8, dir);
    }

    private void dibujarCuerpo(Graphics2D g, int x, int y, int dir, int frame, boolean fantasma) {
        Color contorno = fantasma ? new Color(0x070A09) : new Color(0x0A0E0C);
        Color pelo = fantasma ? new Color(0x0E1512) : new Color(0x1B1512);
        Color piel = fantasma ? new Color(0x6E4A38) : PIEL;
        Color pielLuz = fantasma ? new Color(0x8A6246) : new Color(0xE0B487);
        Color camisa = fantasma ? new Color(0x18241F) : new Color(0x2E3B36);
        Color camisaLuz = fantasma ? new Color(0x223029) : new Color(0x41524A);
        Color delantal = fantasma ? new Color(0x26342E) : new Color(0xB8B2A0);
        Color delantalSombra = fantasma ? new Color(0x1B2621) : new Color(0x8E8878);
        Color pantalon = fantasma ? new Color(0x101714) : new Color(0x1E2623);
        Color zapato = fantasma ? new Color(0x080C0A) : new Color(0x0E1210);

        // contorno: mantiene la silueta legible sobre cualquier piso
        sr(g, x, y, dir, 1, 0, 8, 7, contorno);
        sr(g, x, y, dir, 0, 5, 10, 9, contorno);
        sr(g, x, y, dir, 1, 11, 3, 8, contorno);
        sr(g, x, y, dir, 6, 11, 3, 8, contorno);

        // piernas
        int pierna = frame == 0 ? 0 : 1;
        sr(g, x, y, dir, 2 + pierna, 12, 2, 5, pantalon);
        sr(g, x, y, dir, 6 - pierna, 12, 2, 5, pantalon);
        sr(g, x, y, dir, 2 + pierna, 12, 1, 4, new Color(0x2A3531));
        sr(g, x, y, dir, 1 + pierna, 16, 3, 3, zapato);
        sr(g, x, y, dir, 6 - pierna, 16, 3, 3, zapato);
        sr(g, x, y, dir, 2 + pierna, 16, 2, 1, new Color(0x222A26));

        // torso
        sr(g, x, y, dir, 1, 6, 8, 7, camisa);
        sr(g, x, y, dir, 2, 6, 3, 7, camisaLuz);
        sr(g, x, y, dir, 7, 6, 2, 7, new Color(0x1D2723));
        sr(g, x, y, dir, 2, 7, 6, 6, delantal);
        sr(g, x, y, dir, 2, 7, 1, 6, delantalSombra);
        sr(g, x, y, dir, 3, 8, 4, 2, new Color(0xA8A290));
        // bolsillo y mancha del delantal
        sr(g, x, y, dir, 4, 10, 3, 2, delantalSombra);
        sr(g, x, y, dir, 5, 11, 1, 1, new Color(0x7A322C));
        // brazos
        sr(g, x, y, dir, 0, 7, 2, 5, camisaLuz);
        sr(g, x, y, dir, 0, 11, 2, 2, piel);
        sr(g, x, y, dir, 8, 7, 2, 5, new Color(0x1D2723));
        sr(g, x, y, dir, 8, 11, 2, 2, piel);
        // cabeza
        sr(g, x, y, dir, 2, 1, 6, 6, piel);
        sr(g, x, y, dir, 2, 1, 2, 3, pielLuz);
        sr(g, x, y, dir, 2, 0, 6, 3, pelo);
        sr(g, x, y, dir, 1, 2, 1, 4, pelo);
        sr(g, x, y, dir, 7, 1, 1, 3, pelo);
        sr(g, x, y, dir, 1, 4, 1, 2, piel);
        // ojos, ceja y boca
        Color ojo = fantasma ? new Color(0x8E2F2A) : new Color(0x120F0C);
        sr(g, x, y, dir, 3, 3, 1, 1, ojo);
        sr(g, x, y, dir, 6, 3, 1, 1, ojo);
        if (!fantasma) {
            sr(g, x, y, dir, 3, 2, 2, 1, new Color(0x3A2A20));
            sr(g, x, y, dir, 4, 5, 2, 1, PIEL_OSCURA);
        } else {
            sr(g, x, y, dir, 3, 2, 2, 1, new Color(0x4A2620));
        }
        // cuello y pañuelo
        sr(g, x, y, dir, 4, 6, 3, 1, new Color(0x8E2F2A));
        sr(g, x, y, dir, 5, 7, 1, 2, new Color(0x6E231F));
    }

    /** Dibuja un rectángulo espejado horizontalmente según la mirada. */
    private static void sr(Graphics2D g, int x, int y, int dir, int ox, int oy, int w, int h, Color color) {
        int rx = dir > 0 ? x + ox : x + 9 - ox - w;
        r(g, rx, y + oy, w, h, color);
    }

    private void figuraCliente(Graphics2D g, int cx, int pies, int semilla) {
        sombra(g, cx - 6, pies - 2, 13, 4);
        int bob = (tick / 20 + semilla) % 2;
        int y = pies - 22 + bob;
        Color contorno = new Color(0x070A09);
        Color abrigo = semilla == 0 ? new Color(0x2A3530)
                : semilla == 1 ? new Color(0x3A2E2A) : new Color(0x26323A);
        Color abrigoLuz = mezclar(abrigo, Color.WHITE, 0.16);
        Color abrigoSombra = mezclar(abrigo, Color.BLACK, 0.36);
        Color pantalon = new Color(0x161D1A);
        Color zapato = new Color(0x090D0B);
        Color piel = semilla == 1 ? new Color(0xC99A6B) : PIEL_OSCURA;
        Color pielLuz = mezclar(piel, Color.WHITE, 0.18);

        // piernas y zapatos
        r(g, cx - 3, y + 15, 3, 7, contorno);
        r(g, cx + 1, y + 15, 3, 7, contorno);
        r(g, cx - 2, y + 16, 2, 5, pantalon);
        r(g, cx + 2, y + 16, 2, 5, pantalon);
        r(g, cx - 3, y + 21, 4, 1, zapato);
        r(g, cx + 1, y + 21, 4, 1, zapato);

        // torso con abrigo y pliegues
        r(g, cx - 5, y + 6, 11, 10, contorno);
        r(g, cx - 4, y + 7, 9, 8, abrigo);
        r(g, cx - 4, y + 7, 2, 8, abrigoLuz);
        r(g, cx + 3, y + 7, 1, 8, abrigoSombra);
        r(g, cx - 1, y + 8, 1, 6, abrigoSombra);
        r(g, cx - 5, y + 7, 1, 6, abrigoSombra);
        r(g, cx + 4, y + 7, 1, 6, abrigoSombra);

        // manos que asoman del abrigo
        r(g, cx - 5, y + 13, 2, 2, piel);
        r(g, cx + 4, y + 13, 2, 2, piel);

        // cabeza, pelo y cara
        r(g, cx - 3, y, 7, 8, contorno);
        r(g, cx - 2, y + 1, 5, 6, piel);
        r(g, cx - 2, y + 1, 2, 2, pielLuz);
        r(g, cx - 3, y, 7, 2, new Color(0x171310));
        r(g, cx - 3, y + 1, 2, 3, new Color(0x171310));
        r(g, cx + 2, y + 1, 2, 2, new Color(0x171310));
        r(g, cx - 1, y + 4, 1, 1, new Color(0xD9A441));
        r(g, cx + 2, y + 4, 1, 1, new Color(0xD9A441));
        r(g, cx, y + 6, 2, 1, new Color(0x4A3028));

        // con poca cordura, los ojos se prenden en rojo
        if (juego.getJugador().getCordura() < 45 && (tick / 13 + semilla) % 7 == 0) {
            r(g, cx - 2, y + 4, 1, 1, new Color(0xE07A62));
            r(g, cx + 3, y + 4, 1, 1, new Color(0xE07A62));
        }
    }

    private void dibujarObjetoEnMano(Graphics2D g, int px, int py, int dir) {
        Objeto objeto = juego.getEnMano();
        if (objeto == null) {
            return;
        }
        int ox = dir > 0 ? px + 7 : px - 19;
        if ("PEDIDO".equals(objeto.getTipoObjeto())) {
            r(g, ox, py, 12, 2, METAL);
            r(g, ox + 1, py - 2, 10, 2, new Color(0xD0D8CE));
            r(g, ox + 3, py - 4, 6, 2, new Color(0xE8E4D4));
            r(g, ox + 6, py - 5, 2, 1, MOSTAZA);
        } else if (objeto instanceof Herramienta h) {
            dibujarHerramienta(g, h.getLugar(), ox + 3, py - 4);
        } else {
            dibujarObjeto(g, objeto, ox + 3, py - 3);
        }
    }

    private void dibujarParticulas(Graphics2D g) {
        for (Particula p : particulas) {
            int alfa = Math.min(120, p.vida / 6);
            Color color;
            if (p.tipo == 1) {
                color = new Color(200, 220, 210, alfa);
            } else if (p.tipo == 2) {
                color = new Color(120, 180, 170, alfa);
            } else {
                color = new Color(217, 190, 130, alfa);
            }
            g.setColor(color);
            g.fillRect((int) p.x, (int) p.y, 1, 1);
        }
    }

    // ------------------------------------------------------------------ luz

    private void pintarLuz(Graphics2D g) {
        if (mascaraLuz == null) {
            mascaraLuz = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
            pixelesLuz = new int[ANCHO * ALTO];
        }
        int cordura = Math.max(0, Math.min(100, juego.getJugador().getCordura()));
        boolean apagon = juego.hayTareaPendiente(Lugar.TABLERO);
        float ambiente = apagon ? 0.035f : 0.60f - (100 - cordura) * 0.0024f;
        List<Foco> focos = new ArrayList<>(apagon ? focosApagon() : focosDeSala());
        if (apagon) {
            // Cada sombra arrastra su propia penumbra: se ven venir, pero siguen siendo
            // negro.
            for (Sombra s : sombras) {
                focos.add(new Foco((int) Math.round(s.x), (int) Math.round(s.y) - 12, 30, 0.34f, true));
            }
        }
        float pulso = (float) (0.88 + Math.sin(tick * 0.08) * 0.08);
        boolean linterna = juego.tieneLinterna();
        float haloRadio2 = apagon ? (linterna ? 8100f : 3600f) : 2200f;
        float haloIntensidad = apagon ? (linterna ? 0.90f : 0.58f) : 0.22f;
        if (apagon) {
            haloIntensidad *= (float) (0.90 + Math.sin(tick * 0.19) * 0.10);
        }
        int[] pixeles = pixelesLuz;

        for (int yy = 0; yy < ALTO; yy++) {
            for (int xx = 0; xx < ANCHO; xx++) {
                float luz = ambiente;
                for (Foco f : focos) {
                    float dx = xx - f.x;
                    float dy = yy - f.y;
                    float d2 = dx * dx + dy * dy;
                    float radio2 = f.r2();
                    if (d2 < radio2) {
                        float intensidad = f.i;
                        if (f.parpadea) {
                            intensidad *= pulso;
                        }
                        luz += intensidad * (1f - d2 / radio2);
                    }
                }
                // halo del jugador: en un apagón es lo único que lo acompaña
                float pdx = xx - (float) x;
                float pdy = yy - (float) y;
                float pd2 = pdx * pdx + pdy * pdy;
                if (pd2 < haloRadio2) {
                    luz += haloIntensidad * (1f - pd2 / haloRadio2);
                }
                // viñeta
                float vx = (xx - ANCHO / 2f) / (ANCHO / 2f);
                float vy = (yy - ALTO / 2f) / (ALTO / 2f);
                float viñeta = Math.min(1f, vx * vx + vy * vy);
                luz *= 1f - viñeta * 0.22f;
                luz = Math.max(0, Math.min(1, luz));

                int alfa = (int) Math.round((1f - luz) * 208f);
                // bandas de 4 niveles + tramado para que no queden degradés suaves
                int nivel = alfa / 22;
                alfa = nivel * 22;
                if (((xx + yy) & 1) == 0 && (alfa % 44) == 0) {
                    alfa = Math.min(235, alfa + 12);
                }
                pixeles[yy * ANCHO + xx] = (alfa << 24);
            }
        }
        mascaraLuz.setRGB(0, 0, ANCHO, ALTO, pixeles, 0, ANCHO);
        g.drawImage(mascaraLuz, 0, 0, null);
    }

    private record Foco(int x, int y, int r, float i, boolean parpadea) {
        float r2() {
            return (float) r * r;
        }
    }

    /**
     * Durante un apagón no quedan lámparas de techo: solo el chispazo del tablero,
     * la vela del
     * conserje y una que otra rendija. La linterna del jugador se suma desde el
     * halo.
     */
    private List<Foco> focosApagon() {
        List<Foco> focos = new ArrayList<>();
        switch (sala) {
            case COMEDOR -> {
                focos.add(new Foco(30, 78, 54, 0.46f, true));
                focos.add(new Foco(10, 128, 34, 0.20f, false));
                focos.add(new Foco(374, 128, 34, 0.20f, false));
            }
            case BANO -> {
                focos.add(new Foco(374, 128, 32, 0.20f, false));
                focos.add(new Foco(244, 44, 30, 0.12f, false));
            }
            case COCINA -> {
                focos.add(new Foco(10, 128, 32, 0.20f, false));
                focos.add(new Foco(192, 192, 42, 0.26f, false));
            }
            case ALMACEN -> {
                focos.add(new Foco(342, 188, 38, 0.62f, true));
                focos.add(new Foco(192, 192, 38, 0.20f, false));
            }
            default -> {
            }
        }
        return focos;
    }

    private List<Foco> focosDeSala() {
        boolean tableroRoto = juego.hayTareaPendiente(Lugar.TABLERO);
        List<Foco> focos = new ArrayList<>();
        switch (sala) {
            case COMEDOR:
                focos.add(new Foco(148, 28, 118, 0.62f, false));
                focos.add(new Foco(286, 32, 92, 0.42f, false));
                focos.add(new Foco(192, 126, 88, 0.38f, false));
                focos.add(new Foco(10, 128, 50, 0.32f, false));
                focos.add(new Foco(374, 128, 50, 0.32f, false));
                focos.add(new Foco(30, 88, 36, 0.22f, tableroRoto));
                break;
            case BANO:
                focos.add(new Foco(194, 10, 96, 0.52f, true));
                focos.add(new Foco(244, 44, 58, 0.30f, false));
                focos.add(new Foco(300, 44, 58, 0.30f, false));
                focos.add(new Foco(374, 128, 44, 0.30f, false));
                break;
            case COCINA:
                focos.add(new Foco(92, 20, 92, 0.50f, false));
                focos.add(new Foco(300, 18, 84, 0.40f, false));
                focos.add(new Foco(122, 78, 64, 0.28f, false));
                focos.add(new Foco(350, 108, 56, 0.28f, false));
                focos.add(new Foco(192, 224, 50, 0.34f, false));
                break;
            case ALMACEN:
                focos.add(new Foco(192, 24, 118, 0.50f, true));
                focos.add(new Foco(62, 74, 42, 0.22f, false));
                focos.add(new Foco(62, 148, 40, 0.20f, false));
                focos.add(new Foco(192, 24, 62, 0.26f, false));
                focos.add(new Foco(342, 188, 30, 0.16f, true));
                break;
            default:
                break;
        }
        return focos;
    }

    private void pintarCordura(Graphics2D g) {
        int cordura = juego.getJugador().getCordura();
        if (cordura < 35) {
            int fuerza = (35 - cordura) * 3;
            g.setColor(new Color(110, 12, 12, Math.min(70, fuerza)));
            g.fillRect(0, 0, ANCHO, ALTO);
            // latido de oscuridad en los bordes
            int pulso = (int) (Math.sin(tick * 0.12) * 10);
            g.setColor(new Color(0, 0, 0, 40 + pulso));
            g.fillRect(0, 0, ANCHO, 8);
            g.fillRect(0, ALTO - 8, ANCHO, 8);
            g.fillRect(0, 0, 8, ALTO);
            g.fillRect(ANCHO - 8, 0, 8, ALTO);
        }
    }

    // ------------------------------------------------------------------ iconos

    public static void dibujarHerramienta(Graphics2D g, Lugar lugar, int x, int y) {
        switch (lugar) {
            case BANO:
                r(g, x + 5, y, 1, 9, new Color(0x6B4E32));
                r(g, x + 3, y + 8, 5, 3, new Color(0x8A9691));
                for (int i = 0; i < 4; i++) {
                    r(g, x + 2 + i, y + 10, 1, 2, new Color(0xABB7B0));
                }
                break;
            case MESAS:
                r(g, x + 1, y + 3, 9, 5, new Color(0xD9C56B));
                r(g, x + 1, y + 3, 9, 1, new Color(0xF0E0A0));
                r(g, x + 3, y + 5, 2, 1, new Color(0x8A7A3A));
                r(g, x + 6, y + 6, 2, 1, new Color(0x8A7A3A));
                break;
            case BASURA:
                r(g, x + 6, y, 1, 8, new Color(0x6B4E32));
                r(g, x + 2, y + 7, 9, 3, new Color(0x8A6844));
                for (int i = 0; i < 5; i++) {
                    r(g, x + 2 + i * 2, y + 10, 1, 2, new Color(0xB08A5A));
                }
                break;
            case TABLERO:
                r(g, x + 1, y + 4, 9, 4, new Color(0x8A9691));
                r(g, x + 2, y + 5, 7, 2, new Color(0xC8D2C0));
                r(g, x, y + 4, 2, 4, new Color(0xB08A5A));
                r(g, x + 9, y + 4, 2, 4, new Color(0xB08A5A));
                break;
            case PLANCHA:
                r(g, x + 5, y, 2, 7, new Color(0x6B4E32));
                r(g, x + 2, y + 6, 8, 4, new Color(0x9AA8A0));
                r(g, x + 2, y + 6, 8, 1, new Color(0xC8D2C0));
                break;
            case CAFETERA:
                r(g, x + 2, y + 1, 6, 2, new Color(0x8A9691));
                r(g, x + 1, y + 3, 2, 5, new Color(0x8A9691));
                r(g, x + 7, y + 3, 2, 5, new Color(0x8A9691));
                r(g, x + 3, y + 7, 4, 2, new Color(0x8A9691));
                r(g, x + 3, y + 3, 1, 1, new Color(0x2A2E2A));
                break;
            case HELADERA:
            default:
                r(g, x + 1, y + 2, 3, 5, new Color(0x6D8FA8));
                r(g, x + 2, y + 7, 2, 2, new Color(0x4A6A82));
                r(g, x + 5, y + 1, 3, 6, new Color(0x6D8FA8));
                r(g, x + 6, y + 7, 2, 2, new Color(0x4A6A82));
                break;
        }
    }

    public static void dibujarObjeto(Graphics2D g, Objeto objeto, int x, int y) {
        if (objeto instanceof Herramienta h) {
            dibujarHerramienta(g, h.getLugar(), x, y);
            return;
        }
        String nombre = objeto.getNombre() == null ? "" : objeto.getNombre().toLowerCase();
        if (nombre.contains("bandeja")) {
            r(g, x + 1, y + 5, 11, 2, METAL);
            r(g, x + 2, y + 3, 9, 2, new Color(0xD0D8CE));
            r(g, x + 4, y + 1, 5, 2, new Color(0xE8E4D4));
            return;
        }
        if (nombre.contains("café") || nombre.contains("cafe")) {
            r(g, x + 3, y + 3, 6, 6, new Color(0xD8D2C0));
            r(g, x + 4, y + 4, 4, 3, new Color(0x3A2620));
            r(g, x + 9, y + 5, 2, 2, new Color(0xD8D2C0));
            r(g, x + 4, y + 1, 1, 1, new Color(0xC8D2C8));
            r(g, x + 7, y, 1, 1, new Color(0xC8D2C8));
            return;
        }
        if (nombre.contains("gaseosa")) {
            r(g, x + 4, y + 1, 4, 9, new Color(0x4A6B52));
            r(g, x + 5, y + 2, 2, 4, new Color(0x8AB08A));
            r(g, x + 4, y, 4, 2, new Color(0xD9A441));
            return;
        }
        if (nombre.contains("jabón") || nombre.contains("jabon")) {
            r(g, x + 2, y + 4, 8, 5, new Color(0xD8A8A0));
            r(g, x + 3, y + 5, 6, 2, new Color(0xF0C8C0));
            return;
        }
        if (nombre.contains("propina") || nombre.contains("moneda")) {
            Pixel.elipse(g, x + 3, y + 3, 6, 6, new Color(0xD9A441));
            Pixel.elipse(g, x + 4, y + 4, 4, 4, new Color(0xF0C56B));
            r(g, x + 5, y + 5, 2, 1, new Color(0xB8862E));
            return;
        }
        if (nombre.contains("nota")) {
            r(g, x + 2, y + 1, 8, 10, new Color(0xD8D2C0));
            r(g, x + 4, y + 3, 4, 1, new Color(0x6A5A4A));
            r(g, x + 4, y + 5, 5, 1, new Color(0x6A5A4A));
            r(g, x + 4, y + 7, 3, 1, new Color(0x6A5A4A));
            return;
        }
        if (nombre.contains("linterna")) {
            r(g, x + 2, y + 4, 7, 4, new Color(0xD9A441));
            r(g, x + 1, y + 3, 3, 6, new Color(0x8A9691));
            r(g, x + 9, y + 4, 2, 4, new Color(0xF0E0A0));
            return;
        }
        if (nombre.contains("llave")) {
            r(g, x + 2, y + 4, 6, 2, new Color(0x8A9691));
            r(g, x + 7, y + 3, 3, 4, new Color(0x8A9691));
            r(g, x + 8, y + 4, 1, 2, new Color(0x2A2E2A));
            return;
        }
        // objeto genérico
        r(g, x + 3, y + 2, 6, 8, new Color(0x8A9691));
        r(g, x + 4, y + 3, 4, 2, new Color(0xC8D2C0));
    }
}
