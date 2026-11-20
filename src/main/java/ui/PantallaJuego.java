package ui;

import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.PedidoInvalidoException;
import excepciones.PersistenciaException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Herramienta;
import modelo.Jugador;
import modelo.Lugar;
import modelo.Objeto;
import modelo.Sala;
import modelo.Tarea;
import service.JuegoService;
import service.PartidaService;
import ui.EscenaBuffet.Zona;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Pantalla completa del juego. Todo lo que el jugador ve —el buffet, el estado, los avisos,
 * los diálogos y la intro— se dibuja dentro del mismo lienzo de 384x240 y después se escala
 * con un factor entero para que ningún píxel quede deformado.
 */
public class PantallaJuego extends JPanel implements ActionListener {

    private static final int ANCHO = EscenaBuffet.ANCHO;
    private static final int ALTO = EscenaBuffet.ALTO;

    private enum Estado { INTRO, JUEGO, CUADERNO, CHARLA, PAUSA, CERRAR_DIA, CONFIRMAR_SALIDA, DERROTA }

    /** Texto de la intro: la primera entrada es el título; el resto son las noches. */
    private static final String[][] GUION = {
            {"BUFFET TERROR", "turno noche"},
            {"NOCHE 1", "El cartel de la calle se apaga y el turno empieza igual."},
            {"NOCHE 2", "Los clientes llegan con hambre. Algunos no tienen ojos."},
            {"NOCHE 3", "Tomá el pedido. Retirá la bandeja. No dejes que te mire."},
            {"TODO LISTO", "El mostrador está en el comedor. El almacén, pasando la cocina."}
    };

    private static final String[] OPCIONES_PAUSA = {"Continuar", "Guardar progreso", "Salir al menú"};
    private static final String[] OPCIONES_SALIR = {"Guardar y salir", "Salir sin guardar", "Cancelar"};
    private static final String[] OPCIONES_FIN = {"Seguir en el buffet", "Pasar al día siguiente", "Guardar y salir"};

    private final JuegoService juego;
    private final PartidaService partidas;
    private final Runnable alSalir;
    private final EscenaBuffet escena;
    private final BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
    private final Timer timer = new Timer(16, this);
    private final Set<Integer> teclas = new HashSet<>();

    private Estado estado = Estado.INTRO;
    private int seleccion;
    private int introPaso;
    private long introMillis;
    private boolean introCompleto;
    private String charla = "";
    private int golpeCorduraTicks;
    private int golpesVistos;
    private String causaDerrota;
    private boolean confirmarAvance;
    private boolean confirmarSalida;
    private long ultimaInteraccion;
    private long ultimoLatidoPresion;
    private boolean avisoCierre;
    private long avisoCierreDesde;

