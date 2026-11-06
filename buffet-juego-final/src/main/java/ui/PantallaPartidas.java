package ui;

import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.PersistenciaException;
import modelo.Jugador;
import service.PartidaService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Menú principal: formulario de registro + tabla de partidas guardadas (alta, modificación, baja, consulta). */
public class PantallaPartidas extends JPanel {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Día", "Cordura", "Tareas completadas"};

    private final PartidaService service;
    private final Consumer<Jugador> alJugar;

    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = Tema.etiqueta("", Tema.CUERPO, Tema.TEXTO_SUAVE);

    private final JLabel lblFormulario = Tema.etiqueta("Nueva partida", Tema.SUBTITULO, Tema.TEXTO);
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtDia = new JTextField("1");
    private final JButton btnGuardar = Tema.primario("Crear partida");
    private final JButton btnCancelar = Tema.secundario("Cancelar edición");

    private final JButton btnJugar = Tema.primario("Jugar");
    private final JButton btnEditar = Tema.secundario("Editar");
    private final JButton btnEliminar = Tema.peligro("Eliminar");

    private List<Jugador> partidas = new ArrayList<>();
    private Jugador enEdicion;   // null = el formulario crea una partida nueva

    public PantallaPartidas(PartidaService service, Consumer<Jugador> alJugar) {
        this.service = service;
        this.alJugar = alJugar;

        setLayout(new BorderLayout(0, 24));
        setBorder(new EmptyBorder(24, 32, 24, 32));
        setBackground(Tema.FONDO);

        add(armarEncabezado(), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(20, 0));
        centro.setOpaque(false);
        centro.add(armarFormulario(), BorderLayout.WEST);
        centro.add(armarTabla(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        actualizarBotones();
    }

    private JPanel armarEncabezado() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel titulo = Tema.etiqueta("Buffet Terror", Tema.TITULO, Tema.TEXTO);
        JLabel sub = Tema.etiqueta("Creá una partida o elegí una guardada para empezar tu turno.",
                Tema.CUERPO_GRANDE, Tema.TEXTO_SUAVE);
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(titulo);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        return p;
    }

    private JPanel armarFormulario() {
        JPanel form = Tema.tarjeta(null);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(320, 0));

        txtNombre.putClientProperty("JTextField.placeholderText", "Ej: Octavio");
        txtNombre.addActionListener(e -> guardar());
        txtDia.addActionListener(e -> guardar());
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> cancelarEdicion());
        btnCancelar.setVisible(false);

        form.add(alinear(lblFormulario));
        form.add(Box.createVerticalStrut(18));
        form.add(alinear(Tema.etiqueta("Nombre del jugador", Tema.CUERPO, Tema.TEXTO_SUAVE)));
        form.add(Box.createVerticalStrut(4));
        form.add(alinear(txtNombre));
        form.add(Box.createVerticalStrut(14));
        form.add(alinear(Tema.etiqueta("Día inicial", Tema.CUERPO, Tema.TEXTO_SUAVE)));
        form.add(Box.createVerticalStrut(4));
        form.add(alinear(txtDia));
        form.add(Box.createVerticalStrut(20));
        form.add(alinear(btnGuardar));
        form.add(Box.createVerticalStrut(8));
        form.add(alinear(btnCancelar));
        form.add(Box.createVerticalGlue());
        return form;
    }

    private JPanel armarTabla() {
        tabla.setRowHeight(34);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setMaxWidth(60);
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i < COLUMNAS.length; i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(centrado);
        }
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    jugar();
                }
            }
        });
        btnJugar.addActionListener(e -> jugar());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnJugar);
        botones.add(btnEditar);
        botones.add(btnEliminar);

        JPanel card = Tema.tarjeta(new BorderLayout(0, 12));
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        arriba.add(Tema.etiqueta("Partidas guardadas", Tema.SUBTITULO, Tema.TEXTO), BorderLayout.NORTH);
        arriba.add(lblResumen, BorderLayout.SOUTH);
        card.add(arriba, BorderLayout.NORTH);
        card.add(new JScrollPane(tabla), BorderLayout.CENTER);
        card.add(botones, BorderLayout.SOUTH);
        return card;
    }

    private static JComponent alinear(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        if (c instanceof JTextField || c instanceof JButton) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        return c;
    }

    /** Vuelve a leer la tabla desde MySQL (a través del Service). */
    public void refrescar() {
        try {
            partidas = service.listar();
        } catch (PersistenciaException e) {
            partidas = new ArrayList<>();
            Dialogos.error(this, e);
        }
        modelo.setRowCount(0);
        for (Jugador j : partidas) {
            modelo.addRow(new Object[]{j.getId(), j.getNombre(), j.getDiaActual(), j.getCordura(), j.getTareasCompletadas()});
        }
        lblResumen.setText(partidas.isEmpty()
                ? "Todavía no hay partidas guardadas. Creá la primera con el formulario."
                : partidas.size() + (partidas.size() == 1 ? " partida guardada" : " partidas guardadas"));
        actualizarBotones();
    }

    private void guardar() {
        int dia;
        try {
            dia = Integer.parseInt(txtDia.getText().trim());
        } catch (NumberFormatException e) {
            Dialogos.formato(this, "El día tiene que ser un número entero, por ejemplo 1.");
            return;
        }
        try {
            if (enEdicion == null) {
                service.registrar(txtNombre.getText(), dia);
            } else {
                service.modificar(enEdicion.getId(), txtNombre.getText(), dia);
            }
            cancelarEdicion();   // limpia el formulario y lo deja en modo "nueva partida"
            refrescar();
        } catch (DiaInvalidoException | InteraccionInvalidaException | PersistenciaException e) {
            Dialogos.error(this, e);
        }
    }

    private void editar() {
        Jugador j = seleccionada();
        if (j == null) {
            return;
        }
        enEdicion = j;
        txtNombre.setText(j.getNombre());
        txtDia.setText(String.valueOf(j.getDiaActual()));
        lblFormulario.setText("Editar partida");
        btnGuardar.setText("Guardar cambios");
        btnCancelar.setVisible(true);
        txtNombre.requestFocusInWindow();
    }

    private void cancelarEdicion() {
        enEdicion = null;
        txtNombre.setText("");
        txtDia.setText("1");
        lblFormulario.setText("Nueva partida");
        btnGuardar.setText("Crear partida");
        btnCancelar.setVisible(false);
    }

    private void eliminar() {
        Jugador j = seleccionada();
        if (j == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la partida de " + j.getNombre() + "? No se puede deshacer.",
                "Eliminar partida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.eliminar(j.getId());
            if (enEdicion != null && enEdicion.getId() == j.getId()) {
                cancelarEdicion();
            }
            refrescar();
        } catch (PersistenciaException e) {
            Dialogos.error(this, e);
        }
    }

    private void jugar() {
        Jugador j = seleccionada();
        if (j != null) {
            alJugar.accept(j);
        }
    }

    private Jugador seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : partidas.get(fila);
    }

    private void actualizarBotones() {
        boolean hay = tabla.getSelectedRow() >= 0;
        btnJugar.setEnabled(hay);
        btnEditar.setEnabled(hay);
        btnEliminar.setEnabled(hay);
    }
}
