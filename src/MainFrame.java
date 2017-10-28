import sun.applet.Main;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

public class MainFrame extends JFrame {

    private FTPManager ftpManager;

    private BorderLayout bl;
    private JPanel inputPanel;
    private JTextField hostField;
    private JTextField idField;
    private JPasswordField pwField;
    private JTextField portField;
    private JButton connectBtn;

    private JList<DirectoryItem> serverDirectoryList;
    private JList<DirectoryItem> clientDirectoryList;

    private JTextPane msgField;
    private String msgs = "";

    public MainFrame(){

        Dimension mainSize = new Dimension(1000, 800); // 스크린 기본 사이즈 설정
        this.setSize(mainSize);


        // X버튼 클릭시 종료 및 서버 연결 해제 등록
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ftpManager.quitServer();
            }
        });

        // 프로그램 메인 레이아웃 설정
        bl = new BorderLayout();
        this.setLayout(bl);

        { // host, id, pw, port#을 입력받는 부분 객체 설정
            inputPanel = new JPanel(); // host, id, pw, port#을 입력받는 panel 생성
            inputPanel.setSize(mainSize.width, (int)Math.max(mainSize.getHeight()*0.05, 30)); // 패널의 사이즈 설정

            hostField = new JTextField("speedtest.tele2.net");
            hostField.setPreferredSize(new Dimension(300, inputPanel.getSize().height));
            idField = new JTextField();
            idField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
            pwField = new JPasswordField();
            pwField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
            portField = new JTextField();
            portField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
            connectBtn = new JButton("Connect");
            connectBtn.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
            connectBtn.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // connect 버튼을 누르면 ftp 서버와 연결 시도
                    ftpManager.connectFTPServer(
                            hostField.getText().toString(),
                            idField.getText().toString(),
                            new String(pwField.getPassword()),
                            portField.getText().toString()
                            );
                }
            });

            // 패널에 생성한 객체 추가
            inputPanel.add(new JLabel("Host:"));
            inputPanel.add(hostField);
            inputPanel.add(new JLabel("ID:"));
            inputPanel.add(idField);
            inputPanel.add(new JLabel("PW:"));
            inputPanel.add(pwField);
            inputPanel.add(new JLabel("Port Number:"));
            inputPanel.add(portField);
            inputPanel.add(connectBtn);

            // 메인 프레임에 패널 추가
            this.add(inputPanel, BorderLayout.NORTH);
        }

        { // 클라이언트 및 서버 디렉토리가 표시되는 영역 생성
            Dimension listSize = new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.7, 200)); // 리스트의 사이즈 설정

            // 리스트가 추가될 패널 생성 및 레이아웃을 BorderLayout으로 설정
            JPanel directoryPanel = new JPanel();
            directoryPanel.setLayout(new BorderLayout());

            // server directory
            serverDirectoryList = new JList<DirectoryItem>(); // 서버 디렉토리 목록이 보여질 리스트 객체 생성
            serverDirectoryList.setCellRenderer(new DirectoryCellRenderer()); // 아이콘 및 파일 이름이 표시될 cell 레이아웃 설정
            serverDirectoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 하나만 선택되도록 설정
            serverDirectoryList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList)e.getSource();
                    if (e.getClickCount() == 2) { // 더블클릭시 진입

                        // Double-click detected
                        int index = list.locationToIndex(e.getPoint());
                        ftpManager.selectServerListItem(index); // 파일 또는 폴더에 맞는 동작을 하도록 함

                    }
                }
            });

            // 스크롤이 가능하게 리스트 객체를 스크롤 패널에 등록
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setPreferredSize(new Dimension(listSize.width/2, listSize.height));
            scrollPane.setViewportView(serverDirectoryList);

            // 패널의 오른쪽에 서버 디렉토리 객체 추가
            directoryPanel.add(scrollPane, BorderLayout.EAST);


            // client directory
            clientDirectoryList = new JList<DirectoryItem>(); // 클라이언트 디렉토리 목록이 보여질 리스트 객체 생성
            clientDirectoryList.setCellRenderer(new DirectoryCellRenderer());
            clientDirectoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            clientDirectoryList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList)e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    if (e.getClickCount() == 2) {
                        // Double-click detected
                        ftpManager.selectClientListItem(index);
                    }
                    
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        // Right-click
                        JPopupMenu popupMenu = new JPopupMenu();
                        popupMenu.add(new JMenuItem("menu1"));
                        popupMenu.add(new JMenuItem("menu2"));

                        popupMenu.show((Component)e.getSource(), e.getX(), e.getY());

                        list.setSelectedIndex(index);
                    }
                }
            });

            JScrollPane scrollPane2 = new JScrollPane();
            scrollPane2.setPreferredSize(new Dimension(listSize.width/2, listSize.height));
            scrollPane2.setViewportView(clientDirectoryList);

            // 패널의 왼쪽에 클라이언트 디렉토리 객체 추가
            directoryPanel.add(scrollPane2, BorderLayout.WEST);

            // 메인 프레임에 리스트 패널 추가
            this.add(directoryPanel, BorderLayout.CENTER);
        }

        {
            // 서버와 주고받은 메시지가 기록될 메시지 필드 생성
            msgField = new JTextPane();

            // 스크롤이 가능하도록 스크롤 패널에 메시지 필드 등록
            JScrollPane scroll = new JScrollPane ();
            scroll.setPreferredSize(new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.25, 100)));
            scroll.setViewportView(msgField);

            // 메인 프레임 아래쪽에 메시지 필드 추가
            this.add(scroll, BorderLayout.SOUTH);
        }

        //프레임 보이기
        this.setVisible(true);

        String initPath = new File(".").getAbsolutePath();
        ftpManager = new FTPManager(serverDirectoryList, clientDirectoryList, initPath, msgField);

    }

    public MainFrame(String title){
        this();
        this.setTitle(title);
    }


    class DirectoryCellRenderer extends JLabel implements ListCellRenderer {
        private final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

        public DirectoryCellRenderer() {
            setOpaque(true);
            setIconTextGap(12);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            DirectoryItem entry = (DirectoryItem) value;
            setText(entry.getTitle());
            setIcon(entry.getImage());
            if (isSelected) {
                setBackground(HIGHLIGHT_COLOR);
                setForeground(Color.white);
            } else {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            return this;
        }
    }

}
