package org.opengroupware.pubexport.documents;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOComponentDefinition;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.elements.WOCompoundElement;
import org.getobjects.appserver.elements.WOStaticHTMLElement;
import org.getobjects.appserver.publisher.IGoComponentDefinition;
import org.getobjects.appserver.templates.WOTemplate;
import org.getobjects.appserver.templates.WOTemplateParser;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.ofs.fs.OFSHostFileManager;
import org.opengroupware.pubexport.OGoPubComponentDefinition;

@SuppressWarnings("unchecked")
public abstract class OGoPubComponentDocument extends OGoPubDocument
  implements IGoComponentDefinition
{
  
  /* component definition */

  public IWOComponentDefinition definitionForComponent
    (String _name, String[] _langs, WOResourceManager _rm)
  {
    Class clazz = this.lookupComponentClass(_name, _rm);
    
    WOComponentDefinition cdef = new OGoPubComponentDefinition(this, clazz);
    cdef.setTemplate(this.parseTemplate());
    return cdef;
  }
  
  public abstract Class parserClass();
  
  
  public WOTemplateParser instantiateParser() {
    Class parserClass = this.parserClass();
    if (parserClass == null) {
      log.error("no parser class for document: " + this);
      return null;
    }
    
    WOTemplateParser parser = (WOTemplateParser)
      NSJavaRuntime.NSAllocateObject(parserClass);
    if (parser == null) {
      log.error("could not allocate parser for document: " + this);
      return null;
    }
    
    return parser;
  }
  
  protected static final WOElement emptyElement = new WOStaticHTMLElement("");
  
  
  protected WOTemplate parseTemplate() {
    URL fileURL;
    try {
      // TODO: hack to retrieve a File object
      File file =
        ((OFSHostFileManager)this.fileManager).fileForPath(this.storagePath);
      
      fileURL = file.toURI().toURL();
    }
    catch (MalformedURLException e) {
      log().error("could not parse URL: " + this, e);
      return null;
    }
    
    WOTemplate template = new WOTemplate(fileURL, null /* root */);
    
    if (this.size() == 0) {
      /* got no result? */
      log.info("file is empty: " + this);
      
      /* We create at least an empty element, since empty files are valid
       * in the Publisher. This is to stop complaints of WOComponent.
       */
      template.setRootElement(emptyElement);
      return template;
    }
    
    /* instantiate parser and parse */
    
    WOTemplateParser parser = this.instantiateParser();
    
    if (log.isInfoEnabled())
      log.info("PARSE: " + this.relativePathInTargetHierarchy());
    
    List<WOElement> elements = parser.parseHTMLData(fileURL);
    
    if (log.isInfoEnabled()) log.info("DID PARSE: " + this);
    
    Exception error = parser.lastException();
    if (elements == null) {
      log.error("could not parse template: " + this + ":\n  " +
                (error != null ? error.getMessage() : "unknown error"));
      return null;
    }
    else if (error != null) {
      log.warn("non-fatal errors during parsing: " +
               this.relativePathInTargetHierarchy() +
               ":\n  " + error.getMessage());
    }
    
    /* build template */
    
    if (elements.size() == 0) {
      /* got no result? */
      log.info("parsed no element from the XHTML file: " + this);
      
      /* We create at least an empty element, since empty files are valid
       * in the Publisher. This is to stop complaints of WOComponent.
       */
      template.setRootElement(emptyElement);
    }
    else if (elements.size() == 1)
      template.setRootElement(elements.get(0));
    else
      template.setRootElement(new WOCompoundElement(elements));
    
    return template;
  }
  
}
