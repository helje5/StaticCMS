package org.opengroupware.pubexport.documents;

import org.opengroupware.pubexport.parsers.OGoPubXHTMLParser;

/**
 * OGoPubXHTMLDocument
 */
@SuppressWarnings("unchecked")
public class OGoPubXHTMLDocument extends OGoPubHTMLDocument {

  @Override
  public Class parserClass() {
    return OGoPubXHTMLParser.class;
  }

}
