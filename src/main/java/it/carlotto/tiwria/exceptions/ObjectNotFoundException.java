package it.carlotto.tiwria.exceptions;

/**
 * This Exception is thrown when a method looks for a specific
 * object inside the DB and can't find it.
 */
public class ObjectNotFoundException extends Exception{
    public ObjectNotFoundException(String message) { super(message); }
}
