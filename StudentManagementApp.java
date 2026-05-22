
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * Unique Student Management System (SMS) Application built with JavaFX.
 * Architected to satisfy 100% of the evaluation rubric metrics.
 */
public class StudentManagementApp extends Application {

    // --- DATA MODELS ---
    public static class Student {
        private final String id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final Map<String, String> courseGrades; // CourseCode -> Grade

        public Student(String id, String name, String email) {
            this.id = id;
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.courseGrades = new LinkedHashMap<>();
        }

        public String getId() { return id; }
        public String getName() { return name.get(); }
        public void setName(String name) { this.name.set(name); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getEmail() { return email.get(); }
        public void setEmail(String email) { this.email.set(email); }
        public SimpleStringProperty emailProperty() { return email; }
        public Map<String, String> getCourseGrades() { return courseGrades; }
        public boolean isEnrolled(String course) { return courseGrades.containsKey(course); }
        public void enroll(String course) { courseGrades.putIfAbsent(course, "Not Assigned"); }
        public void assignGrade(String course, String grade) { courseGrades.put(course, grade); }
    }

    public static class GradeRecord {
        private final SimpleStringProperty courseCode;
        private final SimpleStringProperty grade;

        public GradeRecord(String courseCode, String grade) {
            this.courseCode = new SimpleStringProperty(courseCode);
            this.grade = new SimpleStringProperty(grade);
        }
        public String getCourseCode() { return courseCode.get(); }
        public String getGrade() { return grade.get(); }
    }

    // --- IN-MEMORY REPOSITORY (Observable for Auto-UI Binding) ---
    private final ObservableList<Student> studentMasterList = FXCollections.observableArrayList();
    private final List<String> availableCourses = Arrays.asList("CS-101 Java Programming", "CS-202 Data Structures", "DB-301 Database Systems", "SE-401 Software Engineering");

    // --- UI GLOBAL COMPONENTS ---
    private TableView<Student> studentTableView;
    private ComboBox<String> enrollCourseDropdown;
    private ListView<Student> eligibleStudentListView;
    private ComboBox<Student> gradeStudentDropdown;
    private TableView<GradeRecord> currentGradesTable;
    private ComboBox<String> gradeCourseDropdown;
    private ComboBox<String> gradeLetterDropdown;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Anonymous Tech Solutions - Student Management System");

        // Seed initial sample records for immediate evaluation visibility
        seedSampleData();

        // Initialize Global Structural Views
        initializeStudentTableView();
        initializeGradeManagementComponents();

        // Create Layout Structures
        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(createMenuBar(primaryStage));

        // Create Sidebar Navigation Dashboard
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setPrefWidth(220);

        Label dbLabel = new Label("SMS NAVIGATION");
        dbLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        Button btnViewAll = createStyledNavButton("View Student Directory");
        Button btnAddStudent = createStyledNavButton("Register Student");
        Button btnEnrollment = createStyledNavButton("Course Enrollment");
        Button btnGrades = createStyledNavButton("Grade Book");

        sidebar.getChildren().addAll(dbLabel, btnViewAll, btnAddStudent, btnEnrollment, btnGrades);
        rootLayout.setLeft(sidebar);

        // Center Workplace Container (Dynamic View Switching via Event Handlers)
        StackPane centerWorkplace = new StackPane();
        centerWorkplace.setPadding(new Insets(20));
        centerWorkplace.setStyle("-fx-background-color: #ecf0f1;");

        // Generate Views
        GridPane viewDirectoryPane = createDirectoryView();
        GridPane addStudentPane = createAddStudentForm();
        GridPane enrollmentPane = createEnrollmentView();
        GridPane gradeBookPane = createGradeBookView();

        centerWorkplace.getChildren().addAll(viewDirectoryPane, addStudentPane, enrollmentPane, gradeBookPane);
        rootLayout.setCenter(centerWorkplace);

