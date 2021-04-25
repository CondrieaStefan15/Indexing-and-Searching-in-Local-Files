package com.riw.ProjectRIW;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.File;
import java.util.*;

public class DirectIndex {
    private HashMap<String, Integer> exceptionWords = null;
    private HashMap<String, Integer> stopWords = null;

    private MongoCollection<Document> outputMongoCollection = null;

    public DirectIndex(){
        this.exceptionWords = new HashMap<String, Integer>();
        this.stopWords = new HashMap<String, Integer>();
    }

    public void setExceptionWords(HashMap<String, Integer> exceptionWords) {
        this.exceptionWords = exceptionWords;
    }

    public void setStopWords(HashMap<String, Integer> stopWords) {
        this.stopWords = stopWords;
    }

    public void setOutputMongoCollection(MongoCollection<Document> outputMongoCollection) {
        this.outputMongoCollection = outputMongoCollection;
    }

    public void createIndex(File inputFolder)
    {
        if(inputFolder != null) {
            if(inputFolder.exists()){
                if(!inputFolder.isHidden()){

                    parseFolder(inputFolder);
                }
                else{
                    System.out.println("Error: folder \'" + inputFolder.getAbsolutePath() + "\' is hidden.");
                }
            }
            else{
                System.out.println("Error: folder \'" + inputFolder.getAbsolutePath() + "\' doesn't exists!");
            }
        }
        else{
            System.out.println("Error: input folder is null.");
        }
    }

    private void parseFolder(File inputFolder)
    {
        File[] listOfFile = inputFolder.listFiles((file)->{return file.isDirectory() || file.isFile();});

        if(listOfFile != null){
            for(File file : listOfFile)
            {
                if(file.isDirectory()){
                    parseFolder(file);  //recusive
                }
                else{
                    parseFile(file);
                }
            }
        }

    }

    private void parseFile(File file)
    {
        //Step 1 - get string from file
        String text = ProcessFile.getStringFromFile(file);
        text = text.trim();
        if(text.length() == 0)
            return;


        //Step 2 - remove duplicate white-spaces;
        text = text.replaceAll("\\s{2,}", " ");


        //System.out.println((++nrDocumens) + ".\t" + file.getAbsolutePath());

        //Step 3 - split by non-alpha-numeric characters
        String[] words = text.toLowerCase().split("[^a-zA-Z0-9]");
        


        HashMap<String, Integer> wordsFrequency = new HashMap<>();

        for(String word : words)
        {
            if(word.length()>0){

                if(wordsFrequency.containsKey(word)){
                    wordsFrequency.replace(word, wordsFrequency.get(word) + 1);
                }
                else{
                    if(!stopWords.containsKey(word)){
                        wordsFrequency.put(word, 1);
                    }
                    else if(exceptionWords.containsKey(word)){
                        wordsFrequency.put(word, 1);
                    }
                }
            }

        }

        //Step 5-6
        if(wordsFrequency.size() > 0){

            //Step 5 - create...
            Document document = new Document("doc", file.getAbsolutePath());

            AddTermsToDocument(document, wordsFrequency);

            outputMongoCollection.insertOne(document);
        }


/*
        Pas1: Preluare text din fisier
        Pas : Eliminarea caracterelor non-alpha-numerice
        Pas2: Stergerea tuturor spatiilor mai mari decat un spatiu
        Pas3: Eliminarea stopWord-urilor
        Pas4: Creare vector de frecvente, adica un HashMap<String, Integer>
        Pas5: Creare document pe baza Hash-ului
        Pas6: Adugare document in colectie
*/


    }



    private static Document AddTermsToDocument(Document document, HashMap<String, Integer> stringIntegerHashMap)
    {

        if(stringIntegerHashMap.size() > 0){
            List<Document> documentList = new ArrayList<>();

            Collection<Integer> values = stringIntegerHashMap.values();  //view al colectiei nemodificata
            int totalWords = values.stream().mapToInt(i->i).sum();


            for(Map.Entry<String, Integer> entry : stringIntegerHashMap.entrySet())
            {

                Document docI = new Document("term", entry.getKey())
                        .append("count", entry.getValue())
                        .append("tf", ((double)entry.getValue() / totalWords));

                documentList.add(docI);

            }
            document.append("terms", documentList);
        }


        return document;
    }


}
