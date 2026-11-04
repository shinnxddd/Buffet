package ui;

import modelo.Jugador;
import service.PartidaService;

import javax.swing.*;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana única de la aplicación: alterna entre el menú de partidas y el juego. */
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
        raiz.add(pantallaPartidas, MENU);
        setContentPane(raiz);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1200, 760);
        setLocationRelativeTo(null);

        // Cerrar con la X en medio de una partida ofrece guardar antes.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (pantallaJuego == null || pantallaJuego.confirmarSalida()) {
                    dispose();
                }
            }
        });

        // La primera carga de la tabla ocurre con la ventana ya visible.
        SwingUtilities.invokeLater(pantallaPartidas::refrescar);
    }

    private void jugar(Jugador jugador) {
        pantallaJuego = new PantallaJuego(partidas.iniciarJuego(jugador), partidas, this::volverAlMenu);
        raiz.add(pantallaJuego, JUEGO);
        cartas.show(raiz, JUEGO);
    }

    private void volverAlMenu() {
        cartas.show(raiz, MENU);
        raiz.remove(pantallaJuego);
        pantallaJuego = null;
        pantallaPartidas.refrescar();   // el progreso guardado se ve en la tabla
    }
}
