package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Map;

/** Paleta neón sobre violeta oscuro, fuentes de píxeles y fábricas de componentes: todo el aspecto vive acá. */
public final class Tema {

    // Noche violeta con neones magenta / cian / amarillo
    static final Color FONDO = new Color(0x0B0714);
    static final Color TARJETA = new Color(0x150F2B);
    static final Color BORDE = new Color(0x3B2A73);
    static final Color TEXTO = new Color(0xF2EEFF);
    static final Color TEXTO_SUAVE = new Color(0x9A8FC9);
    static final Color ACENTO = new Color(0xFF2E88);
    static final Color SOBRE_ACENTO = new Color(0x12081E);
    static final Color PELIGRO = new Color(0xFF3B4E);
    static final Color BIEN = new Color(0x3CFFA0);
    static final Color CIAN = new Color(0x2EF2FF);
    static final Color MAGENTA = new Color(0xFF2E88);
    static final Color AMARILLO = new Color(0xFFD23F);

    static final Font TITULO = new Font(Font.MONOSPACED, Font.BOLD, 42);
    static final Font SUBTITULO = new Font(Font.MONOSPACED, Font.BOLD, 22);
    static final Font ESCENA = new Font(Font.MONOSPACED, Font.BOLD, 30);
    static final Font CUERPO = new Font(Font.MONOSPACED, Font.BOLD, 14);
    static final Font CUERPO_GRANDE = new Font(Font.MONOSPACED, Font.BOLD, 18);

    private Tema() { }

