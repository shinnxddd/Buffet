package service;

import dao.ClienteDAO;
import dao.EntidadDAO;
import dao.JugadorDAO;
import dao.ObjetoDAO;
import dao.TareaDAO;
import excepciones.ClienteNoDisponibleException;
import excepciones.DiaInvalidoException;
import excepciones.InteraccionInvalidaException;
import excepciones.ObjetoNoDisponibleException;
import excepciones.OpcionInvalidaException;
import excepciones.PedidoInvalidoException;
import excepciones.TareaNoDisponibleException;
import interfaces.Interactuable;
import modelo.Cliente;
import modelo.Inventario;
import modelo.Jugador;
import modelo.Mundo;
import modelo.Objeto;
import modelo.Tarea;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class JuegoService {

    private static final int PROPINA = 50;
    private static final int MULTA_CLIENTE_ENOJADO = 10;
    private static final int MULTA_ROMPER_TODO = 30;

    private Jugador jugador;
    private Inventario inventario = new Inventario();
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Tarea> tareas = new ArrayList<>();

    private final JugadorDAO jugadorDAO = new JugadorDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObjetoDAO objetoDAO = new ObjetoDAO();
    private final TareaDAO tareaDAO = new TareaDAO();

    public JuegoService(Jugador jugador) {
        this.jugador = jugador;
    }

    public Jugador getJugador() { return jugador; }
    public Inventario getInventario() { return inventario; }
    public List<Cliente> getClientes() { return clientes; }
    public List<Tarea> getTareas() { return tareas; }


    public void crearEntidad(Mundo entidad) throws SQLException, OpcionInvalidaException {
        daoPorTipo(entidad.getTipo()).insertar(entidad);
    }

    public Mundo buscarEntidad(String tipo, int id) throws SQLException, OpcionInvalidaException {
        return daoPorTipo(tipo).buscarPorId(id);
    }

    public List<Mundo> listarEntidades(String tipo) throws SQLException, OpcionInvalidaException {
        return daoPorTipo(tipo).listarTodos();
    }

    public void eliminarEntidad(String tipo, int id) throws SQLException, OpcionInvalidaException {
        daoPorTipo(tipo).eliminar(id);
    }

    private EntidadDAO daoPorTipo(String tipo) throws OpcionInvalidaException {
        switch (tipo == null ? "" : tipo.toUpperCase()) {
            case "JUGADOR": return jugadorDAO;
            case "CLIENTE": return clienteDAO;
            case "OBJETO":  return objetoDAO;
            case "TAREA":   return tareaDAO;
            default:
                throw new OpcionInvalidaException("No hay persistencia para el tipo \"" + tipo + "\".");
        }
    }

    public void guardarPartida() throws SQLException {
        guardar(jugadorDAO, jugador);
        for (Cliente c : clientes) {
            guardar(clienteDAO, c);
        }
        for (Tarea t : tareas) {
            guardar(tareaDAO, t);
        }
        for (Objeto o : inventario.getObjetos()) {
            guardar(objetoDAO, o);
        }
    }


    public void cargarPartida(int idJugador) throws SQLException {
        Mundo encontrado = jugadorDAO.buscarPorId(idJugador);
        if (encontrado == null) {
            throw new SQLException("No existe un jugador con id " + idJugador + " en la base de datos.");
        }
        jugador = (Jugador) encontrado;

        clientes.clear();
        for (Mundo m : clienteDAO.listarTodos()) {
            clientes.add((Cliente) m);
        }
        tareas.clear();
        for (Mundo m : tareaDAO.listarTodos()) {
            tareas.add((Tarea) m);
        }
        inventario = new Inventario();
        for (Mundo m : objetoDAO.listarTodos()) {
            inventario.agregar((Objeto) m);
        }
    }

    public void eliminarPartida() throws SQLException {
        for (Cliente c : clientes) {
            if (c.getId() != 0) clienteDAO.eliminar(c.getId());
        }
        for (Tarea t : tareas) {
            if (t.getId() != 0) tareaDAO.eliminar(t.getId());
        }
        for (Objeto o : inventario.getObjetos()) {
            if (o.getId() != 0) objetoDAO.eliminar(o.getId());
        }
        if (jugador.getId() != 0) jugadorDAO.eliminar(jugador.getId());
    }

    private void guardar(EntidadDAO dao, Mundo entidad) throws SQLException {
        if (entidad.getId() == 0) {
            dao.insertar(entidad);
        } else {
            dao.actualizar(entidad);
        }
    }


    public Cliente generarCliente(String nombre, String pedido) {
        Cliente cliente = new Cliente(0, nombre, pedido);
        clientes.add(cliente);
        return cliente;
    }

    public Cliente esperarCliente() throws ClienteNoDisponibleException {
        for (Cliente c : clientes) {
            if (!c.isAtendido()) {
                return c;
            }
        }
        throw new ClienteNoDisponibleException("No vino ningún cliente todavía.");
    }

    public void mostrarPedido(Cliente cliente) {
        System.out.println(cliente.getNombre() + " pidió: " + cliente.getPedido());
    }

    public void entregarPedido(Cliente cliente) throws PedidoInvalidoException, ObjetoNoDisponibleException {
        if (cliente.isAtendido()) {
            throw new PedidoInvalidoException("Ese cliente ya fue atendido.");
        }
        Objeto objeto = inventario.buscarPorNombre(cliente.getPedido());
        if (objeto == null || objeto.getCantidad() <= 0) {
            throw new ObjetoNoDisponibleException("No tenés " + cliente.getPedido() + " en el inventario.");
        }

        objeto.setCantidad(objeto.getCantidad() - 1);
        cliente.marcarAtendido();
        cambiarDinero(+PROPINA);
        System.out.println("Le diste " + cliente.getPedido() + " a " + cliente.getNombre()
                + ". Se va contento (+$" + PROPINA + ").");
    }

    public void atenderCliente(Cliente cliente, String opcion)
            throws PedidoInvalidoException, ObjetoNoDisponibleException, OpcionInvalidaException {
        if (cliente.isAtendido()) {
            throw new PedidoInvalidoException("Ese cliente ya fue atendido.");
        }
        switch (opcion) {
            case "1":
                entregarPedido(cliente);
                break;
            case "2":
                cliente.marcarAtendido();
                cambiarDinero(-MULTA_CLIENTE_ENOJADO);
                System.out.println(cliente.getNombre() + " se va enojado y se queja (-$"
                        + MULTA_CLIENTE_ENOJADO + ").");
                break;
            case "3":
                System.out.println(cliente.getNombre() + " no contesta y se queda mirándote fijo.");
                break;
            default:
                throw new OpcionInvalidaException("Esa opción no existe: " + opcion);
        }
    }

    public Tarea generarTarea(String descripcion, String tipo, int dia) {
        Tarea tarea = new Tarea(0, descripcion, tipo, dia, false);
        tareas.add(tarea);
        return tarea;
    }

    public Tarea siguienteTarea() throws TareaNoDisponibleException {
        for (Tarea t : tareas) {
            if (!t.isCompletada() && t.getDia() <= jugador.getDiaActual()) {
                return t;
            }
        }
        throw new TareaNoDisponibleException("No hay tareas pendientes.");
    }

    public void mostrarTarea(Tarea tarea) {
        tarea.interactuar();
    }

    public void completarTarea(Tarea tarea) throws TareaNoDisponibleException {
        if (tarea.isCompletada()) {
            throw new TareaNoDisponibleException("Esa tarea ya estaba hecha.");
        }
        if (tarea.getDia() > jugador.getDiaActual()) {
            throw new TareaNoDisponibleException("Esa tarea es del día " + tarea.getDia()
                    + " y todavía estás en el día " + jugador.getDiaActual() + ".");
        }
        tarea.setCompletada(true);
        jugador.sumarTareaCompletada();
        System.out.println("Listo, tarea completada.");
    }


    public void realizarTarea(Tarea tarea, String opcion)
            throws TareaNoDisponibleException, OpcionInvalidaException {
        switch (opcion) {
            case "1":
                completarTarea(tarea);
                break;
            case "2":
                System.out.println("La dejás para después.");
                break;
            case "3":
                cambiarDinero(-MULTA_ROMPER_TODO);
                System.out.println("Rompiste todo y tenés que pagar los daños (-$" + MULTA_ROMPER_TODO + ").");
                break;
            default:
                throw new OpcionInvalidaException("Esa opción no existe: " + opcion);
        }
    }

    public void avanzarDia() throws DiaInvalidoException {
        for (Tarea t : tareas) {
            if (!t.isCompletada() && t.getDia() <= jugador.getDiaActual()) {
                throw new DiaInvalidoException("No podés terminar el día: falta \"" + t.getDescripcion() + "\".");
            }
        }
        jugador.setDiaActual(jugador.getDiaActual() + 1);
        System.out.println("Empieza el día " + jugador.getDiaActual() + ".");
    }

    public void mostrarInventario() {
        System.out.println("INVENTARIO");
        boolean hayAlgo = false;
        for (Objeto o : inventario.getObjetos()) {
            if (o.getCantidad() > 0) {
                System.out.println("- " + o.getNombre() + " x" + o.getCantidad());
                hayAlgo = true;
            }
        }
        if (!hayAlgo) {
            System.out.println("(vacío)");
        }
    }

    public void agregarObjeto(Objeto objeto) {
        inventario.agregar(objeto);
    }

    public void eliminarObjeto(String nombre) throws ObjetoNoDisponibleException {
        Objeto objeto = inventario.buscarPorNombre(nombre);
        if (objeto == null || objeto.getCantidad() <= 0) {
            throw new ObjetoNoDisponibleException("No tenés \"" + nombre + "\" en el inventario.");
        }
        objeto.setCantidad(0);
    }

    public void usarObjeto(String nombre) throws ObjetoNoDisponibleException, InteraccionInvalidaException {
        Objeto objeto = inventario.buscarPorNombre(nombre);
        if (objeto == null || objeto.getCantidad() <= 0) {
            throw new ObjetoNoDisponibleException("No tenés \"" + nombre + "\" en el inventario.");
        }
        if (!objeto.isUtilizable()) {
            throw new InteraccionInvalidaException(objeto.getNombre() + " no se puede usar ahora mismo.");
        }
        interactuar(objeto);
        objeto.setCantidad(objeto.getCantidad() - 1);
    }


    public void interactuar(Interactuable elemento) throws InteraccionInvalidaException {
        if (elemento == null) {
            throw new InteraccionInvalidaException("No hay nada con qué interactuar.");
        }
        elemento.interactuar();
    }

    private void cambiarDinero(int delta) {
        jugador.setDinero(Math.max(0, jugador.getDinero() + delta));
    }
}
