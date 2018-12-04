import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class Main {

    private final static String INPUT_PATH = "input";   
    private final static String OUTPUT_PATH = "output";
    private final static double SMOOTHING_DELTA = 0.5;

    private static Map<String, NGram[]> unigrams;
    private static Map<String, NGram[][]> bigrams;
    private static Map<String, String> languages;

    public static void main(String[] args) {  
        // Use LinkedHashMap to preserve adding order
        unigrams = new LinkedHashMap<String, NGram[]>();
        bigrams = new LinkedHashMap<String, NGram[][]>();
        languages = new LinkedHashMap<String, String>();
        languages.put("FR", "FRENCH");
        languages.put("EN", "ENGLISH");
        languages.put("OT", "OTHER");

        // Basic setup
        trainUnigrams("FR");
        trainUnigrams("EN");        
        trainUnigrams("OT");

        trainBigrams("FR");
        trainBigrams("EN"); 
        trainBigrams("OT");

        readSentences("/sentences.txt", "/sentences/out");

        // Experiment 1: Read old forms of all 3 languages using same training data
        readSentences("/experiments/ex1-sentences.txt", "/experiments/sentences/ex1-out");
        // Experiment 2: Read mutually intelligible languages using same training data
        readSentences("/experiments/ex2-sentences.txt", "/experiments/sentences/ex2-out");
        // Experiment 3: Train system to read Haitian Creole (EX), then test with EX sentences
        languages.put("EX", "EXPERIMENT");
        trainUnigrams("EX");
        trainBigrams("EX");
        readSentences("/experiments/ex3-sentences.txt", "/experiments/sentences/ex3-out");
        // Experiment 4: Read basic setup sentences including EX
        readSentences("/sentences.txt", "/experiments/sentences/ex4-out");
    }

    private static NGram[] initUnigrams(String language) {        
        int length = NGram.getCharacterSet().length();
        unigrams.put(language, new NGram[length]);

        for (int i = 0; i < length; i++) {
            unigrams.get(language)[i] = new NGram(language, NGram.getCharacterSet().charAt(i));
        }

        return unigrams.get(language);
    }

    private static void trainUnigrams(String language) {        
        NGram[] unigramArray = initUnigrams(language);

        try {
            String inputFilePath = language == "EX" ? 
                INPUT_PATH + "/experiments/train" + language + ".txt" :
                INPUT_PATH + "/training/train" + language + ".txt";

            BufferedReader in = new BufferedReader(new FileReader(inputFilePath));

            int readValue;
            while ((readValue = in.read()) != -1) {                   
                if (!Character.isAlphabetic(readValue)) {
                    continue;
                }
                char letter = Character.toLowerCase((char)readValue);
                int index = NGram.getCharacterSet().indexOf(letter);
                unigramArray[index].incrementCount();                
            }
            in.close();           
        }
        catch (IOException e) {

        }

        outputUnigrams(unigramArray, language);
    }

    private static void outputUnigrams(NGram[] unigrams, String language) {
        // Calculate P(unigram) = (Count(letter) + delta) / (N + delta * B)
        // N = total number of n-grams in corpus
        // B = size of vocabulary 

        long n = getTotalUnigramCount(unigrams);
        long b = NGram.getVocabularySize();
        double denominator = n + SMOOTHING_DELTA * b;

        try {
            String outputFilePath = language == "EX" ? 
                OUTPUT_PATH + "/experiments/unigram" + language + ".txt" :
                OUTPUT_PATH + "/unigram" + language + ".txt";

            PrintWriter pw = new PrintWriter(outputFilePath);

            for (NGram u : unigrams) {            
                pw.print("(" + u.getCharacter() + ") = ");
                double numerator = u.getCount() + SMOOTHING_DELTA;
                double probability = numerator / denominator;
                pw.println(probability);
            }

            pw.close();
        }
        catch (IOException e) {

        }  
    }

    private static long getTotalUnigramCount(NGram[] unigrams) {
        long count = 0;

        for (NGram u : unigrams) {
            count += u.getCount();
        }

        return count;
    }

    private static NGram[][] initBigrams(String language) {
        /*
        [
            [a, b, c, ..., z], -> bigrams aa, ab, ac, ..., az
            [a, b, c, ..., z], -> bigrams ba, bb, bc, ..., bz
            [a, b, c, ..., z], -> bigrams ca, cb, cc, ..., cz
            ...
            [a, b, c, ..., z]  -> bigrams za, zb, zc, ..., zz
        ]
        */
        int length = NGram.getCharacterSet().length();
        bigrams.put(language, new NGram[length][length]);
        NGram[][] bigramMatrix = bigrams.get(language);

        for (int i = 0; i < bigramMatrix.length; i++) { 
            for (int j = 0; j < bigramMatrix[i].length; j++) {
                bigramMatrix[i][j] = new NGram(language, NGram.getCharacterSet().charAt(j));
            }
        }

        return bigramMatrix;
    }

    private static void trainBigrams(String language) {
        // Initiate bigram 2D matrix
        // aa, ba, ca, ..., zz
        
        NGram[][] bigrams = initBigrams(language);

        // Read from corpora
        try {
            String inputFilePath = language == "EX" ? 
            INPUT_PATH + "/experiments/train" + language + ".txt" :
            INPUT_PATH + "/training/train" + language + ".txt";

            BufferedReader in = new BufferedReader(new FileReader(inputFilePath));

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

                    int firstIndex = NGram.getCharacterSet().indexOf(firstLetter);
                    int secondIndex = NGram.getCharacterSet().indexOf(secondLetter);
                    bigrams[firstIndex][secondIndex].incrementCount();
                }
                else if (Character.isAlphabetic(firstValue) && !Character.isAlphabetic(secondValue)) {
                    // C'est -> ce, es, st
                    // Keep first character then skip
                    firstLetter = Character.toLowerCase((char)firstValue);                  
                    secondValue = in.read();
                }
                else if (!Character.isAlphabetic(firstValue) && Character.isAlphabetic(secondValue)) {
                    // "Hey" -> he, ey
                    // Keep second character then skip
                    firstLetter = Character.toLowerCase((char)secondValue);
                    firstValue = secondValue;
                    secondValue = in.read();
                }
                else {
                    // --HACKLUYT -> ha, ac, ck, kl, lu, uy, yt
                    // Skip twice
                    firstValue = in.read();
                    secondValue = in.read();
                }
            }  

            in.close();           
        }
        catch (IOException e) {

        }

        printProbabilityTable(bigrams, language);
        outputBigrams(bigrams, language);
    }

    private static void outputBigrams(NGram[][] bigrams, String language) {
        // Calculate P(bigram) = (Count(bigram) + delta) / (N + delta * B)
        // P(bigram) = probability of a letter following another
        // e.g. in EN, P(h|w) >>>> P(z|x)
        // N = total number of n-grams in corpus for that bigram
        // e.g. for vocab ab: (a|a) = 8, (a|b) = 2 -> N = 10 
        // B = size of vocabulary 

        try {
            String outputFilePath = language == "EX" ? 
                OUTPUT_PATH + "/experiments/bigram" + language + ".txt" :
                OUTPUT_PATH + "/bigram" + language + ".txt";

            PrintWriter pw = new PrintWriter(outputFilePath);

            for (int i = 0; i < bigrams.length; i++) {            
                for (int j = 0; j < bigrams[i].length; j++) {
                    // Calculate smoothed conditional probability
                    pw.print("(" + NGram.getCharacterSet().charAt(i) + "|" + bigrams[i][j].getCharacter() + ") = ");
                    
                    double numerator = bigrams[i][j].getCount() + SMOOTHING_DELTA;
                    long n = getTotalUnigramCount(bigrams[i]);
                    long b = NGram.getVocabularySize();
                    double denominator = n + SMOOTHING_DELTA * b;
                    
                    double probability = numerator / denominator;
                    pw.println(probability);
                }
            }

            pw.close();
        }
        catch (IOException e) {

        }  
    }

    // For testing purposes only
    private static void printProbabilityTable(NGram[][] bigrams, String language) {
        try {
            String outputFilePath = language == "EX" ? 
                OUTPUT_PATH + "/experiments/probTable" + language + ".txt" :
                OUTPUT_PATH + "/probTable" + language + ".txt";

            PrintWriter pw = new PrintWriter(outputFilePath);

            pw.println(language + " BIGRAMS");
            for (char letter : NGram.getCharacterSet().toCharArray()) {
                pw.print("\t\t\t" + letter);
            }
            pw.println("\t\t\tTotal");

            double total = 0;
            for (int i = 0; i < bigrams.length; i++) {
                pw.print(NGram.getCharacterSet().charAt(i) + "\t\t\t");
                for (int j = 0; j < bigrams[i].length; j++) {
                    double count = bigrams[i][j].getCount() + SMOOTHING_DELTA;
                    pw.printf("%-12s", count);
                    total += count;
                }

                pw.println(total);
                total = 0;
            }

            pw.close();
        } catch (IOException e) {

        }
    }

    private static long getTotalBigramCount(NGram[][] bigrams) {
        long count = 0;

        for (int i = 0; i < bigrams.length; i++) {
            for (int j = 0; j < bigrams[i].length; j++) {
                count += bigrams[i][j].getCount();
            }
        }

        return count;
    }

    private static void readSentences(String inputFile, String outputFilePrefix) {
        LinkedList<String> sentences = new LinkedList<String>();        

        try {
            File sentencesFile = new File(INPUT_PATH + inputFile);
            BufferedReader in = new BufferedReader(new FileReader(sentencesFile));           
            String line;

            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    // Lines that start with # are comments; skip
                    continue;
                }

                sentences.offer(line);
            }
            in.close(); 

            int sentenceCount = 0;
            while (!sentences.isEmpty()) {
                PrintWriter pw = new PrintWriter(OUTPUT_PATH + outputFilePrefix + ++sentenceCount + ".txt");
                String sentence = sentences.poll();

                pw.println(sentence);
                pw.println();
                readSentenceUnigrams(sentence, pw);
                pw.println();
                pw.println("--------------------");
                pw.println();
                readSentenceBigrams(sentence, pw);

                pw.close();
            }
        }
        catch (IOException e) {

        }
    }

    private static void readSentenceUnigrams(String sentence, PrintWriter pw) {        
        pw.println("UNIGRAM MODEL:");
        pw.println();

        // HNB = argmax(log(P(cj)) + sum(log(P(wi|cj))))
        String[] languageKeys = unigrams.keySet().toArray(new String[0]);
        Map<String, Double> logProbabilities = new LinkedHashMap<String, Double>();
        for (String key : languageKeys) {
            logProbabilities.put(key, getUnigramLanguageModelProbability(key));
        }

        for (char c : sentence.toCharArray()) {
            if (!Character.isAlphabetic(c)) {
                continue;
            }
            
            double sumProbability = 0;
            c = Character.toLowerCase(c);
            pw.println("UNIGRAM: " + c);

            for (String key : languageKeys) {
                pw.print(languages.get(key) + ": P(" + c + ") = ");
                double unigramProbability = getUnigramProbability(unigrams.get(key), c);
                sumProbability = logProbabilities.get(key) + unigramProbability;
                logProbabilities.replace(key, sumProbability);
                pw.println(unigramProbability + "\t==> log prob of sentence so far: " + sumProbability);                    
            }
            pw.println();  
        }

        // Initialize to first item in map
        double max = logProbabilities.get(languageKeys[0]);
        String maxLanguage = languages.get(languageKeys[0]);
        for (String key : languageKeys) {
            if (logProbabilities.get(key) > max) {
                max = logProbabilities.get(key);
                maxLanguage = languages.get(key);
            }
        } 
        pw.println("According to the unigram model, the sentence is in " + maxLanguage);
    }

    private static double getUnigramLanguageModelProbability(String language) {
        NGram[] unigramsArray = unigrams.get(language);

        double numerator = getTotalUnigramCount(unigramsArray) + SMOOTHING_DELTA;
        double denominator = 0;
        for (String key : unigrams.keySet()) {
            denominator += getTotalUnigramCount(unigrams.get(key));
        }
        denominator += SMOOTHING_DELTA * NGram.getVocabularySize();

        return Math.log10(numerator / denominator);
    }

    private static double getUnigramProbability(NGram[] unigrams, char character) {
        int index = NGram.getCharacterSet().indexOf(character);
        double numerator = unigrams[index].getCount() + SMOOTHING_DELTA;
        double denominator = getTotalUnigramCount(unigrams) + SMOOTHING_DELTA * NGram.getVocabularySize();

        return Math.log10(numerator / denominator);
    }

    private static void readSentenceBigrams(String sentence, PrintWriter pw) {
        pw.println("BIGRAM MODEL:");
        pw.println();

        // HNB = argmax(log(P(cj)) + sum(log(P(wi|cj))))
        String[] languageKeys = unigrams.keySet().toArray(new String[0]);
        Map<String, Double> logProbabilities = new LinkedHashMap<String, Double>();
        for (String key : languageKeys) {
            logProbabilities.put(key, getBigramLanguageModelProbability(key));
        }
        String cleanedSentence = "";

        for (char c : sentence.toCharArray()) {
           if (!Character.isAlphabetic(c)) {
               continue;
           }
           cleanedSentence += Character.toLowerCase(c); 
        }

        char firstLetter;
        char secondLetter;
        for (int i = 0; i < cleanedSentence.length(); i++) {
            if (i + 1 >= cleanedSentence.length()) {
                break;
            }

            firstLetter = cleanedSentence.charAt(i);
            secondLetter = cleanedSentence.charAt(i + 1);
            double sumProbability = 0;

            pw.println("BIGRAM: " + firstLetter + secondLetter);

            for (String key : languageKeys) {
                pw.print(languages.get(key) + ": P(" + secondLetter + "|" + firstLetter + ") = ");
                double bigramProbability = getBigramProbability(bigrams.get(key), firstLetter, secondLetter);
                sumProbability = logProbabilities.get(key) + bigramProbability;
                logProbabilities.replace(key, sumProbability);
                pw.println(bigramProbability + "\t==> log prob of sentence so far: " + sumProbability);                    
            }
            pw.println();
        }

        // Initialize to first item in map
        double max = logProbabilities.get(languageKeys[0]);
        String maxLanguage = languages.get(languageKeys[0]);
        for (String key : languageKeys) {
            if (logProbabilities.get(key) > max) {
                max = logProbabilities.get(key);
                maxLanguage = languages.get(key);
            }
        } 
        pw.println("According to the bigram model, the sentence is in " + maxLanguage);
    }

    private static double getBigramLanguageModelProbability(String language) {
        NGram[][] bigramMatrix = bigrams.get(language);

        double numerator = getTotalBigramCount(bigramMatrix) + SMOOTHING_DELTA;
        double denominator = 0;
        for (String key : bigrams.keySet()) {
            denominator += getTotalBigramCount(bigrams.get(key));
        }
        denominator += SMOOTHING_DELTA * Math.pow(NGram.getVocabularySize(), 2);

        return Math.log10(numerator / denominator);
    }

    private static double getBigramProbability(NGram[][] bigrams, char firstCharacter, char secondCharacter) {
        int firstIndex = NGram.getCharacterSet().indexOf(firstCharacter);
        int secondIndex = NGram.getCharacterSet().indexOf(secondCharacter);
        double numerator = bigrams[firstIndex][secondIndex].getCount() + SMOOTHING_DELTA;
        double denominator = getTotalUnigramCount(bigrams[firstIndex]) + SMOOTHING_DELTA * NGram.getVocabularySize();
    
        return Math.log10(numerator / denominator);
    }
}