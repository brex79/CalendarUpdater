package it.brex.CalendarUpdater;

import java.util.Date;

import jakarta.mail.Message;

public record EmailDettaglio(
    String mittente,
    String oggetto,
    Date dataRicezione,
    String corpo,
    Message msg
) {}