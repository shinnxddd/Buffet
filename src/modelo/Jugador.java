package modelo;

public class Jugador extends Personaje {

    private int diaActual;
    private int tareasCompletadas;
    private int dinero;

    public Jugador(int id, String nombre, int diaActual, int tareasCompletadas, int dinero) {
        super(id, nombre);
        this.diaActual = diaActual;
        this.tareasCompletadas = tareasCompletadas;
        this.dinero = dinero;
    }

    public int getDiaActual() {
        return diaActual;
    }

    public void setDiaActual(int diaActual) {
        this.diaActual = diaActual;
    }

    public int getTareasCompletadas() {
        return tareasCompletadas;
    }

    public void sumarTareaCompletada() {
        this.tareasCompletadas++;
    }

    public int getDinero() {
        return dinero;
    }

    public void setDinero(int dinero) {
        this.dinero = dinero;
    }

    @Override
    public String getTipo() {
        return "JUGADOR";
    }
}
