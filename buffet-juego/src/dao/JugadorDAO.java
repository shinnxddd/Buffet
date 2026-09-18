package dao;

import modelo.Jugador;
import modelo.Mundo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JugadorDAO implements EntidadDAO {

    @Override
    public void insertar(Mundo entidad) throws SQLException {
        if (!(entidad instanceof Jugador)) {
            throw new IllegalArgumentException("Esta implementación sólo persiste Jugador por ahora.");
        }
        Jugador jugador = (Jugador) entidad;

        String sql = "INSERT INTO entidades "
                + "(nombre, tipo, dia_actual, cordura, tareas_completadas) "
                + "VALUES (?, 'JUGADOR', ?, ?, ?)";

        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, jugador.getNombre());
            ps.setInt(2, jugador.getDiaActual());
            ps.setInt(3, jugador.getCordura());
            ps.setInt(4, jugador.getTareasCompletadas());

            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    jugador.setId(claves.getInt(1));
                }
            }
        }
    }

    @Override
    public Mundo buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM entidades WHERE id = ? AND tipo = 'JUGADOR'";

        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearJugador(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Mundo> listarTodos() throws SQLException {
        List<Mundo> jugadores = new ArrayList<>();
        String sql = "SELECT * FROM entidades WHERE tipo = 'JUGADOR'";

        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                jugadores.add(mapearJugador(rs));
            }
        }
        return jugadores;
    }

    @Override
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM entidades WHERE id = ? AND tipo = 'JUGADOR'";

        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Jugador mapearJugador(ResultSet rs) throws SQLException {
        return new Jugador(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("dia_actual"),
                rs.getInt("cordura"),
                rs.getInt("tareas_completadas")
        );
    }
}
