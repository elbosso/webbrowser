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
import com.sun.webkit.dom.EventTargetImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Base64;

public class WebBrowserTab extends javax.swing.JPanel implements java.awt.event.ActionListener
{
    public static final String EVENT_TYPE_CLICK = "click";
	private final static java.lang.String LOCATIONTFACTIONCMD="LOCATIONTFACTIONCMD";
	private final static org.slf4j.Logger CLASS_LOGGER = org.slf4j.LoggerFactory.getLogger(WebBrowserTab.class);
	private static javafx.scene.web.WebEngine webEngine;
	private JFXPanel fxPanel;
	private java.lang.String currentLocation;
	private javax.swing.JTextField locationtf;
	private de.elbosso.proxy.AdBlockProxyWorkerFactoryBerkeleyDB adBlockProxyWorkerFactory;
	private javafx.scene.web.WebView webView;
	private final java.util.Stack<java.lang.String> forwardStack;
	private final java.util.Stack<java.lang.String> backwardStack;
	private javax.swing.Action forwardAction;
	private javax.swing.Action backwardAction;
	private javax.swing.Action reloadAction;
	private javax.swing.Action toggleJavascriptAction;
	private javax.swing.Action toggleJavascriptPopupsAction;
	private CookieManager cm;
	private javax.swing.JComboBox cookieCb;
	private javax.swing.JTable adBlockStatusTable;

