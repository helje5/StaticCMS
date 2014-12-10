package org.opengroupware.pubexport.parsers.tests;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.List;

import org.getobjects.appserver.core.WOElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengroupware.pubexport.parsers.OGoPubHTMLParser;

public class TOGoPubHTMLParser {

  protected OGoPubHTMLParser parser = null;

  @Before
  public void setUp() {
    this.parser = new OGoPubHTMLParser();
  }

  @After
  public void tearDown() {
    this.parser = null;
  }
  
  /* tests */
  
  @Test public void testParsePage1() {
    List<WOElement> elements = this.parse("page1.html");
    assertNotNull("got no elements", elements);
  }
  
  /* helpers */
  
  public List<WOElement> parse(String _rsrcName) {
    URL url = this.getClass().getResource(_rsrcName);
    return this.parser.parseHTMLData(url);
  }
}