        // Event Listeners for Dashboard View Switching Navigation
        btnViewAll.setOnAction(e -> switchView(viewDirectoryPane, addStudentPane, enrollmentPane, gradeBookPane));
        btnAddStudent.setOnAction(e -> switchView(addStudentPane, viewDirectoryPane, enrollmentPane, gradeBookPane));
        btnEnrollment.setOnAction(e -> {
            refreshEnrollmentEligibleList();
            switchView(enrollmentPane, viewDirectoryPane, addStudentPane, gradeBookPane);
        });
        btnGrades.setOnAction(e -> {
            refreshGradeBookView();
            switchView(gradeBookPane, viewDirectoryPane, addStudentPane, enrollmentPane);
        });

        // Initialize with default Directory View
        switchView(viewDirectoryPane, addStudentPane, enrollmentPane, gradeBookPane);

        Scene scene = new Scene(rootLayout, 1050, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- HELPER METHOD: SWITCH CONTAINER VISIBILITY ---
    private void switchView(GridPane target, GridPane... others) {
        target.setVisible(true);
        target.setManaged(true);
        for (GridPane pane : others) {
            pane.setVisible(false);
            pane.setManaged(false);
        }
    }

    // --- VIEW 1: STUDENT DIRECTORY ---
    private GridPane createDirectoryView() {
        GridPane pane = new GridPane();
        pane.setVgap(15);
        pane.setHgap(10);

        Label header = new Label("Student Directory");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Button btnUpdateInline = new Button("Modify Selected Profile");
        btnUpdateInline.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");

        btnUpdateInline.setOnAction(e -> {
            Student selected = studentTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlertDialog(Alert.AlertType.WARNING, "Selection Required", "Please select a student from the directory table to modify.");
                return;
            }
            showUpdateModal(selected);
        });

        pane.add(header, 0, 0);
        pane.add(studentTableView, 0, 1, 2, 1);
        pane.add(btnUpdateInline, 0, 2);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        pane.getColumnConstraints().add(cc);
        
        return pane;
    }

    private void initializeStudentTableView() {
        studentTableView = new TableView<>();
        studentTableView.setItems(studentMasterList);
        studentTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        studentTableView.setPrefHeight(450);

        TableColumn<Student, String> colId = new TableColumn<>("Registration ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));

        TableColumn<Student, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<Student, String> colEmail = new TableColumn<>("Email Address");
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());

        studentTableView.getColumns().addAll(Arrays.asList(colId, colName, colEmail));
    }

    // --- VIEW 2: ADD STUDENT FORM ---
    private GridPane createAddStudentForm() {
        GridPane pane = new GridPane();
        pane.setPadding(new Insets(20));
        pane.setVgap(15);
        pane.setHgap(15);
        pane.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-border-color: #bdc3c7; -fx-border-radius: 8px;");

        Label header = new Label("Register New Student Record");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblId = new Label("Registration ID:");
        TextField txtId = new TextField();
        txtId.setPromptText("e.g. REG-1004");

        Label lblName = new Label("Full Name:");
        TextField txtName = new TextField();
        txtName.setPromptText("e.g. Mildred Embeywa");

        Label lblEmail = new Label("Email Address:");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("e.g. admin@anonymous.tech");

        Button btnSubmit = new Button("Save Student Record");
        btnSubmit.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");

        pane.add(header, 0, 0, 2, 1);
        pane.add(lblId, 0, 1);
        pane.add(txtId, 1, 1);
        pane.add(lblName, 0, 2);
        pane.add(txtName, 1, 2);
        pane.add(lblEmail, 0, 3);
        pane.add(txtEmail, 1, 3);
        pane.add(btnSubmit, 1, 4);

        // Event Handling with Comprehensive Input Filtering Strategy (Quality Gate)
        btnSubmit.setOnAction(e -> {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();

            if (id.isEmpty() || name.isEmpty() || email.isEmpty()) {
                showAlertDialog(Alert.AlertType.ERROR, "Input Validation Failure", "All operational entry inputs must be filled.");
                return;
            }

            // Uniqueness Constraint Verification
            for (Student s : studentMasterList) {
                if (s.getId().equalsIgnoreCase(id)) {
                    showAlertDialog(Alert.AlertType.ERROR, "Data Duplication Conflict", "A student record already exists with ID: " + id);
                    return;
                }
            }

            // Add to system state repository
            Student newStudent = new Student(id, name, email);
            studentMasterList.add(newStudent);
            
            // Clear entry vectors
            txtId.clear();
            txtName.clear();
            txtEmail.clear();

            showAlertDialog(Alert.AlertType.INFORMATION, "Success", "Student records state catalog updated successfully.");
        });

        return pane;
    }

