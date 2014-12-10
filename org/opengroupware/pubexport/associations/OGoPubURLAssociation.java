package org.opengroupware.pubexport.associations;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.OFSBaseObject;
import org.opengroupware.pubexport.OGoPubComponent;
import org.opengroupware.pubexport.documents.OGoPubDirectory;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/**
 * OGoPubURLAssociation
 * <p>
 * TODO: document<br />
 * - rewrites component relative links to target URL context
 */
public class OGoPubURLAssociation extends WOAssociation {
  protected static final Log log = LogFactory.getLog("OGoPubURL");
  
  protected WOAssociation url;
  
  public OGoPubURLAssociation(WOAssociation _url) {
    super();
    this.url = _url;
  }
  
  /* accessors */

  @Override
  public String keyPath() {
    if (this.url == null)
      return null;
    
    if (this.url.isValueConstant())
      return this.url.stringValueInComponent(null);
    
    return null;
  }
  
  /* value typing */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }

  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  /* URL helpers */
  
  protected static final String[] absolutePrefixes = {
    "http://", "https://",
    "mailto:", "irc:", "ftp://",
    "javascript:"
  };
  
  public static boolean isAbsolute(String _s) {
    if (_s == null)
      return true; /* we treat missing links as absolute */
    
    for (int i = 0; i < absolutePrefixes.length; i++) {
      if (_s.startsWith(absolutePrefixes[i]))
        return true;
    }
    
    if (_s.indexOf("://") != -1)
      return true;
    
    return false;
  }
  
  /* determine source URL */
  
  public String urlInComponent(Object _cursor) {
    if (this.url == null) {
      log.error("missing source link association: " + this);
      return null;
    }
    
    //System.err.println("GET: " + this.url);
    
    Object o = this.url.valueInComponent(_cursor);
    if (o == null) {
      log.warn("source link association returned no link: " + this.url);
      return null;
    }

    if (o instanceof String) /* eg <a href="abc"> */
      return (String)o;
    
    if (o instanceof URL) /* eg <a var:href="baseURL"> (incorrect) */
      return ((URL)o).toExternalForm();
    
    if (o instanceof NSKeyValueCoding) /* eg <a js:href="this"> */ {
      String s =
        NSJavaRuntime.stringValueForKey(o, "absolutePathInTargetHierarchy");
      if (s != null)
        return s;
      
      if ((s = NSJavaRuntime.stringValueForKey(o, "absolutePath")) != null)
        return s;
      
      log.error("cannot determine URL for given object: " + o);
      return "[ERROR]";
    }
    
    log.error("don't know how to turn object into URL: " + o);
    return "[ERROR]";
  }

  /* values */

  @Override
  public String stringValueInComponent(Object _cursor) {
    String urlString = this.urlInComponent(_cursor);
    
    if (_cursor == null) /* if we have no cursor, we can't do rewrites */
      return urlString;
    
    if (urlString != null && urlString.startsWith("nocheck:"))
      return urlString.substring(8);
    
    if (isAbsolute(urlString)) /* full URL, we don't rewrite those */
      return urlString;
    
    if (urlString.startsWith("#")) {
      /* Just a fragment, since named ankers are embedded in the result page,
       * fragments are always relative
       */
      return urlString;
    }
    
    if (log.isInfoEnabled()) log.info("CURSOR: " + _cursor);
    
    OGoPubComponent component = (OGoPubComponent)_cursor;
    WOContext       context   = component.context();
    Object          srcDoc    = component.templateDocument;
    if (srcDoc == null) {
      log.warn("component has no template document: " + component);

      /* this section is a bit hackish, it happens with directories being
       * queried inside lists
       */
      srcDoc = component.document;
      
      if (srcDoc instanceof OGoPubDirectory)
        srcDoc = ((OGoPubDirectory)srcDoc).indexDocument(context);
    }
    
    /* parse URL */
    
    String path = null;
    int cutOffIndex  = urlString.indexOf('#');
    int qidx = urlString.indexOf('?');
    if (cutOffIndex == -1 || (qidx != -1 && qidx < cutOffIndex))
      cutOffIndex = qidx;
    
    path = cutOffIndex != -1 ? urlString.substring(0, cutOffIndex) : urlString;
    
    /* Now lets resolve the link - against the template triggering this export
     * section.
     * 
     * TODO: should we acquire or not? Sure, why not?
     */
    Object linkDoc =
      OGoPubWebSite.lookupPath(srcDoc, path, context, false /* do not acq */);
    
    if (linkDoc == null) {
      // TODO: make more efficient by ignoring images etc
      linkDoc =
        OGoPubWebSite.lookupPath(srcDoc, path + ".html", context,
            false /* do not acq */);
      
      if (linkDoc != null) {
        log.warn("link missed .html extension: " + 
            ((IGoLocation)srcDoc).nameInContainer() + ": " + path);
        path += ".html";
      }
    }
    else if (log.isInfoEnabled()) {
      log.info("found document for path '" + path + "': " + linkDoc);
    }
    
    if (linkDoc == null) {
      /* Note: absolute links are not checked, see above */
      
      log.error("could not resolve link '" + urlString + "' against: " +
                srcDoc);
      return null; /* this should make the link not being rendered */
    }
    // System.err.println("LOOKED UP " + path + ": " + linkDoc);
    
    if (linkDoc instanceof OGoPubDirectory) {
      /* Note: this is not strictly necessary, a _webserver_ would evaluate
       *       it on its own. However, for sites dumped to the filesystem it is
       *       a requirement (otherwise the filesystem browser will open the
       *       plain directory listing).
       */
      OFSBaseObject idxDoc = ((OGoPubDirectory)linkDoc).indexDocument(context);
      if (idxDoc == null)
        log.warn("referred directory has no index document: " + linkDoc);
      else
        linkDoc = idxDoc; /* use the index document */
    }
    
    /* Note: we use the container, the container is the context.
     * 
     * The target we need to rewrite for is the traversal path of the
     * context, NOT the document of the component (because components
     * can nest when they get included in lists and such)
     * NOR for the physical path of the clientObject (because it could
     * have been aquired or be virtual).
     */
    String[] src = OGoPubWebSite.exportPathOfObjectUntilStopKey
      (linkDoc, "isTargetHierarchyRoot");
    
    String[] target = OGoPubWebSite.pathOfObjectUntilStopKey
      (context.goTraversalPath(), "isTargetHierarchyRoot",
       1 /* skip container */);
    
    if (log.isInfoEnabled()) {
      log.info("URL:  " + urlString);
      log.info("  in: " + UString.componentsJoinedByString(src, " / "));
      log.info("  to: " + UString.componentsJoinedByString(target, " / "));
    }
    
    /* shorten URL */
    
    int commonLength;
    for (commonLength = 0; 
         commonLength < src.length && commonLength < target.length;
         commonLength++) {
      if (src[commonLength] == null) {
        log.error("contained null in src-path: " + linkDoc);
        break;
      }
      if (target[commonLength] == null) {
        log.error("contained null in target-path: " +context.goTraversalPath());
        break;
      }
      
      if (!src[commonLength].equals(target[commonLength]))
        break;
    }
    
    /* make relative to document target */
    
    StringBuffer result = new StringBuffer(512);
    
    /* go up till root */
    for (int i = commonLength; i < target.length; i++)
      result.append("../");
    
    /* add absolute path */
    int absSize = src.length;
    for (int i = commonLength; i < absSize; i++) {
      result.append(src[i]);
      
      if (i + 1 != absSize)
        result.append("/");
    }
    
    /* last step: add fragment/query (HACK, should use URL) */

    if (cutOffIndex != -1) result.append(urlString.substring(cutOffIndex));
    
    return result.toString();
  }

  @Override
  public Object valueInComponent(Object _cursor) {
    return this.stringValueInComponent(_cursor);
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.url != null) {
      if (this.url.isValueConstant())
        _d.append(" url=" + this.url.stringValueInComponent(null));
      else
        _d.append(" url=" + this.url);
    }
  }

}
