package org.opengroupware.pubexport.documents;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.ofs.OFSRestorationFactory;
import org.getobjects.ofs.OFSSelect;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;

@SuppressWarnings("rawtypes")
public class OGoPubFactory extends OFSRestorationFactory {
  protected static final Log log = LogFactory.getLog("OGoPubExport");  

  public OGoPubFactory() {
    super();
  }

  /* document factory */
  
  @Override
  public boolean canRestoreObjectFromFileInContext
    (IOFSFileManager _fm, IOFSFileInfo _file, IGoContext _ctx)
  {
    if (super.canRestoreObjectFromFileInContext(_fm, _file, _ctx))
      return true;
    
    return false;
  }
  
  @Override
  public Class ofsClassForDirectoryExtensionInContext
    (String _ext, IGoContext _ctx)
  {
    int len = _ext != null ? _ext.length() : 0;
    if (len == 0) return OGoPubDirectory.class;
    
    if (_ext.equals("website")) return OGoPubWebSite.class;
    
    return OGoPubDirectory.class;
  }

  @Override
  public Class ofsClassForExtensionInContext(String _ext, IGoContext _ctx) {
    int len = _ext != null ? _ext.length() : 0;
    if (len == 0) return OGoPubDocument.class;

    switch (len) {
      case 2:
        if (_ext.equals("sh")) return OGoPubPlainTextDocument.class;
        break;
        
      case 3:
        if (_ext.equals("gif")) return OGoPubImageDocument.class;
        if (_ext.equals("jpg")) return OGoPubImageDocument.class;
        if (_ext.equals("png")) return OGoPubImageDocument.class;
        if (_ext.equals("ico")) return OGoPubImageDocument.class;

        if (_ext.equals("txt")) return OGoPubPlainTextDocument.class;
        
        if (_ext.equals("sfm")) return OGoPubPlainTextDocument.class;
        break;
        
      case 4:
        if (_ext.equals("html")) return OGoPubHTMLDocument.class;
        if (_ext.equals("jpeg")) return OGoPubImageDocument.class;
        
        break;
        
      case 5:
        if (_ext.equals("xhtml")) return OGoPubXHTMLDocument.class;
        if (_ext.equals("xtmpl")) return OGoPubTemplate.class;
        break;
    }
    
    /* publisher specific objects */
    if (_ext.equals("ogopubrss"))    return OGoPubRSSFeed.class;
    if (_ext.equals("ogopubselect")) return OFSSelect.class;
    
    return OGoPubDocument.class;
    
    /* ask superclass */
    //return super.ofsClassForExtensionInContext(_ext, _ctx);
  }
}
