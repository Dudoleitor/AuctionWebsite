package it.carlotto.tiwria.exceptions;

/**
 * This exception is thrown when a functions attempts to
 * insert an object into the DB and the query fails.
 */
public class InsertIntoDBFailedException extends Exception{
    public InsertIntoDBFailedException(String message) {
        super(message);
    }
}
