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
        schemaFilePas.setPromptText(pasForSchema);                                                                      //Устанавливаем путь к файлу в поле после выбора схемы
    }

    @FXML
    private TextField xmlFilePas;

    void xfp() {
        xmlFilePas.setPromptText(String.valueOf(SimpleXMLValidator.XMLFile));                                           //Устанавливаем путь к файлу в поле после выбора файла
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
        console.appendText("\n" + consoleToArea);                                                                       //Добавляем текст в TextArea с сохранением
        console.setWrapText(true);                                                                                      //Выравнивать текст в область текстого поля
    }

    @FXML
    void initialize(){
        SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.invalidFiles));                                        //Удаление не валидные файлов из временной папки при запуске программы
        Stage window = new Stage();                                                                                     // Инициализируем окно
        selectSchemaFile.setOnAction(actionEvent -> {                                                                   //Задаем действие на кнопку selectSchemaFile
            SimpleXMLValidator.stageSchema(window);                                                                     //Вызываем метод выбора файла
            this.sfp();                                                                                                 //Задаем в промте поля путь к выбранному файлу
        });

        selectXMLFile.setOnAction(actionEvent -> {          // задаем действие на кнопку selectXMLFile
            SimpleXMLValidator.stageFile(window);           // Вызываем метод выбора файла
            this.xfp();                                     // Задаем в промте поля путь к выбранному файлу
        });
        startValidation.setOnAction(actionEvent -> {
            consoleToArea = ("\n" + "!!! STARTING VALIDATION !!!" + "\n");
            this.area();

            try {
                for (File pasForFile : pasForFiles) {                                                           //Перебор выбранных XML файлов
                    if (pasForFile.getName().endsWith(".xml")) {                                                //Проверка что файл имеет xml расширение
                        SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());                    //Присваиваем путь к файлу глобальной переменной для передачи на валидацию
                        SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, SimpleXMLValidator.XMLFile); //Передаем файлы в метод валидации
                        this.area();                                                                            //Выводим результат валидации в текстовую область
                    }
                    if (pasForFile.getName().endsWith(".zip")) {                                                //Проверка, если файл является архивом
                        SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());                    //Присваиваем путь к файлу глобальной переменной для передачи на валидацию
                        SimpleXMLValidator.unzip(pasForFile.getAbsolutePath(), SimpleXMLValidator.tempFiles);   //Передаем архив на распаковку
                        File file = new File(SimpleXMLValidator.tempFiles);                                     //Создаемы ссылку на распакованный файл
                        String[] filesList;                                                                     //Массив строк для ссылок на файлы
                        if (file.isDirectory()) {                                                               //Проверяем является ли ссылка директорией
                            filesList = file.list();                                                            //Присваиваем списку файлы из временной директории
                            assert filesList != null;                                                           //Проверка списка на null
                            for (String s : filesList) {                                                        //Перебор списка файлов
                                SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, new File(SimpleXMLValidator.tempFiles + s)); //Передаем разархивированные файлы в метод валидации
                                this.area();                                                                    //Выводим результат валидации в текстовую область
                                String toEqual = consoleToArea;                                                 // Создаем переменную для устранения дублирования вывода в случае сохранения не валидного файла
                                SimpleXMLValidator.writeFile(SimpleXMLValidator.invalidFiles + s);         // Передаем файлы на сохранение  (нужно разобраться как сохранить только не валидные)
                                if (!consoleToArea.equals(toEqual)){
                                    this.area();                                                                //Выводим результат валидации в текстовую область
                                }
                            }
                        }
                        SimpleXMLValidator.deleteDir(new File(SimpleXMLValidator.tempFiles));                   //Удаление файлов из временной папки после валидации содержимого 1-го архива
                    }
                }
            } catch (IOException | NullPointerException e) {                                                    //Отлавливаем ошибки в случае отсутствия пути к файлам или выбора архива не содержащего xml файлов
                e.printStackTrace();                                                                            //Вывод ошибки в консоль
                System.out.println("Error in unzip method " + e);                                               //Вывод в консоль сообщения где возникла ошибка
                consoleToArea = "Selected File(s) is not exist. Please select any file(s)!";                    //Присваиваем глоб.перем. сообщение для вывода в текстовую область программы
                this.area();                                                                                    //Выводим результат в текстовую область
            }
        });
    }
}