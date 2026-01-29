package org.example.jdbcdatenvisualisierung;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HelloController {

    @FXML private BarChart<String, Number> languageBarChart;

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
        xAxis.setLabel("Ländername");
        yAxis.setLabel("Anzahl Sprachen");

        List<CountryLanguageCount> data = new ArrayList<>();

        String sql =
                "SELECT c.name AS country_name, COUNT(DISTINCT cl.language) AS language_count " +
                        "FROM country c " +
                        "JOIN countrylanguage cl ON cl.countrycode = c.code " +
                        "GROUP BY c.code, c.name " +
                        "ORDER BY language_count DESC, c.name " +
                        "LIMIT 30;";

        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://xserv:5432/world2", "reader", "reader");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {


            while (rs.next()) {
                String country = rs.getString("country_name");
                int count = rs.getInt("language_count");
                data.add(new CountryLanguageCount(country, count));
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
