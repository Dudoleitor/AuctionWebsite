package it.carlotto.tiwria.beans;

import it.carlotto.tiwria.exceptions.InsertIntoDBFailedException;
import it.carlotto.tiwria.exceptions.ObjectNotFoundException;
import it.carlotto.tiwria.utils.ConnectionsHandler;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class Article {
    private final int id;
    private final int owner_user_id;
    private final float base_price;
    private final int auction_id;  // =0 if the field is NULL in the DB
    private final String name;
    private final String description;

    private final String imageBase64;

    private Article(int id, int owner_user_id, float base_price, int auction_id, String name, String description, String image_file_name) {
        this.id = id;
        this.owner_user_id = owner_user_id;
        this.base_price = base_price;
        this.auction_id = auction_id;
        this.name = name;
        if (description!=null)
            this.description = description;
        else this.description = "";
        this.imageBase64 = encodeImage(image_file_name);
    }

    public int getId() {
        return id;
    }

    public int getOwner_user_id() {
        return owner_user_id;
    }

    public float getBase_price() {
        return base_price;
    }

    public int getAuction_id() {
        return auction_id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return id == article.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * This function is used to load the list of articles
     * relative to the same auction.
     * @param auction_id ID of the auction
     * @return List of articles
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Article> getArticlesByAuction(int auction_id) throws UnavailableException {
        String query = "SELECT id, owner_user_id, base_price, auction_id, name, description, img_file_name FROM articles WHERE auction_id = ?";
        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
            ) {
            preparedStatement.setInt(1, auction_id);

            return loadArticlesUsingPS(preparedStatement);

        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading articles from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load the list of articles
     * owned by the same user.
     * @param userId ID of the user
     * @param onlyAvailable if True only articles with no auction assigned
     *          are returned
     * @return List of articles
     * @throws UnavailableException when it's not possible to get a SQL connection
     *      or an SQL error happens.
     */
    public static List<Article> getArticlesByUser(int userId, boolean onlyAvailable) throws UnavailableException {
        String query = "SELECT id, owner_user_id, base_price, auction_id, name, description, img_file_name FROM articles WHERE owner_user_id = ?";
        if(onlyAvailable)
            query += " AND auction_id IS NULL";

        Connection connection = ConnectionsHandler.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            preparedStatement.setInt(1, userId);
            return loadArticlesUsingPS(preparedStatement);
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while loading articles from DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load a list of Articles after a PreparedStatement
     * has been prepared.
     * @param preparedStatement PreparedStatement to execute the SQL query.
     * @return List of Article object
     * @throws SQLException
     */
    protected static List<Article> loadArticlesUsingPS(PreparedStatement preparedStatement) throws SQLException {
        List<Article> articlesList = new ArrayList<>();
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            String description;
            while (resultSet.next()) {
                description = resultSet.getString("description");
                if (description==null)
                    description="";

                articlesList.add(new Article(
                        resultSet.getInt("id"),
                        resultSet.getInt("owner_user_id"),
                        resultSet.getFloat("base_price"),
                        resultSet.getInt("auction_id"),
                        resultSet.getString("name"),
                        description,
                        resultSet.getString("img_file_name")
                ));
            }
        }
        return articlesList;
    }

    /**
     * This function is used to add a new article into the DB.
     * @param owner_user_id Int ID of the owner user
     * @param base_price Float base price
     * @param name String name
     * @param description String description, optional (accepted both null o empty string)
     * @param imagePart file Part to be used as image, optional (accepted null)
     * @throws InsertIntoDBFailedException when an SQL Exception is thrown
     *          while attempting to execute the query
     * @throws UnavailableException when an SQL Exception is thrown while
     *          attempting to prepare the PreparedStatement.
     */
    public static void createArticle(int owner_user_id, float base_price, String name, String description, Part imagePart) throws InsertIntoDBFailedException, UnavailableException {
        final int article_id;

        // Updating DB: adding article
        final String query = "INSERT INTO articles (owner_user_id, base_price, name, description) VALUES (?, ?, ?, ?)";
        final Connection connection = ConnectionsHandler.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)){
            preparedStatement.setInt(1, owner_user_id);
            preparedStatement.setFloat(2, base_price);
            preparedStatement.setString(3, name);
            if (description!=null && !description.isEmpty())
                preparedStatement.setString(4, description);
            else preparedStatement.setString(4, null);

            try {
                preparedStatement.executeUpdate();
            } catch (SQLException e) {  // Error occurred while executing query
                throw new InsertIntoDBFailedException("SQL Exception while inserting article into DB: " + e.getMessage());
            }

            // Obtaining the ID of the new item
            try(ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.isBeforeFirst()) {  // Otherwise no row was updated
                    resultSet.next();
                    article_id = resultSet.getInt(1);
                } else {
                    throw new InsertIntoDBFailedException("No row was added while adding article");
                }
            }
        } catch (SQLException e) {  // Error occurred while preparing statement
            throw new UnavailableException("SQL Exception preparing statement to adding article into DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }

        // Saving image into folder
        final String imageExtension;
        if(imagePart==null) return; // No image need to be saved
        switch (imagePart.getContentType()) {
            case "image/jpeg":
                imageExtension = ".jpg";
                break;
            case "image/png":
                imageExtension = ".png";
                break;
            case "image/gif":
                imageExtension = ".gif";
                break;
            default:
                imageExtension = "";

        }

        final String outputPath = System.getenv("imgFolder") + article_id + imageExtension;
        try (InputStream imageContent = imagePart.getInputStream()) {
            Files.copy(imageContent, Paths.get(outputPath));
        } catch (IOException e) {
            throw new UnavailableException("IO Exception while trying to save article image: " + e.getMessage());
        }

        // Updating DB: adding image file name
        final String updateQuery = "UPDATE articles SET img_file_name = ? WHERE id = ?";
        Connection updateConnection = ConnectionsHandler.getConnection();
        try (PreparedStatement preparedStatement = updateConnection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, article_id + imageExtension);
            preparedStatement.setInt(2, article_id);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new InsertIntoDBFailedException("SQL Exception while updating article into DB: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(updateConnection);
        }

    }

    /**
     * This function updates the properties of multiple articles in order
     * to add them to an auction.
     * These checks are performed before attempting to update:
     * (1) The auction does not have bids
     * (2) Each article is not already part of an auction
     * This functions receives a Connection object because supposes that
     * autocommit is disabled.
     * @param connection Connection object
     * @param auction_id Int ID of the auction
     * @param articles_id List of Int, IDs of the articles
     * @throws InsertIntoDBFailedException when checks do not pass or
     *          an SQL error happens.
     */
    protected static void addArticlesToAuction(Connection connection, int auction_id, List<Integer> articles_id) throws InsertIntoDBFailedException {
        // Making sure the auction does not have bids placed
        String queryCheck1 = "SELECT id FROM bids WHERE auction_id = ?";

        // Making sure the article isn't already linked to an auction
        String queryCheck2 = "SELECT id FROM articles WHERE id = ? AND auction_id IS NOT NULL";

        // Updating the article
        String queryUpdate = "UPDATE articles SET auction_id = ? WHERE id = ?";


        try(
                PreparedStatement preparedStatement1 = connection.prepareStatement(queryCheck1);
                PreparedStatement preparedStatement2 = connection.prepareStatement(queryCheck2);
                PreparedStatement preparedStatement3 = connection.prepareStatement(queryUpdate)){
            preparedStatement1.setInt(1, auction_id);
            preparedStatement3.setInt(1, auction_id);

            try(ResultSet resultSet1 = preparedStatement1.executeQuery()) {
                if (resultSet1.isBeforeFirst()) { // Item found, check failed
                    throw new InsertIntoDBFailedException("Error while adding article to auction: auction has bids");
                }
            }

            boolean checksFailed = false;
            int article_id;
            for (int k = 0; k<articles_id.size() && !checksFailed; k++) {
                article_id = articles_id.get(k);
                preparedStatement2.setInt(1, article_id);
                preparedStatement3.setInt(2, article_id);

                try (ResultSet resultSet2 = preparedStatement2.executeQuery()) {
                    if (!resultSet2.isBeforeFirst()) {  // No item found, checks passed
                        preparedStatement3.executeUpdate();
                    } else {
                        checksFailed = true;
                    }
                }
            }
            if (checksFailed) {
                throw new InsertIntoDBFailedException("Some checks did not pass before updating article");
            }
        } catch (SQLException e) {
            throw new InsertIntoDBFailedException("SQL Exception while updating article into DB: " + e.getMessage());
        }
    }

    /**
     * This function checks whether each article related to the provided IDs
     * belongs to the specified owner.
     * @param user_id ID of the supposed owner
     * @param articles_id List of Int, articles to check
     * @return True if each articles belongs to the user,
     *          False if an article belongs to someone else
     * @throws ObjectNotFoundException when an article is not found
     * @throws UnavailableException when it's not possible to get an SQL connection
     *          or an SQL error happens.
     */
    public static boolean checkOwner(int user_id, List<Integer> articles_id) throws ObjectNotFoundException, UnavailableException {
        String query = "SELECT owner_user_id FROM articles WHERE id = ?";
        Connection connection = ConnectionsHandler.getConnection();

        try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int article_id : articles_id) {
                preparedStatement.setInt(1, article_id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if(!resultSet.isBeforeFirst())  // Article not found
                        throw new ObjectNotFoundException("Article not found");

                    resultSet.next();

                    if(resultSet.getInt("owner_user_id") != user_id)
                        return false;
                }
            }
            return true;
        } catch (SQLException e) {
            throw new UnavailableException("SQL Exception while checking for articles owner: " + e.getMessage());
        } finally {
            ConnectionsHandler.returnConnection(connection);
        }
    }

    /**
     * This function is used to load an image from file and encode it
     * into a base64 string.
     * @param img_file_name String name of the file
     * @return String image encoded in base64
     */
    private static String encodeImage(String img_file_name) {
        if (img_file_name==null || img_file_name.isEmpty()) return "";

        final String path = System.getenv("imgFolder");

        String base64Head = "data:image/";

        final String[] extensionFromSplit = img_file_name.split("\\.",2);
        if(extensionFromSplit.length!=2) return "";
        final String extension = extensionFromSplit[1];

        String extensionBase64 = "";
                // Matching extension to attribute for base64
        switch (extension) {
            case "jpg":
            case "JPG":
            case "jpeg":
            case "JPEG":
                extensionBase64 = "jpeg";
                break;
            case "png":
            case "PNG":
                extensionBase64 = "png";
                break;
            case "gif":
            case "GIF":
                extensionBase64 = "gif";
                break;
        }

        // Early return if the extension is unknown
        if (extensionBase64.isEmpty()) return "";
        base64Head += extensionBase64 + ";base64,";

        File imageFile = new File(path + img_file_name);
        // Early return if the file does not exist
        if (!imageFile.exists()) return "";

        // Loading bytes
        byte[] imageData = new byte[(int) imageFile.length()];

        try (FileInputStream imageInFile = new FileInputStream(imageFile)){
            imageInFile.read(imageData);

            // Encoding and returning
            return base64Head + Base64.getEncoder().encodeToString(imageData);
        } catch (Exception e) {
            return "";
        }

    }

    /**
     * This function is used by servlets to determine if the content type
     * of a file is allowed to be used as image of an article.
     * @param content_type String filePart.getContentType()
     * @return True if allowed
     */
    public static boolean allowedContentType(String content_type) {
        return Objects.equals(content_type, "image/jpeg") ||
                Objects.equals(content_type, "image/png") ||
                Objects.equals(content_type, "image/gif");
    }
}
