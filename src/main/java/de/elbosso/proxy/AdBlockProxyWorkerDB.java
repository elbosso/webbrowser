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

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

/**
 *
 * @author elbosso
 */
public class AdBlockProxyWorkerDB extends de.elbosso.util.net.proxy.ProxyWorker
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AdBlockProxyWorkerDB.class);
	final static java.lang.String checkServerWhitelistSql="SELECT name from SERVERWHITELIST WHERE NAME LIKE ?";
	final static java.lang.String checkServerBlacklistSql="SELECT name from SERVERBLACKLIST WHERE NAME LIKE ?";
	final static java.lang.String checkDomainBlacklistSql="SELECT * from DOMAINBLACKLIST WHERE LOCATE (name,?,0) AND LENGTH(?)-LENGTH(name)-LOCATE (name,?,0) =-1";
	final static java.lang.String insertServerBlacklistSql="INSERT INTO SERVERBLACKLIST (NAME) VALUES(?)";
	final static java.lang.String insertServerWhitelistSql="INSERT INTO SERVERWHITELIST (NAME) VALUES(?)";
	final static java.lang.String insertDomainBlacklistSql="INSERT INTO DOMAINBLACKLIST (NAME) VALUES(?)";
	final static java.lang.String removeServerBlacklistSql="DELETE FROM SERVERBLACKLIST WHERE NAME LIKE ?";
	final static java.lang.String removeServerWhitelistSql="DELETE FROM SERVERWHITELIST WHERE NAME LIKE ?";
	final static java.lang.String countServerWhitelistSql="SELECT COUNT(*) FROM SERVERWHITELIST";
	final static java.lang.String countServerBlacklistSql="SELECT COUNT(*) FROM SERVERBLACKLIST";
	final static java.lang.String countDomainBlacklistSql="SELECT COUNT(*) FROM DOMAINBLACKLIST";
	private final java.sql.Connection dbConnection;
	private final de.elbosso.proxy.AdBlockWorkerStatistics adBlockWorkerStatistics;
	private final java.sql.PreparedStatement checkServerWhitelistStmt;
	private final java.sql.PreparedStatement checkDomainBlacklistStmt;
	private final java.sql.PreparedStatement checkServerBlacklistStmt;
	private final java.sql.PreparedStatement insertServerBlacklistStmt;
	private boolean notWhitelistedMeansBlacklisted=true;

	public AdBlockProxyWorkerDB(de.elbosso.proxy.AdBlockWorkerStatistics adBlockWorkerStatistics, java.sql.Connection dbConnection, Socket socket, de.elbosso.util.net.proxy.Context context) throws SQLException
	{
		super(socket, context);
		this.adBlockWorkerStatistics=adBlockWorkerStatistics;
		this.dbConnection=dbConnection;
		checkServerWhitelistStmt =dbConnection.prepareStatement(checkServerWhitelistSql);
		checkServerBlacklistStmt =dbConnection.prepareStatement(checkServerBlacklistSql);
		checkDomainBlacklistStmt =dbConnection.prepareStatement(checkDomainBlacklistSql);
		insertServerBlacklistStmt =dbConnection.prepareStatement(insertServerBlacklistSql);
	}

	@Override
	protected synchronized InetAddress determineAddress(String host) throws UnknownHostException
	{
		InetAddress rv= null;

		try
		{
			checkServerWhitelistStmt.setString(1, host);
			java.sql.ResultSet rs = checkServerWhitelistStmt.executeQuery();
			if (rs.next() == true)
			{
				rs.close();
				rv = super.determineAddress(host); //To change body of generated methods, choose Tools | Templates.
				if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("whitelisted host " + host);
				if (adBlockWorkerStatistics != null)
					adBlockWorkerStatistics.passedOn(host);
			}
			else
			{
				rs.close();
				checkServerBlacklistStmt.setString(1, host);
				rs = checkServerBlacklistStmt.executeQuery();
				if (rs.next() == false)
				{
					rs.close();
					checkDomainBlacklistStmt.setString(1, host);
					checkDomainBlacklistStmt.setString(2, host);
					checkDomainBlacklistStmt.setString(3, host);
					rs = checkDomainBlacklistStmt.executeQuery();
					if (rs.next() == false)
					{
						adBlockWorkerStatistics.notInDatabase(host);
						if(notWhitelistedMeansBlacklisted)
						{
							if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("not in data base - gets blacklisted per default... " + host);
							insertServerBlacklistStmt.setString(1,host);
							insertServerBlacklistStmt.executeUpdate();
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
					rs.close();
				}
				else
				{
					rs.close();
					if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("blacklisted host " + host);
					if (adBlockWorkerStatistics != null)
						adBlockWorkerStatistics.blocked(host);
				}
			}
			if (rv == null)
				if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.trace("blocked " + host);
		}
		catch(java.sql.SQLException sqlex)
		{
			if (CLASS_LOGGER.isTraceEnabled()) CLASS_LOGGER.fatal(sqlex.getMessage(),sqlex);
		}
		return rv;
	}

}