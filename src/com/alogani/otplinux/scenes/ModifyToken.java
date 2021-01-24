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

import com.alogani.otpcore.HashFunction;
import com.alogani.otpcore.OTPType;
import com.alogani.otpcore.QRCode;
import com.alogani.otpcore.Token;
import com.alogani.otplinux.CountdownThread;
import com.alogani.otplinux.Main;
import com.alogani.otplinux.Storage;
import com.alogani.otplinux.ToggleManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

/*
The Scene which allow to create a new token or modify some fields of an existing one
Some implementations are simalar to Primary scene, look at its comments for more details
 */
public class ModifyToken implements CounterScene {
    public static ModifyToken instance;
    // here to be able to call currentStage.close() later
    static private Stage currentStage;

    /*
     Static because no need to create a new thread for each new window
     Instead is instanciated in first openWindow() call and then remains idle after stage is closed
     */
    public static CountdownThread countdownThread;

    /*
    This field is effectively final by instance
    But as the constructor is handled by FXML, it is initialized later in the "custom constructor"
    If it equals -1L, this means no token is beeing modified but a new one is created
     */
    private Long tokenId;

    // Fields to manage Password and secret toggling
    final private ImageView lockPasswordView = new ImageView(ToggleManager.LOCK_IMAGE);
    final private ImageView unlockPasswordView = new ImageView(ToggleManager.UNLOCK_IMAGE);
    private boolean showSecret = ! Storage.showSecretDefault; // will be toggled to init


    @FXML
    public Label intervalOrcounterLabel;
    public TextField issuer;
    public TextField account;
    public PasswordField secretObfuscated;
    public TextField secretClear;
    public ComboBox<OTPType> type;
    public ComboBox<HashFunction> algorithm;
    public TextField digits;
    public TextField intervalOrcounter;
    public Label password;
    public Label passwordLabel;
    public Button qrCode;
    public Button toggleSecretButton;

    public ModifyToken() {
        ModifyToken.instance = this;
    }

    public static void openWindow() {openWindow(-1L);}
    public static void openWindow(Long tokenID) {
        currentStage = new Stage();
        // Using a new scene permits fields to start on an empty ground
        currentStage.setScene(new Scene((Parent) Objects.requireNonNull(Main.loadFXML("ModifyToken.fxml"))));
        currentStage.setOnCloseRequest((event) -> closeWindow());
        currentStage.setResizable(false);
        currentStage.initModality(Modality.APPLICATION_MODAL);

        // create thread or resume it
        if (countdownThread == null)
            countdownThread = new CountdownThread(instance, false);
        else
            countdownThread.scene = instance;

        instance.initScene(tokenID);

        currentStage.showAndWait();
    }

    private void initScene(Long tokenId) {
        // secretObfuscated and secretClear are on top of each other and content is linked, they are not visible at the same time
        secretObfuscated.textProperty().bindBidirectional(secretClear.textProperty());

        this.tokenId = tokenId;
        toggleSecret(null); // init secret showing or hiding

        if (tokenId == -1) {
            // ----- NEW TOKEN WINDOW -----
            currentStage.setTitle("New token");

            // Fill combobox choices
            type.getItems().setAll(OTPType.values());
            algorithm.getItems().setAll(HashFunction.values());

            // Provide default values
            type.getSelectionModel().select(OTPType.TOTP);
            algorithm.getSelectionModel().select(HashFunction.SHA1);
            intervalOrcounter.setText("30");
            digits.setText("6");

            // Add listeners that are in charge of verifying input
            issuer.textProperty().addListener((changed, oldvalue, newvalue) -> verifyNameFieldAndUpdate(issuer, oldvalue, newvalue));
            account.textProperty().addListener((changed, oldvalue, newvalue) -> verifyNameFieldAndUpdate(account, oldvalue, newvalue));
            secretObfuscated.textProperty().addListener((changed, oldvalue, newvalue) -> updateOTP());
            type.getSelectionModel().selectedItemProperty().addListener((changed, oldvalue, newvalue) -> verifyIntervalorCounterAndUpdate(intervalOrcounter, "30", intervalOrcounter.getText()));
            algorithm.getSelectionModel().selectedItemProperty().addListener((changed, oldvalue, newvalue) -> updateOTP());
            digits.textProperty().addListener((changed, oldvalue, newvalue) -> verifyDigitsAndUpdate(digits, oldvalue, newvalue));
            intervalOrcounter.textProperty().addListener((changed, oldvalue, newvalue) -> verifyIntervalorCounterAndUpdate(intervalOrcounter, oldvalue, newvalue));

        } else {
            // ----- MODIFY TOKEN WINDOW -----
            currentStage.setTitle("Modifying token");

            // Import token fields
            Token token = Storage.tokenMap.get(tokenId);
            importTokenToInput(token);

            // Only issuer and account can be modified, others fiels are disabled
            for (Control l : new Control[]{secretClear, secretObfuscated, type, algorithm, digits}) {
                l.setDisable(true);
                l.setStyle("-fx-opacity: 0.8;");
            }
            qrCode.setVisible(false);

            // Counter field can also be modified if token is an HOTP one
            if (OTPType.isTOTP(token)) {
                intervalOrcounterLabel.setText("Interval");
                intervalOrcounter.setDisable(true);
                intervalOrcounter.setStyle("-fx-opacity: 0.8;");
            } else
                intervalOrcounterLabel.setText("Counter");

            // Doesn't bother showing password
            password.setVisible(false);
            passwordLabel.setVisible(false);
        }
    }

