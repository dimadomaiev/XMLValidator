package com.company;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
    public static PrintWriter writer;

    @FXML
    private ProgressIndicator indicator;

    @FXML
    private Tab localTab;

    @FXML
    private Tab ftpTab;

    @FXML
    private TextField ftpLogin;

    @FXML
    private PasswordField ftpPassword;

    @FXML
    private ChoiceBox<String> environment;

    @FXML
    private TextField otherFTPManualDir;

    @FXML
    private ChoiceBox<String> ftpBaseFolder;
    ObservableList<String> ftpBaseFolderList = FXCollections.observableArrayList("fcs_nsi", "fcs_fas", "fcs_rules", "fcs_regions", "Other");

    @FXML
    private Text ftpBaseFolderText;

    @FXML
    private TextField ftpOtherURL;

    @FXML
    private TextField ftpManualDir;

    @FXML
    private TextField schemaFilePath;

    void setPromptSchemaFilePath(File file) {
        schemaFilePath.setPromptText(String.valueOf(file));                                                             //Устанавливаем путь к файлу в поле после выбора схемы
    }

    @FXML
    private TextField xmlFilePath;

    void setPromptXMLFilePath(File file) {
        xmlFilePath.setPromptText(String.valueOf(file));                                                                //Устанавливаем путь к файлу в поле после выбора файла
    }

    @FXML
    private Button startValidation;

    @FXML
    private Button selectSchemaFile;

    @FXML
    private Button selectXMLFile;

    @FXML
    private TextArea console;

    void consoleArea() {
        console.appendText("\n" + consoleToArea);                                                                       //Добавляем текст в TextArea с сохранением
        console.setWrapText(true);                                                                                      //Выравнивать текст в область текстого поля
        writeToLogFile();
    }

    @FXML
    private MenuItem invalidFilesWindow;

    @FXML
    private MenuItem linkToWiki;

    @FXML
    private MenuItem clearConsole;


    @FXML
    private void initialize() {
        SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));
        deleteInvalidFilesAlert();
        SimpleXMLValidator.createConfigFile();
        try {
            if (SimpleXMLValidator.envs.isEmpty()) {
                SimpleXMLValidator.initialEnvironments();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(consoleToArea = "Error initial config file.\n" + e);
            this.consoleArea();
        }
        // Инициализируем ProgressIndicator
        indicator.setVisible(false);
        // Инициализируем окно
        Stage window = new Stage();
        environment.setItems(SimpleXMLValidator.environmentList);
        environment.setOnAction(actionEvent -> {
            ftpOtherURL.setVisible(environment.getValue().equals("Other"));
            ftpLogin.setVisible(environment.getValue().equals("Other"));
            ftpPassword.setVisible(environment.getValue().equals("Other"));
            otherFTPManualDir.setVisible(environment.getValue().equals("Other"));
//----------------------------------------------------------------------------------------------------------------------
            ftpManualDir.setVisible(!environment.getValue().equals("Other"));
            ftpBaseFolder.setVisible(!environment.getValue().equals("Other"));
            ftpBaseFolderText.setVisible(!environment.getValue().equals("Other"));
            SimpleXMLValidator.selectedEnvironment = environment.getValue();
        });

        ftpBaseFolder.setItems(ftpBaseFolderList);
        ftpBaseFolder.setOnAction(actionEvent -> SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue());
        //Задаем действие на кнопку selectSchemaFile
        selectSchemaFile.setOnAction(actionEvent -> {
            schemaFilePath.clear();
            //Вызываем метод выбора файла
            SimpleXMLValidator.stageSchema(window);
            //Задаем в промте поля путь к выбранному файлу
            this.setPromptSchemaFilePath(SimpleXMLValidator.schemaFile);
        });

        //По аналогии со схемами
        selectXMLFile.setOnAction(actionEvent -> {
            xmlFilePath.clear();
            SimpleXMLValidator.stageFile(window);
            this.setPromptXMLFilePath(SimpleXMLValidator.xmlFile);
        });
//----------------------------------------------------Start Validation button-------------------------------------------
        startValidation.setOnAction(actionEvent -> new Thread(() -> {
            long startTime = System.nanoTime();
            SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));
            //Get schema path from text field
            if (!(schemaFilePath.getText().equals(""))) {
                pathForSchema = schemaFilePath.getText();
                SimpleXMLValidator.schemaFile = new File(schemaFilePath.getText());
            }
            //Get file path from text field
            if (!(xmlFilePath.getText().equals(""))) {
                SimpleXMLValidator.pathToSelectedFiles = new File(xmlFilePath.getText());
                SimpleXMLValidator.xmlFile = new File(xmlFilePath.getText());
                SimpleXMLValidator.selectFilesFromDir(String.valueOf(SimpleXMLValidator.pathToSelectedFiles));
            }

            if (SimpleXMLValidator.pathForFiles == null & localTab.isSelected() & SimpleXMLValidator.xmlFile == null) {
                System.out.println(consoleToArea = "You should specify file(s) for validate.\n");
                this.consoleArea();
                this.setPromptXMLFilePath(new File(consoleToArea.toUpperCase(Locale.ROOT)));
                return;
            }

            if (SimpleXMLValidator.schemaFile == null) {
                System.out.println(consoleToArea = "Please select schema to validate file(s) !\n");
                this.consoleArea();
                this.setPromptSchemaFilePath(new File(consoleToArea.toUpperCase(Locale.ROOT)));
                return;
            }
            if (!SimpleXMLValidator.schemaFile.exists()) {
                System.out.println(consoleToArea = "Please make sure the schema file is selected! ...");
                this.consoleArea();
                return;
            }
//----------------------------------------------------Local-Tab---------------------------------------------------------
            if (localTab.isSelected()) {
                System.out.println(consoleToArea = "Copying file(s) to temp folder ...\n");
                this.consoleArea();
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
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
                indicator.setVisible(false);
                logTime(startLocalCopyingTime);
//----------------------------------------------------Unzipping---------------------------------------------------------
                System.out.println(consoleToArea = "Unzipping file(s) from temp folder...\n");
                this.consoleArea();
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
                    indicator.setVisible(false);
                }
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
                logTime(startLocalUnzippingTime);
//----------------------------------------------------Validating--------------------------------------------------------
                System.out.println(consoleToArea = "Validating ...\n");
                this.consoleArea();
                long startLocalValidatingTime = System.nanoTime();
                this.setPromptXMLFilePath(new File(String.valueOf(SimpleXMLValidator.pathForFiles)));
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".xml")) {
                        try {
                            validate(SimpleXMLValidator.schemaFile, pathForFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                SimpleXMLValidator.selectFilesFromDir(String.valueOf(SimpleXMLValidator.pathToSelectedFiles));
                logTime(startLocalValidatingTime);
            }
//------------------------------------------------FTP-Tab---------------------------------------------------------------
            if (ftpTab.isSelected()) {
//------------------------------------------------Not-Other-------------------------------------------------------------
                if (!SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    SimpleXMLValidator.selectedEnvironment = environment.getValue();
                    SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue();
                    SimpleXMLValidator.manualDir = ftpManualDir.getText();
                }
//------------------------------------------------Get-schema-path-from-text-area----------------------------------------
                if (!(schemaFilePath.getText().equals(""))) {
                    pathForSchema = schemaFilePath.getText();
                    SimpleXMLValidator.schemaFile = new File(schemaFilePath.getText());
                }
//------------------------------------------------Preparation-other-FTP-------------------------------------------------

                if (SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    SimpleXMLValidator.otherFTPManualDir = otherFTPManualDir.getText();
                    SimpleXMLValidator.manualDir = otherFTPManualDir.getText();
                    SimpleXMLValidator.ftpOther = ftpOtherURL.getText();
                    SimpleXMLValidator.username = ftpLogin.getText();
                    SimpleXMLValidator.password = ftpPassword.getText();
                }
//------------------------------------------------Initial-Environments--------------------------------------------------
                try {
                    SimpleXMLValidator.initialEnvironments();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(consoleToArea = "Problem with initial environment ! ... \n" + e);
                    this.consoleArea();
                }
//------------------------------------------------Check-necessary-fields------------------------------------------------
                if ((SimpleXMLValidator.selectedEnvironment.isEmpty() | SimpleXMLValidator.ftpBaseFolder.equals("")) & !SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    System.out.println(consoleToArea = "Please specify environment and base folder! ...");
                    this.consoleArea();
                    return;
                }

                if (SimpleXMLValidator.ftpOther.equals("") & SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    System.out.println(consoleToArea = "Please specify other environment! ...");
                    this.consoleArea();
                    return;
                }
//------------------------------------------------ftpClient-------------------------------------------------------------
                consoleToArea = ("\n" + "Connect ... and download FTP files ..." + "\n" + "It could take a lot of time, if end folder contains folders with files ...");
                this.consoleArea();
                long startLoadingTime = System.nanoTime();
                indicator.setVisible(true);
                try {
                    SimpleXMLValidator.ftpClient();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Problem with connection to FTP. Error - " + e);
                    System.out.println(consoleToArea = "Please specify environment ! ... Or check VPN connection... \n Error: " + e);
                    this.consoleArea();
                    indicator.setVisible(false);
                    return;
                }
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
                logTime(startLoadingTime);
                indicator.setVisible(false);
//------------------------------------------------Check-if-temp-folder-is-empty-----------------------------------------
                if (SimpleXMLValidator.pathForFiles == null) {
                    System.out.println(consoleToArea = "--------------------------------------------------- FTP End folder is empty!!! ... Or the path to the FTP files is incorrect ... ---------------------------------------------------");
                    this.consoleArea();
                    return;
                }
//------------------------------------------------Unzipping-------------------------------------------------------------
                consoleToArea = ("\n" + "Unzipping loaded files ..." + "\n");
                this.consoleArea();
                long startUnzippingTime = System.nanoTime();
                indicator.setVisible(true);
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".zip")) {
                        try {
                            SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println(consoleToArea = "Unpacking error ... " + e);
                            this.consoleArea();
                            indicator.setVisible(false);
                            return;
                        }
                    }
                }
                indicator.setVisible(false);
                logTime(startUnzippingTime);
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
//------------------------------------------------Validating-------------------------------------------------------------
                consoleToArea = ("\n" + "Validating ..." + "\n");
                this.consoleArea();
                long startValidatingTime = System.nanoTime();
                this.setPromptXMLFilePath(new File(SimpleXMLValidator.tempFiles));
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".xml")) {
                        try {
                            validate(SimpleXMLValidator.schemaFile, pathForFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println(consoleToArea = "Validation method error ... \n" + e);
                            this.consoleArea();
                            return;
                        }
                    }
                }
                logTime(startValidatingTime);
            }
            consoleToArea = ("\n" + "----------------------------------------------------------------------------- !!! Verification completed !!! ------------------------------------------------------------------------");
            this.consoleArea();
            consoleToArea = ("\n" + "----------------------------------------------------------- !!! Invalid files saved to: \"C:\\XMLValidator\\invalidFiles\" !!! -----------------------------------------------------------" + "\n");
            this.consoleArea();
            logTime(startTime);
        }).start());
