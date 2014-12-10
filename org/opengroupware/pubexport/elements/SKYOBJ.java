package org.opengroupware.pubexport.elements;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.elements.WOCompoundElement;
import org.getobjects.appserver.elements.WOConditional;
import org.getobjects.appserver.elements.WOGenericContainer;
import org.getobjects.appserver.elements.WOGenericElement;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.appserver.elements.WOImage;
import org.getobjects.appserver.elements.WOString;
import org.getobjects.foundation.NSObject;
import org.opengroupware.pubexport.associations.OGoPubAnchorAssociation;
import org.opengroupware.pubexport.associations.OGoPubConditionAssociation;
import org.opengroupware.pubexport.associations.OGoPubSelfAssociation;
import org.opengroupware.pubexport.associations.OGoPubURLAssociation;

/**
 * SKYOBJ
 * <p>
 * A factory to produce the various SKYOBJ elements.
 * 
 * @author helge
 */
public class SKYOBJ extends NSObject {
  protected static final Log log = LogFactory.getLog("OGoPubSKYOBJ");

  /**
   * Build an element. This is called by parseSKYOBJElement in the
   * OGOPubHTMLParser.
   * <p>
   * This method just wraps the children into a single WOElement and then
   * calls the build() method below
   * 
   * @param _assocs   - the associations
   * @param _children - the children
   * @return a WOElement
   */
  public static WOElement build
    (Map<String, WOAssociation> _assocs, List<WOElement> _children)
  {
    WOElement child;
    if (_children == null || _children.size() == 0)
      child = null;
    else if (_children.size() == 1)
      child = _children.get(0);
    else
      child = new WOCompoundElement(_children);
    
    return SKYOBJ.build(_assocs, child);
  }
  
  /**
   * Called by the build() method above (aka parseSKYOBJElement() in
   * OGoPubHTMLParser) or by the OGoPubXHTMLParser.
   * <p>
   * This first checks for an "insertvalue" binding:
   * <ul>
   *   <li>template (OGoPubTemplateReference + 'name')
   *   <li>var      (special: 'body' var)
   *   <li>anchor
   *   <li>meta
   *   <li>image
   *   <li>link
   * </ul>
   * It then checks for conditions and lists bindings:
   * <ul>
   *   <li>condition
   *   <li>list
   * </ul>
   * And finally for:
   * <ul>
   *   <li>includetext (OGoPubIncludeText)
   * </ul>
   * 
   * @param _assocs - the associations
   * @param _child  - the template for the element
   * @return a WOElement
   */
  public static WOElement build
    (Map<String, WOAssociation> _assocs, WOElement _child)
  {
    log.info("building SKYOBJ: " + _assocs.keySet());
    
    /* check for insertvalue tags */
    
    WOAssociation iv = _assocs.get("insertvalue");
    if (iv != null) {
      _assocs.remove("insertvalue");
      
      if (!iv.isValueConstant())
        log.error("detected non-constant insertvalue: " + iv);
      
      String s = iv.stringValueInComponent(null /* cursor */);
      if ("template".equals(s))
        return new OGoPubTemplateReference("template", _assocs, _child);

      if ("var".equals(s))
        return buildInsertVar(_assocs, _child);
      
      if ("anchor".equals(s))
        return buildInsertAnchor(_assocs, _child);
      
      if ("meta".equals(s))
        return buildInsertMeta(_assocs, _child);
      
      if ("image".equals(s))
        return buildInsertImage(_assocs, _child);
      
      if ("link".equals(s))
        log.error("insertvalue='link' is unsupported ...");
      else if ("dynamiclink".equals(s))
        log.error("insertvalue='dynamiclink' is unsupported ...");
      else if ("systemexecute".equals(s))
        log.error("insertvalue='systemexecute' is unsupported ...");
      else
        log.error("unknown insertvalue tag: '" + s + "'");
    }
    
    /* check for additional tags */
    
    if (_assocs.containsKey("condition"))
      return buildCondition(_assocs, _child);

    if (_assocs.containsKey("list"))
      return new OGoPubListRepetition("list", _assocs, _child);

    if (_assocs.containsKey("includetext")) {
      // TODO: whats the difference to insertvalue="var" body?
      /* <SKYOBJ js:includetext="path" /> */
      // => can have a path?
      return new OGoPubIncludeText("include:text", _assocs, _child);
    }

    
    if (_assocs.containsKey("context")) {
      log.error("SKYOBJ context is unsupported ...");
      return null;
    }
    if (_assocs.containsKey("frame")) {
      log.error("SKYOBJ frame is unsupported ...");
      return null;
    }
    if (_assocs.containsKey("modifyvar")) {
      log.error("SKYOBJ modifyvar is unsupported ...");
      return null;
    }
    if (_assocs.containsKey("micronavigation")) {
      log.error("SKYOBJ micronavigation is unsupported ...");
      return null;
    }
    if (_assocs.containsKey("switch")) {
      log.error("SKYOBJ switch is unsupported ...");
      return null;
    }
    if (_assocs.containsKey("table")) {
      log.error("SKYOBJ table is unsupported ...");
      return null;
    }
    
    /* report error */
    
    log.error("unknown SKYOBJ: " + _assocs.keySet());
    return null;
  }

