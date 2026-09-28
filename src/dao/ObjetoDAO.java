package dao;

import modelo.Mundo;
import modelo.Objeto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ObjetoDAO extends AbstractEntidadDAO {

    @Override
    protected String tipo() {
        return "OBJETO";
    }

    @Override
    public void insertar(Mundo entidad) throws SQLException {
        Objeto o = comoObjeto(entidad);
        String sql = "INSERT INTO entidades (nombre, tipo, tipo_objeto, utilizable, cantidad) "
                + "VALUES (?, 'OBJETO', ?, ?, ?)";
        int id = insertarConClave(sql, ps -> {
            ps.setString(1, o.getNombre());
            ps.setString(2, o.getTipoObjeto());
            ps.setBoolean(3, o.isUtilizable());
            ps.setInt(4, o.getCantidad());
        });
        o.setId(id);
    }

    @Override
    public void actualizar(Mundo entidad) throws SQLException {
        Objeto o = comoObjeto(entidad);
        String sql = "UPDATE entidades SET nombre = ?, tipo_objeto = ?, utilizable = ?, cantidad = ? "
                + "WHERE id = ? AND tipo = 'OBJETO'";
        ejecutar(sql, ps -> {
            ps.setString(1, o.getNombre());
            ps.setString(2, o.getTipoObjeto());
            ps.setBoolean(3, o.isUtilizable());
            ps.setInt(4, o.getCantidad());
            ps.setInt(5, o.getId());
        });
    }

    @Override
    protected Mundo mapear(ResultSet rs) throws SQLException {
        return new Objeto(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("tipo_objeto"),
                rs.getBoolean("utilizable"),
                rs.getInt("cantidad"));
    }

    private Objeto comoObjeto(Mundo entidad) {
        if (!(entidad instanceof Objeto)) {
            throw new IllegalArgumentException("ObjetoDAO sólo persiste Objeto.");
        }
        return (Objeto) entidad;
    }
}
