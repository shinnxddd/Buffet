package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * Paleta, fuentes y fábricas de componentes: todo el aspecto de la app vive acá.
 * La idea es un buffet escolar abandonado: verde pizarrón, madera gastada y mostaza de cartel.
 */
public final class Tema {

    // --- paleta base -------------------------------------------------------
    static final Color FONDO = new Color(0x141E1A);
    static final Color FONDO_ALT = new Color(0x0D1512);
    static final Color TARJETA = new Color(0x1F2B26);
    static final Color TARJETA_ALT = new Color(0x18231F);
    static final Color BORDE = new Color(0x33463E);
    static final Color BORDE_SUAVE = new Color(0x27362F);
    static final Color TEXTO = new Color(0xEDE8D8);
    static final Color TEXTO_SUAVE = new Color(0x9DAFA4);
    static final Color ACENTO = new Color(0xD9A441);
    static final Color ACENTO_SUAVE = new Color(0x8A6A2C);
    static final Color SOBRE_ACENTO = new Color(0x1E1A10);
    static final Color PELIGRO = new Color(0xC4513F);
    static final Color BIEN = new Color(0x7FB58A);
    static final Color MADERA = new Color(0x6B4E32);

    // --- tipografías -------------------------------------------------------
    static final Font TITULO = new Font("Georgia", Font.BOLD, 42);
    static final Font SUBTITULO = new Font("Georgia", Font.BOLD, 22);
    static final Font ESCENA = new Font("Georgia", Font.BOLD, 30);
    static final Font CUERPO = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font CUERPO_GRANDE = new Font("Segoe UI", Font.PLAIN, 18);

    static {
        // Georgia y Segoe UI existen en Windows; si no, Java cae a una serif/sans razonable.
        UIManager.put("defaultFont", CUERPO);
    }

    private Tema() { }

    public static void instalar() {
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@background", "#141E1A",
                "@foreground", "#EDE8D8",
                "@accentColor", "#D9A441",
                "@componentBorderColor", "#33463E",
                "@selectionBackground", "#8A6A2C",
                "@selectionForeground", "#EDE8D8"));
        FlatDarkLaf.setup();

        // Esquinas redondeadas y detalles finos en todos los controles.
        UIManager.put("Button.arc", 16);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderWidth", 1);
        UIManager.put("Button.margin", new Insets(8, 18, 8, 18));
        UIManager.put("Button.default.background", "#D9A441");
        UIManager.put("Button.default.foreground", "#1E1A10");
        UIManager.put("Button.hoverBackground", "#2F423A");
        UIManager.put("Button.pressedBackground", "#24332D");
        UIManager.put("Button.focusedBackground", "#2A3C34");
        UIManager.put("Button.disabledBackground", "#1B2622");
        UIManager.put("Button.disabledText", "#617169");

        UIManager.put("Panel.background", "#141E1A");
        UIManager.put("ScrollPane.background", "#141E1A");
        UIManager.put("Viewport.background", "#141E1A");
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", "#141E1A");
        UIManager.put("ScrollBar.thumb", "#33463E");
        UIManager.put("ScrollBar.hoverThumb", "#466056");
        UIManager.put("ScrollBar.pressedThumb", "#5A776B");

        UIManager.put("Table.rowHeight", 36);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.background", "#1F2B26");
        UIManager.put("Table.foreground", "#EDE8D8");
        UIManager.put("Table.selectionBackground", "#2F423A");
        UIManager.put("Table.selectionForeground", "#EDE8D8");
        UIManager.put("Table.gridColor", "#27362F");

        UIManager.put("TableHeader.background", "#18231F");
        UIManager.put("TableHeader.foreground", "#9DAFA4");
        UIManager.put("TableHeader.separatorColor", "#27362F");
        UIManager.put("TableHeader.height", 38);

        UIManager.put("TextField.background", "#18231F");
        UIManager.put("TextField.foreground", "#EDE8D8");
        UIManager.put("TextField.caretForeground", "#D9A441");
        UIManager.put("TextField.placeholderForeground", "#617169");
        UIManager.put("PasswordField.background", "#18231F");
        UIManager.put("PasswordField.caretForeground", "#D9A441");

        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ProgressBar.background", "#26342E");
        UIManager.put("ProgressBar.foreground", "#7FB58A");
        UIManager.put("ProgressBar.height", 10);

        UIManager.put("OptionPane.background", TARJETA);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("OptionPane.messageFont", CUERPO);
        UIManager.put("OptionPane.buttonFont", CUERPO);
        UIManager.put("ToolTip.background", "#0D1512");
        UIManager.put("ToolTip.foreground", "#EDE8D8");
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDE, 1, true));
    }

    static JPanel fondo(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        return p;
    }

    /** Tarjeta con esquina redondeada, degradé sutil y una sombra mínima. */
    static JPanel tarjeta(LayoutManager layout) {
        PanelTarjeta p = new PanelTarjeta(layout);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        return p;
    }

    /** Variante de tarjeta más apretada, para filas de inventario o listas. */
    static JPanel tarjetaCompacta(LayoutManager layout) {
        PanelTarjeta p = new PanelTarjeta(layout);
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        return p;
    }

    static JLabel etiqueta(String texto, Font fuente, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente);
        l.setForeground(color);
        return l;
    }

    static JTextArea texto(Font fuente, Color color) {
        JTextArea t = new JTextArea();
        t.setEditable(false);
        t.setFocusable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setBackground(TARJETA);   // siempre van sobre una tarjeta; no depende de la transparencia del tema
        t.setBorder(null);
        t.setFont(fuente);
        t.setForeground(color);
        return t;
    }

    private static JButton boton(String texto, Color fondo, Color letra) {
        JButton b = new JButton(texto);
        b.setBackground(fondo);
        b.setForeground(letra);
        b.setFont(CUERPO.deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(8, 18, 8, 18));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    static JButton primario(String texto) {
        JButton b = boton(texto, ACENTO, SOBRE_ACENTO);
        b.putClientProperty("JButton.hoverBackground", "#E6B45A");
        b.putClientProperty("JButton.pressedBackground", "#C08F31");
        return b;
    }

    static JButton secundario(String texto) { return boton(texto, BORDE, TEXTO); }

    static JButton peligro(String texto) { return boton(texto, PELIGRO, Color.WHITE); }

    /** Botón grande de la barra de acciones del juego. */
    static JButton accion(String texto) {
        JButton b = boton(texto, BORDE, TEXTO);
        b.setFont(CUERPO.deriveFont(Font.BOLD, 15f));
        b.setMargin(new Insets(14, 18, 14, 18));
        return b;
    }

    /** Panel con fondo redondeado dibujado a mano para que no dependa del look and feel. */
    private static final class PanelTarjeta extends JPanel {

        private static final int ARCO = 18;

        PanelTarjeta(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 4;
            int h = getHeight() - 5;
            if (w > 2 && h > 2) {
                // sombra
                g.setColor(new Color(0, 0, 0, 60));
                g.fillRoundRect(3, 4, w, h, ARCO, ARCO);
                // cuerpo
                g.setPaint(new GradientPaint(0, 0, TARJETA, 0, h, TARJETA_ALT));
                g.fillRoundRect(0, 0, w, h, ARCO, ARCO);
                // borde
                g.setColor(BORDE);
                g.drawRoundRect(0, 0, w - 1, h - 1, ARCO, ARCO);
                // línea de luz superior
                g.setColor(new Color(255, 255, 255, 10));
                g.drawRoundRect(1, 1, w - 3, h - 3, ARCO - 2, ARCO - 2);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
