package com.university.textanalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class MultiThreadedAnalyzer {

    private final TextAnalyzer singleThreadedAnalyzer; //code will not compile without it
    
    // ExecutorService manages the thread pool safely - prevents thread leaks
    // and ensures proper thread lifecycle management, execute tasks asynchronously
    private final ExecutorService executorService;
    
    private final int numberOfThreads;
    public MultiThreadedAnalyzer(int numberOfThreads) {
       
        this.singleThreadedAnalyzer = new TextAnalyzer();
        this.numberOfThreads = numberOfThreads;
        
        // Fixed thread pool prevents unlimited thread creation
        // which could cause resource exhaustion
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    /**
      Default constructor adapts to CPU cores
     */
    public MultiThreadedAnalyzer() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
        Core multi-threaded analysis method
     * Analyzes multiple files concurrently with proper synchronization
     */
    public List<TextAnalyzer.AnalysisResult> analyzeFilesParallel( //track progress for each file , overall progress
            List<File> files, 
            GlobalProgressListener listener) {
        
        //  Handle edge cases for debugging
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        // Thread-safe collections for storing futures and results
        List<Future<TextAnalyzer.AnalysisResult>> futures = new ArrayList<>(); // furure represents the resulr of an asynchronous computation that will be available later, tasks run in background results retrieved later error handled saftely
        List<TextAnalyzer.AnalysisResult> results = new ArrayList<>();

        // submit all analysis tasks - works with ANY number of files
        for (int i = 0; i < files.size(); i++) {
            final File file = files.get(i);
            final int fileIndex = i;
            
            // Each task runs in isolation - no shared mutable state
            // Tracks start time for performance debugging
            Future<TextAnalyzer.AnalysisResult> future = executorService.submit(() -> {  // callable
                //  Debug tracking - when file analysis starts
                if (listener != null) {
                    listener.onFileStarted(fileIndex, file.getName());
                }

                //  Each thread gets its own ProgressListener
                // No shared state between threads - thread safe
                TextAnalyzer.ProgressListener fileProgressListener = new TextAnalyzer.ProgressListener() {
                    @Override
                    public void onProgressUpdate(double progress) {
                        //  Real-time progress tracking for debugging
                        if (listener != null) {
                            listener.onFileProgress(fileIndex, file.getName(), progress); //notify the ui
                        }
                    }

                    @Override
                    public void onComplete(TextAnalyzer.AnalysisResult result) {
                        // Success tracking for debugging
                        if (listener != null) {
                            listener.onFileCompleted(fileIndex, file.getName(), result);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        //  Error tracking for debugging concurrency issues
                        if (listener != null) {
                            listener.onFileError(fileIndex, file.getName(), errorMessage);
                        }
                    }
                };

                //  Thread-safe method call - each file analyzed independently and progress is reported
                return singleThreadedAnalyzer.analyzeFile(file, fileProgressListener);
            });

            futures.add(future);
        }

        // Collect results with proper synchronization
        
        for (int i = 0; i < futures.size(); i++) {
            try {
                //.get() block the ui thred until result is ready - thread-safe retrieval
                TextAnalyzer.AnalysisResult result = futures.get(i).get();
                results.add(result);
                
                //  Track overall progress for performance monitoring
                if (listener != null) {
                    double overallProgress = (i + 1) / (double) files.size();
                    listener.onOverallProgress(overallProgress, i + 1, files.size());
                }
            } catch (InterruptedException | ExecutionException e) {
                //  Debug concurrency errors - captures thread exceptions
                String fileName = files.get(i).getName();
                results.add(new TextAnalyzer.AnalysisResult(
                    fileName, 
                    "Error during concurrent analysis: " + e.getMessage()
                ));
                
                // Report errors for debugging
                if (listener != null) {
                    listener.onFileError(i, fileName, e.getMessage());
                }
            }
        }

        //  Final completion tracking for performance measurement
        if (listener != null) {
            listener.onAllFilesCompleted(results);
        }

        return results;
    }

    /**
     *  Thread-safe single file analysis using thread pool
     * Returns Future for non-blocking operation
     */
    // public Future<TextAnalyzer.AnalysisResult> analyzeFileAsync(
    //         File file, 
    //         TextAnalyzer.ProgressListener listener) {
        
    //     return executorService.submit(() -> 
    //         singleThreadedAnalyzer.analyzeFile(file, listener)
    //     );
    // }

    /**
     *  Proper resource cleanup - prevents thread leaks
     * ensures all threads terminate properly
     */
    public void shutdown() {
        // Graceful shutdown - allows running tasks to complete
        executorService.shutdown();
        try {
            // Wait up to 60 seconds for tasks to finish
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // Force shutdown if tasks don't complete in time
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Force shutdown on interruption and restore interrupt status
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     *Emergency shutdown 
     */
    public void shutdownNow() {
        executorService.shutdownNow();
    }

    /**
     *  Query thread pool size 
     */
    // public int getNumberOfThreads() {
    //     return numberOfThreads;
    // }

    /**
     *  Check if executor is shut 
     */
    // public boolean isShutdown() {
    //     return executorService.isShutdown();
    // }

    /**
     * Global progress listener for debugging and monitoring
     * Provides detailed callbacks for tracking concurrent execution
     */
    public interface GlobalProgressListener {
        /**
         * Debug - when each file starts processing
         */
        void onFileStarted(int fileIndex, String fileName);

        /**
          Debug - track progress of individual files
         */
        void onFileProgress(int fileIndex, String fileName, double progress);

        /**
         *  Debug - when file completes successfully
         */
        void onFileCompleted(int fileIndex, String fileName, TextAnalyzer.AnalysisResult result);

        /**
         *  Debug - track errors in concurrent execution
         */
        void onFileError(int fileIndex, String fileName, String errorMessage);

        /**
         * Debug - overall performance monitoring
         */
        void onOverallProgress(double progress, int completedFiles, int totalFiles);

        /**
         *Debug - final statistics for performance analysis
         */
        void onAllFilesCompleted(List<TextAnalyzer.AnalysisResult> results);
    }

    // public static abstract class GlobalProgressAdapter implements GlobalProgressListener {
    //     @Override
    //     public void onFileStarted(int fileIndex, String fileName) {}

    //     @Override
    //     public void onFileProgress(int fileIndex, String fileName, double progress) {}

    //     @Override
    //     public void onFileCompleted(int fileIndex, String fileName, TextAnalyzer.AnalysisResult result) {}

    //     @Override
    //     public void onFileError(int fileIndex, String fileName, String errorMessage) {}

    //     @Override
    //     public void onOverallProgress(double progress, int completedFiles, int totalFiles) {}

    //     @Override
    //     public void onAllFilesCompleted(List<TextAnalyzer.AnalysisResult> results) {}
    // }
}