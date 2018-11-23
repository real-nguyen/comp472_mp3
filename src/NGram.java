public class NGram {

    private static final String CHARACTER_SET = "abcdefghijklmnopqrstuvwxyz";
    // EN = English
    // FR = French
    // OT = Spanish
    private String language;
    private char character;
    private long count; 
    
    public NGram(String _language, char _character) {
        language = _language;
        character = _character;
    }

    public static String getCharacterSet() {
        return CHARACTER_SET;
    }

    public static int getVocabularySize() {
        return CHARACTER_SET.length();
    }

    public String getLanguage() {
        return language;
    }

    public char getCharacter() {
        return character;
    }

    public long getCount() {
        return count;
    }

    public void incrementCount() {
        count++;
    }
}