# fedwiki-java

Read federated wiki pages from the internet with Java. Uses the Jackson library
for JSON (locally downloaded jars) and the built-in HttpClient from JDK 11+.

To run locally:

```
sh run.sh
```

To run a test script locally:

```
sh test.sh
```

# commands

The provided main program reads and executes commands from standard input.
Commands are unique in their first letter so that is all that is required.

- __case *words*__ motivating *words* for subsequent commands.
- __next__ sequence through the items of the current page.
- __test *word*__ confirm that *word* is present in current item.
- __find *word*__ advance through items until *word* is found
- __link__ resume following the first link in the current item.
- __back *word*__ backup lineup to page with *word* in title.
- __exit__ stop command execution and exit main.

# organization

The source code is contained in the single file *main.java* divided into sections by bold comments as follows.

- INTERPRETER - The Java main program that dispatches each line based on their unique first characters to helper functions that stand in for the web interface that is absent in this implementation.
- HELPERS - Functions typically called by the interpreter to perform imagined user interactions and then report the most essential elements of a response a user would expect.
- RUNTIME - Storage and access to the persistent client-side state that makes up the lineup and neighborhood.
- FEDERATION - Static class definitions for JSON data structures read from the server-sides of sites in the federation. This include a page's items and actions and a sitemap's info for each page.

# roadmap

Additional capabilites will be added as needs or curiosity motivates.

- Resolve links based on provided context by the "collaborative linking" semantic. ✔︎
- Retain recently viewed pages in a managed lineup. ✔︎
- Accumulate sites as they are encountered to form a neighborhood.
- Search for pages based on neighborhood sitemaps.
- Provide implementations for selected plugins.
- Read about pages for available plugins from the origin site.
