module org.example.lab9 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.lab9 to javafx.fxml;
    exports org.example.lab9;
}