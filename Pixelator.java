import java.io.File;
import java.io.IOException;

import java.awt.Color;
import java.awt.Point;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import javax.imageio.ImageIO;

import java.util.Arrays;
import java.util.Stack;
import java.util.HashSet;
import java.util.ArrayList;

public class Pixelator {

    private String inputFileName;
    private String outputFileName;
    
    private BufferedImage image;
    
    private int width;
    private int height;

    private BufferedImage cloneImage(BufferedImage rhs) {
        ColorModel cm = rhs.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = rhs.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private boolean addGrayscale(double redScale, double greenScale, double blueScale) {
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                
                int argb = image.getRGB(x, y);
                Color before = new Color(argb, true);
                
                int temp = (int) (redScale   * before.getRed()
                                + greenScale * before.getGreen()
                                + blueScale  * before.getBlue());
                
                Color after = new Color(temp, temp, temp, 255);
                image.setRGB(x, y, after.getRGB());
            }
        }

        return true;
    }

    public boolean addEdgeContrast() {
        
        Color[][] after = new Color[width][height];
        
        for (int x = 0; x < width; x++) {
            after[x][0]          = Color.BLACK;
            after[x][height - 1] = Color.BLACK;
        }
        
        for (int y = 0; y < height; y++) {
            after[0][y]         = Color.BLACK;
            after[width - 1][y] = Color.BLACK;
        }

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {

                int rightARGB = image.getRGB(x + 1, y);
                int leftARGB  = image.getRGB(x - 1, y);
                int downARGB  = image.getRGB(x, y + 1);
                int upARGB    = image.getRGB(x, y - 1);

                Color rightColor = new Color(rightARGB, true);
                Color leftColor  = new Color(leftARGB,  true);
                Color downColor  = new Color(downARGB,  true);
                Color upColor    = new Color(upARGB,    true);

                int rightIntensity = (rightColor.getRed() + rightColor.getBlue() + rightColor.getGreen()) / 3;
                int leftIntensity  = (leftColor.getRed()  + leftColor.getBlue()  + leftColor.getGreen() ) / 3;
                int downIntensity  = (downColor.getRed()  + downColor.getBlue()  + downColor.getGreen() ) / 3;
                int upIntensity    = (upColor.getRed()    + upColor.getBlue()    + upColor.getGreen()   ) / 3;

                int deltaX = Math.abs(rightIntensity - leftIntensity);
                int deltaY = Math.abs(upIntensity    - downIntensity);

                int gradient = Math.max(deltaX, deltaY);

                after[x][y] = new Color(gradient, gradient, gradient, 255);
            }
        }
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, after[x][y].getRGB());
            }
        }

        return true;
    }

    public Pixelator(String inputFileName, String outputFileName) throws IOException {

        this.inputFileName  = inputFileName;
        this.outputFileName = outputFileName;

        File inputFile = new File(inputFileName);
        image = ImageIO.read(inputFile);

        this.width  = image.getWidth();
        this.height = image.getHeight();
    }

    public boolean addCellShade() {
        
        BufferedImage oldColoredImage = cloneImage(image);
        addGrayscale(0.333, 0.333, 0.333);
        addEdgeContrast();
        
        // building borders for floodfill
        boolean[][] isBorder = new boolean[width][height];
        for (boolean[] line : isBorder) {
            Arrays.fill(line, false);
        }

        double portionMultiplier = 1.0 / (width * height);
        double rawAverage = 0.0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color cursor = new Color(image.getRGB(x, y), true);
                int intensity = (cursor.getRed() + cursor.getGreen() + cursor.getBlue()) / 3;
                rawAverage += intensity * portionMultiplier;
            }
        }

        int processedAverage = (int)rawAverage;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color cursor = new Color(image.getRGB(x, y), true);
                int intensity = (cursor.getRed() + cursor.getGreen() + cursor.getBlue()) / 3;
                isBorder[x][y] = intensity < processedAverage;
            }
        }

        // floodfill with average color
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                
                if (isBorder[x][y]) continue;

                double rawAverageRed   = 0;
                double rawAverageBlue  = 0;
                double rawAverageGreen = 0;
                
                Stack<Point> dfs = new Stack<Point>();
                Point seed = new Point(x, y);
                dfs.push(seed);
                
                ArrayList<Integer> reds   = new ArrayList<Integer>();
                ArrayList<Integer> blues  = new ArrayList<Integer>();
                ArrayList<Integer> greens = new ArrayList<Integer>();
                
                HashSet<Point> seen = new HashSet<Point>();
                ArrayList<Point> pointsToEdit = new ArrayList<Point>();

                seen.add(seed);

                // probing all points in region for average colors
                while (!dfs.empty()) {
                    
                    Point cursor = dfs.pop();
                    
                    int cursorX = (int)cursor.getX();
                    int cursorY = (int)cursor.getY();

                    ArrayList<Point> neighbors = new ArrayList<Point>();
                    
                    if (cursorX - 1 >= 0)     neighbors.add(new Point(cursorX - 1, cursorY));
                    if (cursorX + 1 < width)  neighbors.add(new Point(cursorX + 1, cursorY));
                    if (cursorY - 1 >= 0)     neighbors.add(new Point(cursorX, cursorY - 1));
                    if (cursorY + 1 < height) neighbors.add(new Point(cursorX, cursorY + 1));

                    for (Point neighbor : neighbors) {
                        if (!seen.contains(neighbor)) {
                            seen.add(neighbor);
                            pointsToEdit.add(neighbor);
                        }
                    }
                }

                // calculating average color values and marking points as explored
                for (Point point : pointsToEdit) {
                    
                    int cx = (int)point.getX();
                    int cy = (int)point.getY();

                    isBorder[cx][cy] = true;
                    Color cursor = new Color(image.getRGB(cx, cy), true);
                    
                    reds.add(cursor.getRed());
                    blues.add(cursor.getBlue());
                    greens.add(cursor.getGreen());
                }

                for (int red : reds)     rawAverageRed   += red   * (1.0 / reds.size());
                for (int blue : blues)   rawAverageBlue  += blue  * (1.0 / blues.size());
                for (int green : greens) rawAverageGreen += green * (1.0 / greens.size());

                int averageRed   = (int)rawAverageRed;
                int averageBlue  = (int)rawAverageBlue;
                int averageGreen = (int)rawAverageGreen;
                
                Color color = new Color(averageRed, averageGreen, averageBlue, 255);

                // updating color values of region to average
                for (Point point : pointsToEdit) {

                    int cx = (int)point.getX();
                    int cy = (int)point.getY();
                    
                    oldColoredImage.setRGB(cx, cy, color.getRGB());
                }
            }
        }
        
        // the image holds the grayscale image so you need to replace it
        image = oldColoredImage;
        return true;
    }

    public boolean addGrayscale() {
        return addGrayscale(0.299, 0.587, 0.114);
    }

    public boolean addPixelation() {
        return addPixelation(5);
    }

    public boolean addPixelation(int pixelSize) {
        
        if (pixelSize <= 0) return false;

        int reducedWidth = width   - (width  % pixelSize);
        int reducedHeight = height - (height % pixelSize);

        BufferedImage newImage = new BufferedImage(reducedWidth,
                                                   reducedHeight,
                                                   BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < reducedWidth; x += pixelSize) {
            for (int y = 0; y < reducedHeight; y += pixelSize) {
                
                int baseARGB = image.getRGB(x, y);

                for (int cx = x; cx < x + pixelSize; cx++) {
                    for (int cy = y; cy < y + pixelSize; cy++) {
                        newImage.setRGB(cx, cy, baseARGB);
                    }
                }
            }
        }

        image = newImage;
        return true;
    }

    public void update() throws IOException {
        File outputFile = new File(outputFileName);
        ImageIO.write(image, "png", outputFile);
    }
}

