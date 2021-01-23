package com.alogani.otplinux.scenes;

import com.alogani.otpcore.Token;

import javafx.scene.control.Label;
import javafx.scene.control.Labeled;

/*
CountdownThread manipulate both countdown and password update on multiple scenes
This interface provides CountdownThread the tools to get the job done
 */
public interface CounterScene {
    Labeled getCounterLabel();
    Label getPasswordLabel();
    Token getCurrentToken();
}
