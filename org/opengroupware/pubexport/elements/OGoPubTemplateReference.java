package org.opengroupware.pubexport.elements;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.opengroupware.pubexport.OGoPubComponent;
import org.opengroupware.pubexport.documents.OGoPubComponentDocument;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/**
 * OGoPubTemplateReference
 * <p>
 * Used to invoke a template (subcomponent).
 * 
 * @author helge
 */
public class OGoPubTemplateReference extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("OGoPubTemplate");
  
  protected WOAssociation name;
  protected WOElement     template;
  
  public OGoPubTemplateReference
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    // Note: this can be triggered by subclasses which 'remap' the name binding
    if ((this.name = grabAssociation(_assocs, "name")) == null)
      log.error("missing 'name' of template to insert: " + _assocs);
    
    this.template = _template;
  }
  
  /* generate response */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    // TBD: Is this relevant? The template should always get looked up
    //      based on the traversal path?
    //      Only links are resolved in the target namespace?
    OGoPubComponent page = (OGoPubComponent)_ctx.cursor();
    Object          doc  = page != null ? page.document() : null;
    if (doc == null) {
      log.error("component has no document: " + page);
      return;
    }
    
    String templateName = this.name != null
      ? this.name.stringValueInComponent(_ctx.cursor()) : null;
    if (templateName == null) {
      log.error("got no template name: " + this.name);
      return;
    }
    if (templateName.indexOf('.') == -1)
      templateName += ".xtmpl";
    
    Object tdoc =
      OGoPubWebSite.lookupPath(doc, templateName, _ctx, true /* do acquire */);
    if (tdoc == null) {
      log.error("did not find template: " + templateName + " in " + doc);
      _r.appendContentHTMLString("[did not find template: "+ templateName +"]");
      return;
    }
    if (!(tdoc instanceof OGoPubComponentDocument)) {
      log.error("file is not a template: " + templateName + ": " + tdoc);
      _r.appendContentHTMLString("[not a template: "+ templateName +"]");
      return;
    }
    
    /* attempt to instantiate child */
    
    OGoPubComponent child = (OGoPubComponent)
      page.pageWithName(OGoPubWebSite.absolutePathInTargetHierarchy(tdoc));
    if (child == null) {
      log.error("could not instantiate template component: " + templateName);
      return;
    }
    
    /* render subcomponent */
    
    _ctx.enterComponent(child, this.template);
    child.appendToResponse(_r, _ctx);
    _ctx.leaveComponent(child);
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "name", this.name);
  }
}
