package com.company;

import javafx.application.Platform;
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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

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
    private TextField ftpOtherBaseFolder;

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

    void setPromptXMLFilePath() {
        xmlFilePath.setPromptText(String.valueOf(SimpleXMLValidator.xmlFile));                                           //Устанавливаем путь к файлу в поле после выбора файла
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
    private void initialize() {
        SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.tempFiles));
        SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.invalidFiles));                                        //Удаление не валидные файлов из временной папки при запуске программы
        Stage window = new Stage();                                                                                     // Инициализируем окно
        environment.setItems(environmentList);
        environment.setOnAction(actionEvent -> {
            ftpOtherURL.setVisible(environment.getValue().equals("Other"));
            ftpManualDir.setVisible(!environment.getValue().equals("Other"));
            ftpLogin.setVisible(environment.getValue().equals("Other"));
            ftpPassword.setVisible(environment.getValue().equals("Other"));
            ftpBaseFolder.setVisible(!environment.getValue().equals("Other"));
            ftpOtherBaseFolder.setVisible(environment.getValue().equals("Other"));
            ftpBaseFolderText.setVisible(!environment.getValue().equals("Other"));
            SimpleXMLValidator.selectedEnvironment = environment.getValue();
        });

        ftpBaseFolder.setItems(ftpBaseFolderList);
        ftpBaseFolder.setOnAction(actionEvent -> SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue());

        selectSchemaFile.setOnAction(actionEvent -> {                                                                   //Задаем действие на кнопку selectSchemaFile
            SimpleXMLValidator.stageSchema(window);                                                                     //Вызываем метод выбора файла
            this.setPromptSchemaFilePath();                                                                                                 //Задаем в промте поля путь к выбранному файлу
        });

        selectXMLFile.setOnAction(actionEvent -> {                                                                      // задаем действие на кнопку selectXMLFile
            SimpleXMLValidator.stageFile(window);                                                                       // Вызываем метод выбора файла
            this.setPromptXMLFilePath();                                                                                                 // Задаем в промте поля путь к выбранному файлу
        });

        startValidation.setOnAction(actionEvent -> {
            if (localTab.isSelected()) {
                SimpleXMLValidator.validator();
            }
            if (ftpTab.isSelected()) {
                SimpleXMLValidator.manualDir = ftpManualDir.getText();
                if (SimpleXMLValidator.schemaFile == null) {
                    System.out.println(consoleToArea = "Please select schema to validate file(s)!");
                    appendText(consoleToArea);
                    //this.consoleArea();
                    return;
                }

                if (SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    SimpleXMLValidator.ftpOtherBaseFolder = ftpOtherBaseFolder.getText();
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

                if (SimpleXMLValidator.ftpOtherBaseFolder == null & SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    System.out.println(consoleToArea = "Please specify base folder of other environment ! ...");
                    this.consoleArea();
                    return;
                }

                try {
                    SimpleXMLValidator.ftpClient();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Что-то с подключением к FTP. Error - " + e);
                }

                consoleToArea = ("\n" + "Get loaded files ..." + "\n");
                this.consoleArea();

                SimpleXMLValidator.selectTempFTPFiles();
                consoleToArea = ("\n" + "Validate ..." + "\n");
                this.consoleArea();
                //try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
                SimpleXMLValidator.validator();
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
    }
    public void appendText(String valueOf) {
        Platform.runLater(() -> {
            console.appendText(valueOf);
            //console.setWrapText(true);
        });
    }

}