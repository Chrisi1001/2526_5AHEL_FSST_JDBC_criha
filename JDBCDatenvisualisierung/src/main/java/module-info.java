module org.example.jdbcdatenvisualisierung {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens org.example.jdbcdatenvisualisierung to javafx.fxml;
    exports org.example.jdbcdatenvisualisierung;
}