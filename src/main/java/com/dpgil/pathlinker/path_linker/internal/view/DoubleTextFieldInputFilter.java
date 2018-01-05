package com.dpgil.pathlinker.path_linker.internal.view;

/**
 * TextFieldInputFilter class specifically validate for data type Double
 */
public class DoubleTextFieldInputFilter extends TextFieldInputFilter{

    @Override
    public boolean validate(String text) {
        if (text.trim().isEmpty()) return true;

        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
