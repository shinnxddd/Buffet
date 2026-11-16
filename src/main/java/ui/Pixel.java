package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades de dibujo para la pantalla de juego: fuentes, paneles, barras y texto.
 * Todo se dibuja sin antialias y en coordenadas enteras para que cada píxel quede nítido.
 *
 * Toda la interfaz usa VT323: conserva el aspecto pixel art y distingue bien cada
 * carácter incluso en tamaños pequeños. El texto se dibuja sin suavizado para mantener
 * los bordes nítidos al escalar el lienzo.
 */
public final class Pixel {

    public static final int T_CHICO = 9;
    public static final int T_CUERPO = 11;
    public static final int T_SUBTITULO = 13;
    public static final int T_TITULO = 18;
    public static final int T_GRANDE = 26;

    public static final Color NEGRO = new Color(0x070A09);
    public static final Color CARBON = new Color(0x111815);
    public static final Color PANEL = new Color(0x0E1512, true);
    public static final Color PANEL_OPACO = new Color(0x0E1512);
    public static final Color BORDE = new Color(0x33463E);
    public static final Color BORDE_CLARO = new Color(0x51695C);
    public static final Color CREMA = new Color(0xEDE8D8);
    public static final Color GRIS = new Color(0x9DAFA4);
    public static final Color GRIS_OSCURO = new Color(0x5E7268);
    public static final Color MOSTAZA = new Color(0xD9A441);
    public static final Color MOSTAZA_CLARA = new Color(0xF0C56B);
    public static final Color ROJO = new Color(0xC4513F);
    public static final Color ROJO_CLARO = new Color(0xE07A62);
    public static final Color VERDE = new Color(0x7FB58A);
    public static final Color VERDE_OSCURO = new Color(0x3F6B52);
    public static final Color AZUL = new Color(0x6D8FA8);

    private static final Font DISPLAY = cargar("VT323-Regular.ttf");
    private static final Font DISPLAY_NEGRITA = DISPLAY.deriveFont(Font.BOLD);
    private static final Font TEXTO = cargar("VT323-Regular.ttf");

    private Pixel() { }

    private static Font cargar(String archivo) {
        try (InputStream in = Pixel.class.getResourceAsStream("/fonts/" + archivo)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (Exception ignorada) {
            // Si el recurso no está, seguimos con la monoespaciada del sistema.
        }
        return new Font("Monospaced", Font.PLAIN, 8);
    }

    /** Deja el contexto listo para pixel art: sin suavizado y con interpolación dura. */
    public static void preparar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Fuente de cuerpo: legible incluso a 9 px. */
    public static Font fuenteTexto(int tam) {
        return TEXTO.deriveFont((float) tam);
    }

    /** Fuente de títulos y carteles. Solo conviene usarla desde 13 px. */
    public static Font fuenteDisplay(int tam, boolean negrita) {
        return (negrita ? DISPLAY_NEGRITA : DISPLAY).deriveFont((float) tam);
    }

    /** Elige automáticamente la familia según el tamaño. */
    public static Font fuente(int tam, boolean negrita) {
        return tam >= T_SUBTITULO ? fuenteDisplay(tam, negrita) : fuenteTexto(tam);
    }

    public static void texto(Graphics2D g, String texto, int x, int y, int tam, Color color) {
        texto(g, texto, x, y, tam, color, false);
    }

    public static void texto(Graphics2D g, String texto, int x, int y, int tam, Color color, boolean negrita) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        g.setFont(fuente(tam, negrita));
        g.setColor(color);
        String[] lineas = texto.split("\n", -1);
        int alto = altoLinea(tam);
        for (int i = 0; i < lineas.length; i++) {
            g.drawString(lineas[i], x, y + i * alto);
        }
    }

    public static void textoDisplay(Graphics2D g, String texto, int x, int y, int tam, Color color) {
        textoDisplay(g, texto, x, y, tam, color, false);
    }

