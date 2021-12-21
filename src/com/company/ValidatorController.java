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
import java.io.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
        SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.invalidFiles));                           //Удаление не валидные файлов из временной папки при запуске программы
        SimpleXMLValidator.createConfigFile();
        try {
            SimpleXMLValidator.initialEnvironments();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(consoleToArea = "Error initial config file.\n" + e);
            this.consoleArea();
        }
        Stage window = new Stage();                                                                                     // Инициализируем окно
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

        selectSchemaFile.setOnAction(actionEvent -> {                                                                   //Задаем действие на кнопку selectSchemaFile
            schemaFilePath.clear();
            SimpleXMLValidator.stageSchema(window);                                                                     //Вызываем метод выбора файла
            this.setPromptSchemaFilePath(SimpleXMLValidator.schemaFile);                                                //Задаем в промте поля путь к выбранному файлу
        });


        selectXMLFile.setOnAction(actionEvent -> {                                                                      // Задаем действие на кнопку selectXMLFile
            xmlFilePath.clear();
            SimpleXMLValidator.stageFile(window);                                                                       // Вызываем метод выбора файла
            this.setPromptXMLFilePath(SimpleXMLValidator.xmlFile);                                                      // Задаем в промте поля путь к выбранному файлу
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
//----------------------------------------------------Local-------------------------------------------------------------
            if (localTab.isSelected()) {
                System.out.println(consoleToArea = "Copying file(s) to temp folder ...\n");
                this.consoleArea();
                if (SimpleXMLValidator.pathForFiles == null & ((SimpleXMLValidator.xmlFile.getName().endsWith(".xml") | SimpleXMLValidator.xmlFile.getName().endsWith(".zip")))) {
                    try {
                        SimpleXMLValidator.copyFileUsingStream(SimpleXMLValidator.xmlFile, new File(SimpleXMLValidator.tempFiles + SimpleXMLValidator.xmlFile.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                        try {
                            SimpleXMLValidator.copyFileUsingStream(pathForFile, new File(SimpleXMLValidator.tempFiles + pathForFile.getName()));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);

                System.out.println(consoleToArea = "Unzipping file(s) from temp folder...\n");
                this.consoleArea();
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".zip")) {
                        try {
                            SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);

                System.out.println(consoleToArea = "Validating ...\n");
                this.consoleArea();
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
            }
//------------------------------------------------FTP-------------------------------------------------------------------
            if (ftpTab.isSelected()) {
                if (SimpleXMLValidator.selectedEnvironment == null) {
                    System.out.println(consoleToArea = "Please specify environment ! ...");
                    this.consoleArea();
                    return;
                }

                if (!(schemaFilePath.getText().equals(""))) {
                    pathForSchema = schemaFilePath.getText();
                    SimpleXMLValidator.schemaFile = new File(schemaFilePath.getText());
                }

                SimpleXMLValidator.manualDir = ftpManualDir.getText();
                if (SimpleXMLValidator.selectedEnvironment.equals("Other")) {
                    SimpleXMLValidator.otherFTPManualDir = otherFTPManualDir.getText();
                    SimpleXMLValidator.manualDir = otherFTPManualDir.getText();
                    SimpleXMLValidator.ftpOther = ftpOtherURL.getText();
                    if (!(ftpLogin.getText().equals(""))) {
                        SimpleXMLValidator.username = ftpLogin.getText();
                    }
                    if (!(ftpPassword.getText().equals(""))) {
                        SimpleXMLValidator.password = ftpPassword.getText();
                    }
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

                consoleToArea = ("\n" + "Connect ... and download FTP files ..." + "\n" + "It could take a lot of time, if end folder contains folders with files ...");
                this.consoleArea();

                //SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);

                try {
                    long startLoadingTime = System.nanoTime();
                    SimpleXMLValidator.ftpClient();
                    long endLoadingTime = System.nanoTime();
                    long totalLoadingTimeInMilliseconds = TimeUnit.MILLISECONDS.convert((endLoadingTime - startLoadingTime), TimeUnit.NANOSECONDS);
                    System.out.println(consoleToArea = "Time: " + totalLoadingTimeInMilliseconds + " ms" + "\n");
                    this.consoleArea();
                    SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Problem with connection to FTP. Error - " + e);
                    System.out.println(consoleToArea = "Please specify environment ! ... Or check VPN connection... ");
                    this.consoleArea();
                    return;
                }

                if (SimpleXMLValidator.pathForFiles == null) {
                    System.out.println(consoleToArea = "--------------------------------------------------- FTP End folder is empty!!! ... Or the path to the FTP files is incorrect ... ---------------------------------------------------");
                    this.consoleArea();
                    return;
                }

                consoleToArea = ("\n" + "Unzipping loaded files ..." + "\n");
                this.consoleArea();
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".zip")) {
                        try {
                            long startUnzippingTime = System.nanoTime();
                            SimpleXMLValidator.unzip(String.valueOf(pathForFile), SimpleXMLValidator.tempFiles);
                            long endUnzippingTime = System.nanoTime();
                            long totalUnzippingTimeInMilliseconds = TimeUnit.MILLISECONDS.convert((endUnzippingTime - startUnzippingTime), TimeUnit.NANOSECONDS);
                            System.out.println(consoleToArea = "Time: " + totalUnzippingTimeInMilliseconds + " ms" + "\n");
                            this.consoleArea();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println(consoleToArea = "Unpacking error ... " + e);
                            this.consoleArea();
                            return;
                        }
                    }
                }
                SimpleXMLValidator.selectFilesFromDir(SimpleXMLValidator.tempFiles);

                consoleToArea = ("\n" + "Validating ..." + "\n");
                this.consoleArea();
                this.setPromptXMLFilePath(new File(SimpleXMLValidator.tempFiles));
                for (File pathForFile : SimpleXMLValidator.pathForFiles) {
                    if (pathForFile.getName().endsWith(".xml")) {
                        try {
                            long startValidatingTime = System.nanoTime();
                            validate(SimpleXMLValidator.schemaFile, pathForFile);
                            long endValidatingTime = System.nanoTime();
                            long totalValidatingTimeInMilliseconds = TimeUnit.MILLISECONDS.convert((endValidatingTime - startValidatingTime), TimeUnit.NANOSECONDS);
                            System.out.println(consoleToArea = "Time: " + totalValidatingTimeInMilliseconds + " ms" + "\n");
                            this.consoleArea();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println(consoleToArea = "Validation method error ... \n" + e);
                            this.consoleArea();
                            return;
                        }
                    }
                }
                //SimpleXMLValidator.selectFilesFromDir(3);
            }
            consoleToArea = ("\n" + "----------------------------------------------------------------------------- !!! Verification completed !!! ------------------------------------------------------------------------");
            this.consoleArea();
            consoleToArea = ("\n" + "----------------------------------------------------------- !!! Invalid files saved to: \"C:\\XMLValidator\\invalidFiles\" !!! -----------------------------------------------------------" + "\n");
            this.consoleArea();
            long endTime = System.nanoTime();
            long totalElapsedTimeInMilliseconds = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
            System.out.println(consoleToArea = "Total elapsed time: " + totalElapsedTimeInMilliseconds + " ms" + "\n");
            this.consoleArea();
        }).start());


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

        clearConsole.setOnAction(actionEvent -> {
            console.clear();
            SimpleXMLValidator.deleteAllFilesWithDirs(new File(SimpleXMLValidator.tempFiles));

        });

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
            System.out.println(consoleToArea = (filePath.getName() + " - Size is: " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS VALID \n"));
            this.consoleArea();
        } catch (SAXException | IOException e) {
            if (schemaPath == null || filePath == null) {
                System.out.println(consoleToArea = "Please provide the path to the schema and/or XML file for validation!!!" + "\n");
            } else {
                System.out.println(consoleToArea = (filePath.getName() + " - Size is: " + (new File(SimpleXMLValidator.fileSize(filePath.length()))) + " - IS NOT VALID." + "\n" + "Reason: " + e + "\n"));
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
}