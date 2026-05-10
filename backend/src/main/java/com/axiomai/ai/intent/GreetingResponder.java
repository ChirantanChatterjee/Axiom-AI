package com.axiomai.ai.intent;

import java.util.Random;

public class GreetingResponder {

    private static final Random random =
            new Random();

    public static boolean
            lastGreetingWasInteractive = false;

    private static final String[]
            interactiveGreetings = {

            "Hey! Want to try a question?",

            "Hello! Want a quick math challenge?",

            "Hi there! Want me to ask you something?",

            "Hey! Ready for a math question?"
    };

    private static final String[]
            normalGreetings = {

            "Hello!",

            "Hey there!",

            "Greetings!",

            "Hi! I'm ready to help."
    };

    public static String randomGreeting() {

        boolean interactive =
                random.nextInt(100) < 70;

        lastGreetingWasInteractive =
                interactive;

        if (interactive) {

            return interactiveGreetings[
                    random.nextInt(
                            interactiveGreetings.length
                    )
                    ];
        }

        return normalGreetings[
                random.nextInt(
                        normalGreetings.length
                )
                ];
    }
}