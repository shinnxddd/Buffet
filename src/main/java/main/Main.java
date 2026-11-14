package main;

import ui.Tema;
import ui.VentanaPrincipal;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada: abre la ventana gráfica del juego.
 */
public class Main {

    public static void main(String[] args) {
        // El pixel art se escala por factores enteros. Si Windows aplica un zoom
        // fraccionario (125 %, 150 %), el lienzo queda borroso: lo desactivamos
        // antes de que AWT inicialice el toolkit.
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.uiScale.enabled", "false");

        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            new VentanaPrincipal().setVisible(true);
        });
    }
}
