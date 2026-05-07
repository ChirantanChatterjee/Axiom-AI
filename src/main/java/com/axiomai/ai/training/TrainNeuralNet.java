package com.axiomai.ai.training;

import com.axiomai.ai.neuralnetwork.NeuralNet;
import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.SerializationHelper;

import java.io.*;
import java.util.ArrayList;

public class TrainNeuralNet {

    public static void main(String[] args) throws Exception {

        // -----------------------------
        // Load Weka model
        // -----------------------------
        InputStream modelStream = TrainIntentModel.class.getResourceAsStream("/intent.model");
        if (modelStream == null) throw new FileNotFoundException("intent.model not found");

        File tempModel = File.createTempFile("intent", ".model");
        tempModel.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempModel)) {
            modelStream.transferTo(out);
        }

        FilteredClassifier model = (FilteredClassifier)
                SerializationHelper.read(tempModel.getAbsolutePath());


        // -----------------------------
        // Build header (same as IntentClassifier)
        // -----------------------------
        ArrayList<Attribute> atts = new ArrayList<>();
        atts.add(new Attribute("text", (ArrayList<String>) null));

        ArrayList<String> classVals = new ArrayList<>();
        classVals.add("ARITHMETIC");
        classVals.add("ASK_QUESTION");
        classVals.add("BREAKDOWN");
        classVals.add("CHITCHAT");
        classVals.add("GREETING");
        classVals.add("INVEST_PATTERN");
        classVals.add("INVEST_REQUIRED_PRINCIPAL");
        classVals.add("INVEST_SIMPLE");
        classVals.add("INVEST_YEARS");
        classVals.add("OTHER");
        classVals.add("QUIZ_CONTINUE");
        classVals.add("QUIZ_STOP");
        classVals.add("ADVANCED_MATH");

        atts.add(new Attribute("intent", classVals));

        Instances header = new Instances("IntentData", atts, 1);
        header.setClassIndex(1);

        // -----------------------------
        // Load training CSV
        // -----------------------------
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File("intents.csv"));
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);

        // -----------------------------
        // Create neural net
        // -----------------------------
        NeuralNet nn = new NeuralNet();

        double lr = 0.1;
        int epochs = 200;

        System.out.println("Training neural net...");

        // -----------------------------
        // Training loop
        // -----------------------------
        for (int epoch = 0; epoch < epochs; epoch++) {

            double totalLoss = 0;

            for (int i = 0; i < data.numInstances(); i++) {

                Instance row = data.instance(i);
                String text = row.stringValue(0);
                String trueLabel = row.stringValue(1);

                // Build instance for Weka
                Instance inst = new DenseInstance(header.numAttributes());
                inst.setDataset(header);
                inst.setValue(header.attribute("text"), text);
                inst.setClassMissing();

                // Weka prediction
                double predIndex = model.classifyInstance(inst);
                double[] dist = model.distributionForInstance(inst);
                String predictedLabel = header.classAttribute().value((int) predIndex);

                // Extract top + second confidence
                double top = dist[(int) predIndex];
                double second = 0.0;

                for (int j = 0; j < dist.length; j++) {
                    if (j != (int) predIndex && dist[j] > second) {
                        second = dist[j];
                    }
                }

                double gap = top - second;

                // entropy = -Σ p log(p)
                double entropy = 0.0;
                for (double p : dist) {
                    if (p > 0) entropy -= p * Math.log(p);
                }

                // largest probability that isn't top or second
                double maxBelowTop = 0.0;
                for (int j = 0; j < dist.length; j++) {
                    if (dist[j] != top && dist[j] != second && dist[j] > maxBelowTop) {
                        maxBelowTop = dist[j];
                    }
                }

                double[] x = {top, second, gap, entropy, maxBelowTop};

                double y = predictedLabel.equals(trueLabel) ? 1.0 : 0.0;

                // Train NN
                nn.train(x, y, lr);

                // Track loss
                double yHat = nn.predict(x);
                totalLoss += Math.pow(yHat - y, 2);
            }

            if (epoch % 20 == 0) {
                System.out.println("Epoch " + epoch + " | Loss = " + totalLoss);
            }
        }

        // -----------------------------
        // Save trained neural net
        // -----------------------------
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("nn.model"));
        out.writeObject(nn);
        out.close();

        System.out.println("Training complete. Saved nn.model");
    }
}
