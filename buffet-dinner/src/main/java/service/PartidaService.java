package service;

import dao.EntidadDAO;
import dao.JugadorDAO;
import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.PersistenciaException;
import modelo.Jugador;
import modelo.Mundo;

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

    /** Arma la partida en el día en que quedó guardada (cada día genera sus clientes y tareas). */
    public JuegoService iniciarJuego(Jugador jugador) {
        return new JuegoService(jugador);
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
