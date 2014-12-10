package org.opengroupware.pubexport.associations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;

public class OGoPubConditionAssociation extends WOAssociation {
  protected static final Log log = LogFactory.getLog("OGoPubExport");
    
  protected WOAssociation op;
  protected WOAssociation lhs;
  protected WOAssociation rhs;

  public OGoPubConditionAssociation
    (WOAssociation _op, WOAssociation _lhs, WOAssociation _rhs)
  {
    super();
    this.op  = _op;
    this.lhs = _lhs;
    this.rhs = _rhs;
  }

  @Override
  public String keyPath() {
    return null;
  }

  /* value typing */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }

  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  /* values */
  
  @Override
  public boolean booleanValueInComponent(Object _cursor) {
    String sop = null;
    if (this.op != null) sop = this.op.stringValueInComponent(_cursor);
    
    Object lv = (this.lhs != null) ? this.lhs.valueInComponent(_cursor) : null;
    Object rv = (this.rhs != null) ? this.rhs.valueInComponent(_cursor) : null;
    
    if (sop == null || "isEqual".equals(sop)) {
      if (lv == rv)
        return true;
      if (lv == null || rv == null) /* one of them is null */
        return false;
      
      return lv.equals(rv);
    }
    
    if ("isNotEqual".equals(sop)) {
      if (lv == rv)
        return false;
      if (lv == null || rv == null) /* one of them is null */
        return true;

      return !lv.equals(rv);
    }
    
    if ("isCaseEqual".equals(sop)) {
      if (lv == rv)
        return true;
      if (lv == null || rv == null) /* one of them is null */
        return false;
      
      if (lv.equals(rv))
        return true;
      
      return lv.toString().equalsIgnoreCase(rv.toString());
    }

    if ("matches".equals(sop)) {
      if (lv == null && rv == null)
        return true;
      if (lv == null || rv == null) { /* one of them is null */
        log.warn("matches key or pattern is null ...");
        return false;
      }
      
      return lv.toString().matches(rv.toString());
    }
    
    if ("hasPrefix".equals(sop)) {
      if (rv == null)
        return true;
      if (lv == null)
        return false;
      
      return lv.toString().startsWith(rv.toString());
    }
    
    if ("isNotNil".equals(sop) || "isNotEmpty".equals(sop)) {
      if (lv == null)
        return false;
      if (lv instanceof String && ((String)lv).trim().length() == 0)
        return false;
      
      return true;
    }
    if ("isNil".equals(sop) || "isEmpty".equals(sop)) {
      if (lv == null)
        return true;
      if (lv instanceof String && ((String)lv).trim().length() == 0)
        return true;
      
      return false;
    }
    
    log.error("unknown operator: " + sop);
    return false;
  }

  @Override
  public Object valueInComponent(Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? Boolean.TRUE : Boolean.FALSE;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.op  != null) _d.append(" op="  + this.op);
    if (this.lhs != null) _d.append(" lhs=" + this.lhs);
    if (this.rhs != null) _d.append(" rhs=" + this.rhs);
  }
}
