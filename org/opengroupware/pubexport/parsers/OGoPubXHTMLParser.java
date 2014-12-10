package org.opengroupware.pubexport.parsers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.elements.WOBody;
import org.getobjects.appserver.elements.WOCheckBox;
import org.getobjects.appserver.elements.WOCompoundElement;
import org.getobjects.appserver.elements.WOConditional;
import org.getobjects.appserver.elements.WOConditionalComment;
import org.getobjects.appserver.elements.WOCopyValue;
import org.getobjects.appserver.elements.WOEntity;
import org.getobjects.appserver.elements.WOGenericContainer;
import org.getobjects.appserver.elements.WOGenericElement;
import org.getobjects.appserver.elements.WOHtml;
import org.getobjects.appserver.elements.WOHyperlink;
import org.getobjects.appserver.elements.WOImage;
import org.getobjects.appserver.elements.WOInput;
import org.getobjects.appserver.elements.WOJavaScript;
import org.getobjects.appserver.elements.WOPopUpButton;
import org.getobjects.appserver.elements.WORepetition;
import org.getobjects.appserver.elements.WOStaticHTMLElement;
import org.getobjects.appserver.elements.WOString;
import org.getobjects.appserver.elements.WOText;
import org.getobjects.appserver.templates.WOTemplateParser;
import org.getobjects.appserver.templates.WOTemplateParserHandler;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.XMLNS;
import org.opengroupware.pubexport.associations.OGoPubJavaScriptAssociation;
import org.opengroupware.pubexport.elements.OGoPubHTMLElement;
import org.opengroupware.pubexport.elements.SKYOBJ;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * OGoPubXHTMLParser
 * <p>
 * Parse a DOM tree and convert it into a WOElement hierarchy. To build SKYOBJ
 * elements the object calls the SKYOBJ.build() factory method.
 * 
 * <h4>var:x elements</h4>
 * <pre>
 *   &lt;var:if&gt;        - WOConditional
 *   &lt;var:ifnot&gt;     - WOConditional negate=true
 *   &lt;var:string&gt;    - WOString
 *   &lt;var:foreach&gt;   - WORepetition
 *   &lt;var:ieif&gt;      - WOConditionalComment
 *   &lt;var:entity&gt;    - WOEntity
 *   
 *   &lt;var:input&gt;     - WOInput
 *   &lt;var:a&gt;         - WOHyperlink
 *   &lt;var:popup&gt;     - WOPopUpButton
 *   &lt;var:checkbox&gt;  - WOCheckBox
 *   &lt;var:copyvalue&gt; - WOCopyValue
 *   &lt;var:img&gt;       - WOImage
 *   &lt;var:textarea&gt;  - WOText
 *   &lt;var:html&gt;      - WOHtml
 *   &lt;var:body&gt;      - WOBody
 * </pre>
 * 
 * <h4>Namespace-global elements</h4>
 * <pre>
 *   &lt;entity&gt;
 *   &lt;SKYOBJ&gt;
 * </pre>
 */
@SuppressWarnings("unchecked")
public class OGoPubXHTMLParser extends NSObject implements WOTemplateParser {
  static DocumentBuilderFactory dbf;
  static DocumentBuilderFactory dbfNoNS;
  protected static final Log log = LogFactory.getLog("OGoPubXHTMLParser");
  
  protected WOTemplateParserHandler delegate;
  protected Exception lastException;
  
  /* WOTemplateParser API */

  public void setHandler(final WOTemplateParserHandler _handler) {
    this.delegate = _handler;
  }
  
  public List<WOElement> parseHTMLData(final URL _url) {
    // TODO: would be nice to hack up the content to allow for default
    //       namespaces (eg predefined var:)
    
    final Document doc = this.parseDOM(_url);
    if (doc == null)
      return null;
    
    final WOElement element = this.buildNode(doc);
    if (element == null)
      return null;
    
    final List<WOElement> elements = new ArrayList<WOElement>(1);
    elements.add(element);
    
    return elements;
  }
  
  public Exception lastException() {
    return this.lastException;
  }
  
  /* processing the DOM recursively */
  
