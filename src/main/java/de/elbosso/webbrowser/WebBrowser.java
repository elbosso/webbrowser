package de.elbosso.webbrowser;

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

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import java.awt.event.WindowEvent;
import org.slf4j.event.Level;

public class WebBrowser extends java.lang.Object implements java.awt.event.WindowListener
{
	private final static org.slf4j.Logger CLASS_LOGGER = org.slf4j.LoggerFactory.getLogger(WebBrowser.class);
	private javafx.scene.web.WebEngine webEngine;
	private WebBrowserTab frontTab;
	private de.elbosso.proxy.AdBlockProxyWorkerFactoryBerkeleyDB abpwf;
	private de.elbosso.util.net.proxy.SimpleProxyServer proxyServer;
	private javax.swing.JFrame frame;
	private java.net.CookieManager cm;

	private void initAndShowGUI()
	{
		// This method is invoked on the EDT thread
		frame = new javax.swing.JFrame("Swing and JavaFX");
		javax.swing.JPanel jp = new javax.swing.JPanel(new java.awt.BorderLayout());
		javax.swing.JTabbedPane tabs=new javax.swing.JTabbedPane();
		frontTab=new WebBrowserTab(abpwf,cm);
		jp.add(tabs);
		tabs.addTab("",frontTab);
		frame.add(jp);
		frame.setSize(1280, 1024);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(this);
	}

	public static void main(String[] args) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, Exception, ClassNotFoundException
	{
		de.elbosso.util.Utilities.configureBasicStdoutLogging(Level.TRACE);
		try
		{
			java.util.Properties iconFallbacks = new java.util.Properties();
			java.io.InputStream is=de.netsysit.util.ResourceLoader.getResource("de/elbosso/ressources/data/icon_trans_material.properties").openStream();
			iconFallbacks.load(is);
			is.close();
			de.netsysit.util.ResourceLoader.configure(iconFallbacks);
		}
		catch(java.io.IOException ioexp)
		{
			ioexp.printStackTrace();
		}
		Class.forName("org.hsqldb.jdbcDriver");
		new WebBrowser();
	}
	WebBrowser() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, Exception
	{
		cm=new CookieManager();
		cm.setCookiePolicy(CookiePolicy.ACCEPT_NONE);
		java.net.CookieHandler.setDefault(cm);
		javax.management.MBeanServer mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
		javax.management.ObjectName name = new javax.management.ObjectName(CookieManager.class.getName()+":type=CookieManager");
		mbs.registerMBean(cm, name);
		de.netsysit.util.threads.ThreadManager threadManager = new de.netsysit.util.threads.ThreadManager(de.elbosso.util.net.proxy.SimpleProxyServer.class.getName(), -1);
		name = new javax.management.ObjectName(de.elbosso.util.net.proxy.SimpleProxyServer.class.getName()+":type=ThreadManager");
		mbs.registerMBean(threadManager, name);
		de.elbosso.util.net.proxy.Context context=new de.elbosso.util.net.proxy.Context(threadManager);
		name = new javax.management.ObjectName(de.elbosso.util.net.proxy.SimpleProxyServer.class.getName()+":type=SocketPool");
		context.registerMBeanForSocketPool(mbs, name);
		proxyServer =new de.elbosso.util.net.proxy.SimpleProxyServer(8088,threadManager);
		abpwf=new de.elbosso.proxy.AdBlockProxyWorkerFactoryBerkeleyDB(context);
		name = new javax.management.ObjectName(de.elbosso.util.net.proxy.SimpleProxyServer.class.getName()+":type=AdBlockProxyWorkerFactoryBerkeleyDB");
		mbs.registerMBean(abpwf, name);
		de.elbosso.proxy.AdBlockWorkerStatistics stats=abpwf.getStatistics();
		name = new javax.management.ObjectName(de.elbosso.util.net.proxy.SimpleProxyServer.class.getName()+":type=AdBlockStatistics");
		mbs.registerMBean(stats, name);
		threadManager.execute(stats);
		proxyServer.setProxyWorkerFactory(abpwf);
		threadManager.execute(proxyServer);
		//de.elbosso.util.net.proxy.ProxySelector.setDefault(new de.elbosso.util.net.proxy.ProxySelector(proxyServer));
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					initAndShowGUI();
					frontTab.setCurrentLocation("http://www.heise.de");
				} catch (Throwable ex)
				{
					de.elbosso.util.Utilities.handleException(CLASS_LOGGER,ex);
				}
			}
		});
	}

	@Override
	public void windowOpened(WindowEvent e)
	{

	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		//actually happens when proxyServer.stop() is called -
		//calling it twice provokes Berkeley DB for java to vomit
/*		try
		{
			abpwf.close();
		} catch (Throwable e1)
		{
			de.elbosso.util.Utilities.handleException(CLASS_LOGGER,e1);
		}
*/		proxyServer.stop();
		frame.dispose();
		frame.setVisible(false);
		System.exit(0);
	}

	@Override
	public void windowClosed(WindowEvent e)
	{

	}

	@Override
	public void windowIconified(WindowEvent e)
	{

	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{

	}

	@Override
	public void windowActivated(WindowEvent e)
	{

	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{

	}
}
