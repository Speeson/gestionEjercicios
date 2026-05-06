# README_USUARIO - Guia de defensa y explicacion

Este documento te sirve para defender la actividad explicando que se hizo, por que se hizo asi y como demostrarlo en vivo.

## 1. Resumen rapido para la defensa

Frase recomendada:

> He desarrollado un gestor de ejercicios en Java con BaseX embebido, que realiza altas, modificaciones, bajas y consultas sobre una base de datos XML persistente, usando XQuery para todas las operaciones.

## 2. Que problema resuelve

- Gestionar ejercicios de programacion en XML.
- Persistir cambios entre ejecuciones.
- Ejecutar operaciones CRUD (crear, leer, actualizar, borrar).

## 3. Justificacion de decisiones

1. BaseX embebido.
2. Ajuste al enfoque de la plantilla.
3. Menor dependencia de servicios externos para practica local.
4. Persistencia XML mediante exportacion a `data/ejercicios.xml`.
5. ID autoincremental para reducir errores manuales.
6. Uso de XQuery en todas las operaciones para cumplir el enunciado.

## 3.1 Como interpretar "modo local" (pregunta habitual)

- "Modo local" puede hacerse de dos formas:
1. Embebido (`org.basex.core.*`) dentro de Java.
2. Cliente-servidor (`org.basex.api.client.*`) conectando a `localhost:1984`.
- En esta entrega se usa la opcion embebida, que tambien es local y valida.
- Ventaja para practica y defensa: menos configuracion, no depende de levantar BaseX Server.
- Aunque la UI sea consola, la BD se manipula con BaseX porque las operaciones se lanzan con `Open/CreateDB/XQuery/Close`.

## 4. Que hace cada metodo principal

- Clase `GestorEjercicios` (presentacion):
- `main()`: arranca la app, abre BD y muestra menu.
- `ejecutarMenu(...)`: flujo principal de opciones.
- `insertarEjercicio(...)`, `modificarEjercicio(...)`, `eliminarEjercicio(...)`, `consultarEjercicios(...)`: recogen datos por consola y delegan en el DAO.

- Clase `GestorEjerciciosDAO` (acceso a datos BaseX):
- `abrirBD()`: abre BD existente o la crea en local desde `data/ejercicios.xml`.
- `insertarEjercicio(...)`: calcula ID autoincremental e inserta nodo con XQuery Update.
- `modificarEnunciado(...)` y `modificarDificultad(...)`: actualizan por ID.
- `eliminarEjercicio(...)`: elimina por ID.
- `consultarPorDificultad(...)` y `consultarPorPalabraClave(...)`: consultas obligatorias del enunciado.
- `close()`: exporta XML y cierra recursos.

## 5. Demostracion recomendada (guion)

1. Ejecutar app y mostrar menu.
2. Insertar dos ejercicios.
3. Consultar por dificultad.
4. Modificar dificultad de un ID.
5. Consultar por palabra clave.
6. Eliminar un ID.
7. Cerrar app.
8. Abrir `data/ejercicios.xml` para enseñar persistencia.

## 6. Preguntas tipicas y respuesta corta

1. Por que BaseX y no relacional?
Porque el enunciado exige BD nativa XML y XQuery.

2. Si el enunciado dice "modo local", por que no usas Session con localhost?
Porque en local tambien es valido el modo embebido. Es incluso mas simple para practica y cumple el requisito de interactuar con BaseX desde Java.

3. Como garantizas persistencia?
Se exporta el estado final de la BD a `data/ejercicios.xml`.

4. Como evitas IDs repetidos?
Con autoincremento calculado por XQuery.

5. Que pasa si falla una consulta?
Se captura excepcion y se informa al usuario.

## 7. Riesgos conocidos y defensa

- Riesgo: dificultad con texto libre.
Defensa: cumple requerimiento actual; validacion cerrada propuesta como mejora.

- Riesgo: no hay consultas avanzadas.
Defensa: estan implementadas exactamente las consultas obligatorias.

## 8. Estado de cumplimiento

- Conexion con BaseX local: SI.
- Insertar ejercicio: SI.
- Modificar enunciado/dificultad: SI.
- Eliminar por ID: SI.
- Consultar por dificultad y palabra clave: SI.
- Manejo de errores: SI.
- Persistencia XML: SI.

## 9. Cierre para la defensa

> La solucion cumple los requisitos obligatorios del enunciado, aplica XQuery sobre una BD XML nativa y deja la informacion persistida para reutilizacion en ejecuciones posteriores.
