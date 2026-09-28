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
        objetos.add(objeto);
    }

    public void quitar(Objeto objeto) {
        objetos.remove(objeto);
    }
}
