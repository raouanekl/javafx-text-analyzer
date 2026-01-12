package com.university.textanalyzer;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;

/**
 * Multi-Threaded Text Analyzer Desktop Application
 * Sprint 1: User Interface Development
 * Sprint 2: Single-Threaded Analyzer
 * Sprint 3: Multi-Threaded Analyzer
 * University Abdelhamid Mehri - Constantine 2
 * team members: kihal raouane, boudour hanine
 
 */
public class App extends Application {

    // Observable list to store loaded files
    private ObservableList<FileInfo> fileList = FXCollections.observableArrayList();

    
    private TextAnalyzer analyzer = new TextAnalyzer();
    
    private MultiThreadedAnalyzer multiThreadedAnalyzer;

    // UI Components
    private TableView<FileInfo> fileTable;
    private TextArea detailsArea;
    private Label statusLabel;
    private ProgressBar progressBar;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Multi-Threaded Text Analyzer");

        // QUESTION 1: Initialize multi-threaded analyzer with default CPU cores
        multiThreadedAnalyzer = new MultiThreadedAnalyzer();

        // Create main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        VBox topSection = createTopSection();
        mainLayout.setTop(topSection);

        VBox centerSection = createCenterSection();
        mainLayout.setCenter(centerSection);

        VBox rightSection = createRightSection();
        mainLayout.setRight(rightSection);

        VBox bottomSection = createBottomSection(primaryStage);
        mainLayout.setBottom(bottomSection);

        Scene scene = new Scene(mainLayout, 1000, 600);
        primaryStage.setScene(scene);
      
        primaryStage.setOnCloseRequest(e -> {
            multiThreadedAnalyzer.shutdown();
        });
        
