package org.opengroupware.pubexport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.GoTraversalPath;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.getobjects.foundation.UData;
import org.getobjects.ofs.IGoFolderish;
import org.getobjects.ofs.OFSBaseObject;
import org.getobjects.ofs.fs.IOFSFileManager;
import org.getobjects.ofs.fs.OFSHostFileManager;
import org.opengroupware.pubexport.documents.OGoPubDirectory;
import org.opengroupware.pubexport.documents.OGoPubHTMLDocument;
import org.opengroupware.pubexport.documents.OGoPubTemplate;
import org.opengroupware.pubexport.documents.OGoPubWebSite;
import org.opengroupware.pubexport.documents.Util;

public class pubexport extends OGoPubExport {
  public File        target;
  public EOQualifier match;

  
  public static void main(String[] _args) {
    pubexport app = new pubexport();
    app.init();
    
    if (_args.length < 2) {
      System.err.println("usage: pubexport <srcdir> <targetdir> [qualifier]");
      System.exit(1);
    }
  
    String path = _args[0];
    if (path == null || path.length() == 0 || path.equals("."))
      path = System.getProperty("user.dir");
    File f      = new File(path);

    path = _args[1];
    if (path == null || path.length() == 0 || path.equals("."))
      path = System.getProperty("user.dir");
    File target = new File(path);

    EOQualifier match = null;
    if (_args.length > 2)
      match = EOQualifier.qualifierWithQualifierFormat(_args[2]);
    
    IOFSFileManager fm = new OFSHostFileManager(f);
    if (fm == null) {
      log.error("could not create filemanager for file: " + f);
      System.exit(2);
    }
    
    OGoPubWebSite root = new OGoPubWebSite();
    root.setStorageLocation(fm, null /* root path */);
    root.setLocation(null, null); /* we are root */
    
    app.export(root, target, match);
  }

  public void export(OGoPubWebSite _root, File _target, EOQualifier _match) {
    this.root   = _root;
    this.target = _target;
    this.match  = _match;
    
    log.info("root: " + this.root);
    
    log.info("loading directory properties ...");
    this.root.loadProperties(); /* below we only load the children ... */
    log.info("done: " + this.root + "\n");
    
    log.info("creating directory hierarchy ...");
    this.createDirectoryHierarchy(root, 0);
    log.info("done: " + this.root + "\n");

    log.info("export pages and resources ...");
    this.exportPages(root, 0);
    log.info("done: " + this.root + "\n");
  }
  
  
  /* directory hierarchy */
  
  public void createDirectoryHierarchy(OFSBaseObject _folder, int _depth) {
    if (_folder == null)
      return;

    if (log.isInfoEnabled()) {
      log.info(Util.prefixForDepth(_depth) + "mkdir: " +
          _folder.nameInContainer());
    }
    
    File target = OGoPubWebSite.targetFile(this.target, _folder);
    if (target.exists()) {
      log.info(Util.prefixForDepth(_depth) + 
               "target directory already exists: " + target);
    }
    else {
      log.info(Util.prefixForDepth(_depth) + 
          "creating target directory: " + target);
      if (!target.mkdir())
        log.error("failed to create target directory: " + target);
    }
    
    String rqPath = OGoPubWebSite.relativePathInTargetHierarchy(_folder);
    if (log.isInfoEnabled())
      log.info(Util.prefixForDepth(_depth) + "  relpath: " + rqPath);

    WOContext context = this.contextForExportPath(rqPath);
    
    if (_folder instanceof OGoPubDirectory) { // hack
      if (((OGoPubDirectory)_folder).context() == null)
        ((OGoPubDirectory)_folder)._setContext(context);
    }
    
    List<OFSBaseObject> dirs =
      OGoPubWebSite.exportDirectoriesForFolder((IGoFolderish)_folder, context);
    
    for (OFSBaseObject subdir: dirs)
      this.createDirectoryHierarchy(subdir, _depth + 1);
  }
  
  
  /* export pages */
  
  public Map<String,List<String>> exportRequestHeaders() {
    // TODO
    // ag: accept could trigger automatic image conversion ;-)
    return null;
  }
  
  public WOContext contextForExportPath(String rqPath) {
    WORequest request =
      new WORequest("GET", rqPath, "HTTP/1.0",
                    this.exportRequestHeaders(),
                    null /* contents */,
                    null /* userInfo */);
    
    WOContext context = new WOContext(this /* app */, request);
    return context;
  }
  
