package service;

import excepciones.InteraccionInvalidaException;
import excepciones.PedidoInvalidoException;
import excepciones.TareaNoDisponibleException;
import modelo.Cliente;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Tarea;

import java.util.List;
import java.util.Scanner;

public class JuegoService {

    private final Jugador jugador;
    private final List<Cliente> clientes;
    private final List<Tarea> tareas;
    private final Inventario inventario;
    private final Scanner sc;

    public JuegoService(Jugador jugador, List<Cliente> clientes, List<Tarea> tareas, Inventario inventario) {
        this.jugador = jugador;
        this.clientes = clientes;
        this.tareas = tareas;
        this.inventario = inventario;
        this.sc = new Scanner(System.in);
    }

    public void jugar() {
        boolean jugando = true;
        while (jugando) {
            System.out.println();
            System.out.println("1-esperar a que venga un cliente");
            System.out.println("2-realizar tareas");
            System.out.println("3-abrir inventario");
            System.out.println("4-salir");
            System.out.print("> ");

            String opcion = sc.nextLine().trim();

            try {
                switch (opcion) {
                    case "1":
                        esperarCliente();
                        break;
                    case "2":
                        realizarTarea();
                        break;
                    case "3":
                        abrirInventario();
                        break;
                    case "4":
                        jugando = false;
                        System.out.println("Cerrando el buffet...");
                        break;
                    default:
                        throw new InteraccionInvalidaException("Esa opción no existe.");
                }
            } catch (InteraccionInvalidaException | TareaNoDisponibleException | PedidoInvalidoException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private void esperarCliente() throws InteraccionInvalidaException, PedidoInvalidoException {
        Cliente cliente = clientes.stream()
                .filter(c -> !c.isAtendido())
                .findFirst()
                .orElse(null);

        if (cliente == null) {
            throw new InteraccionInvalidaException("No vino ningún cliente todavía.");
        }

        System.out.println();
        System.out.println(cliente.getNombre().toUpperCase() + " !!!!!!!!!!!!");
        cliente.interactuar();
        System.out.println();
        System.out.println("1)dar " + cliente.getPedido());
        System.out.println("2)no dar " + cliente.getPedido());
        System.out.println("3)preguntar por qué");
        System.out.print("> ");

        String opcion = sc.nextLine().trim();

        switch (opcion) {
            case "1":
                if (cliente.isAtendido()) {
                    throw new PedidoInvalidoException("Ese cliente ya fue atendido.");
                }
                cliente.marcarAtendido();
                jugador.setCordura(jugador.getCordura() + 5);
                System.out.println("Le diste " + cliente.getPedido() + " a " + cliente.getNombre() + ". Se va contento.");
                break;
            case "2":
                cliente.marcarAtendido();
                jugador.setCordura(jugador.getCordura() - 5);
                System.out.println(cliente.getNombre() + " se va enojado.");
                break;
            case "3":
                System.out.println(cliente.getNombre() + " no contesta y se queda mirándote fijo.");
                break;
            default:
                throw new InteraccionInvalidaException("Esa opción no existe.");
        }
    }

    private void realizarTarea() throws TareaNoDisponibleException, InteraccionInvalidaException {
        Tarea tarea = tareas.stream()
                .filter(t -> !t.isCompletada())
                .findFirst()
                .orElse(null);

        if (tarea == null) {
            throw new TareaNoDisponibleException("No hay tareas pendientes.");
        }

        System.out.println();
        System.out.println("TAREAAAAAAAAAAAA!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(tarea.getDescripcion());
        System.out.println();
        System.out.println("1)hacer la tarea");
        System.out.println("2)no hacer la tarea");
        System.out.println("3)romperlo todo");
        System.out.print("> ");

        String opcion = sc.nextLine().trim();

        switch (opcion) {
            case "1":
                if (tarea.isCompletada()) {
                    throw new TareaNoDisponibleException("Esa tarea ya estaba hecha.");
                }
                tarea.setCompletada(true);
                jugador.sumarTareaCompletada();
                System.out.println("Listo, tarea completada.");
                break;
            case "2":
                System.out.println("La dejás para después.");
                break;
            case "3":
                jugador.setCordura(jugador.getCordura() - 15);
                System.out.println("Rompiste todo. Tu cordura baja.");
                break;
            default:
                throw new InteraccionInvalidaException("Esa opción no existe.");
        }
    }

    private void abrirInventario() {
        System.out.println();
        System.out.println("INVENTARIO");
        if (inventario.getObjetos().isEmpty()) {
            System.out.println("(vacío)");
            return;
        }
        inventario.getObjetos().forEach(o -> System.out.println("- " + o.getNombre()));
    }
}
