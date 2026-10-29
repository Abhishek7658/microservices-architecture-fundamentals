# Library Management System (Java 17, Maven, Gson)

## Run
```
mvn clean test        # unit tests
mvn exec:java         # start console app (JSON files saved in ./data)
mvn javadoc:javadoc   # generate Javadoc
```

## Features
Books (add/update/delete/search/availability), Members (register/update/delete/validate),
Issue & return with late fine (Rs 2/day after 14 days), Reports (available, borrowed, top borrowers, fines).

## Structure
`app`, `model`, `service`, `repository`, `reports`, `exceptions` - see `oop-notes.md`.