  protected static Class[] ctorSignature = {
    String.class, Map.class, WOElement.class
  };
  
  public WOElement buildVarElement
    (Element _node, Map<String, WOAssociation> _assocs, WOElement _ctn)
  {
    log.info("building var: element " + _node + " => " + _assocs.keySet());
    
    if (_node == null)
      return null;
    
    String tag = _node.getLocalName();
    if (tag == null) {
      tag = _node.getTagName();
      int idx = tag.indexOf(':');
      if (idx != -1) tag = tag.substring(idx + 1);
    }
    
    if ("if".equals(tag))
      return new WOConditional("if" /* name */, _assocs, _ctn);
    
    if ("ifnot".equals(tag)) {
      if (_assocs.containsKey("negate"))
        log.error("cannot use 'negate' binding with ifnot: " + _node);
      _assocs.put("negate", WOAssociation.associationWithValue(Boolean.TRUE));
      return new WOConditional("if" /* name */, _assocs, _ctn);
    }
    
    if ("string".equals(tag))
      return new WOString("string", _assocs, _ctn);
    
    final Class cls = varTagToElementClass.get(tag);
    if (cls != null) {
      return (WOElement)NSJavaRuntime.NSAllocateObject
        (cls, ctorSignature, new Object[] { tag, _assocs, _ctn });
    }
    
    log.error("unknown var element: '" + tag + "': " + _node);
    return null;
  }
  
  protected static final WOElement ampElem   = new WOStaticHTMLElement("&");
  protected static final WOElement colonElem = new WOStaticHTMLElement(";");
  
  public WOElement buildElement
    (Element _node, Map<String, WOAssociation> _assocs, WOElement _child)
  {
    /* build tag */
    
    String tag = _node.getLocalName();
    String ns  = _node.getNamespaceURI();
    
    if (tag == null) {
      if (log.isInfoEnabled()) /* parsed w/o namespace support */
        log.info("node has no local name: " + _node);
      tag = _node.getTagName();
      
      if (ns == null) {
        ns = namespaceForColonName(tag);
        
        int idx = tag.indexOf(':');
        if (idx != -1)
          tag = tag.substring(idx + 1);
      }
    }
    
    /* build SKYOBJ (same for all namespaces) */
    
    if ("SKYOBJ".equals(tag))
      return SKYOBJ.build(_assocs, _child);

    if ("entity".equals(tag)) {
      WOAssociation a = _assocs.get("name");
      if (a == null || !a.isValueConstant())
        return new WOStaticHTMLElement("[incorrect entity tag]");

      WOElement e = new WOStaticHTMLElement(a.stringValueInComponent(null));
      return new WOCompoundElement(ampElem, e, colonElem);
    }
    
    /* build var: tags */
    
    if (XMLNS.OD_BIND.equals(ns))
      return this.buildVarElement(_node, _assocs, _child);
    
    // TODO: support hash-tags? (eg <#WOString var:value=...")
    // => Nope, this is invalid XML
    if (tag.startsWith("#")) {
      log.warn("found hash tag (not being processed): " + _node);
    }
    
    /* patch known link URLs */
    
    OGoPubHTMLElement.patchLinkAssociations(tag, _assocs);
    
    /* special support for scripts, which always need a close tag */
    
    if ("script".equals(tag))
      return new WOJavaScript("script", _assocs, _child);
    
    /* build as generic element/container */
    
    WOElement element;
    
    if (_assocs == null)
      _assocs = new HashMap<String, WOAssociation>(1);
    
    _assocs.put("elementName", WOAssociation.associationWithValue(tag));
    log.debug("associations for new element: " + tag + ": " + _assocs);

    boolean doContainer = _child != null;
    if (!doContainer) {
      for (int i = 0; i < containerElements.length; i += 2) {
        if (ns != null && !ns.equals(containerElements[i]))
          continue;

        // TBD: not sure, can 'a' be a plain element? (<a name="abc"/>)
        if (tag != null && tag.equals(containerElements[i + 1])) {
          doContainer = true;
          break;
        }
      }
    }
    
    element = (doContainer)
      ? new WOGenericContainer(tag /* name */, _assocs, _child)
      : new WOGenericElement(tag /* name */, _assocs, null);
    
    if (_assocs.size() > 0)
      ((WODynamicElement)element).setExtraAttributes(_assocs);
    
    log.debug("element: " + element);
    return element;
  }
  
