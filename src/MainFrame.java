import sun.applet.Main;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

    private JTextArea msgField;
    private String msgs = "";

    public MainFrame(){

        Dimension mainSize = new Dimension(1000, 800);
        this.setSize(mainSize);


        //swing에만 있는 X버튼 클릭시 종료
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        bl = new BorderLayout();
        this.setLayout(bl);

        {
            inputPanel = new JPanel();
            inputPanel.setSize(mainSize.width, (int)Math.max(mainSize.getHeight()*0.05, 30));

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
                    ftpManager.connectFTPServer(
                            hostField.getText().toString(),
                            idField.getText().toString(),
                            new String(pwField.getPassword()),
                            portField.getText().toString()
                            );
                }
            });

            inputPanel.add(new JLabel("Host:"));
            inputPanel.add(hostField);
            inputPanel.add(new JLabel("ID:"));
            inputPanel.add(idField);
            inputPanel.add(new JLabel("PW:"));
            inputPanel.add(pwField);
            inputPanel.add(new JLabel("Port Number:"));
            inputPanel.add(portField);
            inputPanel.add(connectBtn);

            this.add(inputPanel, BorderLayout.NORTH);
        }

        {
            Dimension listSize = new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.7, 200));

            JPanel directoryPanel = new JPanel();
            directoryPanel.setLayout(new BorderLayout());

            // server directory
            serverDirectoryList = new JList<DirectoryItem>();
            serverDirectoryList.setCellRenderer(new DirectoryCellRenderer());
            serverDirectoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            serverDirectoryList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList)e.getSource();
                    if (e.getClickCount() == 2) {

                        // Double-click detected
                        int index = list.locationToIndex(e.getPoint());
                        ftpManager.selectListItem(index);
//                        ftpManager.addTextToMsgField(ftpManager.getDirectoryItems()[index].getTitle());

                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setPreferredSize(new Dimension(listSize.width/2, listSize.height));
            scrollPane.setViewportView(serverDirectoryList);

            directoryPanel.add(scrollPane, BorderLayout.EAST);


            // client directory
            clientDirectoryList = new JList<DirectoryItem>();
            clientDirectoryList.setCellRenderer(new DirectoryCellRenderer());
            clientDirectoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            clientDirectoryList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList)e.getSource();
                    if (e.getClickCount() == 2) {
                        // Double-click detected
                        int index = list.locationToIndex(e.getPoint());
                        ftpManager.selectClientListItem(index);

                    }
                }
            });

            JScrollPane scrollPane2 = new JScrollPane();
            scrollPane2.setPreferredSize(new Dimension(listSize.width/2, listSize.height));
            scrollPane2.setViewportView(clientDirectoryList);

            directoryPanel.add(scrollPane2, BorderLayout.WEST);


            this.add(directoryPanel, BorderLayout.CENTER);
        }

        {
            msgField = new JTextArea();

            JScrollPane scroll = new JScrollPane ();
            scroll.setPreferredSize(new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.25, 100)));
            scroll.setViewportView(msgField);

            this.add(scroll, BorderLayout.SOUTH);
        }

        //프레임 보이기
        this.setVisible(true);

        ftpManager = new FTPManager(serverDirectoryList, clientDirectoryList, "./", msgField);
//        new ClientDirectoryList("./", clientDirectoryList);

//        new Thread(){
//            @Override
//            public void run(){
//                int count = 0;
//                while(true) {
//                    try {
//                        Thread.sleep(500);
//                        msgField.insert(count + "\n", 0);
//                        count += 1;
////                    msgField.setText(msgs);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

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
