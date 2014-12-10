package org.opengroupware.pubexport.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.opengroupware.pubexport.OGoPubComponent;

public class OGoPubSelfAssociation extends WOAssociation {
  
  public static WOAssociation sharedAssociation = new OGoPubSelfAssociation();

  public OGoPubSelfAssociation() {
  }

  @Override
  public String keyPath() {
    return null;
  }

  @Override
  public Object valueInComponent(Object _cursor) {
    if (_cursor == null) {
      log.warn("'self' association got no cursor: " + this);
      return null;
    }
    
    if (_cursor instanceof OGoPubComponent) {
      Object doc = ((OGoPubComponent)_cursor).document();
      if (doc == null) {
        log.warn("cursor contains no document for 'self' assoc: " + _cursor);
        return null;
      }
      
      //System.err.println("RENDER: " + doc);
      return doc;
    }
    
    log.warn("self association returns cursor: " + _cursor);
    return _cursor;
  }
}
