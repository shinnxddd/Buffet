CREATE DATABASE IF NOT EXISTS buffet_terror;
USE buffet_terror;

CREATE TABLE entidades (
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
