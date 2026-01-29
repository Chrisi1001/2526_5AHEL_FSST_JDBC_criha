package org.example.jdbcdatenvisualisierung;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HelloController {

    @FXML private Label welcomeText;
    @FXML private BarChart<String, Number> languageBarChart;
    @FXML private ComboBox<String> continentComboBox;

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
        // Chart Optik (wie bei dir)
        languageBarChart.setLegendVisible(false);
        CategoryAxis xAxis = (CategoryAxis) languageBarChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) languageBarChart.getYAxis();
        xAxis.setTickLabelRotation(90);

        loadContinents();

        continentComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                welcomeText.setText("Sprachvielfalt pro Land – " + newVal + " (Top 30)");
                loadChartForContinent(newVal);
            }
        });

        if (!continentComboBox.getItems().isEmpty()) {
            continentComboBox.getSelectionModel().select(0);
            String selected = continentComboBox.getValue();
            welcomeText.setText("Sprachvielfalt pro Land – " + selected + " (Top 30)");
            loadChartForContinent(selected);
        }
    }

    private void loadContinents() {
        String sql = "SELECT DISTINCT continent FROM country ORDER BY continent;";

        continentComboBox.getItems().clear();

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

    private void loadChartForContinent(String continent) {
        List<CountryLanguageCount> data = new ArrayList<>();

        String sql =
                "SELECT c.name AS country_name, COUNT(DISTINCT cl.language) AS language_count " +
                        "FROM country c " +
                        "JOIN countrylanguage cl ON cl.countrycode = c.code " +
                        "WHERE c.continent = ? " +
                        "GROUP BY c.code, c.name " +
                        "ORDER BY language_count DESC, c.name " +
                        "LIMIT 30;";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, continent);

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

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (CountryLanguageCount item : data) {
            series.getData().add(new XYChart.Data<>(item.getCountryName(), item.getLanguageCount()));
        }

        languageBarChart.getData().clear();
        languageBarChart.getData().add(series);
    }
}
