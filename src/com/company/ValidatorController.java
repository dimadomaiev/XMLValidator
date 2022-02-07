package com.company;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.awt.*;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ValidatorController {
    public static String pathForSchema;
    public static String consoleToArea;
    public static String controllerOfConsoleToArea;
    public static PrintWriter writer;
    public int counterInvalidFiles = 0;
    public int counterValidFiles = 0;
    public AnchorPane gettingDate;
    //----------------------------------------------------MenuItem------------------------------------------------------
    @FXML
    private MenuItem invalidFilesWindow;

    @FXML
    private MenuItem linkToWiki;

    @FXML
    private MenuItem clearConsole;

    @FXML
    private MenuItem moreOptions;

    @FXML
    private MenuItem openConfigFile;

    @FXML
    private MenuItem openValidationLogFile;
    //----------------------------------------------------localTab------------------------------------------------------
    @FXML
    private Tab localTab;

    @FXML
    private Button selectXMLFile;

    @FXML
    private TextField xmlFilePath;
    //----------------------------------------------------ftpTab--------------------------------------------------------
    @FXML
    private Tab ftpTab;

    @FXML
    private ChoiceBox<String> environment;

    @FXML
    private Text ftpBaseFolderText;

    @FXML
    private ChoiceBox<String> ftpBaseFolder;

    @FXML
    private TextField ftpManualDir;

    @FXML
    private TextField ftpLogin;

    @FXML
    private PasswordField ftpPassword;

    @FXML
    private TextField ftpOtherURL;

    @FXML
    private TextField otherFTPManualDir;
    //----------------------------------------------------AnchorPane-with-TextArea--------------------------------------
    @FXML
    private Button selectSchemaFile;

    @FXML
    private TextField schemaFilePath;

    @FXML
    private Separator separator1;

    @FXML
    private CheckBox onlyCurrMonthFolder;

    @FXML
    private Separator separator2;

    @FXML
    private Text youngerThanDate;

    @FXML
    private TextField dateGetFrom;

    @FXML
    private Button startValidation;

    @FXML
    private ListView<String> listView;

    @FXML
    private ProgressIndicator indicator;

    //----------------------------------------------------initialize----------------------------------------------------
    @FXML
    private void initialize() {
        SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));
        deleteInvalidFilesAlert();
        SimpleXMLValidator.createConfigFile();
        initializeSelectedEnvironment();
        try {
            if (SimpleXMLValidator.envs.isEmpty()) {
                SimpleXMLValidator.initialEnvironments();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(consoleToArea = "Error initial config file.\n" + e);
            writeToLogFile();
        }
        //Initialize ProgressIndicator
        indicator.setVisible(false);
        //Initialize the window
        Stage window = new Stage();
        initializeSpecifyOptions();
        //Set action ot button selectSchemaFile
        selectSchemaFile.setOnAction(actionEvent -> {
            schemaFilePath.clear();
            //Calling the file selection method
            SimpleXMLValidator.stageSchema(window);
            //Set the path to the selected file in the prompt of text field
            this.setPromptSchemaFilePath(SimpleXMLValidator.schemaFile);
        });
        //Set action ot button selectXMLFile
        selectXMLFile.setOnAction(actionEvent -> {
            xmlFilePath.clear();
            SimpleXMLValidator.stageFile(window);
            //Set the path to the selected file in the prompt of text field
            this.setPromptXMLFilePath(SimpleXMLValidator.xmlFile);
        });
        //Get prompt text for copy if mouse inside text field.
        xmlFilePath.setOnMouseClicked(e -> xmlFilePath.setText(xmlFilePath.getPromptText()));
        schemaFilePath.setOnMouseClicked(e -> schemaFilePath.setText(schemaFilePath.getPromptText()));

        //----------------------------------------------------Start Validation button-----------------------------------
        startValidation.setOnAction(actionEvent -> {
            counterInvalidFiles = 0;
            counterValidFiles = 0;
            initializeSelectedEnvironment();
            initializeSpecifyOptions();
            new Thread(this::run).start();
        });
        //------------------------------------------------Initial-open-folder-------------------------------------------
        invalidFilesWindow.setOnAction(actionEvent -> {
                    try {
                        Desktop.getDesktop().open(new File(SimpleXMLValidator.invalidFiles));
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(consoleToArea = "Error opening invalidFiles folder ... \n Directory 'invalidFiles' can be not exist." + e);
                        writeToLogFile();
                    }
                }
        );
        //------------------------------------------------Initial-open-configFile---------------------------------------
        openConfigFile.setOnAction(actionEvent -> {
                    try {
                        Desktop.getDesktop().open(SimpleXMLValidator.configFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(consoleToArea = "Error opening configFile ... \n" + e);
                        writeToLogFile();
                    }
                }
        );
        //------------------------------------------------Initial-open-logFile------------------------------------------
        openValidationLogFile.setOnAction(actionEvent -> {
                    try {
                        Desktop.getDesktop().open(SimpleXMLValidator.logFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(consoleToArea = "Error opening logFile ... \n" + e);
                        writeToLogFile();
                    }
                }
        );
        //------------------------------------------------Initial-clearConsole-button-in-help-menu----------------------
        clearConsole.setOnAction(actionEvent -> {
            listView.getItems().clear();
            SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));
        });
        //------------------------------------------------Initial-linkToWiki-button-in-help-menu------------------------
        linkToWiki.setOnAction(actionEvent -> {
            System.out.println(consoleToArea = "Sorry for inconvenience." + "\n" + "Wiki page does not exist at this moment.");
            writeToLogFile();
        });
        //Show/hide objects by event on moreOptions in the Edit menu
        moreOptions.setOnAction(event -> {
            onlyCurrMonthFolder.setVisible(!onlyCurrMonthFolder.isVisible() && ftpTab.isSelected());
            youngerThanDate.setVisible(!youngerThanDate.isVisible() && ftpTab.isSelected());
            dateGetFrom.setVisible(!dateGetFrom.isVisible() && ftpTab.isSelected());
            separator1.setVisible(!separator1.isVisible() && ftpTab.isSelected());
            separator2.setVisible(!separator2.isVisible() && ftpTab.isSelected());
        });

    }

    //Set the path to the file in the field after selecting the file
    void setPromptXMLFilePath(File file) {
        xmlFilePath.setPromptText(String.valueOf(file));
    }

    //Set the path to the file in the field after selecting the scheme
    void setPromptSchemaFilePath(File file) {
        schemaFilePath.setPromptText(String.valueOf(file));
    }

    //Removing and adding elements to work with the "Other" environment
    void initializeSelectedEnvironment() {
        environment.setItems(SimpleXMLValidator.environmentList);
        environment.setOnAction(actionEvent -> {
            ftpOtherURL.setVisible(environment.getValue().equals("Other"));
            ftpLogin.setVisible(environment.getValue().equals("Other"));
            ftpPassword.setVisible(environment.getValue().equals("Other"));
            otherFTPManualDir.setVisible(environment.getValue().equals("Other"));
            //----------------------------------------------------------------------------------------------------------
            ftpManualDir.setVisible(!environment.getValue().equals("Other"));
            ftpBaseFolder.setVisible(!environment.getValue().equals("Other"));
            ftpBaseFolderText.setVisible(!environment.getValue().equals("Other"));
            SimpleXMLValidator.selectedEnvironment = environment.getValue();
        });
    }

    //BaseFolder Dropdown List
    void initializeSpecifyOptions() {
        ftpBaseFolder.setItems(SimpleXMLValidator.ftpBaseFolderList);
        ftpBaseFolder.setOnAction(actionEvent -> {
            SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue();
            String bufferedBaseFolderValue = "";
            if (ftpBaseFolder.getValue().equals(bufferedBaseFolderValue)) {
                ftpManualDir.clear();
            }
        });
        SimpleXMLValidator.uploadDateFrom = dateGetFrom.getText();
        if (!dateGetFrom.isVisible()) {
            dateGetFrom.clear();
        }
    }

    //Validation method
    void validate(File schemaPath, File filePath) throws IOException {

        File mySchemaFile = new File(String.valueOf(schemaPath));
        Source myXMLFile = new StreamSource(new File(String.valueOf(filePath)));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(mySchemaFile);
            Validator validator = schema.newValidator();
            validator.validate(myXMLFile);
            System.out.println(consoleToArea = (filePath.getName() + " - Size is : " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS VALID \n"));
            counterValidFiles++;
            writeToLogFile();
        } catch (SAXException | IOException e) {
            if (schemaPath == null || filePath == null) {
                System.out.println(consoleToArea = "Please provide the path to the schema and/or XML file for validation!!!" + "\n");
            } else {
                System.out.println(consoleToArea = (filePath.getName() + " - Size is : " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS NOT VALID." + "\n" + "Reason : " + e + "\n"));
                counterInvalidFiles++;
                try {
                    SimpleXMLValidator.copyFileUsingStream(filePath, new File(SimpleXMLValidator.invalidFiles + filePath.getName()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("Error while copping invalid file... \n" + ex);
                }
            }
            writeToLogFile();
        }
    }

    //Notification when the program starts. Appears if there is a directory with invalid files
    void deleteInvalidFilesAlert() {
        SimpleXMLValidator.logFile.delete();
        File inv = new File(String.valueOf(SimpleXMLValidator.invalidFiles));
        if (inv.exists()) {
            //Delete old invalid files if Yes button pressed
            int yesOrNo = JOptionPane.showConfirmDialog(
                    null,
                    "The invalidFiles folder is not empty. \n Would you like delete this files?",
                    "Delete OLD invalid files?",
                    JOptionPane.YES_NO_OPTION);
            // yes -> 0
            if (yesOrNo == 0) {
                SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.invalidFiles));
            }
        }
    }

    //Logging actions in to the console and log file
    void writeToLogFile() {
        try {
            writer = new PrintWriter((new FileWriter(SimpleXMLValidator.logFile, true)));
            writer.println(consoleToArea);
            writer.close();
            listView.getItems().add(consoleToArea);
            //textFlow.getChildren().add(new Text(consoleToArea));
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    //Tread running
    public void run() {
        long startTime = System.nanoTime();
        SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));
        //Get schema path from text field
        if (!(schemaFilePath.getText().equals(""))) {
            pathForSchema = schemaFilePath.getText();
            SimpleXMLValidator.schemaFile = new File(schemaFilePath.getText());
        }
        //Get file path from text field
        if (!(xmlFilePath.getText().equals(""))) {
            SimpleXMLValidator.xmlFile = new File(xmlFilePath.getText());
            controllerOfConsoleToArea = consoleToArea;
            SimpleXMLValidator.selectFilesFromDir(String.valueOf(SimpleXMLValidator.xmlFile));
            if (!controllerOfConsoleToArea.equals(consoleToArea)) {
                System.out.println("Getting file path from text field - controllerOfConsoleToArea - " + consoleToArea);
                writeToLogFile();
            }
        }

        if (SimpleXMLValidator.pathForFiles == null & localTab.isSelected() & SimpleXMLValidator.xmlFile == null) {
            System.out.println(consoleToArea = "You should specify file(s) for validate.\n");
            writeToLogFile();
            this.setPromptXMLFilePath(new File(consoleToArea.toUpperCase(Locale.ROOT)));
            return;
        }

        if (SimpleXMLValidator.schemaFile == null) {
            System.out.println(consoleToArea = "Please select schema to validate file(s) !\n");
            writeToLogFile();
            this.setPromptSchemaFilePath(new File(consoleToArea.toUpperCase(Locale.ROOT)));
            return;
        }
        if (!SimpleXMLValidator.schemaFile.exists()) {
            System.out.println(consoleToArea = "Please make sure the schema file is selected! ...");
            writeToLogFile();
            return;
        }
        //----------------------------------------------------Local-Tab-------------------------------------------------
        if (localTab.isSelected()) {
            writeToLogFile();
            long startLocalCopyingTime = System.nanoTime();
            indicator.setVisible(true);
            if (SimpleXMLValidator.pathForFiles == null & ((SimpleXMLValidator.xmlFile.getName().endsWith(".xml") | SimpleXMLValidator.xmlFile.getName().endsWith(".zip")))) {
                try {
                    SimpleXMLValidator.copyFileUsingStream(SimpleXMLValidator.xmlFile, new File(SimpleXMLValidator.tempFiles + SimpleXMLValidator.xmlFile.getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                    indicator.setVisible(false);
                }
            } else {
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    try {
                        SimpleXMLValidator.copyFileUsingStream(pathForFile, new File(SimpleXMLValidator.tempFiles + pathForFile.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        indicator.setVisible(false);
                    }
                }
            }
            System.out.println(consoleToArea = "File(s) copied to the temp folder ...\n");
            controllerOfConsoleToArea = consoleToArea;
            SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
            if (!controllerOfConsoleToArea.equals(consoleToArea)) {
                System.out.println("Local-Tab - controllerOfConsoleToArea" + consoleToArea);
                writeToLogFile();
            }
            indicator.setVisible(false);
            logTime(startLocalCopyingTime);
            //----------------------------------------------------Unzipping---------------------------------------------
            System.out.println(consoleToArea = "Unzipping file(s) from temp folder...\n");
            writeToLogFile();
            long startLocalUnzippingTime = System.nanoTime();
            indicator.setVisible(true);
            for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                if (pathForFile.getName().endsWith(".zip")) {
                    try {
                        SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);
                    } catch (IOException e) {
                        e.printStackTrace();
                        indicator.setVisible(false);
                    }
                }
            }
            indicator.setVisible(false);
            controllerOfConsoleToArea = consoleToArea;
            SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
            if (!controllerOfConsoleToArea.equals(consoleToArea)) {
                System.out.println("Unzipping - controllerOfConsoleToArea - " + consoleToArea);
                writeToLogFile();
                return;
            }
            logTime(startLocalUnzippingTime);
            //----------------------------------------------------Validating--------------------------------------------
            System.out.println(consoleToArea = "Validating ...\n");
            writeToLogFile();
            long startLocalValidatingTime = System.nanoTime();
            this.setPromptXMLFilePath(new File(String.valueOf(SimpleXMLValidator.tempFiles)));
            for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                if (pathForFile.getName().endsWith(".xml")) {
                    try {
                        validate(SimpleXMLValidator.schemaFile, pathForFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            controllerOfConsoleToArea = consoleToArea;
            SimpleXMLValidator.selectFilesFromDir(String.valueOf(SimpleXMLValidator.xmlFile));
            if (!controllerOfConsoleToArea.equals(consoleToArea)) {
                System.out.println("Validating - controllerOfConsoleToArea - " + consoleToArea);
                writeToLogFile();
            }
            logTime(startLocalValidatingTime);
        }
        //------------------------------------------------FTP-Tab-------------------------------------------------------
        if (ftpTab.isSelected()) {
            //------------------------------------------------Not-Other-------------------------------------------------
            if (!SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                SimpleXMLValidator.selectedEnvironment = environment.getValue();
                SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue();
                SimpleXMLValidator.manualDir = ftpManualDir.getText();
                SimpleXMLValidator.uploadDateFrom = dateGetFrom.getText();
                if (!dateGetFrom.isVisible()) {
                    dateGetFrom.clear();
                }
                try {
                    SimpleXMLValidator.setDefaultLoginData();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Problem with colling the setDefaultLoinData method ... " + e);
                }
            }
            //------------------------------------------------Get-schema-path-from-text-area----------------------------
            if (!(schemaFilePath.getText().equals(""))) {
                pathForSchema = schemaFilePath.getText();
                SimpleXMLValidator.schemaFile = new File(schemaFilePath.getText());
            }
            //------------------------------------------------Preparation-other-FTP-------------------------------------

            if (SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                SimpleXMLValidator.otherFTPManualDir = otherFTPManualDir.getText();
                SimpleXMLValidator.ftpOther = ftpOtherURL.getText();
                SimpleXMLValidator.username = ftpLogin.getText();
                SimpleXMLValidator.password = ftpPassword.getText();
                SimpleXMLValidator.uploadDateFrom = dateGetFrom.getText();
                if (!dateGetFrom.isVisible()) {
                    dateGetFrom.clear();
                }
                try {
                    SimpleXMLValidator.setDefaultLoginData();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Problem with colling the setDefaultLoinData method ... " + e);
                }
            }
            //------------------------------------------------Initial-Environments--------------------------------------
            try {
                SimpleXMLValidator.initialEnvironments();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(consoleToArea = "Problem with initial environment ! ... \n" + e);
                writeToLogFile();
            }

            //------------------------------------------------Check-necessary-fields------------------------------------
            if ((SimpleXMLValidator.selectedEnvironment.isEmpty() | SimpleXMLValidator.ftpBaseFolder.equals("")) & !SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                System.out.println(consoleToArea = "Please specify environment and base folder! ...");
                writeToLogFile();
                return;
            }

            if (SimpleXMLValidator.ftpOther.equals("") & SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                System.out.println(consoleToArea = "Please specify other environment! ...");
                writeToLogFile();
                return;
            }
            //------------------------------------------------ftpClient-------------------------------------------------
            System.out.println(consoleToArea = ("\n" + "Connect ... and download FTP files ..." + "\n" + "It could take a lot of time, if end folder contains folders with files ..."));
            writeToLogFile();
            long startLoadingTime = System.nanoTime();
            indicator.setVisible(true);
            SimpleXMLValidator.uploadDateFrom = dateGetFrom.getText();
            try {
                SimpleXMLValidator.ftpClient();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Problem with connection to FTP. Error - " + e);
                System.out.println(consoleToArea = "Please specify environment ! ... Or check VPN connection... \n Error: " + e);
                writeToLogFile();
                indicator.setVisible(false);
                return;
            }
            controllerOfConsoleToArea = consoleToArea;
            SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
            if (!controllerOfConsoleToArea.equals(consoleToArea)) {
                writeToLogFile();
            }
            logTime(startLoadingTime);
            indicator.setVisible(false);
            //------------------------------------------------Check-if-temp-folder-is-empty-----------------------------
            if (SimpleXMLValidator.pathForFiles == null) {
                System.out.println(consoleToArea = "--------------------------------------------------- FTP End folder is empty!!! ... Or the path to the FTP files is incorrect ... ---------------------------------------------------");
                writeToLogFile();
                return;
            }
            //------------------------------------------------Unzipping-------------------------------------------------
            System.out.println(consoleToArea = ("\n" + "Unzipping loaded files ..." + "\n"));
            writeToLogFile();
            long startUnzippingTime = System.nanoTime();
            indicator.setVisible(true);
            for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                if (pathForFile.getName().endsWith(".zip")) {
                    try {
                        SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(consoleToArea = "Unpacking error ... " + e);
                        writeToLogFile();
                        indicator.setVisible(false);
                        return;
                    }
                }
            }
            indicator.setVisible(false);
            logTime(startUnzippingTime);
            controllerOfConsoleToArea = consoleToArea;
            SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
            if (!controllerOfConsoleToArea.equals(consoleToArea)) {
                System.out.println("Unzipping - controllerOfConsoleToArea - " + consoleToArea);
                writeToLogFile();
            }
            //------------------------------------------------Validating------------------------------------------------
            System.out.println(consoleToArea = ("\n" + "Validating ..." + "\n"));
            writeToLogFile();
            long startValidatingTime = System.nanoTime();
            this.setPromptXMLFilePath(new File(SimpleXMLValidator.tempFiles));
            for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                if (pathForFile.getName().endsWith(".xml")) {
                    try {
                        validate(SimpleXMLValidator.schemaFile, pathForFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(consoleToArea = "Validation method error ... \n" + e);
                        writeToLogFile();
                        return;
                    }
                }
            }
            logTime(startValidatingTime);
        }
        System.out.println(consoleToArea = ("\n" + "----------------------------------------------------------------------------- !!! Verification completed !!! ------------------------------------------------------------------------"));
        writeToLogFile();
        System.out.print(consoleToArea = "Valid files - " + counterValidFiles + "\nInvalid files - " + counterInvalidFiles + "\n");
        writeToLogFile();
        System.out.println(consoleToArea = ("\n" + "----------------------------------------------------------- !!! Invalid files saved to: \"C:\\XMLValidator\\invalidFiles\" !!! -----------------------------------------------------------" + "\n"));
        writeToLogFile();
        System.out.print(consoleToArea = "Total time: ");
        writeToLogFile();
        logTime(startTime);
    }

    //Вывод времени
    void logTime(Long startTime) {
        long endTime = System.nanoTime();
        long totalTimeInMilliseconds = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        System.out.println(consoleToArea = "Time: \n" + "sec.: " + totalTimeInMilliseconds / 1000 + "\nms.: " + totalTimeInMilliseconds);
        writeToLogFile();
    }
}