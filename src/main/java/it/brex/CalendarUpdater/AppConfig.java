package it.brex.CalendarUpdater;

import java.util.Date;

public record AppConfig (
    String host,
    String port,
    String username,
    String password,
    String mittente,
    String oggetto,
    String pathCalendar,
    String folderToMove
) {}
