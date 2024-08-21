package pruebajavafetxt;

import entity.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.ws.rs.core.HttpHeaders;
import logs.TipoLog;
import logs.UtilesLog;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.*;

public class ConsumingPost {

    //Cargando el archivo de configuracion
    Config conf = new Config("src\\configuracion\\config.properties");

    //RUTA para enviar documentos 
    private final String RUTA = conf.getConfig().getProperty("RUTA");

    //TOKEN para enviar documentos
    private final String TOKEN = conf.getConfig().getProperty("TOKEN");

    public ConsumingPost() {

    }

    public void apiConsume(String[] listadoFac) throws IOException {

        HttpPost post = new HttpPost(RUTA);
        post.addHeader("Authorization", TOKEN);
        //Rutas de facturas
        String dirFac = "EDI\\PorEnviar\\";
        String respuesta = "EDI\\Respuesta\\";
        String errores = "EDI\\Errores\\";
        String enviadoFac = conf.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\Enviado";
        String porEnviarFac = conf.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\PorEnviar";
        String carpetaDOCFac = conf.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\Documentos";

        //Recorridos
        try {
            recorridoFAC(listadoFac, post, dirFac, respuesta, enviadoFac, porEnviarFac, carpetaDOCFac, errores);

        } catch (IOException ex) {
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR, "Error en la ejecución: " + ex.getMessage());
        }
    }

    private void recorridoFAC(String[] lista, HttpPost post,
            String directorio, String respuesta, String enviado, String porEnviar, String carpetaDOC, String errores) throws IOException, IllegalStateException {

        for (String nombreArchivo : lista) {
            try {
                if (nombreArchivo.contains(".json")) {
                    try {
                        post.addHeader("Content-Type", "application/json");

                        if (this.fileToJsonObject(directorio + nombreArchivo).contains("consultar_guia")) {
                            post.setEntity(new StringEntity(this.fileToJsonObject(directorio + nombreArchivo), "UTF-8"));
                            try ( CloseableHttpClient cliente = HttpClients.createDefault();  CloseableHttpResponse response = cliente.execute(post);  BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {

                                StringBuilder sb = new StringBuilder();
                                String str;
                                
                                while ((str = rd.readLine()) != null) {
                                    sb.append(str);
                                }   
                                
                                JSONObject obj = new JSONObject(sb.toString());
                                String enlacePDF = obj.getString("enlace_del_pdf");
                                if (!enlacePDF.isBlank()) {
                                    File file = new File(
                                            carpetaDOC + "\\" + nombreArchivo.substring(0, nombreArchivo.length() - 5) + ".pdf");
                                    descargarURL(enlacePDF, file);
                                } else {
                                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.WARNING,
                                            "No se encontró enlace_del_pdf");
                                }
                                String enlaceXML = obj.getString("enlace_del_xml");
                                if (!enlacePDF.isBlank()) {
                                    File file = new File(
                                            carpetaDOC + "\\" + nombreArchivo.substring(0, nombreArchivo.length() - 5) + ".xml");
                                    descargarURL(enlaceXML, file);

                                } else {
                                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.WARNING,
                                            "No se encontró enlace_del_xml");
                                }
                                String enlaceCDR = obj.getString("enlace_del_cdr");
                                if (!enlacePDF.isBlank()) {
                                    File file = new File(
                                            carpetaDOC + "\\" + nombreArchivo.substring(0, nombreArchivo.length() - 5) + ".cdr");
                                    descargarURL(enlaceCDR, file);
                                } else {
                                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.WARNING,
                                            "No se encontró enlace_del_cdr");
                                }
                                enviarAEnviados(porEnviar, nombreArchivo, enviado);

                            } catch (IOException ex) {
                                UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                                        "Error al ejecutar la consulta de guía: " + ex.getMessage());
                                enviarAErrores(porEnviar, errores, nombreArchivo);
                            }
                        } else {
                            post.setEntity(new StringEntity(this.fileToJsonObject(directorio + nombreArchivo), "UTF-8"));
                            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.INFO, "Archivo  " + nombreArchivo + " enviado a nubefact");

                            try ( CloseableHttpClient cliente = HttpClients.createDefault();  CloseableHttpResponse response = cliente.execute(post);  BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {

                                StringBuilder sb = new StringBuilder();
                                String str;
                                while ((str = rd.readLine()) != null) {
                                    sb.append(str);
                                }                            

                                JSONObject obj = new JSONObject(sb.toString());
                                if (response.getStatusLine().getStatusCode() == 200) {
                                    enviarAEnviados(porEnviar, nombreArchivo, enviado);
                                } else {
                                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                                            "Código de respuesta: " + response.getStatusLine().getStatusCode());
                                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                                            "Código nubefact: " + obj.getInt("codigo"));
                                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                                            "Mensaje nubefact: " + obj.getString("errors"));
                                    enviarAErrores(porEnviar, errores, nombreArchivo);
                                }

                            } catch (IOException ex) {
                                UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                                        "Error al ejecutar la solicitud HTTP: " + ex.getMessage());
                                enviarAErrores(porEnviar, errores, nombreArchivo);

                            }
                        }
                    } catch (JSONException ex) {
                        UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR, "Error al manejar el JSON: " + ex.getMessage());
                        enviarAErrores(porEnviar, errores, nombreArchivo);
                    }
                } else {
                    post.addHeader("Content-Type", "text/plain");
                    StringEntity parametros = new StringEntity(this.fileToText(directorio + nombreArchivo), "UTF-8");
                    post.setEntity(parametros);

                    UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.INFO, "Archivo  " + nombreArchivo + " enviado a nubefact");

                    try ( CloseableHttpClient cliente = HttpClients.createDefault();  CloseableHttpResponse response = cliente.execute(post);  BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {

                        String linea;
                        try ( FileWriter fw = new FileWriter(respuesta + nombreArchivo)) {
                            while ((linea = rd.readLine()) != null) {
                                if (linea.substring(0, 6).equalsIgnoreCase("errors")) {
                                    enviarAErrores(porEnviar, errores, nombreArchivo);
                                    break;
                                } else {
                                    descargarRutina(linea, carpetaDOC, nombreArchivo);
                                    fw.write(linea + "\n");
                                }
                            }
                        } catch (IOException ex) {
                            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                                    "Error al ejecutar la solicitud HTTP: " + ex.getMessage());
                            enviarAErrores(porEnviar, errores, nombreArchivo);
                        }
                        enviarAEnviados(porEnviar, nombreArchivo, enviado);

                    } catch (IOException ex) {
                        UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR, "Error al ejecutar la solicitud HTTP: " + ex.getMessage());
                        enviarAErrores(porEnviar, errores, nombreArchivo);
                    }
                }
            } catch (Exception ex) {
                UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR,
                        "Error en el archivo " + nombreArchivo + ": " + ex.getMessage());
                enviarAErrores(porEnviar, errores, nombreArchivo);
            }
        }
    }

    private String fileToJsonObject(String filename) throws IOException {
        StringBuilder builder = new StringBuilder();
        try ( BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String fileToText(String filename) throws IOException {
        StringBuilder builder = new StringBuilder();
        try ( BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
    }
    private void enviarAErrores(String porEnviar, String errores, String nombreArchivo) {
        //mover el archivo erroneo

        Path FROM = Paths.get(porEnviar + "\\" + nombreArchivo);
        Path TO = Paths.get(errores + "\\" + nombreArchivo);

        try {
            //mover el archivo
            Files.move(FROM, TO, StandardCopyOption.REPLACE_EXISTING);
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.INFO, "Archivo " + nombreArchivo + " erroneo se ha desplazado a Errores");
        } catch (IOException e) {
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.WARNING, "" + e);
        }
    }

    private void enviarAEnviados(String porEnviar, String nombreArchivo, String enviado) {

        Path FROM = Paths.get(porEnviar + "\\" + nombreArchivo);
        Path TO = Paths.get(enviado + "\\" + nombreArchivo);

        try {
            //mover el archivo
            Files.move(FROM, TO, StandardCopyOption.REPLACE_EXISTING);
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.INFO, "Archivo " + nombreArchivo + " se ha desplazado a Enviado");
        } catch (IOException e) {
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.WARNING, "" + e);
        }
    }

    private void descargarRutina(String linea, String carpetaDOC, String nombreArchivo) {
        if (linea.length() >= 15) {
            //obteniendo url
            if (linea.substring(0, 15).equals("enlace_del_pdf|")) {
                String url = "";
                url = linea.substring(15, linea.length() - 1);
                if (!url.isEmpty()) {
                    File file = new File(carpetaDOC + "\\" + nombreArchivo.substring(0, nombreArchivo.length() - 4) + ".pdf");
                    descargarURL(url, file);
                }
            }

            if (linea.substring(0, 15).equals("enlace_del_xml|")) {
                String url = "";
                url = linea.substring(15, linea.length() - 1);
                if (!url.isEmpty()) {
                    File file = new File(carpetaDOC + "\\" + nombreArchivo.substring(0, nombreArchivo.length() - 4) + ".xml");
                    descargarURL(url, file);
                }
            }

            if (linea.substring(0, 15).equals("enlace_del_cdr|")) {
                String url = "";
                url = linea.substring(15, linea.length() - 1);
                if (!url.isEmpty()) {
                    File file = new File(carpetaDOC + "\\" + nombreArchivo.substring(0, nombreArchivo.length() - 4) + ".cdr");
                    descargarURL(url, file);
                }
            }
        }
    }

    private void descargarURL(String url, File file) {
        //establecer conexion
        try {

            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            try ( InputStream in = conn.getInputStream();  OutputStream out = new FileOutputStream(file)) {
                int b = 0;
                //Leer y guardar bytes de archivo
                while (b != -1) {
                    b = in.read();
                    if (b != -1) {
                        out.write(b);
                    }
                }
            }

        } catch (MalformedURLException e) {
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.WARNING, "" + e);

        } catch (IOException e) {
            UtilesLog.registrarInfo(ConsumingPost.class, TipoLog.ERROR, "" + e);

        }
    }
}