        primaryStage.show();
    }

    private VBox createTopSection() {
        VBox topBox = new VBox(5);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label("Text File Analyzer");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label descLabel = new Label("Load text files for concurrent analysis");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        topBox.getChildren().addAll(titleLabel, descLabel);
        return topBox;
    }

    private VBox createCenterSection() {
        VBox centerBox = new VBox(5);

        Label tableLabel = new Label("Loaded Files:");
        tableLabel.setStyle("-fx-font-weight: bold;");

        fileTable = new TableView<>();
        fileTable.setItems(fileList);
        fileTable.setPlaceholder(new Label("No files loaded. Click 'Load Files' to begin."));

        TableColumn<FileInfo, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setPrefWidth(250);

        TableColumn<FileInfo, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
        pathCol.setPrefWidth(300);

        TableColumn<FileInfo, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        sizeCol.setPrefWidth(100);

        TableColumn<FileInfo, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        
        TableColumn<FileInfo, ProgressBar> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(new PropertyValueFactory<>("progressBar"));
        progressCol.setPrefWidth(150);

        fileTable.getColumns().addAll(nameCol, pathCol, sizeCol, statusCol, progressCol);

        fileTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        displayFileDetails(newSelection);
                    }
                });

        centerBox.getChildren().addAll(tableLabel, fileTable);
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        return centerBox;
    }

    private VBox createRightSection() {
        VBox rightBox = new VBox(5);
        rightBox.setPadding(new Insets(0, 0, 0, 10));
        rightBox.setPrefWidth(250);

        Label detailsLabel = new Label("File Details:");
        detailsLabel.setStyle("-fx-font-weight: bold;");

        detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setPromptText("Select a file to view details");
        detailsArea.setStyle("-fx-control-inner-background: #f4f4f4;");

        rightBox.getChildren().addAll(detailsLabel, detailsArea);
        VBox.setVgrow(detailsArea, Priority.ALWAYS);

        return rightBox;
    }

    private VBox createBottomSection(Stage stage) {
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_LEFT);

        Button loadButton = new Button("Load Files");
        loadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        loadButton.setPrefWidth(120);
        loadButton.setOnAction(e -> handleLoadFiles(stage));

        Button removeButton = new Button("Remove Selected");
        removeButton.setPrefWidth(120);
        removeButton.setOnAction(e -> handleRemoveFile());

        Button clearButton = new Button("Clear All");
        clearButton.setPrefWidth(120);
        clearButton.setOnAction(e -> handleClearAll());

        Button analyzeButton = new Button("Analyze Files");
        analyzeButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        analyzeButton.setPrefWidth(120);
        analyzeButton.setOnAction(e -> handleAnalyze());
          //new button
        Button analyzeAllButton = new Button("Analyze All");
        analyzeAllButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        analyzeAllButton.setPrefWidth(120);
        analyzeAllButton.setOnAction(e -> handleAnalyzeAll());

        buttonPanel.getChildren().addAll(loadButton, removeButton, clearButton, analyzeButton, analyzeAllButton);


        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-padding: 5px; -fx-background-color: #e0e0e0; -fx-border-color: #cccccc;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        bottomBox.getChildren().addAll(buttonPanel, progressBar, statusLabel);

        return bottomBox;
    }

    
    private void handleLoadFiles(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Text Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

       
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                boolean exists = fileList.stream()
                        .anyMatch(f -> f.getFilePath().equals(file.getAbsolutePath()));

                if (!exists) {
                    FileInfo fileInfo = new FileInfo(
                            file.getName(),
                            file.getAbsolutePath(),
                            formatFileSize(file.length()),
                            "Loaded");
                    fileList.add(fileInfo);
                }
            }
            // QUESTION 3: Status update for debugging
            updateStatus("Loaded " + selectedFiles.size() + " file(s). Total files: " + fileList.size());
        }
    }

    private void handleRemoveFile() {
        FileInfo selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fileList.remove(selected);
            detailsArea.clear();
            updateStatus("Removed: " + selected.getFileName());
        } else {
            showAlert("No Selection", "Please select a file to remove.");
        }
    }

    private void handleClearAll() {
        if (!fileList.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear All Files");
            confirm.setHeaderText("Are you sure?");
            confirm.setContentText("This will remove all loaded files from the list.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    fileList.clear();
                    detailsArea.clear();
                    updateStatus("All files cleared.");
                }
            });
        }
    }

    
    private void handleAnalyze() {
        if (fileList.isEmpty()) {
            showAlert("No Files", "Please load files before starting analysis.");
            return;
        }

        FileInfo selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a file to analyze.");
            return;
        }

        analyzeFileAsync(selected);
    }

    
    private void handleAnalyzeAll() {
        if (fileList.isEmpty()) {
            showAlert("No Files", "Please load files before starting analysis.");
            return;
        }

        
        for (FileInfo fileInfo : fileList) {
            fileInfo.setStatus("Pending");
            fileInfo.getProgressBar().setProgress(0);
        }

        analyzeAllFilesAsync();
    }

    
    private void analyzeFileAsync(FileInfo fileInfo) {
        Task<TextAnalyzer.AnalysisResult> task = new Task<TextAnalyzer.AnalysisResult>() {
            @Override
            protected TextAnalyzer.AnalysisResult call() throws Exception {
                File file = new File(fileInfo.getFilePath());

                TextAnalyzer.ProgressListener listener = new TextAnalyzer.ProgressListener() {
                    @Override
                    public void onProgressUpdate(double progress) {
                        updateProgress(progress, 1.0);
                        // QUESTION 1: Platform.runLater ensures thread-safe UI updates
                        Platform.runLater(() -> {
                            fileInfo.getProgressBar().setProgress(progress);
                        });
                    }

                    @Override
                    public void onComplete(TextAnalyzer.AnalysisResult result) {
                    }

                    @Override
                    public void onError(String errorMessage) {
                    }
                };

                return analyzer.analyzeFile(file, listener);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setVisible(true);
        fileInfo.setStatus("Analyzing...");
        updateStatus("Analyzing: " + fileInfo.getFileName());

        task.setOnSucceeded(event -> {
            TextAnalyzer.AnalysisResult result = task.getValue();
            progressBar.setVisible(false);

            if (result.isSuccess()) {
                fileInfo.setStatus("Completed");
                fileInfo.setAnalysisResult(result);
                displayFileDetails(fileInfo);
                updateStatus("Analysis completed: " + fileInfo.getFileName());
            }
        });

        task.setOnFailed(event -> {
            progressBar.setVisible(false);
            fileInfo.setStatus("Error");
            fileInfo.getProgressBar().setProgress(0);
            showAlert("Analysis Failed", "Failed to analyze file");
            updateStatus("Failed: " + fileInfo.getFileName());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * ========================================================================
     *  SOLUTION: Multi-threaded file analysis
     * ========================================================================
     *  Thread-safe UI updates using Platform.runLater
     * Processes multiple files concurrently
     * Detailed progress tracking and error reporting
     */
    private void analyzeAllFilesAsync() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // QUESTION 2: Convert all loaded files to File objects
                List<File> files = fileList.stream()
                        .map(fi -> new File(fi.getFilePath()))
                        .collect(Collectors.toList());

                // QUESTION 3: GlobalProgressListener for debugging concurrency
                // Tracks: start, progress, completion, errors for each file
                MultiThreadedAnalyzer.GlobalProgressListener listener = 
                    new MultiThreadedAnalyzer.GlobalProgressListener() {
                    
                    @Override
                    public void onFileStarted(int fileIndex, String fileName) {
                        //  when each file starts
                        // Platform.runLater for thread-safe UI update
                        Platform.runLater(() -> {
                            fileList.get(fileIndex).setStatus("Analyzing...");
                        });
                    }

                    @Override
                    public void onFileProgress(int fileIndex, String fileName, double progress) {
                        // Real-time progress monitoring per file
                        // Thread-safe UI update
                        Platform.runLater(() -> {
                            fileList.get(fileIndex).getProgressBar().setProgress(progress);
                        });
                    }

                    @Override
                    public void onFileCompleted(int fileIndex, String fileName, 
                            TextAnalyzer.AnalysisResult result) {
                        // Track successful completions
                        // Thread-safe result storage
                        Platform.runLater(() -> {
                            FileInfo fileInfo = fileList.get(fileIndex);
                            fileInfo.setStatus("Completed");
                            fileInfo.setAnalysisResult(result);
                        });
                    }

                    @Override
                    public void onFileError(int fileIndex, String fileName, String errorMessage) {
                        //  Debug - capture and display errors
                        //  Thread-safe error handling
                        Platform.runLater(() -> {
                            fileList.get(fileIndex).setStatus("Error");
                            fileList.get(fileIndex).getProgressBar().setProgress(0);
                        });
                    }

                    @Override
                    public void onOverallProgress(double progress, int completedFiles, int totalFiles) {
                        //  Overall performance monitoring
                        // Shows: "Analyzing files: 3/10" for debugging
                        updateProgress(progress, 1.0);
                        Platform.runLater(() -> {
                            updateStatus("Analyzing files: " + completedFiles + "/" + totalFiles);
                        });
                    }

                    @Override
                    public void onAllFilesCompleted(List<TextAnalyzer.AnalysisResult> results) {
                        // Final statistics for performance analysis
                        // Shows success rate: "Completed: 8 of 10 files"
                        Platform.runLater(() -> {
                            long successCount = results.stream()
                                    .filter(TextAnalyzer.AnalysisResult::isSuccess)
                                    .count();
                            updateStatus("Completed: " + successCount + " of " + 
                                    results.size() + " files analyzed successfully");
                        });
                    }
                };

                //  Execute multi-threaded analysis
                multiThreadedAnalyzer.analyzeFilesParallel(files, listener);
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setVisible(true);
        updateStatus("Starting multi-threaded analysis...");

        task.setOnSucceeded(event -> {
            progressBar.setVisible(false);
            FileInfo selected = fileTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                displayFileDetails(selected);
            }
        });

        //  Error handling for debugging failures
        task.setOnFailed(event -> {
            progressBar.setVisible(false);
            showAlert("Analysis Failed", "Failed to analyze files: " + task.getException().getMessage());
            updateStatus("Analysis failed");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * QUESTION 3: Display detailed analysis results for debugging
     */
    private void displayFileDetails(FileInfo fileInfo) {
        File file = new File(fileInfo.getFilePath());

        StringBuilder details = new StringBuilder();
        details.append("File Name:\n").append(fileInfo.getFileName()).append("\n\n");
        details.append("Full Path:\n").append(fileInfo.getFilePath()).append("\n\n");
        details.append("Size:\n").append(fileInfo.getFileSize()).append("\n\n");
        details.append("Status:\n").append(fileInfo.getStatus()).append("\n\n");
        details.append("Readable:\n").append(file.canRead() ? "Yes" : "No").append("\n\n");
        details.append("Last Modified:\n").append(
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(file.lastModified())));

        if (fileInfo.getAnalysisResult() != null) {
            TextAnalyzer.AnalysisResult result = fileInfo.getAnalysisResult();
            details.append("\n\n=== ANALYSIS RESULTS ===\n\n");
            details.append("Total Words: ").append(result.getTotalWords()).append("\n");
            details.append("Unique Words: ").append(result.getUniqueWords()).append("\n");
            details.append("Total Characters: ").append(result.getTotalCharacters()).append("\n");
            details.append("Total Lines: ").append(result.getTotalLines()).append("\n");
            details.append("Sentiment: ").append(result.getSentiment()).append("\n");

            details.append("\n=== TOP 10 FREQUENT WORDS ===\n\n");
            int rank = 1;
            for (TextAnalyzer.WordFrequency wf : result.getTopFrequentWords()) {
                details.append(rank++).append(". ")
                        .append(wf.getWord())
                        .append(" (").append(wf.getFrequency()).append(")\n");
            }
        }

        detailsArea.setText(details.toString());
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class FileInfo {
        private String fileName;
        private String filePath;
        private String fileSize;
        private String status;
        private ProgressBar progressBar;
        private TextAnalyzer.AnalysisResult analysisResult;

        public FileInfo(String fileName, String filePath, String fileSize, String status) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.status = status;
            this.progressBar = new ProgressBar(0);
            this.progressBar.setPrefWidth(140);
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public ProgressBar getProgressBar() { return progressBar; }
        public void setProgressBar(ProgressBar progressBar) { this.progressBar = progressBar; }
        public TextAnalyzer.AnalysisResult getAnalysisResult() { return analysisResult; }
        public void setAnalysisResult(TextAnalyzer.AnalysisResult analysisResult) { 
            this.analysisResult = analysisResult; 
        }
    }
}