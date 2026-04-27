package org.example.controller;

import org.example.dao.MeetingDAO;
import org.example.dao.MeetingParticipantDAO;
import org.example.dao.MeetingReportDAO;
import org.example.model.Meeting;
import org.example.model.User;
import org.example.service.EmailService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class MeetingController implements Initializable {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane meetingsContainer;
    @FXML private Label statTotal, statPending, statConfirmed, statCancelled, statUpcoming;
    private final MeetingDAO meetingDAO = new MeetingDAO();
    private final MeetingReportDAO reportDAO = new MeetingReportDAO();
    private final MeetingParticipantDAO participantDAO = new MeetingParticipantDAO();
    private final EmailService emailService = new EmailService();
    private static final Logger LOG = Logger.getLogger(MeetingController.class.getName());
    private List<Meeting> allMeetings = new ArrayList<>();
    private List<Meeting> filteredMeetings = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterType.setItems(FXCollections.observableArrayList("ALL","ONLINE","IN_PERSON","HYBRID"));
        filterType.setValue("ALL");
        filterStatus.setItems(FXCollections.observableArrayList("ALL","PENDING","CONFIRMED","CANCELLED"));
        filterStatus.setValue("ALL");
        sortCombo.setItems(FXCollections.observableArrayList("Date: Newest first","Date: Oldest first","Title: A to Z","Title: Z to A"));
        sortCombo.setValue("Date: Newest first");
        searchField.textProperty().addListener((o,a,b)->applyFilters());
        filterType.valueProperty().addListener((o,a,b)->applyFilters());
        filterStatus.valueProperty().addListener((o,a,b)->applyFilters());
        sortCombo.valueProperty().addListener((o,a,b)->applyFilters());
        loadMeetings();
    }

    private void loadMeetings() {
        try { allMeetings = meetingDAO.findAll(null,"ALL","ALL","scheduledDate","DESC",1,1000); }
        catch (Exception e) { LOG.severe("loadMeetings: "+e.getMessage()); allMeetings=new ArrayList<>(); }
        updateStats(); applyFilters();
    }

    private void updateStats() {
        try {
            statTotal.setText(String.valueOf(meetingDAO.countTotal()));
            Map<String,Integer> s = meetingDAO.countByStatus();
            statPending.setText(String.valueOf(s.getOrDefault("PENDING",0)));
            statConfirmed.setText(String.valueOf(s.getOrDefault("CONFIRMED",0)));
            statCancelled.setText(String.valueOf(s.getOrDefault("CANCELLED",0)));
            statUpcoming.setText(String.valueOf(meetingDAO.countUpcoming()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applyFilters() {
        String q = searchField.getText()!=null?searchField.getText().toLowerCase().trim():"";
        String tf=filterType.getValue(), sf=filterStatus.getValue(), sort=sortCombo.getValue();
        filteredMeetings=new ArrayList<>();
        for (Meeting m:allMeetings) {
            boolean mq=q.isEmpty()||(m.getProjectTitle()!=null&&m.getProjectTitle().toLowerCase().contains(q))||(m.getLocation()!=null&&m.getLocation().toLowerCase().contains(q));
            boolean mt="ALL".equals(tf)||tf.equals(m.getMeetingType());
            boolean ms="ALL".equals(sf)||sf.equals(m.getStatus());
            if(mq&&mt&&ms) filteredMeetings.add(m);
        }
        if ("Date: Oldest first".equals(sort)) {
            filteredMeetings.sort((a,b)->{ if(a.getScheduledDate()==null)return 1; if(b.getScheduledDate()==null)return -1; return a.getScheduledDate().compareTo(b.getScheduledDate()); });
        } else if ("Title: A to Z".equals(sort)) {
            filteredMeetings.sort((a,b)->{ String ta=a.getProjectTitle()!=null?a.getProjectTitle():""; String tb=b.getProjectTitle()!=null?b.getProjectTitle():""; return ta.compareToIgnoreCase(tb); });
        } else if ("Title: Z to A".equals(sort)) {
            filteredMeetings.sort((a,b)->{ String ta=a.getProjectTitle()!=null?a.getProjectTitle():""; String tb=b.getProjectTitle()!=null?b.getProjectTitle():""; return tb.compareToIgnoreCase(ta); });
        } else {
            filteredMeetings.sort((a,b)->{ if(a.getScheduledDate()==null)return 1; if(b.getScheduledDate()==null)return -1; return b.getScheduledDate().compareTo(a.getScheduledDate()); });
        }
        displayMeetings(filteredMeetings);
    }

    @FXML public void handleSearch() { applyFilters(); }
    @FXML public void handleReset() { searchField.clear(); filterType.setValue("ALL"); filterStatus.setValue("ALL"); sortCombo.setValue("Date: Newest first"); }
    @FXML public void handleAdd() { openDialog("MeetingFormDialog.fxml",600,620,ctrl->{ if(ctrl instanceof MeetingFormDialogController) ((MeetingFormDialogController)ctrl).initCreate(m->loadMeetings()); }); }
    @FXML public void handleCalendar() { openDialog("MeetingCalendar.fxml",900,680,ctrl->{}); }
    @FXML public void handleChatbot() { openDialog("MeetingChatbot.fxml",480,580,ctrl->{}); }
    @FXML public void handleStats() { openDialog("MeetingStatsDialog.fxml",620,520,ctrl->{}); }

    private void displayMeetings(List<Meeting> list) {
        meetingsContainer.getChildren().clear();
        if(list.isEmpty()) { meetingsContainer.getChildren().add(buildEmpty()); return; }
        for(Meeting m:list) meetingsContainer.getChildren().add(buildCard(m));
    }

    private VBox buildEmpty() {
        VBox b=new VBox(12); b.setAlignment(Pos.CENTER); b.setPrefSize(400,200); b.setPadding(new Insets(30));
        b.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#e0e0e0;-fx-border-radius:12;-fx-border-width:2;-fx-border-style:dashed;");
        Label l=new Label("No meetings found"); l.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#666;");
        b.getChildren().add(l); return b;
    }

    private VBox buildCard(Meeting m) {
        VBox card=new VBox(12); card.setPrefWidth(340); card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:white;-fx-background-radius:14;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);-fx-border-color:#f0f0f0;-fx-border-radius:14;-fx-border-width:1;");
        HBox hdr=new HBox(10); hdr.setAlignment(Pos.CENTER_LEFT);
        String ts=m.getMeetingType()!=null?m.getMeetingType():"";
        String tc; if("ONLINE".equals(ts))tc="#3b82f6"; else if("IN_PERSON".equals(ts))tc="#10b981"; else if("HYBRID".equals(ts))tc="#f59e0b"; else tc="#6b7280";
        Label tb=new Label(ts.isEmpty()?"N/A":ts); tb.setStyle("-fx-background-color:"+tc+"22;-fx-text-fill:"+tc+";-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:4 10;");
        Region sp=new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
        String ss=m.getStatus()!=null?m.getStatus():"";
        String sc; if("CONFIRMED".equals(ss))sc="#22c55e"; else if("CANCELLED".equals(ss))sc="#ef4444"; else sc="#f59e0b";
        Label sb=new Label("● "+(ss.isEmpty()?"PENDING":ss)); sb.setStyle("-fx-text-fill:"+sc+";-fx-font-size:11px;-fx-font-weight:bold;");
        hdr.getChildren().addAll(tb,sp,sb);
        Label title=new Label(m.getProjectTitle()!=null?m.getProjectTitle():"No project");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:16px;-fx-text-fill:#1e1e1e;"); title.setWrapText(true); title.setMaxWidth(300);
        HBox dr=new HBox(6); dr.setAlignment(Pos.CENTER_LEFT);
        String ds=m.getScheduledDate()!=null?m.getScheduledDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")):"N/A";
        Label dv=new Label(ds+"  |  "+m.getDuration()+" min"); dv.setStyle("-fx-text-fill:#555;-fx-font-size:13px;");
        dr.getChildren().addAll(new Label("📅 "),dv);
        HBox lr=new HBox(6); lr.setAlignment(Pos.CENTER_LEFT);
        Label lv=new Label(m.getLocation()!=null?m.getLocation():"N/A"); lv.setStyle("-fx-text-fill:#888;-fx-font-size:12px;"); lv.setWrapText(true); lv.setMaxWidth(280);
        lr.getChildren().addAll(new Label("📍 "),lv);
        card.getChildren().addAll(hdr,title,dr,lr);
        if("ONLINE".equals(m.getMeetingType())&&m.getMeetingLink()!=null&&!m.getMeetingLink().isBlank()) {
            VBox lb=new VBox(4); lb.setPadding(new Insets(8)); lb.setStyle("-fx-background-color:#a12c2f18;-fx-background-radius:8;");
            Label lt=new Label("🎥 Online Meeting"); lt.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#a12c2f;");
            Label ll=new Label(m.getMeetingLink()); ll.setStyle("-fx-text-fill:#2980b9;-fx-font-size:10px;"); ll.setWrapText(true); ll.setMaxWidth(300);
            lb.getChildren().addAll(lt,ll); card.getChildren().add(lb);
        }
        card.getChildren().add(new Separator());
        HBox act=new HBox(8); act.setAlignment(Pos.CENTER_LEFT);
        Button d=new Button("👁 Details"); d.setStyle("-fx-background-color:#374151;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:6;-fx-padding:6 14;-fx-cursor:hand;");
        d.setOnAction(e->openDialog("MeetingDetailDialog.fxml",560,680,ctrl->{ if(ctrl instanceof MeetingDetailDialogController)((MeetingDetailDialogController)ctrl).init(m); }));
        Button ed=new Button("✏️ Edit"); ed.setStyle("-fx-background-color:#a12c2f;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:6;-fx-padding:6 14;-fx-cursor:hand;");
        ed.setOnAction(e->openDialog("MeetingFormDialog.fxml",600,620,ctrl->{ if(ctrl instanceof MeetingFormDialogController)((MeetingFormDialogController)ctrl).initEdit(m,u->loadMeetings()); }));
        Button rp=new Button("📋 Reports"); rp.setStyle("-fx-background-color:#4b5563;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:6;-fx-padding:6 14;-fx-cursor:hand;");
        rp.setOnAction(e->openDialog("MeetingReports.fxml",1050,680,ctrl->{ if(ctrl instanceof MeetingReportController)((MeetingReportController)ctrl).initForMeeting(m); }));
        Button del=new Button("🗑️"); del.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:6;-fx-padding:6 10;-fx-cursor:hand;");
        del.setOnAction(e->handleDelete(m));
        act.getChildren().addAll(d,ed,rp,del);
        card.getChildren().add(act);
        return card;
    }

    private void handleDelete(Meeting meeting) {
        Alert c=new Alert(Alert.AlertType.CONFIRMATION,"Supprimer ce meeting et ses reports ?",ButtonType.YES,ButtonType.NO);
        c.setHeaderText(null);
        c.showAndWait().ifPresent(btn->{ if(btn==ButtonType.YES) {
            try {
                if(emailService.isEnabled()) new Thread(()->{ try { List<User> r=participantDAO.findParticipantsAndSupervisor(meeting.getId()); if(!r.isEmpty())emailService.sendMeetingCancellation(meeting,r); } catch(Exception ex){LOG.warning(ex.getMessage());} }).start();
                reportDAO.deleteByMeeting(meeting.getId()); meetingDAO.delete(meeting.getId()); loadMeetings();
            } catch(Exception e){e.printStackTrace();showAlert("Erreur suppression.");}
        }});
    }

    private void openDialog(String fxml,double w,double h,java.util.function.Consumer<Object> setup) {
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/"+fxml));
            Pane root=loader.load(); setup.accept(loader.getController());
            Stage stage=new Stage(); stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root,w,h));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.showAndWait();
        } catch(Exception e){e.printStackTrace();}
    }

    private void showAlert(String msg){new Alert(Alert.AlertType.WARNING,msg,ButtonType.OK).showAndWait();}
}
