package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/** Paleta, fuentes y fábricas de componentes: todo el aspecto de la app vive acá. */
public final class Tema {

    // Verde pizarrón de colegio + mostaza de cartel de buffet
    static final Color FONDO = new Color(0x18221F);
    static final Color TARJETA = new Color(0x22302C);
    static final Color BORDE = new Color(0x34463F);
    static final Color TEXTO = new Color(0xEAE6D8);
    static final Color TEXTO_SUAVE = new Color(0x9DB0A6);
    static final Color ACENTO = new Color(0xD9A441);
    static final Color SOBRE_ACENTO = new Color(0x1E1A10);
    static final Color PELIGRO = new Color(0xC4513F);
    static final Color BIEN = new Color(0x7FB58A);

    static final Font TITULO = new Font("Georgia", Font.BOLD, 42);
    static final Font SUBTITULO = new Font("Georgia", Font.BOLD, 22);
    static final Font ESCENA = new Font("Georgia", Font.BOLD, 30);
    static final Font CUERPO = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font CUERPO_GRANDE = new Font("Segoe UI", Font.PLAIN, 18);

    private Tema() { }

    public static void instalar() {
        UIManager.put("defaultFont", CUERPO);
        FlatLaf.setGlobalExtraDefaults(Map.of("@background", "#18221F", "@accentColor", "#D9A441"));
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
    }

    static JPanel fondo(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        return p;
    }

    static JPanel tarjeta(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(TARJETA);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), new EmptyBorder(18, 20, 18, 20)));
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
        b.setMargin(new Insets(8, 18, 8, 18));
        return b;
    }

    static JButton primario(String texto) { return boton(texto, ACENTO, SOBRE_ACENTO); }
    static JButton secundario(String texto) { return boton(texto, BORDE, TEXTO); }
    static JButton peligro(String texto) { return boton(texto, PELIGRO, Color.WHITE); }

    /** Botón grande de la barra de acciones del juego. */
    static JButton accion(String texto) {
        JButton b = boton(texto, BORDE, TEXTO);
        b.setFont(CUERPO.deriveFont(Font.BOLD, 15f));
        b.setMargin(new Insets(14, 18, 14, 18));
        return b;
    }
}
