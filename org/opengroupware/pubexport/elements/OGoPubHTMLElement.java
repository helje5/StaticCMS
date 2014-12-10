package org.opengroupware.pubexport.elements;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOCompoundElement;
import org.getobjects.appserver.elements.WOGenericContainer;
import org.getobjects.appserver.elements.WOGenericElement;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.appserver.elements.WOHyperlink;
import org.getobjects.appserver.elements.WOImage;
import org.opengroupware.pubexport.associations.OGoPubURLAssociation;
import org.opengroupware.pubexport.parsers.OGoPubXHTMLParser;

/*
 * OGoPubHTMLElement
 * 
 * TODO: document
 */
public class OGoPubHTMLElement extends WOHTMLDynamicElement {
  protected static final Log log = LogFactory.getLog("OGoPubHTMLElement");
  
  protected WOElement wrappedElement;
  
  public OGoPubHTMLElement
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.wrappedElement = this.buildWrappedElement(_name, _assocs, _template);
  }
  
  protected WODynamicElement buildWrappedElement
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    _assocs.put("elementName", this.tagNameAssociation(_name));
    
    patchLinkAssociations(_name, _assocs);
    //System.err.println("BUILD: " + _assocs);
    
    WODynamicElement elem = this.isContainerTag(_name, _template)
      ? new WOGenericContainer(_name, _assocs, _template)
      : new WOGenericElement(_name, _assocs, _template);
      
    return elem;
  }
  
  public boolean isContainerTag(String _tag, WOElement _template) {
    if (_template != null)
      return true;
    
    if (_tag != null) {
      String[] tags = OGoPubXHTMLParser.containerElements;
      for (int i = 0; i < tags.length; i += 2) {
        if (_tag.equals(tags[i + 1]))
          return true;
      }
    }
    
    return false;
  }
  public WOAssociation tagNameAssociation(String _name) {
    return WOAssociation.associationWithValue(_name);
  }
  
  @Override
  public void setExtraAttributes(Map<String, WOAssociation> _attrs) {
    if (this.wrappedElement instanceof WODynamicElement)
      ((WODynamicElement)this.wrappedElement).setExtraAttributes(_attrs);
  }

  /* factory */
  
  public static final WOAssociation trueAssociation =
    WOAssociation.associationWithValue(Boolean.TRUE);
  
  public static WOElement build
    (String _name, Map<String, WOAssociation> _assocs, List<WOElement> _tmpl)
  {
    WOElement child = null;
    
    if ("img".equals(_name)) {
      if (!_assocs.containsKey("disableOnMissingLink"))
        _assocs.put("disableOnMissingLink", trueAssociation);
      
      patchLinkAssociations(_name, _assocs);
      return new WOImage(_name, _assocs, null);
    }
    
    /* consolidate template */
    
    if (_tmpl != null) {
      if (_tmpl.size() == 0)
        child = null;
      else if (_tmpl.size() == 1)
        child = _tmpl.get(0);
      else
        child = new WOCompoundElement(_tmpl);
    }
    
    /* check for special elements */
    
    if ("a".equals(_name)) {
      /* either a real link or an anchor, eg: <a name="abc"> </a> */
      if (!_assocs.containsKey("name")) {
        /* hide a tag when the link is missing/incorrect */
        if (!_assocs.containsKey("disableOnMissingLink"))
          _assocs.put("disableOnMissingLink", trueAssociation);
        patchLinkAssociations(_name, _assocs);
        
        return new WOHyperlink(_name, _assocs, child);
      }
      else if (_assocs.containsKey("href"))
        log.warn("link has a name *and* a href: " + _assocs);
    }
    
    /* per default use a generic element */
    return new OGoPubHTMLElement(_name, _assocs, child);
  }

  public static void patchLinkAssociations
    (String _tag, Map<String, WOAssociation> _assocs)
  {
    if (_assocs == null)
      return;
    
    _tag = _tag.toLowerCase();
    
    /* "src" */
    
    if ("a".equals(_tag) || "link".equals(_tag)) {
      WOAssociation a = _assocs.get("href");
      if (a != null && a.isValueConstant())
        _assocs.put("href", new OGoPubURLAssociation(a));
    }
    else if ("img".equals(_tag) || "script".equals(_tag) ||
             "input".equals(_tag)) {
      WOAssociation a = _assocs.get("src");
      if (a != null && a.isValueConstant())
        _assocs.put("src", new OGoPubURLAssociation(a));
    }
    else if ("table".equals(_tag) || "td".equals(_tag) || "body".equals(_tag)) {
      WOAssociation a = _assocs.get("background");
      if (a != null && a.isValueConstant())
        _assocs.put("background", new OGoPubURLAssociation(a));
    }
    else if ("form".equals(_tag)) {
      WOAssociation a = _assocs.get("action");
      if (a != null && a.isValueConstant())
        _assocs.put("action", new OGoPubURLAssociation(a));
    }
  }
  
  /* request handling */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.wrappedElement != null)
      this.wrappedElement.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.wrappedElement == null)
      return null;
    
    return this.wrappedElement.invokeAction(_rq, _ctx);
  }
  
  /* template walking */

  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.wrappedElement != null)
      this.wrappedElement.walkTemplate(_walker, _ctx);
  }

  /* generate response */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (this.wrappedElement != null)
      this.wrappedElement.appendToResponse(_r, _ctx);
  }
}
