package modelo;

import java.util.ArrayList;
import java.util.List;

public class Inventario {

    private List<Objeto> objetos;

    public Inventario() {
        this.objetos = new ArrayList<>();
    }

    public List<Objeto> getObjetos() {
        return objetos;
    }

    public void agregar(Objeto objeto) {
        Objeto existente = buscarPorNombre(objeto.getNombre());
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + objeto.getCantidad());
        } else {
            objetos.add(objeto);
        }
    }

    public void quitar(Objeto objeto) {
        objetos.remove(objeto);
    }

    public Objeto buscarPorNombre(String nombre) {
        for (Objeto o : objetos) {
            if (o.getNombre().equalsIgnoreCase(nombre)) {
                return o;
            }
        }
        return null;
    }
}
