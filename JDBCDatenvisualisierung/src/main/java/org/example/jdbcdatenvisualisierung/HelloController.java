package org.example.jdbcdatenvisualisierung;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.sql.*;

public class HelloController {

    @FXML private Label welcomeText;
    @FXML private BarChart<String, Number> languageBarChart;

    public void initialize() {

        // Chart-Optik (optional)
        languageBarChart.setLegendVisible(false);
        CategoryAxis xAxis = (CategoryAxis) languageBarChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) languageBarChart.getYAxis();
        xAxis.setTickLabelRotation(90);
        xAxis.setLabel("Ländername");
        yAxis.setLabel("Anzahl Sprachen");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://xserv:5432/world2",
                    "reader",
                    "reader"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Statement st = null;
        try {
            st = conn.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ResultSet rs = null;
        try {
            rs = st.executeQuery(
                    "SELECT " +
                            "  c.name AS country_name, " +
                            "  COUNT(DISTINCT cl.language) AS language_count " +
                            "FROM country c " +
                            "JOIN countrylanguage cl ON cl.countrycode = c.code " +
                            "GROUP BY c.code, c.name " +
                            "ORDER BY language_count DESC, c.name " +
                            "LIMIT 30;"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        while (true) {
            try {
                if (!rs.next()) break;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            String country;
            int count;

            try {
                country = rs.getString("country_name");
                count = rs.getInt("language_count");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            series.getData().add(new XYChart.Data<>(country, count));
        }

        languageBarChart.getData().clear();
        languageBarChart.getData().add(series);

        // Ressourcen schließen (wie bei dir)
        try {
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
