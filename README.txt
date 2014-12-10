A hackish implementation of the Publisher export in Java.

Notes
=====
- the clientObject of the context is the page being exported in the context
  - the path of that page is what URLs must be rewritten for, NOT the
    document of the OGoPubComponent

Starting
========

java -cp ~/dev/eclipse/JavaPlayground/ant-obj/lib/pubexport-0.9.4.jar \
  org.opengroupware.pubexport.pubd \
  -Droot=/Users/helge/dev/www/hh-dump-20050406

java -cp ~/dev/eclipse/JavaPlayground/ant-obj/lib/pubexport-0.9.4.jar \
  org.opengroupware.pubexport.pubexport \
  /Users/helge/dev/www/home-trunk \
  /tmp/ogopub

java -cp pubexport-0.9.4.jar \
  org.opengroupware.pubexport.pubd \
  -Droot=/Users/helge/dev/www/home-trunk/ \
  -DWOPort=8988
