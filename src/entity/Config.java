/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entity;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import logs.TipoLog;
import logs.UtilesLog;

public final class Config {
    Properties config = new Properties();
    InputStream configInput = null;
    String url;

    public Config(String url) {
        this.url = url;
        loadConfig();
    }
    
    public void loadConfig(){
        try{
            configInput = new FileInputStream(url);
            config.load(configInput);
            configInput.close();
        } catch(IOException e){
            
            UtilesLog.registrarInfo(Config.class, TipoLog.ERROR, "Error al cargar archivo de configuracion" + e);
        }
    }

    public void close(){
        try {
            configInput.close();
            
        } catch (IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    public Properties getConfig() {
        return config;
    }

    public void setConfig(Properties config) {
        this.config = config;
    }

    public InputStream getConfigInput() {
        return configInput;
    }

    public void setConfigInput(InputStream configInput) {
        this.configInput = configInput;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    
}