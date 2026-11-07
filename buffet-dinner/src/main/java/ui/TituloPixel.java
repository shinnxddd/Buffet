package ui;

import javax.swing.*;
import java.awt.*;

/** Título animado del menú: letras blancas con sombra violeta y, de a ratos, un glitch cian/magenta. */
final class TituloPixel extends JComponent {

    private static final String TEXTO = "BUFFET TERROR";
    private static final int U = 6;
    private int cuadro;

    TituloPixel() {
        javax.swing.Timer t = new javax.swing.Timer(50, e -> { cuadro++; repaint(); });
        addHierarchyListener(e -> {
            if (isShowing()) t.start(); else t.stop();
        });
        setAlignmentX(LEFT_ALIGNMENT);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(FuentePixel.ancho(TEXTO, U) + 20, FuentePixel.alto(U) + 22);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        boolean glitch = cuadro % 70 < 4;
        boolean apagada = cuadro % 130 > 123 && cuadro % 4 < 2;   // una letra de neón que se va
        int x = 6, y = 4;
        FuentePixel.dibujar(g, TEXTO, x + U, y + U, U, new Color(0x2A1255));
        if (glitch) {
            FuentePixel.dibujar(g, TEXTO, x - 4, y + 1, U, Tema.CIAN);
            FuentePixel.dibujar(g, TEXTO, x + 4, y - 1, U, Tema.MAGENTA);
        }
        String visible = apagada ? "BUFFET  ERROR" : TEXTO;
        FuentePixel.dibujar(g, visible, x, y, U, Tema.TEXTO);
        int y2 = y + FuentePixel.alto(U) + 6;
        g.setColor(Tema.MAGENTA);
        g.fillRect(x, y2, FuentePixel.ancho(TEXTO, U) * 2 / 3, 4);
        g.setColor(Tema.CIAN);
        g.fillRect(x + FuentePixel.ancho(TEXTO, U) * 2 / 3 + 6, y2, 24, 4);
        g.dispose();
    }
}
