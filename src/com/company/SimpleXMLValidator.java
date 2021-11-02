package com.company;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SimpleXMLValidator extends Application {
    public static File schemaFile = null;
    public static File XMLFile = null;
    public static File pasToSelectedFiles = null;
    public static String tempFiles = "C:\\XMLValidator\\tempFiles\\";
    public static String invalidFiles = "C:\\XMLValidator\\invalidFiles\\";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleXMLValidator.class.getResource("Validator-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        stage.setTitle("SimpleXMLValidator!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void stageSchema(Stage s) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "XSD files (*.xsd)", "*.xsd");
        fileChooser.getExtensionFilters().add(extFilter);
        File selectedFile;
        if (schemaFile != null) {
            fileChooser.setInitialDirectory(new File(schemaFile.getParent()));
        }
        selectedFile = fileChooser.showOpenDialog(s);
        if (selectedFile != null) {
            schemaFile = selectedFile;
            System.out.println();
            //System.out.print("You have selected the file " + selectedFile.getAbsolutePath());
            ValidatorController.pasForSchema = selectedFile.getAbsolutePath();
            System.out.println();
            //System.out.println("schemaFile получает значение selectedFile = " + schemaFile);
        }
    }

    public static void stageFile(Stage s) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml", "*.zip"));// ,"*.7z","*.rar"));

        if (pasToSelectedFiles != null) {
            fileChooser.setInitialDirectory(new File(pasToSelectedFiles.getParent()));
        }
        ValidatorController.pasForFiles = fileChooser.showOpenMultipleDialog(s);
        if (ValidatorController.pasForFiles != null) {
            for (File pasForFile : ValidatorController.pasForFiles) {
                if (pasForFile != null) {
                    pasToSelectedFiles = new File(pasForFile.getAbsolutePath());
                    XMLFile = pasToSelectedFiles;
                    //System.out.println("Путь к файлу после выбора архива " + pasForFile.getAbsolutePath());
                }
            }
        }
    }

    public static void validate(File absoluteSchemaPath, File absoluteFilePath) throws IOException {
        File mySchemaFile = new File(String.valueOf(absoluteSchemaPath));
        Source myXMLFile = new StreamSource(new File(String.valueOf(absoluteFilePath)));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(mySchemaFile);
            Validator validator = schema.newValidator();
            validator.validate(myXMLFile);
            ValidatorController.consoleToArea = (myXMLFile.getSystemId() + " - Size is: " + (new File(fileSize(absoluteFilePath.length()))) + " - IS VALID ");

        } catch (SAXException | IOException e) {
            if (absoluteSchemaPath == null || absoluteFilePath == null) {
                System.out.println(ValidatorController.consoleToArea = "Please provide the path to the schema and/or XML file for validation!!!");
            } else if (absoluteSchemaPath != null || absoluteFilePath != null) {
                ValidatorController.consoleToArea = (myXMLFile.getSystemId() + " - Size is: " + (new File(fileSize(absoluteFilePath.length()))) + " - IS NOT VALID." + "\n" + "Reason: " + e);
            }
        }
    }

    public static void writeFile(String files) throws IOException {
        if (!Files.exists(Paths.get(invalidFiles))) {
            Files.createDirectories(Paths.get(invalidFiles));
        }
        Writer output;
        File file = new File(files);
        output = new BufferedWriter(new FileWriter(file));
        output.close();
        ValidatorController.consoleToArea = ("Invalid file !!! SAVED !!! to directory: " + invalidFiles);
    }

    static String fileSize(Long size) {
        DecimalFormat df = new DecimalFormat("0.00");
        float sizeKb = 1024.0f;
        float sizeMo = sizeKb * sizeKb;
        float sizeGo = sizeMo * sizeKb;
        float sizeTerra = sizeGo * sizeKb;

        if (size < sizeMo)
            return df.format(size / sizeKb) + " Kb";
        else if (size < sizeGo)
            return df.format(size / sizeMo) + " Mb";
        else if (size < sizeTerra)
            return df.format(size / sizeGo) + " Gb";
        return "";
    }

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {

        if (!Files.exists(Paths.get(destDirectory))) {
            Files.createDirectories(Paths.get(destDirectory));
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            XMLFile = new File(filePath);
            if (!entry.isDirectory()) {
                if (entry.getName().endsWith(".xml")) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                }

            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[8192];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    static void deleteAllTempFile(String destDir) {
        File file = new File(destDir);
        String[] filesList;
        if (file.isDirectory()) {
            filesList = file.list();
            assert filesList != null;
            for (String s : filesList) {
                File tempFile = new File(destDir + s);
                tempFile.delete();
            }
        }
    }
}

