StaticCMS
=========

GETobjects based static website generator, probably not very useful for anyone but me ;-) More of a hack and has quite a few bugs / rough edges. Stay away from it.

### Samples

There is a tiny sample site in org/opengroupware/pubexport/tests/hellosite.

Well, and on the OGo server. E.g. the SOPE website is generated by StaticCMS (http://sope.opengroupware.org),
sources are over here: http://svn.opengroupware.org/cgi-bin/viewvc.cgi/www/sope/trunk/

### How it works

This is all based on the [Go](http://getobjects.googlecode.com) OFS (object file system) with a few extra features for CMS operation.
An OFS is just a directory containing files. Each of those files is turned into an OFS object at runtime (using a factory class, usually based on the extension).
In StaticCMS those are objects like OGoPubHTMLDocument or OGoPubTemplate.

The StaticCMS exporter walks this OFS hierarchy and generates an output document for each document in the tree - excluding templates (.xtmpl files). All HTML files are rendered using those templates, starting at the Main.xtmpl. The Main.xtmpl can then include other templates, and eventually the body of the HTML file itself.

Now the fun part is that you can override templates in subdirectories. Lets say the Main.xtmpl embeds the 'sidebar' template. Which sidebar template is picked depends on the location in the OFS hierarchy. E.g. a /news/sidebar.xtmpl could show different content when you are in the news section of the site.

Another core idea - which isn't actually implemented :-) - is that the templates could export arbitrary OFS objects. That is, instead of the content document being an HTML file, it could also be just a property list or an XML file with the raw data.

P.S.: There is also an Objective-C version of this embedded in OpenGroupware.org (called SkyPublisher). This one is a modern Go based Java rewrite.

### Demo

Sample Main.xtmpl - renders just the root HTML tag and then triggers the head and body templates:
```
<html xmlns="http://www.w3.org/1999/xhtml">
  <SKYOBJ insertvalue="template" name="head" />
  <SKYOBJ insertvalue="template" name="body" />
</html>
```

Sample body.xtmpl - this adds the body tag, triggers a header and footer template and in between inserts the content template:
```
<body xmlns="http://www.w3.org/1999/xhtml"
      onload="setFocus()"
>
  <nodig><SKYOBJ insertvalue="template" name="header" /></nodig>
  <SKYOBJ insertvalue="template" name="content" />
  <nodig><SKYOBJ insertvalue="template" name="footer" /></nodig>
</body>
```

Sample body.xtmpl - this now embeds the actual HTML fragment:
```
<span xmlns="http://www.w3.org/1999/xhtml">
  <SKYOBJ insertvalue="var" name="body" />
</span>
```


#### Notes

- the clientObject of the context is the page being exported in the context
  - the path of that page is what URLs must be rewritten for, NOT the
    document of the OGoPubComponent

#### Starting the exporter or preview daemon

```
java -cp ~/dev/eclipse/JavaPlayground/ant-obj/lib/pubexport-0.9.4.jar \
  org.opengroupware.pubexport.pubd \
  -Droot=/Users/helge/dev/www/hh-dump-20050406
```
```
java -cp ~/dev/eclipse/JavaPlayground/ant-obj/lib/pubexport-0.9.4.jar \
  org.opengroupware.pubexport.pubexport \
  /Users/helge/dev/www/home-trunk \
  /tmp/ogopub
```
```
java -cp pubexport-0.9.4.jar \
  org.opengroupware.pubexport.pubd \
  -Droot=/Users/helge/dev/www/home-trunk/ \
  -DWOPort=8988
```
