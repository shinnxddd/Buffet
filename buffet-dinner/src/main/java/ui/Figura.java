package ui;

import java.awt.*;
import java.awt.geom.*;

import static ui.Dibujo.*;

/** Personajes (con sus aspectos) e íconos de objetos. Todo con curvas, nada de cajas pegadas. */
final class Figura {

    record Aspecto(Color ropa, Color pelo, Color piel, double escala, int accesorio) { }

    static final Aspecto JUGADOR = new Aspecto(new Color(0x3F6B5B), new Color(0x3A2A1E), new Color(0xE8C39E), 1.0, -1);
    static final Aspecto[] CLIENTES = {
            new Aspecto(new Color(0x5B7C99), new Color(0x6B4A2F), new Color(0xD9A87C), 1.00, 0),   // un cliente
            new Aspecto(new Color(0xEDEBDD), new Color(0x2A1B12), new Color(0xE8C39E), 0.80, 1),   // nena de guardapolvo
            new Aspecto(new Color(0xB5483A), new Color(0x3A3A3A), new Color(0xC8956A), 1.10, 2),   // profesor
            new Aspecto(new Color(0xD9B73A), new Color(0x6E6E6E), new Color(0xD9A87C), 1.00, 3),   // señor de impermeable
            new Aspecto(new Color(0x6B3F5E), new Color(0x9A9A9A), new Color(0xE0B98F), 1.00, 4),   // directora
            new Aspecto(new Color(0x4F5B66), new Color(0x4A4038), new Color(0xCFA07A), 1.06, 5),   // portero
            new Aspecto(new Color(0x8A9A5B), new Color(0x7A5A2E), new Color(0xE8C39E), 0.82, 6),   // nene del último banco
            new Aspecto(new Color(0xA85C6E), new Color(0x4B3A44), new Color(0xE0B98F), 1.00, 7),   // maestra de plástica
    };

    private Figura() { }

    /** Dibuja una persona con los pies en (x, y). {@code contento}: null = neutro. {@code mano}: tipo de objeto que lleva. */
    static void dibujar(Graphics2D g0, double x, double y, int mira, double fase, boolean camina, Aspecto a,
                        Boolean contento, boolean cocinero, String mano, int cuadro) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.scale(a.escala(), a.escala());
        sombra(g, 0, 1, 27, 8, 120);
        if (a.accesorio() == 3) {   // el charco que deja el señor de impermeable
            suave(g, mancha(0, -2, 38, 9, 5), new Color(70, 110, 140, 140));
            suave(g, new Ellipse2D.Double(-16, -5, 20, 5), new Color(170, 205, 230, 110));
        }
        double bob = camina ? Math.abs(Math.sin(fase)) * 3 : Math.sin(cuadro * 0.06) * 0.8;
        double sw = camina ? Math.sin(fase) * 8 : 0;
        double yb = -bob;
        Color pantalon = new Color(0x2C3540);

        // piernas y zapatos
        pierna(g, -6, yb - 30, -6 + sw, -(camina ? Math.max(0, Math.cos(fase)) * 3 : 0), pantalon);
        pierna(g, 6, yb - 30, 6 - sw, -(camina ? Math.max(0, -Math.cos(fase)) * 3 : 0), pantalon);

