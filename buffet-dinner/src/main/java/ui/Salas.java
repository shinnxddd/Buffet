package ui;

import modelo.Sala;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import static ui.Dibujo.*;

/** Pinta una sola vez el fondo fijo de cada sala (a doble resolución, para que se vea nítido al agrandar la ventana). */
final class Salas {

    static final int W = 900, H = 520, ESCALA = 2;

    private Salas() { }

    static BufferedImage fondo(Sala sala) {
        BufferedImage img = lienzo(W * ESCALA, H * ESCALA, false);
        Graphics2D g = calidad(img.createGraphics());
        g.scale(ESCALA, ESCALA);
        switch (sala) {
            case SALON: salon(g); break;
            case COCINA: cocina(g); break;
            default: almacen(g); break;
        }
        g.dispose();
        return img;
    }

    /** Lo que va por delante de los personajes (sólo el mostrador del salón). */
    static BufferedImage frente(Sala sala) {
        if (sala != Sala.SALON) {
            return null;
        }
        BufferedImage img = lienzo(W * ESCALA, H * ESCALA, true);
        Graphics2D g = calidad(img.createGraphics());
        g.scale(ESCALA, ESCALA);
        mostrador(g);
        g.dispose();
        return img;
    }

    // ================= SALÓN =================

    private static void salon(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x2F443B), 0, 235, new Color(0x263830)));
        g.fillRect(0, 0, W, 240);
        ruido(g, new Rectangle2D.Double(0, 0, W, 235), 2600, 1, Color.BLACK, 40);
        ruido(g, new Rectangle2D.Double(0, 0, W, 235), 900, 2, new Color(0xCFE0C8), 22);
        // revestimiento inferior, guardasilla y manchas de humedad
        g.setColor(new Color(0x22312B));
        g.fillRect(0, 140, W, 98);
        forma(g, organica(-4, 134, W + 8, 9, 3, 3), new Color(0x8A6A3B), 2f);
        vetas(g, 0, 134, W, 9, 4, new Color(0x4A3418));
        for (int i = 0; i < 5; i++) {
            suave(g, mancha(70 + i * 190, 150, 34, 24, 10 + i), new Color(0, 0, 0, 36));
        }
        suave(g, mancha(310, 40, 40, 26, 21), new Color(0x1F2A25));
        Random r = new Random(7);
        g.setColor(new Color(0, 0, 0, 80));
        g.setStroke(new BasicStroke(1.2f));
        for (int k = 0; k < 4; k++) {   // grietas en la pared
            double x = 20 + r.nextDouble() * 860, y = 10 + r.nextDouble() * 110;
            Path2D p = new Path2D.Double();
            p.moveTo(x, y);
            for (int i = 0; i < 5; i++) { x += (r.nextDouble() - 0.5) * 12; y += 6 + r.nextDouble() * 8; p.lineTo(x, y); }
            g.draw(p);
        }
        ventana(g, 90, 20, true);
        ventana(g, 700, 20, false);
        pizarra(g);
        dibujosInfantiles(g);
        reloj(g, 640, 56);
        casilleros(g);
        tablero(g);
        puertaBano(g);
        for (int x : new int[]{230, 450, 670}) lampara(g, x);

        // zócalo y piso
        forma(g, organica(-4, 228, W + 8, 12, 3, 5), new Color(0x6B4E2A), 2f);
        baldosas(g, 0, 238, W, 282, 46, new Color(0x55665B), new Color(0x4B5B51), 11);
        g.setPaint(new GradientPaint(0, 238, new Color(0, 0, 0, 120), 0, 290, new Color(0, 0, 0, 0)));
        g.fillRect(0, 238, W, 60);
        suave(g, mancha(300, 330, 90, 30, 31), new Color(0, 0, 0, 30));
        suave(g, mancha(620, 470, 110, 26, 32), new Color(0, 0, 0, 34));
        // camino gastado frente al mostrador
        suave(g, mancha(450, 300, 330, 34, 33), new Color(255, 240, 200, 14));
        puerta(g, true, "COCINA", new Color(0xF0C98A));
        puerta(g, false, "ALMACÉN", new Color(0x9DB9C8));
    }

    private static void ventana(Graphics2D g, int x, int y, boolean arbol) {
        forma(g, organica(x - 6, y - 6, 122, 78, 4, x), new Color(0x7A5A2E), 2.4f);
        g.setPaint(new GradientPaint(0, y, new Color(0x0C1823), 0, y + 66, new Color(0x1E3441)));
        g.fill(new Rectangle2D.Double(x, y, 110, 66));
        brillo(g, x + 82, y + 18, 22, new Color(0xE8EFD0), 180);
        suave(g, new Ellipse2D.Double(x + 74, y + 10, 16, 16), new Color(0xF2F0DC));
        if (arbol) {   // silueta de un árbol seco
            g.setColor(new Color(0x050A0E));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Double(x + 20, y + 66, x + 24, y + 40, x + 30, y + 22));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Double(x + 27, y + 34, x + 38, y + 30, x + 44, y + 20));
            g.draw(new QuadCurve2D.Double(x + 25, y + 42, x + 14, y + 34, x + 10, y + 26));
        }
        g.setColor(new Color(255, 255, 255, 26));   // reflejo del vidrio
        Path2D ref = new Path2D.Double();
        ref.moveTo(x + 8, y + 66); ref.lineTo(x + 34, y); ref.lineTo(x + 50, y); ref.lineTo(x + 24, y + 66); ref.closePath();
        g.fill(ref);
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(3.4f));
        g.draw(new Line2D.Double(x + 55, y, x + 55, y + 66));
        g.draw(new Line2D.Double(x, y + 33, x + 110, y + 33));
        forma(g, organica(x - 10, y + 66, 130, 8, 3, x + 3), new Color(0x8A6A3B), 2f);
    }

    private static void pizarra(Graphics2D g) {
        forma(g, organica(296, 8, 308, 66, 6, 41), new Color(0x8A6A3B), 2.6f);
        g.setPaint(new GradientPaint(0, 14, new Color(0x21342C), 0, 68, new Color(0x182720)));
        g.fill(organica(303, 15, 294, 52, 3, 42));
        g.setColor(new Color(255, 255, 255, 16));
        g.fill(new Ellipse2D.Double(330, 22, 150, 22));   // restos de tiza borrada
        g.setColor(new Color(0xE9E4D0));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Random r = new Random(9);
        for (int i = 0; i < 3; i++) {   // renglones de tiza del menú
            double y = 52 + i * 5.5, x = 318;
            g.setColor(new Color(235, 230, 208, 110));
            Path2D p = new Path2D.Double();
            p.moveTo(x, y);
            for (int k = 0; k < 8; k++) { x += 8 + r.nextDouble() * 18; p.lineTo(x, y + (r.nextDouble() - 0.5) * 1.4); if (x > 560) break; }
            g.draw(p);
        }
        forma(g, organica(355, 66, 190, 5, 2, 43), new Color(0x7A5A2E), 1.6f);   // repisa de la tiza
    }

    private static void dibujosInfantiles(Graphics2D g) {
        Color[] papel = {new Color(0xE9DDB8), new Color(0xD8E3C0), new Color(0xEBC9C2), new Color(0xC9DCE6)};
        double[][] pos = {{238, 36, 28, 34}, {270, 50, 24, 28}, {500, 88, 0, 0}};
        for (int i = 0; i < 2; i++) {
            Graphics2D h = (Graphics2D) g.create();
            h.translate(pos[i][0] + pos[i][2] / 2, pos[i][1] + pos[i][3] / 2);
            h.rotate((i == 0 ? -0.06 : 0.07));
            forma(h, organica(-pos[i][2] / 2, -pos[i][3] / 2, pos[i][2], pos[i][3], 1, 50 + i), papel[i], 1.6f);
            h.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            h.setColor(i == 0 ? new Color(0xC4513F) : new Color(0x3B7DBF));
            h.draw(new Ellipse2D.Double(-6, -8, 12, 12));
            h.setColor(new Color(0x3A2A1E));
            h.draw(new Line2D.Double(-8, 8, 8, 8));
            h.dispose();
        }
    }

    private static void reloj(Graphics2D g, double cx, double cy) {
        sombra(g, cx + 2, cy + 3, 26, 26, 90);
        forma(g, new Ellipse2D.Double(cx - 22, cy - 22, 44, 44), new Color(0xE9E4D0), 3f);
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            g.draw(new Line2D.Double(cx + Math.cos(a) * 17, cy + Math.sin(a) * 17, cx + Math.cos(a) * 20, cy + Math.sin(a) * 20));
        }
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));   // clavado a las 2:55, siempre
        g.draw(new Line2D.Double(cx, cy, cx - 9, cy + 11));
        g.draw(new Line2D.Double(cx, cy, cx + 3, cy + 15));
        suave(g, new Ellipse2D.Double(cx - 2, cy - 2, 4, 4), TRAZO);
    }

    private static void casilleros(Graphics2D g) {
        Color[] c = {new Color(0x4C6A62), new Color(0x587A70), new Color(0x44625A)};
        for (int i = 0; i < 3; i++) {
            double x = 30 + i * 40;
            forma(g, organica(x, 112, 39, 118, 3, 60 + i), c[i], 2.2f);
            g.setColor(TRAZO);
            g.setStroke(new BasicStroke(1.6f));
            for (int k = 0; k < 4; k++) g.draw(new Line2D.Double(x + 9, 124 + k * 5, x + 30, 124 + k * 5));
            suave(g, new Ellipse2D.Double(x + 28, 168, 5, 8), new Color(0xD9A441));
            suave(g, mancha(x + 12, 210 + (i % 2) * 4, 9, 6, 70 + i), new Color(0x7A4A2A));   // óxido
        }
        forma(g, organica(112, 110, 38, 4, 1, 66), new Color(0x3A4A44), 1.6f);
        suave(g, new Rectangle2D.Double(63, 150, 14, 16), new Color(0xD9A441));   // calco gastado
        textoIzq(g, "Casilleros", new Font("Segoe UI", Font.BOLD, 12), new Color(0xB8C5B8), 38, 104);
    }

    private static void tablero(Graphics2D g) {
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(207, 160, 214, 175, 207, 188));
        forma(g, organica(186, 104, 42, 56, 5, 80), new Color(0x5B6E66), 2.2f);
        forma(g, organica(192, 110, 30, 32, 2, 81), new Color(0x2C3A35), 1.6f);
        for (int i = 0; i < 3; i++) forma(g, organica(196 + i * 9, 116, 6, 18, 3, 82 + i), new Color(0xD9A441), 1.2f);
        textoIzq(g, "Tablero", new Font("Segoe UI", Font.BOLD, 11), new Color(0xB8C5B8), 189, 99);
    }

    private static void puertaBano(Graphics2D g) {
        forma(g, organica(762, 100, 86, 134, 4, 90), new Color(0x4A3A2A), 2.4f);
        forma(g, organica(770, 108, 70, 120, 3, 91), new Color(0x7A8F86), 2f);
        vetas(g, 770, 108, 70, 120, 92, new Color(0x2B3A34));
        forma(g, new Ellipse2D.Double(826, 168, 7, 7), new Color(0xD9A441), 1.6f);
        forma(g, organica(790, 122, 30, 14, 3, 93), new Color(0xE9E4D0), 1.6f);
        textoIzq(g, "BAÑO", new Font("Georgia", Font.BOLD, 10), new Color(0x1E2A25), 795, 133);
    }

    private static void lampara(Graphics2D g, int x) {
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Line2D.Double(x, 0, x, 18));
        Path2D p = new Path2D.Double();
        p.moveTo(x - 14, 30); p.quadTo(x - 10, 16, x, 16); p.quadTo(x + 10, 16, x + 14, 30); p.quadTo(x, 34, x - 14, 30); p.closePath();
        forma(g, p, new Color(0x6E7B72), 2f);
        suave(g, new Ellipse2D.Double(x - 8, 28, 16, 6), new Color(0xFFF1BF));
    }

    private static void mostrador(Graphics2D g) {
        sombra(g, 450, 250, 330, 14, 110);
        forma(g, organica(158, 213, 584, 33, 5, 100), new Color(0x8E653A), 2.6f);
        vetas(g, 158, 213, 584, 33, 101, new Color(0x4A2E14));
        for (int i = 0; i < 6; i++) {   // cajones
            forma(g, organica(172 + i * 96, 220, 86, 20, 4, 102 + i), new Color(0x9C7242), 1.6f);
            forma(g, new Ellipse2D.Double(210 + i * 96, 227, 8, 6), new Color(0xD9A441), 1.4f);
        }
        forma(g, organica(146, 183, 608, 30, 8, 110), new Color(0xC79A55), 2.6f);
        vetas(g, 146, 183, 608, 30, 111, new Color(0x6B4A22));
        g.setColor(new Color(255, 240, 200, 90));
        g.fill(organica(154, 186, 592, 5, 2, 112));
        // caja registradora
        forma(g, organica(272, 158, 52, 32, 4, 120), new Color(0x5B6E66), 2.2f);
        forma(g, organica(278, 146, 40, 16, 3, 121), new Color(0x2C3A35), 2f);
        suave(g, new Rectangle2D.Double(282, 150, 32, 5), new Color(0x7FB58A));
        for (int i = 0; i < 3; i++) for (int j = 0; j < 2; j++) suave(g, new Ellipse2D.Double(280 + i * 11, 168 + j * 8, 6, 5), new Color(0xD9A441));
        // campanita y cuaderno
        forma(g, organica(190, 176, 36, 7, 2, 122), new Color(0xE9E4D0), 1.6f);
        Path2D camp = new Path2D.Double();
        camp.moveTo(440, 183); camp.curveTo(440, 164, 466, 164, 466, 183); camp.closePath();
        forma(g, camp, new Color(0xD9A441), 1.8f);
        suave(g, new Ellipse2D.Double(450, 160, 6, 6), new Color(0xD9A441));
        // vitrina de alfajores
        forma(g, organica(606, 138, 112, 46, 5, 130), new Color(0x9CCFE0), 2.2f);
        g.setColor(new Color(200, 235, 245, 60));
        g.fill(organica(610, 142, 104, 38, 4, 131));
        for (int i = 0; i < 5; i++) {
            Figura.icono(g, "ALFAJOR", 624 + i * 20, 168, 0.6);
            if (i < 4) Figura.icono(g, "ALFAJOR", 634 + i * 20, 156, 0.6);
        }
        g.setColor(new Color(255, 255, 255, 110));
        g.draw(new Line2D.Double(612, 144, 640, 144));
        textoIzq(g, "Vitrina", new Font("Segoe UI", Font.BOLD, 11), new Color(0xF0DDB4), 640, 132);
        textoIzq(g, "Mostrador", new Font("Segoe UI", Font.BOLD, 13), new Color(0xF0DDB4), 410, 242);
    }

    // ================= COCINA =================

    private static void cocina(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x93A89C), 0, 100, new Color(0x7F9588)));
        g.fillRect(0, 0, W, 100);
        ruido(g, new Rectangle2D.Double(0, 0, W, 100), 1400, 3, Color.BLACK, 36);
        // azulejos
        Random r = new Random(4);
        for (int i = 0; i < W / 30 + 1; i++) {
            for (int j = 0; j < 5; j++) {
                float v = (r.nextFloat() - 0.5f) * 0.14f;
                Color base = new Color(0xB4C7BE);
                g.setColor(v > 0 ? claro(base, v) : oscuro(base, -v));
                g.fillRect(i * 30, 92 + j * 30, 30, 30);
            }
        }
        g.setColor(new Color(0, 0, 0, 55));
        g.setStroke(new BasicStroke(1.3f));
        for (int i = 0; i <= W / 30; i++) g.drawLine(i * 30, 92, i * 30, 240);
        for (int j = 0; j <= 5; j++) g.drawLine(0, 92 + j * 30, W, 92 + j * 30);
        ruido(g, new Rectangle2D.Double(0, 92, W, 148), 1800, 5, Color.BLACK, 40);
        suave(g, mancha(480, 130, 70, 30, 6), new Color(60, 40, 10, 36));   // salpicaduras sobre la plancha
        forma(g, organica(-4, 88, W + 8, 7, 2, 7), new Color(0x6E7B72), 1.8f);

        // estante con frascos
        forma(g, organica(196, 74, 150, 6, 2, 8), new Color(0x8A6A3B), 1.8f);
        for (int i = 0; i < 6; i++) {
            int[] frascos = {0xC4513F, 0xD9A441, 0x7FB58A, 0xE9E4D0, 0xB5762E, 0x9CCFE0};
            forma(g, organica(204 + i * 24, 52, 17, 22, 5, 9 + i), new Color(frascos[i]), 1.6f);
            forma(g, organica(206 + i * 24, 47, 13, 6, 2, 20 + i), new Color(0xB9B9A8), 1.4f);
        }
        // campana, cocina y plancha
        Path2D campana = new Path2D.Double();
        campana.moveTo(352, 20); campana.lineTo(578, 20); campana.lineTo(600, 128); campana.lineTo(330, 128); campana.closePath();
        forma(g, campana, new Color(0x8F9C98), 2.6f);
        g.setColor(new Color(255, 255, 255, 50));
        g.fill(organica(366, 30, 24, 90, 6, 30));
        for (int i = 0; i < 6; i++) g.draw(new Line2D.Double(342 + i * 4, 120, 340 + i * 4, 126));
        forma(g, organica(360, 168, 210, 78, 6, 31), new Color(0x6C7773), 2.6f);
        forma(g, organica(352, 152, 226, 20, 6, 32), new Color(0x4A504F), 2.4f);
        for (int i = 0; i < 4; i++) forma(g, new Ellipse2D.Double(382 + i * 46, 194, 14, 14), new Color(0xD9A441), 1.8f);
        forma(g, organica(374, 214, 186, 22, 4, 33), new Color(0x4A504F), 1.8f);
        textoIzq(g, "Plancha", new Font("Segoe UI", Font.BOLD, 12), new Color(0xE9E4D0), 440, 232);

        // mesada con alacenas (cafetera sobre ella) y pileta
        for (double x : new double[]{190, 606}) {
            double w = x < 300 ? 150 : 234;
            forma(g, organica(x, 196, w, 50, 4, (long) x), new Color(0x6E5A3E), 2.4f);
            vetas(g, x, 196, w, 50, (long) x + 1, new Color(0x2E2214));
            for (int i = 0; i < w / 56; i++) {
                forma(g, organica(x + 6 + i * 56, 202, 48, 38, 3, (long) x + 5 + i), new Color(0x7A6444), 1.6f);
                forma(g, organica(x + 40 + i * 56, 214, 4, 12, 2, (long) x + 9 + i), new Color(0xB9B9A8), 1.2f);
            }
            forma(g, organica(x - 6, 184, w + 12, 14, 4, (long) x + 3), new Color(0xB9C3BF), 2.2f);
            g.setColor(new Color(255, 255, 255, 100));
            g.fill(organica(x, 187, w - 6, 3, 1, (long) x + 4));
        }
        forma(g, organica(650, 176, 80, 12, 4, 40), new Color(0x8E9A96), 2f);   // pileta
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(690, 178, 690, 152, 672, 152));
        // cafetera (cuerpo)
        forma(g, organica(212, 124, 64, 62, 9, 41), new Color(0x3A3F45), 2.4f);
        forma(g, organica(222, 134, 44, 16, 4, 42), new Color(0x22262A), 1.8f);
        forma(g, organica(238, 164, 14, 20, 3, 43), new Color(0x161A1D), 1.6f);
        forma(g, organica(228, 176, 28, 9, 3, 44), new Color(0xB9B9A8), 1.6f);
        textoIzq(g, "Cafetera", new Font("Segoe UI", Font.BOLD, 11), new Color(0xE9E4D0), 216, 118);
        // heladera (cerrada: el estado roto se dibuja arriba)
        heladeraCuerpo(g, 56, 82, 116, 164);
        // ollas colgadas
        g.setColor(new Color(0x4A504F));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(610, 100, 800, 100));
        for (int i = 0; i < 4; i++) {
            double x = 626 + i * 44;
            g.setColor(TRAZO);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Line2D.Double(x, 100, x, 112));
            forma(g, new Ellipse2D.Double(x - 14, 112, 28, 20 + (i % 2) * 6), new Color(i % 2 == 0 ? 0x6C7773 : 0xA8794A), 2f);
        }

        // zócalo y piso
        forma(g, organica(-4, 238, W + 8, 10, 3, 50), new Color(0x4A504F), 1.8f);
        baldosas(g, 0, 248, W, 272, 46, new Color(0x5B6B64), new Color(0x4F5E58), 51);
        g.setPaint(new GradientPaint(0, 248, new Color(0, 0, 0, 110), 0, 296, new Color(0, 0, 0, 0)));
        g.fillRect(0, 248, W, 50);
        suave(g, mancha(480, 290, 90, 20, 52), new Color(80, 50, 10, 36));
        for (int x : new int[]{300, 600}) lampara(g, x);
        puerta(g, false, "SALÓN", new Color(0xF0C98A));
    }

    static void heladeraCuerpo(Graphics2D g, double x, double y, double w, double h) {
        sombra(g, x + w / 2, y + h + 1, w * 0.6, 8, 100);
        forma(g, organica(x, y, w, h, 12, 200), new Color(0xB8C2BE), 2.6f);
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(new Line2D.Double(x + 2, y + h * 0.36, x + w - 2, y + h * 0.36));
        forma(g, organica(x + 10, y + 16, 7, 34, 3, 201), new Color(0x4D5A55), 1.6f);
        forma(g, organica(x + 10, y + h * 0.36 + 14, 7, 54, 3, 202), new Color(0x4D5A55), 1.6f);
        forma(g, organica(x + 54, y + 70, 14, 14, 2, 203), new Color(0xD9A441), 1.4f);
        forma(g, organica(x + 76, y + 92, 14, 14, 2, 204), new Color(0xC4513F), 1.4f);
        g.setColor(new Color(255, 255, 255, 80));
        g.fill(organica(x + w - 18, y + 8, 6, h - 16, 3, 205));
        textoIzq(g, "Heladera", new Font("Segoe UI", Font.BOLD, 11), new Color(0x3A4A44), x + 28, y - 4);
    }

    // ================= ALMACÉN =================

    private static void almacen(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x3A4342), 0, 240, new Color(0x2C3433)));
        g.fillRect(0, 0, W, 240);
        Random r = new Random(6);
        g.setStroke(new BasicStroke(1.2f));
        for (int j = 0; j < 12; j++) {   // ladrillos
            for (int i = 0; i < W / 48 + 1; i++) {
                float v = (r.nextFloat() - 0.5f) * 0.12f;
                Color base = new Color(0x414B49);
                g.setColor(v > 0 ? claro(base, v) : oscuro(base, -v));
                g.fillRect(i * 48 - (j % 2) * 24, j * 20, 47, 19);
            }
        }
        ruido(g, new Rectangle2D.Double(0, 0, W, 240), 3000, 7, Color.BLACK, 44);
        for (int i = 0; i < 5; i++) suave(g, mancha(60 + i * 200, 40 + (i % 2) * 40, 40, 70, 8 + i), new Color(0, 0, 0, 34));
        // caño con juntas y gotera
        forma(g, organica(-4, 52, W + 8, 12, 5, 9), new Color(0x6B5B4A), 2.2f);
        for (int x : new int[]{120, 420, 700}) forma(g, organica(x, 49, 12, 18, 2, x), new Color(0x8A7560), 1.8f);
        suave(g, new Ellipse2D.Double(424, 66, 4, 7), new Color(120, 170, 200, 180));
        // estantes de metal
        estante(g, 96, 90, 290, 150);
        estante(g, 440, 90, 330, 150);
        Figura.icono(g, "TRAPEADOR", 130, 112, 1.3);
        Figura.icono(g, "TRAPO", 190, 125, 1.2);
        Figura.icono(g, "RASQUETA", 252, 120, 1.2);
        Figura.icono(g, "BOLSA", 318, 124, 1.2);
        Figura.icono(g, "DESTAPADOR", 480, 118, 1.3);
        Figura.icono(g, "FUSIBLE", 570, 128, 1.3);
        Figura.icono(g, "CAJA", 660, 124, 1.3);
        for (int i = 0; i < 4; i++) {   // relleno de los estantes
            forma(g, organica(110 + i * 66, 160, 48, 24, 3, 300 + i), new Color(0x8A6A3B), 1.8f);
            forma(g, organica(458 + i * 76, 162, 54, 22, 3, 310 + i), new Color(i % 2 == 0 ? 0x7A8F86 : 0x8A6A3B), 1.8f);
        }
        textoIzq(g, "LIMPIEZA", new Font("Georgia", Font.BOLD, 14), new Color(0xD9A441), 106, 84);
        textoIzq(g, "REPUESTOS", new Font("Georgia", Font.BOLD, 14), new Color(0xD9A441), 452, 84);
        lampara(g, 410);
        forma(g, organica(-4, 238, W + 8, 10, 3, 11), new Color(0x2E3635), 1.8f);
        baldosas(g, 0, 248, W, 272, 60, new Color(0x4D5553), new Color(0x464E4C), 12);
        g.setPaint(new GradientPaint(0, 248, new Color(0, 0, 0, 120), 0, 300, new Color(0, 0, 0, 0)));
        g.fillRect(0, 248, W, 52);
        suave(g, mancha(250, 330, 100, 22, 13), new Color(20, 40, 60, 40));
        puerta(g, true, "SALÓN", new Color(0xF0C98A));
    }

    private static void estante(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x, y, x, y + h));
        g.draw(new Line2D.Double(x + w, y, x + w, y + h));
        g.setColor(new Color(0x8E9A96));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x, y, x, y + h));
        g.draw(new Line2D.Double(x + w, y, x + w, y + h));
        for (double yy : new double[]{y + 44, y + 98, y + h - 4}) {
            forma(g, organica(x - 4, yy, w + 8, 6, 2, (long) (yy + x)), new Color(0x8E9A96), 1.8f);
        }
    }

    // ---------- objetos con profundidad (los dibuja la escena según la altura de sus pies) ----------

    static void mesa(Graphics2D g, double x, boolean sucia, int variante) {
        sombra(g, x + 60, 438, 70, 10, 100);
        for (double dx : new double[]{x + 10, x + 101}) forma(g, organica(dx, 406, 9, 32, 3, (long) dx), new Color(0x6B4A2F), 2f);
        forma(g, organica(x - 2, 388, 124, 22, 10, 400 + variante), new Color(0xC79A55), 2.4f);
        vetas(g, x - 2, 388, 124, 22, 401 + variante, new Color(0x6B4A22));
        g.setColor(new Color(255, 240, 200, 90));
        g.fill(organica(x + 6, 391, 108, 4, 2, 402));
        // taburetes
        for (double dx : new double[]{x - 18, x + 106}) {
            forma(g, organica(dx, 424, 8, 16, 3, (long) dx + 7), new Color(0x6B4A2F), 1.6f);
            forma(g, organica(dx - 6, 416, 20, 8, 4, (long) dx + 8), new Color(0x9C7242), 1.8f);
        }
        if (sucia) {
            suave(g, mancha(x + 32, 398, 17, 5, 410 + variante), new Color(0x4A3322));
            suave(g, mancha(x + 78, 400, 20, 4, 420 + variante), new Color(0x4A3322));
            suave(g, mancha(x + 55, 394, 8, 3, 430), new Color(0x7A3B2E));
            g.setColor(new Color(0xE0C98A));
            for (int i = 0; i < 6; i++) g.fill(new Ellipse2D.Double(x + 12 + i * 17, 402 - (i % 2) * 2, 3.5, 2.6));
            forma(g, new Ellipse2D.Double(x + 80 - variante * 50, 380, 30, 10), new Color(0xEDEBDD), 1.8f);
            suave(g, mancha(x + 92 - variante * 50, 383, 8, 3, 440), new Color(0xB5483A));
            forma(g, organica(x + 22 + variante * 42, 395, 18, 8, 4, 450), new Color(0xA9C7CC), 1.6f);
        } else {
            forma(g, organica(x + 52, 376, 16, 14, 3, 460), new Color(0xEDEBDD), 1.8f);
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(organica(x + 14, 393, 44, 3, 2, 461));
        }
    }

    static void isla(Graphics2D g, Rectangle r) {
        sombra(g, r.getCenterX(), r.y + r.height + 14, r.width * 0.55, 10, 110);
        for (double dx : new double[]{r.x + 10, r.x + r.width - 20}) forma(g, organica(dx, r.y + 14, 10, 34, 3, (long) dx), new Color(0x8E9A96), 2f);
        forma(g, organica(r.x - 6, r.y - 8, r.width + 12, 26, 6, 500), new Color(0xB9C3BF), 2.4f);
        g.setColor(new Color(255, 255, 255, 110));
        g.fill(organica(r.x + 4, r.y - 5, r.width - 8, 4, 2, 501));
        forma(g, organica(r.x + 40, r.y - 20, 56, 12, 4, 502), new Color(0xC79A55), 1.8f);   // tabla de cortar
        forma(g, organica(r.x + 110, r.y - 17, 34, 7, 3, 503), new Color(0xB9C3BF), 1.6f);   // cuchillo
        forma(g, organica(r.x + 146, r.y - 17, 14, 7, 3, 504), new Color(0x3A2A1E), 1.6f);
        forma(g, new Ellipse2D.Double(r.x + 190, r.y - 22, 28, 14), new Color(0x6C7773), 2f);   // cacerola
    }

    static void cajas(Graphics2D g, Rectangle r) {
        sombra(g, r.getCenterX(), r.y + r.height + 6, r.width * 0.6, 9, 110);
        forma(g, organica(r.x, r.y + 28, 62, 44, 3, 600), new Color(0xB98A4E), 2.2f);
        forma(g, organica(r.x + 56, r.y + 36, 54, 36, 3, 601), new Color(0xA8794A), 2.2f);
        forma(g, organica(r.x + 12, r.y - 4, 50, 34, 3, 602), new Color(0xC79A55), 2.2f);
        g.setColor(new Color(0x7A5A2E));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Line2D.Double(r.x + 2, r.y + 44, r.x + 60, r.y + 44));
        g.draw(new Line2D.Double(r.x + 58, r.y + 52, r.x + 108, r.y + 52));
        g.draw(new Line2D.Double(r.x + 14, r.y + 12, r.x + 60, r.y + 12));
        textoIzq(g, "FRÁGIL", new Font("Segoe UI", Font.BOLD, 9), new Color(0x5A3F1E), r.x + 20, r.y + 24);
    }

    static void barril(Graphics2D g, Rectangle r) {
        sombra(g, r.getCenterX(), r.y + r.height + 8, 34, 8, 110);
        forma(g, organica(r.x, r.y - 22, r.width, r.height + 28, 14, 700), new Color(0x4F6A7A), 2.4f);
        g.setColor(TRAZO);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(r.x + 1, r.y - 4, r.x + r.width - 1, r.y - 4));
        g.draw(new Line2D.Double(r.x + 1, r.y + 18, r.x + r.width - 1, r.y + 18));
        forma(g, new Ellipse2D.Double(r.x + 4, r.y - 28, r.width - 8, 12), new Color(0x2C3A40), 1.8f);
    }

    // ---------- puertas de los bordes ----------

    private static void puerta(Graphics2D g, boolean izquierda, String rotulo, Color luz) {
        double x = izquierda ? 0 : W - 46;
        forma(g, organica(x - (izquierda ? 8 : 0), 214, 54, 276, 3, izquierda ? 800 : 801), new Color(0x4A3622), 2.6f);
        g.setPaint(new GradientPaint((float) x, 0, claro(luz, 0.1f), (float) x + 44, 0, oscuro(luz, 0.55f)));
        g.fill(organica(x + (izquierda ? 0 : 4), 226, 40, 262, 2, izquierda ? 802 : 803));
        g.setColor(new Color(0, 0, 0, 90));   // el hueco se pierde en sombra
        g.fill(organica(x + (izquierda ? 0 : 4), 226, 40, 262, 2, izquierda ? 802 : 803));
        brillo(g, izquierda ? x + 6 : x + 38, 430, 70, luz, 90);
        forma(g, organica(izquierda ? x + 2 : x - 2, 232, 44, 18, 4, 804), new Color(0x2C3A35), 1.8f);
        textoIzq(g, rotulo, new Font("Georgia", Font.BOLD, rotulo.length() > 6 ? 7 : 9), luz, izquierda ? x + 4 : x + 1, 244);
        forma(g, organica(izquierda ? x : x - 10, 462, 54, 14, 5, 805), new Color(0x7A3B2E), 1.8f);   // felpudo
    }
}
