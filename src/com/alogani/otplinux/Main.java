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

package com.alogani.otplinux;


import com.alogani.otpcore.TimeProvider;
import com.alogani.otpcore.Token;
import com.alogani.otplinux.scenes.Dialog;
import com.alogani.otplinux.scenes.Primary;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;


public class Main extends Application {
    // Here is the utility to get the current time
    static public TimeProvider timeProvider = new TimeProvider();

    // Method to load FXML Files
    public static Node loadFXML(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            /*
            Code to implement langage
            E.g. means rootdir/locale/locale will be used as default langage
            and rootdir/locale/locale.fr for french
            // Locale locale = Locale.getDefault();
            // ResourceBundle bundle = ResourceBundle.getBundle("locale/locale", locale);
            // loader.setResources(bundle);
             */

            loader.setLocation(Main.class.getResource("/com/alogani/otplinux/scenes/" + fxmlFileName));
            return loader.load();
        } catch (IOException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Fatal Error", "Internal error", "The graphical components haven't been found", e);
            Platform.exit();
            System.exit(1);
            return null;
        }
    }


    public static void main(String[] args) {launch(args);}


    @Override
    public void start(Stage primaryStage) {
        /*
        Time is not synced yet to NTP servers to avoid overloading them.
        timeProvider.updateTime(new String[] {"0.pool.ntp.org", "1.pool.ntp.org"}, true);
        Token.timeProvider = timeProvider; // make otp up to date
         */


        // Data is imported from datafile
        try {
            Storage.importData();
        } catch (Exception e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", "Database error", "Something wrong happened while trying to import tokens from database", e);
            if(! Dialog.askConfirmation("Continue", "Do you wish to continue nontheless ?", "", "Precedent database might be overidden"))
                Platform.exit();
        }

        // Main window is launched
        Primary.openWindow(primaryStage);
    }

    @Override
    public void stop() {
        // Kill all countdown threads
        CountdownThread.terminateAll();
        // Save some parameters config (e.g. the order of tokens in the list view is only saved on closing)
        Storage.syncParameters(false);
    }



}
