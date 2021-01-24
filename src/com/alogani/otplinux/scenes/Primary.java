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

import com.alogani.otpcore.OTPType;
import com.alogani.otpcore.Token;
import com.alogani.otplinux.CountdownThread;
import com.alogani.otplinux.Main;
import com.alogani.otplinux.Storage;
import com.alogani.otplinux.ToggleManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Objects;


/*
This scene is the main one. It is the controller of an FXML file.
It provides :
- access to all other scenes
- a list of all tokens in the database
- a detailed view for a selected token
 */
public class Primary implements CounterScene {
    /*
    FXML loading call the constructor and generate a new instance,
    but it doesn't return an instance but instead a JavaFX node
    Moreover any instruction in the constructor to initialize some fields throws an exception
    Also FXML can't be linked to static fields or methods (yeepee)
    That's why there is in this class and some others :
    - an instance method to initialize the fields that needs it
    - through a static constructor that calls it (from an other class from example)
    - via an "instance" static field that servers as a bridge and represents the instance created by FXML
     */
    static public Primary instance;

    /*
    The methods and fields that handle showing and hiding the counter and password
    have been placed in the ToggleManager class for convenience.
    Only this scene use it for now, ModifyToken relies on other methods
     */
    private ToggleManager toggleManager;

    // Fields to manage secret toggling
    private boolean showSecret = Storage.showSecretDefault;
    final ImageView lockSecretView = new ImageView(ToggleManager.LOCK_IMAGE);
    final ImageView unlockSecretView = new ImageView(ToggleManager.UNLOCK_IMAGE);



    @FXML
    public ListView<String> tokenList;
    public Label issuer;
    public Label account;
    public Label password;
    public Label intervalOrcounterLabel;
    public Label type;
    public Label algorithm;
    public Label digits;
    public Label secret;
    public Label intervalOrcounter;
    public GridPane tokenDetailsGrid;
    public Button togglePasswordButton;
    public Button toggleSecretButton;
    public Button incrementCounter;

    // Constructor called by FXML. The instance thus generated is saved in a static field
    public Primary() {
        Primary.instance = this;
    }

    // Custom static "constructor". Handles also the generation of a stage to lighten Main
    public static void openWindow(Stage primaryStage) {
        primaryStage.setScene(new Scene((Parent) Objects.requireNonNull(Main.loadFXML("Primary.fxml"))));
        primaryStage.setTitle("OTP for linux");
        primaryStage.setResizable(false);

        instance.initScene();
        primaryStage.show();
    }

    /*
     Custom non static "constructor"
     - Instantiate countdownThread & toggleManager
     - Add a listener monitor token selection
     - Update token fields with the first on the list
     - Hide or show supplement details according to user parameters
     */
    private void initScene() {
        // thread is integraly managed by toggleManager instance
        toggleManager = new ToggleManager(new CountdownThread(instance, false), togglePasswordButton, password);

        tokenList.getSelectionModel().selectedItemProperty().addListener((changed, oldvalue, newvalue) -> updateLabels());
        updateList(0);
        if (Storage.showDetailsByDefault) showTokenDetails(null);
    }


    // All the stuff that happen when token infos must be updated
    private void updateLabels() {
        Token currentToken = getCurrentToken();

        // if no token is selected, clear everything
        if (currentToken == null) {
            clearFields();
            return;
        }

        // Set the values of the fields according to the token
        issuer.setText(currentToken.issuer);
        account.setText(currentToken.account);
        type.setText(currentToken.otpType.toString());
        algorithm.setText(currentToken.hashFunction.toString());
        digits.setText(String.valueOf(currentToken.digits));
        intervalOrcounter.setText(String.valueOf(currentToken.intervalTOTP_OR_counterHOTP));

        if (OTPType.isTOTP(currentToken))
            intervalOrcounterLabel.setText("Interval");
        else
            intervalOrcounterLabel.setText("Counter");

        // Manage if secret will be shown an how
        showSecret = Storage.showSecretDefault;
        if (showSecret) {
            secret.setText(currentToken.secretKey);
            toggleSecretButton.setGraphic(unlockSecretView);
        } else {
            secret.setText(ToggleManager.STARS_STRING);
            toggleSecretButton.setGraphic(lockSecretView);
        }

        // Manage if password will be shown
        if (! Storage.showPasswordDefault) {
            toggleManager.hidePassword();
        } else {
            if (OTPType.isTOTP(currentToken)) {
                incrementCounter.setVisible(false);
                toggleManager.showPasswordTOTP(currentToken);
            } else {
                incrementCounter.setVisible(true);
                toggleManager.hidePassword();
                toggleManager.showPasswordHOTP(currentToken);
            }
        }

    }

    // Update list and select automatically the provided index (or a previous one)
    public void updateList() {updateList(tokenList.getSelectionModel().getSelectedIndex()); }
    public void updateList(int index) {
        tokenList.getItems().setAll(Storage.getNiceList());
        if (index != -1)
            tokenList.getSelectionModel().select(index);
    }

