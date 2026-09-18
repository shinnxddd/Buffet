package main;

import modelo.Cliente;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Tarea;
import service.JuegoService;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        Jugador jugador = new Jugador(1, "Octavio", 1, 100, 0);

        List<Cliente> clientes = new ArrayList<>();
        clientes.add(new Cliente(1, "un cliente", "un café"));

        List<Tarea> tareas = new ArrayList<>();
        tareas.add(new Tarea(1, "el baño está sucio", 1, false));

        Inventario inventario = new Inventario();

        JuegoService juego = new JuegoService(jugador, clientes, tareas, inventario);
        juego.jugar();
    }
}
