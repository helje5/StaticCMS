package org.opengroupware.pubexport.parsers;

import java.net.URL;

import org.getobjects.appserver.templates.WOTemplateParser;
import org.getobjects.appserver.templates.WOWrapperTemplateBuilder;

public class OGoPubSkyObjBuilder extends WOWrapperTemplateBuilder {

  public OGoPubSkyObjBuilder() {
    super();
  }

  @Override
  public WOTemplateParser instantiateTemplateParser(URL _url) {
    if (_url != null) { /* kinda hackish, but well ... */
      String p = _url.getPath();
      
      if (p.endsWith("xtmpl"))
        return new OGoPubXTemplateParser();
      
      if (p.endsWith("xhtml"))
        return new OGoPubXHTMLParser();
    }
    
    return new OGoPubHTMLParser();
  }

}
