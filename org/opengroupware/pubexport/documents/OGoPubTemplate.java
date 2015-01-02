package org.opengroupware.pubexport.documents;

import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.opengroupware.pubexport.OGoPubRenderer;
import org.opengroupware.pubexport.OGoPubTemplateComponent;
import org.opengroupware.pubexport.parsers.OGoPubXTemplateParser;

@SuppressWarnings("rawtypes")
public class OGoPubTemplate extends OGoPubComponentDocument
  implements IGoObjectRenderer
{

  /* keys */
  
  @Override
  public String objType() {
    return "template";
  }
  
  @Override
  public String mimeType() {
    return "text/html";
  }
  
  /* component definition */

  public Class lookupComponentClass(String _name, WOResourceManager _rm) {
    return OGoPubTemplateComponent.class;
  }
  
  @Override
  public Class parserClass() {
    return OGoPubXTemplateParser.class;
  }
  
  
  /* rendering */

  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    return _object instanceof OGoPubHTMLDocument;
  }

  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    /* We don't need to lookup the template, because we already are the
     * renderer :-)
     */
    if (true) {
      // this does a second template lookup
      return OGoPubRenderer.sharedRenderer.renderPubPage
        ((OGoPubHTMLDocument)_object, _ctx);
    }
    else {
      // hm. this needs to be fixed first. we need to lookup the resourcemanager
      // in the hierarchy?
      WOResourceManager rm = _ctx.application().resourceManager(); 
    
      // I think technically we *are* a WOComponentDefinition subclass 
      IWOComponentDefinition cdef =
        this.definitionForComponent(this.nameInContainer(), null, rm);
    
      WOComponent page = cdef.instantiateComponent(rm, _ctx);
    
      // TBD
      System.err.println("DO RENDER:" + _object);
      return null;
    }
  }
  
}
