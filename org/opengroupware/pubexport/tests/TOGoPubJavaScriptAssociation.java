package org.opengroupware.pubexport.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.tests.WOTestWithFullEnvironment;
import org.getobjects.ofs.fs.IOFSFileManager;
import org.getobjects.ofs.fs.OFSHostFileManager;
import org.junit.Test;
import org.opengroupware.pubexport.OGoPubComponent;
import org.opengroupware.pubexport.OGoPubPageComponent;
import org.opengroupware.pubexport.associations.OGoPubJavaScriptAssociation;
import org.opengroupware.pubexport.documents.OGoPubHTMLDocument;
import org.opengroupware.pubexport.documents.OGoPubWebSite;

public class TOGoPubJavaScriptAssociation extends WOTestWithFullEnvironment {
  
  OGoPubComponent    page;
  OGoPubWebSite      root;
  OGoPubHTMLDocument doc;
  
  public void setUp() {
    super.setUp();
    
    File rootdir =
      new File(this.getClass().getResource("hellosite").getPath());
    
    IOFSFileManager fm = new OFSHostFileManager(rootdir);
    
    this.root = new OGoPubWebSite();
    this.root.setStorageLocation(fm, null /* root path */);
    this.root.setLocation(null, null); /* we are root */

    this.root.loadProperties();
    
    this.doc = (OGoPubHTMLDocument)
      this.root.lookupName("index.html", this.context, false);
    
    this.page = new OGoPubPageComponent();
    this.page._setContext(this.context);
    this.page.document = this.doc;
    
    this.context.setPage(this.page);
    this.context.enterComponent(this.page, null /* component-content */);
  }
  public void tearDown() {
    this.context.leaveComponent(this.page);
    this.page = null;
    this.doc  = null;
    this.root = null;
    super.tearDown();
  }
  
  /* tests */

  @Test public void testSimpleConstNumberEval() {
    WOAssociation a = new OGoPubJavaScriptAssociation("13 + 29");
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is NaN", result instanceof Number); 
    assertEquals("JS calculation gone wrong", 42, ((Number)result));
  }

  @Test public void testSimpleRegEx() {
    WOAssociation a = new OGoPubJavaScriptAssociation
      ("'/de/index.html'.replace(/\\/de\\//,'/en/')");
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is not a string", result instanceof String); 
    assertEquals("JS calculation gone wrong", "/en/index.html", result);
  }

  @Test public void testDocThis() {
    WOAssociation a = new OGoPubJavaScriptAssociation("this");
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is not an htmldoc",result instanceof OGoPubHTMLDocument); 
  }

  @Test public void testDocPath() {
    WOAssociation a = new OGoPubJavaScriptAssociation("path");
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is not a string", result instanceof String);
    assertEquals("unexpected path", "/index.html", result);
  }

  @Test public void testDocPathRegEx() {
    String js = "path.replace(/\\/index/,'/replacedindex')";
    WOAssociation a = new OGoPubJavaScriptAssociation(js);
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is not a string", result instanceof String);
    assertEquals("unexpected path", "/replacedindex.html", result);
  }

  @Test public void testDocGetAttribute() {
    String js = "getAttribute('NSFileName')";
    WOAssociation a = new OGoPubJavaScriptAssociation(js);
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is not a string", result instanceof String);
    assertEquals("unexpected filename", "index.html", result);
  }

  @Test public void testDocGetAttributeRegEx() {
    String js = "getAttribute('NSFileName').replace(/index/,'replacedindex')";
    WOAssociation a = new OGoPubJavaScriptAssociation(js);
    
    Object result = a.valueInComponent(this.page);
    assertNotNull("association returned no result", result);
    assertTrue("result is not a string", result instanceof String);
    assertEquals("unexpected filename", "replacedindex.html", result);
  }
}