	WebBrowserTab(de.elbosso.proxy.AdBlockProxyWorkerFactoryBerkeleyDB adBlockProxyWorkerFactory,CookieManager cm)
	{
		super(new java.awt.BorderLayout());
		this.adBlockProxyWorkerFactory=adBlockProxyWorkerFactory;
		this.cm=cm;
		forwardStack=new java.util.Stack();
		backwardStack=new java.util.Stack();
		createActions();
		initAndShowGUI();
	}
	private void createActions()
	{
		forwardAction=new javax.swing.AbstractAction(null,de.netsysit.util.ResourceLoader.getIcon("toolbarButtonGraphics/navigation/Forward24.gif"))
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				try
				{
					setCurrentLocationImpl(forwardStack.pop());
				} catch (Exception e)
				{
					de.elbosso.util.Utilities.handleException(CLASS_LOGGER,e);
				}
				updateActionStatus();
			}
		};
		forwardAction.setEnabled(false);
		backwardAction=new javax.swing.AbstractAction(null,de.netsysit.util.ResourceLoader.getIcon("toolbarButtonGraphics/navigation/Back24.gif"))
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				if(currentLocation!=null)
					forwardStack.push(currentLocation);
				try
				{
					setCurrentLocationImpl(backwardStack.pop());
				} catch (Exception e)
				{
					de.elbosso.util.Utilities.handleException(CLASS_LOGGER,e);
				}
				updateActionStatus();
			}
		};
		backwardAction.setEnabled(false);
		reloadAction=new javax.swing.AbstractAction(null,de.netsysit.util.ResourceLoader.getIcon("toolbarButtonGraphics/general/Refresh24.gif"))
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							webView.setDisable(true);
							reloadAction.setEnabled(false);
							locationtf.setEnabled(false);
							webEngine.reload();
							adBlockStatusTable.setModel(adBlockProxyWorkerFactory.openStatisticContext(new java.net.URL(webView.getEngine().getLocation()).getHost()));
							adBlockStatusTable.setEnabled(false);
						} catch (java.lang.Throwable t)
						{
							de.elbosso.util.Utilities.handleException(CLASS_LOGGER,t);
						}
					}
				});
			}
		};
		reloadAction.setEnabled(false);
		toggleJavascriptAction=new javax.swing.AbstractAction("JavaScript")
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							webEngine.setJavaScriptEnabled(((java.lang.Boolean)toggleJavascriptAction.getValue(Action.SELECTED_KEY)).booleanValue());
						} catch (java.lang.Throwable t)
						{
							de.elbosso.util.Utilities.handleException(CLASS_LOGGER,t);
						}
					}
				});
			}
		};
		toggleJavascriptAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
		toggleJavascriptPopupsAction=new javax.swing.AbstractAction("JS-Popups")
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
			}
		};
		toggleJavascriptPopupsAction.putValue(Action.SELECTED_KEY, Boolean.FALSE);
	}
	private void updateActionStatus()
	{
		if (CLASS_LOGGER.isTraceEnabled())
		{
			CLASS_LOGGER.trace("backward stack");
			for (java.lang.String b : backwardStack)
				CLASS_LOGGER.trace(b);
			CLASS_LOGGER.trace("end");
			CLASS_LOGGER.trace("forward stack");
			for (java.lang.String f : forwardStack)
				CLASS_LOGGER.trace(f);
			CLASS_LOGGER.trace("end");
		}
		backwardAction.setEnabled(backwardStack.isEmpty()==false);
		forwardAction.setEnabled(forwardStack.isEmpty()==false);
	}
	private void initAndShowGUI()
	{
		// This method is invoked on the EDT thread
		fxPanel = new JFXPanel();
		de.netsysit.ui.components.DockingPanel dockingPanel=new de.netsysit.ui.components.DockingPanel(fxPanel, SwingConstants.VERTICAL);
		add(dockingPanel);
		adBlockStatusTable=new javax.swing.JTable();
		dockingPanel.addDockable(new javax.swing.JScrollPane(adBlockStatusTable),"AdBlock");
		locationtf=new javax.swing.JTextField();
		javax.swing.JPanel topPanel=new javax.swing.JPanel(new java.awt.BorderLayout());
		topPanel.add(locationtf);
		javax.swing.JToolBar tb=new javax.swing.JToolBar();
		tb.setFloatable(false);
		tb.add(reloadAction);
		tb.add(backwardAction);
		tb.add(forwardAction);
		tb.add(new javax.swing.JToggleButton(toggleJavascriptAction));
		tb.add(new javax.swing.JToggleButton(toggleJavascriptPopupsAction));
		cookieCb=new javax.swing.JComboBox(new java.lang.Object[]{CookiePolicy.ACCEPT_NONE,CookiePolicy.ACCEPT_ORIGINAL_SERVER,CookiePolicy.ACCEPT_ALL});
		cookieCb.addActionListener(this);
		tb.add(cookieCb);
		topPanel.add(tb, BorderLayout.EAST);
		add(topPanel, java.awt.BorderLayout.NORTH);
		locationtf.addActionListener(this);
		locationtf.setActionCommand(LOCATIONTFACTIONCMD);
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					initFX(fxPanel);
				} catch (java.lang.Throwable t)
				{
					de.elbosso.util.Utilities.handleException(CLASS_LOGGER,t);
				}
			}
		});
	}

	private void initFX(JFXPanel fxPanel)
	{
		// This method is invoked on the JavaFX thread
		Scene scene = createScene();
		fxPanel.setScene(scene);
	}

	private Scene createScene()
	{
		Group root = new Group();
		Scene scene = new Scene(root, Color.ALICEBLUE);
		webView = new javafx.scene.web.WebView();

		root.getChildren().add(webView);
		webView.setMinSize(1280, 1024);
		webView.setMaxSize(1280, 1024);

		// Obtain the webEngine to navigate
		webEngine = webView.getEngine();
		webEngine.locationProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				System.out.println("observable "+observable.getValue()+" "+oldValue+" "+newValue);

			}
		});

		webEngine.setJavaScriptEnabled(false);
		if (CLASS_LOGGER.isTraceEnabled())
			CLASS_LOGGER.trace("installing listener");
		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
			@Override
			public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
				if (newState == Worker.State.SUCCEEDED) {
					if (CLASS_LOGGER.isTraceEnabled())
						CLASS_LOGGER.trace("Succeeded!");
					adBlockProxyWorkerFactory.closeStatisticContext();
					webView.setDisable(false);
					webView.requestFocus();
					reloadAction.setEnabled(true);
					locationtf.setEnabled(true);
					adBlockStatusTable.setEnabled(true);
					EventListener listener = new EventListener() {
						@Override
						public void handleEvent(Event ev) {
							String domEventType = ev.getType();
							if (domEventType.equals(EVENT_TYPE_CLICK)) {
								if (CLASS_LOGGER.isTraceEnabled())
									CLASS_LOGGER.trace("click event: "+ev.getTarget());
								String href = ((org.w3c.dom.Element)ev.getTarget()).getAttribute("href");
								if (CLASS_LOGGER.isTraceEnabled())
									CLASS_LOGGER.trace("href raw: "+href);
								if(href!=null)
									href=ev.getTarget().toString();
								if(href==null)
								{
									EventTarget et=ev.getTarget();
									if(org.w3c.dom.Node.class.isAssignableFrom(et.getClass()))
									{
										org.w3c.dom.Node node=(org.w3c.dom.Node)et;
										node=node.getParentNode();
										while(node!=null)
										{
											if(Element.class.isAssignableFrom(node.getClass()))
											{
												Element elem=(Element)node;
												if(elem.getTagName().equalsIgnoreCase("a"))
												{
													href = elem.toString();
													break;
												}
											}
											node=node.getParentNode();
										}
									}
								}
								if(href!=null)
								{
									if (CLASS_LOGGER.isTraceEnabled())
										CLASS_LOGGER.trace("href used: "+href);
									try
									{
										setCurrentLocation(href);
										ev.preventDefault();
										ev.stopPropagation();
									} catch (Exception e)
									{
										de.elbosso.util.Utilities.handleException(CLASS_LOGGER, e);
									}
								}
							}
						}
					};

					Document doc = webEngine.getDocument();
					NodeList nodeList = doc.getElementsByTagName("a");
					for (int i = 0; i < nodeList.getLength(); i++) {
						org.w3c.dom.Node node=nodeList.item(i);
						if(EventTarget.class.isAssignableFrom(node.getClass()))
							((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
						//((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
						//((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
					}
				}
			}
		});
		webEngine.setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {

			@Override
			public WebEngine call(PopupFeatures p) {
				WebEngine rv=null;
				if(((java.lang.Boolean)toggleJavascriptAction.getValue(Action.SELECTED_KEY)).booleanValue())
				{
					Stage stage = new Stage(StageStyle.UTILITY);
					WebView wv2 = new WebView();
					wv2.getEngine().setJavaScriptEnabled(((java.lang.Boolean) toggleJavascriptPopupsAction.getValue(Action.SELECTED_KEY)).booleanValue());
					stage.setScene(new Scene(wv2));
					stage.show();
					rv = wv2.getEngine();
				}
				else
				{
					de.elbosso.util.Utilities.handleError(CLASS_LOGGER,WebBrowserTab.this,null,"Information","Page wanted to open a Popup but this is currently disabled!");
				}
				return rv;
			}
		});
		return (scene);
	}

	private java.io.File determineDownloadFolder()
	{
		File file = new File(System.getProperty("user.home") + "/Download/");
		java.lang.String xdgConfigDirName=System.getenv("XDG_DOWNLOAD_DIR");
		if(xdgConfigDirName!=null)
		{
			file = new java.io.File(xdgConfigDirName);
			file.mkdirs();
		}
		else
		{
			if((file.exists()==false)||(file.isDirectory()==false))
			{
				java.io.File f = new File(System.getProperty("user.home") + "/Downloads/");
				if((f.exists())&&(f.isDirectory()))
					file=f;
				else
					file.mkdirs();
			}
		}
		java.io.File downloadFolder=file;
		return downloadFolder;
	}
	private void download(java.net.URI uri)
	{
		java.lang.String newValue=uri.toString();
		String name=newValue.substring(newValue.lastIndexOf('/')+1);
		new Thread()
		{
			public void run()
			{
				try {
					java.io.File downloadFolder=determineDownloadFolder();
					File download = new File(downloadFolder,name);
					if(download.exists()) {
						de.netsysit.ui.dialog.GeneralPurposeInfoDialog.showInformation(null,"Download already exists!","What you're trying to download already exists ("+name+")");
//                                Dialogs.create().title("Exists").message("What you're trying to download already exists").showInformation();
						return;
					}
					URL url = uri.toURL();
					URLConnection urlConnection = url.openConnection();
					urlConnection.connect();
					long file_size = urlConnection.getContentLength();
					if(file_size<0)
					{
						final String contentLengthStr=urlConnection.getHeaderField("content-length");
						if(contentLengthStr!=null)
							file_size= Long.parseLong(contentLengthStr);
					}
					//GeneralPurposeInfoDialog.showInformation(null,"Download","Downloading "+name+"...\n"+nf.format(file_size)+" Bytes");
					InputStream is=urlConnection.getInputStream();
					OutputStream os=new FileOutputStream(download);
					doTheActualDownload(is,os,file_size,name);
					//de.netsysit.ui.dialog.GeneralPurposeInfoDialog.showInformation(null,"Download","Download is completed your download will be in: " + download.getAbsolutePath());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void doTheActualDownload(InputStream is, OutputStream os,long file_size,java.lang.String name) throws IOException, InterruptedException
	{
		NumberFormat nf=new DecimalFormat("#,###");
		de.elbosso.util.io.StreamCopier stoppable= de.elbosso.util.io.Utilities.prepareCopyBetweenStreams(is,os);
		Thread t=new Thread(stoppable);
		t.start();
		long start=System.currentTimeMillis();
		de.netsysit.ui.dialog.InfoProgressMonitor infoProgressMonitor= de.netsysit.ui.dialog.InfoProgressMonitor.create(null,"Download",0,name);
//                        infoProgressMonitor.showDialog();
		infoProgressMonitor.setCancellable();
		if(file_size>-1)
		{
			infoProgressMonitor.setIndeterminate(false);
			infoProgressMonitor.setMaximum(100);
			infoProgressMonitor.setValue(0);
			infoProgressMonitor.setString("");
		}
		else
			infoProgressMonitor.setIndeterminate(true);

		while(stoppable.isFinished()==false)
		{
			if(infoProgressMonitor.isCancelled())
			{
				stoppable.stop();
				break;
			}
			long sofar = stoppable.getTotal();
			long now = System.currentTimeMillis();
			long seconds = (now - start) / 1000;
			if(file_size>0)
			{
				if (seconds > 0)
				{
					long rate = sofar / seconds;
					int percentage=(int)(sofar*100/file_size);
					long secondsLeft = (file_size - sofar) / java.lang.Math.max(rate, 1l);
					System.out.println(percentage+"% "+nf.format(sofar) + "/" + nf.format(file_size) + " - " + nf.format(rate) + "/s, " + nf.format(secondsLeft) + "s left");
					javax.swing.SwingUtilities.invokeLater(new java.lang.Runnable()
					{
						@Override
						public void run()
						{
							infoProgressMonitor.setValue(percentage);
							infoProgressMonitor.setString(nf.format(rate) + "/s, " + nf.format(secondsLeft) + "s left");
						}
					});
				}
				if (sofar < file_size)
				{
					java.lang.Thread.currentThread().sleep(500);
				}
			}
			else
			{
				if (seconds > 0)
				{
					long rate = sofar / seconds;
					System.out.println(nf.format(sofar) + " - " + nf.format(rate) + "/s");
					javax.swing.SwingUtilities.invokeLater(new java.lang.Runnable()
					{
						@Override
						public void run()
						{
							infoProgressMonitor.setString(nf.format(rate) + "/s, " + nf.format(sofar) );
						}
					});
				}
				java.lang.Thread.currentThread().sleep(100);
			}
		}
		javax.swing.SwingUtilities.invokeLater(new java.lang.Runnable()
		{
			@Override
			public void run()
			{
				infoProgressMonitor.setValue(100);
			}
		});
		t.join();
	}

	public void setCurrentLocation(String currentLocation) throws Exception
	{
		forwardStack.clear();
		setCurrentLocationImpl(currentLocation);
	}
	private void setCurrentLocationImpl(String currentLocation) throws Exception
	{
		if (CLASS_LOGGER.isTraceEnabled())
									CLASS_LOGGER.trace("setCurrentLocation: "+currentLocation);
		if(currentLocation.equals("javascript:history.back()"))
			de.elbosso.util.Utilities.performAction(this,backwardAction);
		else if(currentLocation.toUpperCase().startsWith("JAVASCRIPT:"))
		{
			de.elbosso.util.Utilities.handleError(CLASS_LOGGER,this,null,"Error","Javascript not allowed as URI scheme!");
		}
		else
		{
			java.util.List<String> downloadableExtensions = Arrays.asList(".vid",".doc", ".xls", ".zip", ".exe", ".rar", ".pdf", ".jar", ".png", ".jpg", ".gif",".iso");
			java.lang.String extension=currentLocation.substring(currentLocation.lastIndexOf('.') );
			System.out.println(extension+" "+(downloadableExtensions.contains(extension)));
			if (downloadableExtensions.contains(extension)) {
				webView.setDisable(false);
				webView.requestFocus();
				try
				{
					download(new java.net.URI(currentLocation));
				}
				catch(java.lang.Throwable t)
				{
					de.elbosso.util.Utilities.handleException(null,t);
				}
			}
			else if(currentLocation.startsWith("data:"))
			{
				webView.setDisable(false);
				webView.requestFocus();
				new Thread()
				{
					public void run()
					{
						try {
							String value=currentLocation.substring(5);
							int index=value.indexOf(',');
							String encoded=value.substring(index+1);
							String meta=value.substring(0,index);
							String[] parts=meta.split(";");
							System.out.println(parts.length);
							String mime=parts[0];
							String charset= Charset.defaultCharset().name();
							boolean base64=false;
							if(parts.length>1)
							{
								if (parts[1].startsWith("charset="))
									charset = parts[1].substring("charset=".length());
								else if (parts[1].equals("base64"))
									base64 = true;
							}
							if(parts.length>2)
							{
								if (parts[2].startsWith("charset="))
									charset = parts[2].substring("charset=".length());
								else if (parts[2].equals("base64"))
									base64 = true;
							}
							System.out.println(mime);
							System.out.println(charset);
							System.out.println(base64);
							String result = null;
							if(base64)
							{
								Base64.getDecoder().decode(encoded.getBytes(charset));
							}
							else
							{
								result= URLDecoder.decode(encoded, charset);
							}
							System.out.println(result);
							JFileChooser fc=new JFileChooser();
							fc.setCurrentDirectory(determineDownloadFolder());
							fc.showSaveDialog(null);
							File f=fc.getSelectedFile();
							if(f!=null)
							{
								OutputStream os = new FileOutputStream(f);
								InputStream is = new ByteArrayInputStream(result.getBytes());
								doTheActualDownload(is,os,-1,f.getName());
							}
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
			else
			{
				java.lang.String old = getCurrentLocation();
				this.currentLocation = currentLocation;
				if (old != null)
				{
					if (((backwardStack.isEmpty()) || (backwardStack.peek().equals(old) == false)) &&
							((forwardStack.isEmpty()) || (forwardStack.peek().equals(old) == false)))
						backwardStack.push(old);
					updateActionStatus();
				}
				if ((this.currentLocation != null) && (this.currentLocation.trim().length() > 0))
				{
					java.net.URL url = null;
					try
					{
						url = new java.net.URL(this.currentLocation);
					} catch (java.net.MalformedURLException exp)
					{
						try
						{
							url = new java.net.URL("http://" + this.currentLocation);
						} catch (java.net.MalformedURLException innerexp)
						{
							de.elbosso.util.Utilities.handleException(CLASS_LOGGER, innerexp);
						}
					}
					if (url != null)
					{
						adBlockProxyWorkerFactory.whitelistServer(url.getHost());
						adBlockStatusTable.setModel(adBlockProxyWorkerFactory.openStatisticContext(url.getHost()));
						loadPage(url.toString());
					}
				}
				firePropertyChange("currentLocation", old, getCurrentLocation());
			}
		}
	}

	public String getCurrentLocation()
	{
		return currentLocation;
	}

	private void loadPage(final String location)
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					webView.setDisable(true);
					reloadAction.setEnabled(false);
					locationtf.setEnabled(false);
					adBlockStatusTable.setEnabled(false);
					locationtf.removeActionListener(WebBrowserTab.this);
					locationtf.setText(location);
					locationtf.addActionListener(WebBrowserTab.this);
					webEngine.load(location);
/*					webEngine.getLoadWorker().stateProperty().addListener(
							new javafx.beans.value.ChangeListener<javafx.concurrent.Worker.State>()
							{
								public void changed(javafx.beans.value.ObservableValue ov, javafx.concurrent.Worker.State oldState, javafx.concurrent.Worker.State newState)
								{
									if (newState == javafx.concurrent.Worker.State.SUCCEEDED)
									{
										de.elbosso.util.Utilities.sopln("ready! "+java.lang.Thread.currentThread());
									}
								}
							});
*/				} catch (java.lang.Throwable t)
				{
					de.elbosso.util.Utilities.handleException(CLASS_LOGGER,t);
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getActionCommand().equals(LOCATIONTFACTIONCMD))
		{
			try
			{
				setCurrentLocation(locationtf.getText());
			} catch (Throwable ex)
			{
				de.elbosso.util.Utilities.handleException(CLASS_LOGGER,ex);
			}
		}
		if(e.getSource()==cookieCb)
		{
			cm.setCookiePolicy((CookiePolicy) cookieCb.getSelectedItem());
		}
	}
}
