# Indexing-and-Searching-in-Local-Files
Returns the most relevant results for a given search

It is a Java Spring Boot project that processes files so that we can search for certain words or sentences and provide us with files relevant to our search.
File processing consists of creating Direct Index and Inverse Index for the words in the files and storing these collections in the local Mongo database.
Based on this processing, a series of parameters will be calculated and with their help and some calculation formulas, only the most relevant documents to our search will be displayed.

This project involved:
- connecting to the local mongo database
- directory processing (whether file or directory)
- processing information from files
- checking if a word is stopword or exception word
- creating the Direct Index and Indirect Index collections
- creating information storage formats in the Mongo database
- implementing a search engine (calculating some parameters and using some formulas) that displays the most relevant files
- creating an interface through which the user enters the searched words or sentences
