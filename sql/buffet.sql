CREATE DATABASE IF NOT EXISTS buffet
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE buffet;

CREATE TABLE IF NOT EXISTS entidades (
    id                 INT PRIMARY KEY AUTO_INCREMENT,
    nombre             VARCHAR(50)  NOT NULL,
    tipo               VARCHAR(30)  NOT NULL,
    dia_actual         INT,
    tareas_completadas INT,
    dinero             INT,
    dialogo            VARCHAR(255),
    rol                VARCHAR(50),
    pedido             VARCHAR(100),
    atendido           BOOLEAN,
    tipo_objeto        VARCHAR(50),
    utilizable         BOOLEAN,
    cantidad           INT,
    descripcion        VARCHAR(255),
    tipo_tarea         VARCHAR(50),
    completada         BOOLEAN,
    dia                INT
);


