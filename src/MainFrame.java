import sun.applet.Main;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private BorderLayout bl;
    private JPanel inputPanel;
    private JTextField hostField;
    private JTextField idField;
    private JPasswordField pwField;
    private JButton connectBtn;

    private JList<String> directoryList;
    private DirectoryItem directoryItems[] = {
            new DirectoryItem("abc.txt"),
            new DirectoryItem("bddd.txt"),
            new DirectoryItem("secse"),
            new DirectoryItem("awgdeg")
    };
//    private String[] directories = {"test", "test1", "test2", "test3", "test4"};

    private JTextArea msgField;
    private String msgs = "";

    public MainFrame(){

        Dimension mainSize = new Dimension(1000, 600);
        this.setSize(mainSize);


        //swing에만 있는 X버튼 클릭시 종료
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        bl = new BorderLayout();
        this.setLayout(bl);

        {
            inputPanel = new JPanel();
            inputPanel.setSize(mainSize.width, (int)Math.max(mainSize.getHeight()*0.05, 30));

            hostField = new JTextField();
            hostField.setPreferredSize(new Dimension(300, inputPanel.getSize().height));
            idField = new JTextField();
            idField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
            pwField = new JPasswordField();
            pwField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
            connectBtn = new JButton("Connect");
            connectBtn.setPreferredSize(new Dimension(100, inputPanel.getSize().height));

            inputPanel.add(new JLabel("Host:"));
            inputPanel.add(hostField);
            inputPanel.add(new JLabel("ID:"));
            inputPanel.add(idField);
            inputPanel.add(new JLabel("PW:"));
            inputPanel.add(pwField);
            inputPanel.add(connectBtn);

            this.add(inputPanel, BorderLayout.NORTH);
        }

        {
            Dimension listSize = new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.7, 200));
            directoryList = new JList(directoryItems);
            directoryList.setPreferredSize(listSize);
            directoryList.setCellRenderer(new DirectoryCellRenderer());
            directoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setPreferredSize(listSize);
            scrollPane.setViewportView(directoryList);

            this.add(scrollPane, BorderLayout.CENTER);
        }

        {
            msgField = new JTextArea();
            msgField.setPreferredSize(new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.25, 100)));

            JScrollPane scroll = new JScrollPane (msgField);
            this.add(scroll, BorderLayout.SOUTH);
        }

        //프레임 보이기
        this.setVisible(true);

        new Thread(){
            @Override
            public void run(){
                int count = 0;
                while(true) {
                    try {
                        Thread.sleep(500);
                        msgField.insert(count + "\n", 0);
                        count += 1;
//                    msgField.setText(msgs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

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
