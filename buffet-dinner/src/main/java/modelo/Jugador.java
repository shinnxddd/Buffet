package modelo;

public class Jugador extends Personaje {

    private int diaActual;
    private int cordura;
    private int tareasCompletadas;

    public Jugador(int id, String nombre, int diaActual, int cordura, int tareasCompletadas) {
        super(id, nombre);
        this.diaActual = diaActual;
        this.cordura = cordura;
        this.tareasCompletadas = tareasCompletadas;
    }

    public int getDiaActual() {
        return diaActual;
    }

    public void setDiaActual(int diaActual) {
        this.diaActual = diaActual;
    }

    public int getCordura() {
        return cordura;
    }

    public void setCordura(int cordura) {
        this.cordura = cordura;
    }

    public int getTareasCompletadas() {
        return tareasCompletadas;
    }

    public void sumarTareaCompletada() {
        this.tareasCompletadas++;
    }

    /** Derrota: la partida vuelve al día 1 con la cordura completa. */
    public void reiniciar() {
        this.diaActual = 1;
        this.cordura = 100;
        this.tareasCompletadas = 0;
    }

    @Override
    public String getTipo() {
        return "JUGADOR";
    }
}
