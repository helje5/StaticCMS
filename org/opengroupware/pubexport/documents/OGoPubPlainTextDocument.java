package org.opengroupware.pubexport.documents;

import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.elements.WOStaticHTMLElement;
import org.getobjects.appserver.templates.WOTemplate;
import org.opengroupware.pubexport.OGoPubPageComponent;

/**
 * OGoPubPlainTextDocument
 * <p>
 * Exposes a .txt document as an HTML component?!
 * <p>
 * @author helge
 */
@SuppressWarnings("unchecked")
public class OGoPubPlainTextDocument extends OGoPubComponentDocument {

  /* keys */
  
  @Override
  public String objType() {
    return "document";
  }
  
  @Override
  public String mimeType() {
    // if this is set to HTML, links are converted from .txt => .html, probably
    // note what we want ...
    // return "text/html";
    return "text/plain";
  }
  
  /* parsing */

  public Class lookupComponentClass(String _name, WOResourceManager _rm) {
    return OGoPubPageComponent.class;
  }

  @Override
  public Class parserClass() {
    log.error("do not call parserClass on this document: " + this);
    return null;
  }

  protected WOTemplate parseTemplate() {
    // TODO: do we need that?
    WOElement  root     = new WOStaticHTMLElement(this.contentAsString());
    WOTemplate template = new WOTemplate(null /* file URL */, root);
    
    return template;
  }
}
