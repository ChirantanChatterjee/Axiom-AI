from flask import Flask, request, jsonify
from flask_cors import CORS

import sympy as sp
from sympy import *
from sympy import latex
from sympy import lambdify

import re
import numpy as np

from sympy.parsing.sympy_parser import (
    parse_expr,
    standard_transformations,
    implicit_multiplication_application,
    convert_xor
)

# =========================================================
# APP
# =========================================================

app = Flask(__name__)

CORS(app)

# =========================================================
# SYMBOLS
# =========================================================

x, y, z = symbols('x y z')

# =========================================================
# TRANSFORMATIONS
# =========================================================

transformations = (
        standard_transformations +
        (
            implicit_multiplication_application,
            convert_xor
        )
)

# =========================================================
# LOCAL DICT
# =========================================================

local_dict = {

    "x": x,
    "y": y,
    "z": z,

    "sin": sin,
    "cos": cos,
    "tan": tan,

    "asin": asin,
    "acos": acos,
    "atan": atan,

    "log": log,
    "ln": log,

    "sqrt": sqrt,
    "exp": exp,

    "pi": pi,
    "e": E
}

# =========================================================
# NLP NORMALIZATION
# =========================================================

def normalize_nlp_math(text):

    text = text.lower()

    replacements = {

        "squared": "^2",
        "cubed": "^3",

        "plus": "+",
        "minus": "-",

        "multiplied by": "*",
        "times": "*",

        "divided by": "/",

        "to the power of": "^",

        "equals": "="
    }

    for k, v in replacements.items():

        text = text.replace(k, v)

    # =====================================================
    # REMOVE EXTRA SPACES
    # =====================================================

    text = re.sub(
        r"\s+",
        " ",
        text
    ).strip()

    # =====================================================
    # x ^2 -> x^2
    # =====================================================

    text = re.sub(
        r'([a-zA-Z])\s*\^\s*(\d+)',
        r'\1^\2',
        text
    )

    # =====================================================
    # 5x -> 5*x
    # =====================================================

    text = re.sub(
        r'(\d)([a-zA-Z])',
        r'\1*\2',
        text
    )

    return text

# =========================================================
# CLEANER
# =========================================================

def clean_expression_text(text):

    text = normalize_nlp_math(text)

    text = text.lower().strip()

    garbage = [

        # SOLVE
        "solve for x",
        "solve equation",
        "solve",

        # GRAPH
        "what is the graph of",
        "plot the graph of",
        "show the graph of",
        "show graph of",
        "plot graph of",
        "graph of",

        # DERIVATIVE
        "find the derivative of",
        "find derivative of",
        "derivative of",

        # INTEGRAL
        "find the integral of",
        "find integral of",
        "integral of",

        # SIMPLIFY
        "simplify the expression",

        # GENERIC NLP
        "what is",
        "what's",
        "what are",

        "can you",
        "could you",
        "would you",

        "please",

        "show me",
        "tell me",
        "give me",
        "help me",

        # COMMANDS
        "differentiate",
        "integrate",
        "simplify",

        "graph",
        "plot",
        "draw",
        "visualize",

        "the"
    ]

    garbage.sort(
        key=len,
        reverse=True
    )

    for phrase in garbage:

        escaped = re.escape(phrase)

        text = re.sub(
            rf"\b{escaped}\b",
            " ",
            text
        )

    text = text.replace("^", "**")

    text = re.sub(
        r"\s+",
        " ",
        text
    )

    return text.strip()

# =========================================================
# EQUATION STEPS
# =========================================================

def solve_equation_steps(eq, solutions):

    steps = []

    steps.append(
        rf"{latex(eq.lhs)} = {latex(eq.rhs)}"
    )

    steps.append(
        r"\text{Solving equation for } x"
    )

    sol_text = ", ".join(
        [latex(s) for s in solutions]
    )

    steps.append(
        rf"\text{{Solutions: }} {sol_text}"
    )

    return steps

