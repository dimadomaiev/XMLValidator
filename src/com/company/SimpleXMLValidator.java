package com.company;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SimpleXMLValidator extends Application {
    public static File schemaFile = null;
    public static File XMLFile = null;
    public static String destDirectory = "C:\\XMLValidator\\tempFiles";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleXMLValidator.class.getResource("Validator-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        stage.setTitle("SimpleXMLValidator!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
	// write your code here
        launch(args);
    }

    public static void stageSchema (Stage s) {
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
    public static void stageFile (Stage s){
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)","*.xml", "*.zip"));// ,"*.7z","*.rar"));
        if (XMLFile != null) {
            fileChooser.setInitialDirectory(new File(XMLFile.getParent()));
        }
        ValidatorController.pasForFiles = fileChooser.showOpenMultipleDialog(s);
    }

    public static void parseFiles (List<File> pasForFiles) throws IOException {
        for (File pasForFile : pasForFiles) {
            //System.out.println(pasForFiles);
            if (pasForFile.getName().endsWith(".xml")) {
                SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());
                SimpleXMLValidator.validate(SimpleXMLValidator.schemaFile, SimpleXMLValidator.XMLFile); // Передаем файлы в метод валидации

            }
            if (pasForFile.getName().endsWith(".zip")) {
                SimpleXMLValidator.XMLFile = new File(pasForFile.getAbsolutePath());

            }
        }
    }



    public static void validate(File absoluteSchemaPath, File absoluteFilePath) {
        File mySchemaFile = new File(String.valueOf(absoluteSchemaPath));
        Source myXMLFile = new StreamSource(new File(String.valueOf(absoluteFilePath)));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(mySchemaFile);
            Validator validator = schema.newValidator();
            validator.validate(myXMLFile);
            ValidatorController.consoleToArea = (myXMLFile.getSystemId() + " - IS VALID ");
        } catch (SAXException e) {
            if (absoluteSchemaPath == null || absoluteFilePath == null) {
                System.out.println(ValidatorController.consoleToArea = "Пожалуйста укажите путь к схеме и файлу XML для валидации");
            } else if (absoluteSchemaPath != null || absoluteFilePath != null) {
                ValidatorController.consoleToArea = (myXMLFile.getSystemId() + " - IS NOT VALID."+ "\n" + "Reason: " + e);
            }
        } catch (IOException e) {}
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
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
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
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[8192];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}

