package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import jakarta.servlet.UnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {
    @Test
    void userLoadingTest() throws UnavailableException, ObjectNotFoundException {
        User user = User.loadWithCreds("test2", "testone");
        assertEquals(2, user.getId());
        assertEquals("test2", user.getUsername());
        assertEquals("testname", user.getName());
        assertEquals("testsurname", user.getSurname());
        assertEquals("PlaceTheBombAndRun", user.getAddress());
    }

    @Test
    void wrongPasswordTest() {
        assertThrows(ObjectNotFoundException.class, () -> User.loadWithCreds("test", "adasda"));
    }
}
