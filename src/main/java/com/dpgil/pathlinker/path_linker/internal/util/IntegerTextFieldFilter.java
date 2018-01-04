package com.dpgil.pathlinker.path_linker.internal.util;

/**
 * TextFieldInputFilter class specifically validate for data type Integer
 */
public class IntegerTextFieldFilter extends TextFieldInputFilter {

    @Override
    public boolean validate(String text) {
        if (text.trim().isEmpty()) return true;

        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
