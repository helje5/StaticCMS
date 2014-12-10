package org.opengroupware.pubexport.documents;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.ofs.OFSSelect;

public class OGoPubRSSFeed extends OFSSelect {
  
  public String mimeType() {
    return "text/plain";
  }
  
  public boolean isFolderish() {
    return false;
  }
  
  /* generate response */
  
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    _r.appendContentString("todo: render feed\n");
    _r.appendContentHTMLString(this.fetchObjects(_ctx).toString());
  }
}
