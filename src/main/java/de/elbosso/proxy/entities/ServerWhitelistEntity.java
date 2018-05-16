package de.elbosso.proxy.entities;

import java.util.Date;

@com.sleepycat.persist.model.Entity
public class ServerWhitelistEntity
{
	@com.sleepycat.persist.model.PrimaryKey
	private java.lang.String name;
	private java.util.Date timestamp;

	public Date getTimestamp()
	{
		return timestamp;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}
}
