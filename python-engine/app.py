from flask import Flask, request, jsonify

from sympy import *
from sympy import latex
from sympy import lambdify

from sympy.parsing.sympy_parser import (
    parse_expr,
    standard_transformations,
    implicit_multiplication_application
)

import numpy as np

app = Flask(__name__)

# =========================================================
# SYMBOLS
# =========================================================

x, y, z = symbols('x y z')

# =========================================================
# SMART PARSER
# =========================================================

transformations = (
        standard_transformations +
        (implicit_multiplication_application,)
)

# =========================================================
# STEP GENERATORS
# =========================================================

def generate_derivative_steps(expr, result):

    steps = []

    expr_latex = latex(expr)
    result_latex = latex(result)

    steps.append(
        rf"f(x) = {expr_latex}"
    )

    expr_str = str(expr)

    # -----------------------------------------------------
    # CHAIN RULE
    # -----------------------------------------------------

    if "sin" in expr_str and "**" in expr_str:

        steps.append(
            r"\text{Detected composite trigonometric expression.}"
        )

        steps.append(
            r"\text{Applying chain rule: } "
            r"\frac{d}{dx}[\sin(u)] = \cos(u)\frac{du}{dx}"
        )

    # -----------------------------------------------------
    # PRODUCT RULE
    # -----------------------------------------------------

    if "*" in expr_str:

        steps.append(
            r"\text{Detected multiplication.}"
        )

        steps.append(
            r"\text{Applying product rule: } "
            r"(uv)' = u'v + uv'"
        )

    # -----------------------------------------------------
    # POWER RULE
    # -----------------------------------------------------

    if "**" in expr_str:

        steps.append(
            r"\text{Applying power rule where needed.}"
        )

    # -----------------------------------------------------
    # EXPONENTIAL
    # -----------------------------------------------------

    if "exp" in expr_str or "e**" in expr_str:

        steps.append(
            r"\text{Detected exponential function.}"
        )

    steps.append(
        rf"\text{{Final derivative: }} {result_latex}"
    )

    return steps

# =========================================================

def generate_integral_steps(expr, result):

    steps = []

    expr_latex = latex(expr)
    result_latex = latex(result)

    steps.append(
        rf"\int {expr_latex}\,dx"
    )

    expr_str = str(expr)

    # -----------------------------------------------------
    # POLYNOMIAL
    # -----------------------------------------------------

    if "**" in expr_str:

        steps.append(
            r"\text{Detected polynomial/power expression.}"
        )

        steps.append(
            r"\text{Applying power rule for integration: } "
            r"\int x^n dx = \frac{x^{n+1}}{n+1}"
        )

    # -----------------------------------------------------
    # TRIG
    # -----------------------------------------------------

    if "sin" in expr_str:

        steps.append(
            r"\text{Using identity: } "
            r"\int \sin(x)\,dx = -\cos(x)"
        )

    if "cos" in expr_str:

        steps.append(
            r"\text{Using identity: } "
            r"\int \cos(x)\,dx = \sin(x)"
        )

    # -----------------------------------------------------
    # EXPONENTIAL
    # -----------------------------------------------------

    if "exp" in expr_str or "e**" in expr_str:

        steps.append(
            r"\text{Detected exponential expression.}"
        )

    steps.append(
        rf"\text{{Final integral: }} {result_latex} + C"
    )

    return steps

# =========================================================

def generate_simplify_steps(expr, result):

    steps = []

    expr_latex = latex(expr)
    result_latex = latex(result)

    steps.append(
        rf"{expr_latex}"
    )

    expr_str = str(expr)

    # -----------------------------------------------------
    # TRIG IDENTITY
    # -----------------------------------------------------

    if "sin" in expr_str and "cos" in expr_str:

        simplified_check = simplify(expr)

        if simplified_check == 1:

            steps.append(
                r"\text{Using Pythagorean identity:}"
            )

            steps.append(
                r"\sin^2(x) + \cos^2(x) = 1"
            )

    # -----------------------------------------------------
    # FACTORIZATION
    # -----------------------------------------------------

    factored = factor(expr)

    if factored != expr:

        steps.append(
            r"\text{Expression can be factorized symbolically.}"
        )

        steps.append(
            rf"{latex(factored)}"
        )

    # -----------------------------------------------------
    # EXPANSION
    # -----------------------------------------------------

    expanded = expand(expr)

    if expanded != expr:

        steps.append(
            r"\text{Expanded algebraic form:}"
        )

        steps.append(
            rf"{latex(expanded)}"
        )

    steps.append(
        rf"\text{{Final simplified form: }} {result_latex}"
    )

    return steps

