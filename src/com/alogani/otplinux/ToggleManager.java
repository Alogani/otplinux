package com.alogani.otplinux;

import com.alogani.otpcore.OTPType;
import com.alogani.otpcore.Token;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/*
Handle (at least for Primary scene) :
- the counter thread
- showing and hidden of password field
- the password toggle button image
Also provides this constants LOCK_IMAGE, UNLOCK_IMAGE, STARS_STRING that can be used by other class
 */
public class ToggleManager {
    public boolean showPassword = Storage.showPasswordDefault;

    private final CountdownThread threadInstance;
    private final Button togglePasswordButton;
    private final Label password;

    // Some icons
    static public final Image LOCK_IMAGE = new Image("icons/lock.png");
    static public final Image UNLOCK_IMAGE = new Image("icons/unlock.png");
    static public final String STARS_STRING = "\u2217".repeat(8); // "∗∗∗∗∗∗∗∗" for hidden fields

    private final ImageView lockPasswordView = new ImageView(LOCK_IMAGE);
    private  final ImageView unlockPasswordView = new ImageView(UNLOCK_IMAGE);


    public ToggleManager(CountdownThread threadInstance, Button togglePasswordButton, Label password) {
        this.threadInstance = threadInstance;
        this.togglePasswordButton = togglePasswordButton;
        this.password = password;
    }

    public void togglePassword(Token currentToken) {
        if (currentToken == null) return;
        if (showPassword)
            hidePassword();
        else
            if (OTPType.isTOTP(currentToken))
                showPasswordTOTP(currentToken);
            else
                showPasswordHOTP(currentToken);
    }

    public void hidePassword() {
        threadInstance.pauseAndWait();
        togglePasswordButton.setGraphic(lockPasswordView);
        togglePasswordButton.setText("");
        showPassword = false;
        password.setText(STARS_STRING);
    }

    public void showPasswordTOTP(Token currentToken) {
        if (currentToken == null) return;
        togglePasswordButton.setGraphic(null);
        password.setText(currentToken.getOTP());
        togglePasswordButton.setText(String.valueOf(currentToken.timeBeforeReset()));
        threadInstance.unPause();
        showPassword = true;
    }

    public void showPasswordHOTP(Token currentToken) {
        if (currentToken == null) return;
        password.setText(currentToken.getOTP());
        togglePasswordButton.setGraphic(unlockPasswordView);
        showPassword = true;
    }

}