# =========================================================
# DERIVATIVE STEPS
# =========================================================

def derivative_steps(expr, result):

    steps = []

    expr_latex = latex(expr)
    result_latex = latex(result)

    steps.append(
        rf"f(x) = {expr_latex}"
    )

    expr_str = str(expr)

    if "*" in expr_str:

        steps.append(
            r"\text{Detected multiplication. Applying product rule.}"
        )

        steps.append(
            r"(uv)' = u'v + uv'"
        )

    if "**" in expr_str:

        steps.append(
            r"\text{Applying power rule where needed.}"
        )

        steps.append(
            r"\frac{d}{dx}(x^n)=nx^{n-1}"
        )

    if "sin" in expr_str:

        steps.append(
            r"\frac{d}{dx}(\sin x)=\cos x"
        )

    if "cos" in expr_str:

        steps.append(
            r"\frac{d}{dx}(\cos x)=-\sin x"
        )

    steps.append(
        rf"\text{{Final derivative: }} {result_latex}"
    )

    return steps

# =========================================================
# INTEGRAL STEPS
# =========================================================

def integral_steps(expr, result):

    steps = []

    expr_latex = latex(expr)
    result_latex = latex(result)

    steps.append(
        rf"\int {expr_latex}\,dx"
    )

    expr_str = str(expr)

    if "**" in expr_str:

        steps.append(
            r"\text{Applying power rule for integration.}"
        )

        steps.append(
            r"\int x^n dx = \frac{x^{n+1}}{n+1}"
        )

    if "sin" in expr_str:

        steps.append(
            r"\int \sin(x)\,dx = -\cos(x)"
        )

    if "cos" in expr_str:

        steps.append(
            r"\int \cos(x)\,dx = \sin(x)"
        )

    if "exp" in expr_str:

        steps.append(
            r"\int e^x dx = e^x"
        )

    steps.append(
        rf"\text{{Final integral: }} {result_latex} + C"
    )

    return steps

# =========================================================
# SIMPLIFY STEPS
# =========================================================

def simplify_steps(expr, result):

    steps = []

    expr_latex = latex(expr)
    result_latex = latex(result)

    steps.append(
        rf"{expr_latex}"
    )

    if simplify(expr) == 1:

        steps.append(
            r"\sin^2(x)+\cos^2(x)=1"
        )

    factored = factor(expr)

    if factored != expr:

        steps.append(
            r"\text{Factorized form:}"
        )

        steps.append(
            rf"{latex(factored)}"
        )

    steps.append(
        rf"\text{{Final simplified form: }} {result_latex}"
    )

    return steps

# =========================================================
# GRAPH
# =========================================================

