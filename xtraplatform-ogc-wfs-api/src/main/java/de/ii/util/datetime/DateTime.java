/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.util.datetime;

import de.ii.xtraserver.framework.exceptions.ConversionErrorDateTime;

/**
 * // code taken from XtraServer
 *
 * @author fischer
 */
public class DateTime {

    private int year;		// Jahr
    private int month;		// Monat 1 .. 12
    private int day;		// Tag 1 .. 31
    private int hour;		// Stunde 0 .. 23
    private int minute;		// Minute 0 .. 59
    private int msec;		// Millisekunden 0 .. 59999
    private int zoneMin;	// Abw. der Zeitzone in Minuten (0=UTC)
    private boolean zoneSet;	// Flag, ob Zone explizit gesetzt wurde.
    private int significant;	// Zahl der signifikanten Angaben 1=year bis 6=msec

    public DateTime(String st) throws ConversionErrorDateTime {

        year = 1;
        day = 1;
        hour = 0;
        minute = 0;
        msec = 0;
        zoneMin = 0;
        zoneSet = false;
        significant = 0;

        int dt[] = {0, 0, 0, 0, 0, 0, 0, 0, 0};	// the various numeric parts
        int nd[] = {0, 0, 0, 0, 0, 0, 0, 0, 0};	// # of digits in each
        int idt = 0;								// part currently parsed
        int jdt = 0;								// day or time part counter
        int is = 1;									// Sign of zone

        // Walk over 8601 string and switch state machine
        for (int i = 0; i < st.length(); i++) {
            // No more chars allowed after the "Z" ...
            if (idt > 8) {
                throw new ConversionErrorDateTime(st);
            }

            char c = st.charAt(i);
            if ('0' <= c && c <= '9') {
                // A digit goes into its associated part
                dt[idt] = 10 * dt[idt] + (c - '0');
                nd[idt]++;
            } else if (idt <= 1 && c == '-' || idt == 2 && c == 'T'
                    || (idt == 3 || idt == 4 || idt == 7) && c == ':'
                    || idt == 5 && (c == '.' || c == ',')) // Separators increment part count
            {
                idt++;
            } else if ((2 == idt || 3 <= idt) && idt <= 6 && (c == 'Z' || c == '+' || c == '-')) {
                zoneSet = true;
                // Switch to zoning info
                if (c == '-') {
                    is = -1;
                }
                jdt = idt;
                idt = 7;
                if (c == 'Z') {
                    idt = 9;
                }
            } else // And nothing else
            {
                throw new ConversionErrorDateTime(st);
            }
        }
        if (jdt == 0) {
            jdt = idt;
        }

        // Plausibility checks on the numbers. Could be more precise ...

        if (nd[0] != 4) {
            throw new ConversionErrorDateTime(st);
        }
        if (jdt >= 1 && (nd[1] > 2 || dt[1] < 1 || dt[1] > 12)) {
            throw new ConversionErrorDateTime(st);
        }
        // Hier muss man eigentlich genauer pruefen ...
        if (jdt >= 2 && (nd[2] > 2 || dt[2] < 1 || dt[2] > 31)) {
            throw new ConversionErrorDateTime(st);
        }
        if (jdt >= 3 && (nd[3] > 2 || dt[3] < 0 || dt[3] > 23)) {
            throw new ConversionErrorDateTime(st);
        }
        if (jdt >= 4 && (nd[4] > 2 || dt[4] < 0 || dt[4] > 59)) {
            throw new ConversionErrorDateTime(st);
        }
        if (jdt >= 5 && (nd[5] > 2 || dt[5] < 0 || dt[5] > 59)) {
            throw new ConversionErrorDateTime(st);
        }

        // Normalize milliseconds

        if (jdt >= 6) {
            while (nd[6] > 3) {
                dt[6] /= 10;
                nd[6]--;
            }
            while (nd[6] < 3) {
                dt[6] *= 10;
                nd[6]++;
            }
        }

        // Initialize the output fields
        // (Notice minimum allowed values for year, month and day)
        year = dt[0] == 0 ? 1 : dt[0];
        month = 1 > dt[1] ? 1 : dt[1];
        day = 1 > dt[2] ? 1 : dt[2];
        hour = dt[3];
        minute = dt[4];
        msec = dt[5] * 1000 + dt[6];
        zoneMin = dt[7] * 60 + dt[8];
        zoneMin *= is;
        if (jdt == 6) {
            jdt = 5; // Maximum for significant is 6 (i.e. millisecs 
        }	// are not ment to increase part counter)
        significant = jdt + 1;
    }

