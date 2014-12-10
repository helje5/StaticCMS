package org.opengroupware.pubexport.documents;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.OFSBaseObject;

public class OGoPubDocument extends OFSBaseObject {
  protected static final Log log = LogFactory.getLog("OGoPubExport");  

  public String[] path;
  
  /* accessors */
  
  public String name() {
    return this.nameInContainer();
  }
  public String title() {
    return (String)this.valueForKey("NSFileSubject");
  }
  
  public boolean isRoot() {
    return this.container == null;
  }
  public boolean isTargetHierarchyRoot() {
    return this.isRoot();
  }
  
  /* keys */
  
  public String objType() {
    return "generic";
  }
  
  public String mimeType() {
    /* Note: this is the MIME type used for exports! */
    Map<String, Object> props = this.properties();
    if (props != null) {
      String mt = (String)props.get("NSFileMimeType");
      if (mt != null && mt.length() > 0)
        return mt;
    }
    
    String mt = Util.mimeTypeForExtension(this.pathExtension());
    if (mt != null) return mt;
    
    String fn = this.nameInContainer;
    if (fn.equals("ChangeLog"))       return "text/plain";
    if (fn.equals("README"))          return "text/plain";
    if (fn.equals("NOTES"))           return "text/plain";
    if (fn.startsWith("GNUmakefile")) return "text/x-make";
    
    return "application/octet-stream";
  }
  
  // TODO: what is objClass?
  
  /* hierarchy */
  
  public OGoPubDirectory rootOfTargetHierarchy() {
    return OGoPubWebSite.rootOfTargetHierarchy(this);
  }
  
  public OFSBaseObject nextDocument() {
    return this.container instanceof OGoPubDirectory
      ? ((OGoPubDirectory)this.container).nextDocument(this) : null;
  }
  public OFSBaseObject previousDocument() {
    return this.container instanceof OGoPubDirectory
      ? ((OGoPubDirectory)this.container).previousDocument(this) :null;
  }
  
  public String[] pathInTargetHierarchy() {
    if (this.path == null) {
      this.path =
        OGoPubWebSite.exportPathOfObjectUntilStopKey(this, "isTargetHierarchyRoot");
    }
    return this.path;
  }
  public String relativePathInTargetHierarchy() {
    return OGoPubWebSite.relativePathInTargetHierarchy(this);
  }
  public String absolutePathInTargetHierarchy() {
    return OGoPubWebSite.absolutePathInTargetHierarchy(this);
  }
  
  /* properties */
  
  public Map<String, Object> properties() {
    if (!(this.container instanceof OGoPubDirectory)) {
      log.warn("document has no container for property retrieval: " + this);
      return null;
    }
    
    String n = this.storagePath == null || this.storagePath.length == 0
      ? "/"
      : this.storagePath[this.storagePath.length - 1];
    
    return ((OGoPubDirectory)this.container).propertiesForFileNamed(n);
  }
  
  /* content */
  
  public String contentAsString() {
    return UString.readLatin1FromStream(this.openStream());
  }
  public String getContent() { /* alias for LiveConnect */
    return this.contentAsString();
  }
  
  /* WebDAV support */
  
  public String davDisplayName() {
    String t = this.title();
    return t != null ? t : this.nameInContainer();
  }
  
  /* key/value coding */
  
  @Override
  public Object valueForFileSystemKey(String _key) {
    if ("NSFileSubject".equals(_key)) {
      if (log.isInfoEnabled()) {
        log.info("asked for NSFileSubject: " + this +
                 " props: " + this.properties());
      }
      return null; /* do not return NSFileName so that checks work */
    }
    
    if ("NSFilePath".equals(_key))
      return OGoPubWebSite.absolutePathInTargetHierarchy(this);
    
    if ("NSFileMimeType".equals(_key))
      return this.mimeType();
    
    return super.valueForFileSystemKey(_key);
  }

  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    // if (this.relativePath().equals("index.html"))
    //  System.err.println("KEY: " + _key);

    if ("this".equals(_key) || "self".equals(_key))
      return this;
    
    /* expose properties */
    if (_key != null) {
      Map<String, Object> props = this.properties();
      if (props != null) {
        Object v = props.get(_key);
        if (v != null) return v;
      }
      else if (log.isInfoEnabled())
        log.info("document has no properties: " + this);
    }

    return super.handleQueryWithUnboundKey(_key);
  }
  
  /* log */
  
  @Override
  public Log log() {
    return log;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    String s = this.path != null ? this.relativePathInTargetHierarchy() : null;
    if (s != null && s.length() > 0)
      _d.append(" name=" + s);
  }
}
