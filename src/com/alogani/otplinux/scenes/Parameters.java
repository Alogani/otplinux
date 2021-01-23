package com.alogani.otplinux.scenes;

import com.alogani.otplinux.Main;
import com.alogani.otplinux.Storage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Objects;

public class Parameters {
    static private Parameters instance;
    static private Stage currentStage; // to close the window

    @FXML
    public ComboBox<String> separator;
    public CheckBox showPasswordCheckBox;
    public CheckBox showSecretCheckBox;
    public CheckBox showDetailsButton;


    public Parameters() { Parameters.instance = this; }

    public static void openWindow() {
        currentStage = new Stage();
        currentStage.setResizable(false);
        currentStage.initModality(Modality.APPLICATION_MODAL);
        currentStage.setScene(new Scene((Parent) Objects.requireNonNull(Main.loadFXML("Parameters.fxml"))));

        instance.initScene();

        currentStage.showAndWait();
    }
    
    public void initScene() {
        /*
        Parameters are stored in Storage class variables
        Recuper them from it
         */

        // Available choice for separator
        separator.getItems().setAll("Newline", " : ", " - ", " | ", " ; ", ", ");
        if (separator.getItems().contains(Storage.separator))
            separator.getSelectionModel().select(Storage.separator);
        else
            separator.getSelectionModel().select("Newline");

        showPasswordCheckBox.setSelected(Storage.showPasswordDefault);
        showSecretCheckBox.setSelected(Storage.showSecretDefault);
        showDetailsButton.setSelected(Storage.showDetailsByDefault);

    }

    @FXML
    public void applyParameters(ActionEvent actionEvent) {
        String separatorString = separator.getSelectionModel().getSelectedItem();
        Storage.separator = separatorString.equals("Newline") ? "\n" : separatorString;

        Storage.showPasswordDefault = showPasswordCheckBox.isSelected();
        Storage.showSecretDefault = showSecretCheckBox.isSelected();
        Storage.showDetailsByDefault = showDetailsButton.isSelected();
        Primary.instance.tokenDetailsGrid.setVisible(Storage.showDetailsByDefault);

        Storage.syncParameters(true);
        currentStage.close();
    }

    @FXML
    public void closeStage(ActionEvent actionEvent) {
        currentStage.close();
    }
}
