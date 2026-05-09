package com.axiomai.ai.preprocessing;

public class MatrixParser {

    public static double[][] parse(String text) {
        if (text == null) return null;

        // Normalise input
        String cleaned = text
                .replaceAll("\\s+", "")          // remove spaces
                .replaceAll(";", "],[")          // MATLAB-style row separator
                .replaceAll("\\]\\s*\\[", "],[") // remove spaces between ] and [
                .toLowerCase();

        // Ensure matrix is wrapped in [[ ]]
        if (!cleaned.startsWith("[[")) {
            cleaned = "[[" + cleaned + "]]";
        }

        int start = cleaned.indexOf("[[");
        int end = cleaned.lastIndexOf("]]");
        if (start == -1 || end == -1 || end <= start + 2) {
            return null;
        }

        // Extract inside content
        String inside = cleaned.substring(start + 2, end);

        // Split rows
        String[] rowStrings = inside.split("\\],\\[");

        double[][] matrix = new double[rowStrings.length][];

        try {
            for (int i = 0; i < rowStrings.length; i++) {
                String row = rowStrings[i];

                // Split numbers
                String[] nums = row.split(",");

                matrix[i] = new double[nums.length];

                for (int j = 0; j < nums.length; j++) {
                    matrix[i][j] = Double.parseDouble(nums[j]);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return matrix;
    }
}
