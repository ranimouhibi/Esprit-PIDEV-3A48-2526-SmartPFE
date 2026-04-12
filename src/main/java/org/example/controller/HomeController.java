package org.example.controller;

import org.example.util.NavigationUtil;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    public void handleSignIn() {
        NavigationUtil.navigateTo("Login.fxml");
    }
}
