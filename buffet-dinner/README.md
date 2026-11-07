# Buffet Terror

Juego de terror psicológico ambientado en el buffet de un colegio. Atendés clientes, hacés tareas y cuidás tu cordura.
Proyecto Integrador de **Laboratorio de Programación Orientada a Objetos** (Ejercitación 16: escalado a interfaz gráfica).

- **Integrantes:** Aaron Serrano
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
2. Controlás al personaje en el buffet, que tiene **tres salas** unidas por puertas en los bordes de la pantalla:
   - **El salón:** mostrador (clientes), vitrina de alfajores, mesas, baño, basura, tablero de luz y casilleros.
   - **La cocina** (puerta de la izquierda): cafetera, plancha y heladera, donde se preparan los productos.
   - **El almacén** (puerta de la derecha): estantes de limpieza y de repuestos, con las herramientas.
   - **Moverte:** `W A S D` o las flechas. **Interactuar:** `E`. **Decidir:** teclas `1`–`5` o clic.
3. **Todo se lleva en las manos (un objeto por vez):**
   - Los clientes piden un producto (se ve en la burbuja, con un anillo que marca su paciencia). Hay que **prepararlo y llevárselo**: si te falta, te equivocás o tarda de más, se van furiosos.
   - Cada tarea necesita su **herramienta** del almacén (trapeador, trapo, rasqueta, bolsa, destapador, fusible, caja de bebidas), que se gasta al usarla.
   - Las estaciones rotas (cafetera tapada, plancha grasienta, heladera desordenada) **no se pueden usar** hasta que las arreglás.
   - En los casilleros podés usar lo que llevás (por ejemplo, tomarte el café y recuperar cordura), tirarlo o **dormir**.
4. **Los días:** cuando terminás la jornada podés dormir y empieza otro día con **más clientes, más cosas rotas y gente menos paciente**. Si dormís con cosas pendientes, perdés cordura.
5. **La cordura tiene consecuencias:** con poca la sala se oscurece, parpadean las luces, aparecen siluetas, y los clientes se impacientan más rápido. Si **llega a cero** o **pasás de día con menos de 30**, perdés y la partida vuelve al día 1.
6. **Guardar partida** escribe tu progreso en MySQL (el día y la cordura se conservan). Al **salir al menú** (o cerrar la ventana) te pregunta si querés guardar. Desde el menú podés **editar** el nombre/día de una partida o **eliminarla**.

## Arquitectura en capas

```
src/main/java
├── ui/            Ventana, formulario, tabla y escena 2D (EscenaBuffet, con Salas, Figura y Dibujo para los gráficos en Java2D). Sin reglas de negocio ni SQL.
├── service/       PartidaService (alta/baja/modificación/consulta), JuegoService (reglas), GeneradorDia (clientes y tareas de cada día) y Catalogo (objetos). Lanzan las excepciones.
├── dao/           EntidadDAO / JugadorDAO / Conexion: CRUD sobre MySQL.
├── modelo/        Mundo → Personaje → Jugador, Cliente, NPC; Objeto, Tarea, Inventario (las manos), Sala y Lugar.
├── interfaces/    Interactuable
├── excepciones/   Las 5 excepciones del juego + PersistenciaException (fallas de base de datos)
└── main/          Main: abre la ventana
referencia-consola/  Main y JuegoService originales de la versión de consola (solo como referencia, no se compilan)
```

Flujo: Usuario → UI → Service → DAO → MySQL, y las excepciones viajan de vuelta del Service a la UI, que las muestra en un cuadro de diálogo.

## Capturas

Ver carpeta [`docs/`](docs/): ventana principal, un registro exitoso y un cuadro de error de excepción.
