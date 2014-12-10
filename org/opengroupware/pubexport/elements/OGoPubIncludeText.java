package org.opengroupware.pubexport.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOElement;

/**
 * Embed the contents of a file.
 * <p>
 * Example:<pre>&lt;SKYOBJ js:includetext="path" /&gt;</pre>
 * 
 * @author helge
 */
// TODO: whats the difference to insertvalue="var" body?
public class OGoPubIncludeText extends OGoPubTemplateReference {
  
  public OGoPubIncludeText
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, hackAssocs(_assocs), _template);
  }
  
  public static Map<String, WOAssociation> hackAssocs
    (Map<String, WOAssociation> _assocs)
  {
    // rename 'includetext' to 'name'
    WOAssociation a = grabAssociation(_assocs, "includetext");
    _assocs.put("name", a);
    return _assocs;
  }
}
