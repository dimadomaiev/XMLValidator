package com.company;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.company.ValidatorController.consoleToArea;

public class SimpleXMLValidator extends Application {
    public static File schemaFile = null;
    public static File xmlFile = null;
    public static List<File> pathForFiles;
    public static File pathToSelectedFiles = null;
    public static String tempFiles = "C:\\XMLValidator\\tempFiles\\";
    public static String invalidFiles = "C:\\XMLValidator\\invalidFiles\\";
    public static String selectedEnvironment = null;
    public static String ftpBaseFolder = null;
    public static String ftpOther = null;
    public static String ftpOtherBaseFolder = null;
    public static String manualDir = "";

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
            ValidatorController.pathForSchema = selectedFile.getAbsolutePath();
        }
    }

    public static void stageFile(Stage s) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "XML files (*.xml;*.zip)", "*.xml", "*.zip"));// ,"*.7z","*.rar"));
        if (pathToSelectedFiles != null) {
            fileChooser.setInitialDirectory(new File(pathToSelectedFiles.getParent()));
        }
        pathForFiles = fileChooser.showOpenMultipleDialog(s);
        if (pathForFiles != null) {
            for (File pathForFile : pathForFiles) {
                if (pathForFile != null) {
                    pathToSelectedFiles = new File(pathForFile.getAbsolutePath());
                    xmlFile = pathToSelectedFiles;
                }
            }
        }
    }

    public static void ftpClient() throws IOException {
        System.out.println ("\n" + "Connect... to FTP and Downloading files ... " + "\n");
        long startTime = System.nanoTime();
        FTPClient ftpClient = new FTPClient();
        String env = null;
        Map<String, String> envs = new HashMap<>();
        envs.put("PAK", "ftp.zakupki.gov.ru");
        envs.put("EIS1", "eis.lanit.ru");
        envs.put("EIS2", "eis2.lanit.ru"); //eis2
        envs.put("EIS3", "eis3.lanit.ru"); //eis3
        envs.put("EIS4", "eis4.roskazna.ru"); //eis4
        envs.put("EIS5", "eis5.roskazna.ru"); //eis5
        envs.put("EIS6", "eis6.lanit.ru"); //eis6
        envs.put("EIS7", "eis7.lanit.ru"); //eis7
        envs.put("Other", ftpOther); //Other
        for (String key : envs.keySet()) {
            if (key.equals(selectedEnvironment)) {
                env = envs.get(key);
            }
        }

        System.out.println("Connect... to " + env);
        ftpClient.connect(env);
        System.out.println("Connected.\nLogin...");
        ftpClient.login("free", "free");
        System.out.println("Logged.");
        ftpClient.enterLocalPassiveMode();
        ftpClient.type(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("UTF-8");

        String baseFolder = null;
        Map<String, String> baseFolders = new HashMap<>();
        baseFolders.put("fcs_nsi", "fcs_nsi/");
        baseFolders.put("fcs_fas", "fcs_fas/");
        baseFolders.put("fcs_rules", "fcs_rules/");
        baseFolders.put("fcs_regions", "fcs_regions/");
        for (String key : baseFolders.keySet()) {
            if (key.equals(ftpBaseFolder)) {
                baseFolder = baseFolders.get(key);
            }
            if(!(ftpOtherBaseFolder == null)) {
                baseFolder = ftpOtherBaseFolder;
            }
        }

        FTPFile[] files;
        FTPFile[] dirs = ftpClient.listDirectories(baseFolder);
        //if (baseFolder == null) {
        //    dirs = ftpClient.listDirectories(ftpOtherBaseFolder);
        //    baseFolder = ftpOtherBaseFolder;
        //}

        assert baseFolder != null;
        String fasPrefix = (baseFolder.equals("fcs_fas/")) ? "/currMonth" : "";                                              //Определяет, если выбрана проверка ФАС/ОВК, то добавляет каталог текущего месяца
        //в подкаталог "currMonth".
        String remotePath;
        if (manualDir.isEmpty() & !selectedEnvironment.equals("Other")) {
            for (FTPFile dir : dirs) {
                System.out.println("Dir name - " + dir.getName() + "\n");
                remotePath = dir.getName();
                ftpFileLoader(ftpClient, baseFolder + remotePath, tempFiles);
            }
        }
        if (selectedEnvironment.equals("Other")) {
            for (FTPFile dir : dirs) {
                System.out.println("Dir name - " + dir.getName() + "\n");
                remotePath = dir.getName();
                ftpFileLoader(ftpClient, baseFolder + remotePath, tempFiles);
            }
        } else {
            files = ftpClient.listFiles(baseFolder + manualDir + fasPrefix);
            System.out.println("manualDir - " + manualDir + "\n");
            parseFTPFiles(ftpClient, manualDir, files, baseFolder, fasPrefix);
        }

        ftpClient.logout();
        ftpClient.disconnect();

        long endTime = System.nanoTime();
        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        System.out.println("Total elapsed time: " + elapsedTimeInMillis + " ms" + "\n");
    }

    static void parseFTPFiles(FTPClient ftpClient, String dir, FTPFile[] files, String baseFolder, String fasPrefix) throws IOException {
        for (FTPFile file : files) {
            System.out.println("Downloading - " + file.getName() + " " + fileSize(file.getSize()));
            //DownloadZipFile
            mkDir(new File(new File(tempFiles + file.getName()).getParent()));
            OutputStream output = new FileOutputStream(tempFiles + file.getName());
            ftpClient.retrieveFile(baseFolder + dir + fasPrefix + "/" + file.getName(), output);
            output.close();
        }
    }

    public static void selectTempFTPFiles() {
        System.out.println(consoleToArea = ("\n" + "Get loaded files ..." + "\n"));
        File folder = new File("C:\\XMLValidator\\tempFiles\\");
        File[] listOfFiles = folder.listFiles();

        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                System.out.println(file.getName());
            }
        }
        pathForFiles = Arrays.asList(listOfFiles);
    }
