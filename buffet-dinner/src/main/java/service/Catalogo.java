package service;

import modelo.Objeto;

import java.util.LinkedHashMap;
import java.util.Map;

/** Todos los objetos que se pueden llevar en las manos: productos para los clientes y herramientas para las tareas. */
public final class Catalogo {

    private record Modelo(String nombre, boolean usable, int cordura, String frase) { }

    private static final Map<String, Modelo> MODELOS = new LinkedHashMap<>();
    private static int siguienteId = 1;

    static {
        // productos (se preparan en el salón y la cocina)
        MODELOS.put("CAFE", new Modelo("un café", true, 5, "Estaba rico, aunque era para un cliente."));
        MODELOS.put("ALFAJOR", new Modelo("un alfajor", true, 4, "Dulce de leche. Por un segundo todo parece normal."));
        MODELOS.put("AGUA", new Modelo("una botella de agua", true, 4, "Fría. Te despeja la cabeza."));
        MODELOS.put("TOSTADO", new Modelo("un tostado de jamón y queso", true, 6, "Crocante. Te lo comés sin pensar."));
        MODELOS.put("MILANESA", new Modelo("un sándwich de milanesa", true, 7, "Un manjar. Se te escapa un suspiro."));
        // herramientas (se agarran en el almacén)
        MODELOS.put("TRAPEADOR", new Modelo("el trapeador", false, 0, null));
        MODELOS.put("TRAPO", new Modelo("un trapo limpio", false, 0, null));
        MODELOS.put("RASQUETA", new Modelo("una rasqueta", false, 0, null));
        MODELOS.put("BOLSA", new Modelo("una bolsa de basura nueva", false, 0, null));
        MODELOS.put("DESTAPADOR", new Modelo("un destapador", false, 0, null));
        MODELOS.put("FUSIBLE", new Modelo("un fusible", false, 0, null));
        MODELOS.put("CAJA", new Modelo("una caja de bebidas", false, 0, null));
    }

    private Catalogo() { }

    public static String nombre(String tipo) {
        return MODELOS.get(tipo).nombre();
    }

    public static synchronized Objeto crear(String tipo) {
        Modelo m = MODELOS.get(tipo);
        return new Objeto(siguienteId++, m.nombre(), tipo, m.usable(), m.cordura(), m.frase());
    }
}
