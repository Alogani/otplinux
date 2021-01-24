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

import com.alogani.otpcore.HashFunction;
import com.alogani.otpcore.OTPType;
import com.alogani.otpcore.Token;
import com.alogani.otplinux.scenes.Dialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import net.harawata.appdirs.AppDirsFactory;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/*
This class handles all the I/O between data objects that are saved on disk and the data files
It stores after import the necessary data in fields for the other class to use
 */
public class Storage {
    final public static Path DATADIR_PATH = Path.of(AppDirsFactory.getInstance().getUserDataDir("otplinux", null, "com.alogani"));

    private static boolean parametersOutOfSync = false;

    // Provides data about tokens
    public static List<Long> tokenIdList = new ArrayList<>();
    public static Map<Long, Token> tokenMap = new HashMap<>();

    // Provides data about parameters
    static public boolean showPasswordDefault;
    static public boolean showSecretDefault;
    static public boolean showDetailsByDefault;
    static public String separator;
    //static public String defaultLanguage; // may be implemented


    /*
     Search through database files to grab saved tokens and parameters
     \t is the standard separator used
     */
    public static void importData() throws Exception {
        if (! Files.exists(DATADIR_PATH))
            firstLaunch();

        // RECUPERATE TOKENS
        List<String> allTokenLines = Files.readAllLines(DATADIR_PATH.resolve("tokens"), StandardCharsets.UTF_8);
        for (String tokenLine : allTokenLines) {
            String[] tokenString = tokenLine.split("\\t");
            tokenMap.put(
                    Long.parseLong(tokenString[0]),
                    new Token(tokenString[1], tokenString[2], tokenString[3],OTPType.valueOf(tokenString[4]),
                            Long.parseLong(tokenString[5]), Integer.parseInt(tokenString[6]), HashFunction.valueOf(tokenString[7])
                    )
            );
        }

        // RECUPERATE PARAMETERS
        Map<String, String> parameters = new HashMap<>();
        List<String> allParametersLines = null;
        // ORDER THE TOKENS
        try {
            allParametersLines = Files.readAllLines(DATADIR_PATH.resolve("parameters"), StandardCharsets.UTF_8);
            // RECUPERATE ORDERED LIST
            for (String id : allParametersLines.get(0).split("\\t"))
                if (tokenMap.keySet().contains(id))
                    tokenIdList.add(Long.parseLong(id)); // only add entry corresponding to a token

            // If a tokenID in tokenMap doesn't exist in tokenIDList then add it
            for (Long tokenId : tokenMap.keySet())
                if (! tokenIdList.contains(tokenId))
                    tokenIdList.add(tokenId);
        } catch (Exception ignored) {
            // ORDER OF CREATION IF IOEXCEPTION (OR OTHERS) ON PARAMETERS FILE
            tokenIdList.addAll(tokenMap.keySet());
        }

        try {
            for (int i = 1; i < allParametersLines.size(); i++) {
                String[] keyAndValue = allParametersLines.get(i).split("\t");
                parameters.put(keyAndValue[0], keyAndValue[1]);
            }
        } catch (NullPointerException ignored) {}

        // Save parameters imported in fields, or if not found, assign default values
        showPasswordDefault = parameters.get("showPassword") != null && parameters.get("showPassword").equalsIgnoreCase("true");
        showSecretDefault = parameters.get("showSecret") != null && parameters.get("showSecret").equalsIgnoreCase("true");
        showDetailsByDefault = parameters.get("showDetails") != null && parameters.get("showDetails").equalsIgnoreCase("true");
        separator = parameters.get("separator") != null ? parameters.get("separator") : "newline";
        if (separator.equals("newline"))
            separator = "\n";

    }

