package dao;

import modelo.Mundo;
import modelo.Tarea;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TareaDAO extends AbstractEntidadDAO {

    @Override
    protected String tipo() {
        return "TAREA";
    }

    @Override
    public void insertar(Mundo entidad) throws SQLException {
        Tarea t = comoTarea(entidad);
        String sql = "INSERT INTO entidades (nombre, tipo, descripcion, tipo_tarea, completada, dia) "
                + "VALUES (?, 'TAREA', ?, ?, ?, ?)";
        int id = insertarConClave(sql, ps -> {
            ps.setString(1, nombreCorto(t));
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getTipoTarea());
            ps.setBoolean(4, t.isCompletada());
            ps.setInt(5, t.getDia());
        });
        t.setId(id);
    }

    @Override
    public void actualizar(Mundo entidad) throws SQLException {
        Tarea t = comoTarea(entidad);
        String sql = "UPDATE entidades SET nombre = ?, descripcion = ?, tipo_tarea = ?, completada = ?, dia = ? "
                + "WHERE id = ? AND tipo = 'TAREA'";
        ejecutar(sql, ps -> {
            ps.setString(1, nombreCorto(t));
            ps.setString(2, t.getDescripcion());
            ps.setString(3, t.getTipoTarea());
            ps.setBoolean(4, t.isCompletada());
            ps.setInt(5, t.getDia());
            ps.setInt(6, t.getId());
        });
    }

    @Override
    protected Mundo mapear(ResultSet rs) throws SQLException {
        return new Tarea(
                rs.getInt("id"),
                rs.getString("descripcion"),
                rs.getString("tipo_tarea"),
                rs.getInt("dia"),
                rs.getBoolean("completada"));
    }

    private String nombreCorto(Tarea t) {
        String d = t.getDescripcion();
        return d.length() <= 50 ? d : d.substring(0, 50);
    }

    private Tarea comoTarea(Mundo entidad) {
        if (!(entidad instanceof Tarea)) {
            throw new IllegalArgumentException("TareaDAO sólo persiste Tarea.");
        }
        return (Tarea) entidad;
    }
}
