CREATE DATABASE IF NOT EXISTS buffet_terror;
USE buffet_terror;

CREATE TABLE IF NOT EXISTS entidades (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    dia_actual INT,
    cordura INT,
    tareas_completadas INT,
    rol VARCHAR(50),
    dialogo VARCHAR(255),
    puede_dar_tarea BOOLEAN,
    tipo_objeto VARCHAR(50),
    utilizable BOOLEAN,
    pedido VARCHAR(100),
    atendido BOOLEAN
);

-- Dato de ejemplo: una partida para que la tabla de la app no arranque vacía.
INSERT INTO entidades (nombre, tipo, dia_actual, cordura, tareas_completadas)
SELECT 'Octavio', 'JUGADOR', 1, 100, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM entidades WHERE nombre = 'Octavio' AND tipo = 'JUGADOR');
