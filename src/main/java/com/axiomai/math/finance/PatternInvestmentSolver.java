package com.axiomai.math.finance;

import com.axiomai.ai.preprocessing.NumberExtractor;
import com.axiomai.service.Memory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternInvestmentSolver {

    // Default rates for vague natural‑language patterns
    private static double interpretWordRate(String text) {
        text = text.toLowerCase();

        if (text.contains("grows again")) return 5.0;
        if (text.contains("grows")) return 5.0;
        if (text.contains("rises")) return 4.0;
        if (text.contains("increases")) return 3.0;
        if (text.contains("falls a bit")) return -1.0;
        if (text.contains("falls")) return -3.0;
        if (text.contains("drops")) return -2.0;
        if (text.contains("declines")) return -2.0;

        return 0.0;
    }

    public static String solvePattern(String text) {
        text = text.toLowerCase();

        // 1. Extract principal
        double principal = NumberExtractor.extractEuro(text);
        if (principal == -1) {
            return "I need to know the starting amount in euro.";
        }

        // 2. Extract all explicit percents and years
        double[] rates = NumberExtractor.extractAllPercents(text);
        double[] years = NumberExtractor.extractAllYears(text);

        // 3. If no explicit percents, infer from vague language
        if (rates.length == 0) {
            double inferred = interpretWordRate(text);
            if (inferred == 0.0) {
                return "I couldn't understand the growth/decline pattern clearly enough to compute it.";
            }
            rates = new double[]{ inferred };
        }

        // 4. If no explicit years, cannot compute
        if (years.length == 0) {
            return "I need to know for how many years the pattern applies.";
        }

        // 5. Extract total years (e.g., "after 10 years")
        double totalYears = years[years.length - 1]; // fallback
        Matcher m = Pattern.compile("after\\s+(\\d+\\.?\\d*)\\s*years?")
                .matcher(text);
        if (m.find()) {
            totalYears = Double.parseDouble(m.group(1));
        }

        // 6. Stage durations = the years mentioned before the final "after X years"
        double[] stageYears = years;

        double sumStageYears = 0;
        for (double y : stageYears) sumStageYears += y;

        boolean userMentionedRepeat =
                text.contains("repeat") ||
                        text.contains("similar pattern") ||
                        text.contains("same pattern");

        // Option 3: Ask user if pattern should repeat
        if (totalYears > sumStageYears && !userMentionedRepeat) {
            return "It looks like you described a pattern that covers about " +
                    sumStageYears + " years, but you asked about " + totalYears +
                    " years. Should I repeat the same pattern to fill the remaining years?";
        }

        // 7. Apply pattern (with repetition if user hinted)
        double amount = principal;
        double yearsApplied = 0.0;
        int stageIndex = 0;

        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Breakdown of the pattern:\n\n");
        breakdown.append("Starting amount: ").append(principal).append(" euro\n\n");

        while (yearsApplied < totalYears) {

            double rate = rates[Math.min(stageIndex, rates.length - 1)] / 100.0;
            double stageLen = stageYears[Math.min(stageIndex, stageYears.length - 1)];

            // Trim last stage if overshooting
            if (yearsApplied + stageLen > totalYears) {
                stageLen = totalYears - yearsApplied;
            }

            double before = amount;
            amount = amount * Math.pow(1 + rate, stageLen);

            // Add to breakdown
            breakdown.append("Stage ").append(stageIndex + 1).append(":\n");
            breakdown.append("  Rate: ").append(rate * 100).append("%\n");
            breakdown.append("  Duration: ").append(stageLen).append(" years\n");
            breakdown.append("  From ").append(String.format("%,.2f", before))
                    .append(" → ").append(String.format("%,.2f", amount)).append("\n\n");

            yearsApplied += stageLen;
            stageIndex++;

            // If end of pattern reached
            if (stageIndex >= Math.max(rates.length, stageYears.length)) {
                if (userMentionedRepeat) {
                    stageIndex = 0; // repeat pattern
                } else {
                    break;
                }
            }
        }

        String answer = "Following the described pattern, your " + principal +
                " euro would grow to about " + String.format("%,.2f", amount) +
                " euro after " + String.format("%.2f", yearsApplied) + " years.";

        // Save breakdown + answer for "break it down"
        Memory.lastBreakdown = breakdown.toString();
        Memory.lastAnswer = answer;

        return answer;
    }
}
