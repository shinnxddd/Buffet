package ui;

import java.awt.*;
import java.util.Random;

/** Fondo de las pantallas con ventanas: cielo violeta, estrellas, ciudad de neón y grilla en el horizonte. */
final class FondoNeon {

    private FondoNeon() { }

    static void pintar(Graphics2D g, int w, int h, int t) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x08040F), 0, h, new Color(0x241040)));
        g.fillRect(0, 0, w, h);

        Random r = new Random(11);
        for (int i = 0; i < 70; i++) {   // estrellas que titilan
            int x = r.nextInt(w), y = r.nextInt(Math.max(1, h / 2));
            int brillo = 70 + (int) (60 * Math.abs(Math.sin((t + i * 13) * 0.04)));
            g.setColor(new Color(220, 210, 255, brillo));
            g.fillRect(x, y, 2, 2);
        }

        int base = (int) (h * 0.72);
        int x = 0;
        while (x < w) {   // ciudad: edificios oscuros con ventanas encendidas
            int ancho = 30 + r.nextInt(60), alto = 40 + r.nextInt((int) (h * 0.28));
            g.setColor(new Color(0x120A26));
            g.fillRect(x, base - alto, ancho, alto + 2);
            g.setColor(new Color(0x1C1038));
            g.fillRect(x + ancho - 4, base - alto, 4, alto);
            for (int wy = base - alto + 8; wy < base - 6; wy += 12) {
                for (int wx = x + 6; wx < x + ancho - 10; wx += 10) {
                    if (r.nextInt(5) == 0) {
                        g.setColor(r.nextBoolean() ? new Color(255, 210, 63, 150) : new Color(46, 242, 255, 130));
                        g.fillRect(wx, wy, 4, 5);
                    }
                }
            }
            x += ancho + r.nextInt(6);
        }

        g.setColor(new Color(0x0C0618));
        g.fillRect(0, base, w, h - base);
        g.setColor(new Color(255, 46, 136, 150));
        g.fillRect(0, base, w, 2);                 // línea del horizonte
        g.setColor(new Color(255, 46, 136, 55));
        for (int i = -w; i < w * 2; i += 60) {      // grilla en perspectiva
            g.drawLine(w / 2 + (i - w / 2) / 6, base, i, h);
        }
        for (int i = 1; i < 9; i++) {
            int y = base + (int) ((h - base) * Math.pow((i + (t % 40) / 40.0) / 9.0, 2));
            if (y < h) {
                g.drawLine(0, y, w, y);
            }
        }
        g.setColor(new Color(0, 0, 0, 40));
        for (int y = 0; y < h; y += 4) {
            g.fillRect(0, y, w, 2);                // scanlines
        }
    }
}
