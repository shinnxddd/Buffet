package ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Herramientas de dibujo para que nada se vea "de regla y escuadra": formas apenas irregulares,
 * sombreado suave, texturas con ruido y trazos oscuros pero no negros.
 */
final class Dibujo {

    static final Color TRAZO = new Color(0x1A130F);

    private Dibujo() { }

    // ---------- color ----------

    static Color mezcla(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color oscuro(Color c, float t) { return mezcla(c, new Color(0x0A0705), t); }
    static Color claro(Color c, float t) { return mezcla(c, new Color(0xFFF1CF), t); }
    static Color alfa(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))); }

    // ---------- formas ----------

    /** Rectángulo redondeado "hecho a mano": esquinas y bordes se desvían una fracción de píxel. */
    static Path2D organica(double x, double y, double w, double h, double r, long semilla) {
        Random rnd = new Random(semilla);
        double j = Math.min(1.3, Math.min(w, h) * 0.05);
        double[] d = new double[12];
        for (int i = 0; i < d.length; i++) {
            d[i] = (rnd.nextDouble() - 0.5) * 2 * j;
        }
        r = Math.min(r, Math.min(w, h) / 2);
        double x0 = x + d[0], y0 = y + d[1], x1 = x + w + d[2], y1 = y + d[3];
        double x2 = x + w + d[4], y2 = y + h + d[5], x3 = x + d[6], y3 = y + h + d[7];
        Path2D p = new Path2D.Double();
        p.moveTo(x0 + r, y0);
        p.quadTo((x0 + x1) / 2, (y0 + y1) / 2 + d[8], x1 - r, y1);
        p.quadTo(x1, y1, x1, y1 + r);
        p.quadTo((x1 + x2) / 2 + d[9], (y1 + y2) / 2, x2, y2 - r);
        p.quadTo(x2, y2, x2 - r, y2);
        p.quadTo((x2 + x3) / 2, (y2 + y3) / 2 + d[10], x3 + r, y3);
        p.quadTo(x3, y3, x3, y3 - r);
        p.quadTo((x3 + x0) / 2 + d[11], (y3 + y0) / 2, x0, y0 + r);
        p.quadTo(x0, y0, x0 + r, y0);
        p.closePath();
        return p;
    }

    /** Mancha irregular (charcos, grasa, humedad): un óvalo con el borde ondulado. */
    static Path2D mancha(double cx, double cy, double rx, double ry, long semilla) {
        Random rnd = new Random(semilla);
        int n = 12;
        double[] px = new double[n], py = new double[n];
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n, k = 0.78 + rnd.nextDouble() * 0.4;
            px[i] = cx + Math.cos(a) * rx * k;
            py[i] = cy + Math.sin(a) * ry * k;
        }
        Path2D p = new Path2D.Double();
        p.moveTo((px[n - 1] + px[0]) / 2, (py[n - 1] + py[0]) / 2);
        for (int i = 0; i < n; i++) {
            int s = (i + 1) % n;
            p.quadTo(px[i], py[i], (px[i] + px[s]) / 2, (py[i] + py[s]) / 2);
        }
        p.closePath();
        return p;
    }

    /** Relleno con degradé vertical sutil + contorno redondeado. */
    static void forma(Graphics2D g, Shape s, Color base) {
        forma(g, s, base, 2.4f);
    }

    static void forma(Graphics2D g, Shape s, Color base, float trazo) {
        Rectangle2D b = s.getBounds2D();
        float h = (float) Math.max(1, b.getHeight());
        g.setPaint(new GradientPaint((float) b.getX(), (float) b.getY(), claro(base, 0.10f),
                (float) b.getX(), (float) b.getY() + h, oscuro(base, 0.20f)));
        g.fill(s);
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(trazo, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    /** Sólo el relleno sombreado, sin contorno (para manchas y cosas blandas). */
    static void suave(Graphics2D g, Shape s, Color c) {
        g.setColor(c);
        g.fill(s);
    }

    static void sombra(Graphics2D g, double cx, double cy, double rx, double ry, int alfa) {
        Graphics2D s = (Graphics2D) g.create();
        s.translate(cx, cy);
        s.scale(1, ry / rx);
        s.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) rx, new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, alfa), new Color(0, 0, 0, 0)}));
        s.fill(new Ellipse2D.Double(-rx, -rx, rx * 2, rx * 2));
        s.dispose();
    }

    static void brillo(Graphics2D g, double cx, double cy, double r, Color c, int alfaCentro) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r, new float[]{0f, 1f},
                new Color[]{alfa(c, alfaCentro), alfa(c, 0)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    // ---------- texturas ----------

    static void ruido(Graphics2D g, Rectangle2D r, int puntos, long semilla, Color color, int alfaMax) {
        Random rnd = new Random(semilla);
        for (int i = 0; i < puntos; i++) {
            g.setColor(alfa(color, 8 + rnd.nextInt(Math.max(1, alfaMax))));
            double s = 0.8 + rnd.nextDouble() * 1.8;
            g.fill(new Ellipse2D.Double(r.getX() + rnd.nextDouble() * r.getWidth(), r.getY() + rnd.nextDouble() * r.getHeight(), s, s));
        }
    }

    /** Veta de madera: líneas horizontales onduladas, tenues. */
    static void vetas(Graphics2D g, double x, double y, double w, double h, long semilla, Color color) {
        Random rnd = new Random(semilla);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < Math.max(2, h / 6); i++) {
            double yy = y + 3 + rnd.nextDouble() * (h - 6), a = rnd.nextDouble() * 8;
            Path2D p = new Path2D.Double();
            p.moveTo(x + 3, yy);
            p.curveTo(x + w * 0.3, yy + a - 2, x + w * 0.6, yy - a + 2, x + w - 3, yy + rnd.nextDouble() * 2);
            g.setColor(alfa(color, 28 + rnd.nextInt(30)));
            g.draw(p);
        }
    }

    /** Baldosas con variación de tono, juntas oscuras y alguna rajadura, para que el piso tenga vida. */
    static void baldosas(Graphics2D g, int x, int y, int w, int h, int t, Color a, Color b, long semilla) {
        Random rnd = new Random(semilla);
        for (int i = 0; i * t < w; i++) {
            for (int j = 0; j * t < h; j++) {
                Color base = (i + j) % 2 == 0 ? a : b;
                float v = (rnd.nextFloat() - 0.5f) * 0.16f;
                g.setColor(v > 0 ? claro(base, v) : oscuro(base, -v));
                g.fillRect(x + i * t, y + j * t, Math.min(t, w - i * t), Math.min(t, h - j * t));
            }
        }
        g.setColor(new Color(0, 0, 0, 55));
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i <= w / t; i++) g.drawLine(x + i * t, y, x + i * t, y + h);
        for (int j = 0; j <= h / t; j++) g.drawLine(x, y + j * t, x + w, y + j * t);
        for (int k = 0; k < 7; k++) {   // rajaduras
            double cx = x + rnd.nextDouble() * w, cy = y + rnd.nextDouble() * h;
            Path2D p = new Path2D.Double();
            p.moveTo(cx, cy);
            for (int s = 0; s < 4; s++) {
                cx += (rnd.nextDouble() - 0.4) * 9;
                cy += (rnd.nextDouble() - 0.5) * 7;
                p.lineTo(cx, cy);
            }
            g.setColor(new Color(0, 0, 0, 70));
            g.draw(p);
        }
        ruido(g, new Rectangle2D.Double(x, y, w, h), w * h / 90, semilla + 3, Color.BLACK, 40);
    }

    // ---------- texto ----------

    static void texto(Graphics2D g, String t, Font f, Color c, double x, double y) {
        g.setFont(f);
        double ox = x - g.getFontMetrics().stringWidth(t) / 2.0;
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(t, (float) ox + 1, (float) y + 1.5f);
        g.setColor(c);
        g.drawString(t, (float) ox, (float) y);
    }

    static void textoIzq(Graphics2D g, String t, Font f, Color c, double x, double y) {
        g.setFont(f);
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(t, (float) x + 1, (float) y + 1.5f);
        g.setColor(c);
        g.drawString(t, (float) x, (float) y);
    }

    static BufferedImage lienzo(int w, int h, boolean transparente) {
        return new BufferedImage(w, h, transparente ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    }

    static Graphics2D calidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }
}