  /**
   * This method builds the children and associations and then calls the other
   * buildElement method to construct the actual dynamic element.
   * 
   * @param _node
   * @return
   */
  public WOElement buildElement(final Element _node) {
    if (_node == null)
      return null;
    
    /* build associations */
    
    String ns = _node.getNamespaceURI();
    if (ns == null) ns = namespaceForColonName(_node.getTagName());
    
    final Map associations =
      this.associationsForAttributes(_node.getAttributes(), ns);
    
    /* build children */
    
    final List<WOElement> children = 
      _node.hasChildNodes() ? this.buildNodes(_node.getChildNodes()) : null;
    
    final WOElement childElement;
    if (children == null || children.size() == 0)
      childElement = null;
    else if (children.size() == 1)
      childElement = children.get(0);
    else
      childElement = new WOCompoundElement(children);

    return this.buildElement(_node, associations, childElement);
  }
  
  public WOElement buildNode(final Node _node) {
    if (_node == null)
      return null;
    
    switch (_node.getNodeType()) {
      case Node.ELEMENT_NODE:
        return this.buildElement((Element)_node);
      case Node.TEXT_NODE:
        return this.buildText((Text)_node);
      case Node.CDATA_SECTION_NODE:
        return this.buildCDATASection((CDATASection)_node);
      case Node.COMMENT_NODE:
        return this.buildComment((Comment)_node);
      case Node.DOCUMENT_NODE:
        return this.buildDocument((Document)_node);
    }
    log.error("unsupported XHTML node type: " + _node);
    return null;
  }
  
  public List<WOElement> buildNodes(final NodeList _nodes) {
    if (_nodes == null)
      return null;
    
    final int             len   = _nodes.getLength();
    final List<WOElement> elems = new ArrayList<WOElement>(len);
    
    for (int i = 0; i < len; i++) {
      final Node      node = _nodes.item(i);
      final WOElement elem = this.buildNode(node);
      
      if (elem != null)
        elems.add(elem);
    }
    
    return elems;
  }
  
  public WOElement buildDocument(final Document _doc) {
    if (_doc == null)
      return null;
    
    return this.buildElement(_doc.getDocumentElement());
  }
  
  public WOElement buildCharacterData(final CharacterData _node) {
    if (_node == null)
      return null;
    
    final String s = _node.getData();
    if (s       == null) return null;
    if (s.length() == 0) return null;
    
    return new WOString(WOAssociation.associationWithValue(s),
                        true /* escapeHTML */);
  }

  public WOElement buildText(final Text _node) {
    return this.buildCharacterData(_node);   
  }
  public WOElement buildCDATASection(final CDATASection _node) {
    return this.buildCharacterData(_node);   
  }

  public WOElement buildComment(final Comment _node) {
    return null; /* we do not deliver comments */   
  }
  
  /* associations (copied from WOxElemBuilder ..) */
  
  protected static String hackupDateFormat(String value) {
    if (value.indexOf("%") == -1)
      return value;

    /* eg: %Y-%m-%d %H:%M %Z */
    // TODO: move to jope.foundation, make more efficient
    value = value.replace("%Y", "yyyy");
    value = value.replace("%m", "mm");
    value = value.replace("%d", "dd");
    value = value.replace("%H", "HH");
    value = value.replace("%M", "mm");
    value = value.replace("%Z", "z");
    return value;
  }
  
  protected String namespaceForColonName(final String _name) {
    if (_name == null)
      return null;
    
    int idx = _name.indexOf(':');
    if (idx == -1)
      return null;

    String nsprefix = _name.substring(0, idx);
    for (int i = 1; i < prefixToNS.length; i++) {
      if (nsprefix.equals(prefixToNS[i - 1]))
        return prefixToNS[i];
    }
    
    log.warn("could not map namespace prefix: " + nsprefix + " (" + _name +")");
    return null;
  }
  
