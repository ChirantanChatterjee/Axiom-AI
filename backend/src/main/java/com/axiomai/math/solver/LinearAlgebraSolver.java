package com.axiomai.math.solver;

import com.axiomai.ai.preprocessing.MatrixParser;

import java.util.Arrays;

public class LinearAlgebraSolver{

    public static String solveEigen(String text) {
        double[][] A = MatrixParser.parse(text);
        if (A == null || A.length != 2 || A[0].length != 2) {
            return "Right now I can only find eigenvalues for 2×2 matrices like [[a,b],[c,d]].";
        }

        double a = A[0][0], b = A[0][1];
        double c = A[1][0], d = A[1][1];

        double trace = a + d;
        double det = a * d - b * c;
        double disc = trace * trace - 4 * det;

        if (disc < 0) {
            return "The eigenvalues are complex; I don't fully handle complex eigenvalues yet.";
        }

        double sqrtDisc = Math.sqrt(disc);
        double lambda1 = (trace + sqrtDisc) / 2.0;
        double lambda2 = (trace - sqrtDisc) / 2.0;

        return "For matrix " + Arrays.deepToString(A) +
                " the eigenvalues are λ₁ = " + lambda1 + ", λ₂ = " + lambda2 + ".";
    }

    public static String solveDeterminant(String text) {
        double[][] A = MatrixParser.parse(text);
        if (A == null) {
            return "I couldn't parse the matrix. Use a format like [[a,b],[c,d]].";
        }
        if (A.length == 2 && A[0].length == 2) {
            double det = A[0][0] * A[1][1] - A[0][1] * A[1][0];
            return "The determinant of " + Arrays.deepToString(A) + " is " + det + ".";
        }
        return "I currently only compute determinants for 2×2 matrices.";
    }

    public static String solveDiagonalize(String text) {
        double[][] A = MatrixParser.parse(text);
        if (A == null || A.length != 2 || A[0].length != 2) {
            return "I can only try to diagonalise simple 2×2 matrices right now.";
        }
        // For now, just reuse eigenvalue logic and explain conceptually.
        String eigenInfo = solveEigen(text);
        return eigenInfo + " To diagonalise, you would form a matrix P of eigenvectors and D of eigenvalues.";
    }

}
