package ui;

import javax.swing.*;
import java.awt.*;

/** Barra de cordura hecha de bloques de píxeles: verde, amarilla o roja y titilando cuando queda poco. */
final class BarraCordura extends JComponent {

    private static final int BLOQUES = 25;
    private int valor = 100;
    private String texto = "";
    private Color color = Tema.BIEN;

    BarraCordura() {
        setPreferredSize(new Dimension(280, 34));
        javax.swing.Timer t = new javax.swing.Timer(80, e -> { if (valor <= 30) repaint(); });
        addHierarchyListener(e -> {
            if (isShowing()) t.start(); else t.stop();
        });
    }

    void setValue(int v) { valor = Math.max(0, Math.min(100, v)); repaint(); }
    void setString(String s) { texto = s; repaint(); }
    @Override public void setForeground(Color c) { color = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        int barraH = 16, y = h - barraH - 2;
        FuentePixel.dibujar(g, texto, 0, 1, 2, valor <= 30 ? Tema.PELIGRO : Tema.TEXTO);
        g.setColor(new Color(0x0E0A1F));
        g.fillRect(0, y, w, barraH);
        g.setColor(Tema.BORDE);
        g.drawRect(0, y, w - 1, barraH - 1);
        int lleno = (int) Math.ceil(BLOQUES * valor / 100.0);
        double ancho = (w - 6) / (double) BLOQUES;
        boolean parpadeo = valor <= 30 && (System.currentTimeMillis() / 250) % 2 == 0;
        for (int i = 0; i < lleno; i++) {
            Color c = valor <= 30 ? Tema.PELIGRO : valor <= 60 ? Tema.AMARILLO : color;
            if (parpadeo) {
                c = c.darker();
            }
            g.setColor(c);
            int x = 3 + (int) (i * ancho);
            g.fillRect(x, y + 3, (int) ancho - 2, barraH - 6);
            g.setColor(new Color(255, 255, 255, 90));
            g.fillRect(x, y + 3, (int) ancho - 2, 2);
        }
        g.dispose();
    }
}