  public WOAssociation associationForAttribute(Attr _attr, String _elementNS) {
    if (_attr == null)
      return null;
    
    if (false) {
      log.debug("ASSOC FOR ATTR {" +
                _attr.getNamespaceURI() + "}" + _attr.getLocalName() +
                " - " + _attr.getName() + ": " + _attr);
    }
    
    /* get namespace */
    
    String ns = _attr.getNamespaceURI();
    if (ns == null) {
      String n = _attr.getLocalName();
      if (n == null) {
        /* namespace parsing is off */
        ns = namespaceForColonName(_attr.getName());
      }
      
    }
    if (ns == null)
      ns = (_elementNS != null ? _elementNS : "");
    
    if (XMLNS.XMLNS.equals(ns)) {
      /* do not create associations for namespace declarations */
      return null;
    }
    
    /* map namespace to Class */
    
    Class assocClass = nsToAssocClass.get(ns);
    if (assocClass == null)
      assocClass = nsToAssocClass.get("");
    if (assocClass == null) {
      log.error("could not find association class: " + _attr);
      return null;
    }
    
    String value = _attr.getValue();
    
    /* patch some special cases for compatibility */
    
    if ("dateformat".equals(_attr.getLocalName())) {
      /* translate Foundation date pattern to Java date pattern */
      value = hackupDateFormat(value);
    }
    
    /* construct */
    
    final WOAssociation assoc = (WOAssociation)
      NSJavaRuntime.NSAllocateObject(assocClass, String.class, value);
    
    return assoc;
  }
  
  public Map<String,WOAssociation> associationsForAttributes
    (final NamedNodeMap _m, final String _elementNS)
  {    
    if (_m == null)
      return null;
    
    int len = _m.getLength();
    Map<String,WOAssociation> assocs = new HashMap<String, WOAssociation>(len);
    
    for (int i = 0; i < len; i++) {
      WOAssociation assoc;
      Attr attr;
      
      attr = (Attr)(_m.item(i));
      if ((assoc = this.associationForAttribute(attr, _elementNS)) == null)
        continue;
      
      String n = attr.getLocalName();
      if (n == null) {
        n = attr.getName();
        int idx = n.indexOf(':');
        if (idx != -1) n = n.substring(idx + 1); /* cut off namespace prefix */
      }
      if (n.equals("escapehtml")) n = "escapeHTML";
      assocs.put(n, assoc);
    }
    
    return assocs;
  }

  /* XML processing */
  
  protected DocumentBuilder createDocumentBuilder() {
    try {
      // TODO: is the DocumentBuilderFactory thread safe?
      DocumentBuilder builder = dbf.newDocumentBuilder();
      if (!builder.isNamespaceAware())
        log.warn("DOM builder isn't namespace aware: " + builder);
      return builder;
    }
    catch (ParserConfigurationException e) {
      this.lastException = e;
      log.error("failed to create document builder", e);
      return null;
    }
  }
  
  protected DocumentBuilder createDocumentBuilderNoNS() {
    try {
      // TODO: is the DocumentBuilderFactory thread safe?
      DocumentBuilder builder = dbfNoNS.newDocumentBuilder();
      if (builder.isNamespaceAware())
        log.warn("DOM builder is namespace aware: " + builder);
      return builder;
    }
    catch (ParserConfigurationException e) {
      this.lastException = e;
      log.error("failed to create document builder", e);
      return null;
    }
  }
  
  protected Document parseDOM(final DocumentBuilder _db, final URL _url)
    throws SAXException, IOException
  {
    if (_db == null || _url == null)
      return null;

    return _db.parse(_url.toExternalForm());
  }
  
  public Document parseDOM(final URL _url) {
    if (_url == null)
      return null;

    DocumentBuilder db = this.createDocumentBuilder();
    if (db == null)
      return null;
    
    try {
      Document doc = parseDOM(db, _url);
      return doc;
    }
    catch (SAXException e) {
      this.lastException = e;
      
      db = this.createDocumentBuilderNoNS();
      if (db == null)
        return null;
      
      Document doc;
      try {
        doc = parseDOM(db, _url);
        return doc;
      }
      catch (SAXException e1) {
        log.error("again failed to parse XML: " + _url, e);
      }
      catch (IOException e1) {
        log.error("failed to open file: " + _url, e);
      }

      log.error("failed to parse XML: " + _url + ": " + e.getMessage());
    }
    catch (IOException e) {
      this.lastException = e;
      log.error("failed to open file: " + _url, e);
    }

    return null;
  }
  
