package org.example.util;

import org.example.controller.ApplyDialogController;
import org.example.controller.OfferFormDialogController;
import org.example.model.Offer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class DialogUtil {

    /** Open the modern Apply dialog for a student. onSuccess runs after successful submission. */
    public static void openApplyDialog(Offer offer, Window owner, Runnable onSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogUtil.class.getResource("/fxml/ApplyDialog.fxml"));
            StackPane root = loader.load();
            ApplyDialogController ctrl = loader.getController();

            Stage stage = buildStage(owner);
            ctrl.setOffer(offer);
            ctrl.setDialogStage(stage);
            ctrl.setOnSuccess(onSuccess);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Could not open apply form: " + e.getMessage());
        }
    }

    /** Open the modern Offer form dialog (create or edit). onSuccess runs after save. */
    public static void openOfferFormDialog(Offer offerToEdit, Window owner, Runnable onSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogUtil.class.getResource("/fxml/OfferFormDialog.fxml"));
            StackPane root = loader.load();
            OfferFormDialogController ctrl = loader.getController();

            Stage stage = buildStage(owner);
            if (offerToEdit != null) ctrl.setOffer(offerToEdit);
            ctrl.setDialogStage(stage);
            ctrl.setOnSuccess(onSuccess);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Could not open offer form: " + e.getMessage());
        }
    }

    private static Stage buildStage(Window owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);
        return stage;
    }
}
