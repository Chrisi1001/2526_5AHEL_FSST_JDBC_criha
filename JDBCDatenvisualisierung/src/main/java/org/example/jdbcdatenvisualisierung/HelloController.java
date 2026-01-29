package org.example.jdbcdatenvisualisierung;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Gemachte Zusatzaufgaben
// TODO: Größe der Anwendung anpassen
// TODO: z.Zt. Top 30 - die Anzahl muss über einen Spinner einstellbar sein (im PreparedStatement anpassen)
// TODO: Überschrift des Diagramms muss auch angepasst werden

public class HelloController {

    @FXML private Label welcomeText;
    @FXML private BarChart<String, Number> languageBarChart;
    @FXML private ComboBox<String> continentComboBox;
    @FXML private Spinner<Integer> topNSpinner;

    private static final String DB_URL  = "jdbc:postgresql://xserv:5432/world2";
    private static final String DB_USER = "reader";
    private static final String DB_PASS = "reader";

    private static class CountryLanguageCount {
        private final String countryName;
        private final int languageCount;

        public CountryLanguageCount(String countryName, int languageCount) {
            this.countryName = countryName;
            this.languageCount = languageCount;
        }

        public String getCountryName() { return countryName; }
        public int getLanguageCount() { return languageCount; }
    }

    public void initialize() {
        languageBarChart.setLegendVisible(false);
        CategoryAxis xAxis = (CategoryAxis) languageBarChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) languageBarChart.getYAxis();
        xAxis.setTickLabelRotation(90);

        // FIX (notwendig): Animationen aus, sonst "wandern" Labels/Balken beim Update manchmal auseinander
        languageBarChart.setAnimated(false);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);

        // JavaFX Spinner Doku:
        // https://openjfx.io/javadoc/17/javafx.controls/javafx/scene/control/Spinner.html
        topNSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 100, 30, 5));
        topNSpinner.setEditable(true);

        loadContinents();

        continentComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshChart());
        topNSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshChart());

        if (!continentComboBox.getItems().isEmpty()) {
            continentComboBox.getSelectionModel().select(0);
        }

        refreshChart();
    }

    private void refreshChart() {
        String continent = continentComboBox.getValue();
        if (continent == null) return;

        int topN = topNSpinner.getValue() != null ? topNSpinner.getValue() : 30;

        welcomeText.setText("Sprachvielfalt pro Land – " + continent + " (Top " + topN + ")");
        loadChartForContinent(continent, topN);
    }

    // Für Combobox:
    // https://jenkov.com/tutorials/javafx/combobox.html
    // ResultSet auslesen (JDBC):
    // https://docs.oracle.com/javase/tutorial/jdbc/basics/retrieving.html
    private void loadContinents() {
        String sql = "SELECT DISTINCT continent FROM country ORDER BY continent;";

        continentComboBox.getItems().clear();

        // Ressourcen (Connection, Statement, ResultSet) werden automatisch geschlossen,
        // auch wenn eine Exception passiert (kein "close()" vergessen).
        // Oracle Erklärung:
        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String continent = rs.getString("continent");
                if (continent != null) {
                    continentComboBox.getItems().add(continent);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // PreparedStatement:
    // https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
    private void loadChartForContinent(String continent, int topN) {

        List<CountryLanguageCount> data = new ArrayList<>();

        // PostgreSQL: LIMIT kann als Parameter gesetzt werden (LIMIT ?), wenn man PreparedStatement nutzt.
        // (Hinweis: funktioniert mit modernen PostgreSQL JDBC Treibern)
        // Beispiel/Erklärung:
        // https://stackoverflow.com/questions/79108438/add-a-limit-parameter
        String sql =
                "SELECT c.name AS country_name, COUNT(DISTINCT cl.language) AS language_count " +
                        "FROM country c " +
                        "JOIN countrylanguage cl ON cl.countrycode = c.code " +
                        "WHERE c.continent = ? " +
                        "GROUP BY c.code, c.name " +
                        "ORDER BY language_count DESC, c.name " +
                        "LIMIT ?;";

        // try-with-resources -> automatisch schließen (Connection, PreparedStatement, ResultSet)
        // Oracle Erklärung:
        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, continent);
            ps.setInt(2, topN);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String country = rs.getString("country_name");
                    int count = rs.getInt("language_count");
                    data.add(new CountryLanguageCount(country, count));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // BarChart:
        // https://docs.oracle.com/javase/8/javafx/api/javafx/scene/chart/BarChart.html
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (CountryLanguageCount item : data) {
            series.getData().add(new XYChart.Data<>(item.getCountryName(), item.getLanguageCount()));
        }

        // FIX (notwendig): Achse/Kategorien sauber "resetten", damit Labels wieder zu den Balken passen
        CategoryAxis xAxis = (CategoryAxis) languageBarChart.getXAxis();
        xAxis.getCategories().clear();

        languageBarChart.getData().clear();
        languageBarChart.getData().add(series);

        // FIX (notwendig): Layout neu berechnen erzwingen (damit Tick-Labels korrekt sitzen)
        languageBarChart.applyCss();
        languageBarChart.layout();
    }
}
