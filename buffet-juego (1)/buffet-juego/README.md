# Buffet Terror

Juego de terror psicológico ambientado en el buffet de un colegio. Atendés clientes, hacés tareas y cuidás tu cordura.
Proyecto Integrador de **Laboratorio de Programación Orientada a Objetos** (Ejercitación 16: escalado a interfaz gráfica).

- **Integrantes:** Octavio Pagni -- Jazmin Toledano
- **Tecnología gráfica elegida:** Java **Swing + FlatLaf** (Maven)
- **Base de datos:** MySQL

## Requisitos

- JDK **17** o superior
- Maven 3.8+
- MySQL 8 (o MariaDB) corriendo en `localhost:3306`

## Cómo ejecutarlo

1. **Crear la base:** ejecutá el script `sql/schema.sql` en MySQL (crea la base `buffet_terror`, la tabla `entidades` y una partida de ejemplo).
   ```
   mysql -u root -p < sql/schema.sql
   ```
2. **Configurar la conexión** (opcional si usás `root` sin clave en `localhost`): copiá `config.example.properties` como `config.properties` y completá tus datos. Ese archivo está en `.gitignore`, no se sube a GitHub.
3. **Jugar:**
   ```
   mvn compile exec:java
   ```
   También podés generar un `.jar` ejecutable con `mvn package` y abrir `target/buffet-juego-1.0.0.jar` con doble clic (o `java -jar target/buffet-juego-1.0.0.jar`, parado en la carpeta del proyecto para que encuentre `config.properties`).

Para verificar que compila: `mvn clean compile`.

## Cómo se juega

1. En el menú, **creá una partida** con el formulario (nombre y día inicial) o elegí una de la tabla y tocá **Jugar** (también con doble clic).
2. Controlás al personaje en un plano 2D del buffet:
   - **Moverte:** `W A S D` o las flechas.
   - **Interactuar:** `E` cuando aparece el cartel sobre el personaje.
   - **Decidir:** teclas `1`, `2`, `3` o clic sobre la opción.
3. Los tres lugares equivalen a las tres acciones de la versión de consola:
   - **Mostrador:** esperar a que venga un cliente. Dar el pedido (+5 cordura), no darlo (−5) o preguntar por qué.
   - **Charco y balde:** realizar tareas. Hacerla, no hacerla o romperlo todo (−15 cordura).
   - **Casilleros:** abrir el inventario.
4. Cuanto menos cordura tenés, más se oscurece la sala, parpadean las luces y pasan cosas raras en las ventanas.
5. **Guardar partida** escribe tu progreso en MySQL. Al **salir al menú** (o cerrar la ventana) te pregunta si querés guardar.
6. Desde el menú podés **editar** el nombre/día de una partida o **eliminarla**.

## Arquitectura en capas

```
src/main/java
├── ui/            Ventana, formulario, tabla, escena 2D del buffet (EscenaBuffet, Java2D) y cuadros de diálogo. Sin reglas de negocio ni SQL.
├── service/       PartidaService (alta/baja/modificación/consulta) y JuegoService (reglas del juego). Lanzan las excepciones.
├── dao/           EntidadDAO / JugadorDAO / Conexion: CRUD sobre MySQL.
├── modelo/        Mundo → Personaje → Jugador, Cliente, NPC; Objeto, Tarea, Inventario.
├── interfaces/    Interactuable
├── excepciones/   Las 5 excepciones del juego + PersistenciaException (fallas de base de datos)
└── main/          Main: abre la ventana
referencia-consola/  Main y JuegoService originales de la versión de consola (solo como referencia, no se compilan)
```

Flujo: Usuario → UI → Service → DAO → MySQL, y las excepciones viajan de vuelta del Service a la UI, que las muestra en un cuadro de diálogo.

## Capturas

Ver carpeta [`docs/`](docs/): ventana principal, un registro exitoso y un cuadro de error de excepción.
