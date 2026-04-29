================================================================
  COS221 Practical Assignment 4 - Chinook Music Store GUI
  Student: u25021002
  Date:    29 April 2026
================================================================

PREREQUISITES
-------------------------------------------------------------
  - Java 8 or later       (tested on OpenJDK 1.8.0_482)
  - MariaDB 12.2          with u25021002_chinook imported
  - mysql-connector-j-8.3.0.jar  (download link below)

Download the JDBC driver from:
  https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar


HOW TO COMPILE
-------------------------------------------------------------
From the project root folder (where this readme.txt lives):

  Windows (PowerShell or Command Prompt):

    javac -encoding UTF-8 ^
      -cp "path\to\mysql-connector-j-8.3.0.jar" ^
      src\main\java\chinook\ChinookApp.java ^
      -d out

  Single line:
    javac -encoding UTF-8 -cp "mysql-connector-j-8.3.0.jar" src\main\java\chinook\ChinookApp.java -d out

  Note: Create the out\ folder first if it does not exist:
    mkdir out


HOW TO SET ENVIRONMENT VARIABLES (Task 5)
-------------------------------------------------------------
The application reads all connection details from environment
variables. No credentials are stored in the source code.

  Windows PowerShell:
    $env:CHINOOK_DB_PROTO    = "jdbc:mysql"
    $env:CHINOOK_DB_HOST     = "localhost"
    $env:CHINOOK_DB_PORT     = "3306"
    $env:CHINOOK_DB_NAME     = "u25021002_chinook"
    $env:CHINOOK_DB_USERNAME = "root"
    $env:CHINOOK_DB_PASSWORD = "yourpassword"

  Windows Command Prompt:
    set CHINOOK_DB_PROTO=jdbc:mysql
    set CHINOOK_DB_HOST=localhost
    set CHINOOK_DB_PORT=3306
    set CHINOOK_DB_NAME=u25021002_chinook
    set CHINOOK_DB_USERNAME=root
    set CHINOOK_DB_PASSWORD=yourpassword

  Note: Environment variables must be set in the same terminal
  session before running the application. They reset when the
  terminal is closed.


HOW TO RUN
-------------------------------------------------------------
After setting environment variables, run from the project root:

  java -cp "out;mysql-connector-j-8.3.0.jar" chinook.ChinookApp

Replace the paths with absolute paths if needed:

  java -cp "C:\path\to\out;C:\path\to\mysql-connector-j-8.3.0.jar" chinook.ChinookApp

A GUI window will open. If a connection error appears:
  - Confirm MariaDB is running
  - Confirm the database u25021002_chinook exists
  - Confirm all 6 environment variables are set correctly


TABS OVERVIEW
-------------------------------------------------------------
  Employees       - Lists all employees with supervisor names.
                    Filter by name or city using the search box.

  Tracks          - Browse and search all tracks.
                    Click "+ Add Track" to open a dialog and insert
                    a new track with dropdowns for Album, Genre,
                    and MediaType populated from the database.

  Report          - Auto-generates a genre revenue report each time
                    the tab is opened. Genres ranked highest to lowest
                    using the RANK() SQL window function.

  Notifications   - Top half: full Customer CRUD (Create, Read,
                    Update, Delete) with search.
                    Bottom half: inactive customers (no invoices or
                    last invoice older than 2 years).

  Recommendations - Select a customer to view their total spend,
                    purchase count, last purchase date, and favourite
                    genre. Displays 20 personalised track
                    recommendations from their favourite genre
                    that they have not yet purchased.


ADVANCED SQL USED (Task 6.2)
-------------------------------------------------------------
  1. RANK() OVER (ORDER BY SUM(...) DESC)
       Window function used in the Report tab to rank genres
       by revenue without a subquery or self-join.

  2. DATE_SUB(NOW(), INTERVAL 2 YEAR)
       Date arithmetic in the Notifications tab to identify
       customers whose last invoice is older than 2 years.

  3. NOT IN (correlated subquery) + ORDER BY RAND() LIMIT 20
       Used in the Recommendations tab to exclude tracks the
       customer has already purchased, then return a random
       selection of 20 suggestions.


GITHUB REPOSITORY
-------------------------------------------------------------
  https://github.com/u25021002-create/cos221_PA4


PROJECT STRUCTURE
-------------------------------------------------------------
  u25021002_chinook_pa4/
  |-- src/
  |   └-- main/
  |       └-- java/
  |           └-- chinook/
  |               └-- ChinookApp.java   (single-file GUI application)
  |-- out/                              (compiled .class files)
  |-- pom.xml                           (Maven build file, optional)
  |-- readme.txt                        (this file)
  └-- .gitignore

================================================================

for me:

$env:CHINOOK_DB_PROTO    = "jdbc:mysql"
$env:CHINOOK_DB_HOST     = "localhost"
$env:CHINOOK_DB_PORT     = "3306"
$env:CHINOOK_DB_NAME     = "u25021002_chinook"
$env:CHINOOK_DB_USERNAME = "root"
$env:CHINOOK_DB_PASSWORD = "Hangwani3"

java -cp "C:\Users\onale\OneDrive\Desktop\Uni\COS 221\Prac 4\u25021002_chinook_pa4\out;C:\Users\onale\OneDrive\Desktop\mysql-connector-j-8.3.0.jar" chinook.ChinookApp


