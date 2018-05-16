package de.elbosso.proxy;

public interface AdBlockProxyWorkerFactory extends de.elbosso.util.net.proxy.ProxyWorkerFactory
{
	public void whitelistServer(String host) throws Exception;
	public void blacklistServer(String host) throws Exception;
	public void removeServer(String host) throws Exception;
	public void blacklistDomain(String domain) throws Exception;
	public void removeDomain(String domain) throws Exception;

}