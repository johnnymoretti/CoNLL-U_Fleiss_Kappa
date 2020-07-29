package it.giovannimoretti;

import java.util.ArrayList;
import java.util.List;

public class CoNLL_U_File {
    String ConLLUString = "";
    List<String> CoNNL_U_Sentences = new ArrayList<>();

    CoNLL_U_File(String connlu) {
        this.ConLLUString = connlu;
        this.CoNNL_U_Sentences = getSentences(connlu);
    }

    public List<String> getCoNNL_U_Sentences() {
        return CoNNL_U_Sentences;
    }

    private static List<String> getSentences (String conllu){
        List<String> sentences = new ArrayList<>();
        String sentence ="";
        for (String line : conllu.split("\n")){
            if (line.trim().length() > 0){
                sentence += line+"\n";
            }else{
                if (sentence.length()>0){
                    sentences.add(sentence);
                    sentence="";
                }
            }

        }
        if (sentence.length()>0 ){
            sentences.add(sentence);
        }
        return sentences;
    }
}
