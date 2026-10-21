package it.brex.CalendarUpdater;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ConfigLoader {

    // Percorso relativo: cercherà il file nella stessa cartella da cui viene lanciato il JAR
    private static final String CONFIG_FILE_PATH = "config.json";

    public static AppConfig loadConfig() throws IOException {
        File configFile = new File(CONFIG_FILE_PATH);

        // Verifica che il file esista
        if (!configFile.exists()) {
            throw new IOException("File di configurazione non trovato in: " + configFile.getAbsolutePath());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        
        // Legge il file JSON e lo converte nell'oggetto AppConfig
        return objectMapper.readValue(configFile, AppConfig.class);
    }
}