package com.axiomai.ai.preprocessing;

import com.axiomai.math.expression.*;

public class SymbolicParser {

    public static Expr parse(String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            Expr parse() {
                nextChar();
                Expr x = parseExpression();
                if (pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                return x;
            }

            Expr parseExpression() {
                Expr x = parseTerm();
                for (;;) {
                    if (eat('+')) x = new Add(x, parseTerm());
                    else if (eat('-')) x = new Sub(x, parseTerm());
                    else return x;
                }
            }

            Expr parseTerm() {
                Expr x = parseFactor();
                for (;;) {
                    if (eat('*')) x = new Mul(x, parseFactor());
                    else if (eat('/')) x = new Div(x, parseFactor());
                    else return x;
                }
            }

            Expr parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return new Mul(new Const(-1), parseFactor());

                Expr x;
                int startPos = this.pos;

                if (eat('(')) {
                    x = parseExpression();
                    if (!eat(')')) {
                        throw new RuntimeException("Missing ')'");
                    }
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    double value = Double.parseDouble(str.substring(startPos, this.pos));
                    x = new Const(value);
                } else if (Character.isLetter(ch)) {
                    while (Character.isLetterOrDigit(ch)) nextChar();
                    String name = str.substring(startPos, this.pos);
                    if (name.equals("x")) {
                        x = new Var("x");
                    } else if (name.equals("sin")) {
                        if (!eat('(')) throw new RuntimeException("Expected '(' after sin");
                        Expr arg = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after sin argument");
                        x = new Sin(arg);
                    } else if (name.equals("cos")) {
                        if (!eat('(')) throw new RuntimeException("Expected '(' after cos");
                        Expr arg = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after cos argument");
                        x = new Cos(arg);
                    } else if (name.equals("exp") || name.equals("e")) {
                        if (eat('^')) {
                            // e^something
                            Expr exponent = parseFactor();
                            x = new Exp(exponent);
                        } else if (eat('(')) {
                            Expr arg = parseExpression();
                            if (!eat(')')) throw new RuntimeException("Missing ')' after exp argument");
                            x = new Exp(arg);
                        } else {
                            x = new Var(name);
                        }
                    } else {
                        x = new Var(name);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                // handle exponentiation
                if (eat('^')) {
                    Expr exponent = parseFactor();
                    x = new Pow(x, exponent);
                }

                return x;
            }
        }.parse();
    }
}