    public long toUnix() {
        
        makeUTC();
        
        /* Tage seit Jahresanfang ohne Tage des aktuellen Monats und ohne Schalttag */
        int tage_seit_jahresanfang[] =  {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};

        long unix_zeit;
        long jahre = year - 1970;
        int schaltjahre = ((year - 1) - 1968) / 4 - ((year - 1) - 1900) / 100 + ((year - 1) - 1600) / 400;

        unix_zeit = msec + 60 * minute + 60 * 60 * hour
                + (tage_seit_jahresanfang[month - 1] + day - 1) * 60 * 60 * 24
                + (jahre * 365 + schaltjahre) * 60 * 60 * 24;

        if ((month > 2) && (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))) {
            unix_zeit += 60 * 60 * 24; /* +Schalttag wenn jahr Schaltjahr ist */
        }

        return unix_zeit;
    }
    
    public long toUnixMilliseconds() {
        return this.toUnix()*1000;
    }
    
    
    private void makeUTC()
{
	// Schluss, wenn nichts zu normalisieren ist
	if( significant <= 2 ) return;
	if( zoneMin == 0 ) return;

	// Zonenangabe in Vorzeichen, Stunde und Minute zerlegen
	int isgn = 1;
	if( zoneMin < 0 ) isgn = -1;
	int zon = isgn * zoneMin;
	int h = zon / 60;
	int m = zon - 60 * h;

	// Minuten adjustieren, Stundenuebertag feststellen
	int mins = minute - isgn * m;
	int hover = 0;
	if( mins < 0 ) 
	{
		mins += 60;
		hover = -1;
	}
	else if( mins >= 60 )
	{
		mins -= 60;
		hover = 1;
	}
	minute = mins;

	// Stunden adjustieren, Tagesuebertrag feststellen
	int hours = hour - isgn * h + hover;
	int dover = 0;
	if( hours < 0 )
	{
		hours += 24;
		dover = -1;
	}
	else if( hours >= 24 )
	{
		hours -= 24;
		dover = 1;
	}
	hour = hours;

	// Tage adjustieren, Monatsuebertrag feststellen
	int days = day + dover;
	int md[] = { 31, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	int daysMonth = md[month];
	if( month==2 && ( year%4==0 && year%100!=0 || year%400==0 ) ) 
		daysMonth = 29;
	int mthover = 0;
	if( days < 1 )
	{
		days = md[month-1];
		if( month==3 && ( year%4==0 && year%100!=0 || year%400==0 ) ) 
			days = 29;
		mthover = -1;
	}
	else if( days>31 || days>daysMonth )
	{
		days = 1;
		mthover = 1;
	}
	day = days;

	// Monate adjustieren, Jahr dabei fortschreiben
	int months = month + mthover;
	if( months < 1 )
	{
		months = 12;
		year--;
	}
	else if( months > 12 )
	{
		months -= 12;
		year++;
	}
	month = months;

	// Signifikanzanzeiger veraendern, wenn wir Stunden oder Minuten 
	// fortschreiben mussten
	if( h!= 0 && significant<=3 ) significant = 4;
	if( m!= 0 && significant<=4 ) significant = 5;

	// Jetzt UTC
	zoneMin = 0;
}

}
