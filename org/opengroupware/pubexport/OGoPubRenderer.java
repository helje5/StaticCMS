package org.opengroupware.pubexport;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.GoAccessDeniedException;
import org.getobjects.appserver.publisher.GoInternalErrorException;
import org.getobjects.appserver.publisher.GoNotFoundException;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.opengroupware.pubexport.documents.OGoPubDirectory;
import org.opengroupware.pubexport.documents.OGoPubDocument;
import org.opengroupware.pubexport.documents.OGoPubHTMLDocument;
import org.opengroupware.pubexport.documents.OGoPubRSSFeed;
import org.opengroupware.pubexport.documents.OGoPubTemplate;
import org.opengroupware.pubexport.documents.OGoPubWebSite;
import org.opengroupware.pubexport.documents.Util;

public class OGoPubRenderer extends NSObject implements IGoObjectRenderer {
  protected static final Log log = LogFactory.getLog("OGoPubRenderer");
  
  public static final OGoPubRenderer sharedRenderer = new OGoPubRenderer();

  /* control rendering */
  
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    if (_object instanceof OGoPubHTMLDocument ||
        _object instanceof OGoPubTemplate     ||
        _object instanceof OGoPubDirectory    ||
        _object instanceof OGoPubDocument     ||
        _object instanceof OGoPubRSSFeed)
      return true;
    