  public static WOElement buildInsertVar
    (Map<String, WOAssociation> _assocs, WOElement _child)
  {
    log.info("insert SKYOBJ variable: " + _assocs.keySet());
    
    WOAssociation varName = _assocs.get("name");
    if (varName == null) {
      log.error("missing name of variable to insert: " + _assocs);
      return null;
    }
    _assocs.remove("name");
    
    String b = varName.stringValueInComponent(null /* cursor */);
    
    if ("body".equals(b)) {
      /* special case, "body" is the content */
      // TODO: we might want to support 'body' templates (a template assigned
      //       to the content class of the document)
      return new OGoPubContentReference("var:body", _assocs, _child);
    }
    
    /* patch assocs */
    _assocs.put("value", WOAssociation.associationWithKeyPath(b));
    return new WOString("var", _assocs, _child);
  }
  
  public static WOElement buildInsertMeta
    (Map<String, WOAssociation> _assocs, WOElement _child)
  {
    /*
     * <SKYOBJ insertvalue="meta" name="keywords" content="metakeywords" />
     * 
     * Becomes:
     *   <meta name="keywords" content="Immunf&#228;rbung" />
     */
    
    WOAssociation name = _assocs.get("name");
    
    WOAssociation content = _assocs.get("content");
    if (content == null) {
      /* reuse name if we have no content attribute */
      content = name;
    }
    
    if (content == null) {
      log.warn("SKYOBJ meta has no name or content: " + _assocs);
      return null;
    }
    
    if (content.isValueConstant()) {
      /* turn constant associations into keypathes */
      content = WOAssociation.associationWithKeyPath
        (content.stringValueInComponent(null));
      _assocs.put("content", content);
    }
    
    _assocs.put("elementName", WOAssociation.associationWithValue("meta"));
    
    WOHTMLDynamicElement e =
      new WOGenericElement("meta", _assocs, null /* no children */);
    e.setExtraAttributes(_assocs);
    return e;
  }
  
  public static WOElement buildInsertImage
    (Map<String, WOAssociation> _assocs, WOElement _child)
  {
    /*
     * <SKYOBJ insertvalue="image" name="self" 
     *         witdth="199" height="135" align="right" 
     *         hspace="10" vspace="10"/>
     */
    
    if (_assocs.containsKey("witdth")) {
      log.error("image associations contained 'witdth', probably a typo: " + 
          _assocs);
    }
    
    
    WOAssociation name = _assocs.get("name");
    //System.err.println("IMAGE: " + name);
    if (name != null) {
      _assocs.remove("name");
      
      if (name.isValueConstant()) {
        /* eg: <SKYOBJ name="self" ... /> */
        String s = name.stringValueInComponent(null);
        name = "self".equals(s)
          ? OGoPubSelfAssociation.sharedAssociation
          : WOAssociation.associationWithKeyPath(s);
      }
      name = new OGoPubURLAssociation(name);
      _assocs.put("src", name);
      //System.err.println("  ATTRS: " + _assocs);
    }
    
    if (false) {
      _assocs.put("disableOnMissingLink",
        WOAssociation.associationWithValue(true));
    }
    
    WOHTMLDynamicElement e = new WOImage("var:image", _assocs, _child);
    e.setExtraAttributes(_assocs);
    //System.err.println("  IMAGE: " + e);
    return e;
  }

  public static WOElement buildInsertAnchor
    (Map<String, WOAssociation> _assocs, WOElement _child)
  {
    /*
     * <SKYOBJ insertvalue="anchor" name="self" />
     * 
     * Names: self, next, previous, parent
     */
    WOAssociation a = _assocs.get("name");
    _assocs.remove("name");
    
    if (a == null) {
      log.error("anchor name is missing: " + _assocs);
      return null;
    }
    if (_child == null) {
      log.error("anchor has no content: " + a);
      return null;
    }
    
    _assocs.put("href",        new OGoPubAnchorAssociation(a));
    _assocs.put("elementName", WOAssociation.associationWithValue("a"));
    
    //System.err.println("BUILD ANCHOR: " + _assocs);
    
    // TODO: use OGoPubAnker?
    WOHTMLDynamicElement e = new WOGenericContainer("anker", _assocs, _child);
    e.setExtraAttributes(_assocs);
    return e;
  }

  public static WOElement buildCondition
    (Map<String, WOAssociation> _assocs, WOElement _child)
  {
    log.info("insert SKYOBJ condition: " + _assocs.keySet());
    
    WOAssociation condition = _assocs.get("condition");
    WOAssociation lhs       = _assocs.get("value1");
    WOAssociation rhs       = _assocs.get("value2");
    
    if (lhs == null) {
      if ((lhs = _assocs.get("name1")) != null) {
        // TODO: would be cool to wrap that (so that the key can be dynamic
        lhs = WOAssociation.associationWithKeyPath
          (lhs.stringValueInComponent(null));
        _assocs.remove("name1");
      }
      else if ((lhs = _assocs.get("name")) != null) {
        lhs = WOAssociation.associationWithKeyPath
          (lhs.stringValueInComponent(null));
        _assocs.remove("name");
      }
      else if ((lhs = _assocs.get("value")) != null)
        _assocs.remove("value");
    }
    else
      _assocs.remove("value1");
    
    if (rhs == null) {
      if ((rhs = _assocs.get("name2")) != null) {
        rhs = WOAssociation.associationWithKeyPath
          (rhs.stringValueInComponent(null));
        _assocs.remove("name2");
      }
    }
    else
      _assocs.remove("value2");
    
    _assocs.remove("condition");
    
    //if (_assocs.size() > 0)
    //  log.warn("SKYOBJ condition has unprocessed assocs: " + _assocs);
    
    /* patch WOConditional values */
    
    _assocs.put("condition",
        new OGoPubConditionAssociation(condition, lhs, rhs));
    
    return new WOConditional("if", _assocs, _child);
  }
}
