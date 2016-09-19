package data;

/**
 * Created by BG on 9/15/16.
 */


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class guessList {

    private String         targetWord;
    private Set<Character> goodGuesses;
    private Set<Character> badGuesses;
    private ObjectMapper mapper;        //Trying to put the mapper to put the data direclty

    public void guessList(){
        goodGuesses = null;
        badGuesses = null;
        targetWord = null;
        mapper = new ObjectMapper();
    }


    public String getTargetWord() {
        return targetWord;
    }

    public void setTargetWord(String targetWord) {
        this.targetWord = targetWord;
    }

    public void setJSONTargetWord(File file, String target) throws IOException {
        mapper.writeValue(file,target);
    }

    public Set<Character> getCorrectGuess() {
        return goodGuesses;
    }

    public void setCorrectGuess(Set correctGuess) {
        this.goodGuesses = correctGuess;
    }

    public void setJSONCorrectGuess(File file, Set correctGuess) throws IOException {
        mapper.writeValue(file,correctGuess);
    }

    public Set<Character> getWrongGuess() {
        return badGuesses;
    }

    public void setWrongGuess(Set WrongGuess) {

        this.badGuesses = WrongGuess;

    }

    public void setJSONWrongGuess(File file, Set wrongWord) throws IOException {
        mapper.writeValue(file,wrongWord);
    }
}
