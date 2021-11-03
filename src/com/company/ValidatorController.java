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
        xmlFilePas.setPromptText(String.valueOf(SimpleXMLValidator.XMLFile));
    }

    @FXML
    private Button startValidation;

    @FXML
    private Button selectSchemaFile;

    @FXML
    private Button selectXMLFile;

    @FXML
    private TextArea console;

    void area() {
        console.appendText("\n" + consoleToArea);      // добавляем текст в TextArea с сохранением
        console.setWrapText(true);                        // Выравнивать текст в область текстого поля
    }

    @FXML
    void initialize(){
        SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.invalidFiles));
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
            consoleToArea = ("\n" + "!!! STARTING VALIDATION !!!" + "\n");
            this.area();

            try {
                for (File pasForFile : pasForFiles) {
                    if (pasForFile.getName().endsWith(".xml")) {
                        SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());
                        SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, SimpleXMLValidator.XMLFile); //Передаем файлы в метод валидации
                        this.area();
                    }
                    if (pasForFile.getName().endsWith(".zip")) {
                        SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());
                        SimpleXMLValidator.unzip(pasForFile.getAbsolutePath(), SimpleXMLValidator.tempFiles);
                        File file = new File(SimpleXMLValidator.tempFiles);
                        String[] filesList;
                        if (file.isDirectory()) {
                            filesList = file.list();
                            assert filesList != null;
                            for (String s : filesList) {
                                SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, new File(SimpleXMLValidator.tempFiles + s)); //Передаем разархивированные файлы в метод валидации
                                this.area();
                                String toEqual = consoleToArea;
                                SimpleXMLValidator.writeFile(SimpleXMLValidator.invalidFiles + s);                  // Передаем файлы на сохранение  (нужно разобраться как сохранить только не валидные)
                                if (!consoleToArea.equals(toEqual)){
                                    this.area();
                                }
                            }
                        }
                        SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.tempFiles));
                    }
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                System.out.println("Error in unzip method " + e);
                consoleToArea = "Selected File(s) is not exist. Please select any file(s)!";
                this.area();
            }
        });
    }
}