package dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Conexion {

    private static final Properties CONFIG = cargarConfig();

    // Valores por defecto; se pisan con config.properties si el archivo existe (ver config.example.properties).
    private static Properties cargarConfig() {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:mysql://localhost:3306/buffet_terror?useSSL=false&serverTimezone=UTC");
        p.setProperty("db.usuario", "root");
        p.setProperty("db.clave", "");
        try (InputStream in = new FileInputStream("config.properties")) {
            p.load(in);
        } catch (IOException e) {
            // Sin archivo: se usan los valores por defecto.
        }
        return p;
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.usuario"),
                CONFIG.getProperty("db.clave"));
    }
}
