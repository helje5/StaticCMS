package org.opengroupware.pubexport.js;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.opengroupware.pubexport.documents.OGoPubDocument;

public class OGoPubJSWrapFactory extends WrapFactory {
  protected static final Log log = LogFactory.getLog("OGoPubJavaScript");

  public OGoPubJSWrapFactory() {
  }
  
  /* wrapping */

  @Override
  @SuppressWarnings("rawtypes")
  public Scriptable wrapAsJavaObject
    (Context _ctx, Scriptable _scope, Object _javaObject, Class _staticType)
  {
    /* this is called by wrap() for non-basetypes */
    if (log.isDebugEnabled())
      log.debug("wrapAsJavaObject " + _javaObject + " class " + _staticType);
    
    if (_javaObject instanceof OGoPubDocument)
      return new OGoPubDocumentJSWrapper((OGoPubDocument)_javaObject);
    
    return super.wrapAsJavaObject(_ctx, _scope, _javaObject, _staticType);
  }

  @Override
  public Scriptable wrapNewObject(Context _ctx, Scriptable _scope, Object _o) {
    return super.wrapNewObject(_ctx, _scope, _o);
  }

}
