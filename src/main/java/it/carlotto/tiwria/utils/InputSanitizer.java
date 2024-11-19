package it.carlotto.tiwria.utils;

import it.carlotto.tiwria.exceptions.InvalidCharsException;
import org.apache.commons.text.StringEscapeUtils;

import java.sql.Timestamp;
import java.util.regex.Pattern;

public class InputSanitizer {
    public static String sanitizeAlphanumeric(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if (!Pattern.matches("[a-zA-Z0-9\\s]{0,50}", sanitized)) {
            throw new InvalidCharsException("Invalid chars found when sanitizing input");
        }

        return input;
    }

    public static String sanitizePassword(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if (!Pattern.matches("[a-zA-Z0-9\\s-+_#*'^?!\"£$€%&/()=@]{4,50}", sanitized)) {
            throw new InvalidCharsException("Invalid chars found when sanitizing input");
        }

        return input;
    }

    public static String sanitizeGeneric(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if (!Pattern.matches("[a-zA-Z0-9\\s-+_*!'^?àòèé]{0,50}", sanitized)) {
            throw new InvalidCharsException("Invalid chars found when sanitizing input");
        }

        return input;
    }

    public static int sanitizeNumeric(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if (!Pattern.matches("[0-9]{1,20}", sanitized)) {
            throw new InvalidCharsException("Invalid chars found when sanitizing input");
        }

        return Integer.parseInt(sanitized);
    }

    public static float sanitizeFloat(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if (!Pattern.matches("[0-9]{1,20}.[0-9]{0,2}", sanitized) &&
                !Pattern.matches("[0-9]{1,20}", sanitized)) {
            throw new InvalidCharsException("Invalid chars found when sanitizing input");
        }

        return Float.parseFloat(sanitized);
    }

    public static Timestamp sanitizeDateTime(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if (!Pattern.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}", sanitized)) {
            throw new InvalidCharsException("Invalid chars found when sanitizing input");
        }

        if (input.length()==16)  // Adding seconds if needed
            input += ":00";

        return Timestamp.valueOf(input.replace("T"," "));
    }

    public static boolean sanitizeBoolean(String input) throws InvalidCharsException {
        String sanitized = input;
        sanitized = StringEscapeUtils.escapeJava(sanitized);

        if(sanitized.equals("true")) return true;
        else if(sanitized.equals("false")) return false;
        throw new InvalidCharsException("Invalid chars found when sanitizing input");
    }
}
