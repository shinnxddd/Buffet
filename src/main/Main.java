package main;

import dao.Conexion;
import interfaces.Interactuable;
import modelo.Cliente;
import modelo.Jugador;
import modelo.NPC;
import modelo.Objeto;
import modelo.Tarea;
import service.JuegoService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

// beta funcional  

public class Main {

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private static boolean bdDisponible = true;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--jugar")) {
            JuegoService juego = new JuegoService(new Jugador(0, "Octavio", 1, 0, 0));
            juego.agregarObjeto(new Objeto(0, "Un café", "BEBIDA", true, 3));
            juego.generarCliente("Un cliente", "Un café");
            juego.generarTarea("El baño está sucio", "LIMPIEZA", 1);
            new MenuConsola(juego).jugar();
            return;
        }

        try {
            demo();
        } catch (Exception e) {
            System.out.println("[ERROR INESPERADO, CONTROLADO] " + e);
        }
    }

    private static void demo() throws Exception {
        Jugador jugador = new Jugador(0, "Octavio", 1, 0, 0);
        JuegoService juego = new JuegoService(jugador);


        titulo("1. Conexión y persistencia en MySQL (DAO)");
        try (Connection con = Conexion.obtenerConexion()) {
            System.out.println("Conexión a MySQL OK.");
        } catch (SQLException e) {
            bdDisponible = false;
            System.out.println("[BD] No se pudo conectar (" + e.getMessage() + ").");
            System.out.println("[BD] ¿Ejecutaron sql/buffet.sql y agregaron el conector JDBC? "
                    + "Se sigue la demo sin persistencia.");
        }
        bd(() -> {
            juego.crearEntidad(jugador);
            System.out.println("INSERT jugador -> " + jugador);
            System.out.println("SELECT por id  -> " + juego.buscarEntidad("JUGADOR", jugador.getId()));
            System.out.println("SELECT todos   -> " + juego.listarEntidades("JUGADOR").size() + " jugador(es)");
            System.out.println("SELECT id -1   -> " + juego.buscarEntidad("JUGADOR", -1));
        });

        titulo("2. Preparación del buffet");
        juego.agregarObjeto(new Objeto(0, "un café", "BEBIDA", true, 1));
        juego.agregarObjeto(new Objeto(0, "una medialuna", "COMIDA", true, 3));
        juego.agregarObjeto(new Objeto(0, "trapo roto", "LIMPIEZA", false, 1));
        Tarea baño = juego.generarTarea("el baño está sucio", "LIMPIEZA", 1);
        juego.generarTarea("limpiar las mesas", "LIMPIEZA", 1);
        Tarea heladeras = juego.generarTarea("reponer las heladeras", "REPOSICION", 2);
        juego.mostrarInventario();

        titulo("3. Error forzado: todavía no vino nadie");
        forzar("ClienteNoDisponibleException", () -> juego.esperarCliente());

        titulo("4. Día 1: atender al primer cliente");
        Cliente c1 = juego.generarCliente("un cliente", "un café");
        Cliente c2 = juego.generarCliente("una profesora", "un tostado");
        Cliente c3 = juego.generarCliente("un pibe de 3°", "una medialuna");
        Cliente actual = juego.esperarCliente();
        juego.interactuar(actual);
        juego.mostrarPedido(actual);
        juego.atenderCliente(actual, "1");
        System.out.println("Dinero: $" + jugador.getDinero());

        titulo("5. Polimorfismo: service.interactuar(Interactuable)");
        List<Interactuable> cosas = Arrays.asList(
                c2,
                juego.getInventario().buscarPorNombre("trapo roto"),
                baño,
                new NPC(99, "la directora", "DIRECTIVO", "Quiero el buffet impecable.", true));
        for (Interactuable cosa : cosas) {
            juego.interactuar(cosa);
        }

    
        titulo("6. Errores forzados (reglas de negocio)");
        forzar("PedidoInvalidoException (cliente ya atendido)", () -> juego.atenderCliente(c1, "1"));
        forzar("ObjetoNoDisponibleException (no hay tostado)", () -> juego.atenderCliente(c2, "1"));
        forzar("OpcionInvalidaException (opción 9)", () -> juego.atenderCliente(c2, "9"));
        forzar("OpcionInvalidaException (tipo de entidad sin DAO)", () -> juego.buscarEntidad("MONSTRUO", 1));
        forzar("ObjetoNoDisponibleException (usar objeto inexistente)", () -> juego.usarObjeto("espada láser"));
        forzar("InteraccionInvalidaException (objeto no utilizable)", () -> juego.usarObjeto("trapo roto"));
        forzar("InteraccionInvalidaException (interactuar con null)", () -> juego.interactuar(null));
        forzar("TareaNoDisponibleException (tarea de un día futuro)", () -> juego.completarTarea(heladeras));
        forzar("DiaInvalidoException (quedan tareas pendientes)", () -> juego.avanzarDia());

    
        titulo("7. Cerrar el día 1");
        juego.atenderCliente(c2, "2");
        juego.realizarTarea(juego.siguienteTarea(), "1");
        juego.realizarTarea(juego.siguienteTarea(), "1");
        forzar("TareaNoDisponibleException (tarea ya hecha)", () -> juego.completarTarea(baño));
        juego.avanzarDia();
        bd(() -> {
            juego.guardarPartida();
            System.out.println("[BD] Partida guardada (día 1 cerrado).");
        });

        titulo("8. Día 2");
        juego.realizarTarea(juego.siguienteTarea(), "3");
        juego.realizarTarea(juego.siguienteTarea(), "1");
        juego.atenderCliente(juego.esperarCliente(), "1");
        forzar("TareaNoDisponibleException (ya no quedan tareas)", () -> juego.siguienteTarea());
        forzar("ClienteNoDisponibleException (ya no quedan clientes)", () -> juego.esperarCliente());
        juego.mostrarInventario();
        bd(() -> {
            juego.guardarPartida();
            System.out.println("[BD] Partida guardada (día 2).");
        });

   
        titulo("9. Recuperar la partida desde MySQL");
        System.out.println("En memoria: día " + jugador.getDiaActual() + ", tareas hechas "
                + jugador.getTareasCompletadas() + ", dinero $" + jugador.getDinero());
        bd(() -> {
            JuegoService copia = new JuegoService(new Jugador(0, "vacío", 1, 0, 0));
            copia.cargarPartida(jugador.getId());
            Jugador j = copia.getJugador();
            System.out.println("Desde la BD: " + j + ": día " + j.getDiaActual() + ", tareas hechas "
                    + j.getTareasCompletadas() + ", dinero $" + j.getDinero());
            System.out.println("Clientes: " + copia.getClientes().size()
                    + " | Tareas: " + copia.getTareas().size()
                    + " | Objetos: " + copia.getInventario().getObjetos().size());
            copia.mostrarInventario();
        });

     
        titulo("10. Limpieza de la BD");
        bd(() -> {
            juego.eliminarPartida();
            System.out.println("Partida eliminada. SELECT del jugador -> "
                    + juego.buscarEntidad("JUGADOR", jugador.getId()));
        });

        System.out.println("\nFin de la demo. (Para jugar: java main.Main --jugar)");
    }

    //se encarga de q se ejecute la bdd

    private static void bd(Accion accion) {
        if (!bdDisponible) {
            return;
        }
        try {
            accion.ejecutar();
        } catch (SQLException e) {
            bdDisponible = false;
            System.out.println("[BD] Error de base de datos: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[BD] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void forzar(String descripcion, Accion accion) {
        System.out.println(">> " + descripcion);
        try {
            accion.ejecutar();
            System.out.println("   (no falló: revisar la regla)");
        } catch (Exception e) {
            System.out.println("   [" + e.getClass().getSimpleName() + "] " + e.getMessage());
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }
}
