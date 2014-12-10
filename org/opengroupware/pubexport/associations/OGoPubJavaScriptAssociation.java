package org.opengroupware.pubexport.associations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSKeyValueCoding;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

public class OGoPubJavaScriptAssociation extends WOAssociation {
  protected static final Log log = LogFactory.getLog("OGoPubJavaScript");
  
  protected String scriptString;
  protected Script script;
    
  public OGoPubJavaScriptAssociation(String _script) {
    super();
    this.scriptString = _script;
    this.compile();
  }
  
  /* compilation */
  
  public void compile() {
    if (this.scriptString == null) {
      log.error("association has no script?");
      return;
    }
    
    try {
      Context ctx = ContextFactory.getGlobal().enterContext();
      
      this.script = ctx.compileString
        (this.scriptString,
         "<association>", 1 /* line */,
         null /* security context */);
    }
    finally {
      Context.exit();
    }
  }
  
  /* accessors */

  @Override
  public String keyPath() {
    return this.scriptString;
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
  
  /* value */
  
  @Override
  public Object valueInComponent(Object _cursor) {
    if (this.script == null) {
      log.error("no compiled script available .."); // TODO: improve
      return null;
    }
    
    /* retrieve context from component */
    
    Context jsContext;
    Scriptable jsScope;
    
    if (_cursor instanceof NSKeyValueCoding) {
      NSKeyValueCoding kvc = (NSKeyValueCoding)_cursor;
      jsContext = (Context)kvc.valueForKey("jsContext");
      jsScope   = (Scriptable)kvc.valueForKey("jsScope");
    }
    else {
      jsContext = (Context)
        NSKeyValueCoding.Utility.valueForKey(_cursor, "jsContext");
      jsScope   = (Scriptable)
        NSKeyValueCoding.Utility.valueForKey(_cursor, "jsScope");
    }
    
    /* directly run if we have a proper component environment */
    
    if (jsContext != null) {
      if (jsScope == null) {
        if (log.isWarnEnabled())
          log.warn("got no JavaScript scope from component: " + _cursor);
        jsScope = jsContext.initStandardObjects();
      }
      
      Object result = this.script.exec(jsContext, jsScope);
      
      if (result instanceof Wrapper)
        result = ((Wrapper)result).unwrap();
      return result;
    }
    
    /* setup own environment if we have none ... (expensive!) */

    if (log.isWarnEnabled())
      log.warn("got no JavaScript context from component: " + _cursor);
    
    try {
      jsContext = ContextFactory.getGlobal().enterContext();

      if (jsScope == null) {
        if (log.isWarnEnabled())
          log.warn("got no JavaScript scope from component: " + _cursor);
        jsScope = jsContext.initStandardObjects();
      }
      
      Object result = this.script.exec(jsContext, jsScope);
      
      if (result instanceof Wrapper)
        result = ((Wrapper)result).unwrap();
      return result;
    }
    finally {
      Context.exit();
    }
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.scriptString != null)
      _d.append(" script=" + this.scriptString);
    if (this.script != null)
      _d.append(" compiled");
  }
}
