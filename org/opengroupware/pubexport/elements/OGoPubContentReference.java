package org.opengroupware.pubexport.elements;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/**
 * OGoPubContentReference
 * <p>
 * This dynamic element represents the
 *   <code>&lt;SKYOBJ insertvalue="var" name="body" /&gt;</code>
 * template tag.
 * 
 * It instantiates and triggers the OGoPubComponent which represents the active
 * document. The active document is retrieved using the "document" KVC key of
 * the active component.
 * 
 * <p>
 * This elements processes no associations. It does pass down the template to
 * the component, which could then use <#WOComponentContent/> to invoke it.
 */
public class OGoPubContentReference extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("OGoPubTemplate");
  protected WOElement template;
  
  public OGoPubContentReference
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.template = _template;
  }
  
  /* generate response */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    WOComponent page = (WOComponent)_ctx.cursor();
    Object      doc  = page.valueForKey("document");
    if (doc == null) {
      log.error("component has no document: " + page);
      return;
    }
    
    /* attempt to instantiate child */
    
    String docPath = OGoPubWebSite.relativePathInTargetHierarchy(doc);

    WOComponent child = page.pageWithName("/" + docPath);
    if (child == null) {
      log.error("could not instantiate content component: " + doc);
      return;
    }
    
    /* render subcomponent */
    
    _ctx.enterComponent(child, this.template);
    child.appendToResponse(_r, _ctx);
    _ctx.leaveComponent(child);
  }
}