# =========================================================
# GRAPH GENERATOR
# =========================================================

def generate_graph(expr):

    try:

        func = lambdify(x, expr, "numpy")

        x_vals = np.linspace(-10, 10, 500)

        y_vals = func(x_vals)

        x_list = x_vals.tolist()

        y_list = []

        for val in y_vals:

            try:

                if np.isfinite(val):
                    y_list.append(float(val))
                else:
                    y_list.append(None)

            except:
                y_list.append(None)

        return {
            "x": x_list,
            "y": y_list
        }

    except Exception as e:

        print("GRAPH ERROR:", e)

        return None

# =========================================================
# MAIN API
# =========================================================

@app.route('/solve', methods=['POST'])
def solve():

    data = request.json

    problem = data.get("problem", "")

    try:

        lower = problem.lower()

        # =====================================================
        # GRAPH
        # =====================================================

        if "plot" in lower or "graph" in lower:

            expr_text = (
                lower.replace("plot", "")
                .replace("graph", "")
                .strip()
            )

            expr_text = expr_text.replace("^", "**")

            expr = parse_expr(
                expr_text,
                transformations=transformations
            )

            graph_data = generate_graph(expr)

            return jsonify({

                "type": "graph",

                "text": f"Plotting function: {expr_text}",

                "latex": latex(expr),

                "graph": graph_data,

                "steps": [
                    rf"\text{{Plotting function: }} {latex(expr)}"
                ]
            })

        # =====================================================
        # DERIVATIVE
        # =====================================================

        elif "derivative" in lower or "differentiate" in lower:

            expr_text = (
                lower.replace("find the derivative of", "")
                .replace("derivative of", "")
                .replace("differentiate", "")
                .strip()
            )

            expr_text = expr_text.replace("^", "**")

            expr = parse_expr(
                expr_text,
                transformations=transformations
            )

            result = diff(expr, x)

            steps = generate_derivative_steps(
                expr,
                result
            )

            return jsonify({

                "type": "derivative",

                "result": str(result),

                "latex": latex(result),

                "steps": steps
            })

        # =====================================================
        # INTEGRAL
        # =====================================================

        elif "integral" in lower or "integrate" in lower:

            expr_text = (
                lower.replace("find the integral of", "")
                .replace("integral of", "")
                .replace("integrate", "")
                .strip()
            )

            expr_text = expr_text.replace("^", "**")

            expr = parse_expr(
                expr_text,
                transformations=transformations
            )

            result = integrate(expr, x)

            steps = generate_integral_steps(
                expr,
                result
            )

            return jsonify({

                "type": "integral",

                "result": str(result),

                "latex": latex(result),

                "steps": steps
            })

        # =====================================================
        # LIMIT
        # =====================================================

        elif "limit" in lower:

            return jsonify({

                "type": "limit",

                "result": "Limit support coming next."
            })

        # =====================================================
        # SIMPLIFY
        # =====================================================

        else:

            expr_text = (
                lower.replace("simplify", "")
                .strip()
            )

            expr_text = expr_text.replace("^", "**")

            expr = parse_expr(
                expr_text,
                transformations=transformations
            )

            simplified = simplify(expr)

            result = factor(simplified)

            steps = generate_simplify_steps(
                expr,
                result
            )

            return jsonify({

                "type": "expression",

                "result": str(result),

                "latex": latex(result),

                "steps": steps
            })

    # =========================================================
    # ERROR HANDLING
    # =========================================================

    except Exception as e:

        print("ERROR:", e)

        return jsonify({

            "error": str(e)
        })

# =========================================================
# RUN SERVER
# =========================================================

if __name__ == '__main__':

    app.run(port=5000)