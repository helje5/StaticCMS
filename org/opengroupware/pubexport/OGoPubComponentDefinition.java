package org.opengroupware.pubexport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOComponentDefinition;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.opengroupware.pubexport.documents.OGoPubComponentDocument;
import org.opengroupware.pubexport.documents.OGoPubHTMLDocument;

public class OGoPubComponentDefinition extends WOComponentDefinition {
  protected static final Log log =
    LogFactory.getLog("OGoPubComponentDefinition");
    
  final OGoPubComponentDocument document;

  @SuppressWarnings("rawtypes")
  public OGoPubComponentDefinition(OGoPubComponentDocument _doc, Class _cls) {
    super(_doc.nameInContainer(), _cls);
    this.document = _doc;
  }
  
  /* instantiation */

  @Override
  public WOComponent instantiateComponent
    (final WOResourceManager _rm, final WOContext _ctx)
  {
    log.info("instantiate: " + this);
    
    WOComponent page = super.instantiateComponent(_rm, _ctx);
    
    log.info("  instantiated: " + page);
    
    if (page instanceof OGoPubComponent) {
      OGoPubComponent pc = (OGoPubComponent)page;
      
      if (this.document instanceof OGoPubHTMLDocument) {
        pc.templateDocument = this.document;
        pc.document         = this.document;
      }
      else {
        pc.templateDocument = this.document;
        pc.document = (OGoPubComponentDocument)_ctx.clientObject();
      }
    }
    else
      log.warn("created component is not an OGoPubComponent: " + page);

    log.info("  initialized: " + page);
    
    return page;
  }
}
