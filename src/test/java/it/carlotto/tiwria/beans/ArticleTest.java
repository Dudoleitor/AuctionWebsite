package it.carlotto.tiwria.beans;

import jakarta.servlet.UnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArticleTest {
    @Test
    void byAuctionTest() throws UnavailableException {
        List<Article> articleList = Article.getArticlesByAuction(6);

        assertEquals(2, articleList.size());

        assertEquals(2, articleList.get(0).getOwner_user_id());
        assertEquals(4, articleList.get(0).getId());
        assertEquals(120.0, articleList.get(0).getBase_price());
        assertEquals("testtest", articleList.get(0).getName());
        assertTrue(articleList.get(0).getDescription().isEmpty());

        assertEquals(2, articleList.get(1).getOwner_user_id());
        assertEquals(6, articleList.get(1).getId());
        assertEquals(125.0, articleList.get(1).getBase_price());
        assertEquals("testone", articleList.get(1).getName());
        assertEquals("exampledesc", articleList.get(1).getDescription());
    }

    @Test
    void allArticlesByUserTest() throws UnavailableException {
        List<Article> articleList = Article.getArticlesByUser(2, false);

        assertEquals(3, articleList.size());

        assertEquals(2, articleList.get(0).getOwner_user_id());
        assertEquals(4, articleList.get(0).getId());
        assertEquals(120.0, articleList.get(0).getBase_price());
        assertEquals(6, articleList.get(0).getAuction_id());
        assertEquals("testtest", articleList.get(0).getName());
        assertTrue(articleList.get(0).getDescription().isEmpty());

        assertEquals(2, articleList.get(1).getOwner_user_id());
        assertEquals(6, articleList.get(1).getId());
        assertEquals(125.0, articleList.get(1).getBase_price());
        assertEquals(6, articleList.get(0).getAuction_id());
        assertEquals("testone", articleList.get(1).getName());
        assertEquals("exampledesc", articleList.get(1).getDescription());
    }

    @Test
    void avalArticlesByUserTest() throws UnavailableException {
        List<Article> articleList = Article.getArticlesByUser(2, true);

        assertEquals(1, articleList.size());

        assertEquals(2, articleList.get(0).getOwner_user_id());
        assertEquals(14, articleList.get(0).getId());
        assertEquals(20.0, articleList.get(0).getBase_price());
        assertEquals(0, articleList.get(0).getAuction_id());
        assertEquals("FreeTest", articleList.get(0).getName());
        assertEquals("It's free", articleList.get(0).getDescription());
    }
}
