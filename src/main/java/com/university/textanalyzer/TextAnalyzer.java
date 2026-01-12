package com.university.textanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Sprint 2: Single-Threaded Analyzer Implementation
 */
public class TextAnalyzer {

    public static class AnalysisResult {
        private final String fileName;
        private final int totalWords;
        private final int uniqueWords;
        private final int totalCharacters;
        private final int totalLines;
        private final List<WordFrequency> topFrequentWords;
        private final String sentiment;
        private final boolean success;
        private final String errorMessage;

        public AnalysisResult(String fileName, int totalWords, int uniqueWords,
                int totalCharacters, int totalLines,
                List<WordFrequency> topFrequentWords, String sentiment) {
            this.fileName = fileName;
            this.totalWords = totalWords;
            this.uniqueWords = uniqueWords;
            this.totalCharacters = totalCharacters;
            this.totalLines = totalLines;
            this.topFrequentWords = topFrequentWords;
            this.sentiment = sentiment;
            this.success = true;
            this.errorMessage = null;
        }

        // Error constructor
        public AnalysisResult(String fileName, String errorMessage) {
            this.fileName = fileName;
            this.success = false;
            this.errorMessage = errorMessage;
            this.totalWords = 0;
            this.uniqueWords = 0;
            this.totalCharacters = 0;
            this.totalLines = 0;
            this.topFrequentWords = new ArrayList<>();
            this.sentiment = "N/A";
        }

        // Getters
        public String getFileName() {
            return fileName;
        }

        public int getTotalWords() {
            return totalWords;
        }

        public int getUniqueWords() {
            return uniqueWords;
        }

        public int getTotalCharacters() {
            return totalCharacters;
        }

        public int getTotalLines() {
            return totalLines;
        }

        public List<WordFrequency> getTopFrequentWords() {
            return topFrequentWords;
        }

        public String getSentiment() {
            return sentiment;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * WordFrequency class to store word and its frequency
     */
    public static class WordFrequency {
        private final String word;
        private final int frequency;

        public WordFrequency(String word, int frequency) {
            this.word = word;
            this.frequency = frequency;
        }

        public String getWord() {
            return word;
        }

        public int getFrequency() {
            return frequency;
        }
    }

    /**
     * Progress listener interface for tracking analysis progress
     */
    public interface ProgressListener {
        void onProgressUpdate(double progress);

        void onComplete(AnalysisResult result);

        void onError(String errorMessage);
    }

    /**
     * Analyzes a text file and returns the results
     */
    public AnalysisResult analyzeFile(File file, ProgressListener listener) {
        if (file == null || !file.exists()) {
            String error = "File does not exist";
            if (listener != null)
                listener.onError(error);
            return new AnalysisResult(file != null ? file.getName() : "Unknown", error);
        }

        if (!file.canRead()) {
            String error = "File cannot be read - permission denied";
            if (listener != null)
                listener.onError(error);
            return new AnalysisResult(file.getName(), error);
        }

        try {
            // Read file content
            if (listener != null)
                listener.onProgressUpdate(0.1);
            String content = readFileContent(file);

            if (listener != null)
                listener.onProgressUpdate(0.3);

            // Count total words
            List<String> words = extractWords(content);
            int totalWords = words.size();

            if (listener != null)
                listener.onProgressUpdate(0.5);

            // Count unique words
            Set<String> uniqueWordsSet = new HashSet<>(words);
            int uniqueWords = uniqueWordsSet.size();

            if (listener != null)
                listener.onProgressUpdate(0.6);

            // Count characters and lines
            int totalCharacters = content.length();
            int totalLines = content.split("\n").length;

            if (listener != null)
                listener.onProgressUpdate(0.7);

            // Find most frequent words
            List<WordFrequency> topWords = findMostFrequentWords(words, 10);

            if (listener != null)
                listener.onProgressUpdate(0.9);

            // Perform sentiment analysis (optional)
            String sentiment = analyzeSentiment(content);

            if (listener != null)
                listener.onProgressUpdate(1.0);

            AnalysisResult result = new AnalysisResult(
                    file.getName(),
                    totalWords,
                    uniqueWords,
                    totalCharacters,
                    totalLines,
                    topWords,
                    sentiment);

            if (listener != null)
                listener.onComplete(result);
            return result;

        } catch (IOException e) {
            String error = "Error reading file: " + e.getMessage();
            if (listener != null)
                listener.onError(error);
            return new AnalysisResult(file.getName(), error);
        } catch (Exception e) {
            String error = "Unexpected error during analysis: " + e.getMessage();
            if (listener != null)
                listener.onError(error);
            return new AnalysisResult(file.getName(), error);
        }
    }

    /**
     * Reads the entire content of a file
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Extracts words from text content
     */
    private List<String> extractWords(String content) {
        // Remove punctuation and split by whitespace
        String[] tokens = content.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+");

        List<String> words = new ArrayList<>();
        for (String token : tokens) {
            if (!token.trim().isEmpty()) {
                words.add(token.trim());
            }
        }
        return words;
    }

    /**
     * Finds the most frequent words in the text
     */
    private List<WordFrequency> findMostFrequentWords(List<String> words, int topN) {
        // Count word frequencies
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String word : words) {
            frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
        }

        // Sort by frequency and get top N
        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> new WordFrequency(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Performs basic sentiment analysis on the text
     * Returns: Positive, Negative, or Neutral
     */
    private String analyzeSentiment(String content) {
        // Simple sentiment analysis based on positive and negative word counts
        String lowerContent = content.toLowerCase();

        // Common positive words
        String[] positiveWords = { "good", "great", "excellent", "amazing", "wonderful",
                "fantastic", "happy", "love", "best", "beautiful",
                "perfect", "awesome", "brilliant", "outstanding" };

        // Common negative words
        String[] negativeWords = { "bad", "terrible", "awful", "horrible", "worst",
                "hate", "sad", "poor", "disappointing", "useless",
                "wrong", "fail", "error", "problem" };

        int positiveCount = 0;
        int negativeCount = 0;

        for (String word : positiveWords) {
            positiveCount += countOccurrences(lowerContent, word);
        }

        for (String word : negativeWords) {
            negativeCount += countOccurrences(lowerContent, word);
        }

        // Determine sentiment
        if (positiveCount > negativeCount * 1.2) {
            return "Positive";
        } else if (negativeCount > positiveCount * 1.2) {
            return "Negative";
        } else {
            return "Neutral";
        }
    }

    /**
     * Counts occurrences of a word in text
     */
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }
}