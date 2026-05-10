package com.axiomai.ai.intent;

import com.axiomai.ai.neuralnetwork.NeuralNet;
import com.axiomai.ai.preprocessing.MathNormalizer;
import com.axiomai.service.Memory;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.core.SerializationHelper;

import java.io.ObjectInputStream;
import java.util.ArrayList;

public class IntentClassifier {

    private static FilteredClassifier model;

    private static Instances header;

    private static NeuralNet nn;

    // =====================================================
    // LOAD NN MODEL
    // =====================================================

    static {

        try {

            nn = (NeuralNet)
                    new ObjectInputStream(

                            IntentClassifier.class
                                    .getResourceAsStream("/nn.model")

                    ).readObject();

            System.out.println(
                    "NN PATH = " +
                            IntentClassifier.class
                                    .getResource("/nn.model")
            );

        } catch (Exception e) {

            nn = new NeuralNet();

            System.out.println(
                    "NN model failed to load → fallback NN"
            );

            e.printStackTrace();
        }
    }

    // =====================================================
    // LOAD WEKA MODEL
    // =====================================================

    static {

        try {

            Object obj =
                    SerializationHelper.read(

                            IntentClassifier.class
                                    .getResourceAsStream("/intent.model")
                    );

            model = (FilteredClassifier) obj;

            System.out.println(
                    "Loaded model class = "
                            + obj.getClass().getName()
            );

            ArrayList<Attribute> atts =
                    new ArrayList<>();

            atts.add(
                    new Attribute(
                            "text",
                            (ArrayList<String>) null
                    )
            );

            ArrayList<String> classVals =
                    new ArrayList<>();

            classVals.add("ARITHMETIC");
            classVals.add("ASK_QUESTION");
            classVals.add("BREAKDOWN");
            classVals.add("CHITCHAT");
            classVals.add("GREETING");
            classVals.add("GRAPH");
            classVals.add("INVEST_PATTERN");
            classVals.add("INVEST_REQUIRED_PRINCIPAL");
            classVals.add("INVEST_SIMPLE");
            classVals.add("INVEST_YEARS");
            classVals.add("OTHER");
            classVals.add("QUIZ_CONTINUE");
            classVals.add("QUIZ_STOP");
            classVals.add("ADVANCED_MATH");

            atts.add(
                    new Attribute(
                            "intent",
                            classVals
                    )
            );

            header =
                    new Instances(
                            "IntentData",
                            atts,
                            1
                    );

            header.setClassIndex(1);

            System.out.println(
                    "Header class values = "
                            + header.classAttribute()
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =====================================================
    // MAIN PREDICTION
    // =====================================================

    public static String predict(String text) {

        try {

            text = MathNormalizer.normalize(text);

            text = text.toLowerCase().trim();

            // =====================================================
            // QUIZ FOLLOWUPS
            // =====================================================

            if (Memory.awaitingQuizConfirmation) {

                if (text.equals("yes") ||
                        text.equals("another") ||
                        text.equals("one more") ||
                        text.equals("continue")) {

                    return "QUIZ_CONTINUE";
                }

                if (text.equals("no") ||
                        text.equals("stop") ||
                        text.equals("quit")) {

                    return "QUIZ_STOP";
                }
            }

            // =====================================================
            // HARD COMMAND ROUTING
            // =====================================================

            if (text.equals("math") ||
                    text.equals("maths") ||
                    text.equals("math solving") ||
                    text.equals("solve math")) {

                return "ARITHMETIC";
            }

            if (text.equals("quiz") ||
                    text.equals("ask me something") ||
                    text.equals("ask question") ||
                    text.equals("can you ask me something")) {

                return "ASK_QUESTION";
            }

            if (text.equals("graph plotting") ||
                    text.equals("plot graph")) {

                return "GRAPH";
            }

            // =====================================================
            // BREAKDOWN SHORTCUTS
            // =====================================================

            if (

                    text.equals("explain")

                            ||

                            text.equals("breakdown")

                            ||

                            text.equals("explain this")

                            ||

                            text.equals("how")

                            ||

                            text.equals("why")

            ) {

                return "BREAKDOWN";
            }

            // =====================================================
            // HARD RULE ROUTING
            // =====================================================

            if (IntentDetector.isGraph(text)) {

                return "GRAPH";
            }

            if (IntentDetector.isAdvancedMath(text)) {

                return "ADVANCED_MATH";
            }

            if (IntentDetector.isArithmetic(text)) {

                return "ARITHMETIC";
            }

            if (IntentDetector.isInvestment(text)) {

                if (IntentDetector.isPatternInvestment(text)) {

                    return "INVEST_PATTERN";
                }

                if (IntentDetector.isRequiredPrincipal(text)) {

                    return "INVEST_REQUIRED_PRINCIPAL";
                }

                if (IntentDetector.isYearsQuestion(text)) {

                    return "INVEST_YEARS";
                }

                return "INVEST_SIMPLE";
            }

            // =====================================================
            // WEKA ROUTING
            // =====================================================

            Instance inst =
                    new DenseInstance(2);

            inst.setDataset(header);

            inst.setValue(
                    header.attribute("text"),
                    text
            );

            inst.setClassMissing();

            double predIndex =
                    model.classifyInstance(inst);

            double[] dist =
                    model.distributionForInstance(inst);

            System.out.println(
                    "predIndex = " + predIndex
            );

            System.out.println(
                    "dist = " +
                            java.util.Arrays.toString(dist)
            );

            // =====================================================
            // CONFIDENCE FEATURES
            // =====================================================

            double top =
                    dist[(int) predIndex];

            double second = 0.0;

            for (int i = 0; i < dist.length; i++) {

                if (i != (int) predIndex &&
                        dist[i] > second) {

                    second = dist[i];
                }
            }

            double gap = top - second;

            double entropy = 0.0;

            for (double p : dist) {

                if (p > 0) {

                    entropy -= p * Math.log(p);
                }
            }

            double maxBelowTop = 0.0;

            for (int i = 0; i < dist.length; i++) {

                if (dist[i] != top &&
                        dist[i] != second &&
                        dist[i] > maxBelowTop) {

                    maxBelowTop = dist[i];
                }
            }

            double[] x = {
                    top,
                    second,
                    gap,
                    entropy,
                    maxBelowTop
            };

            double nnScore =
                    nn.predict(x);

            String predictedIntent =
                    header.classAttribute()
                            .value((int) predIndex);

            System.out.println(
                    "NN confidence = " + nnScore
            );

            System.out.println(
                    "Predicted intent = " +
                            predictedIntent
            );

            // =====================================================
            // HYBRID CONFIDENCE ROUTING
            // =====================================================

            // VERY HIGH CONFIDENCE
            if (nnScore >= 0.75) {

                System.out.println(
                        "HIGH CONFIDENCE ROUTE"
                );

                return predictedIntent;
            }

            // MEDIUM CONFIDENCE
            if (

                    nnScore >= 0.45

                            &&

                            top >= 0.55

            ) {

                System.out.println(
                        "MEDIUM CONFIDENCE ROUTE"
                );

                return predictedIntent;
            }

            // STRONG WEKA SIGNAL
            if (top >= 0.80) {

                System.out.println(
                        "WEKA STRONG SIGNAL ROUTE"
                );

                return predictedIntent;
            }

            // LOW CONFIDENCE
            System.out.println(
                    "LOW CONFIDENCE → OTHER"
            );

            return "OTHER";

        } catch (Exception e) {

            e.printStackTrace();

            return "OTHER";
        }
    }
}