    public static void instalar() {
        UIManager.put("defaultFont", CUERPO);
        FlatLaf.setGlobalExtraDefaults(Map.of("@background", "#0B0714", "@accentColor", "#FF2E88"));
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 0);
        UIManager.put("Component.arc", 0);
        UIManager.put("TextComponent.arc", 0);
        UIManager.put("ScrollBar.thumbArc", 0);
        UIManager.put("ScrollBar.thumb", BORDE);
        UIManager.put("ScrollBar.track", FONDO);
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", TARJETA);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("OptionPane.messageFont", CUERPO);
        UIManager.put("OptionPane.buttonFont", CUERPO);
        UIManager.put("Button.background", BORDE);
        UIManager.put("Button.foreground", TEXTO);
        UIManager.put("Button.default.background", ACENTO);
        UIManager.put("Button.default.foreground", SOBRE_ACENTO);
    }

    // ---------- paneles ----------

    static JPanel fondo(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        return p;
    }

    /** Tarjeta oscura con borde violeta, esquinas de neón cian y sombra dura de pixel art. */
    private static final class PanelNeon extends JPanel {
        PanelNeon(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(18, 20, 18, 20));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int w = getWidth() - 4, h = getHeight() - 4;
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(4, 4, w, h);
            g.setColor(TARJETA);
            g.fillRect(0, 0, w, h);
            g.setColor(BORDE);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(new Color(255, 255, 255, 10));
            for (int y = 2; y < h; y += 4) {
                g.drawLine(1, y, w - 2, y);   // líneas de monitor
            }
            g.setColor(CIAN);
            int l = 12;
            g.fillRect(0, 0, l, 2);  g.fillRect(0, 0, 2, l);
            g.fillRect(w - l, h - 2, l, 2);  g.fillRect(w - 2, h - l, 2, l);
            g.setColor(MAGENTA);
            g.fillRect(w - l, 0, l, 2);  g.fillRect(w - 2, 0, 2, l);
            g.fillRect(0, h - 2, l, 2);  g.fillRect(0, h - l, 2, l);
            g.dispose();
        }
    }

    static JPanel tarjeta(LayoutManager layout) {
        return new PanelNeon(layout);
    }

    // ---------- texto ----------

    /** Etiqueta dibujada con la fuente de píxeles. Si no entra en el ancho, achica el tamaño de punto. */
    private static final class EtiquetaPixel extends JLabel {
        EtiquetaPixel(String texto, Font fuente, Color color) {
            super(texto);
            setFont(fuente);
            setForeground(color);
        }

        private int unidad() {
            int tam = getFont().getSize();
            return tam <= 16 ? 2 : tam <= 26 ? 3 : 5;
        }

        @Override
        public Dimension getPreferredSize() {
            int u = unidad();
            return new Dimension(FuentePixel.ancho(getText(), u) + 4, FuentePixel.alto(u) + 8);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(10, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int u = unidad();
            while (u > 1 && FuentePixel.ancho(getText(), u) > getWidth()) {
                u--;
            }
            int y = (getHeight() - FuentePixel.alto(u)) / 2;
            FuentePixel.dibujar(g, getText(), 0, y, u, getForeground());
            g.dispose();
        }
    }

    static JLabel etiqueta(String texto, Font fuente, Color color) {
        return new EtiquetaPixel(texto, fuente, color);
    }

    static JTextArea texto(Font fuente, Color color) {
        JTextArea t = new JTextArea();
        t.setEditable(false);
        t.setFocusable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setBackground(TARJETA);
        t.setBorder(null);
        t.setFont(fuente);
        t.setForeground(color);
        return t;
    }

    static void campo(JTextField c) {
        c.setBackground(new Color(0x0E0A1F));
        c.setForeground(TEXTO);
        c.setCaretColor(MAGENTA);
        c.setSelectionColor(MAGENTA);
        c.setSelectedTextColor(SOBRE_ACENTO);
        c.setFont(CUERPO_GRANDE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CIAN, 2), new EmptyBorder(6, 8, 6, 8)));
    }

    // ---------- botones ----------

    /** Botón de pixel art: borde de neón, y al pasar el mouse se llena del color. */
    private static final class BotonNeon extends JButton {
        private final Color color;
        private final boolean relleno;
        private final int unidad;

        BotonNeon(String texto, Color color, boolean relleno, int unidad) {
            super(texto);
            this.color = color;
            this.relleno = relleno;
            this.unidad = unidad;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(FuentePixel.ancho(getText(), unidad) + 40, FuentePixel.alto(unidad) + 26);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int w = getWidth() - 3, h = getHeight() - 3;
            boolean activo = isEnabled();
            boolean encima = activo && getModel().isRollover();
            boolean presionado = activo && getModel().isPressed();
            Color c = activo ? color : new Color(0x4A4266);
            Color fondo = (relleno || encima) && activo ? (presionado ? c.darker() : c) : new Color(0x120B26);
            Color letra = (relleno || encima) && activo ? SOBRE_ACENTO : c;
            int dy = presionado ? 2 : 0;

            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(3, 3 + dy, w, h);                 // sombra dura
            g.setColor(fondo);
            g.fillRect(0, dy, w, h);
            g.setColor(c);
            g.drawRect(0, dy, w - 1, h - 1);
            g.drawRect(1, dy + 1, w - 3, h - 3);
            g.fillRect(0, dy, 5, 5);                      // muesca de esquina
            if (encima) {
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
                g.drawRect(-2, dy - 2, w + 3, h + 3);     // resplandor
            }
            int ty = dy + (h - FuentePixel.alto(unidad)) / 2;
            FuentePixel.centrado(g, getText(), w / 2 + 2, ty, unidad, letra);
            g.dispose();
        }
    }

    static JButton primario(String texto) { return new BotonNeon(texto, MAGENTA, true, 2); }
    static JButton secundario(String texto) { return new BotonNeon(texto, CIAN, false, 2); }
    static JButton peligro(String texto) { return new BotonNeon(texto, PELIGRO, false, 2); }

    /** Botón grande de la barra de acciones del juego. */
    static JButton accion(String texto) { return new BotonNeon(texto, AMARILLO, false, 3); }

    // ---------- tabla ----------

    static void estilizarTabla(JTable tabla, JScrollPane scroll, int... centradas) {
        tabla.setBackground(new Color(0x0E0A1F));
        tabla.setForeground(TEXTO);
        tabla.setFont(CUERPO_GRANDE.deriveFont(15f));
        tabla.setRowHeight(36);
        tabla.setSelectionBackground(MAGENTA);
        tabla.setSelectionForeground(SOBRE_ACENTO);
        tabla.setGridColor(BORDE);
        tabla.setShowGrid(false);

        DefaultTableCellRenderer celda = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, fila, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setBackground(sel ? MAGENTA : fila % 2 == 0 ? new Color(0x0E0A1F) : new Color(0x160F2E));
                setForeground(sel ? SOBRE_ACENTO : TEXTO);
                boolean centro = false;
                for (int c : centradas) {
                    centro |= c == col;
                }
                setHorizontalAlignment(centro ? SwingConstants.CENTER : SwingConstants.LEFT);
                return this;
            }
        };
        tabla.setDefaultRenderer(Object.class, celda);

        JTableHeader cab = tabla.getTableHeader();
        cab.setReorderingAllowed(false);
        cab.setPreferredSize(new Dimension(0, 36));
        cab.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), false, false, fila, col);
                setBackground(new Color(0x1B1236));
                setForeground(CIAN);
                setFont(CUERPO.deriveFont(13f));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, MAGENTA), new EmptyBorder(0, 10, 0, 10)));
                boolean centro = false;
                for (int c : centradas) {
                    centro |= c == col;
                }
                setHorizontalAlignment(centro ? SwingConstants.CENTER : SwingConstants.LEFT);
                return this;
            }
        });
        scroll.setBorder(BorderFactory.createLineBorder(BORDE, 2));
        scroll.getViewport().setBackground(new Color(0x0E0A1F));
    }
}
