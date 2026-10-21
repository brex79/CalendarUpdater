package it.brex.CalendarUpdater;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Sequence;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CalendarService {

    private static final DateTimeFormatter CALDAV_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /**
     * Cerca eventi in un calendario CalDAV per finestra temporale e nome.
     *
     * @param calendarUrl URL della collezione del calendario (es. "https://caldav.example.com/calendars/user/personal/")
     * @param username Username per l'autenticazione HTTP Basic
     * @param password Password o App Token
     * @param nomeEvento Titolo o stringa da cercare nel sommario (può essere null/vuoto)
     * @param inizio Data/Ora di inizio della finestra di ricerca
     * @param fine Data/Ora di fine della finestra di ricerca
     * @return Lista di eventi trovati
     */
    public List<EventoDettaglio> cercaEventi(
    		AppConfig configFile,
            String nomeEvento,
            ZonedDateTime inizio,
            ZonedDateTime fine) throws Exception {

        // Formattazione date in UTC (formato ISO-8601 compatto es. 20261006T200000Z)
        String startUtc = inizio.toInstant().atZone(java.time.ZoneOffset.UTC).format(CALDAV_DATE_FORMATTER);
        String endUtc = fine.toInstant().atZone(java.time.ZoneOffset.UTC).format(CALDAV_DATE_FORMATTER);

        // Costruzione del body XML per la richiesta REPORT CalDAV (RFC 4791)
        String xmlQuery = """
            <?xml version="1.0" encoding="utf-8" ?>
            <C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
                <D:prop>
                    <D:getetag/>
                    <C:calendar-data/>
                </D:prop>
                <C:filter>
                    <C:comp-filter name="VCALENDAR">
                        <C:comp-filter name="VEVENT">
                            <C:time-range start="%s" end="%s"/>
                        </C:comp-filter>
                    </C:comp-filter>
                </C:filter>
            </C:calendar-query>
            """.formatted(startUtc, endUtc);

        // Esecuzione richiesta HTTP REPORT
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((configFile.username() + ":" + configFile.password()).getBytes());

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(configFile.pathCalendar()))
                .method("REPORT", HttpRequest.BodyPublishers.ofString(xmlQuery))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Depth", "1")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 207 && response.statusCode() != 200) {
            throw new RuntimeException("Errore risposta CalDAV: HTTP " + response.statusCode() + " - " + response.body());
        }

        // Estrazione del contenuto <C:calendar-data> dall'XML Multistatus
        List<String> icsDataList = estraiCalendarDataDaXml(response.body());

        // Parsing degli oggetti iCalendar tramite Biweekly
        List<EventoDettaglio> eventiTrovati = new ArrayList<>();

        for (String icsContent : icsDataList) {
            // Biweekly effettua il parsing diretto della stringa .ics
            ICalendar ical = Biweekly.parse(icsContent).first();

            if (ical != null) {
                for (VEvent vevent : ical.getEvents()) {

                	String uid = vevent.getUid() != null ? vevent.getUid().getValue() : null;
                    String summary = vevent.getSummary() != null ? vevent.getSummary().getValue() : null;
                    String description = vevent.getDescription() != null ? vevent.getDescription().getValue() : null;
                    String location = vevent.getLocation() != null ? vevent.getLocation().getValue() : null;
                    String dataInizio = vevent.getDateStart() != null ? vevent.getDateStart().toString() : null;
                    String dataFine = vevent.getDateEnd() != null ? vevent.getDateEnd().toString() : null;
                    Integer curSequence = vevent.getSequence().getValue();

                    // Filtro per titolo/sommario dell'evento (case-insensitive)
                    if (nomeEvento != null && !nomeEvento.isBlank()) {
                    	
                    	System.out.println("---------------------------------");
                    	System.out.println("uid: " + uid);
                    	System.out.println("nomeEvento: " + nomeEvento);
                    	System.out.println("description: " + description);
                    	System.out.println("location: " + location);
                    	System.out.println("dataInizio: " + dataInizio);
                    	System.out.println("dataFine: " + dataFine);
                    	System.out.println("sequence: " + curSequence);
                    	System.out.println("---------------------------------");
                    	
                        if (summary == null || !summary.toLowerCase().contains(nomeEvento.toLowerCase())) {
                            continue;
                        }
                    }

                    // Estrazione Data/Ora Inizio
                    ZonedDateTime dtStart = null;
                    if (vevent.getDateStart() != null && vevent.getDateStart().getValue() != null) {
                        dtStart = vevent.getDateStart().getValue().toInstant().atZone(inizio.getZone());
                    }

                    // Estrazione Data/Ora Fine
                    ZonedDateTime dtEnd = null;
                    if (vevent.getDateEnd() != null && vevent.getDateEnd().getValue() != null) {
                        dtEnd = vevent.getDateEnd().getValue().toInstant().atZone(fine.getZone());
                    }

                    // Aggiunge evento alla lista
                    eventiTrovati.add(new EventoDettaglio(
                    		uid, 
                    		summary, 
                    		description, 
                    		location, 
                    		dtStart, 
                    		dtEnd, 
                    		curSequence,
                    		vevent
                    		));
                }
            }
        }

        return eventiTrovati;
    }
    
    /**
     * Crea o aggiorna un evento CalDAV utilizzando la libreria biweekly.
     *
     * @param uid       UID dell'evento (se null o vuoto, viene creato un nuovo evento)
     * @param summary   Titolo / Sintesi dell'evento
     * @param startDate Data e ora di inizio
     * @param endDate   Data e ora di fine
     */
    public boolean aggiornaEvento(
    		AppConfig configFile,
    		EventoDettaglio evento) {
    	
    	boolean ret = false;
        boolean isNew = (evento.getUid() == null || evento.getUid().trim().isEmpty());

        if (isNew) {
        	evento.setUid(UUID.randomUUID().toString());
            System.out.println("UID non presente. Generato un nuovo UID per la creazione: " + evento.getUid());
        } else {
            System.out.println("Aggiornamento dell'evento con UID: " + evento.getUid());
        }

        // Creazione della struttura iCalendar con biweekly
        ICalendar ical = new ICalendar();
        VEvent event;
        
        // Se evento esiste lo modifica altrimenti lo crea
        if (evento.getEvento() != null) {
        	event = evento.getEvento();
		} else {
			event = new VEvent();
		}
        
        event.setUid(evento.getUid());
        event.setSummary(evento.getDescrizione());
        event.setDateStart(Date.from(evento.getDataOraInizio().toInstant()));
        event.setDateEnd(Date.from(evento.getDataOraFine().toInstant()));

        ical.addEvent(event);

        // Converte l'oggetto iCalendar in formato stringa standard (.ics)
        String icsPayload = Biweekly.write(ical).go();

        // 2. Invio dei dati al server CalDAV tramite HTTP PUT
        ret = sendCalDavRequest(configFile, evento.getUid(), icsPayload);
        
        return ret;
    }
    
    private boolean sendCalDavRequest(
    		AppConfig configFile,
    		String uid, 
    		String icsPayload) {
    	
    	boolean ret = false;
    	
        try {
            // CORRETTO: Unisce l'URL del calendario di mailbox.org con lo UID dell'evento
            // Esempio finale: https://dav.mailbox.org/caldav/Y2FsOi8vfsd45w/mio-uid-univoco.ics
            String eventUrl = configFile.pathCalendar().endsWith("/") 
                    ? configFile.pathCalendar() + uid + ".ics" 
                    : configFile.pathCalendar() + "/" + uid + ".ics";

            // Preparazione dell'autenticazione HTTP Basic
//            String auth = USERNAME + ":" + PASSWORD;
//            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + Base64.getEncoder().encodeToString((configFile.username() + ":" + configFile.password()).getBytes());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eventUrl))
                    .header("Content-Type", "text/calendar; charset=utf-8")
                    .header("Authorization", authHeader)
                    .PUT(HttpRequest.BodyPublishers.ofString(icsPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 201 Created o 204 No Content indicano successo in WebDAV/CalDAV
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
            	ret = true;
                System.out.println("Evento sincronizzato con successo su mailbox.org! (Codice: " + response.statusCode() + ")");
            } else {
                System.err.println("Errore CalDAV. Codice HTTP: " + response.statusCode());
                System.err.println("Risposta server: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Errore di connessione: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ret;
    }


/*
    private boolean sendCalDavRequest(
    		AppConfig configFile,
    		String uid, 
    		String icsPayload) {
    	
    	boolean ret = false;
    	
        try {
            // URL tipico di una risorsa CalDAV: URL_Calendario / UID.ics
            String calDavUrl = "https://tuo-server-caldav.com/remote.php/dav/calendars/utente/calendario/" + uid + ".ics";

            String authHeader = "Basic " + Base64.getEncoder().encodeToString((configFile.username() + ":" + configFile.password()).getBytes());
            
            HttpClient client = HttpClient.newHttpClient();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(calDavUrl))
                    .header("Content-Type", "text/calendar; charset=utf-8")
                    // Aggiungi qui l'autenticazione Basic o Digest se richiesta dal server
                    // .header("Authorization", "Basic " + encodedCredentials)
                    .header("Authorization", authHeader)
                    .PUT(HttpRequest.BodyPublishers.ofString(icsPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Evento salvato con successo sul server CalDAV.");
                ret = true;
            } else {
                System.err.println("Errore CalDAV. Codice HTTP: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Errore di connessione al server CalDAV: " + e.getMessage());
            e.printStackTrace();
        }
    	
    	return ret;
    }
*/
    
    /**
     * Helper per estrarre il testo ICS dai tag <C:calendar-data>
     */
    private List<String> estraiCalendarDataDaXml(String xmlResponseBody) throws Exception {
        List<String> icsList = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.parse(new InputSource(new StringReader(xmlResponseBody)));

        NodeList nodes = doc.getElementsByTagNameNS("*", "calendar-data");
        for (int i = 0; i < nodes.getLength(); i++) {
            String content = nodes.item(i).getTextContent();
            if (content != null && !content.isBlank()) {
                icsList.add(content);
            }
        }
        return icsList;
    }
}