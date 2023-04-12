# fedwiki-java

Very basics of reading a FedWiki page from Java. Uses the Jackson library
for JSON (locally downloaded jars) and the built-in HttpClient from JDK 11+.

To run locally:

```
java \
  -cp jackson-core-2.14.2.jar:jackson-annotations-2.14.2.jar:jackson-databind-2.14.2.jar \
  Main.java
```

This will output a cryptic looking object output but it's a start.
