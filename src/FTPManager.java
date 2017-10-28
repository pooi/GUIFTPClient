import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPManager {

    private JTextPane msgField; // 서버 및 클라이언트가 통신 중 주고 받는 메시지가 기록됨
    private JList<DirectoryItem> serverDirectoryList; // 서버 디렉토리에 있는 파일 및 폴더 목록을 화면에 띄우는 객체
    private DirectoryItem[] serverDirectoryItems; // 서버 디렉토리에 있는 파일 및 폴더 목록이 저장됨(파일명,폴더명,확장자 등)

    //client
    private JList<DirectoryItem> clientDirectoryList; // 클라이언트 디렉토리에 있는 파일 및 폴더 목록을 화면에 띄우는 객체
    private DirectoryItem[] clientDirectoryItems; // 클라이언트 디렉토리에 있는 파일 및 폴더 목록이 저장됨(파일명,폴더명,확장자 등)
    private String clientDirPath; // 클라이언트의 현재 디렉토리 경로가 저장됨

    private Socket socket = new Socket(); // 서버와의 연결을 위한 소켓
    private BufferedReader ftpIn; // 소켓으로 부터 전달 받은 메시지가 저장됨
    private PrintWriter printWriter; // 서버로 명령어를 전달할 때 사용됨

    /*
    * FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, String initPath)
    * JList<DirectoryItem> serverDirectoryList : 서버 디렉토리가 표시될 객체를 전달받음
    * JList<DirectoryItem> clientDirectoryList : 클라이언트 디렉토리가 표시될 객체를 전달받음
    * String initPath : 클라이언트의 최초 경로를 전달받음
    */
    public FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, String initPath) {
        this.clientDirPath = initPath;
        this.serverDirectoryList = serverDirectoryList;
        this.serverDirectoryItems = new DirectoryItem[0];
        this.clientDirectoryList = clientDirectoryList;
        this.clientDirectoryItems = new DirectoryItem[0];
        getClientDirectoryList(); // 클라이언트 디렉토리 조회
    }

    /*
    * FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, String initPath, JTextPane field)
    * JTextPane field : 서버와 클라이언트 사이에 주고받는 메시지가 표시될 객체를 전달받음(Optional)
    */
    public FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, String initPath, JTextPane field) {
        this(serverDirectoryList, clientDirectoryList, initPath);
        this.msgField = field;
    }

    /*
    * void addTextToMsgField(String text)
    * String text : 기록할 메시지
    * msgField가 null이 아니라면 msgField에 새로운 메시지(text)를 추가함
    * return : void
     */
    public void addTextToMsgField(String text) {
        if (msgField != null) { // 메시지 필드가 존재한다면
            if(text.startsWith("<--")){ // 서버로 부터 받아오는 메시지일 경우
                appendToPane(msgField, text + "\n", new Color(240, 80, 80)); // 빨강색으로 표시
            }else if(text.startsWith("-->")){ // 클라이언트로 부터 나가는 메시지일 경우
                appendToPane(msgField, text + "\n", new Color(80, 80, 240)); // 파랑색으로 표시
            }else{ // 기타 메시지
                appendToPane(msgField, text + "\n", Color.black); // 검은색으로 표시
            }
            msgField.setCaretPosition(msgField.getDocument().getLength()); // 메시지 필드의 스크롤을 가장 아래로 내림
        }
    }

    /*
    * void appendToPane(JTextPane tp, String msg, Color c)
    * JTextPane tp : 메시지가 기록될 객체
    * String msg : 기록할 메시지 내용
    * Color c : 메시지 글자 색
    * return : void
     */
    private void appendToPane(JTextPane tp, String msg, Color c)
    {
        StyleContext sc = StyleContext.getDefaultStyleContext(); // Default Style 로드
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c); // 폰트색 설정

        int len = tp.getDocument().getLength(); // 메시지 길이
        tp.setCaretPosition(len); // 커서를 위치 시킴
        tp.setCharacterAttributes(aset, false); // 스타일 설정
        tp.replaceSelection(msg); // 메시지 필드에 메시지 추가
    }

    /*
    * void connectFTPServer(String host, String id, String pw, String port)
    * String host : host 주소를 입력받음
    * String id : 계정 아이디를 입력받음 (id가 null이라면 anonymous로 처리됨)
    * String pw : 계정 비밀번호를 입력받음
    * String port : 포트 번호를 입력받음 (default 21로 설정)
    * 입력받은 host, id, password, port number를 이용해 ftp 서버에 연결함
    * return : void
     */
    public void connectFTPServer(String host, String id, String pw, String port) {

        // 연결과 무관하게 UI 구성요소를 변경할 수 있게 스레드 사용
        new Thread() {
            @Override
            public void run() {
                try {
                    quitServer();
                    if (socket.isConnected()) { // 소켓이 이미 연결되어 있다면 연결을 끊음
                        socket.close();
                    }


                    // port number를 입력하지 않았다면 기본 포트인 21로 설정
                    int portNumber = 21;
                    try {
                        portNumber = (port == null || "".equals(port)) ? (21) : (Integer.parseInt(port));
                    } catch (Exception e) {
                        addTextToMsgField("Port number is not integer!");
                        return;
                    }

                    // Socket connection
                    try {
                        addTextToMsgField("--> Connect " + host + ":" + portNumber);
                        socket = new Socket(host, portNumber);
                    } catch (Exception e) {
                        addTextToMsgField("Failed to connect server!");
                        return;
                    }

                    // Initialize required basic variables
                    try {
                        ftpIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"));
                    } catch (IOException io) { // 연결에 실패할 경우 모든 변수의 연결을 끊음
                        addTextToMsgField("Control connection I/O error, closing control connection.");
                        socket.close();
                        ftpIn.close();
                        printWriter.close();
                        return;
                    }

                    handleMultiLineResponse(); // 서버로 부터의 응답을 처리함

                    loginToServer(id, pw); // 서버에 정상적으로 연결되었을시 로그인 진행


                } catch (Exception e) {
                    addTextToMsgField("Control connection I/O error, closing control connection.");
                }

            }

        }.start();


    }

    /*
    * void loginToServer(String id, String pw)
    * String id : 서버 연결에 필요한 계정 ID
    * String pw : 서버 연결에 필요한 계정 비밀번호
    * 입력받은 ID와 비밀번호를 이용해 서버에 로그인 함
    * ID가 빈값이라면 anonymous를 이용해 연결을 시도함
    * return : void
     */
    private void loginToServer(String id, String pw) {

        if (id == null || "".equals(id)) { // ID가 빈값이면 anonymous로 설정
            id = "anonymous";
        }

        String userName = "USER " + id; // 로그인을 위한 명령어 세팅
        send(userName); // 서버에 명령어 전달
        String resultCode = handleMultiLineResponse(); // 서버로부터 응답을 가져옴

        if (resultCode.startsWith("331 ")) { // 페스워드가 필요한 경우 페스워드를 삽입함
            String userPassword = "PASS " + pw; // 패스워드를 입력하는 명령어 세팅
            send(userPassword, true); // 명령어를 서버에 전달(true : 메시지 필드에 내용이 표시되지 않도록 함)
            resultCode = handleMultiLineResponse(); // 서버로부터의 결과값을 전달받음
        }

//        send(userPassword, true);
//         resultCode = handleMultiLineResponse();

        if (resultCode.startsWith("230")) { // 로그인이 정상적으로 완료되었을 경우
            send("SYST"); // 서버의 운영체제 종류를 알아내는 명령어 전송(의미 없음)
            String os = handleMultiLineResponse(); // 운영체제별로 별도의 처리를 대비한 만약을 위한 변수

            getServerDirectoryList(); // 연결된 서버의 디렉토리를 가져옴
        } else {

        }

    }

    /*
    * void getServerDirectoryList()
    * 서버의 현재 디렉토리에 있는 파일 및 폴더를 가져오고 화면에 표시함
    * return : void
     */
    private void getServerDirectoryList() {
        new Thread() {
            @Override
            public void run() {

                try {
                    if (!socket.isClosed() && socket.isConnected()) { // 서버와의 연결이 유지되고 있을 경우(끊기지 않은 경우)
                        send("PASV"); // 서버에 파일리스트 출력결과나 파일전송, 즉 데이터전송을 위해 패시브 모드로 설정하는 명령어 전송
                        String result = ftpIn.readLine(); // 서버로부터의 응답을 가져옴
                        if (result.startsWith("530 ")) {
                            addTextToMsgField("<-- Supplied command not expected at this time.");
                            return;
                        } else {
                            addTextToMsgField("<-- " + result);
                        }

                        // result 예시 : 227 Entering Passive Mode (90,130,70,73,96,238)
                        String[] results = result.split("\\("); // 아이피와 포트번호만을 사용하기 위해 result 분리
                        String ip = getIp(results[1]); // 결과값에서 IP 값을 파싱함
                        int port = getPortNum(results[1]); // 결과값에서 port number를 파싱함

                        Socket dataConnection = new Socket(); // 디렉토리 리스트를 가져오기 위한 위한 소켓 준비
                        try {
                            dataConnection = new Socket(ip, port); // 서버로 부터 전달받은 IP와 port number를 이용해 새로운 소켓 연결
                            if (ftpIn.ready()) {
                                String code = ftpIn.readLine();
                                if (code.startsWith("425 ")) {
                                    addTextToMsgField("Data transfer connection failed to open.");
                                    return;
                                }
                            }

//                            send("LIST");
                            send("NLST"); // 서버 디렉토리의 파일 및 폴더 명을 반환하는 명령어 서버에 전송
                            handleResponse(); // 응답 결과를 처리


                            BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataConnection.getInputStream(), "utf-8")); // 서버로부터 받은 데이터를 처리하기 위한 버퍼 준비
                            ArrayList<DirectoryItem> list = new ArrayList<>(); // 전달받은 디렉토리 정보가 저장될 리스트
                            String line;
                            while ((line = dataIn.readLine()) != null) {

//                                String[] strs = line.split(" ");

//                                list.add(new DirectoryItem(strs[strs.length - 1], strs[0]));

                                list.add(new DirectoryItem(line)); // 디렉토리 정보 저장


                            }

                            // 연결 종료
                            dataConnection.close();
                            dataIn.close();

                            handleResponse(); // 서버로부터의 응답결과 처리

                            serverDirectoryItems = changeArrayListToStrings(list); // JList에 삽입하기 위해 ArrayList에서 배열로 리스트 타입 변경
                            serverDirectoryList.setListData(serverDirectoryItems); // 서버 디렉토리를 표시하는 객체에 새로운 디렉토리 정보를 전달 및 화면에 표시

                        } catch (IOException io) { // 입출력 에러가 발생했을 경우
                            addTextToMsgField("Data transfer connection I/O error, closing data connection.");
                            dataConnection.close();
                        } catch (Exception e) { // 데이터 전송에 문제가 발생했을 경우
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

    /*
    * DirectoryItem[] changeArrayListToStrings(ArrayList<DirectoryItem> list)
    * ArrayList<DirectoryItem> list : 배열로 변환할 디렉토리 리스트
    * ArrayList로 구성된 디렉토리 목록을 배열의 형태로 변환함
    * 가장 처음위치에 이전 디렉토리로 돌아갈 수 있는 내용 추가
    * return : DirectoryItem 배열
     */
    private DirectoryItem[] changeArrayListToStrings(ArrayList<DirectoryItem> list) {
        DirectoryItem[] directoryItems = new DirectoryItem[list.size() + 1];
        directoryItems[0] = DirectoryItem.getPreDirectory(); // 이전 디렉토리로 갈 수 있는 내용 추가
        for (int i = 0; i < list.size(); i++) {
            directoryItems[i + 1] = list.get(i);
        }

        return directoryItems;
    }

    /*
    * void selectServerListItem(int index)
    * int index : 서버 디렉토리 목록에서 선택된 아이템의 index
    * 사용자가 폴더를 선택하면 해당 폴더로 진입하고, 파일을 선택하면 해당 파일을 다운받음
    * return : void
     */
    public void selectServerListItem(int index) {

        new Thread() {
            @Override
            public void run() {

                DirectoryItem item = serverDirectoryItems[index]; // 선택된 아이템 정보를 가져옴

                if (DirectoryItem.TYPE_FOLDER.equals(item.getType())) { // 폴더일경우 해당 폴더로 이동
                    enterDirectory(item.getTitle());
                } else { // 파일일 경우 파일 다운로드
                    downloadFile(item.getTitle());
                }

            }
        }.start();

    }

    /*
    * void enterDirectory(String directory)
    * String directory : 새롭게 이동할 디렉토리 경로를 받음
    * 전달받은 경로로 이동함
    * return : void
     */
    private void enterDirectory(String directory) {

        try {
            if (!socket.isClosed() && socket.isConnected()) {
                send("CWD " + directory); // 디렉토리로 진입할 수 있는 명령어 전송
                String response = ftpIn.readLine(); // 서버로부터 결과값을 전달 받음
                if (response.startsWith("250 ")) { // 성공적으로 진입했을 경우
                    addTextToMsgField("<-- " + response);
                } else {
                    addTextToMsgField("<-- " + response);
                    return;
                }

                getServerDirectoryList(); // 현재 서버 디렉토리 내용을 갱신함
            } else {
                addTextToMsgField("Supplied command not expected at this time.");
            }

        } catch (Exception e) {
            addTextToMsgField("Supplied command not expected at this time.");
        }

    }

    /*
    * void getClientDirectoryList()
    * 클라이언트의 디렉토리 목록을 가져옴
    * return : void
     */
    private void getClientDirectoryList() {
        new Thread() {
            @Override
            public void run() {
                File dir = new File(clientDirPath);
                File[] fileList = dir.listFiles();
                try{
                    ArrayList<DirectoryItem> list = new ArrayList<>();
                    for(int i=0; i<fileList.length; i++) {
                        File file = fileList[i];
                        String fileName = file.getName();
                        DirectoryItem Item = file.isDirectory() ? new DirectoryItem(fileName, "d") : new DirectoryItem(fileName);

                        list.add(Item);
                    }
                    clientDirectoryItems = changeArrayListToStrings(list);
                    clientDirectoryList.setListData(clientDirectoryItems);
                } catch (Exception e) {

                }
            }
        }.start();
    }

    /*
    * void enterClientDirectory(String directory)
    * String directory : 새롭게 진입할 디렉토리 경로
    * 전달받은 경로로 진입하고 화면에 표시함
    * return : void
     */
    private void enterClientDirectory(String directory) {
        try {
            File newPath = new File(this.clientDirPath + "/" + directory);
            this.clientDirPath = newPath.getCanonicalPath();
//            System.out.println(this.clientDirPath + directory + " " + this.clientDirPath);
        } catch (Exception e) {
            
        }
        getClientDirectoryList();
    }

    /*
    * void selectClientListItem(int index)
    * int index : 클라이언트 디렉토리 목록에서 선택된 아이템의 index
    * 사용자가 폴더를 선택하면 해당 폴더로 진입하고, 파일을 선택하면 해당 파일을 서버에 업로드함
    * return : void
     */
    public void selectClientListItem(int index) {
        new Thread() {
            @Override
            public void run() {
                DirectoryItem item = clientDirectoryItems[index];

                if (DirectoryItem.TYPE_FOLDER.equals(item.getType())) { // 폴더일 경우 해당 폴더로 진입
                    enterClientDirectory(item.getTitle());
                } else { // 파일일 경우 해당 파일을 업로드
                    uploadFile(item.getTitle());
                }

            }
        }.start();
    }

    /*
    * void downloadFile(String pathName)
    * String pathName : 다운받을 파일의 이름을 입력받음
    * 전달 받은 파일 이름을 이용해 해당 파일을 다운받음
    * return : void
     */
    private void downloadFile(String pathName) {

        try {

            if (!socket.isClosed() && socket.isConnected()) {

                send("TYPE I"); // 파일 전송을 위해 전송모드를 바이너리로 변경
                String response = ftpIn.readLine(); // 서버로부터의 응답을 가져옴

                if (response.startsWith("530 ")) { // 잘못된 응답일 경우
                    addTextToMsgField("<-- Supplied command not expected at this time.");
                    return;
                } else
                    addTextToMsgField("<-- " + response);

                send("SIZE " + pathName); // 파일의 크기를 전달받는 명령어 전송
                String result = ftpIn.readLine(); // 서버로 부터의 응답을 가져옴
                if (result.startsWith("550 ")) { // 파일에 권한이 없을 경우
                    addTextToMsgField("<-- Access to local file " + pathName + " denied");
                    return;
                }
                addTextToMsgField("<-- " + result);

                // result 예 : 213 1024
                String res[] = result.split(" "); // 파일 사이즈만 추출하기 위해 문자열을 나눔
                int size = Integer.parseInt(res[1]);

                send("PASV"); // 데이터 전송을 위해 FTP 연결을 패시브모드로 변경하는 명령어 전송
                result = printAndReturnLastResponse(); // 서버로부터의 응답을 전달받음

                String[] results = result.split("\\("); // IP와 Port number를 추출하기 위해 문자열을 나눔
                String Ip = getIp(results[1]); // IP 파싱
                int portNum = getPortNum(results[1]); // Port number 파싱

                Socket dataConnection = new Socket(); // 데이터 전송을 위한 소켓 준비
                try {
                    dataConnection = new Socket(Ip, portNum); // 서버로부터 전달받은 IP와 Port number를 이용해 새로운 연결 수립

                    if (ftpIn.ready()) { // 연결이 실패했을 경우
                        String code = ftpIn.readLine();
                        if (code.startsWith("425 ")) {
                            addTextToMsgField("<-- Data transfer connection to " + Ip + " on port " + portNum + " failed to open.");
                            return;
                        }
                    }

                    BufferedInputStream dataIn = new BufferedInputStream(dataConnection.getInputStream()); // 서버로부터 데이터를 전달받기 위한 버퍼 준비

                    send("RETR " + pathName); // 파일의 복사본을 전송하는 명령어 서버에 전송
                    response = ftpIn.readLine(); // 서버로부터의 응답 결과 전달 받음
                    if (response.startsWith("450 ")) { // 파일에 접근 권한이 없을 경우
                        addTextToMsgField("<-- Access to local file " + pathName + " denied.");
                        return;
                    } else
                        addTextToMsgField("<-- " + response);

                    byte readIn[] = new byte[size]; // 파일을 저장할 변수
                    int read;
                    int offset = 0;
                    while ((read = dataIn.read(readIn, offset, readIn.length - offset)) != -1) { // 서버로부터 파일을 가져옴
                        offset += read;
                        if (readIn.length - offset == 0) {
                            break;
                        }
                    }

                    //dataIn.read(readIn, 0, size);

                    try {
                        // 현재 클라이언트 경로에 다운받은 파일을 저장함
                        File file = new File(clientDirPath + "/" + pathName);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(readIn);
                        fos.close();
                    } catch (IOException io) {
                        addTextToMsgField("Unable to write into file");
                    }

                    dataConnection.close();
                    dataIn.close();
                    handleMultiLineResponse();

                    getClientDirectoryList(); // 클라이언트 디렉토리를 갱신함

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

    /*
    * void uploadFile(String fileName)
    * String fileName : 업로드할 파일의 이름
    * 전달받은 파일의 이름을 이용해 해당 파일을 서버에 업로드함
    * return : void
     */
    private void uploadFile(String fileName){

        try {
            File file = new File(fileName); // 업로드할 파일을 가져옴
            Socket dataConnection = new Socket(); // 서버와 통신할 소켓 준비
            String Ip = "";
            int portNum = 0;
            try {
                // 서버에 전송할 파일 준비
                FileInputStream fileIn = new FileInputStream(file);
                int fileSize = (int) file.length();
                byte content[] = new byte[fileSize];
                fileIn.read(content, 0, fileSize);

                send("TYPE I"); // 전송 모드를 바이너리로 변경하는 명령어 전송
                String response = ftpIn.readLine();
                if (response.startsWith("530 ")) {
                    addTextToMsgField("<-- Supplied command not expected at this time.");
                    return;
                } else
                    addTextToMsgField("<-- " + response);


                send("PASV"); // 데이터 전송을 위해 패시브모드로 변경하는 명령어 전송
                String result = printAndReturnLastResponse();

                String[] results = result.split("\\(");
                Ip = getIp(results[1]); // 전달받은 IP 파싱
                portNum = getPortNum(results[1]); // 전달받은 port number 파싱

                dataConnection = new Socket(Ip, portNum); // 전달받은 IP, port number를 이용해 새로운 소켓 연결 수립
                if (ftpIn.ready()) {
                    String code = ftpIn.readLine();
                    if (code.startsWith("425 ")) {
                        addTextToMsgField("<-- Data transfer connection to " + Ip + " on port " + portNum + " failed to open.");
                        return;
                    }
                }

                BufferedOutputStream dataOut = new BufferedOutputStream(dataConnection.getOutputStream()); // 서버로 데이터를 전송하기 위한 버퍼 준비

                send("STOR " + fileName); // 데이터 입력 및 서버 쪽 파일로 저장로 저장하는 명령어 전송
                handleMultiLineResponse(); // 서버로부터의 응답값을 처리함

                dataOut.write(content, 0, fileSize); // 파일을 전송함
                dataOut.flush();

                fileIn.close();
                dataOut.close();
                dataConnection.close();

                handleMultiLineResponse();
                
                getServerDirectoryList(); // 서버의 디렉토리를 갱신함

            } catch (FileNotFoundException e) {
                addTextToMsgField("Access to local file " + fileName + " denied.");
            } catch (IOException io) {
                addTextToMsgField("835 Data transfer connection I/O error, closing data connection.");
                dataConnection.close();
            } catch (IllegalArgumentException i) {
                addTextToMsgField("Data transfer connection to " + Ip + " on port " + portNum + " failed to open.");
            }
        }catch (Exception e){
            addTextToMsgField("Supplied command not expected at this time.");
        }

    }

    public void quitServer(){

        try {
            if(socket != null && socket.isConnected()) {
                send("QUIT");
                handleMultiLineResponse();
                socket.close();
                printWriter.close();
                ftpIn.close();
            }
        }catch (Exception e){
            addTextToMsgField("Supplied command not expected at this time.");
        }

    }

    /*
    * String getIp(String input)
    * String input : ip를 파싱할 문자열 입력
    * 000.000.000.000.000.000)에서 IP를 파싱함
    * return : IP Address
     */
    private String getIp(String input) {
        String values[] = new String[10];

        Pattern pattern = Pattern.compile("\\d+"); // 숫자로 구성된 문자열을 찾기 위한 패턴 설정
        Matcher matcher = pattern.matcher(input);
        int i = 0;
        while (matcher.find()) { // 일치하는 문자열을 찾고 배열에 삽입함
            values[i] = (matcher.group());
            i++;
        }
        return values[0] + "." + values[1] + "." + values[2] + "." + values[3]; // 앞에 4부분을 IP로 반환함
    }

    /*
    * int getPortNum(String input)
    * String input : Port number를 파싱할 문자열 입력
    * 000.000.000.000.000.000)에서 port number를 파싱함
    * return : Port Number
     */
    private int getPortNum(String input) {
        int portNum;

        String values[] = new String[10];

        Pattern pattern = Pattern.compile("\\d+"); // 숫자로 구성된 문자열을 찾기 위한 패턴 설정
        Matcher matcher = pattern.matcher(input);
        int i = 0;
        while (matcher.find()) { // 일치하는 문자열을 찾고 배열에 삽입함
            values[i] = (matcher.group());
            i++;
        }
        portNum = Integer.parseInt(values[4]) * 256 + Integer.parseInt(values[5]); // 마지막 2개의 숫자를 이용해 포트번호를 계산함
        return portNum;
    }

    /*
    * void send(String command)
    * String command : 서버로 전송할 명령어
    * 전달받은 명령어를 서버에 전송함
    * return : void
     */
    private void send(String command) {

        // 명령어 전송을 위해 "\r\n"을 추가하고 서버에 전송
        printWriter.print(command + "\r\n");
        printWriter.flush();

        addTextToMsgField("--> " + command); // 전달한 명령어를 메시지 필드에 표시
    }

    /*
    * void send(String command, boolean hideContent)
    * String command : 서버로 전송할 명령어
    * boolean hideContent : 메시지필드에 내용을 그대로 표시할지 말지를 선택
    * 전달받은 명령어를 서버에 전송함
    * return : void
     */
    private void send(String command, boolean hideContent) {

        printWriter.print(command + "\r\n");
        printWriter.flush();


        String msg = command;
        if (hideContent) { // true일 경우 메시지를 *로 치환하여 메시지 필드에 표시
            msg = "";
            for (int i = 0; i < command.length(); i++) {
                msg += "*";
            }
        }

        addTextToMsgField("--> " + msg);
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
