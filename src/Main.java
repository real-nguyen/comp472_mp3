import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {

    private final static String INPUT_PATH = "input";   
    private final static String OUTPUT_PATH = "output";
    private final static double SMOOTHING_DELTA = 0.5;

    private static NGram[] unigramsEN;
    private static NGram[] unigramsFR;
    private static NGram[] unigramsOT;
    private static NGram[][] bigramsEN;
    private static NGram[][] bigramsFR;
    private static NGram[][] bigramsOT;

    public static void main(String[] args) {  
        // trainUnigrams("EN");
        // trainUnigrams("FR");     
        // trainUnigrams("OT");
        trainBigrams("EN");
    }

    private static NGram[] initUnigrams(String language, NGram[] unigrams) {
        for (int i = 0; i < unigrams.length; i++) {
            unigrams[i] = new NGram(language, NGram.getCharacterSet().charAt(i));
        }

        return unigrams;
    }

    private static void trainUnigrams(String language) {
        int length = NGram.getCharacterSet().length();
        NGram[] unigrams;

        if (language == "EN") {
            unigramsEN = new NGram[length];
            unigrams = initUnigrams(language, unigramsEN);
        }
        else if (language == "FR") {
            unigramsFR = new NGram[length];
            unigrams = initUnigrams(language, unigramsFR);
        }
        else {
            unigramsOT = new NGram[length];
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
                int index = NGram.getCharacterSet().indexOf(letter);
                unigrams[index].incrementCount();                
            }
            in.close();           
        }
        catch (IOException e) {

        }

        outputUnigrams(unigrams, language);
    }

    private static void outputUnigrams(NGram[] unigrams, String language) {
        double denominator = unigrams.length + SMOOTHING_DELTA * getUnigramVocabularySize(unigrams);

        try {
            PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/unigram" + language + ".txt");

            // Smoothed probabilities
            // No log applied
            for (NGram u : unigrams) {            
                pw.print("(" + u.getLetter() + ") = ");
                double numerator = u.getCount() + SMOOTHING_DELTA;
                double letterProbability = numerator / denominator;
                pw.println(letterProbability);
            }

            pw.close();
        }
        catch (IOException e) {

        }  
    }

    private static long getUnigramVocabularySize(NGram[] unigrams) {
        long size = 0;

        for (NGram u : unigrams) {
            size += u.getCount();
        }

        return size;
    }

    private static NGram[][] initBigrams(String language, NGram[][] bigrams) {
        /*
        [
            a [a, b, c, ..., z],
            b [a, b, c, ..., z],
            c [a, b, c, ..., z],
            ...
            z [a, b, c, ..., z]
        ]
        Counters are incremented in inner n-grams
        e.g. bigram aa
        a [a <-- counter incremented here]
        ^ counter remains at 0
        */

        return bigrams;
    }

    private static void trainBigrams(String language) {
        // Initiate bigram 2D matrix
        // aa, ba, ca, ..., zz
        int length = NGram.getCharacterSet().length();
        NGram[][] bigrams;

        if (language == "EN") {
            bigramsEN = new NGram[length][length];
            bigrams = initBigrams(language, bigramsEN);
        }
        else if (language == "FR") {
            bigramsFR = new NGram[length][length];
            bigrams = initBigrams(language, bigramsFR);
        }
        else {
            bigramsOT = new NGram[length][length];
            bigrams = initBigrams(language, bigramsOT);
        }

        // Read from corpora
        try {
            File trainingFolder = new File(INPUT_PATH + "/training");
            BufferedReader in = new BufferedReader(new FileReader(trainingFolder + "/train" + language + ".txt"));

            int firstValue = in.read();
            int secondValue = in.read();
            char firstLetter = 0;
            char secondLetter = 0;
            while (firstValue != -1 && secondValue != -1) {
                if (Character.isAlphabetic(firstValue) && Character.isAlphabetic(secondValue)) {
                    // Whale -> wh, ha, al, le
                    // Add bigrams here
                    firstLetter = Character.toLowerCase((char)firstValue);
                    secondLetter = Character.toLowerCase((char)secondValue);
                    firstValue = secondValue;
                    secondValue = in.read();
                }
                else if (Character.isAlphabetic(firstValue) && !Character.isAlphabetic(secondValue)) {
                    // C'est -> ce, es, st
                    // Skip second character until a letter is found
                    firstLetter = Character.toLowerCase((char)firstValue);                  
                    secondValue = in.read();
                    continue;
                }
                else if (!Character.isAlphabetic(firstValue) && Character.isAlphabetic(secondValue)) {
                    // "Hey" -> he, ey
                    // Make second character first character in next iteration
                    firstLetter = Character.toLowerCase((char)secondValue);
                    firstValue = secondValue;
                    secondValue = in.read();
                    continue;
                }
                else {
                    // --HACKLUYT -> ha, ac, ck, kl, lu, uy, yt
                    // Skip twice
                    firstValue = in.read();
                    secondValue = in.read();
                    continue;
                }
                System.out.print(firstLetter);
                System.out.println(secondLetter);
            }

            
            // Ex: abc
            // Bigram: ab => Printed (a|b) in bigram output file
            // P(b|a) => Probability that b follows a
            // P(b|a) = P(b,a) / P(a)

            // Read first letter
            // Get first index
            // Move to next
            // Read second letter
            // Get second index
            // Increment inner n-gram count

            // int index = NGram.getCharacterSet().indexOf(letter);
            // in.
            // unigrams[index].incrementCount();   

            in.close();           
        }
        catch (IOException e) {

        }
        // Output language model
        //outputBigrams(bigrams, language);
    }

    private static void outputBigrams(NGram[][] bigrams, String language) {
        double denominator = bigrams.length + SMOOTHING_DELTA * getBigramVocabularySize(bigrams);

        try {
            PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/bigram" + language + ".txt");

            for (NGram[] outer : bigrams) {            
                for (NGram inner : outer) {
                    // Calculate smoothed conditional probability
                }
            }

            pw.close();
        }
        catch (IOException e) {

        }  
    }

    private static long getBigramVocabularySize(NGram[][] bigrams) {
        long size = 0;

        for (NGram[] outer : bigrams) {            
            for (NGram inner : outer) {
                size += inner.getCount();
            }
        }

        return size;
    }
}