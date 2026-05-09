package com.axiomai.ai.intent;

import java.util.Random;

public class GreetingResponder {

    private static final Random rand = new Random();

    private static final String[] RESPONSES = {
            "Hey! Good to see you.",
            "Hello! Ready for some math?",
            "Hi there — what’s on your mind?",
            "Hey! Want to try a question?",
            "Hello! How can I help today?",
            "Hi! I’m here and ready.",
            "Heya! What shall we do?",
            "Yo! Want to crunch some numbers?",
            "Greetings! What would you like to explore?",
            "Hey! I’m all ears."
    };

    public static String randomGreeting() {
        return RESPONSES[rand.nextInt(RESPONSES.length)];
    }
}
