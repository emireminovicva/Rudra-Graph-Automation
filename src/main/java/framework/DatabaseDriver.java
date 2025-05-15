package framework;

import org.apache.spark.sql.*;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class DatabaseDriver {
    private static final Logger log = LogManager.getLogger(DatabaseDriver.class);
    private static SparkSession spark;
    private static String adlsPath;
    private static Driver neo4jDriver;

    public static void initializeSparkSession() {
        if (spark == null) {
            log.info("✅ Initializing Spark session for Databricks and ADLS access...");

            spark = SparkSession.builder()
                    .appName("DatabricksAutomationFramework")
                    .config("spark.master", "local")
                    .getOrCreate();

            log.info("✅ Spark session initialized successfully!");
        }
    }

    public static void connectToDatabricks(String path) {
        if (spark == null) {
            initializeSparkSession();
        }
        System.out.println("starting spark config");

        spark.conf().set("fs.azure.account.auth.type.hbap.dfs.core.windows.net", "OAuth");
        spark.conf().set("fs.azure.account.oauth.provider.type.hbap.dfs.core.windows.net", "org.apache.hadoop.fs.azurebfs.oauth2.ClientCredsTokenProvider");
        spark.conf().set("fs.azure.account.oauth2.client.id.hbap.dfs.core.windows.net", "32c7c4c2-ab13-4e3f-9300-bab5bf817522");
        spark.conf().set("fs.azure.account.oauth2.client.secret.hbap.dfs.core.windows.net", "gdX8Q~klfF4ad5BMpTWmKNOW7yATKpOziqqg4aN1");
        spark.conf().set("fs.azure.account.oauth2.client.endpoint.hbap.dfs.core.windows.net", "https://login.microsoftonline.com/e44317ef-401c-46f2-838e-7db2894f0568/oauth2/token");

        adlsPath = path;
        log.info("✅ Successfully connected to Databricks and ADLS at: " + adlsPath);
    }

    public static Dataset<Row> executeQuery(String query) {
        if (adlsPath == null || adlsPath.isEmpty()) {
            throw new IllegalArgumentException("❌ ADLS path is missing! Ensure connection is established first.");
        }

        log.info("🔹 Reading Delta Table from ADLS path: " + adlsPath);

        Dataset<Row> spatientDF = spark.read()
                .format("delta")
                .load(adlsPath);

        spatientDF.createOrReplaceTempView("spatient");

        log.info("🔹 Executing SQL Query: " + query);
        Dataset<Row> resultDF = spark.sql(query);
        resultDF.show();

        log.info("✅ Query executed successfully on Databricks");
        return resultDF;
    }

    public static void connectToNeo4j() {
        try {
            Properties config = new Properties();
            try (InputStream input = DatabaseDriver.class.getClassLoader().getResourceAsStream("neo4j_config.properties")) {
                if (input == null) {
                    throw new IOException("❌ Failed to load neo4j_config.properties from classpath!");
                }
                config.load(input);
            }

            String neo4jUrl = config.getProperty("neo4j.url");
            String username = config.getProperty("neo4j.username");
            String password = config.getProperty("neo4j.password");

            if (neo4jUrl == null || username == null || password == null) {
                throw new RuntimeException("❌ Missing Neo4j configuration values! Check neo4j_config.properties.");
            }

            log.info("🔍 Connecting to Neo4j at: " + neo4jUrl);

            if (spark == null) {
                initializeSparkSession();
            }

            neo4jDriver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(username, password));
            log.info("✅ Successfully connected to Neo4j!");

        } catch (IOException e) {
            log.error("❌ Error loading Neo4j configuration: " + e.getMessage());
            throw new RuntimeException("❌ Unable to load Neo4j configuration", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error while connecting to Neo4j: " + e.getMessage());
            throw new RuntimeException("❌ Failed to connect to Neo4j", e);
        }
    }

    public static Dataset<Row> executeNeo4jQuery(String cypherQuery) {
        if (spark == null) {
            initializeSparkSession();
        }

        log.info("🔍 Executing Cypher query on Neo4j via Spark: " + cypherQuery);

        Properties config = new Properties();
        try (InputStream input = DatabaseDriver.class.getClassLoader().getResourceAsStream("neo4j_config.properties")) {
            if (input == null) {
                throw new IOException("❌ Failed to load neo4j_config.properties from classpath!");
            }
            config.load(input);
        } catch (IOException e) {
            throw new RuntimeException("❌ Unable to load Neo4j configuration for Spark", e);
        }

        Map<String, String> options = new HashMap<>();
        options.put("url", config.getProperty("neo4j.url"));
        options.put("authentication.type", "basic");
        options.put("authentication.basic.username", config.getProperty("neo4j.username"));
        options.put("authentication.basic.password", config.getProperty("neo4j.password"));
        options.put("query", cypherQuery);

        Dataset<Row> neo4jData = spark.read()
                .format("org.neo4j.spark.DataSource")
                .options(options)
                .load()
                .limit(10);

        neo4jData.show();
        log.info("✅ Neo4j Query executed successfully!");
        return neo4jData;
    }

    public static boolean compareResults(List<String> databricksResults, List<String> neo4jResults) {
        boolean isEqual = databricksResults.equals(neo4jResults);
        log.info("🔍 Comparing results - Match: " + isEqual);
        return isEqual;
    }

    public static void verifySpatientData() {
        String query = "SELECT * FROM ci_refined.spatient LIMIT 10";
        List<String> results = executeDatabricksQuery(query);
        if (results.isEmpty()) {
            log.warn("⚠️ No data found in spatient table!");
        } else {
            log.info("✅ spatient data verified: " + results);
        }
    }

    public static List<String> executeDatabricksQuery(String query) {
        log.info("🔍 Executing SQL query on Databricks: " + query);
        Dataset<Row> result = spark.sql(query);
        List<String> resultList = result.collectAsList().stream()
                .map(Row::toString)
                .collect(Collectors.toList());

        log.info("✅ Databricks Query Results: " + resultList);
        return resultList;
    }

    public static Properties loadConfig(String filePath) throws IOException {
        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(filePath);
        properties.load(fis);
        fis.close();
        return properties;
    }
}
