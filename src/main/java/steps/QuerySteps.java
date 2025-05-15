package steps;

import framework.DatabaseDriver;
import io.cucumber.java.en.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.testng.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuerySteps {
    private static final Logger log = LogManager.getLogger(QuerySteps.class);

    private Dataset<Row> databricksResultDF;
    private Dataset<Row> neo4jResultDF;
    private List<String> databricksResults;
    private List<String> neo4jResults;
    private final Map<String, Object> contextMap = new HashMap<>();

    @Given("I connect to Databricks and ADLS")
    public void connectToDatabricksAndADLS() {
        DatabaseDriver.initializeSparkSession();
        log.info("✅ Successfully connected to Databricks and ADLS");
    }

    @Given("I verify spatient data in Databricks")
    public void verifySpatientData() {
        DatabaseDriver.verifySpatientData();
    }

    @Given("I connect to Databricks and ADLS with path {string}")
    public void connectToDatabricksAndADLS(String adlsPath) {
        log.info("🔹 Connecting to Databricks with ADLS path: " + adlsPath);
        DatabaseDriver.connectToDatabricks(adlsPath);
    }

    @When("I execute SQL query {string} on Databricks")
    public void executeSQLQueryOnDatabricks(String query) {
        log.info("🔹 Running SQL query: " + query);
        databricksResultDF = DatabaseDriver.executeQuery(query);
    }

    @When("I execute SQL query {string} on Databricks and save value to {string}")
    public void executeSQLAndSaveToKey(String query, String key) {
        log.info("💾 Executing SQL query and saving to key: " + key);
        databricksResultDF = DatabaseDriver.executeQuery(query);
        long value = databricksResultDF.collectAsList().get(0).getLong(0);
        contextMap.put(key, value);
        log.info("✅ Saved value to '" + key + "': " + value);
    }

    @Then("the query result should not be empty")
    public void validateQueryResults() {
        long rowCount = databricksResultDF.count();
        Assert.assertFalse(rowCount == 0, "❌ Query returned no results!");
        log.info("✅ Query results validated successfully with " + rowCount + " rows.");
    }

    @Given("I connect to Neo4j via Spark")
    public void connectToNeo4jViaSpark() {
        DatabaseDriver.connectToNeo4j();
        log.info("✅ Successfully connected to Neo4j using Spark!");
    }

    @When("I execute Cypher query {string} on Neo4j")
    public void executeNeo4jQuery(String cypherQuery) {
        log.info("🔹 Executing Cypher query via Spark: " + cypherQuery);
        neo4jResultDF = DatabaseDriver.executeNeo4jQuery(cypherQuery);
    }

    @When("I execute Cypher query {string} on Neo4j and save value to {string}")
    public void executeCypherAndSaveToKey(String cypherQuery, String key) {
        log.info("💾 Executing Cypher query and saving to key: " + key);
        neo4jResultDF = DatabaseDriver.executeNeo4jQuery(cypherQuery);
        long value = neo4jResultDF.collectAsList().get(0).getLong(0);
        contextMap.put(key, value);
        log.info("✅ Saved value to '" + key + "': " + value);
    }

    @Then("the neo4j result should not be empty")
    public void validateNeo4jQueryResults() {
        Assert.assertFalse(neo4jResultDF.isEmpty(), "❌ Cypher query returned no results!");
        log.info("✅ Cypher query executed successfully.");
    }

    @Then("the results from Databricks and Neo4j should match")
    public void compareResults() {
        databricksResults = databricksResultDF.collectAsList().stream().map(Row::toString).collect(Collectors.toList());
        neo4jResults = neo4jResultDF.collectAsList().stream().map(Row::toString).collect(Collectors.toList());

        Assert.assertTrue(DatabaseDriver.compareResults(databricksResults, neo4jResults), "❌ Results mismatch!");
        log.info("✅ Results from Databricks and Neo4j match!");
    }

    @Then("the diagnosis count from Databricks and Neo4j should match")
    public void compareDiagnosisCounts() {
        long databricksCount = databricksResultDF.collectAsList().get(0).getLong(0);
        long neo4jCount = neo4jResultDF.collectAsList().get(0).getLong(0);

        log.info("📊 Databricks Diagnosis Count: " + databricksCount);
        log.info("📊 Neo4j Diagnosis Count: " + neo4jCount);

        Assert.assertEquals(databricksCount, neo4jCount, "❌ Diagnosis counts do not match!");
        log.info("✅ Diagnosis counts match!");
    }

    @Then("the spatient count from Databricks and Neo4j should match")
    public void compareSpatientCounts() {
        long databricksCount = databricksResultDF.collectAsList().get(0).getLong(0);
        long neo4jCount = neo4jResultDF.collectAsList().get(0).getLong(0);

        log.info("📊 Databricks spatient count: " + databricksCount);
        log.info("📊 Neo4j spatient count: " + neo4jCount);

        Assert.assertEquals(databricksCount, neo4jCount, "❌ Spatient counts do not match!");
        log.info("✅ Spatient counts match!");
    }

    @Then("the PatientSSN from Databricks and Neo4j should match")
    public void comparePatientSSN() {
        String databricksSSN = databricksResultDF.collectAsList().get(0).getString(0);
        String neo4jSSN = neo4jResultDF.collectAsList().get(0).getString(0);

        log.info("🧾 Databricks PatientSSN: " + databricksSSN);
        log.info("🧾 Neo4j PatientSSN: " + neo4jSSN);

        Assert.assertEquals(databricksSSN, neo4jSSN, "❌ PatientSSN does not match between Databricks and Neo4j!");
        log.info("✅ PatientSSN matches for PatientSID 1000608");
    }

    @Then("the {string} and {string} match")
    public void compareSavedValues(String key1, String key2) {
        Object val1 = contextMap.get(key1);
        Object val2 = contextMap.get(key2);

        Assert.assertNotNull(val1, "❌ Missing value for key: " + key1);
        Assert.assertNotNull(val2, "❌ Missing value for key: " + key2);

        log.info("🔍 Comparing values from map: " + key1 + "=" + val1 + ", " + key2 + "=" + val2);
        Assert.assertEquals(val1, val2, "❌ Values for " + key1 + " and " + key2 + " do not match!");
        log.info("✅ Values match!");
    }
}
