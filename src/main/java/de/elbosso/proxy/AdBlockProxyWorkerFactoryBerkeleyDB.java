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

package de.elbosso.proxy;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 *
 * @author elbosso
 */
public class AdBlockProxyWorkerFactoryBerkeleyDB extends Object implements AdBlockProxyWorkerFactory
,AdBlockProxyWorkerFactoryBerkeleyDBMBean
{
	private final static org.slf4j.Logger CLASS_LOGGER = org.slf4j.LoggerFactory.getLogger(AdBlockProxyWorkerFactoryBerkeleyDB.class);
	private final de.elbosso.util.net.proxy.Context context;
	private AdBlockWorkerStatistics adBlockWorkerStatistics;
	private final Object listMonitor;
	private AdBlockProxyWorkerBerkeleyDB mine;
	private com.sleepycat.je.Environment myDbEnvironment;
	private com.sleepycat.persist.EntityStore store;
	private com.sleepycat.persist.PrimaryIndex<String,de.elbosso.proxy.entities.ServerWhitelistEntity> serverWhitelistPrimaryIndex;
	private com.sleepycat.persist.PrimaryIndex<String,de.elbosso.proxy.entities.ServerBlacklistEntity> serverBlacklistPrimaryIndex;
	private com.sleepycat.persist.PrimaryIndex<String,de.elbosso.proxy.entities.DomainBlacklistEntity> domainBlacklistPrimaryIndex;

	public AdBlockProxyWorkerFactoryBerkeleyDB() throws com.sleepycat.je.DatabaseException
	{
		this(new de.elbosso.util.net.proxy.Context());
	}

	public AdBlockProxyWorkerFactoryBerkeleyDB(de.elbosso.util.net.proxy.Context context) throws com.sleepycat.je.DatabaseException
	{
		super();
		this.context = context;
		listMonitor = new Object();
		String filename = "AdBlockProxy";
		java.io.File dbFile = new java.io.File(de.elbosso.util.Utilities.getConfigDirectory("AdBlockProxyBerkeley"), filename);
		if(dbFile.exists()==false)
			dbFile.mkdirs();
		// Open the environment. Allow it to be created if it does not
// already exist.
		com.sleepycat.je.EnvironmentConfig envConfig = new com.sleepycat.je.EnvironmentConfig();
		envConfig.setAllowCreate(true);
		com.sleepycat.persist.StoreConfig storeConfig = new com.sleepycat.persist.StoreConfig();
		storeConfig.setAllowCreate(true);
		myDbEnvironment = new com.sleepycat.je.Environment(dbFile,envConfig);
		store = new com.sleepycat.persist.EntityStore(myDbEnvironment, "EntityStore", storeConfig);
		serverWhitelistPrimaryIndex =store.getPrimaryIndex(java.lang.String.class, de.elbosso.proxy.entities.ServerWhitelistEntity.class);
		serverBlacklistPrimaryIndex =store.getPrimaryIndex(java.lang.String.class, de.elbosso.proxy.entities.ServerBlacklistEntity.class);
		domainBlacklistPrimaryIndex =store.getPrimaryIndex(java.lang.String.class, de.elbosso.proxy.entities.DomainBlacklistEntity.class);
	}

	public AdBlockWorkerStatistics getStatistics()
	{
		AdBlockProxyWorkerFactory fac=this;
		if (adBlockWorkerStatistics == null)
			adBlockWorkerStatistics = new AdBlockWorkerStatistics(this);
		return adBlockWorkerStatistics;
	}

	public AdBlockProxyWorkerFactoryBerkeleyDB(de.elbosso.util.net.proxy.Context context, String blacklistfilename)  throws com.sleepycat.je.DatabaseException
	{
		this(context);
/*		java.sql.PreparedStatement pstmt=dbconnection.prepareStatement("INSERT INTO SERVERBLACKLIST (NAME) VALUES (?)");
		for(java.lang.String name:serverBlackList)
		{
			pstmt.setString(1,name);
			pstmt.executeUpdate();
		}
		pstmt=dbconnection.prepareStatement("INSERT INTO DOMAINBLACKLIST (NAME) VALUES (?)");
		for(java.lang.String name:domainBlackList)
		{
			pstmt.setString(1,name);
			pstmt.executeUpdate();
		}
		pstmt=dbconnection.prepareStatement("INSERT INTO SERVERWHITELIST (NAME) VALUES (?)");
		for(java.lang.String name:serverWhiteList)
		{
			pstmt.setString(1,name);
			pstmt.executeUpdate();
		}
*/
	}

	public de.elbosso.util.net.proxy.ProxyWorker create(java.net.Socket clientSocket)
	{
		de.elbosso.util.net.proxy.ProxyWorker rv = null;
		try
		{
			rv = new AdBlockProxyWorkerBerkeleyDB(getStatistics(), store, clientSocket, context);
		} catch (com.sleepycat.je.DatabaseException e)
		{
			CLASS_LOGGER.error(e.getMessage());
		}
		return rv;
	}

	@Override
	public void close() throws IOException
	{
		try {
			if (store != null)
			{
				store.close();
			}
			if (myDbEnvironment != null)
			{
				myDbEnvironment.close();
			}
//			new java.lang.Throwable().printStackTrace();
		} catch (com.sleepycat.je.DatabaseException dbe) {
			// Exception handling goes here
			throw new IOException(dbe);
		}
	}

	public void whitelistServer(String host) throws com.sleepycat.je.DatabaseException
	{
		if (host != null)
		{
			if (host.indexOf('\t') > -1)
			{
				String[] hosts = host.split("\t");
				for (String h : hosts)
				{
					whitelistServer(h);
				}
			}
			else
			{
				if (host.trim().length() > 1)
				{
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("trying to whitelist server: " + host);
					//check if blacklisted and if so - remove from blacklist
					de.elbosso.proxy.entities.ServerBlacklistEntity serverBlacklistEntity=serverBlacklistPrimaryIndex.get(host);
					if(serverBlacklistEntity!=null)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the blacklist first: " + host);
						serverBlacklistPrimaryIndex.delete(host);
					}
					//add to whitelist
					de.elbosso.proxy.entities.ServerWhitelistEntity serverWhitelistEntity=new de.elbosso.proxy.entities.ServerWhitelistEntity();
					serverWhitelistEntity.setName(host);
					serverWhitelistEntity.setTimestamp(new java.util.Date());
					if(serverWhitelistPrimaryIndex.putNoOverwrite(serverWhitelistEntity))
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new whitelisted server: " + host);
					}
				}
			}
		}
	}

	@Override
	public void blacklistServer(String host)throws com.sleepycat.je.DatabaseException
	{
		if (host != null)
		{
			if (host.indexOf('\t') > -1)
			{
				String[] hosts = host.split("\t");
				for (String h : hosts)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (host.trim().length() > 1)
				{
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("trying to blacklist server: " + host);
					//check if whitelisted and if so - remove from blacklist
					de.elbosso.proxy.entities.ServerWhitelistEntity serverWhitelistEntity=serverWhitelistPrimaryIndex.get(host);
					if(serverWhitelistEntity!=null)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the whitelist first: " + host);
						serverWhitelistPrimaryIndex.delete(host);
					}
					//add to blacklist
					de.elbosso.proxy.entities.ServerBlacklistEntity serverBlacklistEntity=new de.elbosso.proxy.entities.ServerBlacklistEntity();
					serverBlacklistEntity.setName(host);
					serverBlacklistEntity.setTimestamp(new java.util.Date());
					if(serverBlacklistPrimaryIndex.putNoOverwrite(serverBlacklistEntity))
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new blacklisted server: " + host);
					}
				}
			}
		}
	}

	@Override
	public void removeServer(String host)throws com.sleepycat.je.DatabaseException
	{
		if (host != null)
		{
			if (host.indexOf('\t') > -1)
			{
				String[] hosts = host.split("\t");
				for (String h : hosts)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (host.trim().length() > 1)
				{
					de.elbosso.proxy.entities.ServerBlacklistEntity serverBlacklistEntity=serverBlacklistPrimaryIndex.get(host);
					if(serverBlacklistEntity!=null)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the blacklist: " + host);
						serverBlacklistPrimaryIndex.delete(host);
					}
					de.elbosso.proxy.entities.ServerWhitelistEntity serverWhitelistEntity=serverWhitelistPrimaryIndex.get(host);
					if(serverWhitelistEntity!=null)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the whitelist: " + host);
						serverWhitelistPrimaryIndex.delete(host);
					}
				}
			}
		}
	}

	@Override
	public void blacklistDomain(String domain)throws com.sleepycat.je.DatabaseException
	{
		if (domain != null)
		{
			if (domain.indexOf('\t') > -1)
			{
				String[] domains = domain.split("\t");
				for (String h : domains)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (domain.trim().length() > 1)
				{
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("trying to blacklist domain: " + domain);
					//add to blacklist
					de.elbosso.proxy.entities.DomainBlacklistEntity domainBlacklistEntity=new de.elbosso.proxy.entities.DomainBlacklistEntity();
					domainBlacklistEntity.setName(domain);
					domainBlacklistEntity.setTimestamp(new java.util.Date());
					if(domainBlacklistPrimaryIndex.putNoOverwrite(domainBlacklistEntity))
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new blacklisted domain: " + domain);
					}
				}
			}
		}

	}
	@Override
	public void removeDomain(String domain)throws com.sleepycat.je.DatabaseException
	{
		if (domain != null)
		{
			if (domain.indexOf('\t') > -1)
			{
				String[] domains = domain.split("\t");
				for (String h : domains)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (domain.trim().length() > 1)
				{
					de.elbosso.proxy.entities.DomainBlacklistEntity domainBlacklistEntity=domainBlacklistPrimaryIndex.get(domain);
					if(domain!=null)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the domain blacklist: " + domain);
						domainBlacklistPrimaryIndex.delete(domain);
					}
				}
			}
		}
	}

	@Override
	public boolean check(String host) throws UnknownHostException, com.sleepycat.je.DatabaseException
	{
		if (mine == null)
			mine = new AdBlockProxyWorkerBerkeleyDB(getStatistics(), store, null, context);
		return mine.determineAddress(host) != null;
	}

	@Override
	public int getWhitelistedServerCount()throws com.sleepycat.je.DatabaseException
	{
		return (int)serverWhitelistPrimaryIndex.count();
	}

	@Override
	public int getBlacklistedServerCount()throws com.sleepycat.je.DatabaseException
	{
		return (int)serverBlacklistPrimaryIndex.count();
	}

	@Override
	public int getBlacklistedDomainCount()throws com.sleepycat.je.DatabaseException
	{
		return (int)domainBlacklistPrimaryIndex.count();
	}

	public void closeStatisticContext()
	{
		getStatistics().closeContext();
	}

	public javax.swing.table.TableModel openStatisticContext(String host)
	{
		return getStatistics().openContext(host);
	}

}