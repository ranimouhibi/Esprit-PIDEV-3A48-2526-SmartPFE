// Test rapide pour voir si le FXML se charge
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class TestMeetingLoad {
    public static void main(String[] args) {
        try {
            FXMLLoader loader = new FXMLLoader(TestMeetingLoad.class.getResource("/fxml/Meetings.fxml"));
            Pane pane = loader.load();
            System.out.println("SUCCESS: FXML loaded, pane children count: " + pane.getChildren().size());
        } catch (Exception e) {
            System.err.println("ERROR loading FXML:");
            e.printStackTrace();
        }
    }
}
