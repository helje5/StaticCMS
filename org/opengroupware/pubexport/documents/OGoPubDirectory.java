package org.opengroupware.pubexport.documents;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.IGoObjectRendererFactory;
import org.getobjects.appserver.publisher.IGoSecuredObject;
import org.getobjects.appserver.publisher.GoContainerResourceManager;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSPropertyListParser;
import org.getobjects.ofs.OFSBaseObject;
import org.getobjects.ofs.OFSFolder;
import org.getobjects.ofs.fs.OFSFileManager;
import org.opengroupware.pubexport.OGoPubResourceManager;

public class OGoPubDirectory extends OFSFolder
  implements IGoObjectRendererFactory
{
  protected static final Log log = LogFactory.getLog("OGoPubExport");  

  public Map<String, Object> fileProperties;

  public OGoPubDirectory() {
    super();
    
    /* enable object cache */
    if (this.cacheNameToObject == null)
      this.cacheNameToObject = new HashMap<String, Object>(16);
  }
  
  
  /* HACKS for exporter */
  
  public void _setContext(WOContext _ctx) {
    this.context = _ctx; // hack for exporter
  }
  public IGoContext context() {
    return this.context; // hack for exporter
  }
  
  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    if (this.context == null) // dirty hack
      this.context = _ctx;
    
    return super.lookupName(_name, _ctx, _acquire);
  }

  
  /* file properties */
  
  @SuppressWarnings("unchecked")
  public void loadProperties() {
    String[] propPath = OFSFileManager
      .pathForChild(this.storagePath, ".attributes.plist");
    
    InputStream in = this.fileManager.openStreamOnPath(propPath);
    if (in == null) {/* does not exist or some other error */
      if (log().isInfoEnabled())
        log().info("could not open attributes: " + propPath);
      return;
    }
    
    NSPropertyListParser parser = new NSPropertyListParser();
    Object plist = parser.parse(in); // reads UTF-8
    if (plist == null) {
      log().error("could not parse plist: " + this);
      return;
    }
    else if (!(plist instanceof Map)) {
      log().error("plist is not a Map: " + this);
      return;
    }
    
    this.fileProperties = (Map<String, Object>)plist;
  }
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> propertiesForFileNamed(String _name) {
    // System.err.println("*** PROPS FOR " + _name + " in " + this);
    
    if (this.fileProperties == null)
      this.loadProperties();
    
    if (_name == null || this.fileProperties == null)
      return null;
    
    // System.err.println("PROPS: " + this.fileProperties);
    
    return (Map<String, Object>)this.fileProperties.get(_name);
  }

  public Map<String, Object> properties() {
    if (this.container == null) {
      /* special case for root, represented as "/" in the .attributes.plist */
      if (log.isDebugEnabled())
        log.debug("document has no container for property retrieval: " + this);
      return this.propertiesForFileNamed("/");
    }
    
    if (this.container instanceof OGoPubDirectory) {
      String n = this.storagePath == null || this.storagePath.length == 0
        ? "/" : this.storagePath[this.storagePath.length - 1];
      
      return ((OGoPubDirectory)this.container).propertiesForFileNamed(n);
    }

    log.warn("parent folder cannot store properties for object: " + this);
    return new HashMap<String, Object>(0);
  }
  
  
  /* resource manager */
  
  /**
   * Lookup and cache a resource manager for the folder.
   * 
   * @return a WOResourceManager instance
   */
  public WOResourceManager resourceManager() {
    if (this.resourceManager != null)
      return this.resourceManager;
    
    final WOResourceManager parentRM =
      GoContainerResourceManager.lookupResourceManager
        (this.container(), this.context);
    
    this.resourceManager = 
      new OGoPubResourceManager(this, parentRM, this.context);
    return this.resourceManager;
  }

  
  /* rendering */

  public Object rendererForObjectInContext(Object _result, WOContext _ctx) {
    // TBD: Hm, does this actually belong into OGoPubHTMLDocument? I guess so,
    //      it would need to acquire 'Main' and then return it as the renderer.
    Object masterTemplate = null;
    
    if (_result instanceof OGoPubHTMLDocument) {
      /* Oh yes, we support special handling for HTML documents. For those
       * we return the appropriate template as the renderer (if this folder
       * contains one).
       */
      // TBD: we could also let the user enforce a template using some
      //      query parameter?
      
      masterTemplate =
        IGoSecuredObject.Utility.lookupName(this, "Main", _ctx, false /*aq*/);
    }
    
    if (masterTemplate == null)
      return null; /* no template */
    if (masterTemplate instanceof Exception)
      return null; /* no template (404?) or no access to this template */
    
    if (!(masterTemplate instanceof IGoObjectRenderer)) {
      log.warn("master template is not a renderer: " + masterTemplate);
      return null;
    }
    
    IGoObjectRenderer r = (IGoObjectRenderer)masterTemplate;
    if (!r.canRenderObjectInContext(_result, _ctx)) {
      if (log.isInfoEnabled()) {
        log.info("master template cannot render object: " + _result +
            "\n  template: " + masterTemplate);
      }
      return null;
    }
    
    if (log.isInfoEnabled())
      log.info("found master template: " + masterTemplate);
    
    return r;
  }
  
  
  /* index document */
  
  public OFSBaseObject indexDocument(IGoContext _ctx) {
    return (OFSBaseObject)this.lookupName("index", _ctx, false /* acquire? */);
  }
  
  
  /* keys */
  
  public String objType() {
    return "publication";
  }
  
  public String mimeType() {
    return "x-skyrix/filemanager-directory";
  }
  
  
  /* navigation */
  
  public OFSBaseObject nextDocument(OFSBaseObject _doc) {
    List<OFSBaseObject> exportFiles =
      OGoPubWebSite.exportFilesForFolder(this, null /* ctx */);
    if (exportFiles == null || _doc == null) return null;
    
    for (int i = 0; i < exportFiles.size(); i++) {
      if (exportFiles.get(i) == _doc) {
        i++;
        return i < exportFiles.size() ? exportFiles.get(i) : null;
      }
    }
    return null;
  }
  
  public OFSBaseObject previousDocument(OFSBaseObject _doc) {
    List<OFSBaseObject> exportFiles =
      OGoPubWebSite.exportFilesForFolder(this, null /* ctx */);
    if (exportFiles == null || _doc == null) return null;
        
    for (int i = 0; i < exportFiles.size(); i++) {
      if (exportFiles.get(i) == _doc) {
        i--;
        return i >= 0 ? exportFiles.get(i) : null;
      }
    }
    return null;
  }
  
  
  /* fetching */
  
  protected static final EOQualifier allQualifier =
    EOQualifier.qualifierWithQualifierFormat("objType = 'document'");
  
  /* SkyPub: (NSFileType='NSFileTypeDirectory' OR 
   *          NSFileType='NSFileTypeRegular') AND
   *         NOT (NSFileName like '*.xtmpl')
   */
  protected static final EOQualifier toclistQualifier =
    EOQualifier.qualifierWithQualifierFormat
    ("objType = 'document' OR objType = 'publication' OR objType = 'generic'");
  
  public OFSBaseObject[] fetchObjects
    (EOFetchSpecification _fs, IGoContext _ctx)
  {
    /*
     * Lists:
     *   all           - all documents in the publication (objType = 'document')
     *   toclist       - all publications, documents & generic documents
     *   children      - all subobjects (including directories)
     *   relatedLinks  - related links ?
     *   objectsToRoot - all objects till root
     * 
     * TODO: in SkyPub 'all' lists *all* elements of the whole webserver, that
     *       is, it does a deep fetch.
     */
    
    EOQualifier q        = _fs != null ? _fs.qualifier()  : null;
    String      listType = _fs != null ? _fs.entityName() : "toclist";
    
    //EOQualifier aux = null;
    if ("all".equals(listType))
      q = q != null ? new EOAndQualifier(q, allQualifier) : allQualifier;
    else if ("toclist".equals(listType))
      q = q != null ? new EOAndQualifier(q, toclistQualifier) :toclistQualifier;
    
    if ("relatedLinks".equals(listType)) {
      log.error("relatedLinks query is unsupported");
      return null;
    }
    
    EOQualifierEvaluation qe = (EOQualifierEvaluation) q;
    
    List<OFSBaseObject> results = new ArrayList<OFSBaseObject>(32);
    
    if ("objectsToRoot".equals(listType)) {
      for (OGoPubDirectory dir = this; dir != null;
           dir = (OGoPubDirectory)dir.container) {
        OFSBaseObject idx;
        
        if (qe != null && !qe.evaluateWithObject(dir))
          continue;
        
        if ((idx = dir.indexDocument(_ctx)) == null) {
          log.warn("folder has no index document for objectsToRoot: " + dir);
          continue;
        }
        results.add(0, idx);
      }
    }
    else if ("objectsToRoot".equals(listType)) {
      log.error("we do not yet support 'all' lists: " + _fs);
    }
    else {
      /* first scan publications */
      List<OFSBaseObject> exportDirs =
        OGoPubWebSite.exportDirectoriesForFolder(this, _ctx);
      
      if (!"all".equals(listType) && exportDirs != null) {
        for (OFSBaseObject dir: exportDirs) {
          if (qe != null && !qe.evaluateWithObject(dir))
            continue;
          results.add(dir);
        }
      }
      
      /* next scan documents */
      
      List<OFSBaseObject> exportFiles =
        OGoPubWebSite.exportFilesForFolder(this, _ctx);
      for (OFSBaseObject doc: exportFiles) {
        if (qe != null && !qe.evaluateWithObject(doc))
          continue;
        if ("all".equals(listType) && !(doc instanceof OGoPubHTMLDocument))
          continue;

        results.add(doc);
      }
    }
    
    /* sort results */
    
    EOSortOrdering[] sos = _fs != null ? _fs.sortOrderings() : null;
    if (results != null && sos != null && sos.length > 0 && results.size() > 0){
      try {
        EOSortOrdering.sort(results, sos);
      }
      catch (ClassCastException e) {
        log.error("could not sort results: " + sos[0] + " in " + this, e);
      }
    }

    /* convert to array and return */
    
    return results.toArray(new OFSBaseObject[results.size()]);
  }
  
  
  /* key/value coding */
  
  @Override
  public Object valueForFileSystemKey(String _key) {
    // if (this.relativePath().equals("index.html"))
    //  System.err.println("KEY: " + _key);

    if ("this".equals(_key) || "self".equals(_key))
      return this;
    
    if ("NSFilePath".equals(_key))
      return OGoPubWebSite.absolutePathInTargetHierarchy(this);
    
    if ("NSFileSubject".equals(_key)) {
      /* this is a hack for Publisher compat */
      if (false) {
        /* this is bad, because we have no context */
        OFSBaseObject idxDoc = this.indexDocument(null /* ctx */); 
        return idxDoc != null ? idxDoc.valueForFileSystemKey(_key) : null;
      }
      else {
        /* hack, directly access the index.[x]html properties */
        Map<String, Object> props = this.propertiesForFileNamed("index.xhtml");
        if (props == null) props = this.propertiesForFileNamed("index.html");
        return props != null ? props.get("NSFileSubject") : null;
      }
    }
    
    if ("NSFileMimeType".equals(_key)) {
      // hm ...
      return "x-skyrix/filemanager-directory";
    }
    
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
        log.info("folder has no properties: " + this);
    }

    return super.handleQueryWithUnboundKey(_key);
  }
}
