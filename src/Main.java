import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

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
        trainUnigrams("EN");
        trainUnigrams("FR");
        trainUnigrams("OT");

        trainBigrams("EN");
        trainBigrams("FR");
        trainBigrams("OT");

        readSentences();
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
        // Calculate P(unigram) = (Count(letter) + delta) / (N + delta * B)
        // N = total number of n-grams in corpus
        // B = size of vocabulary 

        long n = getTotalUnigramCount(unigrams);
        long b = NGram.getVocabularySize();
        double denominator = n + SMOOTHING_DELTA * b;

        try {
            PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/unigram" + language + ".txt");

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

    private static NGram[][] initBigrams(String language, NGram[][] bigrams) {
        /*
        [
            [a, b, c, ..., z], -> bigrams aa, ab, ac, ..., az
            [a, b, c, ..., z], -> bigrams ba, bb, bc, ..., bz
            [a, b, c, ..., z], -> bigrams ca, cb, cc, ..., cz
            ...
            [a, b, c, ..., z]  -> bigrams za, zb, zc, ..., zz
        ]
        */

        for (NGram[] outer : bigrams) {            
            initUnigrams(language, outer);
        }

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

                    int firstIndex = NGram.getCharacterSet().indexOf(firstLetter);
                    int secondIndex = NGram.getCharacterSet().indexOf(secondLetter);
                    bigrams[firstIndex][secondIndex].incrementCount();
                }
                else if (Character.isAlphabetic(firstValue) && !Character.isAlphabetic(secondValue)) {
                    // C'est -> ce, es, st
                    // Skip second character until a letter is found
                    firstLetter = Character.toLowerCase((char)firstValue);                  
                    secondValue = in.read();
                }
                else if (!Character.isAlphabetic(firstValue) && Character.isAlphabetic(secondValue)) {
                    // "Hey" -> he, ey
                    // Make second character first character in next iteration
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
            PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/bigram" + language + ".txt");

            for (int i = 0; i < bigrams.length; i++) {            
                for (int j = 0; j < bigrams[i].length; j++) {
                    // Calculate smoothed conditional probability
                    pw.print("(" + NGram.getCharacterSet().charAt(i) + "|" + bigrams[i][j].getCharacter() + ") = ");
                    
                    double numerator = bigrams[i][j].getCount() + SMOOTHING_DELTA;
                    long n = getBigramCount(bigrams, i);
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
            PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/probTable" + language + ".txt");

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

    private static long getBigramCount(NGram[][] bigrams, int outerIndex) {
        long count = 0;
     
        for (NGram inner : bigrams[outerIndex]) {
            count += inner.getCount();
        }        

        return count;
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

    private static void readSentences() {
        LinkedList<String> sentences = new LinkedList<String>();        

        try {
            File sentencesFile = new File(INPUT_PATH + "/sentences.txt");
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
                PrintWriter pw = new PrintWriter(OUTPUT_PATH + "/sentences/out" + ++sentenceCount + ".txt");
                String sentence = sentences.poll();

                pw.println(sentence);
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
        double logProbabilityEN = getUnigramLanguageModelProbability("EN");
        double logProbabilityFR = getUnigramLanguageModelProbability("FR");
        double logProbabilityOT = getUnigramLanguageModelProbability("OT");

        for (char c : sentence.toCharArray()) {
            if (!Character.isAlphabetic(c)) {
                continue;
            }
            
            double tempProbability = 0;
            c = Character.toLowerCase(c);
            pw.println("UNIGRAM: " + c);

            pw.print("FRENCH: P(" + c + ") = ");
            tempProbability = getUnigramProbability(unigramsFR, c);
            logProbabilityFR += tempProbability;
            pw.println(tempProbability + "\t==> log prob of sentence so far: " + logProbabilityFR);

            pw.print("ENGLISH: P(" + c + ") = ");
            tempProbability = getUnigramProbability(unigramsEN, c);
            logProbabilityEN += tempProbability;
            pw.println(tempProbability + "\t==> log prob of sentence so far: " + logProbabilityEN);

            pw.print("OTHER: P(" + c + ") = ");
            tempProbability = getUnigramProbability(unigramsOT, c);
            logProbabilityOT += tempProbability;
            pw.println(tempProbability + "\t==> log prob of sentence so far: " + logProbabilityOT);
            pw.println();
        }

        double max = Math.max(logProbabilityEN, Math.max(logProbabilityFR, logProbabilityOT)); 
        pw.print("According to the unigram model, the sentence is in ");
        if (max == logProbabilityEN) {
            pw.println("English");
        }
        else if (max == logProbabilityFR) {
            pw.println("French");
        }
        else {
            pw.println("Other");
        }
    }

    private static double getUnigramLanguageModelProbability(String language) {
        NGram[] unigrams;

        if (language == "EN") {
            unigrams = unigramsEN;
        }
        else if (language == "FR") {
            unigrams = unigramsFR;
        }
        else {
            unigrams = unigramsOT;
        }

        double numerator = getTotalUnigramCount(unigrams) + SMOOTHING_DELTA;
        double denominator = getTotalUnigramCount(unigramsEN) + getTotalUnigramCount(unigramsFR) + getTotalUnigramCount(unigramsOT) + SMOOTHING_DELTA * NGram.getVocabularySize();    

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
        double logProbabilityEN = getBigramLanguageModelProbability("EN");
        double logProbabilityFR = getBigramLanguageModelProbability("FR");
        double logProbabilityOT = getBigramLanguageModelProbability("OT");
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
            double tempProbability = 0;

            pw.println("BIGRAM: " + firstLetter + secondLetter);

            pw.print("FRENCH: P(" + secondLetter + "|" + firstLetter + ") = ");
            tempProbability = getBigramProbability(bigramsFR, firstLetter, secondLetter);
            logProbabilityFR += tempProbability;
            pw.println(tempProbability + "\t==> log prob of sentence so far: " + logProbabilityFR);

            pw.print("ENGLISH: P(" + secondLetter + "|" + firstLetter + ") = ");
            tempProbability = getBigramProbability(bigramsEN, firstLetter, secondLetter);
            logProbabilityEN += tempProbability;
            pw.println(tempProbability + "\t==> log prob of sentence so far: " + logProbabilityEN);

            pw.print("OTHER: P(" + secondLetter + "|" + firstLetter + ") = ");
            tempProbability = getBigramProbability(bigramsOT, firstLetter, secondLetter);
            logProbabilityOT += tempProbability;
            pw.println(tempProbability + "\t==> log prob of sentence so far: " + logProbabilityOT);
            pw.println();
        }

        double max = Math.max(logProbabilityEN, Math.max(logProbabilityFR, logProbabilityOT)); 
        pw.print("According to the unigram model, the sentence is in ");
        if (max == logProbabilityEN) {
            pw.println("English");
        }
        else if (max == logProbabilityFR) {
            pw.println("French");
        }
        else {
            pw.println("Other");
        }
    }

    private static double getBigramLanguageModelProbability(String language) {
        NGram[][] bigrams;

        if (language == "EN") {
            bigrams = bigramsEN;
        }
        else if (language == "FR") {
            bigrams = bigramsFR;
        }
        else {
            bigrams = bigramsOT;
        }

        double numerator = getTotalBigramCount(bigrams) + SMOOTHING_DELTA;
        double denominator = getTotalBigramCount(bigramsEN) + getTotalBigramCount(bigramsEN) + getTotalBigramCount(bigramsEN) + SMOOTHING_DELTA * Math.pow(NGram.getVocabularySize(), 2);

        return Math.log10(numerator / denominator);
    }

    private static double getBigramProbability(NGram[][] bigrams, char firstCharacter, char secondCharacter) {
        int firstIndex = NGram.getCharacterSet().indexOf(firstCharacter);
        int secondIndex = NGram.getCharacterSet().indexOf(secondCharacter);
        double numerator = bigrams[firstIndex][secondIndex].getCount() + SMOOTHING_DELTA;
        double denominator = getBigramCount(bigrams, firstIndex) + SMOOTHING_DELTA * NGram.getVocabularySize();
    
        return Math.log10(numerator / denominator);
    }
}