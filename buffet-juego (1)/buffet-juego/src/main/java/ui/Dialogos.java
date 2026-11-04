package ui;

import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.PersistenciaException;
import excepciones.TareaNoDisponibleException;

import javax.swing.*;
import java.awt.Component;

/** Cuadros de diálogo para informar los errores al usuario (nunca por consola). */
final class Dialogos {

    private Dialogos() { }

    static void error(Component padre, Exception e) {
        int tipo = JOptionPane.WARNING_MESSAGE;
        String titulo;
        if (e instanceof DiaInvalidoException) {
            titulo = "Día no válido";
        } else if (e instanceof InteraccionInvalidaException) {
            titulo = "Acción no disponible";
        } else if (e instanceof TareaNoDisponibleException) {
            titulo = "Tarea no disponible";
        } else if (e instanceof PedidoInvalidoException) {
            titulo = "Pedido no válido";
        } else if (e instanceof ObjetoNoDisponibleException) {
            titulo = "Objeto no disponible";
        } else if (e instanceof PersistenciaException) {
            titulo = "Problema con la base de datos";
            tipo = JOptionPane.ERROR_MESSAGE;
        } else {
            titulo = "Error";
            tipo = JOptionPane.ERROR_MESSAGE;
        }
        aviso(padre, titulo, e.getMessage(), tipo);
    }

    /** Error de formato en un campo (por ejemplo, un texto donde va un número). */
    static void formato(Component padre, String mensaje) {
        aviso(padre, "Dato incorrecto", mensaje, JOptionPane.WARNING_MESSAGE);
    }

    private static void aviso(Component padre, String titulo, String mensaje, int tipo) {
        String seguro = mensaje.replace("&", "&amp;").replace("<", "&lt;");
        JOptionPane.showMessageDialog(padre,
                "<html><body style='width:300px'>" + seguro + "</body></html>", titulo, tipo);
    }
}
