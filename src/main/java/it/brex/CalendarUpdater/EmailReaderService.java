package it.brex.CalendarUpdater;

import jakarta.mail.*;
import jakarta.mail.search.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import biweekly.component.VEvent;



public class EmailReaderService {
	
	/**
	 * Cerca tutte le mail inviate dalla mail di lavoro (carlo.bressan@siav.it)
	 * @return Lista di EmailDettaglio trovate
	 */
		
	public static void main(String[] args) {
		
		EmailReaderService service = new EmailReaderService();
		CalendarService calendar = new CalendarService();
		boolean bInserito = false;
		
		String stEventName = "";
		ZonedDateTime dataInizio;
		ZonedDateTime dataFine;
		
		// Data Formatter
		SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
		
		// Stringa di log
		String logs = "EmailReaderService > Main";
 
        try {
        	
        	System.err.println(logs + " > Inizio");
        	
        	// Data ricerca
    		Date dataRicerca = new Date();
        	// dataRicerca = dateFormatter.parse("26/10/2026");
        	
        	// Carica le configurazioni
        	AppConfig configFile = ConfigLoader.loadConfig();
            
        	// Stampa la configurazione
        	System.out.println("host: " + configFile.host());
        	System.out.println("port: " + configFile.port());
        	System.out.println("username: " + configFile.username());
        	System.out.println("password: " + configFile.password());
        	System.out.println("mittente: " + configFile.mittente());
        	System.out.println("oggetto: " + configFile.oggetto());
        	System.out.println("dataRicezione: " + dataRicerca);
        	System.out.println("pathCalendar: " + configFile.pathCalendar());
        	System.out.println("folderToMove: " + configFile.folderToMove());
        	
        	// Ricerca email da elaborare
            List<EmailDettaglio> emails = service.cercaEmail(
                    configFile.host(),
                    configFile.port(),
                    configFile.username(),
                    configFile.password(),
                    configFile.mittente(),  	// mittente (oppure null)
                    configFile.oggetto(),		// oggetto (oppure null)
                    dataRicerca						// data (oppure null)
            );

            System.out.println("Conta mail: " + emails.size());
            System.out.println("---------------------------------");
            
            for (EmailDettaglio email : emails) {
            	
            	System.out.println("---------------------------------");
                System.out.println("Oggetto: " + email.oggetto());
                System.out.println("Mittente: " + email.mittente());
                System.out.println("Data: " + email.dataRicezione());
                System.out.println("Corpo: " + email.corpo());
                System.out.println("---------------------------------");
                
                bInserito = false;
                
                // split del corpo mail
                 String[] stArr = email.corpo().split("\\|");
                 for (String label : stArr) {
                	 System.out.println(label);
                 }
                
                // Nome evento
                stEventName = stArr[0].substring(9).trim();
                System.out.println("Evento descrizione: " + stEventName);
                
                // Data inizio evento
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localInizio = LocalDateTime.parse(stArr[1].toString().substring(8).trim(), formatter);
                dataInizio = localInizio.atZone(ZoneId.of("Europe/Rome"));
                System.out.println("Evento inizio: " + stArr[1].toString());

                // Data fine evento
                LocalDateTime localFine = LocalDateTime.parse(stArr[2].toString().substring(6).trim(), formatter);
                dataFine = localFine.atZone(ZoneId.of("Europe/Rome"));
                System.out.println("Evento fine: " + stArr[2].toString());
                
                ZonedDateTime inizioRicerca = ZonedDateTime.now();
                ZonedDateTime fineRicerca = inizioRicerca.plusMonths(6);
                
                System.out.println("---------------------------------");
                System.out.println("pathCalendar: " + configFile.pathCalendar());
                System.out.println("username: " + configFile.username());
                System.out.println("password: " + configFile.password());
                System.out.println("stEventName: " + stEventName);
                System.out.println("inizio: " + inizioRicerca.toString());
                System.out.println("fine: " + fineRicerca.toString());
                System.out.println("---------------------------------");
                
                // Cerca un evento nel calendario
                List<EventoDettaglio> eventi = calendar.cercaEventi(
                		configFile,
                        stEventName,
                        inizioRicerca,
                        fineRicerca);
                
                // Modifica/Inserisce l'evento in calendario se non trovato
                for (EventoDettaglio evento : eventi) {
                	
                	System.out.println(
                			evento.getUid() + " - " + 
                			evento.getTitolo() + " - " + 
                			evento.getDescrizione() + " - " + 
                			evento.getDataOraInizio() + " - " + 
                			evento.getDataOraFine());
                	
                	// Modifica evento
                	evento.setTitolo(stEventName.trim());
                	evento.setDescrizione(stEventName.trim());
                	evento.setDataOraInizio(dataInizio);
                	evento.setDataOraFine(dataFine);
                	evento.setSequence(evento.getSequence() + 1);
                	
                	// Aggiorna evento esistente
                	bInserito = calendar.aggiornaEvento(configFile, evento, logs);
				}
                
                // Inserisce se è un evento nuovo
                if (eventi.isEmpty() && bInserito == false) {
                	
                	// Nuovo evento
                	EventoDettaglio evento = new EventoDettaglio(
                			null, 
                			stEventName.trim(), 
                			stEventName.trim(), 
                			"Siav", 
                			dataInizio, 
                			dataFine,
                			1,
                			null);
                	
                	// Inserimento a calendario
                	bInserito = calendar.aggiornaEvento(configFile, evento, logs);
					/*
					 * if (calendar.aggiornaEvento(configFile, evento)) {
					 * System.out.println("Evento " + evento.getTitolo() +
					 * "inserito correttamente."); }
					 */
				}
                
                // Email imposta come letta e sposta in cartella lavorati
                if (bInserito) {
                	service.segnaEmailComeLettaESposta(configFile, email.msg(), logs);
				}
                
                /***************************************************
                TODO
            	- pensare di inserire come eventi anche appuntamenti miei personali legati 
            	   al lavoro in modo che mi compaiano nel calendario usando la stessa logica
            	   degli eventi pianificati e gestiti qui
                ***************************************************/
                
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        System.err.println(logs + " > Fine");
        
	}

	/**
     * Cerca ed estrae le email da una casella di posta IMAP in base ai filtri specificati.
     * 
     * @param host Server IMAP (es. "imap.gmail.com")
     * @param port Porta IMAP (es. "993" per SSL)
     * @param username Email / Username dell'account
     * @param password Password o App Password dell'account
     * @param mittente Filtro mittente (può essere null o vuoto se non richiesto)
     * @param dataRicezione Filtro data di ricezione (confronto sul giorno; può essere null)
     * @param oggetto Filtro oggetto (ricerca parziale/contiene; può essere null o vuoto)
     * @return Lista di EmailDettaglio trovate
     */
    public List<EmailDettaglio> cercaEmail(
            String host,
            String port,
            String username,
            String password,
            String mittente,
            String oggetto,
            Date dataRicezione) throws MessagingException, IOException {

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", port);
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;
        List<EmailDettaglio> emailTrovate = new ArrayList<>();

        try {
            store = session.getStore("imaps");
            store.connect(host, username, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Costruzione dinamica dei filtri di ricerca
            List<SearchTerm> filtri = new ArrayList<>();

            if (mittente != null && !mittente.isBlank()) {
                filtri.add(new FromStringTerm(mittente));
            }

            if (oggetto != null && !oggetto.isBlank()) {
                filtri.add(new SubjectTerm(oggetto));
            }

            if (dataRicezione != null) {
                // ComparisonTerm.EQ effettua il confronto ignorando l'orario (confronta solo il giorno)
                filtri.add(new ReceivedDateTerm(ComparisonTerm.EQ, dataRicezione));
            }

            Message[] messaggi;

            if (!filtri.isEmpty()) {
                // Unisce tutti i filtri attivi in AND
                SearchTerm queryRicerca = new AndTerm(filtri.toArray(new SearchTerm[0]));
                messaggi = inbox.search(queryRicerca);
            } else {
                // Nessun filtro specificato: recupera tutti i messaggi
                messaggi = inbox.getMessages();
            }
            
            // Ordina l'array per data di ricezione (dal più vecchio al più recente)
            Arrays.sort(messaggi, new Comparator<Message>() {
                @Override
                public int compare(Message m1, Message m2) {
                    try {
                        Date date1 = m1.getReceivedDate();
                        Date date2 = m2.getReceivedDate();
                        
                        // Fallback sulla data di invio se la data di ricezione è null
                        if (date1 == null) date1 = m1.getSentDate();
                        if (date2 == null) date2 = m2.getSentDate();
                        
                        // Gestione di sicurezza per eventuali date ancora null
                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;  // i null vanno alla fine
                        if (date2 == null) return -1;
                        
                        // .compareTo() per ordine crescente (dal più vecchio al più recente)
                        return date1.compareTo(date2);
                        
                    } catch (MessagingException e) {
                        // Gestione dell'eccezione nel comparatore
                        e.printStackTrace();
                        return 0;
                    }
                }
            });

            // Legge ed estrae le informazioni dalle mail filtrate
            for (Message msg : messaggi) {
                String mittenteStr = msg.getFrom() != null && msg.getFrom().length > 0 
                        ? msg.getFrom()[0].toString() 
                        : "Sconosciuto";
                
                String oggettoStr = msg.getSubject() != null ? msg.getSubject() : "(Nessun oggetto)";
                Date dataRicezioneDate = msg.getReceivedDate() != null ? msg.getReceivedDate() : msg.getSentDate();
                
                // ESTRAZIONE DEL CORPO DELL'EMAIL
                String corpo = estraiTestoEmail(msg);

                emailTrovate.add(new EmailDettaglio(mittenteStr, oggettoStr, dataRicezioneDate, corpo, msg));
            }

        } finally {
            // Chiusura pulita delle risorse
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }

        return emailTrovate;
    }
    
    
    /**
     * Estrae ricorsivamente il testo dal messaggio o da una sua parte (Part).
     */
    private String estraiTestoEmail(Part part) throws MessagingException, IOException {
    	
        // 1. Se la parte è testo semplice, la restituisce direttamente
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }

        // 2. Se è HTML, la restituisce (puoi usare librerie come Jsoup per rimuovere i tag HTML se preferisci)
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }

        // 3. Se la struttura è multipart/alternative (contiene sia la versione testo che HTML)
        if (part.isMimeType("multipart/alternative")) {
            Multipart mp = (Multipart) part.getContent();
            String testoHtml = null;

            for (int i = 0; i < mp.getCount(); i++) {
                Part bodyPart = mp.getBodyPart(i);
                // Diamo la priorità al testo semplice
                if (bodyPart.isMimeType("text/plain")) {
                    return (String) bodyPart.getContent();
                } else if (bodyPart.isMimeType("text/html")) {
                    testoHtml = (String) bodyPart.getContent();
                }
            }
            return testoHtml != null ? testoHtml : "";
        }

        // 4. Se è un generico multipart (es. multipart/mixed con allegati)
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            StringBuilder testoCompleto = new StringBuilder();

            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bodyPart = mp.getBodyPart(i);

                // Ignoriamo gli allegati scaricabili per concentrarci solo sul corpo del testo
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    continue;
                }

                String testoParte = estraiTestoEmail(bodyPart);
                if (testoParte != null && !testoParte.isBlank()) {
                    testoCompleto.append(testoParte).append("\n");
                }
            }
            return testoCompleto.toString().trim();
        }

        return "";
    }
    
    
    /**
     * Segna un messaggio come letto e lo sposta in una cartella di destinazione.
     * 
     * @param configFile
     * @param message
     * @param logs
     * @return
     * @throws MessagingException
     */
    public boolean segnaEmailComeLettaESposta(
            AppConfig configFile,
            Message message,
            String logs) throws MessagingException {
        
        boolean ret = false;
        
        logs += " > segnaEmailComeLettaESposta";
        System.out.println(logs + " > Inizio");
        
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", configFile.host());
        props.put("mail.imaps.port", configFile.port());
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = null;
        
        // Dichiariamo le cartelle fuori dal try per poterle chiudere nel finally se necessario
        Folder currentFolder = null;
        Folder targetFolder = null;
        
        try {
            store = session.getStore("imaps");
            store.connect(
                    configFile.host(), 
                    configFile.username(), 
                    configFile.password());
            
            // 1. NON usare message.getFolder() direttamente se appartiene a un'altra sessione/store.
            // Ottieniamo invece il nome della cartella d'origine dal vecchio messaggio:
            String originalFolderName = message.getFolder().getFullName();
            
            // Apriamo la cartella di origine sul NUOVO store
            currentFolder = store.getFolder(originalFolderName);
            if (!currentFolder.isOpen()) {
                currentFolder.open(Folder.READ_WRITE);
            }
            
            // 2. Poiché il numero di messaggio (MessageNumber) potrebbe cambiare tra una connessione e l'altra,
            // il modo più sicuro per trovare lo stesso identico messaggio in questa nuova connessione 
            // è usare l'UID (se il server supporta IMAP UID, come quasi tutti) oppure il Message-ID.
            // Se UIDFolder è supportato (standard IMAP moderno):
            Message targetMessage = null;
            if (currentFolder instanceof jakarta.mail.UIDFolder) {
                jakarta.mail.UIDFolder uf = (jakarta.mail.UIDFolder) currentFolder;
                // Nota: per fare questo, nel metodo che ha letto la mail devi salvarti l'UID con uf.getUID(message)
                // Se non hai l'UID, ripieghiamo sul messageNumber ammesso che la cartella sia la stessa istanza.
            }
            
            // Soluzione pragmatica se la cartella viene aperta contestualmente: 
            // Se il messageNumber è > نيك0, prenderlo direttamente dalla cartella appena aperta di questo store:
            int msgNum = message.getMessageNumber();
            if (msgNum <= 0 || msgNum > currentFolder.getMessageCount()) {
                throw new IllegalArgumentException("Numero messaggio non valido nella nuova sessione IMAP.");
            }
            Message freshMessage = currentFolder.getMessage(msgNum);
            
            // 3. Segna la mail come letta sul messaggio "fresco" legato a questo store
            freshMessage.setFlag(Flags.Flag.SEEN, true);
            System.out.println("Messaggio segnato come letto.");

            // 4. Gestione cartella di destinazione
            targetFolder = store.getFolder(configFile.folderToMove());
            if (!targetFolder.exists()) {
                System.err.println("Cartella non trovata: " + configFile.folderToMove());
            }
            
            if (!targetFolder.isOpen()) {
                targetFolder.open(Folder.READ_WRITE);
            }

            // 5. Copia il messaggio
            currentFolder.copyMessages(new Message[]{ freshMessage }, targetFolder);
            System.out.println("Messaggio copiato nella cartella: " + configFile.folderToMove());

            // 6. Segna come eliminato
            freshMessage.setFlag(Flags.Flag.DELETED, true);
            
            // Chiusura pulita cartella destinazione
            targetFolder.close(false);
            targetFolder = null; // Evitiamo doppi rilasci nel finally

            // Espunge i messaggi eliminati dalla cartella corrente
            currentFolder.expunge();
            
            ret = true;
        } 
        catch (Exception ex) {
            System.out.println("ERRORE: " + ex.getMessage());
            ex.printStackTrace(); // Utile per debuggare eventuali stacktrace completi
        }
        finally {
            // Pulizia sicura delle risorse per evitare leak di connessioni
            try {
                if (currentFolder != null && currentFolder.isOpen()) {
                    currentFolder.close(false);
                }
            } catch (Exception e) { /* ignora */ }
            
            try {
                if (targetFolder != null && targetFolder.isOpen()) {
                    targetFolder.close(false);
                }
            } catch (Exception e) { /* ignora */ }

            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (Exception e) { /* ignora */ }
            }	    }
        
        System.out.println(logs + " > Fine");
        return ret;
    }
    
    
    /**
     * Segna un messaggio come letto e lo sposta in una cartella di destinazione.
     *
     * @param message          Il messaggio trovato da elaborare
     * @param currentFolder    La cartella corrente in cui si trova il messaggio (deve essere aperta in READ_WRITE)
     * @param store            Il Mail Store attivo (necessario per accedere alla cartella di destinazione)
     * @param targetFolderName Il nome della cartella di destinazione (es. "Archivio" o "INBOX.Archivio")
     * @throws MessagingException In caso di errori con il server di posta
     */
    public boolean segnaEmailComeLettaESposta_OLD(
    		AppConfig configFile,
    		Message message,
    		String logs) throws MessagingException {
    	
    	boolean ret = false;
    	
    	logs += " > segnaEmailComeLettaESposta";
    	System.out.println(logs + " > Inizio");
    	
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", configFile.host());
        props.put("mail.imaps.port", configFile.port());
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = null;
        
        try {
        
        	// Store
	        store = session.getStore("imaps");
	        store.connect(
	        		configFile.host(), 
	        		configFile.username(), 
	        		configFile.password());
	        
	        // Recupera folder corrente della mail
	        Folder currentFolder = message.getFolder();
	        
	        // Controllo se la cartella è aperta
	        if (!currentFolder.isOpen() && message.getMessageNumber() > 0) {
	            currentFolder.open(Folder.READ_WRITE);
	        }
	    	
	        // Segna la mail come letta
	        message.setFlag(Flags.Flag.SEEN, true);
	        System.out.println("Messaggio segnato come letto.");
	
	        // Ottieni e apri la cartella di destinazione in modalità lettura/scrittura
	        Folder targetFolder = store.getFolder(configFile.folderToMove());
	        if (!targetFolder.exists()) {
	            // Se la cartella non esiste, puoi decidere se crearla o lanciare un'eccezione
	            // targetFolder.create(Folder.HOLDS_MESSAGES);
	        	System.err.println("Cartella non trovata: " + configFile.folderToMove());
	        }
	        
	        // La cartella di destinazione deve essere aperta per poter ricevere messaggi
	        if (!targetFolder.isOpen()) {
	            targetFolder.open(Folder.READ_WRITE);
	        }
	
	        // Copia il messaggio nella cartella di destinazione
	        // Nota: copyMessages accetta un array di messaggi
	        currentFolder.copyMessages(new Message[]{ message }, targetFolder);
	        System.out.println("Messaggio copiato nella cartella: " + configFile.folderToMove());
	
	        // Segna il messaggio originale come eliminato nella cartella di origine
	        message.setFlag(Flags.Flag.DELETED, true);
	        
	        // Chiudiamo la cartella di destinazione (senza bisogno di expunge lì dentro)
	        targetFolder.close(false);
	
	        // Nota importante: Per completare lo spostamento (eliminazione fisica del messaggio originale),
	        // la cartella corrente deve applicare l'expunge. Se chiudi la cartella corrente con close(true), 
	        // i messaggi marcati come DELETED verranno eliminati definitivamente.
	        // Esempio (se gestisci tu la chiusura):
	        // currentFolder.close(true); 
	        // oppure se la cartella deve rimanere aperta, puoi chiamare:
	        currentFolder.expunge();
	        
	        ret = true;
        
	    } 
        catch (Exception ex) {
			// Gestione eccezioni
        	System.out.println("ERRORE: " + ex.getMessage());
		}
        finally {
	        // Chiusura pulita delle risorse
	        if (store != null && store.isConnected()) {
	            store.close();
	        }	        
	    }
        
        System.out.println(logs + " > Fine");
        return ret;
    }    
    
}