package com.axiomai.ai.routing;

import java.util.List;
import java.util.Random;

public class GreetingResponder {

    public static boolean lastGreetingWasInteractive =
            false;

    private static final List<String> greetings =
            List.of(

                    "Hey 👋 What would you like to solve today?",

                    "Hello! Ready for some AI-powered automation or math?",

                    "Hi there 🚀"

            );

    public static String randomGreeting() {

        lastGreetingWasInteractive = true;

        return greetings.get(
                new Random().nextInt(greetings.size())
        );

    }

}