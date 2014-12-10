package org.opengroupware.pubexport.parsers;

/**
 * OGoPubXTemplateParser
 * <p>
 * Parsing templates is different to parsing XHTML pages because we need to
 * replace all tags which contain links to rewrite those to the document
 * location.
 * <p>
 * Remember that links used in a template are relative to the templates storage
 * location but when they get rendered, they must be relative to the document! 
 */
public class OGoPubXTemplateParser extends OGoPubXHTMLParser {

}
