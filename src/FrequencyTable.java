import Jama.Matrix;
import Jama.SingularValueDecomposition;
import libstemmer_java.java.org.tartarus.snowball.ext.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.*;
import java.net.SocketOption;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FrequencyTable {
    private static englishStemmer stemmer = new englishStemmer();

    static String userName = "root";
    static String password = "1234";
    static String url = "jdbc:mariadb://localhost:3306/docdb";
    static String driver = "org.mariadb.jdbc.Driver";
    static Connection conn = null;
    static int numDoc = 1;
    static int numQuery = 1;
    static int currentNumQueries = 0;
    static int numRelevantDocs;
    static double distance;
    static int doc1;
    static int doc2;
    static HashMap<String, Integer> termFreq;

    static String[] stopWords = new String[]{"a", "an", "the"};

    static Matrix originalFreqTable;

    static Matrix reducedAfterDelRows;
    static double[][] frequencyTable;

    static double[][] queryMatrix;

    static String query;

    static Matrix queryBeforeSVD;

    static Matrix queryAfterSVD;
    static List<String> terms = new ArrayList<>();
    static List<String> documents = new ArrayList<>();



    public static void main(String[] args) throws IOException, SQLException {

        termFreq = new HashMap<>(); // stores the frequency of each term
        
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to the document base!");
        System.out.println("---------------------------------------");

        boolean running = true;
        int choice;
        int simOption;
        int choiceAfterSVD;
        int simOptionAfterSVD;
        int disOptionAfterSVD;
        int simOrDisAfterSVD;

        int rowsToEliminate;

        connectionDB();

        while (running) {
            getNumberDocsAndQueries();
            System.out.println("\nWhat would you like to do?");
            System.out.println("1. Add a document to the database");
            System.out.println("2. Perform a query using dissimilarity functions");
            System.out.println("3. Perform a query using similarity functions");
            System.out.println("4. Give a Q to obtain N most relevant docs");
            System.out.println("5. Apply SVD");
            System.out.println("6. Exit");
            System.out.println("7. Erase all the content of the database");
            System.out.print("Enter your choice: ");

            choice = scanner.nextInt();
            scanner.nextLine(); // consume newline character

            switch (choice) {
                case 1:
                    readDocument();
                    break;
                case 2:
                    System.out.println("1.Euclidean Distance");
                    System.out.println("2.Manhattan Distance");
                    System.out.print("Enter your option: ");
                    simOption = scanner.nextInt();
                    System.out.print("Give me the id of the first doc: ");
                    doc1 = scanner.nextInt();
                    System.out.print("Give me the id of the other doc: ");
                    doc2 = scanner.nextInt();
                    switch(simOption){
                        case 1:

                            distance = euclideanDistance(doc1,doc2);
                            break;
                        case 2:
                            distance = manhattanDistance(doc1, doc2);
                            break;
                    }
                    break;
                case 3:
                    System.out.println("1.Cosine");
                    System.out.println("2.Dice coefficient");
                    System.out.print("Enter your option: ");
                    simOption = scanner.nextInt();
                    System.out.print("Give me the id of the first doc: ");
                    doc1 = scanner.nextInt();
                    System.out.print("Give me the id of the other doc: ");
                    doc2 = scanner.nextInt();
                    switch(simOption){
                        case 1:
                            distance = cosine(doc1,doc2);
                            break;
                        case 2:
                            distance = diceCoefficient(doc1,doc2);
                            break;
                    }
                    break;
                case 4:
                    recoverFrequencyTable();
                    originalFreqTable = new Matrix(frequencyTable);
                    System.out.println("\nGive me a query Q: ");
                    query = scanner.nextLine();
                    System.out.println("\nEstablish the n relevant docs to be obtained: ");
                    numRelevantDocs = scanner.nextInt();
                    insertQuery(stemQuery(query));
                    currentNumQueries++;
                    numQuery++;
                    recoverQueryMatrix();
                    queryBeforeSVD = new Matrix(queryMatrix);
                    System.out.println("1.Euclidean Distance: ");
                    System.out.println("2.Manhattan Distance: ");
                    System.out.println("3.Cosine: ");
                    System.out.println("4.Dice Coefficient: ");
                    System.out.println("Give me your option: ");
                    simOrDisAfterSVD = scanner.nextInt();
                    switch(simOrDisAfterSVD){
                        case 1:
                            System.out.println("Euclidean Distance before SVD");
                            euclideanDistanceQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                            break;
                        case 2:
                            System.out.println("Manhattan Distance  before SVD");
                            manhattanDistanceQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                            break;
                        case 3:
                            System.out.println("Cosine before SVD");
                            cosineQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                            break;
                        case 4:
                            System.out.println("Dice coefficient before SVD");
                            diceCoefficientQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                            break;
                    }
                    break;
                case 5:
                    recoverFrequencyTable();
                    originalFreqTable = new Matrix(frequencyTable);
                    System.out.println("\nApply SVD ");
                    System.out.println("\nHow many relevant terms you want to maintain k: ");
                    rowsToEliminate = scanner.nextInt();
                    printFreqTable();
                    performSVD(rowsToEliminate);

                    System.out.println("1. Perform a query using dissimilarity functions");
                    System.out.println("2. Perform a query using similarity functions");
                    System.out.println("3. Give a Q' to obtain N most relevant docs");

                    choiceAfterSVD = scanner.nextInt();

                    switch (choiceAfterSVD) {
                        case 1:
                            System.out.println("1.Euclidean Distance in the new FreqTable");
                            System.out.println("2.Manhattan Distance in the new FreqTable");
                            System.out.print("Enter your option: ");
                            simOptionAfterSVD = scanner.nextInt();
                            System.out.print("Give me the id of the first doc: ");
                            doc1 = scanner.nextInt();
                            System.out.print("Give me the id of the other doc: ");
                            doc2 = scanner.nextInt();

                            switch(simOptionAfterSVD){
                                case 1:
                                    System.out.print("Euclidean Distance before SVD: ");
                                    distance = euclideanDistance(doc1,doc2);
                                    System.out.print("Euclidean Distance after SVD: ");
                                    distance = euclideanDistAfterSVDDocs(reducedAfterDelRows, doc1 - 1, doc2 - 1);
                                    System.out.print(distance);
                                    break;
                                case 2:
                                    System.out.print("Manhattan Distance before SVD: ");
                                    distance = manhattanDistance(doc1,doc2);
                                    System.out.print("Manhattan after SVD: ");
                                    distance = manhattanDistanceAfterSVDDocs(reducedAfterDelRows, doc1 - 1, doc2 - 1);
                                    System.out.print(distance);
                                    break;
                            }
                            break;
                        case 2:
                            System.out.println("1.Cosine");
                            System.out.println("2.Dice coefficient");
                            System.out.print("Enter your option: ");
                            disOptionAfterSVD = scanner.nextInt();
                            System.out.print("Give me the id of the first doc: ");
                            doc1 = scanner.nextInt();
                            System.out.print("Give me the id of the other doc: ");
                            doc2 = scanner.nextInt();

                            switch(disOptionAfterSVD){
                                case 1:
                                    System.out.print("Cosine before SVD: ");
                                    distance  = cosine(doc1, doc2);
                                    System.out.print("Cosine after SVD: ");
                                    distance  = cosineAfterSVDDocs(reducedAfterDelRows, doc1 - 1, doc2 - 1);
                                    System.out.print(distance);
                                    break;
                                case 2:
                                    System.out.print("Dice coefficient before SVD: ");
                                    distance  = diceCoefficient(doc1, doc2);
                                    System.out.print("Dice coefficient after SVD: ");
                                    distance = diceCoeffAfterSVD(reducedAfterDelRows, doc1 - 1, doc2 - 1);
                                    System.out.println(distance);
                                    break;
                            }
                            break;
                        case 3:
                            scanner.nextLine();
                            System.out.println("\nGive me a query Q: ");
                            query = scanner.nextLine();
                            System.out.println("\nEstablish the n relevant docs to be obtained: ");
                            numRelevantDocs = scanner.nextInt();
                            insertQuery(stemQuery(query));
                            currentNumQueries++;
                            numQuery++;
                            recoverQueryMatrix();
                            queryBeforeSVD = new Matrix(queryMatrix);
                            System.out.println("1.Euclidean Distance: ");
                            System.out.println("2.Manhattan Distance: ");
                            System.out.println("3.Cosine: ");
                            System.out.println("4.Dice Coefficient: ");
                            System.out.println("Give me your option: ");
                            simOrDisAfterSVD = scanner.nextInt();
                            querySVD();
                            switch(simOrDisAfterSVD){
                                case 1:
                                    System.out.println("Euclidean Distance before SVD");
                                    euclideanDistanceQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                                    System.out.println("Euclidean Distance after SVD");
                                    euclideanDistanceQuery(queryAfterSVD, originalFreqTable, numRelevantDocs);
                                    break;
                                case 2:
                                    System.out.println("Manhattan Distance  before SVD");
                                    manhattanDistanceQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                                    System.out.println("Manhattan Distance after SVD");
                                    manhattanDistanceQuery(queryAfterSVD, originalFreqTable, numRelevantDocs);
                                    break;
                                case 3:
                                    System.out.println("Cosine before SVD");
                                    cosineQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                                    System.out.println("Cosine after SVD");
                                    cosineQuery(queryAfterSVD, originalFreqTable, numRelevantDocs);
                                    break;
                                case 4:
                                    System.out.println("Dice coefficient before SVD");
                                    diceCoefficientQuery(queryBeforeSVD,originalFreqTable, numRelevantDocs);
                                    System.out.println("Dice coefficient after SVD");
                                    diceCoefficientQuery(queryAfterSVD, originalFreqTable, numRelevantDocs);
                                    break;
                            }
                            break;
                    }
                    break;
                case 6:
                    System.out.println("\nExiting the program. Goodbye!");
                    conn.close();
                    running = false;
                    break;
                case 7:
                    System.out.println("\nEliminating documents...");
                    deleteAndCreateTables();
                    break;
                default:
                    System.out.println("\nInvalid choice. Please try again.");
            }
        }
        scanner.close();
    }
    public static void connectionDB() {
        try {
            Class.forName(driver);

            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(url,userName, password);

            System.out.println("Connected to database.");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    public static void readDocument() throws IOException, SQLException {
        int frequency;

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the filename: ");
        String filename = scanner.nextLine();
        File inputFile = new File(filename);

        if (!inputFile.exists()) {
            System.out.println("Error: File not found.");
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line;
        int lineCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        String title = "";
        String author = "";
        Date releaseDate = null;
        String editorial = "";
        String place = "";

        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (lineCount == 1) {
                title = line;
            } else if (lineCount == 2) {
                author = line;
            } else if (lineCount == 3) {
                releaseDate = Date.valueOf(line);
            } else if (lineCount == 4) {
                editorial = line;
            } else if (lineCount == 5) {
                place = line;
            } else {
                stringBuilder.append(line).append("\n");
            }
        }
        reader.close();

        String text = stringBuilder.toString();
        String[] lines = text.split("\\r?\\n");

        for (String linex : lines) {
            String[] words = linex.split("\\s+");
            for (String word : words) {
                if (Arrays.asList(stopWords).contains(word)) {
                    continue; // skip stop words
                }
                stemmer.setCurrent(word);
                if (stemmer.stem() == false) {
                    termFreq.put(word, termFreq.getOrDefault(word, 0) + 1); // increment frequency of original word
                    System.out.print(word + " ");
                } else {
                    String stemmedWord = stemmer.getCurrent();
                    termFreq.put(stemmedWord, termFreq.getOrDefault(stemmedWord, 0) + 1); // increment frequency of stemmed word
                    System.out.print(stemmedWord + " ");
                }
            }
        }
            addDocument(title, author, releaseDate, editorial, place);

        for (HashMap.Entry<String, Integer> entry : termFreq.entrySet()) {
            String termName = entry.getKey();
            frequency = entry.getValue();
            addTerm(termName); // add term to Term table if it doesn't exist
            addAppears(termName, frequency);
        }
        termFreq.clear();
        numDoc++;
    }

    public static void addDocument(String title, String author, Date releaseDate, String editorial, String place) throws SQLException {
        String sql = "INSERT INTO Document (doc_id, title, author, release_date, editorial, place) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1,numDoc);
        statement.setString(2, title);
        statement.setString(3, author);
        statement.setDate(4, (java.sql.Date) releaseDate);
        statement.setString(5, editorial);
        statement.setString(6, place);

        int rowsInserted = statement.executeUpdate();
        if (rowsInserted > 0) {
            System.out.println("\nA new document was added successfully to the db!");
        }
        statement.close();
    }

    public static void addTerm(String term) throws SQLException {
        String sql = "INSERT IGNORE INTO Term (name) VALUES (?)";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, term);
        statement.executeUpdate();
        statement.close();
        }

    public static void addAppears(String term,int frequency) throws SQLException {
        String sql = "INSERT INTO Appears (doc_id, name, frequency) VALUES (?, ?, ?)";
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1,numDoc);
        statement.setString(2, term);
        statement.setInt(3,frequency);
        statement.executeUpdate();
        statement.close();
    }

    public static double euclideanDistance(int doc1, int doc2) throws SQLException{
        double distance = 0;

        String sql = "SELECT SQRT(SUM( POW (td.frequency - tq.frequency, 2))) AS distance "
                + "FROM Appears AS tq, Appears AS td, Document AS d "
                + "WHERE td.name = tq.name and d.doc_id = tq.doc_id and td.doc_id = (?) and tq.doc_id = (?) "
                + "GROUP BY d.doc_id;";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, doc1);
        statement.setInt(2, doc2);

        // Execute the query and retrieve the result set
        ResultSet result = statement.executeQuery();

        // Extract the distance from the result set
        if (result.next()) {
            distance = result.getDouble("distance");
            System.out.println(distance);
        }
        statement.close();

        return distance;
    }

    public static double manhattanDistance(int doc1, int doc2) throws SQLException{
        double distance = 0;
        System.out.println(doc1 + ""+ doc2);
        String sql = "SELECT SUM(ABS (td.frequency - tq.frequency)) AS distance "
                + "FROM Appears AS tq, Appears AS td, Document AS d "
                + "WHERE td.name = tq.name and d.doc_id = tq.doc_id and td.doc_id = (?) and tq.doc_id = (?) "
                + "GROUP BY d.doc_id;";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, doc1);
        statement.setInt(2, doc2);

        // Execute the query and retrieve the result set
        ResultSet result = statement.executeQuery();

        // Extract the distance from the result set
        if (result.next()) {
            distance = result.getDouble("distance");
            System.out.println(distance);
        }
        statement.close();

        return distance;
    }

    public static double cosine(int doc1, int doc2)throws SQLException{
        double distance = 0;

        String sql = "SELECT SUM(tq.frequency * td.frequency) / (SQRT(SUM(POW(tq.frequency, 2))) * SQRT(SUM(POW(td.frequency, 2)))) AS distance "
                    + "FROM Appears AS tq, Appears AS td, Document AS d "
                    + "WHERE td.name = tq.name AND d.doc_id = tq.doc_id AND td.doc_id = (?) AND tq.doc_id = (?) "
                    + "GROUP BY d.doc_id;";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, doc1);
        statement.setInt(2, doc2);

        // Execute the query and retrieve the result set
        ResultSet result = statement.executeQuery();

        // Extract the distance from the result set
        if (result.next()) {
            distance = result.getDouble("distance");
            System.out.println(distance);
        }
        statement.close();
        return distance;
    }


    public static double diceCoefficient(int doc1, int doc2)throws SQLException{
        double distance = 0;

        String sql = "SELECT (2 * SUM(tq.frequency * td.frequency)) / (SUM(POW(tq.frequency, 2)) + SUM(POW(td.frequency, 2))) AS dice_coefficient" +
        " FROM Appears AS tq, Appears AS td "+
        " WHERE td.name = tq.name AND td.doc_id = ? AND tq.doc_id = ? "+
        "GROUP BY td.doc_id";

        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, doc1);
        statement.setInt(2, doc2);

        ResultSet result = statement.executeQuery();

        if (result.next()) {
            distance = result.getDouble("dice_coefficient");
            System.out.println(distance);
        }
        statement.close();
        return distance;
    }

    public static HashMap<String, Integer> stemQuery(String inputString) {
        // Initialize word count map
       HashMap<String, Integer> wordCountMap = new HashMap<>();

        // Split input string into words
        String[] words = inputString.split("\\s+");

        // Iterate through words
        for (String word : words) {
            // Stem word using Snowball
            stemmer.setCurrent(word);
            stemmer.stem();
            String stemmedWord = stemmer.getCurrent();

            // Check if stemmed word is a stop word
            if (Arrays.asList(stopWords).contains(stemmedWord)) {
                continue;
            }

            // Update word count map
            int count = wordCountMap.getOrDefault(stemmedWord, 0);
            wordCountMap.put(stemmedWord, count + 1);
        }
        return wordCountMap;
    }
    public static void insertQuery(HashMap<String, Integer> wordCountMap) throws SQLException {
        // Prepare SQL statement for inserting into Query table
        String sql = "INSERT INTO Query (num_q, word, freq_w) VALUES (?, ?, ?)";
        PreparedStatement statement = conn.prepareStatement(sql);

        // Iterate through word count map
        for (HashMap.Entry<String, Integer> entry : wordCountMap.entrySet()) {
            // Get stemmed word and frequency count
            String word = entry.getKey();
            int freq = entry.getValue();

            // Insert into Query table
            statement.setInt(1, numQuery);
            statement.setString(2, word);
            statement.setInt(3, freq);
            statement.executeUpdate();
        }
        statement.close();
    }

    public static void getNumberDocsAndQueries() throws SQLException{
        String sql = "SELECT COUNT(*) FROM Document";
        PreparedStatement statement = conn.prepareStatement(sql);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            numDoc = result.getInt(1);
            System.out.println("\nThe document database has " + numDoc + " docs stored.");
            numDoc++;
        }

        sql = "SELECT COUNT(DISTINCT num_q) FROM Query";
        statement = conn.prepareStatement(sql);
        result = statement.executeQuery();
        if (result.next()) {
            numQuery = result.getInt(1);
            System.out.println("The document database has " + numQuery + " queries stored.");
            numQuery++;
        }
    }

    public static void deleteAndCreateTables() throws SQLException {
        // Delete the tables
        Statement deleteStmt = conn.createStatement();
        deleteStmt.executeUpdate("DROP TABLE Appears");
        deleteStmt.executeUpdate("DROP TABLE Query");
        deleteStmt.executeUpdate("DROP TABLE Term");
        deleteStmt.executeUpdate("DROP TABLE Document");

        // Create the tables
        Statement createStmt = conn.createStatement();
        createStmt.executeUpdate("CREATE TABLE Document (doc_id INT NOT NULL, title VARCHAR(100) NOT NULL, author VARCHAR(100) NOT NULL, release_date date NOT NULL, editorial VARCHAR(100), place VARCHAR(100), PRIMARY KEY (doc_id))");
        createStmt.executeUpdate("CREATE TABLE Term (name VARCHAR(100) NOT NULL, PRIMARY KEY (name), CONSTRAINT unique_name_term UNIQUE (name))");
        createStmt.executeUpdate("CREATE TABLE Appears (doc_id INT NOT NULL, name VARCHAR(100) NOT NULL, frequency INT, PRIMARY KEY (doc_id, name), FOREIGN KEY (doc_id) REFERENCES Document(doc_id), FOREIGN KEY (name) REFERENCES Term(name))");
        createStmt.executeUpdate("CREATE TABLE Query (num_q INT NOT NULL, word VARCHAR(100) NOT NULL, freq_w INT)");

        System.out.println("Delete and create tables successfully");
    }

    public static void recoverFrequencyTable() throws SQLException{
        terms.clear();
        documents.clear();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT DISTINCT name FROM Term");

        while (rs.next()) {
            terms.add(rs.getString("name"));
        }

        rs = stmt.executeQuery("SELECT DISTINCT doc_id FROM Document");

        while (rs.next()) {
            documents.add(rs.getString("doc_id"));
        }

        frequencyTable = new double[terms.size()][documents.size()];

        for (int i = 0; i < terms.size(); i++) {
            for (int j = 0; j < documents.size(); j++) {
                rs = stmt.executeQuery("SELECT frequency FROM Appears " +
                        "WHERE name = '" + terms.get(i) + "' AND doc_id = " + documents.get(j));

                if (rs.next()) {
                    frequencyTable[i][j] = rs.getInt("frequency");
                } else {
                    frequencyTable[i][j] = 0;
                }
            }

            // Set frequency to 0 for terms that don't appear in a document
            for (int j = 0; j < documents.size(); j++) {
                if (frequencyTable[i][j] == 0) {
                    boolean termExists = false;
                    rs = stmt.executeQuery("SELECT * FROM Appears " +
                            "WHERE name = '" + terms.get(i) + "' AND doc_id = " + documents.get(j));
                    if (rs.next()) {
                        termExists = true;
                    }
                    if (!termExists) {
                        frequencyTable[i][j] = 0;
                    }
                }
            }
        }

        stmt.close();
    }

    public static void printFreqTable(){
        // Print the frequency table
        System.out.print("\t\t");
        for (String doc : documents) {
            System.out.print(doc + "\t");
        }
        System.out.println();

        for (int i = 0; i < terms.size(); i++) {
            System.out.print(terms.get(i) + "\t");
            for (int j = 0; j < documents.size(); j++) {
                System.out.print(frequencyTable[i][j] + "  ");
            }
            System.out.println();
        }
    }

    public static void recoverQueryMatrix() throws SQLException{
        queryMatrix = new double[originalFreqTable.getRowDimension()][1];

        PreparedStatement statement = conn.prepareStatement("SELECT COALESCE((SELECT Query.freq_w FROM Query WHERE Query.num_q = ? AND Query.word = Term.name), 0) AS frequency FROM Term ORDER BY Term.name ASC");
        statement.setInt(1, numQuery - 1);
        ResultSet result = statement.executeQuery();
        int i = 0;
        while (result.next()) {
            queryMatrix[i][0] = result.getInt("frequency");
            i++;
        }

        for (i = 0; i < queryMatrix.length; i++) {
            for (int j = 0; j < queryMatrix[0].length; j++) {
                System.out.print(queryMatrix[i][j] + " ");
            }
            System.out.println();
        }
        statement.close();
    }


    //Code for SVD of Frequency Table -------------------------------------------------------------------------------------------------------------------------------
    public static void performSVD(int k){
        SingularValueDecomposition svd = originalFreqTable.svd();

        // Get the matrices U, S, and V
        Matrix U = svd.getU();
        Matrix S = svd.getS();
        Matrix V = svd.getV();
        System.out.println("U");
        U.print(6,2);
        System.out.println("S");
        S.print(6,2);
        System.out.println("V^T");
        V.transpose().print(6,2);

        Matrix U_truncated = U.getMatrix(0, U.getRowDimension() - 1, 0, k - 1);
        Matrix S_truncated = S.getMatrix(0, k - 1, 0, k - 1);
        Matrix V_truncated = V.getMatrix(0, V.getColumnDimension() - 1, 0, k-1);

        System.out.println("U truncated");
        U_truncated.print(6,2);
        System.out.println("S truncated");
        S_truncated.print(6,2);
        System.out.println("V^T truncated");
        V_truncated.transpose().print(6,2);

        // Reconstruct the original matrix using the truncated matrices
        Matrix reducedBeforeDelRows = U_truncated.times(S_truncated).times(V_truncated.transpose());
        reducedBeforeDelRows.print(6,2);
        reducedAfterDelRows  = reducedBeforeDelRows;
        //reducedAfterDelRows = reducedBeforeDelRows.getMatrix(0, k - 1, 0, reducedBeforeDelRows.getColumnDimension() -1);

        // Print the original and reduced matrices
        System.out.println("Original matrix:");
        originalFreqTable.print(6, 2);
        System.out.println("Reduced matrix:");
        reducedAfterDelRows.print(6, 3);
    }

    public static double euclideanDistAfterSVDDocs(Matrix data, int doc1, int doc2) {
        double distance = 0;
        for (int i = 0; i < data.getRowDimension(); i++) {
            distance += Math.pow((data.get(i, doc1) - data.get(i, doc2)), 2);
        }
        return Math.sqrt(distance);
    }

    public static double manhattanDistanceAfterSVDDocs(Matrix data, int doc1, int doc2) {
        double distance = 0.0;
        for (int i = 0; i < data.getRowDimension(); i++) {
            distance += Math.abs(data.get(i, doc1) - data.get(i, doc2));
        }
        return distance;
    }

    public static double cosineAfterSVDDocs(Matrix matrix, int doc1, int doc2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            double a = matrix.get(i, doc1);
            double b = matrix.get(i, doc2);
            dotProduct += a * b;
            normA += Math.pow(a, 2);
            normB += Math.pow(b, 2);
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return similarity;
    }

    public static double diceCoeffAfterSVD(Matrix matrix, int doc1, int doc2) {
        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            double a = matrix.get(i, doc1);
            double b = matrix.get(i, doc2);
            numerator += 2 * a * b;
            denominator += Math.pow(a, 2) + Math.pow(b, 2);
        }
        double similarity = numerator / denominator;
        return similarity;
    }


    public static void querySVD() {
        SingularValueDecomposition svdQ = queryBeforeSVD.svd();
        SingularValueDecomposition svd = originalFreqTable.svd();

        // Get the matrices U, S, and V
        Matrix U = svd.getU();
        Matrix S = svd.getS();
        Matrix V = svd.getV();
        /*System.out.println("\nU");
        U.print(6,2);
        System.out.println("S");
        S.print(6,2);
        System.out.println("V^T");
        V.transpose().print(6,2);*/

        Matrix qU = queryBeforeSVD.transpose().times(U);
        int k  = 2;
        Matrix qUk = qU.getMatrix(0, 0, 0, k-1);

        // Compute the reduced query matrix Dq
        Matrix Ssub = S.getMatrix(0, k-1, 0, k-1);
        Matrix Vsub = V.getMatrix(0, V.getRowDimension()-1, 0, k-1);
        Matrix Dq = qUk.times(Ssub.inverse()).times(Vsub.transpose());

        Dq.print(6,2);

        // Compute the resulting query by multiplying the transpose of the original frequency table with Dq
        queryAfterSVD = (Dq.times(originalFreqTable.transpose())).transpose();

        // Print the results
        System.out.println("Resulting query:");
        queryAfterSVD.print(6, 3);
    }

    public static void euclideanDistanceQuery(Matrix query, Matrix freqTable, int n) {
        double[] distances = new double[freqTable.getColumnDimension()];
        for (int i = 0; i < freqTable.getColumnDimension(); i++) {
            Matrix column = freqTable.getMatrix(0, freqTable.getRowDimension() - 1, i, i);
            Matrix difference = column.copy();
            for (int j = 0; j < query.getRowDimension(); j++) {
                difference.set(j, 0, column.get(j, 0) - query.get(j, 0));
            }
            double distance = 0.0;
            for (int j = 0; j < difference.getRowDimension(); j++) {
                double value = difference.get(j, 0);
                distance += value * value;
            }
            distances[i] = Math.sqrt(distance);
        }

        // get the indices of the n columns with the smallest distances
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            indices.add(i);
        }
        indices.sort((i, j) -> Double.compare(distances[i], distances[j]));
        List<Integer> topDocs = indices.subList(0, n);

        for (int doc : topDocs) {
            System.out.println("Doc " + (doc+1) + ": distance = " + distances[doc]);
        }
    }

    public static void manhattanDistanceQuery(Matrix query, Matrix freqTable, int n) {
        double[] distances = new double[freqTable.getColumnDimension()];
        for (int i = 0; i < freqTable.getColumnDimension(); i++) {
            Matrix column = freqTable.getMatrix(0, freqTable.getRowDimension() - 1, i, i);
            Matrix difference = column.copy();
            for (int j = 0; j < query.getRowDimension(); j++) {
                difference.set(j, 0, Math.abs(column.get(j, 0) - query.get(j, 0)));
            }
            double distance = 0.0;
            for (int j = 0; j < difference.getRowDimension(); j++) {
                double value = difference.get(j, 0);
                distance += value;
            }
            distances[i] = distance;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            indices.add(i);
        }
        indices.sort((i, j) -> Double.compare(distances[i], distances[j]));
        List<Integer> topDocs = indices.subList(0, n);

        for (int doc : topDocs) {
            System.out.println("Doc " + (doc+1) + ": distance = " + distances[doc]);
        }
    }

    public static void cosineQuery(Matrix query, Matrix freqTable, int n) {
        double[] similarities = new double[freqTable.getColumnDimension()];
        for (int i = 0; i < freqTable.getColumnDimension(); i++) {
            Matrix column = freqTable.getMatrix(0, freqTable.getRowDimension() - 1, i, i);
            double dotProduct = 0.0;
            double queryMagnitude = 0.0;
            double columnMagnitude = 0.0;
            for (int j = 0; j < query.getRowDimension(); j++) {
                double q = query.get(j, 0);
                double d = column.get(j, 0);
                dotProduct += q * d;
                queryMagnitude += q * q;
                columnMagnitude += d * d;
            }
            similarities[i] = dotProduct / (Math.sqrt(queryMagnitude) * Math.sqrt(columnMagnitude));
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < similarities.length; i++) {
            indices.add(i);
        }
        indices.sort((i, j) -> Double.compare(similarities[j], similarities[i]));
        List<Integer> topDocs = indices.subList(0, n);

        for (int doc : topDocs) {
            System.out.println("Doc " + (doc+1) + ": similarity = " + similarities[doc]);
        }
    }
    public static void diceCoefficientQuery(Matrix query, Matrix freqTable, int n) {
        double[] similarities = new double[freqTable.getColumnDimension()];
        for (int i = 0; i < freqTable.getColumnDimension(); i++) {
            Matrix column = freqTable.getMatrix(0, freqTable.getRowDimension() - 1, i, i);
            double intersection = 0.0;
            double querySumSquared = 0.0;
            double columnSumSquared = 0.0;
            for (int j = 0; j < query.getRowDimension(); j++) {
                double q = query.get(j, 0);
                double d = column.get(j, 0);
                intersection += q * d;
                querySumSquared += q * q;
                columnSumSquared += d * d;
            }
            similarities[i] = 2 * intersection / (querySumSquared + columnSumSquared);
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < similarities.length; i++) {
            indices.add(i);
        }
        indices.sort((i, j) -> Double.compare(similarities[j], similarities[i]));
        List<Integer> topDocs = indices.subList(0, n);

        for (int doc : topDocs) {
            System.out.println("Doc " + (doc+1) + ": similarity = " + similarities[doc]);
        }
    }
}
