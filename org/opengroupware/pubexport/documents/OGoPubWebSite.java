package org.opengroupware.pubexport.documents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.GoTraversalPath;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.IGoFolderish;
import org.getobjects.ofs.OFSBaseObject;

/**
 * OGoPubWebSite
 * <p>
 * The root of a site for exporting.
 */
public class OGoPubWebSite extends OGoPubDirectory {
  private static final String[] emptyStringArray = new String[0];

  /* target (export) hierarchy */

  public boolean isTargetHierarchyRoot() {
    return true;
  }
  
  public OGoPubDirectory rootOfTargetHierarchy() {
    return this;
  }

  public String[] pathInTargetHierarchy() {
    return emptyStringArray;
  }
  
  public String absolutePathInTargetHierarchy() {
    return "/";
  }
  
  protected static final EOQualifier foldersQualifier =
    EOQualifier.qualifierWithQualifierFormat
      ("NSFileType = 'NSFileTypeDirectory'");
  protected static final EOQualifier filesQualifier =
    EOQualifier.qualifierWithQualifierFormat
      ("NSFileType = 'NSFileTypeRegular'");
  
  @SuppressWarnings("unchecked")
  public static List<OFSBaseObject> exportDirectoriesForFolder
    (IGoFolderish _folder, IGoContext _ctx)
  {
    if (_folder == null) {
      log.info("exportDirectoriesForFolder got no folder, ctx: " + _ctx);
      return null;
    }
    if (_ctx == null)
      log.warn("exportDirectoriesForFolder: got no context!");
    
    
    EODataSource ds = _folder.folderDataSource(_ctx);
    ds.setFetchSpecification
      (new EOFetchSpecification(null, foldersQualifier, null));
    
    List<OFSBaseObject> objs = ds.fetchObjects();
    
    if (objs == null) {
      log.warn("exportDirectoriesForFolder: fetch returned null: " +
          _folder + "\n  qualifier=" +
          foldersQualifier,
          ds.lastException());
    }
    else if (objs.size() == 0 && log.isInfoEnabled()) {
      log.warn("exportDirectoriesForFolder: fetch returned no children: " +
          _folder + "\n  qualifier=" +
          foldersQualifier);
    }
    
    return objs;
  }
  
  @SuppressWarnings("unchecked")
  public static List<OFSBaseObject> exportFilesForFolder
    (IGoFolderish _folder, IGoContext _ctx)
  {
    if (_folder == null)
      return null;
    
    EODataSource ds = _folder.folderDataSource(_ctx);
    ds.setFetchSpecification
      (new EOFetchSpecification(null, filesQualifier, null));
    return ds.fetchObjects();
  }
  
  /* child management */
  
  // TODO: all those static methods belong into an 'Exporter' object
  
  public static OGoPubDirectory siteRootForObject(Object _object) {
    return OGoPubWebSite.rootOfTargetHierarchy(_object);
  }
  public static OGoPubDirectory rootOfTargetHierarchy(Object _object) {
    Object dir = NSJavaRuntime.boolValueForKey(_object, "isFolderish")
      ? _object
      : IGoLocation.Utility.containerForObject(_object);
    
    while (dir != null &&
           !NSJavaRuntime.boolValueForKey(dir, "isTargetHierarchyRoot")) {
      Object nextDir = IGoLocation.Utility.containerForObject(dir);
      if (nextDir == null)
        break;
      
      dir = nextDir;
    }
    
    return (OGoPubDirectory)dir;
  }
  
  public static String exportNameOfObject(Object _object, Object _container) {
    if (_object == null) return null;

    String name =
      IGoLocation.Utility.nameOfObjectInContainer(_object, _container);
    
    if (_object instanceof OGoPubDocument) {
      OGoPubDocument doc = (OGoPubDocument)_object;
      
      // hack: fix me
      String mimeType = doc.mimeType();
      if (mimeType.startsWith("text/html"))         name += ".html";
      else if (mimeType.startsWith("image/gif"))    name += ".gif";
      else if (mimeType.startsWith("image/png"))    name += ".png";
      else if (mimeType.startsWith("image/jpeg"))   name += ".jpg";
      else if (mimeType.startsWith("image/x-icon")) name += ".ico";
      else if (mimeType.startsWith("text/plain"))   name += ".txt";
      else if (mimeType.startsWith("text/css"))     name += ".css";
      else {
        String[] sp = doc.storagePath();
        if (sp != null && sp.length > 0) {
          log.info("using storage extension for: " + doc + 
                   " (" + doc.mimeType() + ")");
          
          int eidx = sp[sp.length - 1].indexOf('.');
          if (eidx > 0)
            name += sp[sp.length - 1].substring(eidx);
        }
      }
    }
    
    return name;
  }
  
