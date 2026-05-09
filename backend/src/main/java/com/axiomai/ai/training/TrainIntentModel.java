package com.axiomai.ai.training;

import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.SerializationHelper;

import java.io.*;

public class TrainIntentModel {

    public static void main(String[] args) throws Exception {

        InputStream in = TrainIntentModel.class.getResourceAsStream("/intents.csv");
        if (in == null) throw new FileNotFoundException("intents.csv not found");

        File temp = File.createTempFile("intents", ".csv");
        temp.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(temp)) {
            in.transferTo(out);
        }

        CSVLoader loader = new CSVLoader();
        loader.setStringAttributes("first");
        loader.setSource(temp);

        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);

        StringToWordVector stwv = new StringToWordVector();
        stwv.setTFTransform(true);
        stwv.setIDFTransform(true);
        stwv.setLowerCaseTokens(true);
        stwv.setWordsToKeep(5000);

        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(stwv);
        fc.setClassifier(new NaiveBayesMultinomial());

        fc.buildClassifier(data);

        SerializationHelper.write("intent.model", fc);

        System.out.println("Training complete. Saved intent.model");
    }
}

