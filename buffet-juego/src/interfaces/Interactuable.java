package interfaces;

/**
 * Comportamiento común a todo elemento con el que el jugador
 * puede interactuar (objetos, NPCs). El service la usa de forma
 * polimórfica en interactuar(Interactuable elemento).
 */
public interface Interactuable {
    void interactuar();
}
