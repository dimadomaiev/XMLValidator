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
import java.io.File;
import java.io.IOException;

public class SimpleXMLValidator extends Application {
    public static File schemaFile = null;
    public static File XMLFile = null;

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)"));// ,"*.xml", "*.zip","*.7z","*.rar"));
        if (XMLFile != null) {
            fileChooser.setInitialDirectory(new File(XMLFile.getParent()));
        }
        ValidatorController.pasForFiles = fileChooser.showOpenMultipleDialog(s);



/*
        File selectedFile;
        selectedFile = fileChooser.showOpenDialog(s);
        if (selectedFile != null) {
            XMLFile = selectedFile;
            System.out.println();
            //System.out.print("You have selected the file " + selectedFile.getAbsolutePath());
            ValidatorController.pasForFile = selectedFile.getAbsolutePath();
            System.out.println();
            //System.out.println("schemaFile получает значение XMLFile = " + XMLFile);
        }
*/
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
}
