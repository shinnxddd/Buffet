package ui;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.PersistenciaException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Jugador;
import modelo.Objeto;
import modelo.Tarea;
import service.JuegoService;
import service.PartidaService;
import ui.EscenaBuffet.Opcion;
import ui.EscenaBuffet.Zona;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de juego. El personaje camina por el buffet (EscenaBuffet) y, al interactuar con un lugar,
 * se ejecuta la misma acción de la consola: cliente, tareas o inventario.
 */
public class PantallaJuego extends JPanel {

    private final JuegoService juego;
    private final PartidaService partidas;
    private final Runnable alSalir;
    private final EscenaBuffet escena;
    private int corduraPrevia;

    private final JLabel lblNombre = Tema.etiqueta("", Tema.SUBTITULO, Tema.TEXTO);
    private final JLabel lblDia = Tema.etiqueta("", Tema.CUERPO, Tema.TEXTO_SUAVE);
    private final JLabel lblTareas = Tema.etiqueta("", Tema.CUERPO, Tema.TEXTO);
    private final JProgressBar barra = new JProgressBar(0, 100);
    private final JTextArea txtRegistro = Tema.texto(Tema.CUERPO, Tema.TEXTO_SUAVE);

    public PantallaJuego(JuegoService juego, PartidaService partidas, Runnable alSalir) {
        this.juego = juego;
        this.partidas = partidas;
        this.alSalir = alSalir;
        this.escena = new EscenaBuffet(juego, this::interactuar);
        this.corduraPrevia = juego.getJugador().getCordura();

        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(20, 28, 16, 28));
        setBackground(Tema.FONDO);

