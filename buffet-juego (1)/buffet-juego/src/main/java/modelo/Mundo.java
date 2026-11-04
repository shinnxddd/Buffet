package modelo;

public abstract class Mundo {

    protected int id;
    protected String nombre;

    public Mundo(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public abstract String getTipo();

    @Override
    public String toString() {
        return getTipo() + " #" + id + " [" + nombre + "]";
    }
}