        if (a.accesorio() == 6) {   // mochila (va detrás del cuerpo)
            forma(g, organica(-14, yb - 62, 28, 36, 9, 61), new Color(0x6A4A8C));
            forma(g, organica(-8, yb - 44, 16, 12, 4, 62), new Color(0x57397A));
        }
        // torso
        Path2D t = new Path2D.Double();
        t.moveTo(-15, yb - 60);
        t.curveTo(-19, yb - 48, -17, yb - 34, -14, yb - 27);
        t.quadTo(0, yb - 22, 14, yb - 27);
        t.curveTo(17, yb - 34, 19, yb - 48, 15, yb - 60);
        t.quadTo(0, yb - 66, -15, yb - 60);
        t.closePath();
        forma(g, t, a.ropa());
        g.setColor(new Color(255, 255, 255, 38));   // luz de costado
        g.fill(new Ellipse2D.Double(-12, yb - 58, 6, 24));
        if (cocinero) {
            Path2D d = new Path2D.Double();
            d.moveTo(-11, yb - 54); d.quadTo(0, yb - 58, 11, yb - 54); d.lineTo(13, yb - 27); d.quadTo(0, yb - 23, -13, yb - 27); d.closePath();
            forma(g, d, new Color(0xD9A441), 2f);
            forma(g, organica(-6, yb - 42, 12, 8, 2, 77), new Color(0xC28F30), 1.6f);
        }
        if (a.accesorio() == 7) {   // chal de la maestra
            Path2D c = new Path2D.Double();
            c.moveTo(-17, yb - 58); c.quadTo(0, yb - 50, 17, yb - 58); c.lineTo(10, yb - 36); c.quadTo(0, yb - 30, -10, yb - 36); c.closePath();
            forma(g, c, new Color(0xD7A15E), 2f);
        }
        // cuello
        suave(g, new Ellipse2D.Double(-5, yb - 69, 10, 11), Dibujo.oscuro(a.piel(), 0.15f));

        // brazos (con un objeto en las manos, van hacia adelante)
        double hx = mira * 11;
        if (mano != null) {
            brazo(g, -14, yb - 56, hx - 6, yb - 38, a.ropa(), a.piel());
            brazo(g, 14, yb - 56, hx + 6, yb - 40, a.ropa(), a.piel());
            icono(g, mano, hx, yb - 49, 1.3);
        } else {
            brazo(g, -15, yb - 57, -19 - sw * 0.35, yb - 31 + sw * 0.25, a.ropa(), a.piel());
            brazo(g, 15, yb - 57, 19 + sw * 0.35, yb - 31 - sw * 0.25, a.ropa(), a.piel());
        }

