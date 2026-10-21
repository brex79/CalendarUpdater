
package it.brex.CalendarUpdater;

import java.time.ZonedDateTime;

import biweekly.component.VEvent;

public class EventoDettaglio {
	
    private String uid;
    private String titolo;
	private String descrizione;
    private String luogo;
    private ZonedDateTime dataOraInizio;
    private ZonedDateTime dataOraFine;
    private Integer sequence;
    private VEvent evento;

    // Costruttore
    public EventoDettaglio(
    		String uid, 
    		String titolo, 
    		String descrizione, 
    		String luogo, 
    		ZonedDateTime dataOraInizio, 
    		ZonedDateTime dataOraFine,
    		Integer sequence,
    		VEvent evento) {
    	
        this.uid = uid;
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.luogo = luogo;
        this.dataOraInizio = dataOraInizio;
        this.dataOraFine = dataOraFine;
        this.sequence = sequence;
        this.evento = evento;
        
    }

	// Getter
    public String getUid() { return uid; }
    public String getTitolo() { return titolo; }
    public String getDescrizione() { return descrizione; }
    public String getLuogo() { return luogo; }
    public ZonedDateTime getDataOraInizio() { return dataOraInizio; }
    public ZonedDateTime getDataOraFine() { return dataOraFine; }
    public Integer getSequence() { return sequence; }
    
    // Setter
    public void setUid(String uid) {
        this.uid = uid;
    }
    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }
    public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
    }
	public void setLuogo(String luogo) {
		this.luogo = luogo;
	}
	public void setDataOraInizio(ZonedDateTime dataOraInizio) {
		this.dataOraInizio = dataOraInizio;
	}
	public void setDataOraFine(ZonedDateTime dataOraFine) {
		this.dataOraFine = dataOraFine;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}
    public VEvent getEvento() {
		return evento;
	}
	public void setEvento(VEvent evento) {
		this.evento = evento;
	}
}