    // Make everything blank
    public void clearFields() {
        for (Labeled labeled : Arrays.asList(issuer, account, type, algorithm, digits, intervalOrcounter, password, secret, togglePasswordButton))
            labeled.setText("");
        togglePasswordButton.setGraphic(null);
        toggleSecretButton.setGraphic(null);
        incrementCounter.setVisible(false);
        updateList(0);
    }

// SceneWithCounterAbstract methods
    @Override
    public Labeled getCounterLabel() {
        return togglePasswordButton;
    }

    @Override
    public Label getPasswordLabel() {
        return password;
    }

    @Override
    public Token getCurrentToken() {
        /*
        Get the selected token index in the list view
        Map it through the tokenIDList to get the tokenID
        Map the tokenID through the tokenMap to get the effective token
        See Storage class for more infos
        This avoids redundancy for a little cost
         */
        int selectionIndex = tokenList.getSelectionModel().getSelectedIndex();
        if (selectionIndex == -1) {
            // null if nothing is selected, this will cause the thread to stop
            return null;
        }
        return Storage.tokenMap.get(
                Storage.tokenIdList.get(selectionIndex));
    }

// MANAGE MENU
    @FXML
    public void newToken(ActionEvent actionEvent) {
        pauseScene();
        ModifyToken.openWindow();
        resumeScene();
    }

    @FXML
    public void modifyToken(ActionEvent actionEvent) {
        int index = tokenList.getSelectionModel().getSelectedIndex();
        if (index == -1) return;
        pauseScene();
        ModifyToken.openWindow(Storage.tokenIdList.get(index));
        resumeScene();
    }

    @FXML
    public void deleteToken(ActionEvent actionEvent) {
        if (! Dialog.askConfirmation("Delete token", "You are about to delete an entry",
                "Are you sure you want to delete the following entry :",
                getCurrentToken().issuer + (getCurrentToken().issuer.equals("") ? "" : Storage.separator) + getCurrentToken().account))
            return;
        int index = tokenList.getSelectionModel().getSelectedIndex();
        if (index == -1) return;
        Storage.deleteToken(Storage.tokenIdList.get(index));
        resumeScene();
    }

    @FXML
    public void openParameters(ActionEvent actionEvent) {
        pauseScene();
        Parameters.openWindow();
        resumeScene();
    }

// Some utilities to avoid disturbing background things to happen
    private void pauseScene() {
        toggleManager.hidePassword();
        if (showSecret) toggleSecret(null);
    }

    private void resumeScene() {
        updateList();
        updateLabels();
    }


// Methods to move the tokens on the list
    public void moveOnList(int sourceIndex, int destinationIndex) {
        Storage.moveTokens(sourceIndex, destinationIndex);
        updateList(destinationIndex);
    }

    @FXML
    public void moveOnListUp(ActionEvent actionEvent) {
        int index = tokenList.getSelectionModel().getSelectedIndex();
        if (index < 1) return;
        moveOnList(index, index-1);
    }

    @FXML
    public void moveOnListDown(ActionEvent actionEvent) {
        int index = tokenList.getSelectionModel().getSelectedIndex();
        if (index >= tokenList.getItems().size() - 1) return;
        moveOnList(index, index+1);
    }

    @FXML
    public void moveOnListTop(ActionEvent actionEvent) {
        int index = tokenList.getSelectionModel().getSelectedIndex();
        if (index < 1) return;
        moveOnList(index, 0);
    }

    @FXML
    public void moveOnListBottom(ActionEvent actionEvent) {
        int index = tokenList.getSelectionModel().getSelectedIndex();
        if (index >= tokenList.getItems().size() - 1) return;
        moveOnList(index, tokenList.getItems().size() - 1);
    }



// Buttons Action methods on the token info grid
    @FXML
    public void generateQRCode(ActionEvent actionEvent) {
        if (getCurrentToken() == null) return;
        pauseScene();
        new QRDialog(getCurrentToken());
        resumeScene();
    }

    @FXML
    public void showTokenDetails(ActionEvent actionEvent) {
        tokenDetailsGrid.setVisible(! tokenDetailsGrid.isVisible());
    }

    @FXML
    public void toggleSecret(ActionEvent actionEvent) {
        if (getCurrentToken() == null) return;
        showSecret = ! showSecret;
        secret.setText(showSecret ? getCurrentToken().secretKey: ToggleManager.STARS_STRING);
        toggleSecretButton.setGraphic(showSecret ? unlockSecretView : lockSecretView);
    }

    @FXML
    public void incrementCounter(ActionEvent actionEvent) {
        Token token = getCurrentToken(); // if counter is shown, token must not be null
        if (token.intervalTOTP_OR_counterHOTP == Long.MAX_VALUE) { return; } // won't go beyond

        token.intervalTOTP_OR_counterHOTP += 1;
        /*
        If updateLabel is called, secret and password fields will hide or show according to default,
        which is not wanted
         */
        intervalOrcounter.setText(String.valueOf(token.intervalTOTP_OR_counterHOTP));
        password.setText(token.getOTP());
        Storage.syncTokens();
    }

    @FXML
    public void togglePassword(ActionEvent actionEvent) {
        toggleManager.togglePassword(getCurrentToken()); // Not handled here
    }
}
