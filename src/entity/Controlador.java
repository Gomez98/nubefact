package entity;

import java.io.File;
import java.io.IOException;
import logs.TipoLog;
import logs.UtilesLog;
import pruebajavafetxt.ConsumingPost;

public class Controlador {

    private Config config;
    private File carpetaFac;
    private String[] listadoFac;
    private final Long tempo;
    private final ConsumingPost cp;

    public Controlador() {
        config = new Config("src\\configuracion\\config.properties");
        carpetaFac = new File(config.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\PorEnviar");
        listadoFac = carpetaFac.list();
        tempo = Long.valueOf(config.getConfig().getProperty("TEMPORIZADOR"));
        cp = new ConsumingPost();
    }

    public void ini() {
        System.setProperty("log4j.configurationFile", "src\\configuracion\\log4j2.properties");
        
        if (listadoFac.length == 0) {
            try {
                caso1();
            } catch (InterruptedException | IOException ex) {
                UtilesLog.registrarInfo(Controlador.class, TipoLog.ERROR, ex.toString());

            }
        } else {
            try {
                caso2();
            } catch (IOException | InterruptedException ex) {
                UtilesLog.registrarInfo(Controlador.class, TipoLog.ERROR, ex.toString());
            }
        }
    }

    public void caso1() throws InterruptedException, IOException {

        while (listadoFac.length == 0) {

            Thread.sleep(tempo);

            //Volver a recorrer la carpeta
            carpetaFac = new File(config.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\PorEnviar");
            listadoFac = carpetaFac.list();

            if (listadoFac.length > 0) {
                cp.apiConsume(listadoFac);

                //Volver a recorrer la carpeta
                carpetaFac = new File(config.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\PorEnviar");
                listadoFac = carpetaFac.list();

            }
        }
    }

    public void caso2() throws IOException, InterruptedException {
        while (listadoFac.length != 0) {
            cp.apiConsume(listadoFac);

            //Volver a recorrer la carpeta
            carpetaFac = new File(config.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\PorEnviar");
            listadoFac = carpetaFac.list();

            while (listadoFac.length == 0) {
            
                Thread.sleep(tempo);
                carpetaFac = new File(config.getConfig().getProperty("RUTA_PROGRAMA") + "EDI\\PorEnviar");
                listadoFac = carpetaFac.list();

            }
        }
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public File getCarpetaFac() {
        return carpetaFac;
    }

    public void setCarpetaFac(File carpetaFac) {
        this.carpetaFac = carpetaFac;
    }

    public String[] getListadoFac() {
        return listadoFac;
    }

    public void setListadoFac(String[] listadoFac) {
        this.listadoFac = listadoFac;
    }

}
