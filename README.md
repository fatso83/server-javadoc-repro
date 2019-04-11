# `ClassNotFoundException` with custom xml serializer
> This repo is to reproduce easily an issue with overriding the xml serializer for a doclet

## How to reproduce
```sh
git clone https://github.com/fatso83/server-javadoc-repro
cd server-javadoc-repro
mvn install org.apache.maven.plugins:maven-javadoc-plugin:3.0.1:test-javadoc
```

## The problematic code

```java
System.setProperty("javax.xml.stream.XMLOutputFactory", WstxOutputFactory.class.getName());

// Prepare for crash with ClassNotFound ...
System.out.println("XMLOutputFactory: " + XMLOutputFactory.newInstance().getClass());
```

## The output

```java
$ mvn org.apache.maven.plugins:maven-javadoc-plugin:3.0.1:test-javadoc

...
[ERROR] Exit code: 4 - javadoc: error - fatal error encountered: javax.xml.stream.FactoryConfigurationError: Provider com.ctc.wstx.stax.WstxOutputFactory not found
[ERROR] javadoc: error - Please file a bug against the javadoc tool via the Java bug reporting page
[ERROR] (http://bugreport.java.com) after checking the Bug Database (http://bugs.java.com)
[ERROR] for duplicates. Include error messages and the following diagnostic in your report. Thank you.
[ERROR] javax.xml.stream.FactoryConfigurationError: Provider com.ctc.wstx.stax.WstxOutputFactory not found
[ERROR] 	at java.xml/javax.xml.stream.FactoryFinder.newInstance(FactoryFinder.java:196)
[ERROR] 	at java.xml/javax.xml.stream.FactoryFinder.newInstance(FactoryFinder.java:148)
[ERROR] 	at java.xml/javax.xml.stream.FactoryFinder.find(FactoryFinder.java:260)
[ERROR] 	at java.xml/javax.xml.stream.FactoryFinder.find(FactoryFinder.java:222)
[ERROR] 	at java.xml/javax.xml.stream.XMLOutputFactory.newInstance(XMLOutputFactory.java:138)
[ERROR] 	at docs.JacksonWriter.write(JacksonWriter.java:154)
[ERROR] 	at docs.TestSheetDoclet.run(TestSheetDoclet.java:98)
[ERROR] 	at jdk.javadoc/jdk.javadoc.internal.tool.Start.parseAndExecute(Start.java:588)
[ERROR] 	at jdk.javadoc/jdk.javadoc.internal.tool.Start.begin(Start.java:432)
[ERROR] 	at jdk.javadoc/jdk.javadoc.internal.tool.Start.begin(Start.java:345)
[ERROR] 	at jdk.javadoc/jdk.javadoc.internal.tool.Main.execute(Main.java:63)
[ERROR] 	at jdk.javadoc/jdk.javadoc.internal.tool.Main.main(Main.java:52)
[ERROR] Caused by: java.lang.ClassNotFoundException: com/ctc/wstx/stax/WstxOutputFactory
[ERROR] 	at java.base/java.lang.Class.forName0(Native Method)
[ERROR] 	at java.base/java.lang.Class.forName(Class.java:415)
[ERROR] 	at java.xml/javax.xml.stream.FactoryFinder.getProviderClass(FactoryFinder.java:121)
[ERROR] 	at java.xml/javax.xml.stream.FactoryFinder.newInstance(FactoryFinder.java:185)
[ERROR] 	... 11 more
```