    private static void firstLaunch() {
        try {
            Files.createDirectory(DATADIR_PATH);
            Files.createFile(DATADIR_PATH.resolve("tokens"));
            Files.createFile(DATADIR_PATH.resolve("parameters"));
        } catch (IOException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", "Unable to create application data directory.",
                    "This is normally the first time you launch the app\nBut something wrong occured when trying to create : " + DATADIR_PATH, e);
            Platform.exit();
            System.exit(1);
        }
    }


    /*
    This class is an utility to handle all the write I/O. It does the following :
    - Create a backup of the data
    - Write objects into a temporary file
    - Replace the data file with the temporary one
    - It doesn't handle how the objects will be transformed into valid data (it's via the abstract filler method)
     */
    private static abstract class Syncer /* extends Thread... would be implemented here */{
        // The buffered writer object must be filled with objects beeing saved thanks to the filler method
        BufferedWriter bufferedWriter;

        private final Path datafilePath;
        private final Path backupPath;
        private final Path tmpPath;

        abstract void filler() throws IOException;

        Syncer(String filename) {
            datafilePath = DATADIR_PATH.resolve(filename);
            backupPath = DATADIR_PATH.resolve(filename + ".bak");
            tmpPath = DATADIR_PATH.resolve(filename + ".tmp");
        }

        void sync() throws IOException {
            if (Files.exists(datafilePath))
                Files.copy(datafilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tmpPath);
            Files.createFile(tmpPath);

            bufferedWriter = Files.newBufferedWriter(tmpPath, StandardCharsets.UTF_8);
            filler();
            bufferedWriter.close();

            Files.move(tmpPath, datafilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Implement filler method by saying how tokens will be saved. Also handles exceptions
    public static void syncTokens() {
        Syncer syncer = new Syncer("tokens") {
            @Override
            void filler() throws IOException {
                for (long tokenID : tokenMap.keySet()) {
            Token token = tokenMap.get(tokenID);
            bufferedWriter.append(String.valueOf(tokenID)).append('\t')
                    .append(token.issuer).append('\t')
                    .append(token.account).append('\t')
                    .append(token.secretKey).append('\t')
                    .append(token.otpType.name()).append("\t")
                    .append(String.valueOf(token.intervalTOTP_OR_counterHOTP)).append('\t')
                    .append(String.valueOf(token.digits)).append('\t')
                    .append(token.hashFunction.name()).append('\n');
                }
            }
        };

        try {
            syncer.sync();
        } catch (IOException e) {
            Dialog.simpleDialog(Alert.AlertType.ERROR, "Error", "Database error", "Something wrong happened while trying to update database", e);
        }
    }

    // Implement filler method by saying how parameters will be saved. Also handles exceptions
    public static void syncParameters(boolean mustSync) {
        /*
        Parameters will be synced if :
        - requested (after parameters window has been closed, represented by "mustSync" argument)
        - when the app close if needed (e.g. : change in token orders have happened)
         */
        if (! (parametersOutOfSync || mustSync))
            return;

        Syncer syncer = new Syncer("parameters") {

                @Override
                void filler() throws IOException {
                    for (Long tokenID : tokenIdList)
                        bufferedWriter.append(String.valueOf(tokenID)).append('\t'); // Save the order of the tokens
                    bufferedWriter.append("\n");

                    bufferedWriter.append("showPassword").append('\t').append(String.valueOf(showPasswordDefault)).append('\n');
                    bufferedWriter.append("showSecret").append('\t').append(String.valueOf(showSecretDefault)).append('\n');
                    bufferedWriter.append("showDetails").append('\t').append(String.valueOf(showDetailsByDefault)).append('\n');
                    bufferedWriter.append("separator").append('\t').append(separator.equals("\n") ? "newline" : separator).append('\n');
                }
            };

        try {
            syncer.sync();
        } catch (IOException e) {
            Dialog.simpleDialog(Alert.AlertType.WARNING, "Warning", "Database warning", "Something wrong happened while trying to save parameters", e);
        }
        parametersOutOfSync = false;
    }


    /*
     The following methods manage tokenIdList and tokenMaps when a user modification has occured
     They then ask for syncing to prevent crash loss of data
     */
    public static void addToken(Token token) {
        long newID;
        if (tokenIdList.size() == 0)
            newID = 0L;
        else
            newID = Collections.max(tokenIdList) + 1L;
        tokenIdList.add(newID);
        tokenMap.put(newID, token);
        syncTokens();
    }

    public static void modifyToken(Long tokenID, Token token) {
        if (tokenID == -1L) addToken(token);
        tokenMap.replace(tokenID, token);
        syncTokens();
    }

    public static void deleteToken(Long tokenID) {
        tokenIdList.remove(tokenID);
        tokenMap.remove(tokenID);
        syncTokens();
    }

    public static void moveTokens(int sourceIndex, int destinationIndex) {
        Collections.swap(tokenIdList, sourceIndex, destinationIndex);
        parametersOutOfSync = true;
    }

    // Combine issuer + separator + token to have some nice infos in listview
    public static ObservableList<String> getNiceList() {
        // FOR SHOWING ON LIST VIEW
        ObservableList<String> observableList = FXCollections.observableArrayList();
        for (Long tokenId : tokenIdList) {
            Token token = tokenMap.get(tokenId);
            observableList.add(token.issuer +
                    ((token.issuer.equals("") || token.account.equals("")) ? "" : separator) +
                    token.account);
        }
        return observableList;
    }
}
