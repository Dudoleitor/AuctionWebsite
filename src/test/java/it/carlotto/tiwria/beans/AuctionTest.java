package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import jakarta.servlet.UnavailableException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    @Test
    void bidsTest() throws UnavailableException {
        List<Auction> auctionList = Auction.getAuctions(false);
        Auction auction = auctionList.stream().filter(x -> x.getId()==6).findFirst().get();

        assertEquals(2, auction.getCreator_user_id());
        List<Auction> auctionList2 = Auction.getAuctions(true);
    }

    @Test
    void auctionsPerUserTest() throws UnavailableException {
        List<Auction> auctionList = Auction.getAuctionsPerUser(2, true);
        assertEquals(1, auctionList.size());

        Auction auction = auctionList.get(0);
        assertEquals(2, auction.getMinimum_bid_wedge());
        assertTrue(auction.isClosed_by_user());
        List<Auction> auctionList2 = Auction.getAuctionsPerUser(2, true);
    }

    @Test
    void getAuction() throws ObjectNotFoundException, UnavailableException {
        Auction auction = Auction.getAuctionFromId(6);
        assertEquals(2, auction.getCreator_user_id());
        assertEquals("test2", auction.getCreator_user_username());
        assertTrue(auction.isClosed_by_user());
        assertEquals(2, auction.getMinimum_bid_wedge());

        assertThrows(ObjectNotFoundException.class, ()->Auction.getAuctionFromId(1000));
    }

    @Test
    void auctionsWithSearchTest() throws UnavailableException {
        List<Auction> auctionList1= Auction.getAuctionsWithSearch("test", false);
        List<Auction> auctionList2= Auction.getAuctionsWithSearch("test", true);
    }

    @Test
    void wonAuctionTest() throws ObjectNotFoundException, UnavailableException {
        Auction auction = Auction.getAuctionFromId(8);
        List<Auction> auctionList = new ArrayList<>();
        auctionList.add(auction);
        assertFalse(auction.isOpen());

        AuctionClosed auctionClosed = AuctionClosed.getAuctionsTerminatedFromAuctions(auctionList).get(0);
        assertEquals(auction.getId(), auctionClosed.getId());
        assertEquals(740.0, auctionClosed.getFinal_bid_amount());
        assertEquals(7, auctionClosed.getWinner_user_id());
        assertEquals("prova1", auctionClosed.getWinner_user_username());
    }

    @Test
    void maxBidTest() throws UnavailableException {
        assertEquals(130.0, Auction.getMaxBidFromAuctionId(4));
        assertEquals(0.0, Auction.getMaxBidFromAuctionId(6));
    }
}
