package com.alogani.otplinux.scenes;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Optional;

public class Dialog {

    // Simple dialog wrapper
    static public void simpleDialog(AlertType alertType, String title, String content) { simpleDialog(alertType, title, null, content, null); }
    static public void simpleDialog(AlertType alertType, String title, String header, String content) { simpleDialog(alertType, title, header, content, null); }
    static public void simpleDialog(AlertType alertType, String title, String header, String content, Exception e) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        // Custom size of the dialog determined by the content length (or lengthiest line if multiline)
        alert.getDialogPane().setMinWidth(((int) Arrays.asList(content.split("\\n"))
                .stream().map(String::length).max(Integer::compareTo).get()
                * 7.4f));
        if (e != null)
            alert.getDialogPane().setExpandableContent(getExpandableContent(e));

        alert.showAndWait();
    }

    // Permit dialog to show exception stacktrace if willed
    static private GridPane getExpandableContent(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);


        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane gridPane = new GridPane();
        gridPane.add(label, 0 ,0);
        gridPane.add(textArea, 0, 1);

        return gridPane;
    }

    /*
    Dialog wrapper with ;
    - Ok / Cancel
    - Some italic to highlight important stuff
    - return true if user click OK
     */
    static public boolean askConfirmation(String title, String header, String stringNormal, String stringItalic) {
        Alert alert = new Alert(AlertType.WARNING);

        // Here is the italic stuff
        TextFlow flow = new TextFlow();
        Text textItalic = new Text(stringItalic);
        textItalic.setStyle("-fx-font-style: italic");
        if (stringNormal.equals(""))
            flow.getChildren().setAll(textItalic);
        else {
            flow.getChildren().addAll(new Text(stringNormal), new Text("\n\n"), textItalic);
            alert.getDialogPane().setMinHeight(180);
        }

        // Init dialog
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setContent(flow);
        alert.getDialogPane().setMinWidth(400);

        // MAKE FALSE THE DEFAULT BUTTON
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDefaultButton(false);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setDefaultButton(true);

        // Get result
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && (result.get() == ButtonType.OK);
    }
}
