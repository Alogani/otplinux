/*
 * OTP for linux
 * Copyright (C) 2021 Alogani
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA  02110-1335, USA.
 */

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
