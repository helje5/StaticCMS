package org.opengroupware.pubexport.documents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * TODO: this is crap, probably the functionality is already contained in
 *       Java?
 */
public class Util {
  public static final Log log = LogFactory.getLog("OGoPubExport");  
  
  /* MIME types */
  
  public static String[] extToMIME = {
    "html",       "text/html",
    "xhtml",      "application/xhtml+xml",
    
    "txt",        "text/plain",
    "css",        "text/css",
    "js",         "text/javascript",
    "make",       "text/x-makefile",
    "pl",         "text/x-perl",
    
    "gif",        "image/gif",
    "ico",        "image/x-icon",
    "jpg",        "image/jpeg",
    "png",        "image/png",
    
    "pdf",        "application/pdf",
    "sh",         "application/x-sh",
    "sed",        "application/x-sed",
    "gz",         "application/x-gzip",
    "zip",        "application/zip",
    "xul",        "application/vnd.mozilla.xul+xml",
    
    "xtmpl",        "skyrix/xtmpl",
    "sfm",          "skyrix/form",
    "website",      "x-ogopub/website",
    "ogopubrss",    "x-ogopub/rssfeed",
    "ogopubselect", "x-ogopub/select"
  };
  
  public static String mimeTypeForExtension(String _ext) {
    if (_ext == null || _ext.length() == 0)
      return null;
    
    for (int i = 1; i < extToMIME.length; i += 2) {
      if (_ext.equals(extToMIME[i - 1]))
        return extToMIME[i];
    }
    
    int idx = _ext.indexOf('.');
    if (idx != -1)
      return mimeTypeForExtension(_ext.substring(idx + 1));
    
    return null;
  }
  
  public static String mimeTypeForFile(File _file) {
    if (_file == null)
      return null;
    
    String fn = _file.getName();
    
    int idx = fn.indexOf('.', 1 /* avoid filenames starting with a dot */);
    if (idx != -1) {
      String mt = mimeTypeForExtension(fn.substring(idx + 1));
      if (mt != null) return mt;
    }
    else {
      if (fn.equals("ChangeLog"))
        return "text/plain";
      if (fn.equals("README"))
        return "text/plain";
      if (fn.equals("NOTES"))
        return "text/plain";
      
      if (fn.startsWith("GNUmakefile"))
        return "text/x-make";
    }
    
    if (_file.isDirectory())
      return "x-skyrix/filemanager-directory";
    
    //System.err.println("found no MIME type for file: " + _file);
    return null;
  }
  
  /* copying */

  public static void copyStream(InputStream fis, OutputStream fos) {
    try {
      byte[] buffer = new byte[0xFFFF];
      for (int len; (len = fis.read(buffer)) != -1; )
        fos.write( buffer, 0, len );
    }
    catch (IOException e) {
      System.err.println( e );
    }
    finally {
      if (fis != null) {
        try {
          fis.close();
        }
        catch (IOException e) { e.printStackTrace(); }
      }
      if (fos != null) {
        try {
          fos.close();
        }
        catch (IOException e) { e.printStackTrace(); }
      }
    }
  }
  
  /* util */
  
  public static String prefixForDepth(int _depth) {
    // cant this be done with a simple format pattern?
    switch (_depth) {
      case 0: return "";
      case 1: return "  ";
      case 2: return "    ";
      case 4: return "      ";
      case 5: return "        ";
      default: {
        StringBuilder sb = new StringBuilder(_depth * 2 + 1);
        for (int i = 0; i < _depth; i++)
          sb.append("  ");
        return sb.toString();
      }
    }
  }

  /* URL processing */
  
  public static String buildURLForPath(String[] _path) {
    if (_path == null)
      return null;

    int len = _path.length;
    if (len == 0)
      return "";
    
    StringBuilder url = new StringBuilder(len * 16);
    for (int i = 0; i < len; i++) {
      if (i != 0) url.append("/");
      
      // TODO: escape HTML
      url.append(_path[i]);
    }
    
    return url.toString();
  }
}
