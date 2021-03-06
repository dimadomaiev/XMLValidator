package com.company;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SimpleXMLValidator extends Application {

    public static File schemaFile = null;
    public static File xmlFile = null;
    public static List<File> pathForFiles;
    public static String tempFiles = "C:\\XMLValidator\\tempFiles\\";
    public static String invalidFiles = "C:\\XMLValidator\\invalidFiles\\";
    public static String selectedEnvironment = "";
    public static String ftpBaseFolder = "";
    public static String ftpOther = "";
    public static String otherFTPManualDir = "";
    public static String manualDir = "";
    public static String username = "";
    public static String password = "";
    public static File ftpURL = new File("ftp://" + username + ":" + password + "@" + ftpOther);
    public static File configFile = new File("C:\\XMLValidator\\config.txt");
    public static File logFile = new File("C:\\XMLValidator\\validationLog.txt");
    public static ObservableList<String> environmentList = FXCollections.observableArrayList();
    public static ObservableList<String> ftpBaseFolderList = FXCollections.observableArrayList("fcs_nsi", "fcs_fas", "fcs_rules", "fcs_regions");
    public static Map<String, String> environments = new HashMap<>();
    public static String uploadDateFrom = "";


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

    public static void createConfigFile() {

        try {
            if (!SimpleXMLValidator.configFile.exists()) {
                PrintWriter writer = new PrintWriter(SimpleXMLValidator.configFile, "UTF-8");
                writer.println(";?????? ???????????? ????????, ?? ???????? ?????????? ?????????????????? ?????????????????????? ??????????????????(Environment) ?????????????? ?????????? ???????????????? ?????? ???????????? ...");
                writer.println(";???????????? \" ; \" ?????????? ?? ?????????????? ?????????????????????? ???????????? ... ");
                writer.println(";?????????????????? ???????????? ???????? ???????????? ?????????????????? ???????????? ?????????? \" : \" ?????????????????? ...");
                writer.println(";????\" : \" ?????????????????? ???????????????? ???????????????? ?????????????????? ?? ???????????????????? ????????????.");
                writer.println(";??????????\" : \" ?????????????????? ???????????????? ?????????? ?????????????????? ???? ???????????????? ?????????? ???????????????????????? ?????????????????????? ???? ??????.");
                writer.println(";???????????? \"EIS1:eis.lanit.ru\" ");
                writer.println(";???????????????? ???????? ?????????????????? ???????? ????????????????. ");
                writer.println(";?????????? ???????? ?????????????????? ????????, ??????????????????????, ?????????????????????????? ???????????????????? ?? ?????????????????? ???????? ??????????????. ");
                writer.println(";_________________________________");
                writer.println(";?????????????????????? ???????????? ?????? ???????? ?????????????????? ?????????? Other.");
                writer.println(";username:free");
                writer.println(";password:free");
                writer.println(";_________________________________");
                writer.println("EIS3:eis3.lanit.ru");
                writer.println("EIS4:eis4.roskazna.ru");
                writer.println("EIS5:eis5.roskazna.ru");
                writer.println("EIS6:192.168.232.17");
                writer.println("EIS7:eis7.lanit.ru");
                writer.println("PAK:ftp.zakupki.gov.ru");
                writer.close();
            }

        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
            ValidatorController.pathForSchema = selectedFile.getAbsolutePath();
        }
    }

    public static void stageFile(Stage s) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "XML files (*.xml;*.zip)", "*.xml", "*.zip"));// ,"*.7z","*.rar"));
        //if (pathToSelectedFiles != null) {
        if (xmlFile != null) {
            fileChooser.setInitialDirectory(new File(xmlFile.getParent()));
        }
        //???????????? ???????????????????? ???? ??????. ???? ?????? ?????????????????????? ???????? ??????...
        if (selectedEnvironment.equals("Other")) {
            fileChooser.setInitialDirectory(ftpURL);
        }
        pathForFiles = fileChooser.showOpenMultipleDialog(s);
        if (pathForFiles != null) {
            for (File pathForFile : pathForFiles) {
                if (pathForFile != null) {
                    xmlFile = new File(pathForFile.getAbsolutePath());
                }
            }
        }
    }

    public static void setDefaultLoginData() throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":", 2);
            if (parts.length >= 2 && line.contains("username")) {
                username = parts[1];
            } else if (parts.length >= 2 && line.contains("password")) {
                password = parts[1];
            }
        }
    }

    public static void initialEnvironments() throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(configFile));

        if (environments.isEmpty()) {
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length >= 2 && !line.contains(";")) {
                    String key = parts[0];
                    String value = parts[1];
                    environments.put(key, value);
                    System.out.println("Add \"" + key + " = " + value + "\" to the Environment choice box.");
                    environmentList.add(key);
                }
            }
            environments.put("Other", ftpOther); //Other
            environmentList.add("Other");
        }
        if (selectedEnvironment.equals("Other")) {
            //environmentList.remove("Other");
            environments.put("Other", ftpOther); //Other
        }
        System.out.println(environments);
    }

    public static void ftpClient() throws IOException {
        System.out.println("\n" + "Connect... to FTP and Downloading files ... " + "\n");
        FTPClient ftpClient = new FTPClient();
        String env = null;
        for (String key : environments.keySet()) {
            if (key.equals(selectedEnvironment)) {
                env = environments.get(key);
            }
        }
        System.out.println("Connect... to " + env);
        ftpClient.connect(env);
        //???????????????? ?????????????????? ???????????????????? ?? ???????????????????? ?? ??????.
        //tpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        System.out.println("Connected.\nLogin...");
        ftpClient.login(username, password);
        System.out.println("Logged.");
        ftpClient.enterLocalPassiveMode();
        ftpClient.type(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("UTF-8");

        String baseFolder = "";
        Map<String, String> baseFolders = new HashMap<>();
        baseFolders.put("fcs_nsi", "fcs_nsi/");
        baseFolders.put("fcs_fas", "fcs_fas/");
        baseFolders.put("fcs_rules", "fcs_rules/");
        baseFolders.put("fcs_regions", "fcs_regions/");
        for (String key : baseFolders.keySet()) {
            if (key.equals(ftpBaseFolder)) {
                baseFolder = baseFolders.get(key);
            }

            if (selectedEnvironment.equals("Other")) {
                baseFolder = otherFTPManualDir;
                manualDir = "";

                if (baseFolder.startsWith("/")) {
                    baseFolder = baseFolder.substring(1);
                }
            }

        }
        FTPFile[] dirs = ftpClient.listDirectories(baseFolder + manualDir);

        if (!selectedEnvironment.equals("Other") && !(dirs.length == 0)) {
            for (FTPFile dir : dirs) {
                System.out.println("Dir name - " + dir.getName() + "\n");
                ftpFileLoader(ftpClient, baseFolder + manualDir + "/" + dir.getName(), tempFiles);
            }
        }
        if (selectedEnvironment.equals("Other") && !(dirs.length == 0)) {
            for (FTPFile dir : dirs) {
                System.out.println("Dir name - " + dir.getName() + "\n");
                ftpFileLoader(ftpClient, baseFolder + "/" + dir.getName(), tempFiles);
            }

        }
        if (dirs.length == 0) {
            System.out.println("Dir name - " + baseFolder + "\n");
            ftpFileLoader(ftpClient, baseFolder  + manualDir, tempFiles);
        } else {
            System.out.println(ValidatorController.consoleToArea = "Is not match required conditions in the method 'ftpClient'.");
            return;
        }

        ftpClient.logout();
        System.out.println("Logout.");
        ftpClient.disconnect();
    }

    public static void selectFilesFromDir(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        System.out.println("\n" + "Select files from " + folder + " ... \n");

        if (folder.isDirectory() & listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    System.out.println(file.getName() + " - is selected.");
                }
            }
            pathForFiles = Arrays.asList(listOfFiles);
        }
        if (!folder.isDirectory()) {
            xmlFile = folder;
        }
        assert listOfFiles != null;
        if (listOfFiles.length == 0){
            System.out.println(ValidatorController.consoleToArea = "Temp folder is empty!");
        }
    }

    private static void ftpFileLoader(FTPClient ftpClient, String remotePath, String localPath) throws IOException {

        if (!Files.exists(Paths.get(localPath))) {
            Files.createDirectories(Paths.get(localPath));
        }
        System.out.println("Downloading folder " + remotePath + " to " + localPath);
        FTPFile[] remoteFiles = ftpClient.listFiles(remotePath);
        for (FTPFile remoteFile : remoteFiles) {
            if (!remoteFile.getName().equals(".") && !remoteFile.getName().equals("..")) {
                String remoteFilePath = remotePath + "/" + remoteFile.getName();
                long emptyFile = remoteFile.getSize();
                if (remoteFile.isDirectory()) {
                    ftpFileLoader(ftpClient, remoteFilePath, localPath);
                }
                String localFilePath = localPath + "/" + remoteFile.getName();
                if (!uploadDateFrom.equals("")) {
                    Calendar fileCreationData = remoteFile.getTimestamp();
                    String dayOfMoth = String.valueOf(fileCreationData.get(Calendar.DAY_OF_MONTH));
                    int calendarMonth = fileCreationData.get(Calendar.MONTH);
                    int increaseMoth = calendarMonth + 1;
                    String month = String.valueOf(increaseMoth);
                    String year = String.valueOf(fileCreationData.get(Calendar.YEAR));
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                    try {
                        Date fileDate = formatter.parse(dayOfMoth + "." + month + "." + year);
                        Date customerDate = formatter.parse(uploadDateFrom);
                        if (emptyFile > 22 && fileDate.after(customerDate)) {
                            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePath));
                            ftpClient.retrieveFile(remoteFilePath, outputStream);
                            System.out.println("File : " + remoteFilePath + " - is loaded. \n");
                            outputStream.close();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        System.out.println("Invalid date format :" + e);
                        return;
                    }
                }
                if (emptyFile >= 22 && uploadDateFrom.equals("")) {
                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePath));
                    ftpClient.retrieveFile(remoteFilePath, outputStream);
                    System.out.println("File : " + remoteFilePath + " - is loaded. \n");
                    outputStream.close();
                }
            }
        }
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
                    System.out.println("XML ???????? (" + fileName + ") ????????????????.");
                    copyFileUsingStream(new File(replacedFilePath), new File(tempFiles + fileName.getName()));
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            //Delete zip file
            Files.delete(Paths.get(zipFilePath));
        } catch (IllegalArgumentException e) {
            System.out.println("Encoding error in to the Zip file - " + e);
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

    static void deleteAllFilesWithDirs(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteAllFilesWithDirs(f);
            }
        }
        file.delete();
    }
}