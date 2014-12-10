package org.opengroupware.pubexport.elements;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.ofs.OFSBaseObject;
import org.opengroupware.pubexport.OGoPubComponent;
import org.opengroupware.pubexport.OGoPubPageComponent;
import org.opengroupware.pubexport.documents.OGoPubComponentDocument;
import org.opengroupware.pubexport.documents.OGoPubDirectory;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

/**
 * OGoPubListRepetition
 * <p>
 * eg:<pre>
 * &lt;SKYOBJ sortedby="zip" list="/en/users/support/partners"
 *         query="country='Switzerland'"&gt;
 * &lt;SKYOBJ list="toclist"
 *         query="NSFileName hasSuffix: '.h' OR NSFileName hasSuffix: '.m'"
 *         sortedby="name"&gt;</pre>
 * <p>
 * Lists:<pre>
 *   all           - all documents in the publication (objType = 'document')
 *   toclist       - all publications, documents & generic documents
 *   children      - all subobjects (diff to 'toclist'?)
 *   relatedLinks  - related links ?
 *   objectsToRoot - all objects till root
 *   $path         - all subobjects of 'path' (eg /en/news/)</pre>
 */
public class OGoPubListRepetition extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("OGoPubList");
  
  protected static final String[] listTypes = {
    "all", "toclist", "children", "relatedLinks", "objectsToRoot"
  };
  
  protected WOAssociation list;
  protected WOAssociation query;
  protected WOAssociation sortedby;
  protected WOAssociation reverse;
  protected WOElement     template;

  public OGoPubListRepetition
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.list     = grabAssociation(_assocs, "list");
    this.query    = grabAssociation(_assocs, "query");
    this.sortedby = grabAssociation(_assocs, "sortedby");
    this.reverse  = grabAssociation(_assocs, "reverse");
    
    if ((this.template = _template) == null) {
      log.warn("list has no content: " + this);
      return;
    }
  }
  
  /* generate response */
  
  protected EOQualifier qualifierInContext(WOContext _ctx) {
    if (this.query == null)
      return null;
    
    Object o = this.query.valueInComponent(_ctx.cursor());
    if (o == null)
      return null;
    
    if (o instanceof EOQualifier)
      return (EOQualifier)o;
    
    if (o instanceof String) {
      if (log.isInfoEnabled()) log.info("qualifier: '" + o + "'");
      
      String      s = (String)o;
      EOQualifier q = EOQualifier.qualifierWithQualifierFormat(s);
      if (q == null) log.error("could not parse qualifier: '" + s + "'");
      return q;
    }
    
    log.error("unexpected 'query' binding value: " + o);
    return null;
  }
  
  protected EOSortOrdering[] sortOrderingsInContext(WOContext _ctx) {
    if (this.sortedby == null)
      return null;
    
    Object o = this.sortedby.valueInComponent(_ctx.cursor());
    if (o == null)
      return null;
    
    if (o instanceof EOSortOrdering)
      return new EOSortOrdering[] { (EOSortOrdering)o };
    
    if (o instanceof String) {
      String  s       = (String)o;
      boolean reverse = false;
      
      if (s.endsWith(".reverse")) {
        reverse = true;
        s = s.substring(0, s.length() - 8);
      }
      
      if (this.reverse != null) {
        if (this.reverse.booleanValueInComponent(_ctx.cursor()))
          reverse = !reverse;
      }
      
      return new EOSortOrdering[] {
          new EOSortOrdering(s, (reverse 
              ? EOSortOrdering.EOCompareDescending
              : EOSortOrdering.EOCompareAscending))
      };
    }
    
    log.error("unexpected 'sortedby' binding value: " + o);
    return null;
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    boolean isDebugOn = log.isDebugEnabled();
    
    if (isDebugOn) log.debug("generate list: " + this.list);
    
    OGoPubComponent page = (OGoPubComponent)_ctx.cursor();
    Object          doc  = page.document();
    if (doc == null) {
      log.error("component has no document: " + page);
      return;
    }
    
    /* determine list type */
    
    String listType = this.list.stringValueInComponent(_ctx.cursor());
    boolean isPath = true;
    if (listType == null || listType.length() == 0) {
      listType = "toclist";
      isPath = false;
    }
    else {
      for (String lt: listTypes) {
        if (lt.equals(listType)) {
          isPath = false;
          break;
        }
      }
    }
    
    if (isDebugOn)
      log.debug("  listtype: " + listType + " (" +(isPath?"path":"list") + ")");
    
    /* determine collection to query */
    
    OGoPubDirectory collection;
    if (isPath) {
      /* eg /en/news */
      collection = (OGoPubDirectory)
        OGoPubWebSite.lookupPath(doc, listType, _ctx, false /* aq */);
      listType = "toclist";
    }
    else if (doc instanceof OGoPubDirectory)
      collection = (OGoPubDirectory)doc;
    else
      collection = (OGoPubDirectory)IGoLocation.Utility.containerForObject(doc);
    
    if (collection == null) {
      log.warn("did not find collection for list query");
      return;
    }
    
    if (isDebugOn) {
      log.debug("  base: " + (isPath? "(type=" + listType + "):" :"") + 
                collection);
    }
    
    /* query */
    
    EOQualifier      q   = this.qualifierInContext(_ctx);
    EOSortOrdering[] sos = this.sortOrderingsInContext(_ctx);
    
    EOFetchSpecification fs =
      new EOFetchSpecification(listType /* entity */, q, sos);
    
    OFSBaseObject[] docs = collection.fetchObjects(fs, _ctx);
    
    if (docs == null || docs.length == 0) {
      log.info("query returned no results: " + fs + " on " + collection);
      return;
    }
    
    /* render results */
    
    if (log.isInfoEnabled()) {
      log.info("query returned " + docs.length + " results: " + 
               Arrays.asList(docs));
    }
    
    //_r.appendContentHTMLString(Arrays.asList(docs).toString());
    
    if (isDebugOn)
      log.debug("  start rendering of list with itemcount: " + docs.length);
    
    WOComponent parent = _ctx.component();
    if (parent == null) {
      log.error("did not find parent of child component");
      return;
    }
    
    for (OFSBaseObject subdoc: docs) {
      // TODO: I don't get this one. Isn't tdoc the same like subdoc?
      String abspath = OGoPubWebSite.absolutePathInTargetHierarchy(subdoc);
      OFSBaseObject tdoc = (OFSBaseObject)
        OGoPubWebSite.lookupPath(doc, abspath, _ctx, false);
      
      if (tdoc == null) {
        log.error("did not find list item: " + doc);
        _r.appendContentHTMLString("[did not find list item: "+ subdoc +"]");
        continue;
      }
      
      if (isDebugOn) log.debug("    render: " + subdoc);
      
      /*
       * Note: you can also use lists to iterate over images or so, its not
       *       restricted to components ...
       * TODO: We use WOComponents to represent the (evaluation) context,
       *       possibly the cursor would be the better option.
       */
      
      OGoPubComponent child;
      if (subdoc instanceof OGoPubComponentDocument) {
        String docPath = OGoPubWebSite.absolutePathInTargetHierarchy(subdoc);
        child = (OGoPubComponent)page.pageWithName(docPath);
      }
      else {
        child = new OGoPubPageComponent(); // TODO: FIXME
        child.document = subdoc;
      }

      if (isDebugOn) log.debug("    child: " + child);
      
      if (child == null) {
        log.error("could not instantiate child component: " + subdoc);
        continue;
      }
      
      /* fixup */
      
      if (child.document != child.templateDocument &&
          child.templateDocument != null)
        log.warn("child doc != child template!");
      
      /* render subcomponent */
      
      if (isDebugOn) log.debug("    render child: " + child);
      _ctx.enterComponent(child, null /* template */);
      
      /* Note: we do _NOT_ call the childs appendToResponse! We only switch the
       *       evaluation context to the new component. The children of the
       *       repetition is still the subtemplate!
       */
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      else
        child.appendToResponse(_r, _ctx);
      _ctx.leaveComponent(child);
      
      if (isDebugOn) log.debug("    did render child: " + child);
    }
    
    if (isDebugOn) log.debug("finished list generation.");
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "list",     this.list);
    this.appendAssocToDescription(_d, "query",    this.query);
    this.appendAssocToDescription(_d, "sortedby", this.sortedby);
    this.appendAssocToDescription(_d, "reverse",  this.reverse);
  }

}
