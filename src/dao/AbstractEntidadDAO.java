package dao;

import modelo.Mundo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public abstract class AbstractEntidadDAO implements EntidadDAO {

    @FunctionalInterface
    protected interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    protected abstract String tipo();

    protected abstract Mundo mapear(ResultSet rs) throws SQLException;

    @Override
    public Mundo buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM entidades WHERE id = ? AND tipo = ?";
        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, tipo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Mundo> listarTodos() throws SQLException {
        List<Mundo> lista = new ArrayList<>();
        String sql = "SELECT * FROM entidades WHERE tipo = ? ORDER BY id";
        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM entidades WHERE id = ? AND tipo = ?";
        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, tipo());
            ps.executeUpdate();
        }
    }


    protected int insertarConClave(String sql, Parametros parametros) throws SQLException {
        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametros.asignar(ps);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        }
        return 0;
    }

    protected void ejecutar(String sql, Parametros parametros) throws SQLException {
        try (Connection con = Conexion.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            parametros.asignar(ps);
            ps.executeUpdate();
        }
    }
}