/*
    public static void saveFileIfInvalid(File files) throws IOException {
        if (!Files.exists(Paths.get(invalidFiles))) {
            Files.createDirectories(Paths.get(invalidFiles));
        }
        if (consoleToArea.contains("IS NOT VALID.")) {
            copyFileUsingStream(files, new File(invalidFiles + files.getName()));
            consoleToArea = ("Invalid file !!! SAVED !!! to directory: " + invalidFiles + "\n");
        }
        copyFileUsingStream(files, new File(tempFiles + files.getName()));

    }
*/
    private static void ftpFileLoader(FTPClient ftpClient, String remotePath, String localPath) throws IOException {
        if (!Files.exists(Paths.get(localPath))) {
            Files.createDirectories(Paths.get(localPath));
        }
        System.out.println("Downloading folder " + remotePath + " to " + localPath);

        FTPFile[] remoteFiles = ftpClient.listFiles(remotePath);

        for (FTPFile remoteFile : remoteFiles) {
            if (!remoteFile.getName().equals(".") && !remoteFile.getName().equals("..")) {
                String remoteFilePath = remotePath + "/" + remoteFile.getName();
                String localFilePath = localPath + "/" + remoteFile.getName();

                if (remoteFile.isDirectory()) {
                    new File(localFilePath).mkdirs();

                    ftpFileLoader(ftpClient, remoteFilePath, localFilePath);
                } else {
                    System.out.println("Downloading file " + remoteFilePath + " to " +
                            localFilePath);

                    OutputStream outputStream =
                            new BufferedOutputStream(new FileOutputStream(localFilePath));
                    if (!ftpClient.retrieveFile(remoteFilePath, outputStream)) {
                        System.out.println("Failed to download file " + remoteFilePath);
                    }
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
                    System.out.println("XML файл (" + fileName + ") извлечён.");
                    copyFileUsingStream(new File(replacedFilePath), new File(tempFiles + fileName.getName()));
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            //Delete zip files
            Files.delete(Paths.get(zipFilePath));
        } catch (IllegalArgumentException e) {
            consoleToArea = "Encoding error in to the Zip file - " + e;
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