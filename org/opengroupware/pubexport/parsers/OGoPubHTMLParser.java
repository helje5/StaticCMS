package org.opengroupware.pubexport.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.elements.WOCompoundElement;
import org.getobjects.appserver.elements.WOStaticHTMLElement;
import org.getobjects.appserver.templates.WOHTMLParser;
import org.getobjects.appserver.templates.WOWrapperTemplateBuilder;
import org.opengroupware.pubexport.elements.OGoPubHTMLElement;
import org.opengroupware.pubexport.elements.SKYOBJ;

/**
 * OGoPubHTMLParser
 * <p>
 * This class parses an HTML template file into a WOElement hierarchy. The
 * actual element construction is done by the OGoPubSkyObjBuilder.
 */
public class OGoPubHTMLParser extends WOHTMLParser {

  public OGoPubHTMLParser() {
    super();
  }
  
  protected static final String[] tags = {
    "SKYOBJ", "A", "IMG", "LINK", "FORM", "INPUT", "SCRIPT",
    "TABLE", "TR", "TD", "BODY",
    "ENTITY"
  };
  protected static final String[] htmlAutoCloseTags = {
    "IMG", "LINK", "INPUT"
  };
  
  protected boolean isParsedTag(boolean _checkClose) {
    if ((this.idx + 2) >= this.len) /* check whether it is long enough */
      return false;
    
    if (this.buffer[this.idx] != '<') /* check whether it is a tag */
      return false;
    
    if (_checkClose && this.buffer[this.idx + 1] != '/')
      /* check whether it is a close tag */
      return false;
    
    for (String tag: tags) {
      if ((this.idx + tag.length() + (_checkClose? 3 : 2)) >= this.len) {
        /* check whether it is long enough */
        continue;
      }
      //System.err.println("CHECK: " + tag + " at " +
      //    new String(this.buffer, this.idx, tag.length() + 3));
      
      if (_ucIsCaseEqual(this.buffer, this.idx + (_checkClose?2:1), tag))
        return true;
    }
    return false;
  }
  protected boolean isStartTag(String _name) {
    int nameLen = _name.length();
    if ((this.idx + nameLen + 2) >= this.len)
      /* check whether it is long enough */
      return false;
    
    if (this.buffer[this.idx] != '<')
      return false;
    
    if (!_ucIsCaseEqual(this.buffer, this.idx + 1, _name))
      return false;
    
    /* the char following the name must be a space or a > or a / */
    char nextChar = this.buffer[this.idx + 1 + nameLen]; 
    if (nextChar != '>' && nextChar != '/' && !_isHTMLSpace(nextChar))
      return false;
    
    return true;
  }
  protected boolean isCloseTag(String _name) {
    int nameLen = _name.length();
    if ((this.idx + nameLen + 3) >= this.len)
      /* check whether it is long enough */
      return false;
    
    if (this.buffer[this.idx] != '<' || this.buffer[this.idx + 1] != '/')
      return false;
    
    if (!_ucIsCaseEqual(this.buffer, this.idx + 2, _name))
      return false;
    
    /* the char following the name must be a space or a > or a / */
    char nextChar = this.buffer[this.idx + 2 + nameLen];
    if (nextChar != '>' && !_isHTMLSpace(nextChar))
      return false;
    // TODO: we could scan for '>'
    
    return true;
  }
  
  protected boolean isAutoCloseTag(String _name) {
    if (_name == null) {
      log.error("got an invalid name: " + _name);
      return false;
    }
    
    for (String tag: htmlAutoCloseTags) {
      if (tag.equalsIgnoreCase(_name))
        return true;
    }
    return false;
  }
  
