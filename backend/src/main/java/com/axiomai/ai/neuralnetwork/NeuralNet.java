package com.axiomai.ai.neuralnetwork;

import java.io.Serializable;

public class NeuralNet implements Serializable {

    private static final long serialVersionUID = 1L;

    // 5 inputs → 4 hidden neurons
    double[][] W1 = new double[4][5];
    double[] B1 = new double[4];

    // 4 hidden → 1 output
    double[] W2 = new double[4];
    double B2 = Math.random();

    public NeuralNet() {
        // Random initialization
        for (int i = 0; i < 4; i++) {
            B1[i] = Math.random();
            W2[i] = Math.random();
            for (int j = 0; j < 5; j++) {
                W1[i][j] = Math.random();
            }
        }
    }

    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public static double sigmoidDerivative(double x) {
        double s = sigmoid(x);
        return s * (1 - s);
    }

    public double[] forwardHidden(double[] x) {
        double[] z1 = new double[4];
        double[] a1 = new double[4];

        for (int i = 0; i < 4; i++) {
            z1[i] = B1[i];
            for (int j = 0; j < 5; j++) {
                z1[i] += W1[i][j] * x[j];
            }
            a1[i] = sigmoid(z1[i]);
        }
        return a1;
    }

    public double forwardOutput(double[] a1) {
        double z2 = B2;
        for (int i = 0; i < 4; i++) {
            z2 += W2[i] * a1[i];
        }
        return sigmoid(z2);
    }

    public double predict(double[] x) {
        return forwardOutput(forwardHidden(x));
    }

    public void train(double[] x, double y, double lr) {

        double[] z1 = new double[4];
        double[] a1 = new double[4];

        for (int i = 0; i < 4; i++) {
            z1[i] = B1[i];
            for (int j = 0; j < 5; j++) {
                z1[i] += W1[i][j] * x[j];
            }
            a1[i] = sigmoid(z1[i]);
        }

        double z2 = B2;
        for (int i = 0; i < 4; i++) {
            z2 += W2[i] * a1[i];
        }
        double yHat = sigmoid(z2);

        double delta2 = (yHat - y) * sigmoidDerivative(z2);

        double[] delta1 = new double[4];
        for (int i = 0; i < 4; i++) {
            delta1[i] = delta2 * W2[i] * sigmoidDerivative(z1[i]);
        }

        for (int i = 0; i < 4; i++) {
            W2[i] -= lr * delta2 * a1[i];
        }
        B2 -= lr * delta2;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                W1[i][j] -= lr * delta1[i] * x[j];
            }
            B1[i] -= lr * delta1[i];
        }
    }
}
