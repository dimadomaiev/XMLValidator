package com.company;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ValidatorController {
    public static String pasForSchema;
    public static String pasForFile;
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
        xmlFilePas.setPromptText(pasForFile);
    }

    @FXML
    private Button startValidation;

    @FXML
    private Button selectSchemaFile;

    @FXML
    private Button selectXMLFile;

    @FXML
    private TextArea console;

    void area(){
        console.appendText("\n" + consoleToArea );      // добавляем текст в TextArea с сохранением
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
            SimpleXMLValidator.stageFile(window);             // Вызываем метод выбора файла
            this.xfp();                                     // Задаем в промте поля путь к выбранному файлу
        });

        startValidation.setOnAction(actionEvent -> {        // Задаем действие на кнопку startValidation
            for (int i = 0; i < pasForFiles.size(); i++) {
                if (pasForFiles.get(i).getName().endsWith(".xml")) {
                    SimpleXMLValidator.XMLFile = new File(pasForFiles.get(i).getAbsolutePath());
                    SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, SimpleXMLValidator.XMLFile); // Передаем файлы в метод валидации
                    this.area();

                }
            }
                                              // Вызов метода вывода полученного результата валидации в текстовое поле
        });

    }
}
