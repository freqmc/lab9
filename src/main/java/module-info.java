module org.example.lab9 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.protobuf;


    opens org.example.lab9 to javafx.fxml;
    exports org.example.lab9;
}