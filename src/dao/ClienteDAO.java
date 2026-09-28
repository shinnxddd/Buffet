package dao;

import modelo.Cliente;
import modelo.Mundo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteDAO extends AbstractEntidadDAO {

    @Override
    protected String tipo() {
        return "CLIENTE";
    }

    @Override
    public void insertar(Mundo entidad) throws SQLException {
        Cliente c = comoCliente(entidad);
        String sql = "INSERT INTO entidades (nombre, tipo, rol, dialogo, pedido, atendido) "
                + "VALUES (?, 'CLIENTE', ?, ?, ?, ?)";
        int id = insertarConClave(sql, ps -> {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getRol());
            ps.setString(3, c.getDialogo());
            ps.setString(4, c.getPedido());
            ps.setBoolean(5, c.isAtendido());
        });
        c.setId(id);
    }

    @Override
    public void actualizar(Mundo entidad) throws SQLException {
        Cliente c = comoCliente(entidad);
        String sql = "UPDATE entidades SET nombre = ?, pedido = ?, atendido = ? "
                + "WHERE id = ? AND tipo = 'CLIENTE'";
        ejecutar(sql, ps -> {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getPedido());
            ps.setBoolean(3, c.isAtendido());
            ps.setInt(4, c.getId());
        });
    }

    @Override
    protected Mundo mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("pedido"),
                rs.getBoolean("atendido"));
    }

    private Cliente comoCliente(Mundo entidad) {
        if (!(entidad instanceof Cliente)) {
            throw new IllegalArgumentException("ClienteDAO sólo persiste Cliente.");
        }
        return (Cliente) entidad;
    }
}