def generate_graph(expr):

    try:

        expr_str = str(expr)

        if "log" in expr_str or "ln" in expr_str:

            x_vals = np.linspace(
                0.1,
                10,
                2000
            )

        elif "tan" in expr_str:

            x_vals = np.linspace(
                -2 * np.pi,
                2 * np.pi,
                4000
            )

        else:

            x_vals = np.linspace(
                -10,
                10,
                2000
            )

        func = lambdify(
            x,
            expr,
            "numpy"
        )

        y_vals = func(x_vals)

        cleaned_x = []
        cleaned_y = []

        for xv, yv in zip(x_vals, y_vals):

            try:

                if not np.isfinite(yv):

                    cleaned_x.append(float(xv))
                    cleaned_y.append(None)

                    continue

                if abs(yv) > 100:

                    cleaned_x.append(float(xv))
                    cleaned_y.append(None)

                    continue

                cleaned_x.append(float(xv))
                cleaned_y.append(float(yv))

            except:

                cleaned_x.append(float(xv))
                cleaned_y.append(None)

        return {

            "x": cleaned_x,
            "y": cleaned_y
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

    problem = data.get(
        "problem",
        ""
    )

    try:

        # =====================================================
        # IMPORTANT NORMALIZATION
        # =====================================================

        normalized_problem = normalize_nlp_math(problem)

        lower = normalized_problem.lower()

        # =====================================================
        # EQUATION SOLVER
        # =====================================================

        if "=" in lower:

            expr_text = clean_expression_text(
                normalized_problem
            )

            print("SOLVE EXPR =", expr_text)

            left, right = expr_text.split("=")

            lhs = parse_expr(
                left,
                transformations=transformations,
                local_dict=local_dict
            )

            rhs = parse_expr(
                right,
                transformations=transformations,
                local_dict=local_dict
            )

            eq = Eq(lhs, rhs)

            solutions = sp.solve(eq, x)

            solution_text = ", ".join(
                [str(s) for s in solutions]
            )

            return jsonify({

                "type": "equation",

                "result": solution_text,

                "latex": ", ".join(
                    [latex(s) for s in solutions]
                ),

                "steps": solve_equation_steps(
                    eq,
                    solutions
                ),

                "graph": None
            })

        # =====================================================
        # GRAPH
        # =====================================================

        elif (
                "plot" in lower or
                "graph" in lower or
                "draw" in lower or
                "visualize" in lower
        ):

            expr_text = clean_expression_text(
                normalized_problem
            )

            print("\n========================")
            print("GRAPH REQUEST")
            print("========================")

            print("ORIGINAL =", problem)
            print("CLEANED =", expr_text)

            expr = parse_expr(
                expr_text,
                transformations=transformations,
                local_dict=local_dict
            )

            print("PARSED =", expr)

            graph_data = generate_graph(expr)

            return jsonify({

                "type": "graph",

                "result": f"Graph of {expr_text}",

                "latex": latex(expr),

                "graph": graph_data,

                "steps": [
                    rf"\text{{Plotting function }} {latex(expr)}"
                ]
            })

        # =====================================================
        # DERIVATIVE
        # =====================================================

        elif (
                "derivative" in lower or
                "differentiate" in lower
        ):

            expr_text = clean_expression_text(
                normalized_problem
            )

            print("DERIVATIVE EXPR =", expr_text)

            expr = parse_expr(
                expr_text,
                transformations=transformations,
                local_dict=local_dict
            )

            result = diff(expr, x)

            return jsonify({

                "type": "derivative",

                "result": str(result),

                "latex": latex(result),

                "steps": derivative_steps(
                    expr,
                    result
                ),

                "graph": None
            })

        # =====================================================
        # INTEGRAL
        # =====================================================

        elif (
                "integral" in lower or
                "integrate" in lower
        ):

            expr_text = clean_expression_text(
                normalized_problem
            )

            print("INTEGRAL EXPR =", expr_text)

            expr = parse_expr(
                expr_text,
                transformations=transformations,
                local_dict=local_dict
            )

            result = integrate(expr, x)

            return jsonify({

                "type": "integral",

                "result": str(result),

                "latex": latex(result),

                "steps": integral_steps(
                    expr,
                    result
                ),

                "graph": None
            })

        # =====================================================
        # SIMPLIFY
        # =====================================================

        else:

            expr_text = clean_expression_text(
                normalized_problem
            )

            print("SIMPLIFY EXPR =", expr_text)

            expr = parse_expr(
                expr_text,
                transformations=transformations,
                local_dict=local_dict
            )

            simplified = simplify(expr)

            return jsonify({

                "type": "expression",

                "result": str(simplified),

                "latex": latex(simplified),

                "steps": simplify_steps(
                    expr,
                    simplified
                ),

                "graph": None
            })

    except Exception as e:

        print("\nERROR:", e)

        return jsonify({

            "type": "error",

            "result": "SymPy engine failed.",

            "latex": "",

            "steps": [str(e)],

            "graph": None
        })

# =========================================================
# RUN
# =========================================================

if __name__ == '__main__':

    app.run(
        port=5000,
        debug=True
    )