//------------------------------------------------Initial-open-folder-button-in-help-menu-------------------------------
        invalidFilesWindow.setOnAction(actionEvent -> {
                    try {
                        Desktop.getDesktop().open(new File(SimpleXMLValidator.invalidFiles));
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(consoleToArea = "Error opening invalidFiles folder ... \n" + e);
                        this.consoleArea();
                    }
                }
        );
//------------------------------------------------Initial-clearConsole-button-in-help-menu------------------------------
        clearConsole.setOnAction(actionEvent -> {
            console.clear();
            SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));

        });
//------------------------------------------------Initial-linkToWiki-button-in-help-menu--------------------------------
        linkToWiki.setOnAction(actionEvent -> {
            consoleToArea = "Sorry for inconvenience." + "\n" + "Wiki page does not exist at this moment.";
            this.consoleArea();
        });
    }

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
            this.consoleArea();
        } catch (SAXException | IOException e) {
            if (schemaPath == null || filePath == null) {
                System.out.println(consoleToArea = "Please provide the path to the schema and/or XML file for validation!!!" + "\n");
            } else {
                System.out.println(consoleToArea = (filePath.getName() + " - Size is : " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS NOT VALID." + "\n" + "Reason : " + e + "\n"));
                try {
                    SimpleXMLValidator.copyFileUsingStream(filePath, new File(SimpleXMLValidator.invalidFiles + filePath.getName()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("Error while copping invalid file... \n" + ex);
                }
            }
            this.consoleArea();
        }
    }

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

    public static void writeToLogFile() {
        try {
            writer = new PrintWriter((new FileWriter(SimpleXMLValidator.logFile, true)));
            writer.println(consoleToArea);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void logTime(Long startTime) {
        long endTime = System.nanoTime();
        long totalTimeInMilliseconds = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        System.out.println(consoleToArea = "Time: \n" + "sec.: " + totalTimeInMilliseconds / 1000 + "\nms.: " + totalTimeInMilliseconds);
        this.consoleArea();
    }

}