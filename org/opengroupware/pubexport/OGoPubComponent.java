package org.opengroupware.pubexport;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.OFSBaseObject;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;
import org.opengroupware.pubexport.documents.OGoPubComponentDocument;
import org.opengroupware.pubexport.documents.OGoPubWebSite;
import org.opengroupware.pubexport.js.OGoPubDocumentJSWrapper;
import org.opengroupware.pubexport.js.OGoPubJSWrapFactory;

public abstract class OGoPubComponent extends WOComponent {
  protected static final Log log = LogFactory.getLog("OGoPubExport");
  
  public static final String CONTENT_SPECIALKEY = "$$CONTENT$$";
  
  public OFSBaseObject document;
  public OGoPubComponentDocument templateDocument;
  
  /* document */
  
  public OFSBaseObject document() {
    if (this.document == null)
      this.document = (OFSBaseObject)this.context().clientObject();
    return this.document;
  }
  
  public String[] docPathInTargetHierarchy() {
    return OGoPubWebSite.exportPathOfObjectUntilStopKey
             (this.document(), "isTargetHierarchyRoot");
  }
  public String path() {
    String[] lPath = this.docPathInTargetHierarchy();
    return "/" + UString.componentsJoinedByString(lPath, "/");
  }
  
  /* key/value coding */
  
  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    if ("this".equals(_key) || "self".equals(_key)) {
      System.err.println("THIS OR SELF: " + this);
      return this.document();
    }
    
    return this.document().valueForKey(_key);
  }
  
  /* JavaScript support */
  
  protected static org.mozilla.javascript.Scriptable rootScope; 
  protected org.mozilla.javascript.Scriptable jsScope; 
  protected org.mozilla.javascript.Scriptable docScope; 
  protected org.mozilla.javascript.Context    jsContext;
  
  public org.mozilla.javascript.Context jsContext() {
    if (this.jsContext == null)
      this.jsContextEnter();
    
    return this.jsContext;
  }
  
  public ContextFactory jsContextFactory() {
    return ContextFactory.getGlobal();
  }
  
  public synchronized org.mozilla.javascript.Scriptable jsRootScope() {
    // TODO: improved THREADing behaviour
    if (rootScope == null) {
      /* according to Rhino docs we can create scopes in arbitary contexts */
      try {
        org.mozilla.javascript.Context rootCtx =
          this.jsContextFactory().enterContext();
        
        rootScope = rootCtx.initStandardObjects();

        try {
          ScriptableObject.defineClass
            (rootScope, OGoPubDocumentJSWrapper.class);
        }
        catch (IllegalAccessException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        catch (InstantiationException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        catch (InvocationTargetException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      finally {
        org.mozilla.javascript.Context.exit();
      }
    }
    return rootScope;
  }
  
  public org.mozilla.javascript.Scriptable jsComponentScope() {
    // THREAD: one component instance is not supposed to be used in multiple
    //         threads, so this should be OK.
    
    if (this.jsContext == null)
      this.jsContextEnter();
    
    if (this.jsScope == null) {
      org.mozilla.javascript.Scriptable sharedScope = this.jsRootScope();
      
      this.jsScope = this.jsContext.newObject(sharedScope);
      this.jsScope.setPrototype(sharedScope);
      this.jsScope.setParentScope(null /* we are the root */);
    }
    return this.jsScope;
  }

  public org.mozilla.javascript.Scriptable jsScope() {
    /* this is the scope which is actually used for evaluation */
    if (this.docScope != null)
      return this.docScope;
    
    if (false) {
      this.docScope = this.jsComponentScope();
      
      this.docScope.put("path", this.docScope, this.docPathInTargetHierarchy());
      this.docScope.put("doc",  this.docScope,
           org.mozilla.javascript.Context.javaToJS(document(), this.docScope));
    }
    else {
      this.docScope = OGoPubDocumentJSWrapper.wrap
        (this.jsContext(), this.jsComponentScope(), this.document());
    }
    return this.docScope;
  }
  
  public void jsContextEnter() {
    this.jsContextExit();
    this.jsContext = this.jsContextFactory().enterContext();
    this.jsContext.setWrapFactory(new OGoPubJSWrapFactory());
  }
  public void jsContextExit() {
    if (this.jsContext != null) {
      org.mozilla.javascript.Context.exit();
      this.jsContext = null;
    }
  }
  
  /* notifications */

  @Override
  public void reset() {
    super.reset();
    this.jsContextExit();
  }

  @Override
  public void sleep() {
    super.sleep();
    this.jsContextExit();
  }

  /* generate response */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    /* ensure that a JavaScript context is available during rendering */
    
    try {
      this.jsContextEnter();
      super.appendToResponse(_r, _ctx);
    }
    finally {
      this.jsContextExit();
    }
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.templateDocument != null)
      _d.append(" tmpl=" + this.templateDocument);
    else
      _d.append(" no-tmpl");
    
    if (this.document != null)
      _d.append(" doc=" + this.document);
    else
      _d.append(" no-doc");
  }

}
