package classes.Pipeline;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class DescriptiveStatistics {

    private final BufferedImage image;
    private final int width;
    private final int height;

    private final Integer[][] grayValueMatrix;
    private Integer[][] coOccurrenceMatrix;
    private Double[] relativeFrequency;
    private Double[] relativeCumulativeFrequency;

    private int median;
    private double mean;
    private double variance;
    private double standardDeviation;
    private double entropy;

    /**
     * Values calculated later get set to -1 to allow for checks
     * @param image the image to perform the statistical calculations on
     */
    public DescriptiveStatistics(BufferedImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();

        this.grayValueMatrix = new Integer[this.width][this.height];
        for(int x = 0; x < this.width; x++){
            for(int y = 0; y < this.height; y++){
                this.grayValueMatrix[x][y] = -1;
            }
        }

        this.median = -1;
        this.mean = -1;
        this.variance = -1;
        this.standardDeviation = -1;
    }

    /**
     * calls every function to calculates all statistics
     * {@link #calculateGrayValueMatrix()}
     * {@link #calculateCoOccurrenceMatrix()}
     * {@link #calculateMean()}
     * {@link #calculateMedian()}
     * {@link #calculateVariance()}
     * {@link #calculateStandardDeviation()}
     * {@link #calculateEntropy()}
     * {@link #calculateRelativeFrequencyArray()}
     * {@link #calculateRelativeCumulativeFrequencyArray()}
     */
    public void calculateAllStatistics() {
        this.calculateGrayValueMatrix();
        this.calculateCoOccurrenceMatrix();
        this.calculateMean();
        this.calculateMedian();
        this.calculateVariance();
        this.calculateStandardDeviation();
        this.calculateEntropy();
        this.calculateRelativeFrequencyArray();
        this.calculateRelativeCumulativeFrequencyArray();
    }

    /**
     * {@code @Link} #timestep(String, Runnable)
     */
    void timeCalculationOfAllStatistics() {
        System.out.println("\n--- Starting Statistics classes.Pipeline ---\n");

        timeStep("Gray Value Matrix",     this::calculateGrayValueMatrix);
        timeStep("Co-occurrence Matrix",  this::calculateCoOccurrenceMatrix);
        timeStep("Mean",                  this::calculateMean);
        timeStep("Median",                this::calculateMedian);
        timeStep("Variance",              this::calculateVariance);
        timeStep("Standard Deviation",    this::calculateStandardDeviation);
        timeStep("Entropy",               this::calculateEntropy);
        timeStep("Relative Frequency",    this::calculateRelativeFrequencyArray);
        timeStep("Relative Cumulative Frequency",this::calculateRelativeCumulativeFrequencyArray);

        System.out.println("--- All Calculations Complete ---\n");
    }

    private void timeStep(String name, Runnable calculation) {
        System.out.println("Beginning to calculate " + name.toLowerCase() + "...");

        long start = System.nanoTime();
        calculation.run();
        long end = System.nanoTime();

        double ms = (end - start) / 1_000_000.0;
        System.out.printf("Finished calculating %s! (Time: %.4f ms)%n%n", name.toLowerCase(), ms);
    }

    /**
     * Calculates the gray-value-matrix from a BufferedImage.
     * Extracts rgb-values and calculates gray-value for each pixel.
     */
    public void calculateGrayValueMatrix() {

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int rgb = this.image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int gray = (int) Math.floor(0.299 * r + 0.587 * g + 0.114 * b);
                this.grayValueMatrix[x][y] = gray;
            }
        }
    }


    /**
     * Overload for specific number of gray values. {@link  #calcCoOcMat(int) } for calculation
     * @param numGrays specific number of gray values
     */
    void calculateCoOccurrenceMatrix(int numGrays){
        calcCoOcMat(numGrays);
    }

    /**
     * Overload for standard number of gray values.
     * {@link #calcCoOcMat(int) } for calculation
     */
    void calculateCoOccurrenceMatrix(){
        calcCoOcMat(256);
    }

    /**
     * Calculates the co-occurrence-matrix from a given gray-value-matrix.
     * Traverses gray-value-matrix and increments corresponding co-occurrence (right neighbour).
     * @param numGrays number of different gray values
     */
    void calcCoOcMat(int numGrays){
        int grayMatrixRows = this.grayValueMatrix.length;
        int grayMatrixCols = this.grayValueMatrix[0].length;

        this.coOccurrenceMatrix = new Integer[numGrays][numGrays];

        for(int rows = 0; rows < numGrays; rows++){
            for(int cols = 0; cols < numGrays; cols++){
                this.coOccurrenceMatrix[rows][cols] = 0;
            }
        }

        for(int rows = 0; rows < grayMatrixRows; rows++){
            for(int cols = 0; cols < grayMatrixCols-1; cols++){

                int grayValueCurrent = this.grayValueMatrix[rows][cols];
                int grayValueNeighbour = this.grayValueMatrix[rows][cols+1];

                this.coOccurrenceMatrix[grayValueCurrent][grayValueNeighbour]++;
            }
        }
    }

    /**
     * Calculates median of gray-value-array.
     * Sorts array with {@link #countingSort()} before determining median.
     */
    void calculateMedian() {
        if (this.grayValueMatrix[0][0] == -1){
            this.calculateGrayValueMatrix();
        }
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        int middleElement = (grayValueArray.length / 2);

        // sorting
        Integer[] sortedGrayValues = this.countingSort();

        int count = 0;
        for (int i=0; i < sortedGrayValues.length; i++){
            count += sortedGrayValues[i];
            if (count >= middleElement){
                this.median = i;
                break;
            }
        }
    }

    public void calculateMean() {
        if (this.grayValueMatrix[0][0] == -1){
            this.calculateGrayValueMatrix();
        }
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        double sum = 0;

        for (Integer integer : grayValueArray) {
            sum += integer;
        }

        this.mean = sum / grayValueArray.length;
    }

    /**
     * Calculates the variance of an array.
     * Uses {@link #calculateMean()} for mean^2
     */
    public void calculateVariance() {
        if (this.grayValueMatrix[0][0] == -1){
            this.calculateGrayValueMatrix();
        }
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        if (this.mean == -1) {
            this.calculateMean();
        }
        double mean2 = Math.pow(this.mean, 2);
        double sum = 0;

        for (Integer integer : grayValueArray) {
            sum += (int) Math.pow(integer, 2) - mean2;
        }

        this.variance = sum / grayValueArray.length;
    }

    /**
     * Calculates standard deviation through variance.
     * Calls {@link #calculateVariance()} if variance is not yet calculated.
     */
    void calculateStandardDeviation() {
        if (this.variance == -1.0) {
            this.calculateVariance();
        }

        this.standardDeviation = Math.sqrt(this.variance);
    }

    /**
     * Calculates the entropy of an array.
     * Uses {@link #countingSort()} to sort the array.
     * Checks if probability is zero before adding.
     */
    public void calculateEntropy() {
        if (this.grayValueMatrix[0][0] == -1){
            this.calculateGrayValueMatrix();
        }
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        Integer[] amounts = this.countingSort();
        double entropy = 0.0;

        for (int i = 0; i < 256; i++) {
            double prob = (double) amounts[i] / grayValueArray.length;
            if (prob > 0){
                entropy -= prob * log2(prob);
            }
        }

        this.entropy = entropy;
    }

    /**
     * Calculates a relative frequency array from an array.
     * Uses {@link #countingSort()} to sort the array
     */
    void calculateRelativeFrequencyArray() {
        if (this.grayValueMatrix[0][0] == -1){
            this.calculateGrayValueMatrix();
        }
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        Integer[] amounts = this.countingSort();
        Double[] amountsD = new Double[256];

        for (int i = 0; i < amounts.length; i++) {
            amountsD[i] = (double) amounts[i] / grayValueArray.length;
        }

        this.relativeFrequency = amountsD;
    }

    public void calculateRelativeCumulativeFrequencyArray() {
        if (this.grayValueMatrix[0][0] == -1){
            this.calculateGrayValueMatrix();
        }
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        Integer[] amounts = this.countingSort();
        Double[] amountsD = new Double[256];

        for (int i = 0; i < amounts.length; i++) {
            amountsD[i] = (double) amounts[i] / grayValueArray.length;
            if (i > 0){
                amountsD[i] += amountsD[i-1];
            }
        }

        this.relativeCumulativeFrequency = amountsD;
    }




    // Helper functions

    /**
     * Helper function to convert matrices into arrays.
     * @param matrix the matrix to convert to array
     * @return Integer[] array from matrix
     */
    Integer[] matrixToArray(Integer[][] matrix) {
        int rows =  matrix.length;
        int cols = matrix[0].length;

        Integer[] array = new Integer[rows*cols];

        int counter = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                array[counter] = matrix[row][col];
                counter++;
            }
        }

        return array;
    }

    /**
     * Helper function that performs counting sort algorithm on gray-value-matrix
     * @return Integer[] sorted array
     */
    Integer[] countingSort() {
        Integer[] grayValueArray = matrixToArray(this.grayValueMatrix);
        Integer[] amounts = new Integer[256];

        Arrays.fill(amounts, 0);

        for (Integer integer : grayValueArray) {
            amounts[integer]++;
        }

        return amounts;
    }

    double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * Prints all statistics. If not calculated prints -1.
     * Converts relative frequency array to histogram and removes bars with length 0.
     */
    void printStatistics() {
        if (this.grayValueMatrix[0][0] == -1 || this.mean == -1 || this.median == -1 || this.variance == -1 || this.standardDeviation == -1 || this.entropy == -1) {
            this.calculateAllStatistics();
        }
        System.out.println("Median: " + this.getMedian());
        System.out.printf("Mean: %.2f\n", this.getMean());
        System.out.printf("Variance: %.2f\n", this.getVariance());
        System.out.printf("Standard Deviation: %.2f\n", this.getStandardDeviation());
        System.out.printf("Entropy: %.2f\n", this.getEntropy());

        // relative frequency
        Double[] freq = this.getRelativeFrequency();
        double maxFreq = Arrays.stream(freq).max(Double::compare).orElse(1.0);
        int screenWidth = 40;

        System.out.println("Index | Frequency Histogram");
        System.out.println("---------------------------");
        this.printHistogram(freq, maxFreq, screenWidth);

        // relative cumulative frequency
        freq = this.getRelativeCumulativeFrequency();
        maxFreq = Arrays.stream(freq).max(Double::compare).orElse(1.0);
        System.out.println("Index | Cumulative Frequency Histogram");
        System.out.println("---------------------------");
        this.printHistogram(freq, maxFreq, screenWidth);
    }

    void printHistogram(Double[] freq, double maxFreq, int screenWidth) {
        for (int i = 0; i < freq.length; i++) {
            int barLength = (int) ((freq[i] / maxFreq) * screenWidth);
            if (barLength > 0) {
                String bar = "█".repeat(barLength);
                System.out.printf("[%3d] | %s (%.4f)%n", i, bar, freq[i]);
            }
        }
    }




    // Getter

    public BufferedImage getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Integer[][] getGrayValueMatrix() {
        return grayValueMatrix;
    }

    public Integer[][] getCoOccurrenceMatrix() {
        return coOccurrenceMatrix;
    }

    public Double[] getRelativeFrequency() {
        return relativeFrequency;
    }

    public Double[] getRelativeCumulativeFrequency() {
        return relativeCumulativeFrequency;
    }

    public int getMedian() {
        return median;
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return variance;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getEntropy() {
        return entropy;
    }
}
