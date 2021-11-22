package com.company;

import org.xml.sax.SAXException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SimpleXMLValidator extends Application {
    public static File schemaFile = null;
    public static File XMLFile = null;
    public static List<File> pasForFiles;
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
            ValidatorController.pasForSchema = selectedFile.getAbsolutePath();
        }
    }

    public static void stageFile(Stage s) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "XML files (*.xml;*.zip)", "*.xml", "*.zip"));// ,"*.7z","*.rar"));
        if (pasToSelectedFiles != null) {
            fileChooser.setInitialDirectory(new File(pasToSelectedFiles.getParent()));
        }
        pasForFiles = fileChooser.showOpenMultipleDialog(s);
        if (pasForFiles != null) {
            for (File pasForFile : pasForFiles) {
                if (pasForFile != null) {
                    pasToSelectedFiles = new File(pasForFile.getAbsolutePath());
                    XMLFile = pasToSelectedFiles;
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
            ValidatorController.consoleToArea = (myXMLFile.getSystemId() + " - Size is: " +
                    (new File(fileSize(absoluteFilePath.length()))) + " - IS VALID ");
        } catch (SAXException | IOException e) {
            if (absoluteSchemaPath == null || absoluteFilePath == null) {
                System.out.println(ValidatorController.consoleToArea =
                        "Please provide the path to the schema and/or XML file for validation!!!");
            } else {
                ValidatorController.consoleToArea = (myXMLFile.getSystemId() + " - Size is: " +
                        (new File(fileSize(absoluteFilePath.length()))) + " - IS NOT VALID." + "\n" + "Reason: " + e);
            }
        }
    }

    public static void writeFile(File files) throws IOException {
        if (!Files.exists(Paths.get(invalidFiles))) {
            Files.createDirectories(Paths.get(invalidFiles));
        }
        if (ValidatorController.consoleToArea.contains("IS NOT VALID.")) {
            copyFileUsingStream(files, new File(invalidFiles + files.getName()));
            ValidatorController.consoleToArea = ("Invalid file !!! SAVED !!! to directory: " + invalidFiles + "\n");
        }
        copyFileUsingStream(files, new File(tempFiles + files.getName()));

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
        try {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + entry.getName();
                String replacedFilePath = filePath;
                if (filePath.contains("/")) {
                    replacedFilePath = (filePath.replace("/", "\\"));
                }
                if (entry.isDirectory()) {
                    mkDir(new File(replacedFilePath));
                }
                // if the entry is a directory, make the directory
                if (!entry.isDirectory() & replacedFilePath.endsWith(".xml")) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, tempFiles + entry.getName());
                    File fileName = new File(entry.getName());
                    System.out.println("XML файл ("+ fileName +") извлечён.");
                    copyFileUsingStream(new File(replacedFilePath), new File(tempFiles + fileName.getName()));
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        } catch (IllegalArgumentException e) {
            ValidatorController.consoleToArea = "Encoding error in to the Zip file - " + e;
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        mkDir(new File(new File(filePath).getParent()));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[8192];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        mkDir(new File(dest.getParent()));
        if (!source.equals(dest)) {
            try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            }
        }
    }

    static void mkDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                mkDir(f);
            }
        }
        file.mkdirs();
    }

    static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }
}