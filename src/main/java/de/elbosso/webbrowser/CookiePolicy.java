package de.elbosso.tools.misc;
/*
Copyright (c) 2012-2018.

Juergen Key. Alle Rechte vorbehalten.

Weiterverbreitung und Verwendung in nichtkompilierter oder kompilierter Form,
mit oder ohne Veraenderung, sind unter den folgenden Bedingungen zulaessig:

   1. Weiterverbreitete nichtkompilierte Exemplare muessen das obige Copyright,
die Liste der Bedingungen und den folgenden Haftungsausschluss im Quelltext
enthalten.
   2. Weiterverbreitete kompilierte Exemplare muessen das obige Copyright,
die Liste der Bedingungen und den folgenden Haftungsausschluss in der
Dokumentation und/oder anderen Materialien, die mit dem Exemplar verbreitet
werden, enthalten.
   3. Weder der Name des Autors noch die Namen der Beitragsleistenden
duerfen zum Kennzeichnen oder Bewerben von Produkten, die von dieser Software
abgeleitet wurden, ohne spezielle vorherige schriftliche Genehmigung verwendet
werden.

DIESE SOFTWARE WIRD VOM AUTOR UND DEN BEITRAGSLEISTENDEN OHNE
JEGLICHE SPEZIELLE ODER IMPLIZIERTE GARANTIEN ZUR VERFUEGUNG GESTELLT, DIE
UNTER ANDEREM EINSCHLIESSEN: DIE IMPLIZIERTE GARANTIE DER VERWENDBARKEIT DER
SOFTWARE FUER EINEN BESTIMMTEN ZWECK. AUF KEINEN FALL IST DER AUTOR
ODER DIE BEITRAGSLEISTENDEN FUER IRGENDWELCHE DIREKTEN, INDIREKTEN,
ZUFAELLIGEN, SPEZIELLEN, BEISPIELHAFTEN ODER FOLGENDEN SCHAEDEN (UNTER ANDEREM
VERSCHAFFEN VON ERSATZGUETERN ODER -DIENSTLEISTUNGEN; EINSCHRAENKUNG DER
NUTZUNGSFAEHIGKEIT; VERLUST VON NUTZUNGSFAEHIGKEIT; DATEN; PROFIT ODER
GESCHAEFTSUNTERBRECHUNG), WIE AUCH IMMER VERURSACHT UND UNTER WELCHER
VERPFLICHTUNG AUCH IMMER, OB IN VERTRAG, STRIKTER VERPFLICHTUNG ODER
UNERLAUBTE HANDLUNG (INKLUSIVE FAHRLAESSIGKEIT) VERANTWORTLICH, AUF WELCHEM
WEG SIE AUCH IMMER DURCH DIE BENUTZUNG DIESER SOFTWARE ENTSTANDEN SIND, SOGAR,
WENN SIE AUF DIE MOEGLICHKEIT EINES SOLCHEN SCHADENS HINGEWIESEN WORDEN SIND.

 */

import de.netsysit.scratch.ui.AbstractAction;

import java.net.HttpCookie;
import java.net.URI;

public interface CookiePolicy extends java.net.CookiePolicy{
	/**
	 * One pre-defined policy which accepts all cookies.
	 */
	public static final java.net.CookiePolicy ACCEPT_ALL = new AcceptAll();

	/**
	 * One pre-defined policy which accepts no cookies.
	 */
	public static final java.net.CookiePolicy ACCEPT_NONE = new AcceptNone();

	/**
	 * One pre-defined policy which only accepts cookies from original server.
	 */
	public static final java.net.CookiePolicy ACCEPT_ORIGINAL_SERVER  = new AcceptOriginalServer();

	class AcceptAll extends java.lang.Object implements CookiePolicy
	{
		private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AcceptAll.class);
		public boolean shouldAccept(URI uri, HttpCookie cookie) {
			if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("accepting unconditionally "+cookie.getName()+":"+cookie.getValue()+" from "+uri);
			return true;
		}

		@Override
		public String toString()
		{
			return "accepts all cookies";
		}
	}
	class AcceptNone extends java.lang.Object implements CookiePolicy
	{
		private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AcceptNone.class);
		public boolean shouldAccept(URI uri, HttpCookie cookie) {
			if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("declining unconditionally "+cookie.getName()+":"+cookie.getValue()+" from "+uri);
			return false;
		}
		@Override
		public String toString()
		{
			return "accepts no cookies";
		}
	}
	class AcceptOriginalServer extends java.lang.Object implements CookiePolicy
	{
		private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AcceptOriginalServer.class);
		public boolean shouldAccept(URI uri, HttpCookie cookie) {
			boolean rv=false;
			if (uri == null)
			{
				if (CLASS_LOGGER.isTraceEnabled())
					CLASS_LOGGER.trace("declining " + cookie.getName() + ":" + cookie.getValue() + " because no URI was given");
			}
			else if(cookie == null)
			{
				if (CLASS_LOGGER.isTraceEnabled())
					CLASS_LOGGER.trace("declining from "+uri+" because cookie was null");
			}
			else
			{
				rv = HttpCookie.domainMatches(cookie.getDomain(), uri.getHost());
				if(CLASS_LOGGER.isTraceEnabled())
				{
					if(rv)
					{
						CLASS_LOGGER.trace("accepting "+cookie.getName()+":"+cookie.getValue()+" from "+uri+" because domain match was successfull");
					}
					else
					{
						CLASS_LOGGER.trace("declining "+cookie.getName()+":"+cookie.getValue()+" from "+uri+" because domain match was not successfull");
					}
				}
			}
			return rv;
		}
		@Override
		public String toString()
		{
			return "accepts only cookies from the original server";
		}
	}
}
