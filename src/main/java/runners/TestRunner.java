package runners;

import io.cucumber.core.cli.Main;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class TestRunner {

    public static void main(String[] args) {
        // Prevent System.exit() to avoid Databricks job termination issues
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(java.security.Permission perm) {}

            @Override
            public void checkExit(int status) {
                throw new SecurityException("System.exit() blocked");
            }
        });

        String reportPath = "/dbfs/FileStore/results"; // Default fallback

        // Parse --REPORT_PATH argument
        for (int i = 0; i < args.length - 1; i++) {
            if ("--REPORT_PATH".equals(args[i])) {
                reportPath = args[i + 1];
                break;
            }
        }

        String jsonReportPath = reportPath + "/cucumber.json";
        String htmlReportPath = reportPath + "/cucumber-html";

        String[] cucumberOptions = new String[]{
                "--plugin", "pretty",
                "--plugin", "json:" + jsonReportPath,
                "--plugin", "html:" + htmlReportPath,
                "--glue", "steps",
                "classpath:features"
        };

        try {
            Main.run(cucumberOptions, Thread.currentThread().getContextClassLoader());

            if (Files.exists(Paths.get(jsonReportPath))) {
                System.out.println("✅ JSON report saved at: " + jsonReportPath);
                System.out.println("✅ HTML report saved at: " + htmlReportPath);
            } else {
                System.err.println("❌ Report generation failed. JSON report not found.");
            }

        } catch (SecurityException e) {
            System.out.println("🔒 System.exit() was blocked (expected in Databricks)." );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}