    private void importTokenToInput(Token token) {
        issuer.setText(token.issuer);
        account.setText(token.account);
        secretObfuscated.setText(token.secretKey);
        type.getSelectionModel().select(token.otpType);
        algorithm.getSelectionModel().select(token.hashFunction);
        digits.setText(String.valueOf(token.digits));
        intervalOrcounter.setText(String.valueOf(token.intervalTOTP_OR_counterHOTP));
    }

// METHODS TO VERIFY INPUT ARE CORRECT
    public void verifyNameFieldAndUpdate(TextField textField, String oldvalue, String newvalue) {
        if ( ! newvalue.matches("(\\w|[ .@:-])*"))
            textField.setText(oldvalue);
    }

    public void verifyDigitsAndUpdate(TextField textField, String oldvalue, String newvalue) {
        // 1 to 12 are legal input, but making sense is another point...
        if ( ! newvalue.matches("([1-9]|(1[0-2]))?"))
            textField.setText(oldvalue);
        else
            updateOTP();
    }

    public void verifyIntervalorCounterAndUpdate(TextField textField, String oldvalue, String newvalue) {
        if (type.getSelectionModel().getSelectedItem() == OTPType.TOTP) {
            if ( ! newvalue.matches("([1-9][0-9]?)?")) { // 1s to 99s are legal...
                textField.setText(oldvalue);

            }
        } else {
        if ( ! newvalue.matches("[0-9]*") || BigInteger.valueOf(Long.MAX_VALUE).compareTo(new BigInteger(newvalue.equals("") ? "0" : newvalue)) == -1)
            // any positive long is legal...
            textField.setText(oldvalue);
        }
        updateOTP();
    }

    /*
     Update password field, but also if label text should be interval or counter
     Pause(unpause) also thread if secret is hidden(showing)
     */
    public void updateOTP() {
        if (type.getSelectionModel().getSelectedItem() == OTPType.TOTP) {
            intervalOrcounterLabel.setText("Interval");
            if (showSecret) {
                countdownThread.unPause();
            } else {
                countdownThread.pauseAndWait();
            }
        }
        else {
            intervalOrcounterLabel.setText("Counter");
            countdownThread.pauseAndWait();
        }
        if (showSecret) password.setText(getCurrentToken().getOTP());
    }

    // GETTERS FOR COUNTER THREAD
    @Override
    public Labeled getCounterLabel() { return new Label(); } // NOT IMPLEMENTED

    @Override
    public Label getPasswordLabel() {
        return password;
    }

    @Override
    public Token getCurrentToken() {
        if (intervalOrcounter.getText().equals("") || digits.getText().equals(""))
            return new Token("");
        else
            return new Token(issuer.getText(), account.getText(), secretObfuscated.getText(), type.getSelectionModel().getSelectedItem(), Long.parseLong(intervalOrcounter.getText()), Integer.parseInt(digits.getText()), algorithm.getSelectionModel().getSelectedItem());
    }


    @FXML
    public void createTokenButton(ActionEvent actionEvent) {
        // Verification 1 : issuer or account must exist
        if (issuer.getText().equals("") && account.getText().equals("")) {
            Dialog.simpleDialog(Alert.AlertType.WARNING, "Warning", "An issuer or an account must be provided");
            return;
        }

        // Verification 2 : password can be calculated
        Token token = getCurrentToken();
        if (token.getOTP() == null) {
            Dialog.simpleDialog(Alert.AlertType.WARNING, "Warning", "Invalid secret", "Modify your input");
            return;
        }

        Storage.modifyToken(tokenId, token);
        closeWindow();
    }

    @FXML
    public void toggleSecret(ActionEvent actionEvent) {
        showSecret = ! showSecret;
        if (showSecret) {
            secretObfuscated.setVisible(false);
            secretClear.setVisible(true);
            toggleSecretButton.setGraphic(unlockPasswordView);
            updateOTP();
        } else {
            secretObfuscated.setVisible(true);
            secretClear.setVisible(false);
            countdownThread.pauseAndWait();
            password.setText(ToggleManager.STARS_STRING);
            toggleSecretButton.setGraphic(lockPasswordView);
        }
    }

    @FXML
    public void importQRCodeButton(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select QR Code file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedFile = fileChooser.showOpenDialog(new Stage());
        try {
            Token t = QRCode.getTokenfromQRCodeFile(selectedFile.getAbsolutePath());
            importTokenToInput(t);
        } catch (QRCode.NoQRCodeFoundException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", null, "No QR Code have been found", e);
        } catch (QRCode.InvalidQRCodeException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", null, "QR Code do not correspond to a one-time password", e);
        } catch (IOException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", null, "File couldn't be opened", e);
        }

    }

    @FXML
    public void cancelButton(ActionEvent actionEvent) {
        closeWindow();
    }

    static public void closeWindow() {
        countdownThread.pauseAndWait();
        currentStage.close();
    }


}
