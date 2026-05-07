package com.axiomai.math.finance;

import com.axiomai.ai.preprocessing.NumberExtractor;
import com.axiomai.service.Memory;

public class InvestmentSolver {

    // Compound interest: A = P(1+r)^t
    public static double compoundForward(double principal, double rate, double years) {
        return principal * Math.pow(1 + rate, years);
    }

    // Reverse: P = A / (1+r)^t
    public static double requiredPrincipal(double target, double rate, double years) {
        return target / Math.pow(1 + rate, years);
    }

    // Reverse: t = ln(A/P) / ln(1+r)
    public static double yearsToReach(double principal, double rate, double target) {
        return Math.log(target / principal) / Math.log(1 + rate);
    }

    // Forward interest
    public static String solveForward(String text) {
        double principal = NumberExtractor.extractEuro(text);
        double rate = NumberExtractor.extractPercent(text);
        double years = NumberExtractor.extractYears(text);

        if (principal == -1 || rate == -1 || years == -1)
            return "I need principal, rate, and years.";

        double result = compoundForward(principal, rate / 100.0, years);

        String answer = "If you invest " + principal + " euro at " + rate +
                "% for " + years + " years, you will have about " +
                String.format("%,.2f", result) + " euro.";

        Memory.lastAnswer = answer;
        return answer;
    }

    // Required principal
    public static String solveRequiredPrincipal(String text) {
        double target = NumberExtractor.extractEuro(text);
        double rate = NumberExtractor.extractPercent(text);
        double years = NumberExtractor.extractYears(text);

        if (target == -1 || rate == -1 || years == -1)
            return "I need target amount, rate, and years.";

        double principal = requiredPrincipal(target, rate / 100.0, years);

        String answer = "To reach " + target + " euro in " + years +
                " years at " + rate + "%, you need to invest about " +
                String.format("%,.2f", principal) + " euro.";

        Memory.lastAnswer = answer;
        return answer;
    }

    // Years needed
    public static String solveYears(String text) {
        double[] euros = NumberExtractor.extractTwoEuroValues(text);
        if (euros == null) return "I need starting and target euro amounts.";

        double principal = euros[0];
        double target = euros[1];

        double rate = NumberExtractor.extractPercent(text);
        if (rate == -1) return "I need the interest rate.";

        double years = yearsToReach(principal, rate / 100.0, target);

        String answer = "To grow " + principal + " euro to " + target +
                " euro at " + rate + "%, it will take about " +
                String.format("%.2f", years) + " years.";

        Memory.lastAnswer = answer;
        return answer;
    }
}

