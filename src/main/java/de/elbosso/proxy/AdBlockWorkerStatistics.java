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

import de.netsysit.util.threads.CubbyHole;

import javax.management.*;
import java.beans.PropertyChangeListener;

/**
 *
 * @author elbosso
 */
public class AdBlockWorkerStatistics extends java.lang.Object implements Runnable
,AdBlockWorkerStatisticsMBean
,de.netsysit.util.beans.PropertyChangeSender
	,javax.management.NotificationBroadcaster
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(AdBlockWorkerStatistics.class);
	private de.netsysit.util.threads.CubbyHole<Capsule> ch;
	private java.util.Set<java.lang.String> blocked;
	private java.util.Set<java.lang.String> passedOn;
	private java.util.Set<java.lang.String> notInDatabase;
	private final java.lang.Object blockedMonitor;
	private final java.lang.Object passedOnMonitor;
	private final java.lang.Object notInDatabaseMonitor;
	private java.beans.PropertyChangeSupport pcs;
	private java.util.Map<java.lang.String, java.util.Map<Type,java.util.Set<java.lang.String> > >contextMap;
	private java.util.Map<Type,java.util.Set<java.lang.String> > currentContext;
	private java.util.Map<java.lang.String,TableModel> tableModels;
	private String currentContextName;
	private final AdBlockProxyWorkerFactoryDB factory;
	private javax.management.NotificationBroadcasterSupport notificationBroadcasterSupport;
	private long notificationSequence = 0;

	AdBlockWorkerStatistics()
	{
		this(null);
	}
	AdBlockWorkerStatistics(AdBlockProxyWorkerFactoryDB factory)
	{
		super();
		this.factory=factory;
		ch=new de.netsysit.util.threads.SimpleBufferingCubbyHole<Capsule>();
		blocked=new de.elbosso.util.lang.CaseInsensitiveHashSetForStrings();
		passedOn=new de.elbosso.util.lang.CaseInsensitiveHashSetForStrings();
		notInDatabase=new de.elbosso.util.lang.CaseInsensitiveHashSetForStrings();
		blockedMonitor=new java.lang.Object();
		passedOnMonitor=new java.lang.Object();
		notInDatabaseMonitor=new java.lang.Object();
		pcs=new java.beans.PropertyChangeSupport(this);
		contextMap=new java.util.HashMap();
		tableModels=new java.util.HashMap();
		notificationBroadcasterSupport=new javax.management.NotificationBroadcasterSupport();
	}

	public CubbyHole getCh()
	{
		return ch;
	}
	
	public void blocked(java.lang.String name)
	{
		ch.put(new Capsule(Type.BLOCKED, name));
		Notification notif=new Notification(
				"AdBlockWorkerStatistics.blocked",     // type
				this,              // source
				++notificationSequence,     // seq. number
				name
		);
//		notif.setUserData(uri);
		notificationBroadcasterSupport.sendNotification(notif);
	}
	public void passedOn(java.lang.String name)
	{
		ch.put(new Capsule(Type.PASSEDON, name));
		Notification notif=new Notification(
				"AdBlockWorkerStatistics.passedOn",     // type
				this,              // source
				++notificationSequence,     // seq. number
				name
		);
//		notif.setUserData(uri);
		notificationBroadcasterSupport.sendNotification(notif);
	}
	public void notInDatabase(java.lang.String name)
	{
		ch.put(new Capsule(Type.NOTINDATABASE, name));
		Notification notif=new Notification(
				"AdBlockWorkerStatistics.notInDatabase",     // type
				this,              // source
				++notificationSequence,     // seq. number
				name
		);
//		notif.setUserData(uri);
		notificationBroadcasterSupport.sendNotification(notif);
	}
	public int getBlockedCount()
	{
		int rv=0;
		synchronized(blockedMonitor)
		{
			rv=blocked.size();
		}
		return rv;
	}
	public int getPassedOnCount()
	{
		int rv=0;
		synchronized(passedOnMonitor)
		{
			rv=passedOn.size();
		}
		return rv;
	}
	public int getNotInDatabaseCount()
	{
		int rv=0;
		synchronized(notInDatabaseMonitor)
		{
			rv=notInDatabase.size();
		}
		return rv;
	}
	public void resetBlocked()
	{
		synchronized(blockedMonitor)
		{
			int old=getBlockedCount();
			blocked.clear();
			pcs.firePropertyChange("blockedCount", old, getBlockedCount());
		}
	}
	public void resetPassedOn()
	{
		synchronized(passedOnMonitor)
		{
			int old=getPassedOnCount();
			passedOn.clear();
			pcs.firePropertyChange("passedOnCount", old, getPassedOnCount());
		}
	}
	public void resetNotInDatabase()
	{
		synchronized(blockedMonitor)
		{
			int old=getNotInDatabaseCount();
			notInDatabase.clear();
			pcs.firePropertyChange("notInDatabaseCount", old, getNotInDatabaseCount());
		}
	}
	public java.lang.String[] getBlocked()
	{
		java.lang.String[] rv=null;
		synchronized(blockedMonitor)
		{
			rv=blocked.toArray(new java.lang.String[0]);
		}
		return rv;
	}	
	public java.lang.String[] getPassedOn()
	{
		java.lang.String[] rv=null;
		synchronized(passedOnMonitor)
		{
			rv=passedOn.toArray(new java.lang.String[0]);
		}
		return rv;
	}	
	public java.lang.String[] getNotInDatabase()
	{
		java.lang.String[] rv=null;
		synchronized(notInDatabaseMonitor)
		{
			rv=notInDatabase.toArray(new java.lang.String[0]);
		}
		return rv;
	}
	public void run()
	{
		while(true)
		{
			try
			{
				Capsule capsule=ch.get();
				if(capsule!=null)
				{
					switch(capsule.getType())
					{
						case BLOCKED:
						{
							synchronized(blockedMonitor)
							{
								int old=getBlockedCount();
								blocked.add(capsule.getContent());
								pcs.firePropertyChange("blockedCount", old, getBlockedCount());
							}
							break;
						}
						case PASSEDON:
						{
							synchronized(passedOnMonitor)
							{
								int old=getPassedOnCount();
								passedOn.add(capsule.getContent());
								pcs.firePropertyChange("passedOnCount", old, getPassedOnCount());
							}
							break;
						}
						case NOTINDATABASE:
						{
							synchronized(notInDatabaseMonitor)
							{
								int old=getNotInDatabaseCount();
								notInDatabase.add(capsule.getContent());
								pcs.firePropertyChange("notInDatabaseCount", old, getNotInDatabaseCount());
							}
							break;
						}
					}
					java.util.Map<Type,java.util.Set<java.lang.String> >workContext=null;
					java.lang.String workContextName=null;
					if(currentContext!=null)
					{
						workContext=currentContext;
						workContextName=currentContextName;
					}
					else
					{
						if(capsule.getContextName()!=null)
						{
							workContextName=capsule.getContextName();
							workContext=contextMap.get(workContextName);
						}
					}
					if(workContext!=null)
					{
						for(Type type:Type.values())
						{
							if(type==capsule.getType())
							{
								if(workContext.containsKey(type)==false)
									workContext.put(type,new java.util.HashSet());
								java.util.Set<java.lang.String> set=workContext.get(type);
								set.add(capsule.getContent());
							}
							else
							{
								if(workContext.containsKey(type))
								{
									java.util.Set<java.lang.String> set=workContext.get(type);
									set.remove(capsule.getContent());
								}
							}
						}
						if(tableModels.containsKey(workContextName))
						{
							tableModels.get(workContextName).update();
						}
					}
				}
			}
			catch(java.lang.InterruptedException exp)
			{
				
			}
		}
	}

	public void addPropertyChangeListener(String name, PropertyChangeListener l)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public void removePropertyChangeListener(String name, PropertyChangeListener l)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public javax.swing.table.TableModel openContext(String host)
	{
		javax.swing.table.TableModel rv=null;
		if(host!=null)
		{
			if (contextMap.containsKey(host) == false)
				contextMap.put(host, new java.util.HashMap());
			currentContext = contextMap.get(host);
			currentContextName = host;
			if (tableModels.containsKey(currentContextName) == false)
			{
				TableModel tableModel = new TableModel(currentContextName);
				tableModels.put(currentContextName, tableModel);
			}
			tableModels.get(currentContextName).update();
			rv=tableModels.get(currentContextName);
		}
		return rv;
	}

	public void closeContext()
	{
		currentContext=null;
		currentContextName=null;
	}
	public void addContextListener(java.lang.String name,javax.swing.event.TableModelListener listener)
	{
		if(tableModels.containsKey(name)==false)
		{
			TableModel tableModel = new TableModel(name);
			tableModels.put(name, tableModel);
		}
		tableModels.get(name).addTableModelListener(listener);
	}
	public void removeContextListener(java.lang.String name,javax.swing.event.TableModelListener listener)
	{
		if(tableModels.containsKey(name))
		{
			tableModels.get(name).removeTableModelListener(listener);
		}
	}
	private enum Type
	{
		BLOCKED,PASSEDON,NOTINDATABASE;
	}
	private class Capsule extends java.lang.Object
	{
		private final Type type;
		private final java.lang.String content;
		private final java.lang.String contextName;

		public Capsule(Type type, String content)
		{
			this(type,content,null);
		}
		public Capsule(Type type, String content,String contextName)
		{
			super();
			this.type = type;
			this.content = content;
			this.contextName=contextName;
		}

		public Type getType()
		{
			return type;
		}

		public String getContent()
		{
			return content;
		}

		public String getContextName()
		{
			return contextName;
		}
	}
	class TableModel extends de.netsysit.model.table.EventHandlingSupport
	{
		private java.util.List<java.lang.String> names;
		private java.lang.String myName;

		public TableModel(java.lang.String name)
		{
			super();
			myName=name;
			names=new java.util.LinkedList();
		}

		@Override
		public int getRowCount()
		{
			return names.size();
		}

		@Override
		public int getColumnCount()
		{
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			return columnIndex==0?"blocked":"host";
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return columnIndex==0?java.lang.Boolean.class:java.lang.String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex==0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			java.lang.Object rv=null;
			java.lang.String name=names.get(rowIndex);
			switch(columnIndex)
			{
				case 0:
				{
					if(contextMap.containsKey(myName))
						if(contextMap.get(myName).containsKey(Type.BLOCKED))
							rv=contextMap.get(myName).get(Type.BLOCKED).contains(name);
					break;
				}
				case 1:
				{
					rv=name;
					break;
				}
			}
			return rv;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if (CLASS_LOGGER.isTraceEnabled())
				CLASS_LOGGER.trace(aValue);
			java.lang.String name=getValueAt(rowIndex,1).toString();
			openContext(myName);
			if(((java.lang.Boolean)aValue).booleanValue())
			{
				ch.put(new Capsule(Type.BLOCKED, name, myName));
				if(factory!=null)
					try
					{
						factory.blacklistServer(name);
					}
					catch(java.sql.SQLException exp)
					{
						de.elbosso.util.Utilities.handleException(CLASS_LOGGER,exp);
					}
			}
			else
			{
				ch.put(new Capsule(Type.PASSEDON, name, myName));
				if(factory!=null)
					try
					{
						factory.whitelistServer(name);
					}
					catch(java.sql.SQLException exp)
					{
						de.elbosso.util.Utilities.handleException(CLASS_LOGGER,exp);
					}
			}
			closeContext();
		}
		void update()
		{
			names.clear();
			for(Type type:Type.values())
			{
				if(type!=Type.NOTINDATABASE)
					if(contextMap.get(myName).containsKey(type))
						names.addAll(contextMap.get(myName).get(type));
			}
			java.util.Collections.sort(names);
			fireTableChanged();
		}
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
								{"AdBlockWorkerStatistics.blocked","AdBlockWorkerStatistics.passedOn","AdBlockWorkerStatistics.notInDatabase"}, // notif. types
						Notification.class.getName(), // notif. class
						"AdBlockWorkerStatistics information"     // description
				)
		};
	}
}
