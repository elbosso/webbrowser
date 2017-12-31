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

import org.apache.log4j.Level;

import javax.management.*;

public class CookieManager extends java.net.CookieManager implements javax.management.NotificationBroadcaster
	,CookieManagerMBean
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(CookieManager.class);
	private javax.management.NotificationBroadcasterSupport notificationBroadcasterSupport;
	private long notificationSequence = 0;

	public CookieManager()
	{
		super();
		notificationBroadcasterSupport=new javax.management.NotificationBroadcasterSupport();
	}

	@Override
	public java.util.Map<String, java.util.List<String>> get(java.net.URI uri, java.util.Map<String, java.util.List<String>> requestHeaders) throws java.io.IOException
	{
		boolean headerPrinted=false;
		java.util.Map<String, java.util.List<String>> rv = super.get(uri, requestHeaders);
		for (java.lang.String key : rv.keySet())
		{
			if(rv.get(key).isEmpty()==false)
			{
				if (headerPrinted == false)
				{
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("cookies to be sent for " + uri);
					headerPrinted = true;
				}
				if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace(key + ": " + rv.get(key));
				Notification notif=new Notification(
						"CookieManager.get",     // type
						this,              // source
						++notificationSequence,     // seq. number
						key + ": " + rv.get(key)
				);
				notif.setUserData(uri);
				notificationBroadcasterSupport.sendNotification(notif);
			}
		}
		return rv;
	}

	@Override
	public void put(java.net.URI uri, java.util.Map<String, java.util.List<String>> responseHeaders) throws java.io.IOException
	{
		boolean headerPrinted=false;
		// pre-condition check
		if (uri != null && responseHeaders != null)
		{
			for (String headerKey : responseHeaders.keySet())
			{
				// RFC 2965 3.2.2, key must be 'Set-Cookie2'
				// we also accept 'Set-Cookie' here for backward compatibility
				if (headerKey == null
						|| !(headerKey.equalsIgnoreCase("Set-Cookie2")
						|| headerKey.equalsIgnoreCase("Set-Cookie")
				)
						)
				{
					continue;
				}

				for (String headerValue : responseHeaders.get(headerKey))
				{
					try
					{
						java.util.List<java.net.HttpCookie> cookies;
						try
						{
							cookies = java.net.HttpCookie.parse(headerValue);
						} catch (IllegalArgumentException e)
						{
							// Bogus header, make an empty list and log the error
							cookies = java.util.Collections.emptyList();
							if (CLASS_LOGGER.isEnabledFor(Level.ERROR))
								CLASS_LOGGER.error("Invalid cookie for " + uri + ": " + headerValue);
						}
						for (java.net.HttpCookie cookie : cookies)
						{
							if (cookie.getPath() == null)
							{
								// If no path is specified, then by default
								// the path is the directory of the page/doc
								String path = uri.getPath();
								if (!path.endsWith("/"))
								{
									int i = path.lastIndexOf("/");
									if (i > 0)
									{
										path = path.substring(0, i + 1);
									}
									else
									{
										path = "/";
									}
								}
								cookie.setPath(path);
							}

							// As per RFC 2965, section 3.3.1:
							// Domain  Defaults to the effective request-host.  (Note that because
							// there is no dot at the beginning of effective request-host,
							// the default Domain can only domain-match itself.)
							if (cookie.getDomain() == null)
							{
								String host = uri.getHost();
								if (host != null && !host.contains("."))
									host += ".local";
								cookie.setDomain(host);
							}
							if(headerPrinted==false)
							{
								if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("cookies offered to be saved for " + uri);
								headerPrinted = true;
							}
							if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace(cookie+" "+cookie.getPath());
							Notification notif=new Notification(
									"CookieManager.put",     // type
									this,              // source
									++notificationSequence,     // seq. number
									cookie+" "+cookie.getPath()
							);
							notif.setUserData(uri);
							notificationBroadcasterSupport.sendNotification(notif);
						}
					} catch (IllegalArgumentException e)
					{
						// invalid set-cookie header string
						// no-op
					}
				}
			}
		}

		super.put(uri, responseHeaders);
	}

	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException
	{
		notificationBroadcasterSupport.addNotificationListener(listener,filter,handback);
	}

	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException
	{
		notificationBroadcasterSupport.removeNotificationListener(listener);
	}

	@Override
	public MBeanNotificationInfo[] getNotificationInfo()
	{
		return new MBeanNotificationInfo[]{
				new MBeanNotificationInfo(
						new String[]
								{"CookieManager.put","CookieManager.get"}, // notif. types
						Notification.class.getName(), // notif. class
						"Cookies"     // description
				)
		};
	}
}