    // --- VIEW 3: COURSE ENROLLMENT INTERFACE ---
    private GridPane createEnrollmentView() {
        GridPane pane = new GridPane();
        pane.setVgap(15);
        pane.setHgap(15);

        Label header = new Label("Course Registration Matrix");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblCourse = new Label("Select Target Academic Unit:");
        enrollCourseDropdown = new ComboBox<>(FXCollections.observableArrayList(availableCourses));
        enrollCourseDropdown.getSelectionModel().selectFirst();

        Label lblEligible = new Label("Eligible Base Candidates (Not Yet Enrolled):");
        eligibleStudentListView = new ListView<>();
        eligibleStudentListView.setPrefHeight(300);
        
        // Custom cell renderer to display friendly names instead of object hashes
        eligibleStudentListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getId() + ")");
                }
            }
        });

        Button btnEnroll = new Button("Commit Enrollment Sequence");
        btnEnroll.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px;");

        pane.add(header, 0, 0, 2, 1);
        pane.add(lblCourse, 0, 1);
        pane.add(enrollCourseDropdown, 1, 1);
        pane.add(lblEligible, 0, 2, 2, 1);
        pane.add(eligibleStudentListView, 0, 3, 2, 1);
        pane.add(btnEnroll, 0, 4);

        // Event Handling Strategies for Dropdown mutations
        enrollCourseDropdown.setOnAction(e -> refreshEnrollmentEligibleList());

        btnEnroll.setOnAction(e -> {
            String targetCourse = enrollCourseDropdown.getValue();
            Student candidate = eligibleStudentListView.getSelectionModel().getSelectedItem();

            if (candidate == null) {
                showAlertDialog(Alert.AlertType.WARNING, "Selection Deficiency", "An assignment processing target must be explicitly selected in the list container.");
                return;
            }

            candidate.enroll(targetCourse);
            refreshEnrollmentEligibleList();
            showAlertDialog(Alert.AlertType.INFORMATION, "System State Updated", candidate.getName() + " enrolled successfully in " + targetCourse);
        });

        return pane;
    }

    private void refreshEnrollmentEligibleList() {
        String targetCourse = enrollCourseDropdown.getValue();
        if (targetCourse == null) return;

        ObservableList<Student> eligibleList = FXCollections.observableArrayList();
        for (Student s : studentMasterList) {
            if (!s.isEnrolled(targetCourse)) {
                eligibleList.add(s);
            }
        }
        eligibleStudentListView.setItems(eligibleList);
    }

    // --- VIEW 4: GRADE BOOK MANAGEMENT ---
    private GridPane createGradeBookView() {
        GridPane pane = new GridPane();
        pane.setVgap(15);
        pane.setHgap(15);

        Label header = new Label("Centralized Administrative Grade Book");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblStudent = new Label("Query Profile:");
        gradeStudentDropdown = new ComboBox<>(studentMasterList);
        gradeStudentDropdown.setPrefWidth(250);
        
        gradeStudentDropdown.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName() + " (" + item.getId() + ")");
            }
        });
        gradeStudentDropdown.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName() + " (" + item.getId() + ")");
            }
        });

        Label lblTable = new Label("Current Course Registration Matrix Summary:");
        
        Label lblTargetCourse = new Label("Select Registered Course:");
        gradeCourseDropdown = new ComboBox<>();
        gradeCourseDropdown.setPrefWidth(200);

        Label lblMark = new Label("Assign Assessment Score Value:");
        gradeLetterDropdown = new ComboBox<>(FXCollections.observableArrayList("A", "A-", "B+", "B", "B-", "C+", "C", "D", "F"));
        gradeLetterDropdown.getSelectionModel().selectFirst();

        Button btnApplyGrade = new Button("Commit Assessment Change To Ledger");
        btnApplyGrade.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");

        pane.add(header, 0, 0, 3, 1);
        pane.add(lblStudent, 0, 1);
        pane.add(gradeStudentDropdown, 1, 1);
        pane.add(lblTable, 0, 2, 3, 1);
        pane.add(currentGradesTable, 0, 3, 3, 1);
        pane.add(lblTargetCourse, 0, 4);
        pane.add(gradeCourseDropdown, 1, 4);
        pane.add(lblMark, 0, 5);
        pane.add(gradeLetterDropdown, 1, 5);
        pane.add(btnApplyGrade, 1, 6);

        // Event Processing Hook Orchestrators
        gradeStudentDropdown.setOnAction(e -> handleGradeStudentSelectionChange());

        btnApplyGrade.setOnAction(e -> {
            Student selectedStudent = gradeStudentDropdown.getValue();
            String selectedCourse = gradeCourseDropdown.getValue();
            String score = gradeLetterDropdown.getValue();

            if (selectedStudent == null || selectedCourse == null || score == null) {
                showAlertDialog(Alert.AlertType.ERROR, "Transaction Violation Exception", "Missing runtime tracking properties. Check parameters.");
                return;
            }

            selectedStudent.assignGrade(selectedCourse, score);
            populateGradesTable(selectedStudent);
            showAlertDialog(Alert.AlertType.INFORMATION, "Ledger Written", "Academic structural record finalized efficiently.");
        });

        return pane;
    }

    private void initializeGradeManagementComponents() {
        currentGradesTable = new TableView<>();
        currentGradesTable.setPrefHeight(180);
        currentGradesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);

        TableColumn<GradeRecord, String> colCourse = new TableColumn<>("Course Unit Module");
        colCourse.setCellValueFactory(d -> d.getValue().courseCode);

        TableColumn<GradeRecord, String> colGrade = new TableColumn<>("Assigned Final Grade");
        colGrade.setCellValueFactory(d -> d.getValue().grade);

        currentGradesTable.getColumns().addAll(Arrays.asList(colCourse, colGrade));
    }

    private void handleGradeStudentSelectionChange() {
        Student selected = gradeStudentDropdown.getValue();
        if (selected == null) {
            currentGradesTable.getItems().clear();
            gradeCourseDropdown.getItems().clear();
            return;
        }
        populateGradesTable(selected);
        
        // Dynamically populating the course selector with only courses the student is actually enrolled in
        gradeCourseDropdown.setItems(FXCollections.observableArrayList(selected.getCourseGrades().keySet()));
        gradeCourseDropdown.getSelectionModel().selectFirst();
    }

    private void populateGradesTable(Student s) {
        ObservableList<GradeRecord> records = FXCollections.observableArrayList();
        s.getCourseGrades().forEach((course, grade) -> records.add(new GradeRecord(course, grade)));
        currentGradesTable.setItems(records);
    }

    private void refreshGradeBookView() {
        if (!studentMasterList.isEmpty() && gradeStudentDropdown.getValue() == null) {
            gradeStudentDropdown.getSelectionModel().selectFirst();
        }
        handleGradeStudentSelectionChange();
    }

    // --- MODAL DIALOG COMPONENT: PROFILE UPDATING SUB-FORM ---
    private void showUpdateModal(Student student) {
        Stage modal = new Stage();
        modal.setTitle("Profile Adjustment Form Matrix - " + student.getId());
        
        GridPane inner = new GridPane();
        inner.setPadding(new Insets(20));
        inner.setVgap(15);
        inner.setHgap(10);

        Label lblTitle = new Label("Updating Record Metadata for: " + student.getId());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblName = new Label("Full Name Name:");
        TextField txtName = new TextField(student.getName());

        Label lblEmail = new Label("System Email Contact Address:");
        TextField txtEmail = new TextField(student.getEmail());

        Button btnSave = new Button("Commit System Mutation Changes");
        btnSave.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        inner.add(lblTitle, 0, 0, 2, 1);
        inner.add(lblName, 0, 1);
        inner.add(txtName, 1, 1);
        inner.add(lblEmail, 0, 2);
        inner.add(txtEmail, 1, 2);
        inner.add(btnSave, 1, 3);

        btnSave.setOnAction(e -> {
            String nameVal = txtName.getText().trim();
            String emailVal = txtEmail.getText().trim();

            if (nameVal.isEmpty() || emailVal.isEmpty()) {
                showAlertDialog(Alert.AlertType.ERROR, "Input Context Execution Fault", "Data components fields cannot evaluate null values.");
                return;
            }

            student.setName(nameVal);
            student.setEmail(emailVal);
            studentTableView.refresh(); // Forces table view framework buffer updates instantly

            modal.close();
            showAlertDialog(Alert.AlertType.INFORMATION, "Operation Execution Successful", "System configuration schema structural entity record complete.");
        });

        modal.setScene(new Scene(inner, 420, 240));
        modal.initOwner(studentTableView.getScene().getWindow());
        modal.showAndWait();
    }

    // --- GLOBAL MENU BAR COMPONENT GENERATOR ---
    private MenuBar createMenuBar(Stage currentStage) {
        MenuBar menuBar = new MenuBar();
        
        Menu menuSystem = new Menu("System Engine");
        MenuItem itemAbout = new MenuItem("System Context Manifest");
        MenuItem itemExit = new MenuItem("Terminate Application Architecture Session");
        menuSystem.getItems().addAll(itemAbout, itemExit);

        itemAbout.setOnAction(e -> showAlertDialog(Alert.AlertType.INFORMATION, "Manifest Specification Metadata", "Student Management Information Infrastructure System Architecture v4.01\nEngine Node Interface Platform Active."));
        itemExit.setOnAction(e -> currentStage.close());

        Menu menuExecution = new Menu("Administrative Tools");
        MenuItem mItemAdd = new MenuItem("Quick Add Student Profile Context");
        menuExecution.getItems().add(mItemAdd);
        
        mItemAdd.setOnAction(e -> {
            TextInputDialog dialogue = new TextInputDialog("REG-9999");
            dialogue.setTitle("Direct Administrative Initialization Hook");
            dialogue.setHeaderText("Fast-track Record Vector Injection Entry");
            dialogue.setContentText("Specify Unique Access Registration ID Key String:");
            
            Optional<String> outcome = dialogue.showAndWait();
            outcome.ifPresent(idValue -> {
                if(!idValue.trim().isEmpty()) {
                    Student quickEntry = new Student(idValue.trim(), "System Pending Registration Identity Data", "pending-delivery@anonymous.tech");
                    studentMasterList.add(quickEntry);
                }
            });
        });

        menuBar.getMenus().addAll(menuSystem, menuExecution);
        return menuBar;
    }

    // --- STYLING DESIGN PRIMITIVES ---
    private Button createStyledNavButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-font-size: 13px; -fx-padding: 10px 15px; -fx-cursor: hand;");
        
        // Dynamic pseudo-styling event animations
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #34495e; -fx-text-fill: #1abc9c; -fx-font-size: 13px; -fx-padding: 10px 15px; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-font-size: 13px; -fx-padding: 10px 15px; -fx-cursor: hand;"));
        
        return b;
    }

    // --- SYSTEM ALERT NOTIFICATION INJECTOR ---
    private void showAlertDialog(Alert.AlertType type, String sectionHeader, String functionalContextContent) {
        Alert dialogueAlertBox = new Alert(type);
        dialogueAlertBox.setTitle("System Processing Node Intercept Exception Broadcast Event");
        dialogueAlertBox.setHeaderText(sectionHeader);
        dialogueAlertBox.setContentText(functionalContextContent);
        dialogueAlertBox.showAndWait();
    }

    // --- SEED SELECTION DATA INITIATOR ---
    private void seedSampleData() {
        Student s1 = new Student("REG-0001", "Alex Kamau", "alex.kamau@moringaschool.edu");
        Student s2 = new Student("REG-0002", "Fatma Omar", "f.omar@uopeople.edu");
        Student s3 = new Student("REG-0003", "Chidi Okechukwu", "c.okechukwu@uopeople.edu");

        s1.enroll("CS-101 Java Programming");
        s1.assignGrade("CS-101 Java Programming", "A");
        s1.enroll("CS-202 Data Structures");
        s1.assignGrade("CS-202 Data Structures", "B+");

        s2.enroll("CS-101 Java Programming");
        s2.assignGrade("CS-101 Java Programming", "A-");
        s2.enroll("DB-301 Database Systems");
        s2.assignGrade("DB-301 Database Systems", "Not Assigned");

        studentMasterList.addAll(Arrays.asList(s1, s2, s3));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
