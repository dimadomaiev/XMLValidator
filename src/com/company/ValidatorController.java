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

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ValidatorController {
    public static String pathForSchema;
    public static String consoleToArea;

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
    ObservableList<String> environmentList = FXCollections.observableArrayList("PAK", "EIS1", "EIS2", "EIS3", "EIS4", "EIS5", "EIS6", "EIS7", "Other");

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

    void setPromptSchemaFilePath() {
        schemaFilePath.setPromptText(pathForSchema);                                                                      //Устанавливаем путь к файлу в поле после выбора схемы
    }

    @FXML
    private TextField xmlFilePath;

    void setPromptXMLFilePath(File file) {
        xmlFilePath.setPromptText(String.valueOf(file));                                           //Устанавливаем путь к файлу в поле после выбора файла
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
        SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.invalidFiles));                                        //Удаление не валидные файлов из временной папки при запуске программы
        Stage window = new Stage();                                                                                     // Инициализируем окно
        environment.setItems(environmentList);
        environment.setOnAction(actionEvent -> {
            ftpOtherURL.setVisible(environment.getValue().equals("Other"));
            ftpManualDir.setVisible(!environment.getValue().equals("Other"));
            ftpLogin.setVisible(environment.getValue().equals("Other"));
            ftpPassword.setVisible(environment.getValue().equals("Other"));
            ftpBaseFolder.setVisible(!environment.getValue().equals("Other"));
            otherFTPManualDir.setVisible(environment.getValue().equals("Other"));
            ftpBaseFolderText.setVisible(!environment.getValue().equals("Other"));
            SimpleXMLValidator.selectedEnvironment = environment.getValue();
        });

        ftpBaseFolder.setItems(ftpBaseFolderList);
        ftpBaseFolder.setOnAction(actionEvent -> SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue());

        selectSchemaFile.setOnAction(actionEvent -> {                                                                   //Задаем действие на кнопку selectSchemaFile
            SimpleXMLValidator.stageSchema(window);                                                                     //Вызываем метод выбора файла
            this.setPromptSchemaFilePath();                                                                             //Задаем в промте поля путь к выбранному файлу
        });

        selectXMLFile.setOnAction(actionEvent -> {                                                                      // Задаем действие на кнопку selectXMLFile
            SimpleXMLValidator.stageFile(window);                                                                       // Вызываем метод выбора файла
            this.setPromptXMLFilePath(SimpleXMLValidator.xmlFile);                                                                                // Задаем в промте поля путь к выбранному файлу
        });
//----------------------------------------------------------------------------------------------------------------------
        startValidation.setOnAction(actionEvent -> {
            if (SimpleXMLValidator.schemaFile == null) {
                System.out.println(consoleToArea = "Please select schema to validate file(s)!");
                this.consoleArea();
                return;
            }
            if (localTab.isSelected()) {
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    try {
                        SimpleXMLValidator.copyFileUsingStream(pathForFile, new File(SimpleXMLValidator.tempFiles + pathForFile.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                SimpleXMLValidator.selectTempFTPFiles();
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".zip")) {
                        try {
                            SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                SimpleXMLValidator.selectTempFTPFiles();
                this.setPromptXMLFilePath(new File(SimpleXMLValidator.tempFiles));

                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".xml")) {
                        try {
                            validate(SimpleXMLValidator.schemaFile, pathForFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
//----------------------------------------------------------------------------------------------------------------------
            if (ftpTab.isSelected()) {
                SimpleXMLValidator.manualDir = ftpManualDir.getText();
                if (SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    SimpleXMLValidator.otherFTPManualDir = otherFTPManualDir.getText();
                    SimpleXMLValidator.manualDir = otherFTPManualDir.getText();
                    SimpleXMLValidator.ftpOther = ftpOtherURL.getText();
                }

                consoleToArea = ("\n" + "Connect... to FTP and Downloading files ... " + "\n");
                this.consoleArea();

                if (SimpleXMLValidator.selectedEnvironment == null) {
                    System.out.println(consoleToArea = "Please specify environment ! ...");
                    this.consoleArea();
                    return;
                }

                if (SimpleXMLValidator.ftpBaseFolder == null & !SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    System.out.println(consoleToArea = "Please specify base folder of environment ! ...");
                    this.consoleArea();
                    return;
                }

                if (SimpleXMLValidator.otherFTPManualDir == null & SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    System.out.println(consoleToArea = "Please specify base folder of other environment ! ...");
                    this.consoleArea();
                    return;
                }

                consoleToArea = ("\n" + "Connect ... and download FTP files ..." + "\n");
                this.consoleArea();
                try {
                    SimpleXMLValidator.ftpClient();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Что-то с подключением к FTP. Error - " + e);
                }

                consoleToArea = ("\n" + "Unzip loaded files ..." + "\n");
                this.consoleArea();
                SimpleXMLValidator.selectTempFTPFiles();
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".zip")) {
                        try {
                            SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                consoleToArea = ("\n" + "Validate ..." + "\n");
                this.consoleArea();
                SimpleXMLValidator.selectTempFTPFiles();
                this.setPromptXMLFilePath(new File(SimpleXMLValidator.tempFiles));
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".xml")) {
                        try {
                            validate(SimpleXMLValidator.schemaFile, pathForFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
            }
        });

        invalidFilesWindow.setOnAction(actionEvent -> {
                    try {
                        Desktop.getDesktop().open(new File(SimpleXMLValidator.invalidFiles));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

        );
        clearConsole.setOnAction(actionEvent -> {
            console.clear();
            SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));
        });
        linkToWiki.setOnAction(actionEvent -> {
            consoleToArea = "Sorry for the inconvenience." + "\n" + "Wiki page does not exist at this moment.";
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
            System.out.println(consoleToArea = (myXMLFile.getSystemId() + " - Size is: " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS VALID \n"));
            this.consoleArea();
        } catch (SAXException | IOException e) {
            if (schemaPath == null || filePath == null) {
                System.out.println(consoleToArea = "Please provide the path to the schema and/or XML file for validation!!!" + "\n");
            } else {
                System.out.println(consoleToArea = (myXMLFile.getSystemId() + " - Size is: " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS NOT VALID." + "\n" + "Reason: " + e + "\n"));
                SimpleXMLValidator.copyFileUsingStream(filePath, new File(SimpleXMLValidator.invalidFiles + filePath.getName()));
            }
            this.consoleArea();
        }
    }

}