        // cabeza
        g.translate(mira * 1.6, 0);
        double hy = yb - 85;
        suave(g, new Ellipse2D.Double(-19, hy - 2, 7, 9), a.piel());   // orejas
        suave(g, new Ellipse2D.Double(12, hy - 2, 7, 9), a.piel());
        Path2D cab = new Path2D.Double();
        cab.moveTo(-17, hy - 2);
        cab.curveTo(-18, hy - 21, 18, hy - 21, 17, hy - 2);
        cab.curveTo(16, hy + 13, 8, hy + 19, 0, hy + 19);
        cab.curveTo(-8, hy + 19, -16, hy + 13, -17, hy - 2);
        cab.closePath();
        forma(g, cab, a.piel());
        Path2D pelo = new Path2D.Double();
        pelo.moveTo(-18, hy - 1);
        pelo.curveTo(-22, hy - 29, 22, hy - 29, 18, hy - 1);
        pelo.curveTo(14, hy - 8, 10, hy - 14, 3, hy - 12);
        pelo.curveTo(-3, hy - 9, -12, hy - 9, -18, hy - 1);
        pelo.closePath();
        forma(g, pelo, a.pelo(), 2.2f);
        if (a.accesorio() == 6) {   // pelo despeinado
            for (int i = -1; i <= 1; i++) {
                Path2D p = new Path2D.Double();
                p.moveTo(i * 8 - 4, hy - 20); p.lineTo(i * 8 + 1 + i * 2, hy - 29); p.lineTo(i * 8 + 5, hy - 19); p.closePath();
                forma(g, p, a.pelo(), 1.6f);
            }
        }
        for (int s = -1; s <= 1; s += 2) {   // ojos, cejas y mejillas
            double ex = s * 7 + mira * 1.4;
            suave(g, new Ellipse2D.Double(ex - 3.6, hy - 3, 7.2, 8.2), Color.WHITE);
            suave(g, new Ellipse2D.Double(ex - 1.8 + mira * 1.2, hy - 1.2, 3.8, 5), TRAZO);
            suave(g, new Ellipse2D.Double(ex - 0.6 + mira * 1.2, hy - 0.8, 1.4, 1.4), Color.WHITE);
            g.setColor(TRAZO);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double inclina = contento == null ? 0 : contento ? -0.8 : s * 1.6;   // enojado: cejas hacia el centro
            g.draw(new Line2D.Double(ex - 3.6, hy - 6.8 + inclina, ex + 3.6, hy - 6.8 - inclina));
            suave(g, new Ellipse2D.Double(s * 11 - 3, hy + 6, 6, 3.4), new Color(220, 110, 100, 70));
        }
        g.setColor(oscuro(a.piel(), 0.35f));
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(-2.5 + mira, hy + 6, 5, 4, 200, 140, Arc2D.OPEN));   // nariz
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (contento == null) {
            g.draw(new Line2D.Double(-3.5 + mira, hy + 13, 3.5 + mira, hy + 13));
        } else if (contento) {
            g.draw(new Arc2D.Double(-5 + mira, hy + 8, 10, 7, 200, 140, Arc2D.OPEN));
        } else {
            g.draw(new Arc2D.Double(-5 + mira, hy + 12, 10, 7, 20, 140, Arc2D.OPEN));
        }
        if (cocinero) {
            Color blanco = new Color(0xEDE8D8);
            forma(g, new Ellipse2D.Double(-18, hy - 44, 17, 18), blanco, 2f);
            forma(g, new Ellipse2D.Double(0, hy - 44, 17, 18), blanco, 2f);
            forma(g, new Ellipse2D.Double(-9, hy - 48, 18, 19), blanco, 2f);
            forma(g, organica(-15, hy - 30, 30, 10, 3, 31), blanco, 2f);
        }
        accesorio(g, a, hy, yb, cuadro);
        g.dispose();
    }

    private static void pierna(Graphics2D g, double hx, double hy, double fx, double fy, Color c) {
        Line2D l = new Line2D.Double(hx, hy, fx, fy - 4);
        g.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(TRAZO);
        g.draw(l);
        g.setStroke(new BasicStroke(7.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(l);
        forma(g, new Ellipse2D.Double(fx - 7.5, fy - 6, 15, 8), new Color(0x1C1B1D), 2f);
    }

    private static void brazo(Graphics2D g, double sx, double sy, double hx, double hy, Color manga, Color piel) {
        Path2D p = new Path2D.Double();
        p.moveTo(sx, sy);
        p.quadTo((sx + hx) / 2 + Math.signum(sx) * 4, (sy + hy) / 2, hx, hy);
        g.setStroke(new BasicStroke(10.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(TRAZO);
        g.draw(p);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(manga);
        g.draw(p);
        forma(g, new Ellipse2D.Double(hx - 4, hy - 3, 8, 8), piel, 1.8f);
    }

    private static void accesorio(Graphics2D g, Aspecto a, double hy, double yb, int cuadro) {
        switch (a.accesorio()) {
            case 1:   // trenzas y moños
                forma(g, organica(-27, hy - 2, 8, 30, 4, 1), a.pelo(), 1.8f);
                forma(g, organica(19, hy - 2, 8, 30, 4, 2), a.pelo(), 1.8f);
                forma(g, new Ellipse2D.Double(-27, hy - 14, 13, 10), new Color(0xE58FB0), 1.8f);
                forma(g, new Ellipse2D.Double(14, hy - 14, 13, 10), new Color(0xE58FB0), 1.8f);
                break;
            case 2:   // gorra azul y silbato
                forma(g, organica(-18, hy - 27, 36, 16, 9, 3), new Color(0x2F5E8A), 2f);
                forma(g, organica(-23, hy - 13, 46, 5, 3, 4), new Color(0x244B6E), 1.8f);
                g.setColor(TRAZO);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new Line2D.Double(-6, yb - 62, 0, yb - 47));
                g.draw(new Line2D.Double(6, yb - 62, 0, yb - 47));
                forma(g, new Ellipse2D.Double(-5, yb - 49, 10, 8), new Color(0xE6E6E6), 1.6f);
                break;
            case 3:   // sombrero de lluvia y gotas
                forma(g, organica(-19, hy - 29, 38, 18, 10, 5), new Color(0xD9B73A), 2f);
                forma(g, organica(-27, hy - 14, 54, 6, 3, 6), new Color(0xC0A02E), 1.8f);
                for (int i = 0; i < 3; i++) {
                    double caida = (cuadro * 1.8 + i * 17) % 52;
                    g.setColor(new Color(110, 160, 200, (int) (230 * (1 - caida / 52))));
                    g.fill(new Ellipse2D.Double(-15 + i * 15, yb - 48 + caida, 4, 7));
                }
                break;
            case 4:   // rodete y anteojos
                forma(g, new Ellipse2D.Double(-9, hy - 36, 18, 16), a.pelo(), 2f);
                gafas(g, hy);
                break;
            case 5:   // gorra de portero, bigote y llavero
                forma(g, organica(-18, hy - 25, 36, 14, 8, 7), new Color(0x4A5560), 2f);
                forma(g, organica(-21, hy - 12, 42, 5, 3, 8), new Color(0x36404A), 1.8f);
                forma(g, organica(-8, hy + 7, 16, 5, 2.5, 9), new Color(0x3A3028), 1.6f);
                forma(g, new Ellipse2D.Double(11, yb - 31, 7, 7), new Color(0xB9B9A8), 1.6f);
                break;
            case 7:   // rodete y anteojos finitos
                forma(g, new Ellipse2D.Double(-8, hy - 35, 16, 15), a.pelo(), 2f);
                gafas(g, hy);
                break;
            default:
                break;
        }
    }

    private static void gafas(Graphics2D g, double hy) {
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(-14.5, hy - 5, 13, 12));
        g.draw(new Ellipse2D.Double(1.5, hy - 5, 13, 12));
        g.draw(new Line2D.Double(-1.5, hy, 1.5, hy));
    }

    // ---------- íconos de objetos (productos y herramientas) ----------

    static void icono(Graphics2D g0, String tipo, double cx, double cy, double esc) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(cx, cy);
        g.scale(esc, esc);
        switch (tipo) {
            case "CAFE":
                forma(g, organica(-9, -7, 18, 15, 7, 11), new Color(0xEDEBDD), 1.8f);
                suave(g, new Ellipse2D.Double(-7, -8, 14, 5), new Color(0x4A2A18));
                g.setColor(TRAZO);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Arc2D.Double(4, -4, 9, 9, -90, 180, Arc2D.OPEN));
                g.setColor(new Color(255, 255, 255, 140));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new QuadCurve2D.Double(-3, -10, -6, -14, -2, -18));
                g.draw(new QuadCurve2D.Double(3, -10, 0, -14, 4, -18));
                break;
            case "ALFAJOR":
                forma(g, new Ellipse2D.Double(-11, 0, 22, 9), new Color(0x7A4A2A), 1.8f);
                suave(g, new Ellipse2D.Double(-10, -2, 20, 6), new Color(0xE9D7A8));
                forma(g, new Ellipse2D.Double(-11, -8, 22, 9), new Color(0x8A5630), 1.8f);
                g.setColor(new Color(255, 255, 255, 190));
                for (int i = 0; i < 5; i++) g.fill(new Ellipse2D.Double(-7 + i * 3.4, -5 + (i % 2) * 2, 2, 2));
                break;
            case "AGUA":
                forma(g, organica(-6, -8, 12, 21, 6, 12), new Color(0x9CCFE0), 1.8f);
                forma(g, organica(-3, -14, 6, 8, 2, 13), new Color(0xB5DDEA), 1.6f);
                forma(g, organica(-3.5, -18, 7, 5, 2, 14), new Color(0x3B7DBF), 1.6f);
                suave(g, new Rectangle2D.Double(-6, -1, 12, 6), new Color(0x3B7DBF));
                suave(g, new Rectangle2D.Double(-3, -6, 2, 10), new Color(255, 255, 255, 120));
                break;
            case "TOSTADO":
                forma(g, organica(-12, -10, 24, 21, 6, 15), new Color(0xD9A441), 1.8f);
                suave(g, organica(-9, -7, 18, 15, 4, 16), new Color(0xF0C970));
                g.setColor(new Color(0x8A5A1E));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 3; i++) g.draw(new Line2D.Double(-6 + i * 6, -5, -8 + i * 6, 6));
                suave(g, mancha(8, 10, 5, 3, 17), new Color(0xF2D45B));
                break;
            case "MILANESA":
                forma(g, organica(-12, 3, 24, 7, 3, 18), new Color(0xD9A86A), 1.8f);
                forma(g, new Ellipse2D.Double(-14, -4, 28, 9), new Color(0xB5762E), 1.8f);
                suave(g, mancha(0, 0, 11, 2, 19), new Color(0x7EAA5A));
                forma(g, organica(-12, -12, 24, 10, 5, 20), new Color(0xE0B473), 1.8f);
                break;
            case "TRAPEADOR":
                g.setColor(TRAZO);
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(-5, -19, 3, 7));
                g.setColor(new Color(0xA8794A));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(-5, -19, 3, 7));
                forma(g, organica(-9, 5, 18, 8, 4, 21), new Color(0xB8B29A), 1.8f);
                g.setColor(new Color(0x8E8975));
                g.setStroke(new BasicStroke(1.4f));
                for (int i = 0; i < 5; i++) g.draw(new Line2D.Double(-7 + i * 3.5, 12, -8 + i * 3.7, 17));
                break;
            case "TRAPO":
                forma(g, organica(-12, -6, 24, 15, 4, 22), new Color(0xE6C84A), 1.8f);
                g.setColor(new Color(0xB8962E));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Line2D.Double(-12, 2, 12, 1));
                suave(g, new Ellipse2D.Double(-6, -4, 3, 3), new Color(0xB8962E));
                suave(g, new Ellipse2D.Double(4, 3, 3, 3), new Color(0xB8962E));
                forma(g, mancha(9, 12, 4, 4, 23), new Color(0xE6C84A), 1.6f);
                break;
            case "RASQUETA":
                forma(g, organica(-3, -2, 7, 17, 3, 24), new Color(0xA8794A), 1.8f);
                forma(g, organica(-13, -12, 26, 10, 2, 25), new Color(0xB9C3BF), 1.8f);
                suave(g, new Rectangle2D.Double(-10, -10, 20, 2), new Color(255, 255, 255, 140));
                break;
            case "BOLSA":
                Path2D b = new Path2D.Double();
                b.moveTo(-3, -12); b.curveTo(-16, -4, -14, 12, 0, 12); b.curveTo(14, 12, 16, -4, 3, -12); b.quadTo(0, -9, -3, -12); b.closePath();
                forma(g, b, new Color(0x26332E), 1.8f);
                forma(g, organica(-4, -17, 8, 7, 3, 26), new Color(0x26332E), 1.6f);
                suave(g, new Ellipse2D.Double(-9, -4, 4, 9), new Color(255, 255, 255, 50));
                break;
            case "DESTAPADOR":
                forma(g, organica(-2.5, -18, 5, 22, 2, 27), new Color(0xA8794A), 1.6f);
                Path2D c = new Path2D.Double();
                c.moveTo(-10, 8); c.curveTo(-10, -2, 10, -2, 10, 8); c.quadTo(0, 12, -10, 8); c.closePath();
                forma(g, c, new Color(0xC4513F), 1.8f);
                break;
            case "FUSIBLE":
                forma(g, organica(-9, -5, 18, 10, 4, 28), new Color(0xEDE8D8), 1.8f);
                forma(g, organica(-13, -4, 6, 8, 2, 29), new Color(0xB9B9A8), 1.6f);
                forma(g, organica(7, -4, 6, 8, 2, 30), new Color(0xB9B9A8), 1.6f);
                g.setColor(new Color(0xB5483A));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Line2D.Double(-5, 0, 5, 0));
                break;
            case "CAJA":
                for (int i = 0; i < 3; i++) forma(g, organica(-9 + i * 7, -15, 5, 9, 2, 31 + i), new Color(i == 1 ? 0xC4513F : 0x7FB58A), 1.5f);
                forma(g, organica(-13, -9, 26, 19, 3, 35), new Color(0xB98A4E), 1.8f);
                g.setColor(new Color(0x7A5A2E));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(-13, -1, 13, -1));
                g.draw(new Line2D.Double(-13, 6, 13, 6));
                break;
            default:
                forma(g, new Ellipse2D.Double(-8, -8, 16, 16), new Color(0xD9A441), 1.8f);
        }
        g.dispose();
    }
}
