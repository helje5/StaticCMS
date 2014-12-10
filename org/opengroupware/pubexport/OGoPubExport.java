package org.opengroupware.pubexport;

import java.io.File;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.ofs.OFSApplication;
import org.getobjects.ofs.fs.IOFSFileManager;
import org.getobjects.ofs.fs.OFSHostFileManager;
import org.opengroupware.pubexport.documents.OGoPubFactory;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/*
 * OGoPubExport
 * 
 * Main entry point for the publisher.
 */
public class OGoPubExport extends OFSApplication {
  protected static final Log log = LogFactory.getLog("OGoPubExport");

  public OGoPubWebSite root;
  
  /* setup */

  @Override
  public void init() {
    super.init();
    
    this.defaultRestorationFactory = new OGoPubFactory();
  }
  
  /* caching */
  
  public boolean isCachingEnabled() {
    return false;
  }
  
  /* app init */
  
  public String ofsDatabasePathInContext(WOContext _ctx, String[] _path) {
    String p = pubd.rootPath;
    if (p != null && p.length() > 0)
      return p;

    return System.getProperty("user.dir");
  }
  
  public synchronized Object ofsRootObjectInContext
    (WOContext _ctx, String[] _path)
  {
    if (this.root == null) {
      File rootdir =
        new File(this.ofsDatabasePathInContext(_ctx, _path));
      
      IOFSFileManager fm = new OFSHostFileManager(rootdir);
      if (fm == null) {
        log().error("could not create filemanager for file: " + rootdir);
        return null;
      }
      
      // TBD: we manually select the class, should be mysite.website
      //      where .website maps to OGoPubWebSite?!
      this.root = new OGoPubWebSite();
      this.root.setStorageLocation(fm, null /* root path */);
      this.root.setLocation(null, null); /* we are root */
      this.root.awakeFromRestoration(defaultRestorationFactory(), 
          null, fm, null, _ctx);
      
      this.root.loadProperties();
    }
    return this.root;
  }
  
  /* caches */
  
  public synchronized void resetRootDirectory() {
    this.root = null;
  }
  
  protected long lastReset = 0;
  
  @Override
  public WOResponse dispatchRequest(WORequest _rq) {
    long now = new Date().getTime();
    if (this.lastReset == 0 || (now - lastReset > 1500)) {  
      // avoid exccessive reloads on image pages
      this.resetRootDirectory();
      log.info("flushing caches ...");
    }
    
    return super.dispatchRequest(_rq);
  }
  
  /* logging */
  
  public Log log() {
    return log;
  }
  
}
