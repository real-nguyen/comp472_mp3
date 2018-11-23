public class NGram {

    private static final String CHARACTER_SET = "abcdefghijklmnopqrstuvwxyz";
    // EN = English
    // FR = French
    // OT = Spanish
    private String language;
    private char letter;
    private long count; 
    
    public NGram(String _language, char _letter) {
        language = _language;
        letter = _letter;
    }

    public static String getCharacterSet() {
        return CHARACTER_SET;
    }

    public String getLanguage() {
        return language;
    }

    public char getLetter() {
        return letter;
    }

    public long getCount() {
        return count;
    }

    public void incrementCount() {
        count++;
    }
}