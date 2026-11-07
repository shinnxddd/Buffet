package main;

import ui.Tema;
import ui.VentanaPrincipal;

import javax.swing.SwingUtilities;

/** Punto de entrada: abre la ventana gráfica. (La versión de consola quedó en referencia-consola/.) */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            new VentanaPrincipal().setVisible(true);
        });
    }
}
