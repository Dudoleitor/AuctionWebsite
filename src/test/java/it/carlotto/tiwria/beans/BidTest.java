package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import jakarta.servlet.UnavailableException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BidTest {
    @Test
    void bidFromUserTest() throws UnavailableException, ObjectNotFoundException, SQLException {
        List<Bid> bidList = Bid.getBidsPerUser(User.loadWithId(2));

        assertEquals(4, bidList.size());
        for (Bid bid : bidList) {
            assertEquals(2, bid.getBidder_user_id());
            assertEquals("test2", bid.getBidder_user_username());
            assertEquals(4, bid.getAuction_id());
        }

        assertEquals(130.0, bidList.get(0).getAmount());
        assertEquals(120.0, bidList.get(1).getAmount());
        assertEquals(110.0, bidList.get(2).getAmount());
        assertEquals(100.0, bidList.get(3).getAmount());
    }

    @Test
    void bidPerAuctionTest() throws UnavailableException, ObjectNotFoundException {
        List<Bid> bidList = Bid.getBidsPerAuction(4);

        assertEquals(4, bidList.size());
        for (Bid bid : bidList) {
            assertEquals(2, bid.getBidder_user_id());
            assertEquals("test2", bid.getBidder_user_username());
            assertEquals(4, bid.getAuction_id());
        }

        assertEquals(130.0, bidList.get(0).getAmount());
        assertEquals(120.0, bidList.get(1).getAmount());
        assertEquals(110.0, bidList.get(2).getAmount());
        assertEquals(100.0, bidList.get(3).getAmount());
    }

}
