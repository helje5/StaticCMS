package org.opengroupware.pubexport.documents;

import org.getobjects.appserver.core.WOResourceManager;
import org.opengroupware.pubexport.OGoPubPageComponent;
import org.opengroupware.pubexport.parsers.OGoPubHTMLParser;

@SuppressWarnings("rawtypes")
public class OGoPubHTMLDocument extends OGoPubComponentDocument {

  /* keys */
  
  @Override
  public String objType() {
    return "document";
  }
  
  @Override
  public String mimeType() {
    return "text/html";
  }

  /* component definition */
  
  public Class lookupComponentClass(String _name, WOResourceManager _rm) {
    return OGoPubPageComponent.class;
  }
  
  @Override
  public Class parserClass() {
    return OGoPubHTMLParser.class;
  }
  
  /* renderer */
}
