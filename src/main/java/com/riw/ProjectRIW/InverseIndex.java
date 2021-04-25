package com.riw.ProjectRIW;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.*;

public class InverseIndex {
    private MongoCollection<Document> inputDirectIndex;
    private MongoCollection<Document> outputInverseIndex;

    public static int nrDocuments = 0;


    public InverseIndex(MongoCollection<Document> inputDirectIndex, MongoCollection<Document> outputInverseIndex){
        this.inputDirectIndex = inputDirectIndex;
        this.outputInverseIndex = outputInverseIndex;
    }

	


    public void createInverseIndexLocal(){

        FindIterable<Document> documentFindIterable = inputDirectIndex.find();
        Iterator iterator = documentFindIterable.iterator(); //pentru a avea optiuile de hasnext, next etc

        HashMap<String, HashMap<String, Integer>> inverseIndexeHashMap = new HashMap<>();


        //  Parcurg lista de documente
        while (iterator.hasNext())
        {
            Document document = (Document)iterator.next();
            String docName = document.getString("doc");

            List<Document> terms = (List<Document>) document.get("terms");

            //System.out.println((++nrDocuments) + ". " + docName.substring(62) + "\t" + terms.size() + " cuv; ");

            for(Document docTerm: terms)
            {
                String term = docTerm.getString("term");
                Integer count = docTerm.getInteger("count");

                if (inverseIndexeHashMap.containsKey(term))
                {
                    inverseIndexeHashMap.get(term).put(docName, count);
                }
                else
                {
                    HashMap<String, Integer> newDocHash = new HashMap<>();

                    newDocHash.put(docName, count);
                    inverseIndexeHashMap.put(term, newDocHash);
                }

            }
        }


        long D = inputDirectIndex.count();  //nr total de documente


        for(Map.Entry<String, HashMap<String, Integer>> entry : inverseIndexeHashMap.entrySet()){

            String term = entry.getKey();

            Document documentTerm = new Document("term", term);

            HashMap<String, Integer> documents = entry.getValue();

            List<Document> documentList = new ArrayList<>();
            for(Map.Entry<String, Integer> entry1 : documents.entrySet()){
                Document document = new Document("doc", entry1.getKey()).append("count", entry1.getValue());
                documentList.add(document);
            }

            long keyD = documentList.size();    //nr de documente care contin termenul 'term'

            documentTerm.append("idf", (double) D/keyD);
            documentTerm.append("docs", documentList);
            outputInverseIndex.insertOne(documentTerm);
        }
    }

    public void createVectorDocuments(){

        HashMap<String, Double> idfDocs = new HashMap<>();  //va contine toate documentele din indexul invers pentru a prelua idf-ul din acestea

        FindIterable<Document> findIterableInverseIndex = outputInverseIndex.find();
        Iterator iteratorInverseIndex = findIterableInverseIndex.iterator();

        while(iteratorInverseIndex.hasNext()){
            Document document = (Document) iteratorInverseIndex.next();

            idfDocs.put(document.getString("term"), document.getDouble("idf"));
        }




        FindIterable<Document> findIterableDirectIndex = inputDirectIndex.find();
        Iterator iteratorDirectIndex = findIterableDirectIndex.iterator();

        int i=1;

        while (iteratorDirectIndex.hasNext())
        {
            Document document = (Document) iteratorDirectIndex.next();

            Document vectorAndModule = createVectorAndModule(document, idfDocs);


            Document modifyDocument = new Document();
            modifyDocument.put("$set", vectorAndModule);

            inputDirectIndex.updateOne(document, modifyDocument);


        }
    }

    //creaza vectorul corespunzator unui document si calculeaza modulul acestuia
    //ifds este un hash map care contine idfu-urile termenilor din indexul invers
    private static Document createVectorAndModule(Document document, HashMap<String, Double> idfs)
    {
        List<Document> terms = (List<Document>) document.get("terms");

        double sumSquare = 0;
        List<Document> vector = new ArrayList<>();

        for(Document docTerm : terms){
            String term = docTerm.getString("term");
            Double tf = docTerm.getDouble("tf");

            double idf = idfs.get(term);

            double element = tf * Math.log(idf);
            sumSquare += element * element;
            vector.add(new Document("term", term).append("value", element));
        }

        double module = Math.sqrt(sumSquare);        //se calculeaza modulul documentului -> este folosit la calcularea cosinusului.Fiecare document(inregistrare din directIndex) are un modul.

        Document vectorAndModule = new Document("module", module)
                .append("vector", vector);
        return vectorAndModule;
    }





}
