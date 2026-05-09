package com.axiomai.math.solver;

public class TheoremSolver {

    public static String solve(String text) {

        String lower = text.toLowerCase();

        // =====================================================
        // BASIC GEOMETRY
        // =====================================================

        if (lower.contains("pythagoras")) {
            return pythagoras();
        }

        if (lower.contains("triangle inequality")) {
            return triangleInequality();
        }

        if (lower.contains("thales")) {
            return thales();
        }

        // =====================================================
        // CALCULUS
        // =====================================================

        if (lower.contains("fundamental theorem of calculus")) {
            return fundamentalTheoremOfCalculus();
        }

        if (lower.contains("mean value theorem")) {
            return meanValueTheorem();
        }

        if (lower.contains("rolle")) {
            return rollesTheorem();
        }

        if (lower.contains("stokes")) {
            return stokesTheorem();
        }

        if (lower.contains("green")) {
            return greensTheorem();
        }

        if (lower.contains("divergence theorem")) {
            return divergenceTheorem();
        }

        // =====================================================
        // LINEAR ALGEBRA
        // =====================================================

        if (lower.contains("rank-nullity") ||
                lower.contains("rank nullity")) {

            return rankNullity();
        }

        if (lower.contains("spectral theorem")) {
            return spectralTheorem();
        }

        if (lower.contains("cayley-hamilton")) {
            return cayleyHamilton();
        }

        // =====================================================
        // VECTOR / INNER PRODUCT
        // =====================================================

        if (lower.contains("cauchy-schwarz") ||
                lower.contains("cauchy schwarz")) {

            return cauchySchwarz();
        }

        // =====================================================
        // NUMBER THEORY
        // =====================================================

        if (lower.contains("fermat")) {
            return fermatLittle();
        }

        if (lower.contains("euclid")) {
            return euclidInfinitePrimes();
        }

        if (lower.contains("chinese remainder")) {
            return chineseRemainder();
        }

        // =====================================================
        // PROBABILITY
        // =====================================================

        if (lower.contains("bayes")) {
            return bayesTheorem();
        }

        if (lower.contains("central limit")) {
            return centralLimit();
        }

        if (lower.contains("law of large numbers")) {
            return lawOfLargeNumbers();
        }

        return """
                I recognise this as a theorem question,
                but I do not yet have a stored explanation.

                Supported theorem areas:
                • geometry
                • calculus
                • linear algebra
                • number theory
                • probability
                • vector mathematics
                """;
    }

    // =====================================================
    // GEOMETRY
    // =====================================================

    private static String pythagoras() {

        return """
                Pythagoras Theorem:

                In a right triangle:

                a² + b² = c²

                where c is the hypotenuse.

                The square of the hypotenuse equals
                the sum of the squares of the other two sides.
                """;
    }

    private static String triangleInequality() {

        return """
                Triangle Inequality Theorem:

                For any triangle:

                a + b > c
                a + c > b
                b + c > a

                The sum of any two sides must exceed
                the third side.
                """;
    }

    private static String thales() {

        return """
                Thales' Theorem:

                If A, B, C lie on a circle
                and AC is the diameter,
                then angle ABC is 90°.
                """;
    }

    // =====================================================
    // CALCULUS
    // =====================================================

    private static String fundamentalTheoremOfCalculus() {

        return """
                Fundamental Theorem of Calculus:

                1) If:

                F(x) = ∫_a^x f(t)dt

                then:

                F'(x) = f(x)

                2) Also:

                ∫_a^b f(x)dx = F(b) − F(a)

                where F is an antiderivative of f.
                """;
    }

    private static String meanValueTheorem() {

        return """
                Mean Value Theorem:

                If f is continuous on [a,b]
                and differentiable on (a,b),

                then there exists c in (a,b)
                such that:

                f'(c) = (f(b)-f(a))/(b-a)
                """;
    }

    private static String rollesTheorem() {

        return """
                Rolle's Theorem:

                If:
                • f is continuous on [a,b]
                • differentiable on (a,b)
                • f(a)=f(b)

                then there exists c such that:

                f'(c)=0
                """;
    }

    private static String stokesTheorem() {

        return """
                Stokes' Theorem:

                ∮ F·dr = ∬ (curl F)·n dS

                It relates circulation around a boundary
                to curl over the surface.
                """;
    }

    private static String greensTheorem() {

        return """
                Green's Theorem:

                ∮ (Pdx + Qdy)

                =

                ∬ (∂Q/∂x − ∂P/∂y)dA

                Converts line integrals into area integrals.
                """;
    }

    private static String divergenceTheorem() {

        return """
                Divergence Theorem:

                ∭ div(F)dV = ∬ F·n dS

                Relates flux through a closed surface
                to divergence inside the volume.
                """;
    }

    // =====================================================
    // LINEAR ALGEBRA
    // =====================================================

    private static String rankNullity() {

        return """
                Rank-Nullity Theorem:

                dim(V) = rank(T) + nullity(T)

                Dimension equals:
                rank + dimension of kernel.
                """;
    }

    private static String spectralTheorem() {

        return """
                Spectral Theorem:

                Any real symmetric matrix
                can be diagonalised
                by an orthogonal matrix.

                Eigenvalues are real.
                """;
    }

    private static String cayleyHamilton() {

        return """
                Cayley-Hamilton Theorem:

                Every square matrix satisfies
                its own characteristic equation.
                """;
    }

    // =====================================================
    // VECTOR
    // =====================================================

    private static String cauchySchwarz() {

        return """
                Cauchy-Schwarz Inequality:

                |u·v| ≤ ||u|| ||v||

                The magnitude of the dot product
                is bounded by the product
                of vector norms.
                """;
    }

    // =====================================================
    // NUMBER THEORY
    // =====================================================

    private static String fermatLittle() {

        return """
                Fermat's Little Theorem:

                If p is prime and a is not divisible by p:

                a^(p−1) ≡ 1 mod p
                """;
    }

    private static String euclidInfinitePrimes() {

        return """
                Euclid's Theorem:

                There are infinitely many prime numbers.
                """;
    }

    private static String chineseRemainder() {

        return """
                Chinese Remainder Theorem:

                Simultaneous congruences
                with coprime moduli
                have a unique solution
                modulo the product.
                """;
    }

    // =====================================================
    // PROBABILITY
    // =====================================================

    private static String bayesTheorem() {

        return """
                Bayes' Theorem:

                P(A|B) = P(B|A)P(A)/P(B)

                Used to update probabilities
                using new evidence.
                """;
    }

    private static String centralLimit() {

        return """
                Central Limit Theorem:

                Sums/averages of large random samples
                approach a normal distribution.
                """;
    }

    private static String lawOfLargeNumbers() {

        return """
                Law of Large Numbers:

                As sample size increases,
                sample averages converge
                to the expected value.
                """;
    }
}