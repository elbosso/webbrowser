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

import javax.management.*;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;

/**
 *
 * @author elbosso
 */
public class AdBlockProxyWorkerFactoryDB extends Object implements de.elbosso.util.net.proxy.ProxyWorkerFactory
,AdBlockProxyWorkerFactoryDBMBean
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AdBlockProxyWorkerFactoryDB.class);
	private final de.elbosso.util.net.proxy.Context context;
	private de.elbosso.util.net.proxy.AdBlockWorkerStatistics adBlockWorkerStatistics;
	private final Object listMonitor;
	private final java.sql.Connection dbConnection;
	private final java.sql.PreparedStatement checkServerWhitelistStmt;
	private final java.sql.PreparedStatement checkDomainBlacklistStmt;
	private final java.sql.PreparedStatement checkServerBlacklistStmt;
	private final java.sql.PreparedStatement insertServerBlacklistStmt;
	private final java.sql.PreparedStatement insertServerWhitelistStmt;
	private final java.sql.PreparedStatement insertDomainBlacklistStmt;
	private final java.sql.PreparedStatement removeServerBlacklistStmt;
	private final java.sql.PreparedStatement removeServerWhitelistStmt;
	private final java.sql.PreparedStatement countServerWhitelistStmt;
	private final java.sql.PreparedStatement countServerBlacklistStmt;
	private final java.sql.PreparedStatement countDomainBlacklistStmt;
	private AdBlockProxyWorkerDB mine;

	public AdBlockProxyWorkerFactoryDB() throws SQLException
	{
		this(new de.elbosso.util.net.proxy.Context());
	}

	public AdBlockProxyWorkerFactoryDB(de.elbosso.util.net.proxy.Context context) throws SQLException
	{
		super();
		this.context = context;
		listMonitor = new Object();
		String filename = "AdBlockProxy";
		java.io.File dbFile = new java.io.File(de.elbosso.util.Utilities.getConfigDirectory("AdBlockProxy"), filename);
		String dburl = "jdbc:hsqldb:file:" + dbFile;
		java.util.Properties props = new java.util.Properties();
//		props.setProperty("databaseName",db);
//		props.setProperty("user",user);
//		if(pw!=null)
//			props.setProperty("password",pw);
		if (CLASS_LOGGER.isInfoEnabled()) CLASS_LOGGER.info(dburl);
		dbConnection = java.sql.DriverManager.getConnection(dburl, props);
		de.elbosso.db.domains.modelmgmt.Utilities.bootstrap(dbConnection,"model");
		checkServerWhitelistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.checkServerWhitelistSql);
		checkServerBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.checkServerBlacklistSql);
		checkDomainBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.checkDomainBlacklistSql);
		insertServerBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.insertServerBlacklistSql);
		insertServerWhitelistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.insertServerWhitelistSql);
		insertDomainBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.insertDomainBlacklistSql);
		removeServerBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.removeServerBlacklistSql);
		removeServerWhitelistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.removeServerWhitelistSql);
		countServerWhitelistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.countServerWhitelistSql);
		countServerBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.countServerBlacklistSql);
		countDomainBlacklistStmt = dbConnection.prepareStatement(AdBlockProxyWorkerDB.countDomainBlacklistSql);
	}

	public de.elbosso.util.net.proxy.AdBlockWorkerStatistics getStatistics()
	{
		if (adBlockWorkerStatistics == null)
			adBlockWorkerStatistics = new de.elbosso.util.net.proxy.AdBlockWorkerStatistics(this);
		return adBlockWorkerStatistics;
	}

	public de.elbosso.util.net.proxy.ProxyWorker create(java.net.Socket clientSocket)
	{
		de.elbosso.util.net.proxy.ProxyWorker rv = null;
		try
		{
			rv = new de.elbosso.proxy.AdBlockProxyWorkerDB(getStatistics(), dbConnection, clientSocket, context);
		} catch (SQLException e)
		{
			CLASS_LOGGER.fatal(e.getMessage());
		}
		return rv;
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			dbConnection.close();
		} catch (SQLException e)
		{
			throw new IOException(e);
		}
	}

	public void whitelistServer(String host) throws SQLException
	{
		if (host != null)
		{
			if (host.indexOf('\t') > -1)
			{
				java.lang.String[] hosts = host.split("\t");
				for (java.lang.String h : hosts)
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
					checkServerBlacklistStmt.setString(1, host);
					java.sql.ResultSet rs = checkServerBlacklistStmt.executeQuery();
					if (rs.next() == true)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the blacklist first: " + host);
						removeServerBlacklistStmt.setString(1, host);
						removeServerBlacklistStmt.executeUpdate();
					}
					rs.close();
					//add to whitelist
					checkServerWhitelistStmt.setString(1, host);
					rs = checkServerWhitelistStmt.executeQuery();
					if (rs.next() == false)
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new whitelisted server: " + host);
						insertServerWhitelistStmt.setString(1, host);
						insertServerWhitelistStmt.executeUpdate();
					}
					rs.close();
				}
			}
		}
	}

	@Override
	public void blacklistServer(String host) throws SQLException
	{
		if (host != null)
		{
			if (host.indexOf('\t') > -1)
			{
				java.lang.String[] hosts = host.split("\t");
				for (java.lang.String h : hosts)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (host.trim().length() > 1)
				{
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("trying to blacklist server: " + host);
					//check if blacklisted and if so - remove from blacklist
					checkServerWhitelistStmt.setString(1, host);
					java.sql.ResultSet rs = checkServerWhitelistStmt.executeQuery();
					if (rs.next() == true)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the whitelist first: " + host);
						removeServerWhitelistStmt.setString(1, host);
						removeServerWhitelistStmt.executeUpdate();
					}
					rs.close();
					//add to whitelist
					checkServerBlacklistStmt.setString(1, host);
					rs = checkServerBlacklistStmt.executeQuery();
					if (rs.next() == false)
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new blacklisted server: " + host);
						insertServerBlacklistStmt.setString(1, host);
						insertServerBlacklistStmt.executeUpdate();
					}
					rs.close();
				}
			}
		}
	}

	@Override
	public void removeServer(String host) throws SQLException
	{
		if (host != null)
		{
			if (host.indexOf('\t') > -1)
			{
				java.lang.String[] hosts = host.split("\t");
				for (java.lang.String h : hosts)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (host.trim().length() > 1)
				{
					checkServerWhitelistStmt.setString(1, host);
					java.sql.ResultSet rs = checkServerWhitelistStmt.executeQuery();
					if (rs.next() == true)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the whitelist: " + host);
						removeServerWhitelistStmt.setString(1, host);
						removeServerWhitelistStmt.executeUpdate();
					}
					rs.close();
					checkServerBlacklistStmt.setString(1, host);
					rs = checkServerBlacklistStmt.executeQuery();
					if (rs.next() == true)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.info("have to remove it from the blacklist: " + host);
						removeServerBlacklistStmt.setString(1, host);
						removeServerBlacklistStmt.executeUpdate();
					}
					rs.close();
				}
			}
		}
	}

	@Override
	public void blacklistDomain(String domain) throws SQLException
	{
		if (domain != null)
		{
			if (domain.indexOf('\t') > -1)
			{
				java.lang.String[] domains = domain.split("\t");
				for (java.lang.String h : domains)
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
					checkDomainBlacklistStmt.setString(1, domain);
					java.sql.ResultSet rs = checkDomainBlacklistStmt.executeQuery();
					if (rs.next() == false)
					{
						if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.info("new blacklisted domain: " + domain);
						insertDomainBlacklistStmt.setString(1, domain);
						insertDomainBlacklistStmt.executeUpdate();
					}
					rs.close();
				}
			}
		}

	}

	@Override
	public void removeDomain(String domain) throws SQLException
	{
		if (domain != null)
		{
			if (domain.indexOf('\t') > -1)
			{
				java.lang.String[] domains = domain.split("\t");
				for (java.lang.String h : domains)
				{
					blacklistServer(h);
				}
			}
			else
			{
				if (domain.trim().length() > 1)
				{
					checkDomainBlacklistStmt.setString(1, domain);
					java.sql.ResultSet rs = checkDomainBlacklistStmt.executeQuery();
					if (rs.next() == true)
					{
						if (CLASS_LOGGER.isTraceEnabled())
							CLASS_LOGGER.trace("have to remove it from the whitelist: " + domain);
						removeServerBlacklistStmt.setString(1, domain);
						removeServerBlacklistStmt.executeUpdate();
					}
					rs.close();
				}
			}
		}
	}

	@Override
	public boolean check(String host) throws SQLException, UnknownHostException
	{
		if (mine == null)
			mine = new AdBlockProxyWorkerDB(getStatistics(), dbConnection, null, context);
		return mine.determineAddress(host) != null;
	}

	@Override
	public int getWhitelistedServerCount() throws SQLException
	{
		java.sql.ResultSet rs = countServerWhitelistStmt.executeQuery();
		rs.next();
		int rv = rs.getInt(1);
		rs.close();
		return rv;
	}

	@Override
	public int getBlacklistedServerCount() throws SQLException
	{
		java.sql.ResultSet rs = countServerBlacklistStmt.executeQuery();
		rs.next();
		int rv = rs.getInt(1);
		rs.close();
		return rv;
	}

	@Override
	public int getBlacklistedDomainCount() throws SQLException
	{
		java.sql.ResultSet rs = countDomainBlacklistStmt.executeQuery();
		rs.next();
		int rv = rs.getInt(1);
		rs.close();
		return rv;
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