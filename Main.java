import java.io.IOException;
import java.util.*;

public class Main {
    
    private static HashSet<String> options = new HashSet<String>();
    private static String[] rawOptions = {
        "add-greyscale",
        "pixelate",
        "add-cel-shade",
        "add-cell-shade",
        "add-edge-contrast"
    };

    public static void main(String[] args) {
        
        for (String command : rawOptions) options.add(command);

        if (args.length != 3) {
            
            System.out.println("Usage:");
            System.out.println("java Main <command> <input_file> <output_file>");
            System.out.println("Commands:");

            for (String command : rawOptions) System.out.println(command);
            
            System.out.println("Exiting... (Exit Code: 1)");
            System.exit(1);
        }

        try {
            
            String command = args[0];
            String inputFileName = args[1];
            String outputFileName = args[2];
            
            System.out.println("Reading from file " + inputFileName);
            Pixelator pixelator = new Pixelator(inputFileName, outputFileName);
            
            switch (command) {
                case "add-edge-contrast":
                    pixelator.addEdgeContrast();
                    break;
                case "add-cel-shade":
                case "add-cell-shade":
                    pixelator.addCellShade();
                    break;
                case "pixelate":
                    pixelator.addPixelation();
                    break;
                case "add-greyscale":
                case "add-grayscale":
                    pixelator.addGrayscale();
                    break;
                default:
                    System.out.println("Invalid command. Exiting... (Error Code: 2)");
                    System.exit(2);
            }

            System.out.println("Writing to file " + outputFileName);
            pixelator.update();

            System.out.println("All operations completed. Exiting... (Error Code: 0)");
        }
        catch (IOException ex) {
            System.out.println("FileIO failed. Exiting... (Error Code: 3)");
            System.exit(3);
        }
    }
}