    public static void textoDisplay(Graphics2D g, String texto, int x, int y, int tam, Color color, boolean negrita) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        g.setFont(fuenteDisplay(tam, negrita));
        g.setColor(color);
        String[] lineas = texto.split("\n", -1);
        int alto = altoLinea(tam);
        for (int i = 0; i < lineas.length; i++) {
            g.drawString(lineas[i], x, y + i * alto);
        }
    }

    public static void textoCentrado(Graphics2D g, String texto, int centroX, int y, int tam, Color color) {
        textoCentrado(g, texto, centroX, y, tam, color, false);
    }

    public static void textoCentrado(Graphics2D g, String texto, int centroX, int y, int tam, Color color,
                                     boolean negrita) {
        if (texto == null) {
            return;
        }
        texto(g, texto, centroX - ancho(g, texto, tam, negrita) / 2, y, tam, color, negrita);
    }

    public static void textoCentradoDisplay(Graphics2D g, String texto, int centroX, int y, int tam, Color color,
                                            boolean negrita) {
        if (texto == null) {
            return;
        }
        textoDisplay(g, texto, centroX - anchoDisplay(g, texto, tam, negrita) / 2, y, tam, color, negrita);
    }

    public static void textoDerecha(Graphics2D g, String texto, int derechaX, int y, int tam, Color color) {
        textoDerecha(g, texto, derechaX, y, tam, color, false);
    }

    public static void textoDerecha(Graphics2D g, String texto, int derechaX, int y, int tam, Color color,
                                    boolean negrita) {
        texto(g, texto, derechaX - ancho(g, texto, tam, negrita), y, tam, color, negrita);
    }

    public static void textoDerechaDisplay(Graphics2D g, String texto, int derechaX, int y, int tam, Color color,
                                           boolean negrita) {
        textoDisplay(g, texto, derechaX - anchoDisplay(g, texto, tam, negrita), y, tam, color, negrita);
    }

    /** Texto con contorno de un píxel: clave para que se lea sobre el arte. */
    public static void textoBorde(Graphics2D g, String texto, int x, int y, int tam, Color color, Color borde) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    texto(g, texto, x + dx, y + dy, tam, borde, true);
                }
            }
        }
        texto(g, texto, x, y, tam, color, true);
    }

    public static int ancho(Graphics2D g, String texto, int tam) {
        return ancho(g, texto, tam, false);
    }

    public static int ancho(Graphics2D g, String texto, int tam, boolean negrita) {
        if (texto == null || texto.isEmpty()) {
            return 0;
        }
        g.setFont(fuente(tam, negrita));
        FontMetrics fm = g.getFontMetrics();
        int maximo = 0;
        for (String linea : texto.split("\n", -1)) {
            maximo = Math.max(maximo, fm.stringWidth(linea));
        }
        return maximo;
    }

    public static int anchoDisplay(Graphics2D g, String texto, int tam, boolean negrita) {
        if (texto == null || texto.isEmpty()) {
            return 0;
        }
        g.setFont(fuenteDisplay(tam, negrita));
        FontMetrics fm = g.getFontMetrics();
        int maximo = 0;
        for (String linea : texto.split("\n", -1)) {
            maximo = Math.max(maximo, fm.stringWidth(linea));
        }
        return maximo;
    }

    public static int altoLinea(int tam) {
        return tam + (tam >= T_SUBTITULO ? 4 : 2);
    }

    /** Corta una línea con puntos suspensivos si no entra en el ancho dado. */
    public static String recortar(Graphics2D g, String texto, int maxAncho, int tam, boolean negrita) {
        if (texto == null) {
            return "";
        }
        if (ancho(g, texto, tam, negrita) <= maxAncho) {
            return texto;
        }
        String puntos = "...";
        int i = texto.length();
        while (i > 0 && ancho(g, texto.substring(0, i) + puntos, tam, negrita) > maxAncho) {
            i--;
        }
        return texto.substring(0, Math.max(0, i)).stripTrailing() + puntos;
    }

    public static String recortarDisplay(Graphics2D g, String texto, int maxAncho, int tam, boolean negrita) {
        if (texto == null) {
            return "";
        }
        if (anchoDisplay(g, texto, tam, negrita) <= maxAncho) {
            return texto;
        }
        String puntos = "...";
        int i = texto.length();
        while (i > 0 && anchoDisplay(g, texto.substring(0, i) + puntos, tam, negrita) > maxAncho) {
            i--;
        }
        return texto.substring(0, Math.max(0, i)).stripTrailing() + puntos;
    }

    /** Divide un texto en líneas que entren en el ancho indicado. */
    public static List<String> envolver(Graphics2D g, String texto, int maxAncho, int tam) {
        return envolver(g, texto, maxAncho, tam, false);
    }

    public static List<String> envolver(Graphics2D g, String texto, int maxAncho, int tam, boolean negrita) {
        List<String> salida = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return salida;
        }
        for (String parrafo : texto.split("\n", -1)) {
            String[] palabras = parrafo.trim().split("\\s+");
            StringBuilder linea = new StringBuilder();
            for (String palabra : palabras) {
                String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;
                if (ancho(g, prueba, tam, negrita) <= maxAncho || linea.isEmpty()) {
                    linea.setLength(0);
                    linea.append(prueba);
                } else {
                    salida.add(linea.toString());
                    linea.setLength(0);
                    linea.append(palabra);
                }
            }
            if (!linea.isEmpty()) {
                salida.add(linea.toString());
            }
        }
        return salida;
    }

    /** Párrafo con salto automático y un máximo de líneas. */
    public static int parrafo(Graphics2D g, String texto, int x, int y, int maxAncho, int maxLineas,
                              int tam, Color color) {
        List<String> lineas = envolver(g, texto, maxAncho, tam);
        int alto = altoLinea(tam);
        int n = Math.min(maxLineas, lineas.size());
        for (int i = 0; i < n; i++) {
            String linea = lineas.get(i);
            if (i == n - 1 && lineas.size() > n) {
                linea = recortar(g, linea + "...", maxAncho, tam, false);
            }
            texto(g, linea, x, y + i * alto, tam, color);
        }
        return n * alto;
    }

    /** Panel de pixel art con sombra, borde y una luz superior. */
    public static void panel(Graphics2D g, int x, int y, int w, int h, Color fondo, Color borde) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(x + 3, y + 3, w, h);
        g.setColor(fondo);
        g.fillRect(x, y, w, h);
        g.setColor(borde);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(new Color(255, 255, 255, 26));
        g.fillRect(x + 1, y + 1, w - 2, 1);
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(x + 1, y + h - 2, w - 2, 1);
    }

    public static void panel(Graphics2D g, int x, int y, int w, int h) {
        panel(g, x, y, w, h, new Color(0x0E1512), BORDE);
    }

    /** Barra segmentada de pixel art. El último segmento llega justo al borde interno. */
    public static void barra(Graphics2D g, int x, int y, int w, int h, int valor, int maximo,
                             Color relleno, Color fondo, Color borde) {
        g.setColor(fondo);
        g.fillRect(x, y, w, h);
        int segmentos = Math.max(1, w / 7);
        int interior = Math.max(1, w - 2);
        int base = interior / segmentos;
        int extra = interior % segmentos;
        int llenos = maximo <= 0 ? 0
                : (int) Math.round((double) Math.max(0, Math.min(maximo, valor)) / maximo * segmentos);
        int cursor = x + 1;
        for (int i = 0; i < segmentos; i++) {
            int anchoSeg = base + (i < extra ? 1 : 0);
            g.setColor(i < llenos ? relleno : new Color(0x1E2A24));
            if (anchoSeg > 1 && h > 2) {
                g.fillRect(cursor, y + 1, anchoSeg - 1, h - 2);
            }
            cursor += anchoSeg;
        }
        g.setColor(borde);
        g.drawRect(x, y, w - 1, h - 1);
    }

    /** Elipse dibujada por filas de píxeles: sin bordes suavizados ni píxeles sueltos. */
    public static void elipse(Graphics2D g, int x, int y, int w, int h, Color color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.setColor(color);
        double rx = w / 2.0;
        double ry = h / 2.0;
        for (int fila = 0; fila < h; fila++) {
            double dy = (fila + 0.5 - ry) / Math.max(0.5, ry);
            double ancho = rx * Math.sqrt(Math.max(0, 1 - dy * dy));
            int mitad = (int) Math.round(ancho);
            if (mitad > 0) {
                g.fillRect(x + (int) Math.round(rx) - mitad, y + fila, mitad * 2, 1);
            }
        }
    }

    /** Panel translúcido para que el arte de abajo siga leyéndose. */
    public static void panelTenue(Graphics2D g, int x, int y, int w, int h, int alfa, Color borde) {
        g.setColor(new Color(7, 10, 9, Math.max(0, Math.min(255, alfa))));
        g.fillRect(x, y, w, h);
        if (borde != null) {
            g.setColor(borde);
            g.drawRect(x, y, w - 1, h - 1);
            g.setColor(new Color(255, 255, 255, 14));
            g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
        }
    }

    /** Botón de pixel art: 3 píxeles de alto por dentro para que parezca biselado. */
    public static void boton(Graphics2D g, int x, int y, int w, int h, String etiqueta,
                             boolean elegido, boolean peligro, Color acento) {
        Color base = peligro ? new Color(0x4A1B16) : new Color(0x18241F);
        Color borde = peligro ? new Color(0xB84A3C) : (elegido ? acento : BORDE);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(x + 2, y + 2, w, h);
        g.setColor(base);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(255, 255, 255, elegido ? 26 : 12));
        g.fillRect(x + 1, y + 1, w - 2, 1);
        g.setColor(borde);
        g.drawRect(x, y, w - 1, h - 1);
        if (elegido) {
            g.setColor(acento);
            g.fillRect(x, y, 2, h);
            g.fillRect(x, y, w, 1);
        }
        String corta = recortar(g, etiqueta, w - 8, T_CUERPO, false);
        textoCentrado(g, corta, x + w / 2, y + (h + T_CUERPO) / 2 - 1, T_CUERPO,
                elegido ? CREMA : GRIS);
    }

    /** Oscurece con un patrón de tablero de ajedrez para dar volumen sin degradés suaves. */
    public static void dither(Graphics2D g, int x, int y, int w, int h, Color a, Color b) {
        for (int yy = 0; yy < h; yy += 2) {
            for (int xx = 0; xx < w; xx += 2) {
                g.setColor((((x + xx) / 2 + (y + yy) / 2) & 1) == 0 ? a : b);
                g.fillRect(x + xx, y + yy, 2, 2);
            }
        }
    }

    /** Líneas horizontales tenues, tipo monitor viejo. */
    public static void scanlines(Graphics2D g, int ancho, int alto) {
        g.setColor(new Color(0, 0, 0, 22));
        for (int y = 0; y < alto; y += 3) {
            g.fillRect(0, y, ancho, 1);
        }
    }

    /**
     * Posición y escala entera del lienzo dentro de la ventana. La escala entera evita
     * píxeles deformados; el centrado reparte los márgenes sobrantes.
     */
    public record Encuadre(int escala, int x, int y, int ancho, int alto) {
        public boolean contiene(int mx, int my) {
            return mx >= x && mx < x + ancho && my >= y && my < y + alto;
        }

        public int lienzoX(int mx) {
            return (mx - x) / escala;
        }

        public int lienzoY(int my) {
            return (my - y) / escala;
        }
    }

    public static Encuadre calcularLienzo(int ventanaAncho, int ventanaAlto, int lienzoAncho, int lienzoAlto) {
        int escala = Math.max(1, Math.min(ventanaAncho / lienzoAncho, ventanaAlto / lienzoAlto));
        int ancho = lienzoAncho * escala;
        int alto = lienzoAlto * escala;
        return new Encuadre(escala, (ventanaAncho - ancho) / 2, (ventanaAlto - alto) / 2, ancho, alto);
    }

    /**
     * Dibuja el lienzo en pantalla completa: un fondo de textura oscura ocupa todo el
     * monitor y el lienzo nítido queda centrado con un marco. Así no aparecen franjas
     * negras muertas ni copias gigantes de la interfaz en 1920x1080.
     */
    public static void pintarPantalla(Graphics2D g, BufferedImage lienzo, int ventanaAncho, int ventanaAlto) {
        g.setColor(NEGRO);
        g.fillRect(0, 0, ventanaAncho, ventanaAlto);
        if (lienzo == null || ventanaAncho <= 0 || ventanaAlto <= 0) {
            return;
        }
        // Textura de tablero muy tenue: da profundidad sin repetir el contenido.
        g.setPaint(texturaFondo());
        g.fillRect(0, 0, ventanaAncho, ventanaAlto);
        g.setPaint(null);

        // Viñeta en tiras: más oscura hacia los bordes del monitor.
        for (int i = 0; i < 16; i++) {
            g.setColor(new Color(0, 0, 0, 64 - i * 3));
            g.fillRect(0, i * 4, ventanaAncho, 4);
            g.fillRect(0, ventanaAlto - 4 - i * 4, ventanaAncho, 4);
            g.fillRect(i * 8, 0, 8, ventanaAlto);
            g.fillRect(ventanaAncho - 8 - i * 8, 0, 8, ventanaAlto);
        }

        Encuadre encuadre = calcularLienzo(ventanaAncho, ventanaAlto, lienzo.getWidth(), lienzo.getHeight());
        int x = encuadre.x();
        int y = encuadre.y();
        int w = encuadre.ancho();
        int h = encuadre.alto();
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(x - 10, y - 10, w + 20, h + 20);
        g.setColor(new Color(0x0B1411));
        g.fillRect(x - 4, y - 4, w + 8, h + 8);
        g.setColor(BORDE);
        g.drawRect(x - 4, y - 4, w + 7, h + 7);
        g.setColor(new Color(255, 255, 255, 18));
        g.drawLine(x - 3, y - 3, x + w + 2, y - 3);
        g.drawImage(lienzo, x, y, w, h, null);
    }

    private static TexturePaint texturaFondo;

    private static TexturePaint texturaFondo() {
        if (texturaFondo == null) {
            BufferedImage tile = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
            Graphics2D tg = tile.createGraphics();
            tg.setColor(new Color(0x050807));
            tg.fillRect(0, 0, 8, 8);
            tg.setColor(new Color(0x07100D));
            tg.fillRect(0, 0, 4, 4);
            tg.fillRect(4, 4, 4, 4);
            tg.setColor(new Color(0x060B09));
            tg.fillRect(4, 0, 4, 4);
            tg.fillRect(0, 4, 4, 4);
            tg.setColor(new Color(0x0A1411));
            tg.fillRect(0, 0, 1, 1);
            tg.fillRect(4, 4, 1, 1);
            tg.dispose();
            texturaFondo = new TexturePaint(tile, new Rectangle(0, 0, 8, 8));
        }
        return texturaFondo;
    }
}