  public WOContext preparedContextForObject(OFSBaseObject page) {
    /* setup request environment */
    
    String rqPath = OGoPubWebSite.relativePathInTargetHierarchy(page);
    WOContext context = this.contextForExportPath(rqPath);
    
    String[] pathInTargetHierarchy =
      OGoPubWebSite.exportPathOfObjectUntilStopKey(page,
          "isTargetHierarchyRoot");
    
    GoTraversalPath tpath =
      new GoTraversalPath(pathInTargetHierarchy, this.root, context);
    tpath.traverse(); /* kinda superflous, but keep it standard ... */
    
    context._setGoTraversalPath(tpath);
    
    return context;
  }
  
  
  /**
   * Exports a file as-is, that is, it copies the file to the target
   * destination.
   * 
   * @param doc
   * @param _depth
   * @param target
   */
  public void exportResource(OFSBaseObject doc, int _depth, File target) {
    if (doc instanceof OGoPubHTMLDocument) /* skip pages, exported below */
      return;
    if (doc instanceof OGoPubTemplate) /* skip templs */
      return;
    
    if (log.isDebugEnabled()) {
      log.debug(Util.prefixForDepth(_depth) +
                "copy to resource " + target + " from: " + doc);
    }

    // TBD: would be nice to setup a streaming response to the target file,
    //      then we could use the regular renderer
    // to do this we would need a WORequest subclass which supports
    // prepareForStreaming()/outputStream()
    
    InputStream      in  = null;
    FileOutputStream out = null;
    try {
      in  = ((OFSBaseObject)doc).openStream();
      out = new FileOutputStream(target, false /* do not append */);
      Util.copyStream(in, out);
    }
    catch (FileNotFoundException e) {
      log.error("could not copy resource " + doc + " to " + target);
    }
    finally {
      if (in  != null) {
        try { in.close(); } catch (IOException e) {}
      }
      if (out != null) {
        try { out.close(); } catch (IOException e) {}
      }
    }
  }

  
  public void exportPages(OFSBaseObject _folder, int _depth) {
    if (_folder == null)
      return;
    
    WOContext context = this.contextForExportPath("-"); // dummy path
    
    List<OFSBaseObject> exportFiles =
      OGoPubWebSite.exportFilesForFolder((IGoFolderish)_folder, context);
    
    if (exportFiles != null && exportFiles.size() > 0) {
      log.info(Util.prefixForDepth(_depth) + 
               "processing pages of directory: " + _folder.nameInContainer() +
               " (" + exportFiles.size() + ")");
      
      WOResourceManager     oldRM = this.resourceManager();
      OGoPubResourceManager rm =
        new OGoPubResourceManager(_folder, oldRM, context);
      this.setResourceManager(rm);
      
      for (OFSBaseObject file: exportFiles) {
        /* check whether the page qualifies */
        if (this.match != null) {
          if (!((EOQualifierEvaluation)this.match).evaluateWithObject(file))
            continue;
        }
        
        /* find target */
        
        File target = OGoPubWebSite.targetFile(this.target, file);
        if (target.exists()) {
          log.info("target page already exists: " + target);
          // TODO: delete?
          //continue;
        }
        
        if (file instanceof OGoPubHTMLDocument) {
          // TBD: should we just do this for resources as well?
          WOContext pageContext = this.preparedContextForObject(file);
          
          /* invoke renderer */
          OGoPubRenderer renderer = OGoPubRenderer.sharedRenderer;
          Exception rerror = renderer.renderObjectInContext(file, pageContext);
          if (rerror != null)
            log.error("rendering error on page: "+ file);
          else {
            byte[] content = pageContext.response().content();
          
            if (UData.writeToFile(content, target, false) != null)
              log.error("could not copy response for " + file + " to " +target);
          }
        }
        else {
          this.exportResource(file, _depth, target);
        }
      }

      this.setResourceManager(oldRM /* reset per-folder resource manager */);
    }

    /* recurse */
    
    List<OFSBaseObject> dirs =
      OGoPubWebSite.exportDirectoriesForFolder((IGoFolderish)_folder, context);
    for (OFSBaseObject subdir: dirs)
      this.exportPages(subdir, _depth + 1);
  }
}
