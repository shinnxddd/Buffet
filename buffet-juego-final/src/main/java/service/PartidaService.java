package service;

import dao.EntidadDAO;
import dao.JugadorDAO;
import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.PersistenciaException;
import modelo.Cliente;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Lugar;
import modelo.Mundo;
import modelo.Objeto;
import modelo.Tarea;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Gestiona las partidas guardadas (Jugador en MySQL). La UI habla con esta clase y nunca con el DAO. */
public class PartidaService {

    private static final int LARGO_MAX_NOMBRE = 50;   // VARCHAR(50) en la tabla
    private static final int CORDURA_INICIAL = 100;   // igual que el Main de consola

    private final EntidadDAO dao = new JugadorDAO();

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws SQLException;
    }

    private <T> T enBaseDeDatos(String problema, Operacion<T> operacion) throws PersistenciaException {
        try {
            return operacion.ejecutar();
        } catch (SQLException e) {
            throw new PersistenciaException(problema
                    + " Revisá que MySQL esté encendido y que la base buffet_terror exista.", e);
        }
    }

    public List<Jugador> listar() throws PersistenciaException {
        return enBaseDeDatos("No se pudieron leer las partidas guardadas.", () -> {
            List<Jugador> jugadores = new ArrayList<>();
            for (Mundo m : dao.listarTodos()) {
                jugadores.add((Jugador) m);
            }
            return jugadores;
        });
    }

    public Jugador registrar(String nombre, int dia)
            throws DiaInvalidoException, InteraccionInvalidaException, PersistenciaException {
        String limpio = validar(nombre, dia, 0);
        Jugador jugador = new Jugador(0, limpio, dia, CORDURA_INICIAL, 0);
        enBaseDeDatos("No se pudo crear la partida.", () -> {
            dao.insertar(jugador);
            return null;
        });
        return jugador;
    }

    public void modificar(int id, String nombre, int dia)
            throws DiaInvalidoException, InteraccionInvalidaException, PersistenciaException {
        String limpio = validar(nombre, dia, id);
        Jugador jugador = enBaseDeDatos("No se pudo leer la partida.", () -> (Jugador) dao.buscarPorId(id));
        if (jugador == null) {
            throw new InteraccionInvalidaException("Esa partida ya no existe.");
        }
        jugador.setNombre(limpio);
        jugador.setDiaActual(dia);
        guardarProgreso(jugador);
    }

    /** Persiste el estado actual del jugador (cordura, tareas, día, nombre). */
    public void guardarProgreso(Jugador jugador) throws PersistenciaException {
        enBaseDeDatos("No se pudo guardar la partida.", () -> {
            dao.actualizar(jugador);
            return null;
        });
    }

    public void eliminar(int id) throws PersistenciaException {
        enBaseDeDatos("No se pudo eliminar la partida.", () -> {
            dao.eliminar(id);
            return null;
        });
    }

    /** Arma el turno: el cliente y la tarea del baño originales, más 4 clientes y 6 tareas nuevas. */
    public JuegoService iniciarJuego(Jugador jugador) {
        List<Cliente> clientes = new ArrayList<>();
        clientes.add(new Cliente(1, "un cliente", "un café",
                "Un cliente no contesta y se queda mirándote fijo.",
                "Un cliente parpadea una sola vez. Tarda demasiado en volver a abrir los ojos."));
        clientes.add(new Cliente(2, "una nena de guardapolvo", "un alfajor",
                "La nena de guardapolvo dice que su mamá la espera afuera. Afuera no hay nadie.",
                "La nena susurra: \"Vos también te vas a quedar acá\"."));
        clientes.add(new Cliente(3, "el profesor de gimnasia", "una botella de agua",
                "El profesor de gimnasia dice que hace años que corre y no logra salir del patio.",
                "El profesor te muestra el cronómetro: marca la hora de mañana."));
        clientes.add(new Cliente(4, "un señor de impermeable", "un tostado de jamón y queso",
                "El señor de impermeable gotea sobre el mostrador. Afuera no llueve.",
                "El señor dice que ya vino ayer. Y anteayer. Y el año que viene."));
        clientes.add(new Cliente(5, "la directora", "un sándwich de milanesa",
                "La directora te recuerda que el buffet no cierra nunca.",
                "La directora anota algo en su libreta. Es tu nombre, repetido cientos de veces."));

        List<Tarea> tareas = new ArrayList<>();
        tareas.add(new Tarea(1, "el baño está sucio", 1, false));
        tareas.add(new Tarea(2, "las mesas del comedor quedaron pegajosas y con restos de comida", 1, false,
                Lugar.MESAS, "MESAAAAAS PEGAJOSAAAAAS!!!!!!!!!!!!",
                "Dejaste las mesas relucientes. Debajo de un plato había una propina.",
                new Objeto(1, "una propina", "MONEDA", true, 5, "Te compraste un alfajor con la propina.")));
        tareas.add(new Tarea(3, "la plancha está llena de grasa quemada y larga un humo raro", 1, false,
                Lugar.PLANCHA, "HUMOOOOO!!!!!!!!!!!!!!!!!!",
                "Raspaste la plancha hasta que quedó como espejo. Algo brilló en la grasa.",
                new Objeto(2, "una llave oxidada", "LLAVE", false)));
        tareas.add(new Tarea(4, "la cafetera gotea algo oscuro que no parece café", 1, false,
                Lugar.CAFETERA, "GOTEOOOOOOO!!!!!!!!!!!",
                "Destapaste la cafetera y salió un café perfecto.",
                new Objeto(3, "un café recién hecho", "BEBIDA", true, 10, "Está caliente y por un rato todo parece normal.")));
        tareas.add(new Tarea(5, "la heladera quedó abierta y hay bebidas tiradas por el piso", 1, false,
                Lugar.HELADERA, "FRIOOOOOOOO!!!!!!!!!!!",
                "Acomodaste todo y cerraste la heladera. Se salvó una gaseosa.",
                new Objeto(4, "una gaseosa fría", "BEBIDA", true, 8, "Qué alivio.")));
        tareas.add(new Tarea(6, "la bolsa de basura está por explotar y hay moscas por todos lados", 1, false,
                Lugar.BASURA, "MOSCAAAAAAS!!!!!!!!!",
                "Sacaste la basura al patio. No mires hacia atrás. Se te cayó un papel del bolsillo del delantal.",
                new Objeto(5, "una nota arrugada", "NOTA", true, -5,
                        "Dice: \"NO LE DES NADA AL ÚLTIMO CLIENTE\". La letra es tuya.")));
        tareas.add(new Tarea(7, "el tablero de luz chispea y las luces parpadean solas", 1, false,
                Lugar.TABLERO, "CHISPASSSSSS!!!!!!!!!!!!",
                "Cambiaste el fusible. La luz vuelve a ser estable. Había una linterna en el cajón.",
                new Objeto(6, "una linterna", "HERRAMIENTA", true, 6, "Alumbra rincones donde antes no te animabas a mirar.")));

        return new JuegoService(jugador, clientes, tareas, new Inventario());
    }

    // Reglas de negocio del formulario: nombre obligatorio, largo, día válido y sin nombres repetidos.
    private String validar(String nombre, int dia, int idActual)
            throws DiaInvalidoException, InteraccionInvalidaException, PersistenciaException {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new InteraccionInvalidaException("Escribí un nombre para la partida.");
        }
        if (limpio.length() > LARGO_MAX_NOMBRE) {
            throw new InteraccionInvalidaException("El nombre puede tener hasta " + LARGO_MAX_NOMBRE + " caracteres.");
        }
        if (dia < 1) {
            throw new DiaInvalidoException("El día tiene que ser 1 o mayor.");
        }
        for (Jugador existente : listar()) {
            if (existente.getId() != idActual && existente.getNombre().equalsIgnoreCase(limpio)) {
                throw new InteraccionInvalidaException("Ya existe una partida con el nombre \"" + limpio + "\".");
            }
        }
        return limpio;
    }
}