        add(armarBarraSuperior(), BorderLayout.NORTH);
        add(armarCentro(), BorderLayout.CENTER);
        add(Tema.etiqueta("Mover: WASD o flechas   ·   Interactuar: E   ·   Decidir: 1, 2, 3 o clic",
                Tema.CUERPO, Tema.TEXTO_SUAVE), BorderLayout.SOUTH);
        actualizarHud();
    }

    // ---------- armado de la pantalla ----------

    private JPanel armarBarraSuperior() {
        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));
        izquierda.add(lblNombre);
        izquierda.add(lblDia);

        barra.setStringPainted(true);
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 28, 0, 28);
        centro.add(barra, c);

        JButton btnGuardar = Tema.primario("Guardar partida");
        JButton btnSalir = Tema.secundario("Salir al menú");
        btnGuardar.setFocusable(false);   // el teclado queda siempre en la escena
        btnSalir.setFocusable(false);
        btnGuardar.addActionListener(e -> guardar());
        btnSalir.addActionListener(e -> {
            if (confirmarSalida()) {
                alSalir.run();
            }
        });
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        derecha.add(lblTareas);
        derecha.add(btnGuardar);
        derecha.add(btnSalir);

        JPanel hud = Tema.tarjeta(new BorderLayout());
        hud.add(izquierda, BorderLayout.WEST);
        hud.add(centro, BorderLayout.CENTER);
        hud.add(derecha, BorderLayout.EAST);
        return hud;
    }

    private JPanel armarCentro() {
        JPanel marco = new JPanel(new BorderLayout());
        marco.setBorder(BorderFactory.createLineBorder(Tema.BORDE, 2));
        marco.add(escena, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(txtRegistro);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        JPanel registro = Tema.tarjeta(new BorderLayout(0, 10));
        registro.setPreferredSize(new Dimension(230, 0));
        registro.add(Tema.etiqueta("Registro del turno", Tema.CUERPO.deriveFont(Font.BOLD, 15f), Tema.TEXTO), BorderLayout.NORTH);
        registro.add(scroll, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(14, 0));
        centro.setOpaque(false);
        centro.add(marco, BorderLayout.CENTER);
        centro.add(registro, BorderLayout.EAST);
        return centro;
    }

    // ---------- estado visible ----------

    private void actualizarHud() {
        Jugador j = juego.getJugador();
        lblNombre.setText(j.getNombre());
        lblDia.setText("Día " + j.getDiaActual());
        lblTareas.setText("Tareas completadas: " + j.getTareasCompletadas());
        barra.setValue(Math.max(0, Math.min(100, j.getCordura())));
        barra.setString("Cordura " + j.getCordura());
        barra.setForeground(j.getCordura() <= 30 ? Tema.PELIGRO : Tema.BIEN);
        if (j.getCordura() != corduraPrevia) {
            escena.efectoCordura(j.getCordura() - corduraPrevia);
            corduraPrevia = j.getCordura();
        }
    }

    /** Cierra el diálogo y muestra (y registra) el resultado de la última decisión. */
    private void volver(String mensaje) {
        escena.cerrarDialogo();
        if (mensaje != null) {
            log(mensaje);
            escena.aviso(capitalizar(mensaje));
        }
        actualizarHud();
        escena.requestFocusInWindow();
    }

    private void error(Exception e) {
        Dialogos.error(this, e);
        escena.requestFocusInWindow();
    }

    private void log(String mensaje) {
        txtRegistro.append("• " + capitalizar(mensaje) + "\n\n");
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------- interacción física -> acción del juego ----------

    private void interactuar(Zona zona) {
        switch (zona) {
            case CLIENTE:
                esperarCliente();
                break;
            case TAREA:
                realizarTarea();
                break;
            case INVENTARIO:
                abrirInventario();
                break;
        }
    }

    // ---------- 1) esperar cliente ----------

    private void esperarCliente() {
        try {
            Cliente c = juego.esperarCliente();
            escena.mostrarDialogo(c.getNombre().toUpperCase() + " !!!!!!!!!!!!",
                    capitalizar(c.getNombre()) + " dice: \"Hola, quiero " + c.getPedido() + "\"",
                    new Opcion(capitalizar("dar " + c.getPedido()), () -> responderCliente(c, JuegoService.CLIENTE_DAR)),
                    new Opcion(capitalizar("no dar " + c.getPedido()), () -> responderCliente(c, JuegoService.CLIENTE_NO_DAR)),
                    new Opcion("Preguntar por qué", () -> responderCliente(c, JuegoService.CLIENTE_PREGUNTAR)));
        } catch (InteraccionInvalidaException e) {
            error(e);
        }
    }

    private void responderCliente(Cliente cliente, int decision) {
        try {
            String mensaje = juego.responderCliente(cliente, decision);
            if (decision == JuegoService.CLIENTE_DAR) {
                escena.reaccionCliente(true);
            } else if (decision == JuegoService.CLIENTE_NO_DAR) {
                escena.reaccionCliente(false);
            }
            volver(mensaje);
        } catch (InteraccionInvalidaException | PedidoInvalidoException e) {
            error(e);
            volver(null);
        }
    }

    // ---------- 2) realizar tareas ----------

    private void realizarTarea() {
        try {
            Tarea t = juego.realizarTarea();
            escena.mostrarDialogo("TAREAAAAAAAAAAAA!!!!!!!!!!!!!!!!!!!!!!", capitalizar(t.getDescripcion()),
                    new Opcion("Hacer la tarea", () -> responderTarea(t, JuegoService.TAREA_HACER)),
                    new Opcion("No hacer la tarea", () -> responderTarea(t, JuegoService.TAREA_POSPONER)),
                    new Opcion("Romperlo todo", () -> responderTarea(t, JuegoService.TAREA_ROMPER)));
        } catch (TareaNoDisponibleException e) {
            error(e);
        }
    }

    private void responderTarea(Tarea tarea, int decision) {
        try {
            volver(juego.responderTarea(tarea, decision));
        } catch (TareaNoDisponibleException | InteraccionInvalidaException e) {
            error(e);
            volver(null);
        }
    }

    // ---------- 3) abrir inventario ----------

    private void abrirInventario() {
        List<Objeto> objetos = juego.abrirInventario();
        StringBuilder cuerpo = new StringBuilder();
        List<Opcion> opciones = new ArrayList<>();
        if (objetos.isEmpty()) {
            cuerpo.append("(vacío)");
        }
        for (Objeto o : objetos) {
            cuerpo.append("- ").append(o.getNombre()).append("\n");
            opciones.add(new Opcion("Usar " + o.getNombre(), () -> usarObjeto(o)));
        }
        opciones.add(new Opcion("Cerrar inventario", () -> volver(null)));
        escena.mostrarDialogo("INVENTARIO", cuerpo.toString().trim(), opciones.toArray(new Opcion[0]));
    }

    private void usarObjeto(Objeto objeto) {
        try {
            volver(juego.usarObjeto(objeto));
        } catch (ObjetoNoDisponibleException e) {
            error(e);   // se queda en el inventario
        }
    }

    // ---------- guardar / salir ----------

    private boolean guardar() {
        try {
            partidas.guardarProgreso(juego.getJugador());
            log("Partida guardada.");
            escena.aviso("Partida guardada.");
            return true;
        } catch (PersistenciaException e) {
            error(e);
            return false;
        }
    }

    /** Pregunta si guardar antes de salir. Devuelve true si se puede abandonar la partida. */
    public boolean confirmarSalida() {
        int r = JOptionPane.showConfirmDialog(this, "¿Querés guardar la partida antes de salir?",
                "Salir al menú", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        return r != JOptionPane.YES_OPTION || guardar();
    }
}