  protected boolean _isSKYOBJTag() {
    /* check for "<SKYOBJ .......>" (len 9) (lowercase is allowed) */
    if ((this.idx + 8) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<') /* check whether it is a tag */
      return false;

    return _ucIsCaseEqual(this.buffer, this.idx, "<SKYOBJ");
  }

  protected boolean _isSKYOBJCloseTag() {
    /* check for </SKYOBJ> (len=9) */
    if ((this.idx + 9) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<' && this.buffer[this.idx + 1] != '/')
      return false; /* not a close tag */
    
    return _ucIsCaseEqual(this.buffer, this.idx, "</SKYOBJ>");
  }
  
  protected boolean isEntityTag() {
    /* check for "<entity .......>" (len 9) (lowercase is allowed) */
    if ((this.idx + 8) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<') /* check whether it is a tag */
      return false;

    return _ucIsCaseEqual(this.buffer, this.idx, "<ENTITY");
  }

  @Override
  protected boolean shouldContinueParsingText() {
    if (!super.shouldContinueParsingText())
      return false;
    
    if (this.isParsedTag(false /* open tag */))
      return false;
    if (this.isParsedTag(true /* close tag */))
      return false;
    
    return true;
  }

  protected static final WOElement ampElem   = new WOStaticHTMLElement("&");
  protected static final WOElement colonElem = new WOStaticHTMLElement(";");
  
  protected WOElement parseEntityElement() {
    boolean isDebugOn = log.isDebugEnabled();
    
    if (this.idx >= this.len) return null; /* EOF */
    
    if (!this.isEntityTag())
      return null; /* not an entity tag */
    
    if (isDebugOn) log.debug("parse entity element ...");
    
    this.idx += 7; /* skip '<entity' */
    
    /* parse attributes */
    
    if (isDebugOn) log.debug("  parse entity attributes ...");
    Map<String,String> attrs = this._parseTagAttributes();
    if (this.lastException != null || attrs == null)
      return null; // invalid tag attrs    
    
    if (this.idx >= this.len) {
      this.addException("unexpected EOF: missing '>' in entity tag (EOF).");
      return null; // unexpected EOF
    }
    
    /* parse tag end (/>) */
    if (this.buffer[this.idx] != '/' && this.buffer[this.idx + 1] != '>') {
      this.addException("missing '/>' in entity tag.");
      return null; // unexpected EOF
    }
    this.idx += 2; /* skip /> */
    
    return new WOCompoundElement
      (ampElem, new WOStaticHTMLElement(attrs.get("name")), colonElem);
  }
  
  protected WOElement parseSKYOBJElement() {
    boolean isDebugOn = log.isDebugEnabled();
    
    if (this.idx >= this.len) return null; /* EOF */
    
    if (!this._isSKYOBJTag())
      return null; /* not a SKYOBJ tag */
    
    if (isDebugOn) log.debug("parse SKYOBJ element ...");
    
    this.idx += 7; /* skip '<SKYOBJ' */
    
    /* parse attributes */
    
    if (isDebugOn) log.debug("  parse SKYOBJ attributes ...");
    Map<String,String> attrs = this._parseTagAttributes();
    if (this.lastException != null || attrs == null)
      return null; // invalid tag attrs    
    
    if (this.idx >= this.len) {
      this.addException("unexpected EOF: missing '>' in SKYOBJ tag (EOF).");
      return null; // unexpected EOF
    }
    
    /* parse tag end (> or /) */
    if (this.buffer[this.idx] != '>' && this.buffer[this.idx] != '/') {
      this.addException("missing '>' in SKYOBJ tag.");
      return null; // unexpected EOF
    }
    
    boolean isAutoClose = false;
    boolean foundEndTag = false;
    List<WOElement> children = null;
    
    if (this.buffer[this.idx] == '>') { /* SKYOBJ is closed */
      /* has sub-elements (<SKYOBJ ...>...</SKYOBJ>) */
      this.idx += 1; // skip '>'
      
      if (isDebugOn) log.debug("  parsing SKYOBJ children ...");
    
      while ((this.idx < this.len) && (this.lastException == null)) {
        WOElement subElement = null;
        
        if (this._isSKYOBJCloseTag()) {
          foundEndTag = true;
          break;
        }
  
        subElement = this.parseElement();
      
        if (subElement != null) {
          if (children == null)
            children = new ArrayList<WOElement>(16);
          children.add(subElement);
        }
      }
    }
    else { /* is an empty tag (<SKYOBJ .../>) */
      /* has no sub-elements (<SKYOBJ .../>) */
      if (isDebugOn) log.debug("  is autoclose SKYOBJ-tag ...");
      this.idx += 1; // skip '/'
      isAutoClose = true;
      if (this.buffer[this.idx] != '>') {
        this.addException("missing '>' in SKYOBJ autoclose tag.");
        return null; // unexpected EOF
      }
      this.idx += 1; // skip '>'
    }
    
    /* produce element */
    
    Map<String, WOAssociation> assocs =
      WOWrapperTemplateBuilder.buildAssociationsForTagAttributes(attrs);
    
    WOElement element = SKYOBJ.build(assocs, children);
    if (isDebugOn) log.debug("  SKYOBJ element: " + element);
    
    if (element == null) { // build error
      this.addException("could not build SKYOBJ element !.");
      return null;
    }
    
    if (!foundEndTag && !isAutoClose) {
      this.addException("did not find SKYOBJ end tag (</SKYOBJ>) ..");
      return null;
    }
    else if (!isAutoClose) {
      /* skip close tag ('</SKYOBJ>') */
      if (!this._isSKYOBJCloseTag())
        log.error("invalid parser state ..");
      
      this.idx += 8; // skip '</SKYOBJ'
      while ((this.idx < this.len) && (this.buffer[this.idx] != '>'))
        this.idx += 1;
      this.idx += 1; // skip '>'
    }
    return element;
  }
  
  /* parsing regular HTML */
  
  protected WOElement parseKnownElement() {
    boolean isDebugOn = log.isDebugEnabled();
    
    if (this.idx + 2 >= this.len) return null; /* EOF */
    
    if (isDebugOn) log.debug("parse known element ...");
    
    this.idx += 1; /* skip '<' */
    
    /* parse tag name */
    
    String name = this._parseStringValue();
    if (name == null && this.lastException != null) // if there was an error ..
      return null;
    this._skipSpaces();
    if (isDebugOn)
      log.debug("  parsed known element name: '" + name + "' at " + this.idx);
    
    /* parse attributes */
    
    if (isDebugOn) log.debug("  parse attributes at " + this.idx);
    Map<String,String> attrs = this._parseTagAttributes();
    if (this.lastException != null && attrs == null)
      return null; // invalid tag attrs, Note: it is valid to have no attrs ...
    
    if (this.idx >= this.len) {
      this.addException("unexpected EOF: missing '>' in "+ name +" tag (EOF).");
      return null; // unexpected EOF
    }
    
    /* parse tag end (> or /) */
    if (this.buffer[this.idx] != '>' && this.buffer[this.idx] != '/') {
      this.addException("missing '>' in " + name + " tag end.");
      return null; // unexpected EOF
    }
    
    boolean isAutoClose  = false;
    //boolean closeMissing = false;
    boolean foundEndTag  = false;
    List<WOElement> children = null;
    
    if (this.buffer[this.idx] == '>') { /* tag is closed */
      /* has sub-elements (<a ...>...</a>) */
      this.idx += 1; // skip '>'
      
      if (!this.isAutoCloseTag(name)) {
        if (isDebugOn)
          log.debug("  parsing " + name + " children at " + this.idx);
        
        if (log.isWarnEnabled() && "img".equals(name)) {
          log.warn("parsing children of 'img' tag? line: " + 
                   this.currentLine() + ", context: " + this.stringContext() +
                   "\n  attrs: " + attrs);
        }

        children = new ArrayList<WOElement>(16);
        while ((this.idx < this.len) && (this.lastException == null)) {
          WOElement subElement = null;
          
          if (isDebugOn)
            log.debug("    parse next child of " + name + " at " + this.idx);

          if (this.isCloseTag(name)) {
            if (debugOn) log.debug("  found close tag: '" + name + "'");
            foundEndTag = true;
            break;
          }
          
          if (this.isParsedTag(true /* close tag */)) {
            /* eg: <tr><td>...</tr> */
            // closeMissing = true; // TODO: never read
            break;
          }
          
          if ("a".equals(name)) { /* detect nested 'a' tags */
            if (this.isStartTag("a")) {
              this.addException("nested 'a' tag");
              log.error("detected nested 'a' tag at " + this.idx + " / line " + 
                        this.currentLine() + " ctx: |" +
                        this.stringContext() + "|");
              isAutoClose = true;
              break;
            }
          }

          subElement = this.parseElement();

          if (subElement != null)
            children.add(subElement);
        }
      }
      else {
        isAutoClose = true;
        if (isDebugOn)
          log.debug("  is autoclose tag, do not continue at " + this.idx);
      }
    }
    else { /* is an empty tag (<img .../>) */
      /* has no sub-elements (<img .../>) */
      if (isDebugOn) log.debug("  is autoclose tag ...");
      this.idx += 1; // skip '/'
      isAutoClose = true;
      if (this.buffer[this.idx] != '>') {
        this.addException("missing '>' in " + name + " autoclose tag.");
        return null; // unexpected EOF
      }
      this.idx += 1; // skip '>'
    }
    
    /* produce element */
    
    Map<String, WOAssociation> assocs =
      WOWrapperTemplateBuilder.buildAssociationsForTagAttributes(attrs);
    
    WOElement element =
      OGoPubHTMLElement.build(name.toLowerCase(), assocs, children);
    if (isDebugOn) log.debug("  " + name + " element: " + element);
    
    if (element == null) { // build error
      this.addException("could not build element for tag: " + name);
      return null;
    }
    
    /* apply extra attrs */
    if (element instanceof WODynamicElement && assocs.size() > 0)
      ((WODynamicElement)element).setExtraAttributes(assocs);
    
    if (!foundEndTag && !isAutoClose) {
      // if we add this, parsing stops.
      // TODO: we need an add-warning
      //this.addException("did not find '" + name + "' end tag");
    }
    else if (!isAutoClose) {
      /* skip close tag ('</a>') */
      if (!this.isCloseTag(name))
        log.error("invalid parser state ..");
      
      this.idx += 2; // skip '</'
      this.idx += name.length();
      while ((this.idx < this.len) && (this.buffer[this.idx] != '>'))
        this.idx += 1;
      this.idx += 1; // skip '>'
    }
    return element;
  }
  
  protected String skipAnyCloseTag() {
    this.idx += 2; // skip '</'
    int s = this.idx;
    while ((this.idx < this.len) && (this.buffer[this.idx] != '>'))
      this.idx += 1;
    String ct = ((this.idx - s) > 0)
      ? new String(this.buffer, s, this.idx - s) : null;
    this.idx += 1; // skip '>'
    return ct;
  }
  
  @Override
  protected WOElement parseElement() {
    if (this.idx >= this.len) /* EOF */
      return null;
    
    //System.err.println("PARSE ELEMENT AT: " +
    //    new String(this.buffer, this.idx,(this.idx + 5 < this.len ? 5 : 1)));
    
    if (this.buffer[this.idx] == '<') {
      if (this._isSKYOBJTag()) {
        /* start parsing of dynamic content */
        if (log.isDebugEnabled()) log.debug("detected <SKYOBJ> at " + this.idx);
        return this.parseSKYOBJElement();
      }
      
      if (this.isEntityTag()) {
        if (log.isDebugEnabled())
          log.debug("detected entity tag at " + this.idx);
        return this.parseEntityElement();
      }
      
      if (this.isParsedTag(false /* open tag */)) {
        if (log.isDebugEnabled())
          log.debug("detected parsed tag at " + this.idx);
        return this.parseKnownElement();
      }
      
      if (this._isSKYOBJCloseTag()) {
        //log.warn("unexpected SKYOBJ close tag (</SKYOBJ>)");
        this.skipAnyCloseTag();
        this.addException("unexpected SKYOBJ close tag");
        // TODO: in SOPE we raise an exception
      }
      
      if (this.isParsedTag(true /* close tag */)) {
        String s = this.skipAnyCloseTag();
        this.addException("unexpected close tag: '" + s + "'");
        //log.warn("unexpected close tag (</" + s + ">)");
      }
    }
    
    return super.parseElement();
  }
}
