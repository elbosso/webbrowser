package de.elbosso.proxy;
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

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

/**
 *
 * @author elbosso
 */
public class AdBlockProxyWorkerBerkeleyDB extends de.elbosso.util.net.proxy.ProxyWorker
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AdBlockProxyWorkerBerkeleyDB.class);
	private final AdBlockWorkerStatistics adBlockWorkerStatistics;
	private boolean notWhitelistedMeansBlacklisted=true;
	private com.sleepycat.persist.EntityStore store;
	private com.sleepycat.persist.PrimaryIndex<String,de.elbosso.proxy.entities.ServerWhitelistEntity> serverWhitelistPrimaryIndex;
	private com.sleepycat.persist.PrimaryIndex<String,de.elbosso.proxy.entities.ServerBlacklistEntity> serverBlacklistPrimaryIndex;
	private com.sleepycat.persist.PrimaryIndex<String,de.elbosso.proxy.entities.DomainBlacklistEntity> domainBlacklistPrimaryIndex;

	public AdBlockProxyWorkerBerkeleyDB(AdBlockWorkerStatistics adBlockWorkerStatistics, com.sleepycat.persist.EntityStore store, Socket socket, de.elbosso.util.net.proxy.Context context)  throws com.sleepycat.je.DatabaseException
	{
		super(socket, context);
		this.adBlockWorkerStatistics=adBlockWorkerStatistics;
		this.store=store;
		serverWhitelistPrimaryIndex =store.getPrimaryIndex(java.lang.String.class, de.elbosso.proxy.entities.ServerWhitelistEntity.class);
		serverBlacklistPrimaryIndex =store.getPrimaryIndex(java.lang.String.class, de.elbosso.proxy.entities.ServerBlacklistEntity.class);
		domainBlacklistPrimaryIndex =store.getPrimaryIndex(java.lang.String.class, de.elbosso.proxy.entities.DomainBlacklistEntity.class);
	}

	@Override
	protected synchronized InetAddress determineAddress(String host) throws UnknownHostException
	{
		InetAddress rv= null;

		try
		{
			de.elbosso.proxy.entities.ServerWhitelistEntity serverWhitelistEntity=serverWhitelistPrimaryIndex.get(host);
			if(serverWhitelistEntity!=null)
			{
				rv = super.determineAddress(host); //To change body of generated methods, choose Tools | Templates.
				if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("whitelisted host " + host);
				if (adBlockWorkerStatistics != null)
					adBlockWorkerStatistics.passedOn(host);
			}
			else
			{
				de.elbosso.proxy.entities.ServerBlacklistEntity serverBlacklistEntity=serverBlacklistPrimaryIndex.get(host);
				if(serverBlacklistEntity!=null)
				{
					com.sleepycat.persist.EntityCursor<de.elbosso.proxy.entities.DomainBlacklistEntity> cursor = domainBlacklistPrimaryIndex.entities();
					boolean foundMatch=false;
					for (de.elbosso.proxy.entities.DomainBlacklistEntity entity = cursor.first();
						 entity != null;
						 entity = cursor.next())
					{
						if(entity.getName().endsWith(host))
						{
							foundMatch = true;
							break;
						}
					}
					cursor.close();
					if (foundMatch==false)
					{
						adBlockWorkerStatistics.notInDatabase(host);
						if(notWhitelistedMeansBlacklisted)
						{
							if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("not in data base - gets blacklisted per default... " + host);
							serverBlacklistEntity=new de.elbosso.proxy.entities.ServerBlacklistEntity();
							serverBlacklistEntity.setName(host);
							serverBlacklistEntity.setTimestamp(new java.util.Date());
							if(serverBlacklistPrimaryIndex.putNoOverwrite(serverBlacklistEntity))
							{
								if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new blacklisted server: " + host);
							}
							if (adBlockWorkerStatistics != null)
								adBlockWorkerStatistics.blocked(host);
						}
						else
						{
							if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("not blacklisted - searching for inetaddress... " + host);
							rv = super.determineAddress(host);
							if (adBlockWorkerStatistics != null)
								adBlockWorkerStatistics.passedOn(host);
						}
					}
					else
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("blacklisted domain " + host);
						if (adBlockWorkerStatistics != null)
							adBlockWorkerStatistics.blocked(host);
					}
				}
				else
				{
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("blacklisted host " + host);
					if (adBlockWorkerStatistics != null)
						adBlockWorkerStatistics.blocked(host);
				}
			}
			if (rv == null)
				if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("blocked " + host);
		}
		catch(com.sleepycat.je.DatabaseException sqlex)
		{
			if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.fatal(sqlex.getMessage(),sqlex);
		}
		return rv;
	}

}