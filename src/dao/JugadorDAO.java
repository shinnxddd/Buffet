package dao;

import modelo.Jugador;
import modelo.Mundo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JugadorDAO extends AbstractEntidadDAO {

    @Override
    protected String tipo() {
        return "JUGADOR";
    }

    @Override
    public void insertar(Mundo entidad) throws SQLException {
        Jugador j = comoJugador(entidad);
        String sql = "INSERT INTO entidades (nombre, tipo, dia_actual, tareas_completadas, dinero) "
                + "VALUES (?, 'JUGADOR', ?, ?, ?)";
        int id = insertarConClave(sql, ps -> {
            ps.setString(1, j.getNombre());
            ps.setInt(2, j.getDiaActual());
            ps.setInt(3, j.getTareasCompletadas());
            ps.setInt(4, j.getDinero());
        });
        j.setId(id);
    }

    @Override
    public void actualizar(Mundo entidad) throws SQLException {
        Jugador j = comoJugador(entidad);
        String sql = "UPDATE entidades SET nombre = ?, dia_actual = ?, tareas_completadas = ?, dinero = ? "
                + "WHERE id = ? AND tipo = 'JUGADOR'";
        ejecutar(sql, ps -> {
            ps.setString(1, j.getNombre());
            ps.setInt(2, j.getDiaActual());
            ps.setInt(3, j.getTareasCompletadas());
            ps.setInt(4, j.getDinero());
            ps.setInt(5, j.getId());
        });
    }

    @Override
    protected Mundo mapear(ResultSet rs) throws SQLException {
        return new Jugador(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("dia_actual"),
                rs.getInt("tareas_completadas"),
                rs.getInt("dinero"));
    }

    private Jugador comoJugador(Mundo entidad) {
        if (!(entidad instanceof Jugador)) {
            throw new IllegalArgumentException("JugadorDAO sólo persiste Jugador.");
        }
        return (Jugador) entidad;
    }
}
