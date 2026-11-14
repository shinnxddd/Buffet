package ui;

import modelo.Jugador;
import service.PartidaService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * Ventana única de la aplicación. El menú y el juego comparten el mismo lienzo
 * interno de 384x240 y se muestran con un factor entero (hasta 4x en 1920x1080)
 * para que ningún píxel quede deformado.
 */
public class VentanaPrincipal extends JFrame {

    private static final String MENU = "menu";
    private static final String JUEGO = "juego";

    private final CardLayout cartas = new CardLayout();
    private final JPanel raiz = new JPanel(cartas);
    private final PartidaService partidas = new PartidaService();
    private final PantallaPartidas pantallaPartidas = new PantallaPartidas(partidas, this::jugar);
    private PantallaJuego pantallaJuego;

    public VentanaPrincipal() {
        super("Buffet Terror");
        raiz.setBackground(Tema.FONDO);
        raiz.add(pantallaPartidas, MENU);
        setContentPane(raiz);
        setIconImage(crearIcono());
        setUndecorated(true);
        setResizable(false);

        // Cerrar con la X guarda el avance en medio de una partida.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (pantallaJuego == null || pantallaJuego.confirmarSalida()) {
                    dispose();
                }
            }
        });

        // La primera carga de la lista ocurre con la ventana ya visible.
        SwingUtilities.invokeLater(() -> {
            pantallaPartidas.refrescar();
            pantallaPartidas.requestFocusInWindow();
        });
    }

    private void jugar(Jugador jugador) {
        pantallaJuego = new PantallaJuego(partidas.iniciarJuego(jugador), partidas, this::volverAlMenu);
        raiz.add(pantallaJuego, JUEGO);
        cartas.show(raiz, JUEGO);
        raiz.revalidate();
        raiz.repaint();
        SwingUtilities.invokeLater(() -> pantallaJuego.requestFocusInWindow());
    }

    private void volverAlMenu() {
        cartas.show(raiz, MENU);
        raiz.remove(pantallaJuego);
        pantallaJuego = null;
        pantallaPartidas.refrescar(); // el progreso guardado se ve en la lista
        raiz.revalidate();
        raiz.repaint();
        SwingUtilities.invokeLater(pantallaPartidas::requestFocusInWindow);
    }

    /** Pantalla completa: el lienzo se escala solo dentro de cada panel. */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().setFullScreenWindow(this);
        }
    }

    /** Ícono de 16x16 dibujado píxel por píxel y ampliado sin suavizado. */
    private static Image crearIcono() {
        int lado = 16;
        BufferedImage chico = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = chico.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g.setColor(new Color(0x0D1512));
        g.fillRect(0, 0, 16, 16);
        g.setColor(new Color(0x22352D));
        g.fillRect(1, 1, 14, 14);
        g.setColor(new Color(0x3A4A43));
        g.fillRect(2, 2, 12, 12);

        // bandeja
        g.setColor(new Color(0x8A9691));
        g.fillRect(2, 10, 12, 3);
        g.setColor(new Color(0xC3CCC6));
        g.fillRect(3, 9, 10, 3);
        g.setColor(new Color(0x5A6A60));
        g.fillRect(2, 13, 12, 1);

        // ojo
        g.setColor(new Color(0xF1EAD8));
        g.fillRect(5, 6, 6, 5);
        g.setColor(new Color(0x7FB58A));
        g.fillRect(6, 7, 4, 3);
        g.setColor(new Color(0x120F0C));
        g.fillRect(7, 7, 2, 3);
        g.setColor(new Color(0xC4513F));
        g.fillRect(4, 8, 1, 1);
        g.fillRect(11, 8, 1, 1);

        // brillo
        g.setColor(new Color(0xD9A441));
        g.fillRect(5, 4, 6, 1);
        g.dispose();

        int grande = 64;
        BufferedImage imagen = new BufferedImage(grande, grande, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = imagen.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gg.drawImage(chico, 0, 0, grande, grande, null);
        gg.dispose();
        return imagen;
    }
}