package com.axiomai.service;

import java.util.List;
import java.util.Map;

public class Memory {

    // =====================================================
    // BASIC MEMORY
    // =====================================================

    public static String lastAnswer = null;

    public static String lastBreakdown = null;

    public static String lastIntent = "";

    public static String lastQuestion = null;

    // =====================================================
    // SYMBOLIC MEMORY
    // =====================================================

    public static String lastLatex = null;

    public static List<String> lastSteps = null;

    public static String lastType = null;

    public static Map<String, Object> lastGraph = null;

    // =====================================================
    // QUIZ MEMORY
    // =====================================================

    public static boolean quizActive = false;

    public static double quizCorrectAnswer = 0;

    public static boolean awaitingQuizConfirmation = false;

    // =====================================================
    // CLEAR SYMBOLIC MEMORY
    // =====================================================

    public static void clearSymbolicMemory() {

        lastLatex = null;

        lastSteps = null;

        lastType = null;

        lastGraph = null;
    }

    // =====================================================
    // CLEAR ALL MEMORY
    // =====================================================

    public static void clearAll() {

        lastAnswer = null;

        lastBreakdown = null;

        lastIntent = "";

        lastQuestion = null;

        clearSymbolicMemory();

        quizActive = false;

        quizCorrectAnswer = 0;

        awaitingQuizConfirmation = false;
    }
}