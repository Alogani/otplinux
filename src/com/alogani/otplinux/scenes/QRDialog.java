package com.alogani.otplinux.scenes;

import com.alogani.otpcore.QRCode;
import com.alogani.otpcore.Token;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class QRDialog {
    private Stage myStage;

    public QRDialog(Token token) {
        myStage = new Stage();

        // Create bufferImage from QRCode utilities, which can be used later
        BufferedImage bufferedImage;
        try {
            bufferedImage = QRCode.generateQRCode(token, 300, 300);
        } catch (Exception e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", null, "QR Code couldn't be generated", e);;
            return;
        }

        // ---- GUI ELEMENTS -----
        // Get the qrcode in an image view
        ImageView qrView = new ImageView();
        qrView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

        // Implements buttons
        Button save = new Button("Save");
        Button close = new Button("Close");
        save.setMinWidth(80.0);
        close.setMinWidth(80.0);
        save.setOnAction((event) -> saveImage(bufferedImage, token));
        close.setOnAction((event) -> myStage.close());

        // Set the container for buttons
        HBox hboxButton = new HBox();
        hboxButton.getChildren().addAll(save, close);
        hboxButton.setSpacing(20.0);
        hboxButton.setAlignment(Pos.CENTER_RIGHT);
        hboxButton.setPadding(new Insets(0.0,20.0,0.0,0.0));

        // Set the layout
        VBox root = new VBox();
        root.getChildren().addAll(qrView, hboxButton);
        root.setSpacing(10.0);
        // ------------------------------


        myStage.setTitle("QR Code");
        myStage.setScene(new Scene(root, 300, 350));
        myStage.setResizable(false);
        myStage.showAndWait();
    }

    private void saveImage(BufferedImage bufferedImage, Token token) {
        FileChooser fileChooser = new FileChooser();
        // Add some filter the user can use
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files (*.*)", "*.*"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.png)", "*.png"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.jpg)", "*.jpg"));

        // Default filename
        fileChooser.setInitialFileName("QRCode_" +
                (token.issuer.equals("") ? token.account : token.issuer) +
                ".png");
        File file = fileChooser.showSaveDialog(new Stage());

        if (file == null) {
            return; // No file choosen
        }

        String path = file.getAbsolutePath();
        String extension = path.substring(path.lastIndexOf('.') + 1);

        if (! extension.matches("png|jpg")) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", "Wrong extension\nFile must be .png, or .jpg");
            return;
        }

        File outputfile = new File(path);
        try {
            ImageIO.write(bufferedImage, extension, outputfile);
        } catch (IOException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", null, "File counldn't be saved", e);
        }
    }

}
