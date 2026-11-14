package ui;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades de dibujo para la pantalla de juego: fuente pixel, paneles, barras y texto.
 * Todo se dibuja sin antialias y en coordenadas enteras para que cada píxel quede nítido.
 */
public final class Pixel {

    // Un punto más que antes en los tamaños chicos: en 384x240 cada píxel cuenta
    // y la letra de 6 px se volvía difícil de leer sobre el arte.
    public static final int T_CHICO = 7;
    public static final int T_CUERPO = 9;
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

    private static final Font REGULAR = cargar("Silkscreen-Regular.ttf");
    private static final Font NEGRITA = cargar("Silkscreen-Bold.ttf");

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

    public static Font fuente(int tam, boolean negrita) {
        return (negrita ? NEGRITA : REGULAR).deriveFont((float) tam);
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

    public static void textoDerecha(Graphics2D g, String texto, int derechaX, int y, int tam, Color color) {
        textoDerecha(g, texto, derechaX, y, tam, color, false);
    }

    public static void textoDerecha(Graphics2D g, String texto, int derechaX, int y, int tam, Color color,
                                    boolean negrita) {
        texto(g, texto, derechaX - ancho(g, texto, tam, negrita), y, tam, color, negrita);
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

    public static int altoLinea(int tam) {
        return tam + 2;
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

    /** Divide un texto en líneas que entren en el ancho indicado. */
    public static List<String> envolver(Graphics2D g, String texto, int maxAncho, int tam) {
        List<String> salida = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return salida;
        }
        for (String parrafo : texto.split("\n", -1)) {
            String[] palabras = parrafo.trim().split("\\s+");
            StringBuilder linea = new StringBuilder();
            for (String palabra : palabras) {
                String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;
                if (ancho(g, prueba, tam) <= maxAncho || linea.isEmpty()) {
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

    /** Barra segmentada de pixel art. */
    public static void barra(Graphics2D g, int x, int y, int w, int h, int valor, int maximo,
                             Color relleno, Color fondo, Color borde) {
        g.setColor(fondo);
        g.fillRect(x, y, w, h);
        int segmentos = Math.max(1, w / 7);
        int ancho = Math.max(1, (w - 2) / segmentos);
        int llenos = (int) Math.round((double) Math.max(0, Math.min(maximo, valor)) / maximo * segmentos);
        for (int i = 0; i < segmentos; i++) {
            g.setColor(i < llenos ? relleno : new Color(0x1E2A24));
            g.fillRect(x + 1 + i * ancho, y + 1, Math.max(1, ancho - 1), h - 2);
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
        textoCentrado(g, etiqueta, x + w / 2, y + (h + T_CUERPO) / 2 - 1, T_CUERPO,
                elegido ? CREMA : GRIS, elegido);
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
}
