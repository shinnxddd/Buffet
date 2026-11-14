package ui;

import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.PersistenciaException;
import modelo.Jugador;
import service.PartidaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Menú principal dibujado a mano sobre el mismo lienzo de 384x240 que el juego.
 * La idea es que la primera pantalla también sea pixel art: lista de partidas,
 * ficha del jugador, formulario de alta/edición y confirmación de borrado.
 */
public class PantallaPartidas extends JPanel {

    private static final int ANCHO = EscenaBuffet.ANCHO;
    private static final int ALTO = EscenaBuffet.ALTO;

    private static final int FILAS = 6;
    private static final int FILA_ALTO = 16;
    private static final int FILA_Y = 84;

    private static final Rectangle LISTA = new Rectangle(14, 52, 356, 130);
    private static final Rectangle BTN_JUGAR = new Rectangle(16, 188, 110, 18);
    private static final Rectangle BTN_NUEVA = new Rectangle(132, 188, 76, 18);
    private static final Rectangle BTN_EDITAR = new Rectangle(214, 188, 76, 18);
    private static final Rectangle BTN_BORRAR = new Rectangle(296, 188, 72, 18);

    private static final Rectangle CAMPO_NOMBRE = new Rectangle(76, 82, 232, 17);
    private static final Rectangle CAMPO_DIA = new Rectangle(76, 116, 64, 17);
    private static final Rectangle BTN_GUARDAR = new Rectangle(76, 156, 120, 18);
    private static final Rectangle BTN_CANCELAR = new Rectangle(204, 156, 104, 18);

    private static final Rectangle BTN_BORRAR_SI = new Rectangle(86, 146, 108, 18);
    private static final Rectangle BTN_BORRAR_NO = new Rectangle(202, 146, 104, 18);

    private enum Capa { LISTA, FORMULARIO, BORRAR }

    private final PartidaService service;
    private final Consumer<Jugador> alJugar;
    private final BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
    private final Timer timer;
    private final DefaultTableModel modeloPartidas;
    private final JTable tablaPartidas;

    private List<Jugador> partidas = new ArrayList<>();
    private int seleccion;
    private int desplazamiento;
    private int tick;

    private String error = "";
    private int errorTicks;

    private Capa capa = Capa.LISTA;
    private String nombre = "";
    private String dia = "1";
    private int campo;
    private Jugador enEdicion;
    private Jugador porBorrar;
    private int confirmacion = 1;   // 0 = borrar, 1 = cancelar

    private int ratonX = -1;
    private int ratonY = -1;

