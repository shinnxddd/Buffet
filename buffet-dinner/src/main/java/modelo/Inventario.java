package modelo;

import java.util.ArrayList;
import java.util.List;

/** Lo que el personaje lleva encima. Con capacidad 1 son "las manos": un objeto a la vez. */
public class Inventario {

    private final List<Objeto> objetos = new ArrayList<>();
    private final int capacidad;

    public Inventario() {
        this(Integer.MAX_VALUE);
    }

    public Inventario(int capacidad) {
        this.capacidad = capacidad;
    }

    public List<Objeto> getObjetos() { return objetos; }
    public boolean isLleno() { return objetos.size() >= capacidad; }
    public void agregar(Objeto objeto) { objetos.add(objeto); }
    public void quitar(Objeto objeto) { objetos.remove(objeto); }
}
