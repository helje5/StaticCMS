package org.opengroupware.pubexport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.servlets.WOServletAdaptor;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHolder;

public class pubd extends NSObject {
  protected static final Log log = LogFactory.getLog("OGoPubExport");
  public static String rootPath;
  
  /* main */

  public static void main(String[] args) {
    /* parse properties */

    int port = 8989;
    for (String arg: args) {
      if (arg.startsWith("-Droot="))
        rootPath = arg.substring(7);
      else if (arg.startsWith("-DWOPort="))
        port = Integer.parseInt(arg.substring(9));
    }
    
    /* setup HTTP server */
    
    Server server = new Server(port);
    log.info("application started on HTTP port: " + port);
    
    /* create a context */

    org.mortbay.jetty.servlet.Context root = 
    	new org.mortbay.jetty.servlet.Context(server, "/",
    			org.mortbay.jetty.servlet.Context.NO_SESSIONS |
    			org.mortbay.jetty.servlet.Context.NO_SECURITY);
    
    /* a ServletHolder wraps a Servlet configuration in Jetty */
    String _appName = "org.opengroupware.pubexport.OGoPubExport";
    ServletHolder servletHolder = new ServletHolder(WOServletAdaptor.class);
    servletHolder.setName(_appName);
    servletHolder.setInitParameter("WOAppName", _appName);
    
    /* This makes the Servlet being initialize on startup (instead of first
     * request).
     */
    servletHolder.setInitOrder(10); /* positive values: init asap */
    
    /* add Servlet to the Jetty Context */
    
    root.addServlet(servletHolder, "/");
    
    /* start server */
    
    log.debug("starting Jetty ...");
    try {
      server.start();
      log.debug("Jetty is running ...");
    }
    catch (Exception e) {
      log.error("Jetty exception", e);
    }
  }

}