  public static String[] exportPathOfObjectUntilStopKey
    (Object _object, String _k)
  {
    /* Traverse the hierarchy until either root is found or the given key
     * evaluates to 'true' on the traversed collection.
     */
    if (_object == null || _k == null)
      return null;
    
    /* this is also overridden in OGoPubWebSite */
    if (NSJavaRuntime.boolValueForKey(_object, _k))
      return emptyStringArray;
    
    /* build up document hierarchy in a list of strings */
    
    List<String> cPath = new ArrayList<String>(12);
    
    Object container = IGoLocation.Utility.containerForObject(_object);
    if (container == null) /* we are already at the root */
      return emptyStringArray;
    
    String name = exportNameOfObject(_object, container);
    if (name == null) {
      log.warn("object has no name: " + _object +
               "\n  in container: " + container);
    }
    else
      cPath.add(name);
    
    Object dir = container;
    while (dir != null &&
           !NSJavaRuntime.boolValueForKey(dir, _k))
    {
      Object nextContainer = IGoLocation.Utility.containerForObject(dir);
      name = exportNameOfObject(dir, nextContainer);
      if (name == null)
        break;
      
      cPath.add(name);
      dir = nextContainer;
    }
    
    /* convert hierarchy into a path */
    
    String[] lPath = new String[cPath.size()];
    for (int i = 0; i < lPath.length; i++)
      lPath[lPath.length - i - 1] = cPath.get(i);
    
    return lPath;
  }
  
  public static String[] pathOfObjectUntilStopKey
    (GoTraversalPath _path, String _k, int _skip)
  {
    /* Traverse the lookup path until either root is found or the given key
     * evaluates to 'true' on the traversed collection.
     */
    if (_path == null || _k == null)
      return null;
    
    /* build up document hierarchy in a list of strings */
    
    List<String> cPath = new ArrayList<String>(12);

    String[] spath = _path.path();
    Object[] tpath = _path.objectTraversalPath();
    if (tpath == null)
      return null;
    
    for (int i = tpath.length - 1 - _skip; i >= 0; i--) {
      if (NSJavaRuntime.boolValueForKey(tpath[i], _k))
        break;
      
      cPath.add(spath[i]);
    }
    
    /* convert hierarchy into a path */
    
    String[] lPath = new String[cPath.size()];
    for (int i = 0; i < lPath.length; i++)
      lPath[lPath.length - i - 1] = cPath.get(i);
    
    return lPath;
  }
  
  public static Object lookupPath
    (Object _object, String _path, IGoContext _ctx, boolean _acquire)
  {
    // TODO: this is crap, fix the name and implementation
    /*
     * Note: when you run this against a folder, lookup will be relative to
     *       the container! eg:
     *         this = /en/images
     *         path = css
     *       => /en/css (NOT /en/images/css)
     */
    if (_path == null)
      return _object;

    IGoObject doc = (IGoObject)IGoLocation.Utility.containerForObject(_object);
    if (_path.startsWith("/"))
      doc = rootOfTargetHierarchy(_object);

    //System.err.println("LOOKUP " + _path + " in " + doc);

    for (String pc: _path.split("/")) {
      if (doc == null)
        return null;

      if (".".equals(pc) || pc.length() == 0)
        continue;

      if ("..".equals(pc)) {
        doc = (IGoObject)IGoLocation.Utility.containerForObject(doc);
        continue;
      }

      IGoObject nextDoc = (IGoObject)doc.lookupName(pc, _ctx, _acquire);
      if (nextDoc == null) {
        log.warn("did not find document: '" + pc +
            "'\n  in:     " + doc +
            "\n  cursor: " +
            ((_ctx instanceof WOContext)?((WOContext)_ctx).cursor():"-"));
        break;
      }
      doc = nextDoc;
    }
    
    return doc;
  }
  
  public static String[] pathOfDocumentInTargetHierarchy(Object _o) {
    return exportPathOfObjectUntilStopKey(_o, "isTargetHierarchyRoot");
  }
  
  public static String relativePathInTargetHierarchy(Object _o) {
    // TODO: do not use, use array-pathes instead
    if (_o == null)
      return null;
    
    String[] path = 
      OGoPubWebSite.exportPathOfObjectUntilStopKey(_o, "isTargetHierarchyRoot");
    if (path == null || path.length == 0)
      return null;
    
    /* Note: we don't use File.pathSeparator, because its ':' on MacOS */
    return UString.componentsJoinedByString(path, "/");
  }
  public static String absolutePathInTargetHierarchy(Object _o) {
    // TODO: do not use, use array-pathes instead
    if (_o == null)
      return null;
    
    String s = relativePathInTargetHierarchy(_o);
    return s != null ? ("/" + s) : "/";
  }
  
  public static File targetFile(File _targetRoot, OFSBaseObject _srcfile) {
    if (_targetRoot == null || _srcfile == null)
      return null;
    
    String[] path =
      OGoPubWebSite.exportPathOfObjectUntilStopKey(_srcfile, "isTargetHierarchyRoot");
    
    return new File(_targetRoot, UString.componentsJoinedByString(path, "/"));
  }
}
