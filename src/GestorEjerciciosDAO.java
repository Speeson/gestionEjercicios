import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.Export;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Capa de acceso a datos.
 * Implementa conexion local embebida a BaseX y operaciones XQuery/XQuery 
 * Update sobre la BD de ejercicios.
 */
public class GestorEjerciciosDAO implements AutoCloseable {

    private static final String DB_NAME = "ejercicios";
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path XML_PATH = DATA_DIR.resolve("ejercicios.xml");

    // BaseX embebido: el Context vive dentro de la aplicacion Java.
    // No se usa modo cliente-servidor (Session/localhost:1984).
    private final Context context;

    public GestorEjerciciosDAO() {
        // Crear el contexto equivale a inicializar la conexion local embebida.
        this.context = new Context();
    }

    public void abrirBD() throws BaseXException, IOException {
        // Garantiza que exista la carpeta fisica donde se exporta el XML persistente.
        Files.createDirectories(DATA_DIR);

        if (dbExiste(DB_NAME)) {
            // Comando BaseX ejecutado en memoria local (embebido).
            new Open(DB_NAME).execute(context);
            System.out.println("BD abierta: " + DB_NAME);
            return;
        }

        if (!Files.exists(XML_PATH)) {
            Files.writeString(XML_PATH, "<ejercicios/>");
        }

        // Si no existe, se crea la BD local desde el XML persistente.
        new CreateDB(DB_NAME, XML_PATH.toString()).execute(context);
        System.out.println("BD creada desde: " + XML_PATH.toAbsolutePath());
    }

    public int insertarEjercicio(String titulo, String enunciado, String dificultad, String etiquetasInput)
        throws BaseXException {

        // ID autoincremental calculado en BaseX para evitar colisiones manuales.
        int nuevoId = obtenerSiguienteId();
        String etiquetasXml = construirEtiquetasXml(etiquetasInput);

        String nodoNuevo = "<ejercicio id=\"" + nuevoId + "\">"
            + "<titulo>" + escaparXml(titulo) + "</titulo>"
            + "<enunciado>" + escaparXml(enunciado) + "</enunciado>"
            + "<dificultad>" + escaparXml(dificultad) + "</dificultad>"
            + "<etiquetas>" + etiquetasXml + "</etiquetas>"
            + "</ejercicio>";

        // XQuery Update ejecutado directamente sobre BaseX embebido.
        String xquery = "insert node " + nodoNuevo + " into /ejercicios";
        new XQuery(xquery).execute(context);
        return nuevoId;
    }

    public void modificarEnunciado(int id, String nuevoEnunciado) throws BaseXException {
        // Requisito del enunciado: modificar enunciado por ID.
        actualizarCampoTexto(id, "enunciado", nuevoEnunciado);
    }

    public void modificarDificultad(int id, String nuevaDificultad) throws BaseXException {
        // Requisito del enunciado: modificar dificultad por ID.
        actualizarCampoTexto(id, "dificultad", nuevaDificultad);
    }

    public void eliminarEjercicio(int id) throws BaseXException {
        String xquery = "delete node /ejercicios/ejercicio[@id='" + id + "']";
        new XQuery(xquery).execute(context);
    }

    public String consultarPorDificultad(String dificultad) throws BaseXException {
        // Consulta case-insensitive para evitar fallos por mayusculas/minusculas.
        String consulta = "for $e in /ejercicios/ejercicio "
            + "where lower-case(normalize-space($e/dificultad)) = lower-case('"
            + escaparXQuery(dificultad) + "') "
            + "return $e";
        return new XQuery(consulta).execute(context);
    }

    public String consultarPorPalabraClave(String palabra) throws BaseXException {
        // Consulta por coincidencia parcial en el enunciado.
        String consulta = "for $e in /ejercicios/ejercicio "
            + "where contains(lower-case($e/enunciado), lower-case('"
            + escaparXQuery(palabra) + "')) "
            + "return $e";
        return new XQuery(consulta).execute(context);
    }

    public boolean existeEjercicio(int id) throws BaseXException {
        String xquery = "exists(/ejercicios/ejercicio[@id='" + id + "'])";
        String resultado = new XQuery(xquery).execute(context).trim();
        return "true".equalsIgnoreCase(resultado);
    }

    @Override
    public void close() throws BaseXException {
        if (dbExiste(DB_NAME)) {
            // Exportamos a XML para dejar evidencia de persistencia tras usar BaseX embebido.
            new Open(DB_NAME).execute(context);
            new Export(DATA_DIR.toString()).execute(context);
            new Close().execute(context);
            try {
                formatearXmlPersistido();
            } catch (Exception e) {
                System.err.println("Aviso: no se pudo formatear el XML exportado: " + e.getMessage());
            }
            System.out.println("Cambios exportados a: " + XML_PATH.toAbsolutePath());
        }
        // Cierre de la conexion embebida.
        context.close();
    }

    private boolean dbExiste(String nombreBD) {
        try {
            new Open(nombreBD).execute(context);
            return true;
        } catch (BaseXException e) {
            return false;
        }
    }

    private int obtenerSiguienteId() throws BaseXException {
        String xquery = "let $ids := /ejercicios/ejercicio/@id ! xs:integer(.) "
            + "return (if (empty($ids)) then 1 else max($ids) + 1)";
        String resultado = new XQuery(xquery).execute(context).trim();
        return Integer.parseInt(resultado);
    }

    private void actualizarCampoTexto(int id, String campo, String valor) throws BaseXException {
        // Escapado simple para evitar romper el literal XQuery.
        String valorSeguro = escaparXQuery(valor);
        String xquery = "replace value of node /ejercicios/ejercicio[@id='" + id + "']/" + campo
            + " with '" + valorSeguro + "'";
        new XQuery(xquery).execute(context);
    }

    private String construirEtiquetasXml(String etiquetasInput) {
        if (etiquetasInput == null || etiquetasInput.trim().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String[] etiquetas = etiquetasInput.split(",");
        for (String etiqueta : etiquetas) {
            String limpia = etiqueta.trim();
            if (!limpia.isEmpty()) {
                sb.append("<etiqueta>").append(escaparXml(limpia)).append("</etiqueta>");
            }
        }
        return sb.toString();
    }

    private String escaparXQuery(String texto) {
        return texto.replace("'", "''");
    }

    private String escaparXml(String texto) {
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private void formatearXmlPersistido() throws Exception {
        // Formato legible del XML final para revision y defensa.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(XML_PATH.toFile());

        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setAttribute("indent-number", 4);
        } catch (IllegalArgumentException ignored) {
            // Some Transformer implementations do not support this attribute.
        }

        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(document), new StreamResult(XML_PATH.toFile()));
    }
}
