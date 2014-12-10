package org.opengroupware.pubexport.associations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.ofs.OFSBaseObject;
import org.opengroupware.pubexport.OGoPubComponent;
import org.opengroupware.pubexport.documents.OGoPubDirectory;
import org.opengroupware.pubexport.documents.OGoPubDocument;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/*
 * OGoPubAnchorAssociation
 * 
 * Used in <SKYOBJ insertvalue="anchor" .../>
 * 
 * Names: self, parent, next, previous
 */
public class OGoPubAnchorAssociation extends WOAssociation {
  protected static final Log log = LogFactory.getLog("OGoPubURL");
    
  protected WOAssociation anchor;

  public OGoPubAnchorAssociation(WOAssociation _anchor) {
    super();
    this.anchor = _anchor;
  }
  
  /* accessors */

  @Override
  public String keyPath() {
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
  
  /* URL rewriting */
  
  public Object clientObjectForCursor(Object _cursor) {
    if (_cursor == null)
      return null;
    
    if (_cursor instanceof WOComponent)
      return ((WOComponent)_cursor).context().clientObject();

    return NSKeyValueCoding.Utility.valueForKey(_cursor, "clientObject");
  }
  
  public String relativeUrlForDocument(Object _doc, Object _cursor) {
    Object targetObject = (OGoPubDocument)
      this.clientObjectForCursor(_cursor);
    
    if (targetObject == null || targetObject == _doc) {
      /* "self" anchor */
      return IGoLocation.Utility.nameOfObjectInContainer(_doc, null);
    }
          
    /* third step: make relative to document target */
    // TODO: a bit of a DUP to OGoPubURLAssociation
    
    StringBuffer result = new StringBuffer(512);
    
    /* go up till root */
    String[] tPath =OGoPubWebSite.pathOfDocumentInTargetHierarchy(targetObject);
    for (int i = tPath.length; i >= 0 ; i--)
      result.append("../");
    
    /* add absolute path */
    
    String[] path =
      OGoPubWebSite.exportPathOfObjectUntilStopKey
        (_doc, "isTargetHierarchyRoot");
    
    int absSize = path.length;
    for (int i = 0; i < absSize; i++) {
      result.append(path[i]);
      
      if (i + 1 != absSize)
        result.append("/");
    }
    
    return result.toString();
  }

  /* values */

  @Override
  public String stringValueInComponent(Object _cursor) {
    // System.err.println("*** ANCHOR: " + this.anchor);
    if (_cursor == null || this.anchor == null)
      return null;
    
    String anchorName = this.anchor.stringValueInComponent(_cursor);
    if (anchorName == null) {
      log.warn("missing anchor name: " + this.anchor);
      anchorName = "self";
    }
    
    /* determine the document the anchor is relative to */
    
    OFSBaseObject baseDoc = ((OGoPubComponent)_cursor).document();
    if (baseDoc == null) {
      log.error("component has no document: " + _cursor);
      return null;
    }
    
    /* evaluate anchor */
    
    Object doc;
    Object container = IGoLocation.Utility.containerForObject(baseDoc);

    if ("self".equals(anchorName))
      doc = baseDoc;
    else if (baseDoc.container() == null) {
      log.error("cannot use '" + anchorName + "' anchor on root doc: " + 
                baseDoc);
      return null;
    }
    else if ("parent".equals(anchorName))
      doc = container;
    else if ("next".equals(anchorName)) {
      // TODO: consolidate sorted document collections
      if (container instanceof OGoPubDirectory)
        doc = ((OGoPubDirectory)container).nextDocument(baseDoc);
      else {
        log.error("cannot determine next object in container: " + container);
        doc = null;
      }
    }
    else if ("previous".equals(anchorName)) {
      // TODO: consolidate sorted document collections
      if (container instanceof OGoPubDirectory)
        doc = ((OGoPubDirectory)container).previousDocument(baseDoc);
      else {
        log.error("cannot determine prev object in container: " + container);
        doc = null;
      }
    }
    else {
      log.error("unknown/unsupported anchor name: '" + anchorName + "'");
      return null;
    }
    
    if (doc instanceof OGoPubDirectory) { /* use index docs for directories */
      doc = ((OGoPubDirectory)doc).indexDocument
        (((WOComponent)_cursor).context());
    }
    
    if (doc == null) {
      log.error("found no proper anchor target for anchor: " + anchorName);
      return null;
    }
    
    /* rewrite anchor */
    // TODO: its not _THAT_ easy ;-) self must be rewritten for the root
    //       context (clientObject in the context)
    
    return this.relativeUrlForDocument(doc, _cursor);
  }

  @Override
  public Object valueInComponent(Object _cursor) {
    return this.stringValueInComponent(_cursor);
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.anchor != null)
      _d.append(" anchor=" + this.anchor);
  }
}