    public PantallaJuego(JuegoService juego, PartidaService partidas, Runnable alSalir) {
        this.juego = juego;
        this.partidas = partidas;
        this.alSalir = alSalir;
        this.escena = new EscenaBuffet(juego);
        this.introMillis = System.currentTimeMillis();

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setBackground(new Color(0x060908));
        setPreferredSize(new Dimension(ANCHO * 3, ALTO * 3));
        setLayout(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                teclaPresionada(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                teclas.remove(e.getKeyCode());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                teclas.clear();
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

    // ------------------------------------------------------------------ ciclo

    @Override
    public void actionPerformed(ActionEvent e) {
        long ahora = System.currentTimeMillis();

        if (estado == Estado.JUEGO) {
            escena.actualizar(teclas, false);
            if (escena.getGolpes() > golpesVistos) {
                golpesVistos = escena.getGolpes();
                golpeCorduraTicks = 32;
            }
            if (!juego.isPerdida() && ahora - ultimoLatidoPresion >= JuegoService.INTERVALO_PRESION_MS) {
                ultimoLatidoPresion = ahora;
                int dano = juego.latidoPresion();
                if (dano > 0) {
                    golpeCorduraTicks = 32;
                }
            }
            if (juego.isPerdida()) {
                derrota(juego.getCausaPerdida());
            } else if (juego.turnoTerminado() && !avisoCierre) {
                avisoCierre = true;
                avisoCierreDesde = ahora;
            } else if (avisoCierre && ahora - avisoCierreDesde > 2400) {
                abrirCierreDeJornada();
            }
        } else {
            ultimoLatidoPresion = ahora;
            escena.actualizar(Set.of(), true);
        }

        if (estado == Estado.INTRO && !introCompleto && introPaso > 0 && introPaso < GUION.length - 1) {
            String cuerpo = GUION[introPaso][1];
            if (ahora - introMillis > cuerpo.length() * 24L + 400) {
                introCompleto = true;
            }
        }

        if (golpeCorduraTicks > 0) {
            golpeCorduraTicks--;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        dibujarEscena();
        Graphics2D g2 = (Graphics2D) g.create();
        Pixel.preparar(g2);
        Pixel.pintarPantalla(g2, lienzo, getWidth(), getHeight());

        // La interfaz va sobre la ventana, con la misma escala que el escenario. Así el
        // texto se rasteriza a la resolución real y deja de verse como pixel art.
        Pixel.Encuadre encuadre = Pixel.calcularLienzo(getWidth(), getHeight(), ANCHO, ALTO);
        Graphics2D ui = (Graphics2D) g2.create();
        Pixel.prepararInterfaz(ui);
        ui.clipRect(encuadre.x(), encuadre.y(), encuadre.ancho(), encuadre.alto());
        ui.translate(encuadre.x(), encuadre.y());
        ui.scale(encuadre.escala(), encuadre.escala());
        dibujarInterfaz(ui);
        ui.dispose();
        g2.dispose();
    }

    private void dibujarEscena() {
        Graphics2D g = lienzo.createGraphics();
        Pixel.preparar(g);
        g.setColor(Pixel.NEGRO);
        g.fillRect(0, 0, ANCHO, ALTO);
        escena.pintar(g);
        g.dispose();
    }

    private void dibujarInterfaz(Graphics2D g) {
        switch (estado) {
            case INTRO -> dibujarIntro(g);
            case JUEGO -> dibujarHud(g);
            case CUADERNO -> {
                dibujarHud(g);
                dibujarCuaderno(g);
            }
            case CHARLA -> {
                dibujarHud(g);
                dibujarCharla(g);
            }
            case PAUSA -> {
                dibujarHud(g);
                dibujarPausa(g);
            }
            case CONFIRMAR_SALIDA -> {
                dibujarHud(g);
                dibujarConfirmarSalida(g);
            }
            case CERRAR_DIA -> {
                dibujarHud(g);
                dibujarFinDeDia(g);
            }
            case DERROTA -> {
                dibujarHud(g);
                dibujarDerrota(g);
            }
            default -> dibujarHud(g);
        }
    }

    // ------------------------------------------------------------------ intro

    private void dibujarIntro(Graphics2D g) {
        g.setColor(new Color(4, 7, 8, 184));
        g.fillRect(0, 0, ANCHO, ALTO);

        // lluvia sobre el vidrio
        for (int i = 0; i < 54; i++) {
            int x = (i * 71 + tickLluvia(i)) % ANCHO;
            int y = (i * 47 + tickLluvia(i) * 2) % ALTO;
            g.setColor(new Color(120, 160, 166, 28 + (i % 3) * 10));
            g.fillRect(x, y, 1, 3 + (i % 2));
        }

        long t = System.currentTimeMillis() - introMillis;
        if (introPaso == 0) {
            dibujarTitulo(g);
        } else if (introPaso < GUION.length - 1) {
            dibujarNoche(g, t);
        } else {
            dibujarEntrada(g);
        }

        // marco del cine y pie
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, ANCHO, 12);
        g.fillRect(0, ALTO - 12, ANCHO, 12);
        Pixel.textoCentrado(g, "ENTER continuar  ·  ESC salir", ANCHO / 2, ALTO - 4, Pixel.T_CHICO,
                Pixel.GRIS_OSCURO);
    }

    private static int tickLluvia(int i) {
        return (int) ((System.currentTimeMillis() / 28 + i * 7) % 400);
    }

    private void dibujarTitulo(Graphics2D g) {
        int parpadeo = (int) (System.currentTimeMillis() / 90) % 23;
        Color crema = parpadeo == 0 ? new Color(0x8E8878) : Pixel.CREMA;
        Color rojo = parpadeo == 0 ? new Color(0x6E231F) : Pixel.ROJO_CLARO;

        // bandeja con un ojo servido
        int x = ANCHO / 2;
        dibujarBandeja(g, x - 22, 48);
        Pixel.elipse(g, x - 10, 54, 20, 13, new Color(0xF1EAD8));
        Pixel.elipse(g, x - 5, 57, 10, 8, new Color(0x7FB58A));
        Pixel.elipse(g, x - 2, 59, 4, 4, new Color(0x120F0C));
        g.setColor(new Color(0xC4513F));
        g.fillRect(x - 12, 58, 2, 1);
        g.fillRect(x + 10, 58, 2, 1);

        Pixel.textoCentrado(g, "BUFFET", ANCHO / 2 + 2, 104, 36, new Color(0x000000), true);
        Pixel.textoCentrado(g, "BUFFET", ANCHO / 2, 102, 36, crema, true);
        Pixel.textoCentrado(g, "TERROR", ANCHO / 2 + 2, 140, 36, new Color(0x000000), true);
        Pixel.textoCentrado(g, "TERROR", ANCHO / 2, 138, 36, rojo, true);

        Pixel.textoCentrado(g, "T U R N O   N O C H E", ANCHO / 2, 160, Pixel.T_CUERPO, Pixel.MOSTAZA);

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            Pixel.textoCentrado(g, "PRESIONÁ ENTER", ANCHO / 2, 198, Pixel.T_CUERPO, Pixel.MOSTAZA_CLARA, true);
        }
        Pixel.textoCentrado(g, "un turno. una bandeja. nadie se va sin comer.",
                ANCHO / 2, 218, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    private void dibujarNoche(Graphics2D g, long t) {
        String[] noche = GUION[introPaso];
        Pixel.textoCentrado(g, noche[0], ANCHO / 2, 88, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        g.setColor(new Color(0x51695C));
        g.fillRect(ANCHO / 2 - 58, 96, 116, 1);

        List<String> lineas = Pixel.envolver(g, noche[1], 300, Pixel.T_CUERPO);
        int visibles = introCompleto ? Integer.MAX_VALUE : (int) ((t - 250) / 24);
        visibles = Math.max(0, visibles);
        int y = 118;
        for (String linea : lineas) {
            if (visibles <= 0) {
                break;
            }
            String trozo = linea.substring(0, Math.min(linea.length(), visibles));
            visibles -= linea.length();
            Pixel.textoCentrado(g, trozo, ANCHO / 2, y, Pixel.T_CUERPO, Pixel.CREMA);
            y += Pixel.altoLinea(Pixel.T_CUERPO) + 2;
        }

        if (introCompleto && (System.currentTimeMillis() / 420) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER", ANCHO / 2, 186, Pixel.T_CUERPO, Pixel.MOSTAZA_CLARA, true);
        }
    }

    private void dibujarEntrada(Graphics2D g) {
        Pixel.textoCentrado(g, "TODO LISTO", ANCHO / 2, 74, Pixel.T_TITULO, Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "El buffet abre en un rato.", ANCHO / 2, 88, Pixel.T_CHICO, Pixel.GRIS);

        Pixel.panelTenue(g, 44, 100, 296, 98, 196, Pixel.BORDE);
        Pixel.texto(g, "WASD / flechas", 58, 118, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Pixel.texto(g, "moverte por el buffet", 160, 118, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "SHIFT", 58, 132, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Pixel.texto(g, "correr", 160, 132, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "E / ENTER", 58, 146, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Pixel.texto(g, "interactuar", 160, 146, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "I", 58, 160, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Pixel.texto(g, "usar la mochila", 160, 160, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "Q", 58, 174, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Pixel.texto(g, "soltar lo que tenés", 160, 174, Pixel.T_CUERPO, Pixel.CREMA);
        Pixel.texto(g, "TAB", 58, 188, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Pixel.texto(g, "cuaderno de turno", 160, 188, Pixel.T_CUERPO, Pixel.CREMA);

        Pixel.textoCentrado(g, "Cruzá las puertas de los bordes para cambiar de sala.",
                ANCHO / 2, 210, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        if ((System.currentTimeMillis() / 420) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER para entrar", ANCHO / 2, 228, Pixel.T_CUERPO,
                    Pixel.MOSTAZA_CLARA, true);
        }
    }

    private void avanzarIntro() {
        long ahora = System.currentTimeMillis();
        if (introPaso == 0) {
            introPaso = 1;
            introMillis = ahora;
            introCompleto = false;
            return;
        }
        if (introPaso == GUION.length - 1) {
            empezarTurno();
            return;
        }
        if (!introCompleto) {
            introCompleto = true;
            return;
        }
        introPaso++;
        introMillis = ahora;
        introCompleto = introPaso == GUION.length - 1;
    }

    private void empezarTurno() {
        estado = Estado.JUEGO;
        seleccion = 0;
        confirmarAvance = false;
        avisoCierre = false;
        ultimoLatidoPresion = System.currentTimeMillis();
        escena.setSala(juego.getSala());
        escena.reiniciarPosicion();
        golpeCorduraTicks = 0;
        golpesVistos = escena.getGolpes();
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------ HUD

    private void dibujarHud(Graphics2D g) {
        Jugador j = juego.getJugador();
        int cordura = Math.max(0, Math.min(100, j.getCordura()));
        Color colorCordura = cordura < JuegoService.CORDURA_MINIMA_DIA ? Pixel.ROJO
                : cordura < 55 ? Pixel.MOSTAZA : Pixel.VERDE;

        int nivel = juego.nivelPresion();
        Color colorPresion = switch (nivel) {
            case 1 -> Pixel.AZUL;
            case 2 -> Pixel.MOSTAZA;
            case 3 -> Pixel.ROJO_CLARO;
            case 4 -> Pixel.ROJO;
            default -> Pixel.VERDE;
        };

        // Dos franjas separadas: a la izquierda el turno y el objetivo; a la derecha,
        // bien aparte, la cordura con su barra y la presión. Todo un poco más bajo.
        Pixel.panelTenue(g, 4, 3, 264, 24, 228, Pixel.BORDE);
        Pixel.panelTenue(g, 272, 3, 108, 24, 228, nivel >= 4 ? Pixel.ROJO : Pixel.BORDE);
        String dia = "DÍA " + j.getDiaActual();
        Pixel.texto(g, dia, 9, 11, Pixel.T_CHICO, Pixel.MOSTAZA);
        String sala = " · " + juego.getSala().getNombre().toUpperCase();
        int xDia = 9 + Pixel.ancho(g, dia, Pixel.T_CHICO);
        int espacioSala = Math.max(0, 104 - xDia);
        Pixel.texto(g, Pixel.recortar(g, sala, espacioSala, Pixel.T_CHICO, false),
                xDia, 11, Pixel.T_CHICO, Pixel.GRIS);

        String pedidos = juego.clientesAtendidos() + "/" + juego.totalClientes();
        String tareas = juego.tareasHechas() + "/" + juego.totalTareas();
        int px = 108;
        Pixel.texto(g, "PEDIDOS", px, 11, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        px += Pixel.ancho(g, "PEDIDOS ", Pixel.T_CHICO);
        Pixel.texto(g, pedidos, px, 11, Pixel.T_CHICO, Pixel.CREMA);
        px += Pixel.ancho(g, pedidos, Pixel.T_CHICO);
        px += 8;
        Pixel.texto(g, "TAREAS", px, 11, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        px += Pixel.ancho(g, "TAREAS ", Pixel.T_CHICO);
        Pixel.texto(g, tareas, px, 11, Pixel.T_CHICO, Pixel.CREMA);

        String valorCordura = String.valueOf(cordura);
        int anchoBarraCordura = 20;
        int anchoCordura = Pixel.ancho(g, "CORDURA ", Pixel.T_CHICO)
                + Pixel.ancho(g, valorCordura, Pixel.T_CHICO) + 4 + anchoBarraCordura;
        int xCordura = Math.max(276, 375 - anchoCordura);
        Pixel.texto(g, "CORDURA", xCordura, 11, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        int xValor = xCordura + Pixel.ancho(g, "CORDURA ", Pixel.T_CHICO);
        Color colorNumero = golpeCorduraTicks > 0 ? Pixel.ROJO_CLARO : colorCordura;
        Pixel.texto(g, valorCordura, xValor, 11, Pixel.T_CHICO, colorNumero);
        int xBarra = xValor + Pixel.ancho(g, valorCordura, Pixel.T_CHICO) + 4;
        Pixel.barra(g, xBarra, 6, anchoBarraCordura, 5, cordura, 100, colorCordura,
                new Color(0x1B2621), Pixel.BORDE);

        // segunda línea de cada panel, sin que la presión invada el objetivo
        g.setColor(new Color(255, 255, 255, 12));
        g.drawLine(8, 15, 264, 15);
        g.drawLine(276, 15, 376, 15);
        Pixel.texto(g, "OBJETIVO", 9, 21, Pixel.T_CHICO, Pixel.MOSTAZA);
        int maxPista = 264 - 64 - 4;
        int tamPista = Pixel.tamAjustado(g, juego.getPista(), maxPista, Pixel.T_CHICO, Pixel.T_CHICO - 2);
        Pixel.texto(g, Pixel.recortar(g, juego.getPista(), maxPista, tamPista, false),
                64, 21, tamPista, Pixel.CREMA);
        String presion = "PRESIÓN " + juego.getPresionEtiqueta();
        int tamPresion = Pixel.tamAjustado(g, presion, 100, Pixel.T_CHICO, Pixel.T_CHICO - 2);
        int anchoPresion = Pixel.ancho(g, presion, tamPresion, false);
        Pixel.texto(g, presion, 375 - anchoPresion, 21, tamPresion, colorPresion);

        // La mano se consulta en el cuaderno (TAB): no queda ningún cartel fijo abajo.

        // aviso de interacción: chip chico con tecla E. Solo aparece si la acción
        // realmente está disponible en ese momento.
        Zona zona = escena.zonaCercana();
        if (zona != null && !juego.turnoTerminado()) {
            String texto = etiquetaDeZona(zona);
            boolean listo = true;
            if (zona.id().startsWith("tarea:") || zona.id().startsWith("estacion:")) {
                Lugar lugar = Lugar.valueOf(zona.id().substring(zona.id().indexOf(':') + 1));
                Tarea tarea = juego.tareaPendienteEn(lugar);
                if (tarea != null && tarea.requiereHerramienta() && !juego.tieneHerramientaPara(lugar)) {
                    listo = false;
                    texto += " · falta " + tarea.getHerramienta().getNombre();
                }
            }
            // El chip se estira hasta donde haga falta; si el texto no entra, baja un
            // punto de tamaño en vez de cortarse, así siempre se lee la acción completa.
            int tam = Pixel.tamAjustado(g, texto, 300 - 17, Pixel.T_CHICO, Pixel.T_CHICO - 2);
            int ancho = Pixel.ancho(g, texto, tam, false) + 17;
            int x = Math.max(4, Math.min(ANCHO - ancho - 4, (int) Math.round(escena.getX()) - ancho / 2));
            int y = Math.max(36, Math.min(186, (int) Math.round(escena.getY()) - 28));
            Pixel.panelTenue(g, x, y, ancho, 12, 214, listo ? Pixel.MOSTAZA : Pixel.ROJO);
            g.setColor(new Color(0x1B2621));
            g.fillRect(x + 2, y + 2, 8, 8);
            g.setColor(listo ? Pixel.MOSTAZA : Pixel.ROJO);
            g.drawRect(x + 2, y + 2, 7, 7);
            Pixel.textoCentrado(g, "E", x + 6, y + 9, Pixel.T_CHICO, listo ? Pixel.CREMA : Pixel.ROJO_CLARO);
            Pixel.texto(g, texto, x + 13, y + 9, tam, listo ? Pixel.CREMA : Pixel.ROJO_CLARO);
        }

        if (estado == Estado.JUEGO && juego.turnoTerminado()) {
            String texto = "ENTER para cerrar la caja";
            int ancho = Pixel.ancho(g, texto, Pixel.T_CHICO) + 14;
            int x = (ANCHO - ancho) / 2;
            Pixel.panelTenue(g, x, 224, ancho, 13, 214, Pixel.MOSTAZA);
            Pixel.textoCentrado(g, texto, ANCHO / 2, 233, Pixel.T_CHICO, Pixel.MOSTAZA_CLARA);
        }

        dibujarStamina(g);
    }

    /** Barra de aguante abajo a la izquierda: se vacía mientras corrés y vuelve sola. */
    private void dibujarStamina(Graphics2D g) {
        int valor = (int) Math.round(Math.max(0, Math.min(escena.getStaminaMaxima(), escena.getStamina())));
        int maximo = (int) Math.round(escena.getStaminaMaxima());
        Color color = valor <= 20 ? Pixel.ROJO_CLARO : valor <= 45 ? Pixel.MOSTAZA : Pixel.VERDE;
        Pixel.panelTenue(g, 6, 220, 100, 14, 214, valor <= 20 ? Pixel.ROJO : Pixel.BORDE);
        Pixel.texto(g, "STAMINA", 10, 230, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        int xBarra = 10 + Pixel.ancho(g, "STAMINA ", Pixel.T_CHICO);
        Pixel.barra(g, xBarra, 224, 102 - xBarra, 6, valor, maximo, color,
                new Color(0x1B2621), Pixel.BORDE);
    }

    /** Texto del chip según lo que se pueda hacer ahí en este instante. */
    private String etiquetaDeZona(Zona zona) {
        String id = zona.id();
        if (id.startsWith("estacion:")) {
            Lugar estacion = Lugar.valueOf(id.substring("estacion:".length()));
            if (juego.hayTareaPendiente(estacion)) {
                return "Reparar " + estacion.getEtiqueta();
            }
            Cliente c = juego.clienteActual();
            return c == null ? "Retirar pedido" : "Tomar " + c.getPedido() + " de " + estacion.getEtiqueta();
        }
        if (id.equals("cliente")) {
            Cliente c = juego.clienteActual();
            return c != null && !c.isPedidoTomado() ? "Tomar el pedido" : "Entregar el pedido";
        }
        return zona.etiqueta();
    }

    // ------------------------------------------------------------------ cuaderno

    private List<Objeto> inventario() {
        return juego.abrirInventario();
    }

    private void dibujarCuaderno(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 18, 12, 348, 216, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.panelTenue(g, 21, 15, 342, 30, 90, null);
        Pixel.textoCentradoDisplay(g, "CUADERNO DE TURNO", ANCHO / 2, 32, Pixel.T_SUBTITULO,
                Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "DÍA " + juego.getJugador().getDiaActual() + "  ·  "
                + juego.getSala().getNombre().toUpperCase(), ANCHO / 2, 43, Pixel.T_CHICO, Pixel.GRIS);
        g.setColor(Pixel.BORDE);
        g.fillRect(24, 49, 336, 1);

        // columna izquierda: cliente actual y hasta tres tareas
        Pixel.texto(g, "PEDIDOS", 30, 64, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Cliente c = juego.clienteActual();
        if (c == null) {
            Pixel.texto(g, "Mostrador vacío.", 30, 81, Pixel.T_CHICO, Pixel.GRIS);
        } else {
            Pixel.texto(g, Pixel.recortar(g, c.getNombre(), 154, Pixel.T_CUERPO, false),
                    30, 80, Pixel.T_CUERPO, Pixel.CREMA);
            List<String> pedido = Pixel.envolver(g, c.getPedido(), 154, Pixel.T_CHICO);
            int py = 93;
            for (int i = 0; i < Math.min(2, pedido.size()); i++) {
                Pixel.texto(g, Pixel.recortar(g, pedido.get(i), 154, Pixel.T_CHICO, false),
                        30, py, Pixel.T_CHICO, Pixel.GRIS);
                py += 11;
            }
            String estadoPedido;
            Color colorPedido;
            if (!c.isPedidoTomado()) {
                estadoPedido = "Pedido sin anotar";
                colorPedido = Pixel.ROJO_CLARO;
            } else if (juego.tieneBandejaPara(c)) {
                estadoPedido = "Bandeja lista";
                colorPedido = Pixel.VERDE;
            } else {
                estadoPedido = "Falta retirarla en la cocina";
                colorPedido = Pixel.MOSTAZA;
            }
            int tamEstado = Pixel.tamAjustado(g, "· " + estadoPedido, 154, Pixel.T_CHICO, Pixel.T_CHICO - 2);
            Pixel.texto(g, "· " + estadoPedido, 30, 115, tamEstado, colorPedido);
        }

        Pixel.texto(g, "TAREAS", 30, 128, Pixel.T_CUERPO, Pixel.MOSTAZA);
        List<Tarea> pendientes = new ArrayList<>();
        for (Tarea t : juego.getTareas()) {
            if (!t.isCompletada()) {
                pendientes.add(t);
            }
        }
        if (pendientes.isEmpty()) {
            Pixel.texto(g, "Todo limpio. Por ahora.", 30, 144, Pixel.T_CHICO, Pixel.GRIS);
        } else {
            int maximo = 3;
            int ty = 143;
            for (int i = 0; i < Math.min(maximo, pendientes.size()); i++) {
                Tarea t = pendientes.get(i);
                String marca = t.getPrioridad() >= 3 ? "!!" : t.getPrioridad() == 2 ? "!" : "·";
                Color colorMarca = t.getPrioridad() >= 3 ? Pixel.ROJO_CLARO
                        : t.getPrioridad() == 2 ? Pixel.MOSTAZA : Pixel.GRIS;
                Pixel.texto(g, marca, 30, ty, Pixel.T_CHICO, colorMarca);
                int tamTitulo = Pixel.tamAjustado(g, t.getTitulo(), 138, Pixel.T_CHICO, Pixel.T_CHICO - 2);
                Pixel.texto(g, Pixel.recortar(g, t.getTitulo(), 138, tamTitulo, false),
                        46, ty, tamTitulo, Pixel.CREMA);
                String detalle = t.getLugar().getSala().getNombre()
                        + (t.requiereHerramienta() ? " · " + t.getHerramienta().getNombre()
                        : " · " + t.getLugar().getEtiqueta());
                int tamDetalle = Pixel.tamAjustado(g, detalle, 162, Pixel.T_CHICO, Pixel.T_CHICO - 2);
                Pixel.texto(g, Pixel.recortar(g, detalle, 162, tamDetalle, false),
                        30, ty + 10, tamDetalle, Pixel.GRIS_OSCURO);
                ty += 20;
            }
            if (pendientes.size() > maximo) {
                Pixel.texto(g, "+" + (pendientes.size() - maximo) + " tareas más", 30, ty, Pixel.T_CHICO,
                        Pixel.GRIS_OSCURO);
            }
        }

        // columna derecha: mochila
        int x = 200;
        Pixel.texto(g, "MOCHILA", x, 64, Pixel.T_CUERPO, Pixel.MOSTAZA);
        Objeto enMano = juego.getEnMano();
        String mano = enMano == null ? "nada" : Pixel.recortar(g, enMano.getNombre(), 118, Pixel.T_CHICO, false);
        Pixel.texto(g, "En mano: " + mano, x, 77, Pixel.T_CHICO, Pixel.GRIS);
        g.setColor(Pixel.BORDE);
        g.fillRect(x, 82, 154, 1);

        List<Objeto> objetos = inventario();
        if (objetos.isEmpty()) {
            Pixel.texto(g, "Vacía. Limpiá tareas para", x, 100, Pixel.T_CHICO, Pixel.GRIS);
            Pixel.texto(g, "conseguir cosas.", x, 112, Pixel.T_CHICO, Pixel.GRIS);
        } else {
            int filas = Math.min(6, objetos.size());
            if (seleccion >= objetos.size()) {
                seleccion = Math.max(0, objetos.size() - 1);
            }
            for (int i = 0; i < filas; i++) {
                Objeto o = objetos.get(i);
                int fy = 88 + i * 20;
                boolean elegido = i == seleccion;
                if (elegido) {
                    Pixel.panelTenue(g, x - 3, fy - 2, 157, 19, 170, Pixel.MOSTAZA);
                }
                dibujarIcono(g, o, x, fy + 2);
                int tamNombre = Pixel.tamAjustado(g, o.getNombre(), 96, Pixel.T_CHICO, Pixel.T_CHICO - 2);
                Pixel.texto(g, o.getNombre(), x + 15, fy + 11, tamNombre,
                        elegido ? Pixel.CREMA : Pixel.GRIS);
                String etiqueta = o instanceof Herramienta ? "EQUIPAR" : (o.isUtilizable() ? "USAR" : "—");
                Pixel.textoDerecha(g, etiqueta, x + 149, fy + 11, Pixel.T_CHICO, Pixel.MOSTAZA);
            }
            if (objetos.size() > filas) {
                Pixel.texto(g, "+" + (objetos.size() - filas) + " más", x, 213, Pixel.T_CHICO,
                        Pixel.GRIS_OSCURO);
            }
        }

        Pixel.textoCentrado(g, "Flechas elegir  ·  ENTER usar  ·  TAB cerrar",
                ANCHO / 2, 222, Pixel.T_CHICO, Pixel.GRIS);
    }

    /** Diálogo del conserje: reemplaza al viejo log, sin carteles fijos abajo. */
    private void dibujarCharla(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 22, 36, 340, 164, new Color(0x0D1411), Pixel.MOSTAZA);
        Pixel.textoCentradoDisplay(g, "EL CONSERJE", ANCHO / 2, 58, Pixel.T_SUBTITULO,
                Pixel.MOSTAZA, true);
        g.setColor(Pixel.BORDE);
        g.drawLine(38, 66, 346, 66);

        List<String> lineas = Pixel.envolver(g, charla, 296, Pixel.T_CUERPO);
        int y = 84;
        for (int i = 0; i < Math.min(8, lineas.size()); i++) {
            Pixel.texto(g, lineas.get(i), 44, y, Pixel.T_CUERPO, Pixel.CREMA);
            y += Pixel.altoLinea(Pixel.T_CUERPO);
        }
        if (lineas.size() > 8) {
            Pixel.texto(g, "…", 44, y, Pixel.T_CUERPO, Pixel.GRIS);
        }
        Pixel.textoCentrado(g, "ENTER para seguir", ANCHO / 2, 190, Pixel.T_CHICO,
                Pixel.GRIS_OSCURO);
    }

    private void dibujarPausa(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 96, 58, 192, 124, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.textoCentradoDisplay(g, "PAUSA", ANCHO / 2, 82, Pixel.T_SUBTITULO, Pixel.MOSTAZA, true);
        for (int i = 0; i < OPCIONES_PAUSA.length; i++) {
            Pixel.boton(g, 112, 98 + i * 23, 160, 17, OPCIONES_PAUSA[i], i == seleccion,
                    i == 2, Pixel.MOSTAZA);
        }
        Pixel.textoCentrado(g, "ESC para volver", ANCHO / 2, 174, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    private void dibujarConfirmarSalida(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 48, 56, 288, 132, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.textoCentradoDisplay(g, "SALIR DE LA PARTIDA", ANCHO / 2, 80, Pixel.T_SUBTITULO,
                Pixel.MOSTAZA, true);
        String pregunta = "¿Querés guardar el progreso antes de volver?";
        int tamPregunta = Pixel.tamAjustado(g, pregunta, 264, Pixel.T_CUERPO, Pixel.T_CHICO);
        Pixel.textoCentrado(g, pregunta, ANCHO / 2, 99, tamPregunta, Pixel.GRIS);
        for (int i = 0; i < OPCIONES_SALIR.length; i++) {
            Pixel.boton(g, 62, 112 + i * 22, 260, 17, OPCIONES_SALIR[i], i == seleccion,
                    i == 1, Pixel.MOSTAZA);
        }
        Pixel.textoCentrado(g, "ENTER confirmar  ·  ESC cancelar", ANCHO / 2, 181, Pixel.T_CHICO,
                Pixel.GRIS_OSCURO);
    }

    // ------------------------------------------------------------------ fin de día

    private void dibujarFinDeDia(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 20, 6, 344, 228, new Color(0x0D1411), Pixel.BORDE_CLARO);
        Pixel.textoCentradoDisplay(g, "SE TERMINÓ LA JORNADA", ANCHO / 2, 32, Pixel.T_SUBTITULO,
                Pixel.MOSTAZA, true);
        Pixel.textoCentrado(g, "DÍA " + juego.getJugador().getDiaActual() + "  ·  "
                + juego.getJugador().getNombre(), ANCHO / 2, 45, Pixel.T_CHICO, Pixel.GRIS);

        int[] valores = {juego.clientesAtendidos(), juego.tareasHechas(), juego.getJugador().getDiaActual()};
        String[] titulos = {"CLIENTES", "TAREAS", "DÍA"};
        String[] maximos = {"/" + juego.totalClientes(), "/" + juego.totalTareas(), ""};
        for (int i = 0; i < 3; i++) {
            int x = 40 + i * 104;
            Pixel.panelTenue(g, x, 52, 96, 32, 120, Pixel.BORDE);
            Pixel.textoCentradoDisplay(g, valores[i] + maximos[i], x + 48, 71, Pixel.T_SUBTITULO,
                    i == 2 ? Pixel.MOSTAZA : Pixel.CREMA, true);
            Pixel.textoCentrado(g, titulos[i], x + 48, 82, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        }

        int cordura = juego.getJugador().getCordura();
        Color color = cordura < JuegoService.CORDURA_MINIMA_DIA ? Pixel.ROJO
                : cordura < 55 ? Pixel.MOSTAZA : Pixel.VERDE;
        Pixel.texto(g, "CORDURA AL CERRAR", 40, 99, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        Pixel.barra(g, 76, 104, 236, 8, cordura, 100, color, new Color(0x1B2621), Pixel.BORDE);
        Pixel.textoDisplay(g, String.valueOf(cordura), 40, 133, Pixel.T_TITULO, color, true);
        if (cordura < JuegoService.CORDURA_MINIMA_DIA) {
            Pixel.texto(g, "Debajo del mínimo (" + JuegoService.CORDURA_MINIMA_DIA
                    + "): si pasás, perdés.", 76, 133, Pixel.T_CHICO, Pixel.ROJO_CLARO);
        } else {
            Pixel.texto(g, "Al dormir recuperás " + JuegoService.RECUPERACION_DIARIA
                    + " de cordura.", 76, 133, Pixel.T_CHICO, Pixel.GRIS);
        }

        List<Objeto> consumibles = new ArrayList<>();
        for (Objeto o : inventario()) {
            if (o.isUtilizable()) {
                consumibles.add(o);
            }
        }

        Pixel.texto(g, "CONSUMIBLES", 40, 148, Pixel.T_CUERPO, Pixel.MOSTAZA);
        if (consumibles.isEmpty()) {
            Pixel.texto(g, "No te queda nada. Vas a cerrar así.", 40, 167, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
        } else {
            for (int i = 0; i < Math.min(4, consumibles.size()); i++) {
                Objeto o = consumibles.get(i);
                boolean elegido = i == seleccion;
                int fy = 158 + i * 15;
                if (elegido) {
                    Pixel.panelTenue(g, 36, fy - 1, 156, 14, 175, Pixel.MOSTAZA);
                }
                dibujarIcono(g, o, 40, fy);
                int tamNombre = Pixel.tamAjustado(g, o.getNombre(), 108, Pixel.T_CHICO, Pixel.T_CHICO - 2);
                Pixel.texto(g, o.getNombre(), 55, fy + 10, tamNombre, elegido ? Pixel.CREMA : Pixel.GRIS);
                if (o.getEfectoCordura() != 0) {
                    Pixel.textoDerecha(g, (o.getEfectoCordura() > 0 ? "+" : "") + o.getEfectoCordura(),
                            188, fy + 10, Pixel.T_CHICO, Pixel.VERDE);
                }
            }
        }

        Pixel.texto(g, "DECISIÓN", 204, 148, Pixel.T_CUERPO, Pixel.MOSTAZA);
        int base = consumibles.size();
        for (int i = 0; i < OPCIONES_FIN.length; i++) {
            String texto = OPCIONES_FIN[i];
            boolean peligro = false;
            if (i == 1) {
                if (confirmarAvance) {
                    texto = "CONFIRMAR Y ARRIESGAR";
                    peligro = true;
                } else if (cordura < JuegoService.CORDURA_MINIMA_DIA) {
                    texto = "Pasar (peligro)";
                    peligro = true;
                }
            }
            Pixel.boton(g, 204, 154 + i * 21, 146, 17, texto, seleccion == base + i, peligro, Pixel.MOSTAZA);
        }
        Pixel.texto(g, "ENTER confirmar  ·  ESC volver", 204, 225, Pixel.T_CHICO, Pixel.GRIS_OSCURO);
    }

    private void dibujarDerrota(Graphics2D g) {
        oscurecer(g);
        Pixel.panel(g, 32, 34, 320, 184, new Color(0x120C0B), new Color(0x6E231F));
        Pixel.textoCentradoDisplay(g, "PERDISTE", ANCHO / 2, 70, Pixel.T_GRANDE, Pixel.ROJO_CLARO, true);
        String causa = causaDerrota == null
                ? "La noche se cerró sobre el buffet y ya no hubo salida."
                : causaDerrota;
        List<String> lineas = Pixel.envolver(g, causa, 276, Pixel.T_CUERPO);
        int y = 96;
        for (int i = 0; i < Math.min(4, lineas.size()); i++) {
            String linea = i == 3 && lineas.size() > 4
                    ? Pixel.recortar(g, lineas.get(i) + "...", 276, Pixel.T_CUERPO, false)
                    : lineas.get(i);
            Pixel.textoCentrado(g, linea, ANCHO / 2, y, Pixel.T_CUERPO, Pixel.CREMA);
            y += Pixel.altoLinea(Pixel.T_CUERPO);
        }
        int separador = Math.max(126, Math.min(152, y + 8));
        g.setColor(new Color(0x6E231F));
        g.fillRect(64, separador, 256, 1);
        Pixel.textoCentrado(g, "Clientes atendidos: " + juego.clientesAtendidos()
                + "  ·  Tareas: " + juego.tareasHechas(), ANCHO / 2, separador + 16,
                Pixel.T_CHICO, Pixel.GRIS);
        Pixel.textoCentrado(g, "Días sobrevividos: " + Math.max(0, juego.getJugador().getDiaActual() - 1),
                ANCHO / 2, separador + 29, Pixel.T_CHICO, Pixel.GRIS);
        if ((System.currentTimeMillis() / 450) % 2 == 0) {
            Pixel.textoCentrado(g, "ENTER para volver al menú", ANCHO / 2, 200, Pixel.T_CUERPO,
                    Pixel.MOSTAZA_CLARA, true);
        }
    }

    private void oscurecer(Graphics2D g) {
        g.setColor(new Color(3, 5, 5, 185));
        g.fillRect(0, 0, ANCHO, ALTO);
    }

    // ------------------------------------------------------------------ teclado

    private void teclaPresionada(KeyEvent e) {
        int codigo = e.getKeyCode();
        if (esMovimiento(codigo) || codigo == KeyEvent.VK_SHIFT) {
            teclas.add(codigo);
        }

        switch (estado) {
            case INTRO -> {
                if (codigo == KeyEvent.VK_ENTER || codigo == KeyEvent.VK_SPACE) {
                    avanzarIntro();
                } else if (codigo == KeyEvent.VK_ESCAPE) {
                    alSalir.run();
                }
            }
            case JUEGO -> jugarTecla(codigo);
            case CUADERNO -> cuadernoTecla(codigo);
            case CHARLA -> {
                if (codigo == KeyEvent.VK_ENTER || codigo == KeyEvent.VK_ESCAPE
                        || codigo == KeyEvent.VK_SPACE) {
                    estado = Estado.JUEGO;
                    charla = "";
                    teclas.clear();
                }
            }
            case PAUSA -> pausaTecla(codigo);
            case CONFIRMAR_SALIDA -> salirTecla(codigo);
            case CERRAR_DIA -> finDiaTecla(codigo);
            case DERROTA -> {
                if (codigo == KeyEvent.VK_ENTER || codigo == KeyEvent.VK_ESCAPE) {
                    alSalir.run();
                }
            }
            default -> { }
        }
    }

    private void jugarTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_E -> interactuar();
            case KeyEvent.VK_ENTER -> {
                if (juego.turnoTerminado()) {
                    abrirCierreDeJornada();
                } else if (escena.zonaCercana() != null) {
                    interactuar();
                }
            }
            case KeyEvent.VK_I -> usarRapido();
            case KeyEvent.VK_Q -> soltar();
            case KeyEvent.VK_TAB -> abrirCuaderno();
            case KeyEvent.VK_ESCAPE -> {
                seleccion = 0;
                estado = Estado.PAUSA;
            }
            default -> { }
        }
    }

    private void cuadernoTecla(int codigo) {
        List<Objeto> objetos = inventario();
        switch (codigo) {
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_TAB -> {
                estado = Estado.JUEGO;
                seleccion = 0;
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                if (!objetos.isEmpty()) {
                    seleccion = (seleccion + objetos.size() - 1) % objetos.size();
                }
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                if (!objetos.isEmpty()) {
                    seleccion = (seleccion + 1) % objetos.size();
                }
            }
            case KeyEvent.VK_ENTER, KeyEvent.VK_E -> {
                if (!objetos.isEmpty()) {
                    Objeto elegido = objetos.get(Math.min(seleccion, objetos.size() - 1));
                    usarObjeto(elegido);
                }
            }
            default -> { }
        }
    }

    private void pausaTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> estado = Estado.JUEGO;
            case KeyEvent.VK_UP, KeyEvent.VK_W -> seleccion = (seleccion + OPCIONES_PAUSA.length - 1)
                    % OPCIONES_PAUSA.length;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> seleccion = (seleccion + 1) % OPCIONES_PAUSA.length;
            case KeyEvent.VK_ENTER -> {
                if (seleccion == 0) {
                    estado = Estado.JUEGO;
                } else if (seleccion == 1) {
                    guardarSilencioso();
                } else {
                    seleccion = 0;
                    estado = Estado.CONFIRMAR_SALIDA;
                }
            }
            default -> { }
        }
    }

    private void salirTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> estado = Estado.PAUSA;
            case KeyEvent.VK_UP, KeyEvent.VK_W -> seleccion = (seleccion + OPCIONES_SALIR.length - 1)
                    % OPCIONES_SALIR.length;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> seleccion = (seleccion + 1) % OPCIONES_SALIR.length;
            case KeyEvent.VK_ENTER -> {
                if (seleccion == 0) {
                    if (guardarSilencioso()) {
                        alSalir.run();
                    }
                } else if (seleccion == 1) {
                    alSalir.run();
                } else {
                    estado = Estado.PAUSA;
                }
            }
            default -> { }
        }
    }

    private void finDiaTecla(int codigo) {
        List<Objeto> consumibles = new ArrayList<>();
        for (Objeto o : inventario()) {
            if (o.isUtilizable()) {
                consumibles.add(o);
            }
        }
        int total = consumibles.size() + OPCIONES_FIN.length;
        switch (codigo) {
            case KeyEvent.VK_ESCAPE -> {
                estado = Estado.JUEGO;
                confirmarAvance = false;
                seleccion = 0;
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W -> seleccion = (seleccion + total - 1) % total;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> seleccion = (seleccion + 1) % total;
            case KeyEvent.VK_ENTER -> {
                if (seleccion < consumibles.size()) {
                    usarObjeto(consumibles.get(seleccion));
                    seleccion = Math.min(seleccion, Math.max(0, consumibles.size() - 1));
                } else {
                    int opcion = seleccion - consumibles.size();
                    if (opcion == 0) {
                        estado = Estado.JUEGO;
                        confirmarAvance = false;
                        seleccion = 0;
                    } else if (opcion == 1) {
                        if (juego.getJugador().getCordura() < JuegoService.CORDURA_MINIMA_DIA
                                && !confirmarAvance) {
                            confirmarAvance = true;
                        } else {
                            avanzarDia();
                        }
                    } else {
                        if (guardarSilencioso()) {
                            alSalir.run();
                        }
                    }
                }
            }
            default -> { }
        }
    }

    private static boolean esMovimiento(int codigo) {
        return codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_A
                || codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_D
                || codigo == KeyEvent.VK_UP || codigo == KeyEvent.VK_DOWN
                || codigo == KeyEvent.VK_LEFT || codigo == KeyEvent.VK_RIGHT;
    }

    // ------------------------------------------------------------------ acciones

    private void interactuar() {
        if (estado != Estado.JUEGO) {
            return;
        }
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaInteraccion < 220) {
            return;
        }
        ultimaInteraccion = ahora;

        Zona zona = escena.zonaCercana();
        if (zona == null) {
            return;
        }

        try {
            switch (prefijo(zona.id())) {
                case "cliente" -> atenderMostrador();
                case "estacion" -> juego.interactuarEstacion(
                        Lugar.valueOf(zona.id().substring("estacion:".length())));
                case "tarea" -> juego.usarHerramientaEnTarea(Lugar.valueOf(zona.id().substring(6)));
                case "herramienta" -> juego.recogerHerramienta(Lugar.valueOf(zona.id().substring(12)));
                case "npc" -> {
                    charla = juego.hablarConConserje();
                    estado = Estado.CHARLA;
                    teclas.clear();
                }
                default -> { }
            }
        } catch (InteraccionInvalidaException | TareaNoDisponibleException | PedidoInvalidoException e) {
            avisarError(e);
        }
        verificarFin();
    }

    private static String prefijo(String id) {
        int corte = id.indexOf(':');
        return corte < 0 ? id : id.substring(0, corte);
    }

    private String atenderMostrador() throws InteraccionInvalidaException, PedidoInvalidoException {
        Cliente c = juego.clienteActual();
        if (c == null) {
            return "No queda nadie en el mostrador. Podés cerrar la jornada.";
        }
        if (!c.isPedidoTomado()) {
            return juego.tomarPedido();
        }
        // Siempre pasa por el Service: si la bandeja no es la correcta,
        // PedidoInvalidoException viaja hasta la UI y se muestra con JOptionPane.
        return juego.entregarPedido();
    }

    private void usarRapido() {
        try {
            juego.usarLoPrimero();
        } catch (ObjetoNoDisponibleException | InteraccionInvalidaException e) {
            avisarError(e);
        }
        verificarFin();
    }

    private void usarObjeto(Objeto objeto) {
        try {
            juego.usarObjeto(objeto);
        } catch (ObjetoNoDisponibleException | InteraccionInvalidaException e) {
            avisarError(e);
        }
        verificarFin();
    }

    private void soltar() {
        try {
            juego.soltarEnMano();
        } catch (ObjetoNoDisponibleException e) {
            avisarError(e);
        }
        verificarFin();
    }

    private void abrirCuaderno() {
        seleccion = 0;
        estado = Estado.CUADERNO;
    }

    private void verificarFin() {
        if (estado != Estado.JUEGO) {
            return;
        }
        if (juego.isPerdida()) {
            derrota(juego.getCausaPerdida());
        }
    }

    private void derrota(String causa) {
        if (estado == Estado.DERROTA) {
            return;
        }
        causaDerrota = causa;
        estado = Estado.DERROTA;
        seleccion = 0;
        teclas.clear();
        // No se guarda la derrota: al volver al menú se retoma la última partida guardada.
    }

    private void abrirCierreDeJornada() {
        if (!juego.turnoTerminado()) {
            return;
        }
        seleccion = 0;
        confirmarAvance = false;
        estado = Estado.CERRAR_DIA;
        teclas.clear();
    }

    private void avanzarDia() {
        JuegoService.ResultadoDia resultado = juego.avanzarDia();
        if (resultado.perdio()) {
            derrota(resultado.detalle());
            return;
        }
        if (!resultado.avanzo()) {
            estado = Estado.JUEGO;
            return;
        }
        guardarSilencioso();
        estado = Estado.JUEGO;
        seleccion = 0;
        confirmarAvance = false;
        avisoCierre = false;
        escena.setSala(juego.getSala());
        escena.reiniciarPosicion();
        golpeCorduraTicks = 0;
    }

    // ------------------------------------------------------------------ guardado y salida

    private boolean guardarSilencioso() {
        try {
            partidas.guardarProgreso(juego.getJugador());
            return true;
        } catch (PersistenciaException e) {
            avisarError(e);
            return false;
        }
    }

    /** Lo llama VentanaPrincipal al cerrar la ventana. Guarda el avance y deja cerrar igual. */
    public boolean confirmarSalida() {
        if (estado != Estado.DERROTA) {
            guardarSilencioso();
        }
        return true;
    }

    // ------------------------------------------------------------------ utilidades

    /**
     * Muestra el cuadro de diálogo de la excepción y detiene el reloj mientras está abierto:
     * un JOptionPane modal comparte el EDT y, si no paráramos el Timer, la partida seguiría corriendo detrás.
     */
    private void avisarError(Exception e) {
        boolean corriendo = timer.isRunning();
        timer.stop();
        try {
            Dialogos.error(this, e);
        } finally {
            if (corriendo && isDisplayable()) {
                timer.start();
            }
        }
    }

    private static void dibujarIcono(Graphics2D g, Objeto objeto, int x, int y) {
        if (objeto instanceof Herramienta h) {
            EscenaBuffet.dibujarHerramienta(g, h.getLugar(), x, y);
            return;
        }
        if ("PEDIDO".equals(objeto.getTipoObjeto())) {
            dibujarBandeja(g, x, y + 2);
            return;
        }
        EscenaBuffet.dibujarObjeto(g, objeto, x, y);
    }

    /** Bandeja de metal con un ojo: el icono del juego. */
    private static void dibujarBandeja(Graphics2D g, int x, int y) {
        Pixel.elipse(g, x + 2, y + 6, 20, 7, new Color(0x1A211E));
        Pixel.elipse(g, x + 3, y + 5, 18, 6, new Color(0x8A9691));
        Pixel.elipse(g, x + 4, y + 5, 16, 4, new Color(0xC3CCC6));
        Pixel.elipse(g, x + 7, y + 1, 10, 7, new Color(0xF1EAD8));
        Pixel.elipse(g, x + 9, y + 2, 6, 5, new Color(0x7FB58A));
        Pixel.elipse(g, x + 11, y + 3, 2, 3, new Color(0x120F0C));
    }
}
