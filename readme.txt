================================================================
  COS221 Practical Assignment 4 – Chinook Music Store GUI
  Student: u25021002
================================================================

HOW TO BUILD
─────────────────────────────────────────────────────────────
Prerequisites:
  • Java 17 or later  (check: java -version)
  • Apache Maven 3.8+ (check: mvn -version)
  • MariaDB 12.2 with the Chinook database imported

Steps:
  1. Open a terminal in the project root (same folder as pom.xml).
  2. Run:
       mvn package -q
  3. This produces:
       target/chinook-gui-1.0.0.jar     (shaded fat-jar, contains JDBC driver)

HOW TO CONNECT THE APPLICATION TO THE DATABASE
─────────────────────────────────────────────────────────────
The application reads credentials from environment variables (Task 5).
Set these before running the app:

  Windows (Command Prompt):
    set CHINOOK_DB_PROTO=jdbc:mysql
    set CHINOOK_DB_HOST=localhost
    set CHINOOK_DB_PORT=3306
    set CHINOOK_DB_NAME=u25021002_chinook
    set CHINOOK_DB_USERNAME=root
    set CHINOOK_DB_PASSWORD=yourpassword

  Windows (PowerShell):
    $env:CHINOOK_DB_PROTO   = "jdbc:mysql"
    $env:CHINOOK_DB_HOST    = "localhost"
    $env:CHINOOK_DB_PORT    = "3306"
    $env:CHINOOK_DB_NAME    = "u25021002_chinook"
    $env:CHINOOK_DB_USERNAME = "root"
    $env:CHINOOK_DB_PASSWORD = "yourpassword"

  macOS / Linux:
    export CHINOOK_DB_PROTO=jdbc:mysql
    export CHINOOK_DB_HOST=localhost
    export CHINOOK_DB_PORT=3306
    export CHINOOK_DB_NAME=u25021002_chinook
    export CHINOOK_DB_USERNAME=root
    export CHINOOK_DB_PASSWORD=yourpassword

HOW TO RUN
─────────────────────────────────────────────────────────────
After setting env vars (same terminal session):

    java -jar target/chinook-gui-1.0.0.jar

A GUI window will open. If a connection error appears, check
that MariaDB is running and the env vars are correct.

ALTERNATIVE – without Maven (manual classpath)
─────────────────────────────────────────────────────────────
  1. Download mysql-connector-j-8.3.0.jar from:
       https://dev.mysql.com/downloads/connector/j/
  2. Compile:
       javac -cp ".;mysql-connector-j-8.3.0.jar" src/main/java/chinook/ChinookApp.java -d out/
  3. Run:
       java -cp "out;mysql-connector-j-8.3.0.jar" chinook.ChinookApp

TABS OVERVIEW
─────────────────────────────────────────────────────────────
  👤 Employees       – Lists all employees; filter by name or city
  🎵 Tracks          – Browse/search tracks; add new track via popup dialog
  📊 Report          – Genre revenue report (auto-refreshes on tab open)
  🔔 Notifications   – Full Customer CRUD + inactive customers (>2 year threshold)
  ⭐ Recommendations – Per-customer spending summary + personalised track suggestions

ADVANCED SQL USED (Bonus Task 7)
─────────────────────────────────────────────────────────────
  • RANK() OVER (ORDER BY ...)   – window function for genre revenue ranking (Report tab)
  • DATE_SUB(NOW(), INTERVAL 2 YEAR) – date arithmetic for inactive customers
  • Correlated NOT IN subquery   – recommendation engine filters purchased tracks
  • ORDER BY RAND() LIMIT 20    – random sampling for recommendations
================================================================
