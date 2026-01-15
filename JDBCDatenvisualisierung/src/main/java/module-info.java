module org.example.jdbcdatenvisualisierung {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.jdbcdatenvisualisierung to javafx.fxml;
    exports org.example.jdbcdatenvisualisierung;
}