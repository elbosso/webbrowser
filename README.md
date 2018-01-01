# webbrowser

Tech demo and prototype for what i would call a safe browser thats kinda UN*X inspired: Todays
    browsers all forgot "You hab one job!"
    
I got inspired by a blogpost that informed me of what probably
many people know now: that mozilla autonatically rolled out an addon,
that displayed ads in pages the user visited.

This is unacceptable.

Therefore i dusted off an old project i had lying around: an HTTP-Proxy.
at the time of its inception, it was merely a practice project and
one that had the goal to be able to debug HTTP connections.

Now i reworked it a bit and it grew to a fully-featured transparent
proxy. That means that it is still a HTTP proxy (as opposed
to a SOCKS proxy for example) but one can run both HTTP and HTTPS through it.

Transparent means that it does not comrpomise HTTPS connections:
The proxy does only establich the connection on a socket level - the
both communication partners then establish the crypto channel on this
foundation.

So the proxy wont ever look into confidential communication.

With that in place - i slapped on a JavaFX-WebView for displaying
content and some means for blacklisting and whitelisting hosts and domains.

So now the features of this webbroser demonstrator is: 
 * no multiple tabs
 * javascript is always deactivated at the start but can be activated
 * cookies are always disallowed at the start but can be allowed
 * whenever a page is explicitly opened (by using the location textfield or by clicking on a link) - the server that is hosting the page is automatically whitelisted
 * all other pages this page wants to load resources from are automatically blacklisted if they are not explicitly whitelisted
 * blacklisted and whitelisted servers as well as blacklisted domains are stored in a relational database
 * the webbrowser offers extensive logging, currently only configurable via source code changes
 * the webbrowser offers a gui for easily white-/blacklisting arbitrary hosts
 * the webbrowser offers an extensive JMX interface for monitoring its operation

[WARN]
----
This code is by no means production ready - it is meant to
be a foundation on which new ideas are tested!
----

However - im always grateful for input - sodont hesitate to 
leave behind comments or even issues.

One last word (well - a last line actually): The command for building is
```bash
mvn package assembly:single
``` 

And the command for executing (with Oracles JVM - OpenJVM does not seen to bring JavaFX with it?) is:
```bash
java -jar target/webbrowser-1.0-SNAPSHOT-jar-with-dependencies.jar
```
 