package com.oldsboy.ideaplugin;

import java.util.List;

public class Config {
    private List<String> regex_list;
    private List<String> black_list;

    public List<String> getBlack_list() {
        return black_list;
    }

    public void setBlack_list(List<String> black_list) {
        this.black_list = black_list;
    }

    public List<String> getRegex_list() {
        return regex_list;
    }

    public void setRegex_list(List<String> regex_list) {
        this.regex_list = regex_list;
    }
}
