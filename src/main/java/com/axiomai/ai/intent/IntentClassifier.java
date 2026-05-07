package com.axiomai.ai.intent;

import com.axiomai.ai.neuralnetwork.NeuralNet;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.core.SerializationHelper;

import java.io.ObjectInputStream;
import java.util.ArrayList;

public class IntentClassifier {

    private static FilteredClassifier model;
    private static Instances header;
    private static NeuralNet nn;

    static {
        try {
            nn = (NeuralNet) new ObjectInputStream(
                    IntentClassifier.class.getResourceAsStream("/nn.model")
            ).readObject();
        } catch (Exception e) {
            nn = new NeuralNet(); // fallback
        }
    }
    // ← NEW

    static {
        try {
            Object obj = SerializationHelper.read(
                    IntentClassifier.class.getResourceAsStream("/intent.model")
            );
            System.out.println("Loaded model class = " + obj.getClass().getName());
            model = (FilteredClassifier) obj;

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

            header = new Instances("IntentData", atts, 1);
            header.setClassIndex(1);

            System.out.println("Header class values = " + header.classAttribute());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String predict(String text) {
        try {
            text = text.toLowerCase();
            if (IntentDetector.isAdvancedMath(text)) {
                return "ADVANCED_MATH";
            }

            if (IntentDetector.isArithmetic(text)) {
                return "ARITHMETIC";
            }

            Instance inst = new DenseInstance(2);
            inst.setDataset(header);
            inst.setValue(header.attribute("text"), text);
            inst.setClassMissing();

            double predIndex = model.classifyInstance(inst);
            double[] dist = model.distributionForInstance(inst);

            System.out.println("predIndex = " + predIndex);
            System.out.println("dist[0..] = " + java.util.Arrays.toString(dist));

            // -----------------------------
            // Extract top + second confidence
            // -----------------------------
            double top = dist[(int) predIndex];
            double second = 0.0;

            for (int i = 0; i < dist.length; i++) {
                if (i != (int) predIndex && dist[i] > second) {
                    second = dist[i];
                }
            }

            // -----------------------------
            // Neural Net Confidence Filter
            // -----------------------------
            double gap = top - second;

            double entropy = 0.0;
            for (double p : dist) {
                if (p > 0) entropy -= p * Math.log(p);
            }

            double maxBelowTop = 0.0;
            for (int i = 0; i < dist.length; i++) {
                if (dist[i] != top && dist[i] != second && dist[i] > maxBelowTop) {
                    maxBelowTop = dist[i];
                }
            }

            double[] x = {top, second, gap, entropy, maxBelowTop};
            double nnScore = nn.predict(x);

            System.out.println("NN confidence = " + nnScore);

            System.out.println("Weka predicted intent = " + header.classAttribute().value((int) predIndex));
            System.out.println("Weka top confidence = " + top);
            System.out.println("Weka second confidence = " + second);
            System.out.println("NeuralNet score = " + nnScore);



            if (nnScore < 0.3) {
                System.out.println("NN says low confidence → fallback to OTHER");
                return "OTHER";
            }

            // -----------------------------
            // Return Weka label
            // -----------------------------
            return header.classAttribute().value((int) predIndex);

        } catch (Exception e) {
            e.printStackTrace();
            return "OTHER";
        }
    }
}
