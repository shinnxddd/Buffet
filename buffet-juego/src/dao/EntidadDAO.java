package dao;

import modelo.Mundo;

import java.sql.SQLException;
import java.util.List;

public interface EntidadDAO {
    void insertar(Mundo entidad) throws SQLException;
    Mundo buscarPorId(int id) throws SQLException;
    List<Mundo> listarTodos() throws SQLException;
    void eliminar(int id) throws SQLException;
}
