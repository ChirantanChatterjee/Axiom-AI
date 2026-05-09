package com.axiomai.service;

import java.util.Random;

public class DynamicQuestionGenerator {

    private static final Random rand = new Random();

    public static QuizQuestion generate() {
        int type = rand.nextInt(4);

        switch (type) {
            case 0: return addition();
            case 1: return subtraction();
            case 2: return multiplication();
            case 3: return division();
            default: return addition();
        }
    }

    private static QuizQuestion addition() {
        int a = rand.nextInt(50) + 1;
        int b = rand.nextInt(50) + 1;
        return new QuizQuestion("What is " + a + " + " + b, a + b);
    }

    private static QuizQuestion subtraction() {
        int a = rand.nextInt(100) + 50;
        int b = rand.nextInt(50) + 1;
        return new QuizQuestion("What is " + a + " - " + b, a - b);
    }

    private static QuizQuestion multiplication() {
        int a = rand.nextInt(12) + 1;
        int b = rand.nextInt(12) + 1;
        return new QuizQuestion("What is " + a + " × " + b, a * b);
    }

    private static QuizQuestion division() {
        int b = rand.nextInt(12) + 1;
        int a = b * (rand.nextInt(12) + 1);
        return new QuizQuestion("What is " + a + " ÷ " + b, a / b);
    }
}

