package org.opengroupware.pubexport;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoComponentDefinition;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoSecuredObject;
import org.getobjects.appserver.publisher.GoContainerResourceManager;
import org.opengroupware.pubexport.documents.OGoPubComponentDocument;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/**
 * OGoPubResourceManager
 * <p>
 * Why does it need the 'parent' resource manager?
 */
public class OGoPubResourceManager extends GoContainerResourceManager {
  protected static final Log log = LogFactory.getLog("OGoPubExport");
  
  protected Map<String, IWOComponentDefinition> cache;
  
  public OGoPubResourceManager
    (IGoObject object, WOResourceManager _rm, IGoContext _ctx)
  {
    super(object, _rm, _ctx);
    this.cache = new HashMap<String, IWOComponentDefinition>(16);
  }
  
  /* lookup context */
  
  public Object container() {
    return this.container;
  }
  
  /* lookup */
  
  @Override
  public IWOComponentDefinition definitionForComponent
    (String _name, String[] _langs, WOResourceManager _clsctx)
  {
    // this is called by the WOResourceManager machinery. We lookup the name
    // and then return a component definition for it
    // - its called by definitionForComponent(String,String[],classctx)
    // - which is called by pageWithName() and templateWithName()
    
    // Note: the extension in '_name' doesn't matter anymore (in OFS)
    if (log.isInfoEnabled())
    log.info("lookup page " + _name + " in " + this.container);

    // hm, would be better to resolve the name first?
    IWOComponentDefinition cdef =
      this.cache != null ? this.cache.get(_name) : null;
    if (cdef != null) {
      log.info("found cdef named " + _name + " in cache: " + cdef);
      return cdef;
    }
    
    /* lookup publisher document */

    OGoPubComponentDocument doc = primaryLookupPage(_name, this.context);
    if (doc == null) {
        /* attempt to lookup regular component outside the hierarchy */
        // TODO: we might need to consider security implications for untrusted
        //       accounts
        if (this.parentRM != null) {
          cdef = this.parentRM.definitionForComponent(_name, _langs, _clsctx);
          if (cdef != null)
            return cdef;
        }
        
        log.warn("did not find page: " + _name + " in " + this.container);
        return null;
    }
      
    log.info("  found page: " + doc);

    /* lookup cdef */
    
    if ((cdef = doc.definitionForComponent(_name, _langs, _clsctx)) == null) {
      log.error("got no component-definition for document: " + doc);
      return null;
    }
    
    /* cache cdef */

    if (this.cache != null && this.shouldCachePageWithName(_name))
      this.cache.put(_name, cdef);
    
    return cdef;
  }
  
  public boolean shouldCachePageWithName(String _name) {
    /* cache templates */
    if (_name == null)
      return false;
    
    if (_name.endsWith(".xtmpl"))
      return true;

    return (_name.indexOf('.') < 0); // default to cache non-ext pages
  }

  /**
   * Lookup an OGoPubComponentDocument with the given name relative to the
   * container.
   * 
   * @param _name - name of component/page to look up
   * @param _ctx  - the context in which to lookup
   * @return an OGoPubComponentDocument or null if none was found
   */
  protected OGoPubComponentDocument primaryLookupPage
    (String _name, IGoContext _ctx)
  {
    // isn't that ALWAYS the clientObject in _ctx?
    // hm, currently its the folder, not the client object
    // for caching purposes, since the lookup is likely the same. but then
    // the OFS already caches keys
    IGoObject rdoc = (IGoObject) this.container;
    if (rdoc == null)
      log.error("resource manager has no container?: " + this);
    
    if (_name.indexOf('/') == -1) {
      // no separators in name, direct lookup
      // TBD: better use IGoSecuredObject.lookupName
      rdoc = (IGoObject)(rdoc != null
        ? rdoc.lookupName(_name, _ctx, true /* acquire */) : null); 
    }
    else {
      // TODO: consolidate the lookup
      // Note: we cannot use lookupPath(), this breaks lookup because it
      //       starts one too high => need to fix that
      //System.err.println("LOOKUP " + _name + " IN: " + rdoc);
      
      if (_name.startsWith("/")) {
        // Hm, this is because the user can use names like
        // "/contact/mypage.html". Which needs to be resolved against the
        // site root (for multisite OFS hierarchies)
        // => OFSSite object?!
        // Note: the site root is the same like the target hierarchy
        rdoc = OGoPubWebSite.siteRootForObject(rdoc);
      }
      
      //System.err.println("  root: " + rdoc);

      for (String pc: _name.split("/")) {
        if (rdoc == null)
          break;

        if (".".equals(pc) || "".equals(pc) || "/".equals(pc))
          continue;

        if ("..".equals(pc)) {
          rdoc = (IGoObject)IGoLocation.Utility.containerForObject(rdoc);
          continue;
        }
        
        rdoc = (IGoObject)IGoSecuredObject.Utility.lookupName
          (rdoc, pc, _ctx, true /* acquire */);
      }
    }

    //System.err.println("  GOT: " + rdoc);

    if (rdoc instanceof OGoPubComponentDocument)
      return (OGoPubComponentDocument)rdoc;
    
    if (rdoc != null && log.isWarnEnabled()) {
      log.warn("document located for " + _name + " is not a page:\n" + 
          rdoc + "\n  dir: " + this.container);
    }
    
    return null;
  }

  
  @SuppressWarnings("unchecked")
  @Override
  public Class lookupComponentClass(String _name) {
    /* Note: this is only called for components */
    
    // note: changed NOT to resolve '.' as component pathes (eg common.Frame)
    
    IGoComponentDefinition joComp = this.findGoComponentWithName(_name);
    return (joComp != null)
      ? joComp.lookupComponentClass(_name, this)
      : this.parentRM.lookupComponentClass(_name);
  }

}
