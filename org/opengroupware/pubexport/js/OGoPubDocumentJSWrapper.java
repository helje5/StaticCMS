package org.opengroupware.pubexport.js;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.ofs.OFSBaseObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.opengroupware.pubexport.documents.OGoPubDirectory;
import org.opengroupware.pubexport.documents.OGoPubDocument;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/**
 * OGoPubDocumentJSWrapper
 * <p>
 * Used to expose OGoPubDocument properties/methods to JavaScript.
 * <p>
 * TODO: do we actually need to wrap? Probably, to support the properties.<br>
 * TODO: we probably need to transform ourselves?
 * <p>
 * SkyPublisher:<pre>
 * body                    getContent()              -
 * self                    this                      self
 * contentType             contentType               NGFileMimeType
 * lastChanged             lastChanged               NSFileModificationDate
 * hasSuperLinks           hasSuperLinks             -
 * id                      id                        self.globalID
 * isRoot                  isRoot                    -
 * path                    path                      NSFilePath
 * name                    name                      NSFileName
 * objType                 objType                   -
 * objClass                objClass                  -
 * prefixPath                                        -
 * title                   title                     NSFileSubject
 * version                 getLastVersion()          SkyVersionName
 * visibleName             name                      NSFileName
 * visiblePath             path                      NSFilePath
 * parent                  getParentDocument()       -
 * index                   getIndexDocument()        -
 * toclist                 getTocList()              -
 * children                getChildList()            -
 * relatedLinks            -                         -
 * objectsToRoot           getDocumentsToRoot()      -</pre>
*/
public class OGoPubDocumentJSWrapper extends ScriptableObject
  implements Wrapper, NSKeyValueCoding
{
  // TODO: would be good to attach a WOContext?
  protected static final Log log = LogFactory.getLog("OGoPubJavaScript");
  private static final long serialVersionUID = 1L;

  public OGoPubDocumentJSWrapper() {
    super();
  }
  public OGoPubDocumentJSWrapper(Scriptable _scope, Scriptable _prototype) {
    super(_scope, _prototype);
  }

  public OGoPubDocumentJSWrapper(OFSBaseObject _doc) {
    super();
    this.document = _doc;
  }
  
  /* ctor */
  
  public static Scriptable wrap
    (Context _ctx, Scriptable _scope, OFSBaseObject _pubdoc)
  {
    return _ctx.newObject(_scope, "OGoPubDocument", new Object[] { _pubdoc });
  }
  
  public Object unwrap() {
    return this.document;
  }
  
  /* ivars */
  
  protected OFSBaseObject document;

  /* JavaScript side constructor */

  @Override
  public String getClassName() {
    return "OGoPubDocument";
  }
  
  public void jsConstructor(Object _document) {
    this.document = (OFSBaseObject)_document;
  }
  
  /* properties */
    
  public String jsGet_path() {
    return OGoPubWebSite.absolutePathInTargetHierarchy(this.document);
  }
  public String jsGet_name() {
    return ((IGoLocation)this.document).nameInContainer();
  }
  public String jsGet_title() {
    return (String)
      ((NSKeyValueCoding)this.document).valueForKey("NSFileSubject");
  }

  public String jsGet_objType() {
    return (String)((NSKeyValueCoding)this.document).valueForKey("objType");
  }
  
  public boolean jsGet_isRoot() {
    return this.document.container() == null;
  }
  public boolean jsGet_hasSuperLinks() {
    return false;
  }

  /* functions */
    
  public Object jsFunction_getAttribute(String _name) {
    return this.document.valueForKey(_name);
  }

  public Object jsFunction_getContent() {
    // TODO: support OFSFile
    if (this.document instanceof OGoPubDocument)
      return ((OGoPubDocument)this.document).contentAsString();
    
    return null;
  }

  public Object jsFunction_getParentDocument() {
    // TODO: do we need to do wrapping ourselves? probably!
    // => possibly handled by our wrap factory?
    return this.document.container();
  }

  public Object jsFunction_getIndexDocument() {
    // TODO: do we need to do wrapping ourselves? probably!
    if (this.document instanceof OGoPubDirectory)
      return ((OGoPubDirectory)this.document).indexDocument(null /* ctx */);
    
    return ((OGoPubDirectory)this.document.container()).indexDocument(null /*ctx*/);
  }
  
  protected Object fetchList(String _listName) {
    OGoPubDirectory dir = (this.document instanceof OGoPubDirectory)
      ? (OGoPubDirectory)this.document
      : (OGoPubDirectory)this.document.container();
    
    EOFetchSpecification fs =
      new EOFetchSpecification(_listName, null /* qual */, null /* sorts */);
      
    return dir.fetchObjects(fs, null /* context */);
  }
  
  public Object jsFunction_getChildList() {
    return this.fetchList("children");
  }
  public Object jsFunction_getTocList() {
    return this.fetchList("toclist");
  }
  public Object jsFunction_getDocumentsToRoot() {
    return this.fetchList("objectsToRoot");
  }
  
  /* KVC */
  // TBD: is this ever called? Shouldn't the 'unwrap' happen?
  
  public Object handleQueryWithUnboundKey(String _key) {
    return this.document.handleQueryWithUnboundKey(_key);
  }
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    this.document.handleTakeValueForUnboundKey(_value, _key);
  }
  public void takeValueForKey(Object _value, String _key) {
    this.document.takeValueForKey(_value, _key);
  }
  public Object valueForKey(String _key) {
    return this.document.valueForKey(_key);
  }
}
