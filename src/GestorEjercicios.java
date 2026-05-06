import java.util.Scanner;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Capa de presentacion (consola).
 * Esta clase solo gestiona interaccion con el usuario y delega el acceso a datos 
 * en GestorEjerciciosDAO.
 */
public class GestorEjercicios {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        // Try-with-resources garantiza cierre correcto de la conexion embebida (DAO.close()).
        try (GestorEjerciciosDAO dao = new GestorEjerciciosDAO()) {
            dao.abrirBD();
            ejecutarMenu(dao);
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ejecutarMenu(GestorEjerciciosDAO dao) {
        // Menu principal alineado con los requisitos del enunciado (CRUD + consultas).
        boolean salir = false;
        while (!salir) {
            System.out.println("\n===== GESTOR DE EJERCICIOS =====");
            System.out.println("1. Insertar ejercicio");
            System.out.println("2. Modificar ejercicio");
            System.out.println("3. Eliminar ejercicio");
            System.out.println("4. Consultar ejercicios");
            System.out.println("0. Salir");
            System.out.print("Elige una opcion: ");

            int opcion = leerEntero();
            switch (opcion) {
                case 1:
                    insertarEjercicio(dao);
                    break;
                case 2:
                    modificarEjercicio(dao);
                    break;
                case 3:
                    eliminarEjercicio(dao);
                    break;
                case 4:
                    consultarEjercicios(dao);
                    break;
                case 0:
                    salir = true;
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        }
    }

    private static void insertarEjercicio(GestorEjerciciosDAO dao) {
        try {
            System.out.print("Titulo: ");
            String titulo = SCANNER.nextLine().trim();
            System.out.print("Enunciado: ");
            String enunciado = SCANNER.nextLine().trim();
            System.out.print("Dificultad (Facil, Media, Dificil): ");
            String dificultad = SCANNER.nextLine().trim();
            System.out.print("Etiquetas separadas por coma (ej: bucles,for): ");
            String etiquetasInput = SCANNER.nextLine().trim();

            int nuevoId = dao.insertarEjercicio(titulo, enunciado, dificultad, etiquetasInput);
            System.out.println("Ejercicio insertado con ID: " + nuevoId);
        } catch (Exception e) {
            System.err.println("No se pudo insertar el ejercicio: " + e.getMessage());
        }
    }

    private static void modificarEjercicio(GestorEjerciciosDAO dao) {
        try {
            System.out.print("ID del ejercicio a modificar: ");
            int id = leerEntero();

            if (!dao.existeEjercicio(id)) {
                System.out.println("No existe un ejercicio con ID " + id);
                return;
            }

            System.out.println("1. Modificar enunciado");
            System.out.println("2. Modificar dificultad");
            System.out.print("Elige opcion: ");
            int opcion = leerEntero();

            switch (opcion) {
                case 1:
                    System.out.print("Nuevo enunciado: ");
                    String enunciado = SCANNER.nextLine().trim();
                    dao.modificarEnunciado(id, enunciado);
                    System.out.println("Enunciado actualizado.");
                    break;
                case 2:
                    System.out.print("Nueva dificultad: ");
                    String dificultad = SCANNER.nextLine().trim();
                    dao.modificarDificultad(id, dificultad);
                    System.out.println("Dificultad actualizada.");
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        } catch (Exception e) {
            System.err.println("No se pudo modificar el ejercicio: " + e.getMessage());
        }
    }

    private static void eliminarEjercicio(GestorEjerciciosDAO dao) {
        try {
            System.out.print("ID del ejercicio a eliminar: ");
            int id = leerEntero();

            if (!dao.existeEjercicio(id)) {
                System.out.println("No existe un ejercicio con ID " + id);
                return;
            }

            dao.eliminarEjercicio(id);
            System.out.println("Ejercicio eliminado.");
        } catch (Exception e) {
            System.err.println("No se pudo eliminar el ejercicio: " + e.getMessage());
        }
    }

    private static void consultarEjercicios(GestorEjerciciosDAO dao) {
        try {
            System.out.println("1. Consultar por dificultad");
            System.out.println("2. Consultar por palabra clave en enunciado");
            System.out.print("Elige opcion: ");
            int opcion = leerEntero();

            String resultado;
            switch (opcion) {
                case 1:
                    System.out.print("Dificultad a filtrar: ");
                    String dificultad = SCANNER.nextLine().trim();
                    resultado = dao.consultarPorDificultad(dificultad);
                    break;
                case 2:
                    System.out.print("Palabra clave: ");
                    String palabra = SCANNER.nextLine().trim();
                    resultado = dao.consultarPorPalabraClave(palabra);
                    break;
                default:
                    System.out.println("Opcion no valida.");
                    return;
            }

            if (resultado == null || resultado.trim().isEmpty()) {
                System.out.println("No hay resultados.");
            } else {
                System.out.println("\nResultados:");
                // Presentacion amigable para defensa: muestra los datos como ficha y 
                // no como XML bruto.
                System.out.println(formatearResultadosComoLista(resultado));
            }
        } catch (Exception e) {
            System.err.println("No se pudo realizar la consulta: " + e.getMessage());
        }
    }

    private static int leerEntero() {
        while (true) {
            String linea = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.print("Introduce un numero valido: ");
            }
        }
    }

    private static String formatearResultadosComoLista(String xmlFragment) {
        String limpio = xmlFragment == null ? "" : xmlFragment.trim();
        if (limpio.isEmpty()) {
            return limpio;
        }

        try {
            // Se envuelve el fragmento para parsear multiples nodos <ejercicio> 
            // de forma segura.
            String envuelto = "<resultados>" + limpio + "</resultados>";
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(envuelto)));

            StringBuilder sb = new StringBuilder();
            NodeList ejercicios = document.getElementsByTagName("ejercicio");
            for (int i = 0; i < ejercicios.getLength(); i++) {
                Element ejercicio = (Element) ejercicios.item(i);
                String id = ejercicio.getAttribute("id");
                String titulo = getTextoHijo(ejercicio, "titulo");
                String enunciado = getTextoHijo(ejercicio, "enunciado");
                String dificultad = getTextoHijo(ejercicio, "dificultad");
                String etiquetas = getEtiquetas(ejercicio);

                sb.append("- Ejercicio ID: ").append(id).append(System.lineSeparator());
                sb.append("  - Titulo: ").append(titulo).append(System.lineSeparator());
                sb.append("  - Enunciado: ").append(enunciado).append(System.lineSeparator());
                sb.append("  - Dificultad: ").append(dificultad).append(System.lineSeparator());
                sb.append("  - Etiquetas: ").append(etiquetas).append(System.lineSeparator());

                if (i < ejercicios.getLength() - 1) {
                    sb.append(System.lineSeparator());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return xmlFragment;
        }
    }

    private static String getTextoHijo(Element parent, String tag) {
        NodeList lista = parent.getElementsByTagName(tag);
        if (lista.getLength() == 0) {
            return "";
        }
        return lista.item(0).getTextContent().trim();
    }

    private static String getEtiquetas(Element ejercicio) {
        NodeList etiquetas = ejercicio.getElementsByTagName("etiqueta");
        if (etiquetas.getLength() == 0) {
            return "(sin etiquetas)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etiquetas.getLength(); i++) {
            String valor = etiquetas.item(i).getTextContent().trim();
            if (!valor.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(valor);
            }
        }
        return sb.length() == 0 ? "(sin etiquetas)" : sb.toString();
    }
}
