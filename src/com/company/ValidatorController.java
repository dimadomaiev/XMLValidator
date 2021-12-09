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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ValidatorController {
    public static String pathForSchema;
    public static String consoleToArea;

    @FXML
    private TextField ftpLogin;

    @FXML
    private PasswordField ftpPassword;

    @FXML
    private ChoiceBox<String> environment;
    ObservableList<String> environmentList = FXCollections.observableArrayList("PAK", "EIS1", "EIS2", "EIS3", "EIS4", "EIS5", "EIS6", "EIS7", "Other");

    @FXML
    private Text ftpBFText;

    @FXML
    private TextField ftpOtherBaseFolder;

    @FXML
    private ChoiceBox<String> ftpBaseFolder;
    ObservableList<String> ftpBaseFolderList = FXCollections.observableArrayList("fcs_nsi", "fcs_fas", "fcs_rules", "fcs_regions", "Other");

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
    private TextField ftpSchemaFilePath;

    void setPromptTextFTPSchemaFilePath() {
        ftpSchemaFilePath.setPromptText(pathForSchema);                                                                      //Устанавливаем путь к файлу в поле после выбора схемы
    }

    @FXML
    private TextField xmlFilePath;

    void setPromptXMLFilePath() {
        xmlFilePath.setPromptText(String.valueOf(SimpleXMLValidator.xmlFile));                                           //Устанавливаем путь к файлу в поле после выбора файла
    }

    @FXML
    private Button startValidation;

    @FXML
    private Button ftpStartValidation;

    @FXML
    private Button selectSchemaFile;

    @FXML
    private Button ftpSelectSchemaFile;

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
            ftpBFText.setVisible(!environment.getValue().equals("Other"));
            SimpleXMLValidator.stand = environment.getValue();
        });

        ftpBaseFolder.setItems(ftpBaseFolderList);
        ftpBaseFolder.setOnAction(actionEvent -> SimpleXMLValidator.ftpBaseFolder = ftpBaseFolder.getValue());

        selectSchemaFile.setOnAction(actionEvent -> {                                                                   //Задаем действие на кнопку selectSchemaFile
            SimpleXMLValidator.stageSchema(window);                                                                     //Вызываем метод выбора файла
            this.setPromptSchemaFilePath();                                                                                                 //Задаем в промте поля путь к выбранному файлу
        });

        ftpSelectSchemaFile.setOnAction(actionEvent -> {                                                                   //Задаем действие на кнопку selectSchemaFile
            SimpleXMLValidator.stageSchema(window);                                                                     //Вызываем метод выбора файла
            this.setPromptTextFTPSchemaFilePath();                                                                                                 //Задаем в промте поля путь к выбранному файлу
        });

        selectXMLFile.setOnAction(actionEvent -> {                                                                      // задаем действие на кнопку selectXMLFile
            SimpleXMLValidator.stageFile(window);                                                                       // Вызываем метод выбора файла
            this.setPromptXMLFilePath();                                                                                                 // Задаем в промте поля путь к выбранному файлу
        });

        startValidation.setOnAction(actionEvent -> validator());

        ftpStartValidation.setOnAction(actionEvent -> {
            SimpleXMLValidator.manualDir = ftpManualDir.getText();
            if (SimpleXMLValidator.schemaFile == null) {
                System.out.println(consoleToArea = "Please select schema to validate file(s)!");
                this.consoleArea();
                return;
            }

            if (SimpleXMLValidator.stand.equals("Other")) {
                SimpleXMLValidator.ftpOtherBaseFolder = ftpOtherBaseFolder.getText();
                SimpleXMLValidator.ftpOther = ftpOtherURL.getText();
            }

            consoleToArea = ("\n" + "Connect... to FTP and Downloading files ... " + "\n");
            this.consoleArea();

            if (SimpleXMLValidator.stand == null) {
                System.out.println(consoleToArea = "Please specify environment ! ...");
                this.consoleArea();
                return;
            }

            if (SimpleXMLValidator.ftpBaseFolder == null & !SimpleXMLValidator.stand.equals("Other")) {
                System.out.println(consoleToArea = "Please specify base folder of environment ! ...");
                this.consoleArea();
                return;
            }

            if (SimpleXMLValidator.ftpOtherBaseFolder == null & SimpleXMLValidator.stand.equals("Other")) {
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
            validator();

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

    void validator() {
        consoleToArea = ("\n" + "-------------------------------------------------------------------------" + "\n");
        this.consoleArea();
        try {
            for (File pathForFile : SimpleXMLValidator.pathForFiles) {                                                //Перебор выбранных XML файлов
                if (pathForFile.getName().endsWith(".xml")) {                                                        //Проверка что файл имеет xml расширение
                    SimpleXMLValidator.xmlFile = new File(pathForFile.getAbsolutePath());                            //Присваиваем путь к файлу глобальной переменной для передачи на валидацию
                    SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, SimpleXMLValidator.xmlFile);         //Передаем файлы в метод валидации
                    this.consoleArea();                                                                                    //Выводим результат валидации в текстовую область
                    String toEqual = consoleToArea;                                                                 //Создаем переменную для устранения дублирования вывода в случае сохранения не валидного файла
                    SimpleXMLValidator.writeFile(SimpleXMLValidator.xmlFile);                                       //Передаем файлы на сохранение  (нужно разобраться как сохранить только не валидные)
                    if (!consoleToArea.equals(toEqual)) {                                                           //Проверяем что consoleToArea не равна предидущему значению
                        this.consoleArea();                                                                                //Выводим результат валидации в текстовую область
                    }
                }
                if (pathForFile.getName().endsWith(".zip")) {                                                        //Проверка, если файл является архивом
                    SimpleXMLValidator.xmlFile = new File(pathForFile.getAbsolutePath());                            //Присваиваем путь к файлу глобальной переменной для передачи на валидацию
                    System.out.println("Start unzipping " + pathForFile.getName());
                    this.consoleArea();
                    SimpleXMLValidator.unzip(pathForFile.getAbsolutePath(), SimpleXMLValidator.tempFiles);           //Передаем архив на распаковку
                    File file = new File(SimpleXMLValidator.tempFiles);                                             //Создаемы ссылку на распакованный файл
                    String[] filesList;                                                                             //Массив строк для ссылок на файлы
                    if (file.isDirectory()) {                                                                       //Проверяем является ли ссылка директорией
                        filesList = file.list();                                                                    //Присваиваем списку файлы из временной директории
                        assert filesList != null;                                                                   //Проверка списка на null
                        for (String s : filesList) {                                                                //Перебор списка файлов
                            if (s.endsWith(".xml")) {
                                System.out.println("Validate - " + s);
                                SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile,
                                        new File(SimpleXMLValidator.tempFiles + s));                       //Передаем разархивированные файлы в метод валидации
                                this.consoleArea();                                                                        //Выводим результат валидации в текстовую область
                                String toEqual = consoleToArea;                                                     //Создаем переменную для устранения дублирования вывода в случае сохранения не валидного файла
                                if (ValidatorController.consoleToArea.contains("IS NOT VALID.")) {
                                    if (!Files.exists(Paths.get(SimpleXMLValidator.invalidFiles))) {
                                        Files.createDirectories(Paths.get(SimpleXMLValidator.invalidFiles));
                                    }
                                    SimpleXMLValidator.copyFileUsingStream(new File(SimpleXMLValidator.tempFiles + s), new File(SimpleXMLValidator.invalidFiles + s));
                                    ValidatorController.consoleToArea = ("Invalid file !!! SAVED !!! to directory: " + SimpleXMLValidator.invalidFiles + "\n");
                                }
                                if (!consoleToArea.equals(toEqual)) {                                               //Проверяем что consoleToArea не равна предидущему значению
                                    this.consoleArea();                                                                    //Выводим результат валидации в текстовую область
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | NullPointerException e) {                                                            //Отлавливаем ошибки в случае отсутствия пути к файлам или выбора архива не содержащего xml файлов
            e.printStackTrace();                                                                                    //Вывод ошибки в консоль
            System.out.println("Error in unzip method " + e);                                                       //Вывод в консоль сообщения где возникла ошибка
            consoleToArea = "Selected File(s) is not exist. Please select any file(s)!";                            //Присваиваем глоб.перем. сообщение для вывода в текстовую область программы
            this.consoleArea();                                                                                            //Выводим результат в текстовую область
        }
        //SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.tempFiles));                                           //Удаление файлов из временной папки после валидации содержимого 1-го архива
    }
}