//package com.axiomai.math.solver;
//
//import com.axiomai.math.finance.InvestmentSolver;
//
//public class SolverFactory {
//
//    public ExpressionSolver getSolver(String intent) {
//
//        if (intent == null) {
//            return new ExpressionSolver();
//        }
//
//        switch (intent.toUpperCase()) {
//
//            case "ADVANCED_MATH":
//                return new AdvancedMathSolver();
//
//            case "INVEST_SIMPLE":
//            case "INVEST_PATTERN":
//            case "INVEST_REQUIRED_PRINCIPAL":
//            case "INVEST_YEARS":
//                return new InvestmentSolver();
//
//            default:
//                return new ExpressionSolver();
//        }
//    }
//}