# Gestor de Ejercicios de Programacion con BaseX

Aplicacion Java de consola para gestionar ejercicios de programacion almacenados en una base de datos XML nativa (BaseX embebido).

## Objetivo

Cumplir la actividad evaluable de Acceso a Datos:

1. Conexion local a BaseX.
2. Insercion de ejercicios.
3. Modificacion de enunciado o dificultad.
4. Eliminacion por ID.
5. Consultas XQuery por dificultad y por palabra clave.
6. Persistencia de informacion y manejo de excepciones.

## Arquitectura de la solucion

- Lenguaje: Java.
- Motor XML: BaseX embebido (`org.basex.core.*`).
- Entrada/salida: consola.
- Estructura en 2 clases:
  - `GestorEjercicios`: menu e interaccion por consola.
  - `GestorEjerciciosDAO`: acceso a datos y operaciones XQuery.
- Persistencia: base de datos BaseX llamada `ejercicios`.
- Persistencia: exportacion del contenido a `data/ejercicios.xml` al cerrar.

## Que significa "modo local" en esta entrega

- En esta solucion, "modo local" se implementa como **BaseX embebido** dentro de la aplicacion Java.
- No se usa conexion cliente-servidor (`Session("localhost", 1984, ...)`), por lo que no hace falta arrancar un servidor BaseX aparte.
- La interaccion con BaseX se realiza mediante su API embebida: `Context`, `Open`, `CreateDB`, `XQuery`, `Close`.
- Usar una interfaz de consola para pedir datos al usuario es correcto: la consola es la UI y BaseX es el motor de BD que ejecuta las operaciones.

## Estructura de datos XML

Cada ejercicio se almacena como:

```xml
<ejercicio id="N">
  <titulo>...</titulo>
  <enunciado>...</enunciado>
  <dificultad>...</dificultad>
  <etiquetas>
    <etiqueta>...</etiqueta>
  </etiquetas>
</ejercicio>
```

Raiz del documento:

```xml
<ejercicios>
  ...
</ejercicios>
```

## Flujo funcional

1. Arranque.
2. Creacion de carpeta `data` si no existe.
3. Apertura de BD `ejercicios` si ya existe.
4. Si no existe, creacion de `data/ejercicios.xml` con `<ejercicios/>`.
5. Si no existe, creacion de BD desde ese archivo.
6. Insercion con ID autoincremental por XQuery.
7. Modificacion de `enunciado` o `dificultad` por ID.
8. Eliminacion por ID.
9. Consulta por dificultad.
10. Consulta por palabra clave en enunciado.
11. Exportacion de la BD a `data/` al cerrar.
12. Cierre de BD y del contexto BaseX.

## Decisiones tecnicas

- BaseX embebido para simplificar despliegue y ajustarse al enfoque de la plantilla.
- La expresion "interactuar con BaseX" se cumple porque todas las operaciones de datos se ejecutan con comandos/API de BaseX desde Java.
- ID autoincremental para evitar colisiones manuales.
- Escapado XML/XQuery para evitar errores por caracteres especiales.
- Manejo de excepciones por operacion con mensajes claros.

## Compilacion y ejecucion

1. Asegura tener el/los `.jar` de BaseX en el classpath.
2. Compila `src/GestorEjercicios.java` y `src/GestorEjerciciosDAO.java`.
3. Ejecuta la clase principal `GestorEjercicios`.

Ejemplo orientativo en PowerShell (adaptar rutas de jar):

```powershell
javac -cp ".;ruta\basex.jar" src\*.java
java -cp ".;ruta\basex.jar;src" GestorEjercicios
```

## Limitaciones actuales

- No incluye validacion cerrada de dificultad.
- No permite modificar etiquetas ni titulo.
- No incluye consultas extra fuera del enunciado.

## Mejoras futuras

1. Validar dificultad con valores fijos (`Facil`, `Media`, `Dificil`).
2. Permitir modificar titulo y etiquetas.
3. Anadir consultas extra (por etiqueta, listado ordenado, etc.).
4. Separar capa de acceso a datos y capa de interfaz.
