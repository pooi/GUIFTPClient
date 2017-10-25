import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPManager {

    private JTextArea msgField;
    private JList<DirectoryItem> serverDirectoryList;
    private DirectoryItem[] serverDirectoryItems;
    private JList<DirectoryItem> clientDirectoryList;
    private DirectoryItem[] clientDirectoryItems;

    private Socket socket = new Socket();
    private BufferedReader ftpIn;
    private PrintWriter printWriter;

    public FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList) {
        this.serverDirectoryList = serverDirectoryList;
        this.serverDirectoryItems = new DirectoryItem[0];
        this.clientDirectoryList = clientDirectoryList;
        this.clientDirectoryItems = new DirectoryItem[0];
        getClientDirectoryList();
    }

    public FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, JTextArea field) {
        this(serverDirectoryList, clientDirectoryList);
        this.msgField = field;
    }

    public void addTextToMsgField(String text) {
        if (msgField != null) {
            msgField.insert(text + "\n", 0);
        }
    }

    private void getClientDirectoryList() {

    }

    public void connectFTPServer(String host, String id, String pw, String port) {

        new Thread() {
            @Override
            public void run() {
                try {
                    if (socket.isConnected()) {
                        socket.close();
                    }


                    int portNumber = 21;
                    try {
                        portNumber = (port == null || "".equals(port)) ? (21) : (Integer.parseInt(port));
                    } catch (Exception e) {
                        addTextToMsgField("Port number is not integer!");
                        return;
                    }

                    // socket connection
                    try {
                        socket = new Socket(host, portNumber);
                    } catch (Exception e) {
                        addTextToMsgField("Failed to connect server!");
                        return;
                    }

                    // Initialize
                    try {
                        ftpIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        printWriter = new PrintWriter(socket.getOutputStream());
//                        socket.setSoTimeout(3000);
//                        send("OPEN " + host + " " + portNumber);
//                        handleMultiLineResponse();
//                        send("PORT " + portNumber);
                    } catch (IOException io) {
                        addTextToMsgField("Control connection I/O error, closing control connection.");
                        socket.close();
                        ftpIn.close();
                        printWriter.close();
                        return;
                    }

                    handleMultiLineResponse();

                    loginToServer(id, pw);


                } catch (Exception e) {
                    addTextToMsgField("Control connection I/O error, closing control connection.");
                }

            }

        }.start();


    }

    private void loginToServer(String id, String pw) {

        String userName = "USER " + id;
        send(userName);
        String resultCode = handleMultiLineResponse();

        if(resultCode.startsWith("331 ")) {
            String userPassword = "PASS " + pw;//removeNewLine(new String(cmdString, "UTF-8"));
            send(userPassword, true);
            resultCode = handleMultiLineResponse();
        }

//        send(userPassword, true);
//         resultCode = handleMultiLineResponse();

        if (resultCode.startsWith("230")) {
            getDirectoryList();
        } else {

        }

    }

    private void getDirectoryList() {
        new Thread() {
            @Override
            public void run() {

                try {
                    if (!socket.isClosed() && socket.isConnected()) {
                        send("PASV");
                        String result = ftpIn.readLine();
                        if (result.startsWith("530 ")) {
                            addTextToMsgField("<-- Supplied command not expected at this time.");
                            return;
                        } else {
                            addTextToMsgField("<-- " + result);
                        }

                        String[] results = result.split("\\(");
                        String ip = getIp(results[1]);
                        int port = getPortNum(results[1]);

                        Socket dataConnection = new Socket();
                        try {
                            dataConnection = new Socket(ip, port);
                            if (ftpIn.ready()) {
                                String code = ftpIn.readLine();
                                if (code.startsWith("425 ")) {
                                    addTextToMsgField("Data transfer connection failed to open.");
                                    return;
                                }
                            }

                            send("LIST");
                            handleResponse();

                            BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
                            ArrayList<DirectoryItem> list = new ArrayList<>();
                            String line;
                            while ((line = dataIn.readLine()) != null) {
//                                System.out.println(line);
                                String[] strs = line.split(" ");
//                                System.out.println(strs[strs.length-1]);

                                list.add(new DirectoryItem(strs[strs.length - 1], strs[0]));
                            }
                            dataConnection.close();
                            dataIn.close();

                            handleResponse();

                            serverDirectoryItems = changeArrayListToStrings(list);
                            serverDirectoryList.setListData(serverDirectoryItems);

                        } catch (IOException io) {
                            addTextToMsgField("Data transfer connection I/O error, closing data connection.");
                            dataConnection.close();
                        } catch (Exception e) {
                            addTextToMsgField("Data transfer connection failed to open.");
                        }

                    }
                } catch (IOException io) {
                    addTextToMsgField("Data transfer connection I/O error, closing data connection.");
                } catch (Exception e) {
                    addTextToMsgField("Data transfer connection failed to open.\"");
                }
            }

        }.start();
    }

    private DirectoryItem[] changeArrayListToStrings(ArrayList<DirectoryItem> list) {
        DirectoryItem[] directoryItems = new DirectoryItem[list.size() + 1];
        directoryItems[0] = DirectoryItem.getPreDirectory();
        for (int i = 0; i < list.size(); i++) {
            directoryItems[i + 1] = list.get(i);
        }

        return directoryItems;
    }

    public void selectListItem(int index) {

        new Thread() {
            @Override
            public void run() {

                DirectoryItem item = serverDirectoryItems[index];

                if (DirectoryItem.TYPE_FOLDER.equals(item.getType())) {
                    enterDirectory(item.getTitle());
                } else {
                    downloadFile(item.getTitle());
                }

            }
        }.start();

    }

    private void enterDirectory(String directory) {

        try {
            if (!socket.isClosed() && socket.isConnected()) {
                send("CWD " + directory);
                String response = ftpIn.readLine();
                if (response.startsWith("530 ")) {
                    addTextToMsgField("<-- Supplied command not expected at this time.");
                    return;
                } else if(response.startsWith("550 ")){
                    addTextToMsgField("<-- " + response);
                    return;
                } else {
                    addTextToMsgField("<-- " + response);
                }

                getDirectoryList();
            } else {
                addTextToMsgField("Supplied command not expected at this time.");
            }

        } catch (Exception e) {
            addTextToMsgField("Supplied command not expected at this time.");
        }

    }

    private void downloadFile(String pathName) {

        try {

            if (!socket.isClosed() && socket.isConnected()) {

                send("TYPE I");
                String response = ftpIn.readLine();

                if (response.startsWith("530 ")) {
                    addTextToMsgField("<-- Supplied command not expected at this time.");
                    return;
                } else
                    addTextToMsgField("<-- " + response);

                send("SIZE " + pathName);
                String result = ftpIn.readLine();
                if (result.startsWith("550 ")) {
                    addTextToMsgField("<-- Access to local file " + pathName + " denied");
                    return;
                }
                addTextToMsgField("<-- " + result);

                String res[] = result.split(" ");
                int size = Integer.parseInt(res[1]);

                send("PASV");
                result = printAndReturnLastResponse();

                String[] results = result.split("\\(");
                String Ip = getIp(results[1]);
                int portNum = getPortNum(results[1]);

                Socket dataConnection = new Socket();
                try {
                    dataConnection = new Socket(Ip, portNum);

                    if (ftpIn.ready()) {
                        String code = ftpIn.readLine();
                        if (code.startsWith("425 ")) {
                            addTextToMsgField("<-- Data transfer connection to " + Ip + " on port " + portNum + " failed to open.");
                            return;
                        }
                    }

                    BufferedInputStream dataIn = new BufferedInputStream(dataConnection.getInputStream());

                    send("RETR " + pathName);
                    response = ftpIn.readLine();
                    if (response.startsWith("450 ")) {
                        addTextToMsgField("<-- Access to local file " + pathName + " denied.");
                        return;
                    } else
                        addTextToMsgField("<-- " + response);

                    byte readIn[] = new byte[size];
                    int read;
                    int offset = 0;
                    addTextToMsgField("");
                    while ((read = dataIn.read(readIn, offset, readIn.length - offset)) != -1) {
                        offset += read;
                        msgField.insert("<-- Download : " + offset / size * 100 + "%", 0);
                        if (readIn.length - offset == 0) {
                            break;
                        }
                    }

                    //dataIn.read(readIn, 0, size);

                    try {
                        File file = new File(pathName);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(readIn);
                        fos.close();
                    } catch (IOException io) {
                        addTextToMsgField("Unable to write into file");
                    }

                    dataConnection.close();
                    dataIn.close();
                    handleMultiLineResponse();

                } catch (IOException io) {
                    addTextToMsgField("Data transfer connection I/O error, closing data connection.");
                    dataConnection.close();
                } catch (IllegalArgumentException e) {
                    addTextToMsgField("830 Data transfer connection to " + Ip + " on port " + portNum + " failed to open.");
                }

            } else {
                addTextToMsgField("Supplied command not expected at this time.");
            }

        } catch (Exception e) {
            addTextToMsgField("Supplied command not expected at this time.");
        }

    }

    private String getIp(String input) {
        String values[] = new String[10];

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        int i = 0;
        while (matcher.find()) {
            values[i] = (matcher.group());
            i++;
        }
        return values[0] + "." + values[1] + "." + values[2] + "." + values[3];
    }

    private int getPortNum(String input) {
        int portNum;

        String values[] = new String[10];

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        int i = 0;
        while (matcher.find()) {
            values[i] = (matcher.group());
            i++;
        }
        portNum = Integer.parseInt(values[4]) * 256 + Integer.parseInt(values[5]);
        return portNum;
    }

    private String handleMultiLineResponse() {
        try {
            String result;
            while (!(result = ftpIn.readLine()).matches("\\d\\d\\d\\s.*")) {
                addTextToMsgField(result);
            }
            addTextToMsgField("<-- " + result);

            return result;
        } catch (IOException e) {
            addTextToMsgField("Control connection I/O error, closing control connection");
            try {
                socket.close();
                ftpIn.close();
                printWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return "";
        }
    }

    private void handleResponse() {
        try {
            String result = ftpIn.readLine();

            result = result.replaceFirst("-", " ");
            String results[] = result.split(" ");
            int errorCode = Integer.parseInt(results[0]);
            switch (errorCode) {
                case 503:
                    result = "Invalid Argument";
                case 501:
                    result = "Invalid Argument";
            }
            addTextToMsgField("<-- " + result);


        } catch (IOException e) {
            addTextToMsgField("Control connection I/O error, closing control connection");
            try {
                socket.close();
                ftpIn.close();
                printWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void send(String command) {

        printWriter.print(command + "\r\n");
        printWriter.flush();

        addTextToMsgField("--> " + command);
    }

    private void send(String command, boolean hideContent) {

        printWriter.print(command + "\r\n");
        printWriter.flush();


        String msg = command;
        if (hideContent) {
            msg = "";
            for (int i = 0; i < command.length(); i++) {
                msg += "*";
            }
        }

        addTextToMsgField("--> " + msg);
    }

    private String printAndReturnLastResponse() {
        String result = "";
        try {

            while (!(result = ftpIn.readLine()).matches("\\d\\d\\d\\s.*")) {
                System.out.println("<-- " + result);
            }
            System.out.println("<-- " + result);

        } catch (IOException e) {
            System.out.println("825 Control connection I/O error, closing control connection");
            try {
                socket.close();
                ftpIn.close();
                printWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return result;
    }
}
