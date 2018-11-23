import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {

    private final static String INPUT_PATH = "input";   
    private final static String OUTPUT_PATH = "output";
    private final static double SMOOTHING_DELTA = 0.5;

    private static Unigram[] unigramsEN;
    private static Unigram[] unigramsFR;
    private static Unigram[] unigramsOT;

    public static void main(String[] args) {  
        trainUnigrams("EN");
        trainUnigrams("FR");     
        trainUnigrams("OT");
    }

    private static Unigram[] initUnigrams(String language, Unigram[] unigrams) {
        for (int i = 0; i < unigrams.length; i++) {
            unigrams[i] = new Unigram(language, Unigram.getCharacterSet().charAt(i));
        }

        return unigrams;
    }

    private static void trainUnigrams(String language) {
        int length = Unigram.getCharacterSet().length();
        Unigram[] unigrams;

        if (language == "EN") {
            unigramsEN = new Unigram[length];
            unigrams = initUnigrams(language, unigramsEN);
        }
        else if (language == "FR") {
            unigramsFR = new Unigram[length];
            unigrams = initUnigrams(language, unigramsFR);
        }
        else {
            unigramsOT = new Unigram[length];
            unigrams = initUnigrams(language, unigramsOT);
        }

        try {
            File trainingFolder = new File(INPUT_PATH + "/training");
            BufferedReader in = new BufferedReader(new FileReader(trainingFolder + "/train" + language + ".txt"));

            int readValue;
            while ((readValue = in.read()) != -1) {                   
                if (!Character.isAlphabetic(readValue)) {
                    // Not a letter
                    continue;
                }
                char letter = Character.toLowerCase((char)readValue);
                int index = Unigram.getCharacterSet().indexOf(letter);
                unigrams[index].incrementLetterCount();                
            }
            in.close();           
        }
        catch (IOException e) {

        }

        outputUnigrams(language);
    }

    private static void outputUnigrams(String language) {
        Unigram[] unigrams;

        if (language == "EN") {
            unigrams = unigramsEN;
        }
        else if (language == "FR") {
            unigrams = unigramsFR;
        }
        else {
            unigrams = unigramsOT;
        }
        
        double denominator = unigrams.length + SMOOTHING_DELTA * getVocabularySize(language);

        try {
            PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/unigram" + language + ".txt");

            // Smoothed probabilities
            // No log applied
            for (Unigram u : unigrams) {            
                pw.print("(" + u.getLetter() + ") = ");
                double numerator = u.getLetterCount() + SMOOTHING_DELTA;
                double letterProbability = numerator / denominator;
                pw.println(letterProbability);
            }

            pw.close();
        }
        catch (IOException e) {

        }
    
    }

    private static long getVocabularySize(String language) {
        long size = 0;
        Unigram[] unigrams;

        if (language == "EN") {
            unigrams = unigramsEN;
        }
        else if (language == "FR") {
            unigrams = unigramsFR;
        }
        else {
            unigrams = unigramsOT;
        }

        for (Unigram u : unigrams) {
            size += u.getLetterCount();
        }

        return size;
    }
}