  /* constants */
  
  protected static final Class[] dynElemCtorSignature = {
    String.class,   /* element name */
    Map.class,      /* associations */
    WOElement.class /* template     */
  };
  
  protected static final String[] prefixToNS = { // TODO: move to JOPE
    "var",   XMLNS.OD_BIND,
    "js",    XMLNS.OD_EVALJS,
    "const", XMLNS.OD_CONST,
    "ognl",  XMLNS.OGo_OGNL
  };
  
  // also used by OGoPubHTMLElement, we should move this to JOPE
  public static final String[] containerElements = {
    "http://www.w3.org/1999/xhtml", "a",
    "http://www.w3.org/1999/xhtml", "script",
    "http://www.w3.org/1999/xhtml", "textarea",
    "http://www.w3.org/1999/xhtml", "form",
    "http://www.w3.org/1999/xhtml", "p",
    "http://www.w3.org/1999/xhtml", "html",
    "http://www.w3.org/1999/xhtml", "head",
    "http://www.w3.org/1999/xhtml", "body",
    "http://www.w3.org/1999/xhtml", "table",
    "http://www.w3.org/1999/xhtml", "tr",
    "http://www.w3.org/1999/xhtml", "td",
    "http://www.w3.org/1999/xhtml", "th",
    "http://www.w3.org/1999/xhtml", "style",
    "http://www.w3.org/1999/xhtml", "div",
    "http://www.w3.org/1999/xhtml", "span"
  };
  
  protected static Map<String, Class> nsToAssocClass;
  protected static Map<String, Class> varTagToElementClass;
  
  static {
    dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);

    dbfNoNS = DocumentBuilderFactory.newInstance();
    dbfNoNS.setNamespaceAware(false);

    nsToAssocClass = new HashMap<String, Class>(16);
    nsToAssocClass.put("",
      org.getobjects.appserver.associations.WOValueAssociation.class);
    nsToAssocClass.put(XMLNS.OD_BIND,
      org.getobjects.appserver.associations.WOKeyPathAssociation.class);
    nsToAssocClass.put(XMLNS.OD_CONST,
        org.getobjects.appserver.associations.WOValueAssociation.class);
    nsToAssocClass.put(XMLNS.OGo_OGNL,
        org.getobjects.appserver.associations.WOOgnlAssociation.class);
    nsToAssocClass.put("OGo:bind",
      org.getobjects.appserver.associations.WOKeyPathAssociation.class);
    nsToAssocClass.put("OGo:value",
      org.getobjects.appserver.associations.WOValueAssociation.class);

    nsToAssocClass.put(XMLNS.OD_EVALJS, OGoPubJavaScriptAssociation.class);
    
    /* tags */
    
    varTagToElementClass = new HashMap<String, Class>(16);
    varTagToElementClass.put("if",        WOConditional.class);
    varTagToElementClass.put("string",    WOString.class);
    varTagToElementClass.put("input",     WOInput.class);
    varTagToElementClass.put("a",         WOHyperlink.class);
    varTagToElementClass.put("popup",     WOPopUpButton.class);
    varTagToElementClass.put("checkbox",  WOCheckBox.class);
    varTagToElementClass.put("copyvalue", WOCopyValue.class);
    varTagToElementClass.put("img",       WOImage.class);
    varTagToElementClass.put("foreach",   WORepetition.class);
    varTagToElementClass.put("textarea",  WOText.class);
    varTagToElementClass.put("ieif",      WOConditionalComment.class);
    varTagToElementClass.put("entity",    WOEntity.class);
    varTagToElementClass.put("html",      WOHtml.class);
    varTagToElementClass.put("body",      WOBody.class);
  }  
}
