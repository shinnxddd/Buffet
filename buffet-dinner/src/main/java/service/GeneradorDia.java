package service;

import modelo.Cliente;
import modelo.Lugar;
import modelo.Tarea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Arma cada jornada. Es determinista (misma semilla = mismo día), así que al cargar una partida guardada
 * el día se reconstruye igual. Cada día vienen más clientes y hay más cosas rotas.
 */
public final class GeneradorDia {

    private record Perfil(String nombre, int aspecto, String uno, String dos) { }

    private static final Perfil[] PERFILES = {
            new Perfil("un cliente", 0, "Un cliente no contesta y se queda mirándote fijo.",
                    "Un cliente parpadea una sola vez. Tarda demasiado en volver a abrir los ojos."),
            new Perfil("una nena de guardapolvo", 1, "La nena de guardapolvo dice que su mamá la espera afuera. Afuera no hay nadie.",
                    "La nena susurra: \"Vos también te vas a quedar acá\"."),
            new Perfil("el profesor de gimnasia", 2, "El profesor de gimnasia dice que hace años que corre y no logra salir del patio.",
                    "El profesor te muestra el cronómetro: marca la hora de mañana."),
            new Perfil("un señor de impermeable", 3, "El señor de impermeable gotea sobre el mostrador. Afuera no llueve.",
                    "El señor dice que ya vino ayer. Y anteayer. Y el año que viene."),
            new Perfil("la directora", 4, "La directora te recuerda que el buffet no cierra nunca.",
                    "La directora anota algo en su libreta. Es tu nombre, repetido cientos de veces."),
            new Perfil("el portero", 5, "El portero dice que cerró todas las puertas. Todas menos esta.",
                    "El portero te muestra su llavero. No tiene ninguna llave."),
            new Perfil("el nene del último banco", 6, "El nene del último banco dice que está en penitencia desde 1994.",
                    "La mochila del nene se mueve sola. Él no la mira."),
            new Perfil("la maestra de plástica", 7, "La maestra de plástica pregunta si ya terminaste el dibujo. No empezaste ninguno.",
                    "La maestra sonríe. La sonrisa tarda un segundo de más en llegar."),
    };

    private static final String[][] PRODUCTOS = {
            {"CAFE", "un café"}, {"ALFAJOR", "un alfajor"}, {"AGUA", "una botella de agua"},
            {"TOSTADO", "un tostado de jamón y queso"}, {"MILANESA", "un sándwich de milanesa"}};

    private GeneradorDia() { }

    public static int cantidadClientes(int dia) {
        return Math.min(PERFILES.length, 2 + dia);
    }

    /** Segundos que espera un cliente en el mostrador: cada día se impacientan más. */
    public static double paciencia(int dia) {
        return Math.max(38, 66 - 3.0 * dia);
    }

    public static List<Cliente> clientes(int dia) {
        Random azar = new Random(dia * 7919L);
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < PERFILES.length; i++) {
            orden.add(i);
        }
        Collections.shuffle(orden, azar);
        List<Cliente> lista = new ArrayList<>();
        for (int i = 0; i < cantidadClientes(dia); i++) {
            Perfil p = PERFILES[orden.get(i)];
            String[] producto = PRODUCTOS[azar.nextInt(PRODUCTOS.length)];
            lista.add(new Cliente(dia * 100 + i + 1, p.nombre(), producto[1], producto[0], p.aspecto(),
                    paciencia(dia), p.uno(), p.dos()));
        }
        return lista;
    }

    public static List<Tarea> tareas(int dia) {
        Object[][] todas = {
                {Lugar.BANO, "el baño perdió agua y hay un charco que no para de crecer", "AGUAAAAAAAAA!!!!!!!!!!!!!!!!!!",
                        "Secaste el charco. El agua dejó de salir, aunque nadie tocó la canilla.", "TRAPEADOR"},
                {Lugar.MESAS, "las mesas del comedor quedaron pegajosas y con restos de comida", "MESAAAAAS PEGAJOSAAAAAS!!!!!!!!!!!!",
                        "Dejaste las mesas relucientes. Alguien dibujó una carita en el vidrio de la ventana.", "TRAPO"},
                {Lugar.BASURA, "la bolsa de basura está por explotar y hay moscas por todos lados", "MOSCAAAAAAS!!!!!!!!!",
                        "Cambiaste la bolsa. No mires hacia atrás.", "BOLSA"},
                {Lugar.CAFETERA, "la cafetera gotea algo oscuro que no parece café", "GOTEOOOOOOO!!!!!!!!!!!",
                        "Destapaste la cafetera y volvió a salir café de verdad.", "DESTAPADOR"},
                {Lugar.PLANCHA, "la plancha está llena de grasa quemada y larga un humo raro", "HUMOOOOO!!!!!!!!!!!!!!!!!!",
                        "Raspaste la plancha hasta que quedó como espejo. En el reflejo, alguien te saludó.", "RASQUETA"},
                {Lugar.HELADERA, "la heladera quedó abierta y hay bebidas tiradas por el piso", "FRIOOOOOOOO!!!!!!!!!!!",
                        "Acomodaste todo y cerraste la heladera. Adentro algo se acomodó contigo.", "CAJA"},
                {Lugar.TABLERO, "el tablero de luz chispea y las luces parpadean solas", "CHISPASSSSSS!!!!!!!!!!!!",
                        "Cambiaste el fusible. La luz vuelve a ser estable... por ahora.", "FUSIBLE"},
        };
        List<Tarea> lista = new ArrayList<>();
        int cantidad = Math.min(todas.length, 2 + dia);
        for (int i = 0; i < cantidad; i++) {
            Object[] t = todas[i];
            lista.add(new Tarea(i + 1, (String) t[1], dia, false, (Lugar) t[0], (String) t[2], (String) t[3], (String) t[4]));
        }
        return lista;
    }
}