    return false;
  }
  
  /* rendering */

  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    if (_object instanceof OGoPubHTMLDocument)
      return this.renderPubPage((OGoPubHTMLDocument)_object, _ctx);
    
    if (_object instanceof OGoPubTemplate)
      return this.renderPubTemplate((OGoPubTemplate)_object, _ctx);
    
    if (_object instanceof OGoPubRSSFeed)
      return this.renderPubFeed((OGoPubRSSFeed)_object, _ctx);
    
    if (_object instanceof OGoPubDirectory)
      return this.renderPubDirectory((OGoPubDirectory)_object, _ctx);
    
    if (_object instanceof OGoPubDocument)
      return this.renderPubDocument((OGoPubDocument)_object, _ctx);
    
    log.error("cannot render object: " + _object);
    return new GoInternalErrorException("cannot render given object");
  }

  public Exception renderPubDirectory(OGoPubDirectory _dir, WOContext _ctx){
    String[] pi = _ctx.goTraversalPath().pathInfo();
    if (pi != null && pi.length > 0) {
      log.error("could not resolve path: " +
                UString.componentsJoinedByString(pi, "/"));
      return new GoNotFoundException("did not find path");
    }
    else if (log.isDebugEnabled())
      log.debug("JOPATH: " + _ctx.goTraversalPath());
    
    /* result is a directory, redirect to index document */
    Object indexDoc = ((OGoPubDirectory)_dir).indexDocument(_ctx);

    if (indexDoc == null) {
      log.warn("directory has no index document: " + _dir);
      return new GoNotFoundException("directory has no index document");
    }
    
    /* Calculate path, when running inside pubd, this is relative to the
     * full root.
     */
    
    String[] path =
      OGoPubWebSite.exportPathOfObjectUntilStopKey(indexDoc, "isRoot");
    
    // TODO: some devices, eg mobile ones, might have issues here
    WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_FOUND /* Redirect */);
    r.setHeaderForKey("/" + Util.buildURLForPath(path), "location");
    return null /* everything went fine */;
  }
  
  public Exception renderPubDocument(OGoPubDocument _doc, WOContext _ctx) {
    WOResponse r = _ctx.response();
    
    String mimeType = _doc.mimeType();
    r.setStatus(WOMessage.HTTP_STATUS_OK);
    r.setHeaderForKey(mimeType, "content-type");
    
    /* setup caching headers */
    
    Date              now = new Date();
    GregorianCalendar cal = new GregorianCalendar();
    
    cal.setTime(_doc.lastModified());
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "last-modified");
    
    cal.setTime(now);
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "date");
    
    if (mimeType.startsWith("image/"))
      cal.add(Calendar.HOUR, 1);
    else if (mimeType.startsWith("text/css"))
      cal.add(Calendar.MINUTE, 10);
    else
      cal.add(Calendar.SECOND, 5);
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "expires");
    
    /* transfer content */
    
    byte[] content = _doc.content();
    r.setHeaderForKey("" + content.length, "content-length");
    
    /* remember that no headers can be set after streaming got activated */
    // TODO: this should be ensured by WOResponse
    if (!r.enableStreaming())
      log.info("could not enable streaming for doc: " + _doc);

    r.appendContentData(content, content.length);
    return null /* rendering went fine */;
  }
  
  
  /**
   * This method instantiates and runs the WOComponent for the
   * "master template". The master template is the template named 'Main.xtmpl'.
   * This template will be located using the OGoPubResourceManager (which itself
   * is invoked by WOApplication pageWithName()).
   * <p>
   * The document which is passed in is set as the mainPage 'document'. Its
   * WOComponent will only get triggered if the template uses
   * <code>&lt;SKYOBJ insertvalue="var" name="body" /&gt;</code>, which in turn
   * is mapped to the OGoPubContentReference dynamic element.
   * 
   * @param _p   - the (WOComponent based) page to be rendered
   * @param _ctx - the associated WOContext
   * @return an Exception on error or null if everything went fine
   */
  public Exception renderPubPage(OGoPubHTMLDocument _p, WOContext _ctx) {
    // TBD: what we *really* want is that the Template WOComponent is the
    //      renderer and produced by a proper IGoRenderFactory!!!
    WOApplication app = _ctx.application();
    
    // The resourcemanager should be attached to the OFS hierarchy. I think.
    // Hm, but maybe it should just "use" the OFS hierarchy.
    // The funny thing is that even templates locate their subtemplates using
    // the regular traversal path.
    
    /* Reuses existing resource manager if the lookup context is the same. This
     * is used by the bulk exporter which sets the RM just once per directory.
     */
    WOResourceManager     oldRM = app.resourceManager();
    OGoPubResourceManager rm = null; 
    if (oldRM instanceof OGoPubResourceManager) {
      rm = (OGoPubResourceManager)oldRM;
      if (rm.container() != _p.container())
        rm = null; /* different context, we need a new resource manager */
    }
    if (rm == null)
      rm = new OGoPubResourceManager((IGoObject)_p.container(), oldRM, _ctx);
    if (rm != oldRM && rm != null) app.setResourceManager(rm);
    
    /* instantiate cdef */
    
    /* Note: this triggers lookupName() with a null-context */
    // TODO: somehow fix this
    // TBD: does it? this method has a ctx argument?
    OGoPubComponent mainPage = (OGoPubComponent)
      app.pageWithName("Main", _ctx); // extension doesn't matter?!

    if (mainPage == null) {
      if (oldRM != rm) {
        app.setResourceManager(oldRM /* reset per-folder resource manager */);
        oldRM = null;
      }
      
      log.error("did not find master template at " + _p);
      return new GoNotFoundException("did not find master template");
    }

    mainPage.document = _p;

    /* generate response by calling main template */

    _ctx.setPage(mainPage);
    _ctx.enterComponent(mainPage, null /* component content */);
    try {
      mainPage.ensureAwakeInContext(_ctx);
      _ctx.response().setHeaderForKey(_p.mimeType(), "content-type");
      mainPage.appendToResponse(_ctx.response(), _ctx);
    }
    finally {
      _ctx.leaveComponent(mainPage);
      if (oldRM != rm)
        app.setResourceManager(oldRM /* reset per-folder resource manager */);
    }
    
    return null; /* rendering went fine */
  }
  
  public Exception renderPubTemplate(OGoPubTemplate _tmpl, WOContext _ctx) {
    return new GoAccessDeniedException();
  }

  public Exception renderPubFeed(OGoPubRSSFeed _feed, WOContext _ctx) {
    _feed.appendToResponse(_ctx.response(), _ctx);
    return null;
  }
}
