package main;

import excepciones.ClienteNoDisponibleException;
import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.OpcionInvalidaException;
import excepciones.PedidoInvalidoException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Tarea;
import service.JuegoService;

import java.sql.SQLException;
import java.util.Scanner;

// este es el menu de la consola interactivo para jugar lolol

public class MenuConsola {

    private final JuegoService juego;
    private final Scanner sc = new Scanner(System.in);

    public MenuConsola(JuegoService juego) {
        this.juego = juego;
    }

    public void jugar() {
        while (true) {
            System.out.println();
            System.out.println("Día " + juego.getJugador().getDiaActual()
                    + " | Dinero $" + juego.getJugador().getDinero());
            System.out.println("1- Esperar a que venga un cliente");
            System.out.println("2- Realizar tareas");
            System.out.println("3- Abrir inventario");
            System.out.println("4- Usar objeto");
            System.out.println("5- Terminar el día");
            System.out.println("6- Guardar partida (se guarda en la base de datos)");
            System.out.println("7-salir");

            String opcion = leer();
            if (opcion == null || opcion.equals("7")) {
                System.out.println("Cerrando el buffet...");
                return;
            }

            try {
                switch (opcion) {
                    case "1": atenderCliente(); break;
                    case "2": hacerTarea(); break;
                    case "3": juego.mostrarInventario(); break;
                    case "4": usarObjeto(); break;
                    case "5": juego.avanzarDia(); break;
                    case "6": guardar(); break;
                    default: throw new OpcionInvalidaException("Esa opción no existe.");
                }
            } catch (ClienteNoDisponibleException | TareaNoDisponibleException | ObjetoNoDisponibleException
                     | PedidoInvalidoException | InteraccionInvalidaException | OpcionInvalidaException
                     | DiaInvalidoException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private void atenderCliente() throws ClienteNoDisponibleException, InteraccionInvalidaException,
            PedidoInvalidoException, ObjetoNoDisponibleException, OpcionInvalidaException {
        Cliente cliente = juego.esperarCliente();
        System.out.println();
        System.out.println(cliente.getNombre().toUpperCase() + " !!!!!!!!!!!!");
        juego.interactuar(cliente);
        System.out.println("1) Dar " + cliente.getPedido());
        System.out.println("2) No dar " + cliente.getPedido());
        System.out.println("3) Preguntar por qué quiere eso");
        juego.atenderCliente(cliente, leerOpcion());
    }

    private void hacerTarea() throws TareaNoDisponibleException, OpcionInvalidaException {
        Tarea tarea = juego.siguienteTarea();
        System.out.println();
        System.out.println("¡¡¡TAREAAAAAAAAAAAA!!!");
        juego.mostrarTarea(tarea);
        System.out.println("1) Hacer la tarea");
        System.out.println("2) No hacer la tarea");
        System.out.println("3) Romperlo todo");
        juego.realizarTarea(tarea, leerOpcion());
    }

    private void usarObjeto() throws ObjetoNoDisponibleException, InteraccionInvalidaException {
        juego.mostrarInventario();
        System.out.println("¿Qué objeto querés usar? (nombre)");
        juego.usarObjeto(leerOpcion());
    }

    private void guardar() {
        try {
            juego.guardarPartida();
            System.out.println("Partida guardada.");
        } catch (SQLException e) {
            System.out.println("[BD] No se pudo guardar: " + e.getMessage());
        }
    }

    private String leerOpcion() {
        String s = leer();
        return s == null ? "" : s;
    }

    private String leer() {
        System.out.print("> ");
        if (!sc.hasNextLine()) {
            return null;
        }
        return sc.nextLine().trim();
    }
}
