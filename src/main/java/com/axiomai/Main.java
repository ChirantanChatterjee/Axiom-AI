package com.axiomai;

import com.axiomai.math.solver.MathSolver;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("Math Expert AI — powered by ML intent detection");
        System.out.println("------------------------------------------------");

        while (true) {
            System.out.print("\nYou: ");
            String input = sc.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            String response = MathSolver.solve(input);
            System.out.println("Math Expert AI: " + response);
        }
    }
}
