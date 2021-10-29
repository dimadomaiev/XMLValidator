package com.company;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ValidatorController {
    public static String pasForSchema;
    public static List<File> pasForFiles;
    public static String consoleToArea;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private MenuItem linkToViki;

    @FXML
    private TextField schemaFilePas;

    void sfp() {
        schemaFilePas.setPromptText(pasForSchema);
    }

    @FXML
    private TextField xmlFilePas;

    void xfp() {
        xmlFilePas.setPromptText(String.valueOf(pasForFiles));
    }

    @FXML
    private Button startValidation;

    @FXML
    private Button selectSchemaFile;

    @FXML
    private Button selectXMLFile;

    @FXML
    private TextArea console;

    void area(String consoleToArea) {
        console.appendText("\n" + consoleToArea);      // добавляем текст в TextArea с сохранением
        //console.setText("\n" + consoleToArea );          // Добавляем новый текст в TextArea
        console.setWrapText(true);                        // Выравнивать текст в область текстого поля

    }

    @FXML
    void initialize() {
        Stage window = new Stage();                         // Инициализируем окно
        selectSchemaFile.setOnAction(actionEvent -> {       // задаем действие на кнопку selectSchemaFile
            SimpleXMLValidator.stageSchema(window);           // Вызываем метод выбора файла
            this.sfp();                                     // Задаем в промте поля путь к выбранному файлу
        });

        selectXMLFile.setOnAction(actionEvent -> {          // задаем действие на кнопку selectXMLFile
            SimpleXMLValidator.stageFile(window);           // Вызываем метод выбора файла
            this.xfp();                                     // Задаем в промте поля путь к выбранному файлу
        });
        startValidation.setOnAction(actionEvent -> {
            for (File pasForFile : pasForFiles) {
                //System.out.println(pasForFiles);
                if (pasForFile.getName().endsWith(".xml")) {
                    SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());
                    SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, SimpleXMLValidator.XMLFile); // Передаем файлы в метод валидации

                }
                if (pasForFile.getName().endsWith(".zip")) {
                    SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());
                    try {
                        SimpleXMLValidator.unzip(pasForFile.getAbsolutePath(),SimpleXMLValidator.destDirectory);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error in unzip method " + e);
                    }
                }
            }
        });
    }
}