    public PantallaPartidas(PartidaService service, Consumer<Jugador> alJugar) {
        this.service = service;
        this.alJugar = alJugar;

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setBackground(new Color(0x060908));
        setPreferredSize(new Dimension(ANCHO * 4, ALTO * 4));
        setLayout(null);

        // Tabla real de Swing: es la fuente de datos de la lista y del detalle.
        // Se pinta dentro del lienzo pixel art con la selección sincronizada.
        modeloPartidas = new DefaultTableModel(
                new Object[]{"Partida", "Día", "Tareas", "Cordura"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tablaPartidas = new JTable(modeloPartidas);
        tablaPartidas.setAutoCreateRowSorter(false);
        tablaPartidas.setFocusable(false);
        tablaPartidas.setOpaque(false);
        tablaPartidas.setRowHeight(FILA_ALTO - 1);
        tablaPartidas.setShowGrid(false);
        tablaPartidas.setTableHeader(null);
        tablaPartidas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPartidas.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int fila = tablaPartidas.getSelectedRow();
            if (fila >= 0 && fila < partidas.size()) {
                seleccion = fila;
                asegurarVisible();
            }
        });

        timer = new Timer(80, e -> {
            tick++;
            if (errorTicks > 0) {
                errorTicks--;
            }
            repaint();
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tecla(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                escribir(e.getKeyChar());
            }
        });

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                clic(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = aLienzo(e);
                ratonX = p == null ? -1 : p.x;
                ratonY = p == null ? -1 : p.y;
                setCursor(sobreAlgo(ratonX, ratonY)
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ratonX = -1;
                ratonY = -1;
                setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    // ------------------------------------------------------------------ datos

    /** Vuelve a leer las partidas desde MySQL (a través del Service). */
    public void refrescar() {
        int idPrevio = seleccionada() == null ? -1 : seleccionada().getId();
        try {
            partidas = service.listar();
            if (errorTicks <= 0) {
                error = "";
            }
        } catch (PersistenciaException e) {
            partidas = new ArrayList<>();
            mostrarError(e.getMessage());
            Dialogos.error(this, e);
        }

        cargarTabla();
        if (idPrevio >= 0) {
            for (int i = 0; i < partidas.size(); i++) {
                if (partidas.get(i).getId() == idPrevio) {
                    seleccion = i;
                    break;
                }
            }
        }
        seleccion = partidas.isEmpty() ? 0 : Math.max(0, Math.min(seleccion, partidas.size() - 1));
        asegurarVisible();
        sincronizarTabla();
        repaint();
    }

    /** Copia las partidas persistidas al modelo de la JTable (se llama al abrir y tras cada CRUD). */
    private void cargarTabla() {
        modeloPartidas.setRowCount(0);
        for (Jugador j : partidas) {
            modeloPartidas.addRow(new Object[]{
                    j.getNombre(), j.getDiaActual(), j.getTareasCompletadas(), j.getCordura()
            });
        }
    }

    /** Refleja la selección de la lista dibujada en la JTable. */
    private void sincronizarTabla() {
        if (tablaPartidas.getRowCount() == 0) {
            tablaPartidas.clearSelection();
            return;
        }
        int fila = Math.max(0, Math.min(seleccion, tablaPartidas.getRowCount() - 1));
        tablaPartidas.setRowSelectionInterval(fila, fila);
    }

    private Jugador seleccionada() {
        return seleccion >= 0 && seleccion < partidas.size() ? partidas.get(seleccion) : null;
    }

    private void seleccionar(int indice) {
        if (partidas.isEmpty()) {
            seleccion = 0;
            desplazamiento = 0;
            sincronizarTabla();
            return;
        }
        seleccion = Math.max(0, Math.min(indice, partidas.size() - 1));
        asegurarVisible();
        sincronizarTabla();
    }

    private void asegurarVisible() {
        if (seleccion < desplazamiento) {
            desplazamiento = seleccion;
        }
        if (seleccion >= desplazamiento + FILAS) {
            desplazamiento = seleccion - FILAS + 1;
        }
        int maximo = Math.max(0, partidas.size() - FILAS);
        desplazamiento = Math.max(0, Math.min(desplazamiento, maximo));
    }

    private void mover(int delta) {
        if (partidas.isEmpty()) {
            return;
        }
        seleccionar((seleccion + delta + partidas.size()) % partidas.size());
    }

    // ------------------------------------------------------------------ pintado

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        dibujarLienzo();

        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(0x060908));
        g.fillRect(0, 0, getWidth(), getHeight());

        int escala = Math.max(1, Math.min(getWidth() / ANCHO, getHeight() / ALTO));
        int ancho = ANCHO * escala;
        int alto = ALTO * escala;
        int x = (getWidth() - ancho) / 2;
        int y = (getHeight() - alto) / 2;
        g.drawImage(lienzo, x, y, ancho, alto, null);
        g.setColor(new Color(0x24312B));
        g.drawRect(x - 1, y - 1, ancho + 1, alto + 1);
        g.dispose();
    }

    private void dibujarLienzo() {
        Graphics2D g = lienzo.createGraphics();
        Pixel.preparar(g);
        dibujarFondo(g);
        dibujarTitulo(g);
        dibujarLista(g);
        dibujarDetalle(g);
        dibujarBotones(g);
        dibujarPie(g);
        dibujarError(g);

        if (capa == Capa.FORMULARIO) {
            dibujarFormulario(g);
        } else if (capa == Capa.BORRAR) {
            dibujarConfirmacion(g);
        }
        g.dispose();
    }

    private void dibujarFondo(Graphics2D g) {
        // Pared del buffet: paneles gastados, zócalo y una noche de lluvia.
        for (int y = 0; y < 96; y += 8) {
            g.setColor(y < 40 ? new Color(0x17251F) : new Color(0x1C2C25));
            g.fillRect(0, y, ANCHO, 8);
        }
        for (int x = 0; x < ANCHO; x += 48) {
            g.setColor(new Color(0x101C17));
            g.fillRect(x, 0, 1, 92);
            g.setColor(new Color(0x23362D));
            g.fillRect(x + 1, 0, 1, 92);
        }
        g.setColor(new Color(0x0E1713));
        g.fillRect(0, 88, ANCHO, 8);
        g.setColor(new Color(0x4A3524));
        g.fillRect(0, 90, ANCHO, 2);
        g.setColor(new Color(0x0A100D));
        g.fillRect(0, 96, ANCHO, 2);

        // Piso en damero oscuro, con juntas que se abren en perspectiva.
        int y = 98;
        int fila = 0;
        while (y < ALTO) {
            int alto = Math.min(16, 6 + fila * 2);
            int ancho = Math.min(34, 20 + fila * 2);
            int off = (fila % 2 == 0) ? 0 : -(ancho / 2);
            for (int x = off; x < ANCHO; x += ancho) {
                g.setColor(((x / ancho) + fila) % 2 == 0
                        ? new Color(0x151B17) : new Color(0x111713));
                g.fillRect(x, y, ancho, alto);
                g.setColor(new Color(0x0A0F0C));
                g.fillRect(x, y + alto - 1, ancho, 1);
                g.fillRect(x + ancho - 1, y, 1, alto);
            }
            y += alto;
            fila++;
        }

        // Ventana con luna y lluvia animada.
        ventana(g, 22, 12, 66, 46);
        // Póster de la mascota perdida.
        poster(g, 294, 12, 40, 50);
        // Reloj de pared, detenido a las 3:33.
        reloj(g, 168, 12);
        // Dos lámparas cálidas sobre el título.
        lampara(g, 118, 4);
        lampara(g, 266, 4);
        // Silueta del mostrador, apenas visible al fondo.
        g.setColor(new Color(0x0B110E));
        g.fillRect(126, 60, 132, 28);
        g.setColor(new Color(0x1B2821));
        g.fillRect(126, 60, 132, 4);
    }

    private void dibujarTitulo(Graphics2D g) {
        int tam = Pixel.T_GRANDE;
        int w1 = Pixel.ancho(g, "BUFFET ", tam, true);
        int w2 = Pixel.ancho(g, "TERROR", tam, true);
        int x = (ANCHO - (w1 + w2)) / 2;
        Pixel.texto(g, "BUFFET ", x + 1, 33, tam, new Color(0x050807), true);
        Pixel.texto(g, "TERROR", x + w1 + 1, 33, tam, new Color(0x050807), true);
        Pixel.texto(g, "BUFFET ", x, 31, tam, Pixel.CREMA, true);
        Pixel.texto(g, "TERROR", x + w1, 31, tam, Pixel.ROJO_CLARO, true);
        Pixel.textoCentrado(g, "TURNO NOCHE  ·  ELEGÍ TU PARTIDA", ANCHO / 2, 45,
                Pixel.T_CHICO, Pixel.MOSTAZA);
    }

    private void dibujarLista(Graphics2D g) {
        Pixel.panel(g, LISTA.x, LISTA.y, LISTA.width, LISTA.height,
                new Color(0x0B1210), Pixel.BORDE);
        Pixel.texto(g, "PARTIDAS GUARDADAS", 22, 66, Pixel.T_CUERPO, Pixel.MOSTAZA, true);
        Pixel.textoDerecha(g, tablaPartidas.getRowCount()
                        + (tablaPartidas.getRowCount() == 1 ? " guardada" : " guardadas"),
                362, 66, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        g.setColor(Pixel.BORDE);
        g.fillRect(20, 70, 224, 1);

        if (tablaPartidas.getRowCount() == 0) {
            Pixel.textoCentrado(g, "Todavía no hay partidas.", 134, 128,
                    Pixel.T_CUERPO, Pixel.CREMA, true);
            Pixel.textoCentrado(g, "Creá la primera con N.", 134, 144,
                    Pixel.T_CHICO, Pixel.GRIS);
            return;
        }

        // Encabezados de la tabla. La JTable real es la que guarda estos valores.
        Pixel.texto(g, "PARTIDA", 27, 81, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.texto(g, "DÍA", 140, 81, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.texto(g, "CORD.", 166, 81, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.texto(g, "TAREAS", 208, 81, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        g.setColor(new Color(0x1B2621));
        g.fillRect(20, 83, 224, 1);

        int fin = Math.min(tablaPartidas.getRowCount(), desplazamiento + FILAS);
        for (int i = desplazamiento; i < fin; i++) {
            int fila = i - desplazamiento;
            int fy = FILA_Y + fila * FILA_ALTO;
            boolean elegida = i == seleccion;

            if (elegida) {
                Pixel.panelTenue(g, 20, fy, 224, 15, 165, Pixel.MOSTAZA);
                g.setColor(Pixel.MOSTAZA);
                g.fillRect(20, fy, 2, 15);
            } else if (i % 2 == 1) {
                g.setColor(new Color(255, 255, 255, 7));
                g.fillRect(20, fy, 224, 15);
            }

            String nombre = Pixel.recortar(g, String.valueOf(tablaPartidas.getValueAt(i, 0)),
                    104, Pixel.T_CUERPO, elegida);
            Pixel.texto(g, nombre, 27, fy + 11, Pixel.T_CUERPO,
                    elegida ? Pixel.CREMA : Pixel.GRIS, elegida);
            Pixel.texto(g, "D" + numero(tablaPartidas.getValueAt(i, 1)), 140, fy + 11,
                    Pixel.T_CHICO, elegida ? Pixel.MOSTAZA_CLARA : Pixel.GRIS_OSCURO, elegida);

            int cordura = Math.max(0, Math.min(100, numero(tablaPartidas.getValueAt(i, 3))));
            g.setColor(new Color(0x1B2621));
            g.fillRect(166, fy + 5, 36, 5);
            g.setColor(colorCordura(cordura));
            int relleno = Math.max(0, Math.round(34 * cordura / 100f));
            if (relleno > 0) {
                g.fillRect(167, fy + 6, relleno, 3);
            }
            Pixel.texto(g, "T" + numero(tablaPartidas.getValueAt(i, 2)), 208, fy + 11,
                    Pixel.T_CHICO, elegida ? Pixel.CREMA : Pixel.GRIS_OSCURO, elegida);
        }

        if (tablaPartidas.getRowCount() > FILAS) {
            int total = tablaPartidas.getRowCount();
            int altoBarra = Math.max(8, 90 * FILAS / total);
            int yBarra = FILA_Y + 90 * desplazamiento / total;
            g.setColor(new Color(0x1B2621));
            g.fillRect(241, FILA_Y, 2, 90);
            g.setColor(Pixel.GRIS_OSCURO);
            g.fillRect(241, yBarra, 2, altoBarra);
        }
    }

    private static int numero(Object valor) {
        return valor instanceof Number n ? n.intValue() : 0;
    }

    private void dibujarDetalle(Graphics2D g) {
        g.setColor(Pixel.BORDE);
        g.fillRect(246, 58, 1, 118);

        Pixel.texto(g, "FICHA", 252, 66, Pixel.T_CUERPO, Pixel.MOSTAZA, true);
        Jugador j = seleccionada();
        if (j == null) {
            Pixel.textoCentrado(g, "Elegí una", 306, 118, Pixel.T_CHICO, Pixel.GRIS);
            Pixel.textoCentrado(g, "partida", 306, 130, Pixel.T_CHICO, Pixel.GRIS);
            return;
        }

        dibujarRetrato(g, 252, 74);
        String nombre = Pixel.recortar(g, j.getNombre(), 76, Pixel.T_CUERPO, true);
        Pixel.texto(g, nombre, 284, 84, Pixel.T_CUERPO, Pixel.CREMA, true);
        Pixel.texto(g, "DÍA " + j.getDiaActual(), 284, 96, Pixel.T_CHICO, Pixel.MOSTAZA);
        g.setColor(Pixel.BORDE);
        g.fillRect(252, 110, 108, 1);

        int cordura = Math.max(0, Math.min(100, j.getCordura()));
        Pixel.texto(g, "CORDURA", 252, 121, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoDerecha(g, String.valueOf(cordura), 360, 121, Pixel.T_CUERPO,
                colorCordura(cordura), true);
        Pixel.barra(g, 252, 125, 108, 6, cordura, 100, colorCordura(cordura),
                new Color(0x1B2621), Pixel.BORDE);

        Pixel.texto(g, "TAREAS", 252, 145, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoDerecha(g, String.valueOf(j.getTareasCompletadas()), 360, 145,
                Pixel.T_CUERPO, Pixel.CREMA, true);

        Pixel.texto(g, "ESTADO", 252, 160, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        String estado = cordura < 30 ? "AL LÍMITE" : cordura < 55 ? "INQUIETO" : "EN PIE";
        Pixel.textoDerecha(g, estado, 360, 160, Pixel.T_CHICO,
                cordura < 30 ? Pixel.ROJO_CLARO : cordura < 55 ? Pixel.MOSTAZA : Pixel.VERDE, true);
    }

    private void dibujarBotones(Graphics2D g) {
        boolean hay = seleccionada() != null;
        boton(g, BTN_JUGAR, "JUGAR", hay, false);
        boton(g, BTN_NUEVA, "NUEVA", true, false);
        boton(g, BTN_EDITAR, "EDITAR", hay, false);
        boton(g, BTN_BORRAR, "BORRAR", hay, true);
    }

    private void dibujarPie(Graphics2D g) {
        Pixel.textoCentrado(g, "ENTER jugar  ·  N nueva  ·  E editar  ·  SUPR borrar",
                ANCHO / 2, 215, Pixel.T_CHICO, Pixel.GRIS);
        Pixel.textoCentrado(g, "flechas elegir  ·  R actualizar  ·  doble clic para entrar",
                ANCHO / 2, 227, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    private void dibujarError(Graphics2D g) {
        if (errorTicks <= 0 || error == null || error.isBlank()) {
            return;
        }
        List<String> lineas = Pixel.envolver(g, error, 320, Pixel.T_CHICO);
        int alto = Math.min(2, lineas.size()) * 10 + 8;
        Pixel.panelTenue(g, 24, 183 - alto, 336, alto, 235, Pixel.ROJO);
        for (int i = 0; i < Math.min(2, lineas.size()); i++) {
            Pixel.textoCentrado(g, lineas.get(i), ANCHO / 2, 191 - alto + i * 10,
                    Pixel.T_CHICO, Pixel.ROJO_CLARO, true);
        }
    }

    private void dibujarFormulario(Graphics2D g) {
        g.setColor(new Color(3, 5, 5, 200));
        g.fillRect(0, 0, ANCHO, ALTO);
        Pixel.panel(g, 56, 34, 272, 180, new Color(0x0D1411), Pixel.BORDE_CLARO);

        String titulo = enEdicion == null ? "NUEVA PARTIDA" : "EDITAR PARTIDA";
        Pixel.textoCentrado(g, titulo, ANCHO / 2, 57, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, enEdicion == null
                ? "Anotá tu nombre y el día en que arrancás."
                : "Cambiá el nombre o el día guardado.", ANCHO / 2, 70, Pixel.T_CHICO, Pixel.GRIS);

        Pixel.texto(g, "NOMBRE", 76, 80, Pixel.T_CHICO, Pixel.MOSTAZA, true);
        campo(g, CAMPO_NOMBRE, campo == 0);
        String visible = nombre.isEmpty() ? "Escribí un nombre" : nombre;
        Pixel.texto(g, Pixel.recortar(g, visible, 224, Pixel.T_CUERPO, !nombre.isEmpty()),
                80, 94, Pixel.T_CUERPO, nombre.isEmpty() ? Pixel.GRIS_OSCURO : Pixel.CREMA, !nombre.isEmpty());
        if (campo == 0 && (tick / 5) % 2 == 0) {
            int cursor = 80 + (nombre.isEmpty() ? 0 : Pixel.ancho(g, nombre, Pixel.T_CUERPO, true));
            g.setColor(Pixel.MOSTAZA_CLARA);
            g.fillRect(cursor, 86, 1, 10);
        }

        Pixel.texto(g, "DÍA INICIAL", 76, 114, Pixel.T_CHICO, Pixel.MOSTAZA, true);
        campo(g, CAMPO_DIA, campo == 1);
        Pixel.texto(g, dia, 80, 128, Pixel.T_CUERPO, Pixel.CREMA, true);
        if (campo == 1 && (tick / 5) % 2 == 0) {
            int cursor = 80 + Pixel.ancho(g, dia, Pixel.T_CUERPO, true);
            g.setColor(Pixel.MOSTAZA_CLARA);
            g.fillRect(cursor, 120, 1, 10);
        }
        Pixel.texto(g, "1 es el primer turno.", 148, 128, Pixel.T_CHICO, Pixel.GRIS);

        boton(g, BTN_GUARDAR, enEdicion == null ? "CREAR" : "GUARDAR", true, false);
        boton(g, BTN_CANCELAR, "CANCELAR", true, false);
        Pixel.textoCentrado(g, "TAB cambiar de campo  ·  ENTER guardar  ·  ESC volver",
                ANCHO / 2, 190, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.textoCentrado(g, "Los datos se guardan en la base buffet_terror.",
                ANCHO / 2, 203, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    private void dibujarConfirmacion(Graphics2D g) {
        g.setColor(new Color(3, 5, 5, 200));
        g.fillRect(0, 0, ANCHO, ALTO);
        Pixel.panel(g, 72, 72, 240, 112, new Color(0x120C0B), new Color(0x6E231F));

        Pixel.textoCentrado(g, "BORRAR PARTIDA", ANCHO / 2, 96, Pixel.T_SUBTITULO,
                Pixel.ROJO_CLARO, true);
        String quien = porBorrar == null ? "" : porBorrar.getNombre();
        Pixel.textoCentrado(g, "¿Seguro que querés borrar a " + quien + "?",
                ANCHO / 2, 116, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.textoCentrado(g, "Esta acción no se puede deshacer.",
                ANCHO / 2, 130, Pixel.T_CHICO, Pixel.GRIS);

        boton(g, BTN_BORRAR_SI, "BORRAR", true, true);
        boton(g, BTN_BORRAR_NO, "CANCELAR", confirmacion == 1, false);
        if (confirmacion == 0) {
            g.setColor(Pixel.ROJO_CLARO);
            g.drawRect(BTN_BORRAR_SI.x - 1, BTN_BORRAR_SI.y - 1,
                    BTN_BORRAR_SI.width + 1, BTN_BORRAR_SI.height + 1);
        } else {
            g.setColor(Pixel.MOSTAZA);
            g.drawRect(BTN_BORRAR_NO.x - 1, BTN_BORRAR_NO.y - 1,
                    BTN_BORRAR_NO.width + 1, BTN_BORRAR_NO.height + 1);
        }

        Pixel.textoCentrado(g, "ENTER confirmar  ·  ESC cancelar  ·  ← → elegir",
                ANCHO / 2, 177, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    // ------------------------------------------------------------------ teclado

    private void tecla(KeyEvent e) {
        int codigo = e.getKeyCode();
        if (capa == Capa.FORMULARIO) {
            formularioTecla(codigo);
        } else if (capa == Capa.BORRAR) {
            borrarTecla(codigo);
        } else {
            listaTecla(codigo);
        }
    }

    private void listaTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> mover(-1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> mover(1);
            case KeyEvent.VK_PAGE_UP -> seleccionar(seleccion - FILAS);
            case KeyEvent.VK_PAGE_DOWN -> seleccionar(seleccion + FILAS);
            case KeyEvent.VK_ENTER -> jugar();
            case KeyEvent.VK_N -> abrirFormulario(null);
            case KeyEvent.VK_E -> abrirFormulario(seleccionada());
            case KeyEvent.VK_DELETE -> pedirBorrado();
            case KeyEvent.VK_R, KeyEvent.VK_F5 -> refrescar();
            default -> { }
        }
    }

    private void formularioTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> cerrarFormulario();
            case KeyEvent.VK_TAB, KeyEvent.VK_UP, KeyEvent.VK_DOWN -> campo = 1 - campo;
            case KeyEvent.VK_ENTER -> guardarFormulario();
            case KeyEvent.VK_BACK_SPACE -> borrarCaracter();
            default -> { }
        }
    }

    private void borrarTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> {
                capa = Capa.LISTA;
                porBorrar = null;
            }
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_TAB ->
                    confirmacion = 1 - confirmacion;
            case KeyEvent.VK_ENTER -> {
                if (confirmacion == 0) {
                    borrarDefinitivo();
                } else {
                    capa = Capa.LISTA;
                    porBorrar = null;
                }
            }
            default -> { }
        }
    }

    private void escribir(char c) {
        if (capa != Capa.FORMULARIO || Character.isISOControl(c)) {
            return;
        }
        if (campo == 0) {
            if (nombre.length() < 50) {
                nombre += c;
            }
        } else if (Character.isDigit(c) && dia.length() < 3) {
            if (dia.equals("0")) {
                dia = "";
            }
            dia += c;
        }
        repaint();
    }

    private void borrarCaracter() {
        if (capa != Capa.FORMULARIO) {
            return;
        }
        if (campo == 0 && !nombre.isEmpty()) {
            nombre = nombre.substring(0, nombre.length() - 1);
        } else if (campo == 1 && !dia.isEmpty()) {
            dia = dia.substring(0, dia.length() - 1);
        }
        repaint();
    }

    // ------------------------------------------------------------------ ratón

    private void clic(MouseEvent e) {
        Point p = aLienzo(e);
        if (p == null) {
            return;
        }
        if (capa == Capa.FORMULARIO) {
            clicFormulario(p, e.getClickCount());
        } else if (capa == Capa.BORRAR) {
            if (BTN_BORRAR_SI.contains(p)) {
                confirmacion = 0;
                borrarDefinitivo();
            } else if (BTN_BORRAR_NO.contains(p)) {
                capa = Capa.LISTA;
                porBorrar = null;
            }
        } else {
            clicLista(p, e.getClickCount());
        }
    }

    private void clicLista(Point p, int clics) {
        for (int fila = 0; fila < FILAS; fila++) {
            int indice = desplazamiento + fila;
            if (indice >= partidas.size()) {
                break;
            }
            Rectangle r = new Rectangle(20, FILA_Y + fila * FILA_ALTO, 224, 15);
            if (r.contains(p)) {
                seleccionar(indice);
                if (clics >= 2) {
                    jugar();
                }
                return;
            }
        }
        if (BTN_JUGAR.contains(p)) {
            jugar();
        } else if (BTN_NUEVA.contains(p)) {
            abrirFormulario(null);
        } else if (BTN_EDITAR.contains(p)) {
            abrirFormulario(seleccionada());
        } else if (BTN_BORRAR.contains(p)) {
            pedirBorrado();
        }
    }

    private void clicFormulario(Point p, int clics) {
        if (CAMPO_NOMBRE.contains(p)) {
            campo = 0;
        } else if (CAMPO_DIA.contains(p)) {
            campo = 1;
        } else if (BTN_GUARDAR.contains(p)) {
            guardarFormulario();
        } else if (BTN_CANCELAR.contains(p)) {
            cerrarFormulario();
        } else if (clics >= 2) {
            // Doble clic fuera de los campos no hace nada raro.
            return;
        }
    }

    private boolean sobreAlgo(int x, int y) {
        if (x < 0 || y < 0) {
            return false;
        }
        if (capa == Capa.FORMULARIO) {
            return CAMPO_NOMBRE.contains(x, y) || CAMPO_DIA.contains(x, y)
                    || BTN_GUARDAR.contains(x, y) || BTN_CANCELAR.contains(x, y);
        }
        if (capa == Capa.BORRAR) {
            return BTN_BORRAR_SI.contains(x, y) || BTN_BORRAR_NO.contains(x, y);
        }
        if (BTN_JUGAR.contains(x, y) || BTN_NUEVA.contains(x, y)
                || BTN_EDITAR.contains(x, y) || BTN_BORRAR.contains(x, y)) {
            return true;
        }
        int fila = (y - FILA_Y) / FILA_ALTO;
        return y >= FILA_Y && fila >= 0 && fila < FILAS
                && (desplazamiento + fila) < partidas.size();
    }

    /** Convierte el clic de la ventana a coordenadas del lienzo interno de 384x240. */
    private Point aLienzo(MouseEvent e) {
        int escala = Math.max(1, Math.min(getWidth() / ANCHO, getHeight() / ALTO));
        int ancho = ANCHO * escala;
        int alto = ALTO * escala;
        int ox = (getWidth() - ancho) / 2;
        int oy = (getHeight() - alto) / 2;
        int x = (e.getX() - ox) / escala;
        int y = (e.getY() - oy) / escala;
        if (x < 0 || y < 0 || x >= ANCHO || y >= ALTO) {
            return null;
        }
        return new Point(x, y);
    }

    // ------------------------------------------------------------------ acciones

    private void jugar() {
        Jugador j = seleccionada();
        if (j != null) {
            alJugar.accept(j);
        }
    }

    private void abrirFormulario(Jugador jugador) {
        enEdicion = jugador;
        nombre = jugador == null ? "" : jugador.getNombre();
        dia = jugador == null ? "1" : String.valueOf(jugador.getDiaActual());
        campo = 0;
        capa = Capa.FORMULARIO;
        error = "";
        errorTicks = 0;
        repaint();
    }

    private void cerrarFormulario() {
        capa = Capa.LISTA;
        enEdicion = null;
        nombre = "";
        dia = "1";
        campo = 0;
        repaint();
    }

    private void guardarFormulario() {
        int numeroDia;
        try {
            numeroDia = Integer.parseInt(dia.trim());
        } catch (NumberFormatException e) {
            Dialogos.formato(this, "El día tiene que ser un número entero, por ejemplo 1.");
            mostrarError("El día tiene que ser un número entero, por ejemplo 1.");
            return;
        }

        try {
            Jugador creado = null;
            if (enEdicion == null) {
                creado = service.registrar(nombre, numeroDia);
            } else {
                service.modificar(enEdicion.getId(), nombre, numeroDia);
            }
            capa = Capa.LISTA;
            enEdicion = null;
            nombre = "";
            dia = "1";
            campo = 0;
            refrescar();
            if (creado != null) {
                for (int i = 0; i < partidas.size(); i++) {
                    if (partidas.get(i).getNombre().equalsIgnoreCase(creado.getNombre())) {
                        seleccionar(i);
                        break;
                    }
                }
            }
        } catch (DiaInvalidoException | InteraccionInvalidaException | PersistenciaException e) {
            mostrarError(e.getMessage());
            Dialogos.error(this, e);
        }
    }

    private void pedirBorrado() {
        Jugador j = seleccionada();
        if (j == null) {
            return;
        }
        porBorrar = j;
        confirmacion = 1;
        capa = Capa.BORRAR;
        repaint();
    }

    private void borrarDefinitivo() {
        if (porBorrar == null) {
            capa = Capa.LISTA;
            return;
        }
        int id = porBorrar.getId();
        try {
            service.eliminar(id);
            capa = Capa.LISTA;
            porBorrar = null;
            refrescar();
        } catch (PersistenciaException e) {
            capa = Capa.LISTA;
            porBorrar = null;
            mostrarError(e.getMessage());
            Dialogos.error(this, e);
        }
    }

    private void mostrarError(String texto) {
        error = texto == null || texto.isBlank() ? "No se pudo completar la operación." : texto;
        errorTicks = 70;
        repaint();
    }

    // ------------------------------------------------------------------ ayudas de dibujo

    private static Color colorCordura(int cordura) {
        return cordura < 30 ? Pixel.ROJO : cordura < 55 ? Pixel.MOSTAZA : Pixel.VERDE;
    }

    private void campo(Graphics2D g, Rectangle r, boolean activo) {
        g.setColor(new Color(0x070C0A));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(activo ? Pixel.MOSTAZA : Pixel.BORDE);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setColor(new Color(255, 255, 255, 12));
        g.fillRect(r.x + 1, r.y + 1, r.width - 2, 1);
    }

    private void boton(Graphics2D g, Rectangle r, String texto, boolean activo, boolean peligro) {
        Color base = peligro ? new Color(0x3A1512) : new Color(0x18241F);
        Color borde = activo
                ? (peligro ? new Color(0xB84A3C) : Pixel.MOSTAZA)
                : Pixel.BORDE;
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(r.x + 2, r.y + 2, r.width, r.height);
        g.setColor(base);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(r.x + 1, r.y + 1, r.width - 2, 1);
        g.setColor(borde);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        if (activo) {
            g.setColor(peligro ? Pixel.ROJO_CLARO : Pixel.MOSTAZA);
            g.fillRect(r.x, r.y, 2, r.height);
            g.fillRect(r.x, r.y, r.width, 1);
        }
        Color letra = !activo ? Pixel.GRIS_OSCURO
                : peligro ? Pixel.ROJO_CLARO : Pixel.CREMA;
        Pixel.textoCentrado(g, texto, r.x + r.width / 2, r.y + (r.height + Pixel.T_CUERPO) / 2 - 1,
                Pixel.T_CUERPO, letra, activo);
    }

    private void dibujarRetrato(Graphics2D g, int x, int y) {
        Pixel.panelTenue(g, x, y, 28, 32, 150, Pixel.BORDE);
        // delantal y camisa
        r(g, x + 7, y + 18, 14, 12, new Color(0x2E3B36));
        r(g, x + 9, y + 19, 10, 10, new Color(0xB8B2A0));
        r(g, x + 9, y + 19, 2, 10, new Color(0x8E8878));
        // cuello y pañuelo
        r(g, x + 11, y + 16, 6, 2, new Color(0x8E2F2A));
        // cabeza
        r(g, x + 9, y + 5, 10, 11, new Color(0x0A0E0C));
        r(g, x + 10, y + 6, 8, 9, new Color(0xC99A6B));
        r(g, x + 10, y + 6, 3, 4, new Color(0xE0B487));
        // pelo
        r(g, x + 10, y + 4, 8, 3, new Color(0x1B1512));
        r(g, x + 9, y + 6, 1, 5, new Color(0x1B1512));
        r(g, x + 18, y + 5, 1, 4, new Color(0x1B1512));
        // ojos y boca
        r(g, x + 11, y + 9, 1, 1, new Color(0x120F0C));
        r(g, x + 15, y + 9, 1, 1, new Color(0x120F0C));
        r(g, x + 13, y + 12, 2, 1, new Color(0x8A6246));
    }

    private void ventana(Graphics2D g, int x, int y, int w, int h) {
        r(g, x - 2, y - 2, w + 4, h + 4, new Color(0x120D08));
        r(g, x, y, w, h, new Color(0x0B1720));
        for (int i = 0; i < 7; i++) {
            g.setColor(i < 3 ? new Color(0x102432) : new Color(0x0C1A24));
            g.fillRect(x, y + i * (h / 7), w, h / 7 + 1);
        }
        Pixel.elipse(g, x + 42, y + 6, 12, 12, new Color(0xC8D2C0));
        Pixel.elipse(g, x + 44, y + 7, 9, 10, new Color(0xE4EBDD));
        r(g, x + 46, y + 9, 2, 2, new Color(0x8A9A8A));
        // lluvia
        for (int i = 0; i < 18; i++) {
            int rx = x + 3 + (i * 17 + tick / 2) % (w - 6);
            int ry = y + 3 + (i * 11 + tick) % (h - 5);
            r(g, rx, ry, 1, 3, new Color(0x6C8C9A, true));
        }
        // marco y travesaños
        r(g, x + w / 2, y, 2, h, new Color(0x4A3524));
        r(g, x, y + h / 2, w, 2, new Color(0x4A3524));
        r(g, x - 1, y - 1, w + 2, 1, new Color(0x6B4E32));
        r(g, x - 1, y + h, w + 2, 1, new Color(0x2E1F14));
    }

    private void poster(Graphics2D g, int x, int y, int w, int h) {
        r(g, x + 1, y + 1, w, h, new Color(0, 0, 0, 100));
        r(g, x, y, w, h, new Color(0xB8A98D));
        r(g, x + 1, y + 1, w - 2, h - 2, new Color(0xD6C9AA));
        r(g, x + 3, y + 4, w - 6, 1, new Color(0x8C7A5C));
        r(g, x + 3, y + 6, w - 6, 20, new Color(0x8E9E92));
        // silueta de una nena
        r(g, x + 16, y + 9, 7, 6, new Color(0x4A5A50));
        r(g, x + 15, y + 15, 9, 7, new Color(0x4A5A50));
        r(g, x + 17, y + 10, 1, 1, new Color(0xE8E4D4));
        r(g, x + 21, y + 10, 1, 1, new Color(0xE8E4D4));
        Pixel.texto(g, "PERDIDA", x + 5, y + 34, 6, new Color(0x5A3428), true);
        r(g, x + 5, y + 38, w - 10, 1, new Color(0x8C7A5C));
        r(g, x + 5, y + 42, w - 14, 1, new Color(0x8C7A5C));
        r(g, x + 5, y + 46, w - 11, 1, new Color(0x8C7A5C));
    }

    private void reloj(Graphics2D g, int x, int y) {
        r(g, x + 2, y + 2, 26, 26, new Color(0, 0, 0, 90));
        r(g, x, y, 26, 26, new Color(0x1A211E));
        Pixel.elipse(g, x + 2, y + 2, 22, 22, new Color(0xD6C9AA));
        r(g, x + 12, y + 13, 1, 7, new Color(0x3A2A20));
        r(g, x + 12, y + 13, 6, 1, new Color(0x3A2A20));
        r(g, x + 12, y + 20, 1, 1, new Color(0x8E2F2A));
    }

    private void lampara(Graphics2D g, int cx, int cy) {
        r(g, cx, 0, 1, cy + 8, new Color(0x1A211E));
        r(g, cx - 7, cy + 8, 15, 2, new Color(0x3A4741));
        r(g, cx - 5, cy + 10, 11, 2, new Color(0x5A6A60));
        r(g, cx - 3, cy + 12, 7, 2, new Color(0xFFE6A0));
        for (int i = 3; i >= 1; i--) {
            Pixel.elipse(g, cx - 8 - i * 4, cy + 12 - i * 2, 17 + i * 8, 7 + i * 4,
                    new Color(255, 214, 140, 8 + i * 2));
        }
    }

    private static void r(Graphics2D g, int x, int y, int w, int h, Color color) {
        g.setColor(color);
        g.fillRect(x, y, w, h);
    }
}
