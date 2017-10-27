import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClientDirectoryList
{
    private JList<DirectoryItem> clientDirectoryList;
    private DirectoryItem[] clientDirectoryItems;
    private String dirPath;
    
    public ClientDirectoryList(String initPath, JList<DirectoryItem> clientDirectoryList) {
        this.dirPath = initPath;
        this.clientDirectoryList = clientDirectoryList;
        this.clientDirectoryItems = new DirectoryItem[0];

        this.clientDirectoryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList list = (JList)e.getSource();
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = list.locationToIndex(e.getPoint());
                    selectListItem(index);
                }
            }
        });

        this.getClientDirectoryList();
    }

    private void getClientDirectoryList() {
        new Thread() {
            @Override
            public void run() {
                File dir = new File(dirPath);
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

    private void enterDirectory(String directory) {
        String[] path = this.dirPath.split("/");

        if (directory == "../") {
            if (path.length <= 2) {
                this.dirPath = path[0] + "/";
            } else {
                path = Arrays.copyOf(path, path.length-1);
                this.dirPath = String.join("/", path);
            }
        } else {
            this.dirPath += path.length > 1 ? "/" + directory : directory;
        }
        System.out.println(directory + " " + dirPath);
        getClientDirectoryList();
    }

    private void selectListItem(int index) {
        new Thread() {
            @Override
            public void run() {
                DirectoryItem item = clientDirectoryItems[index];

                if (DirectoryItem.TYPE_FOLDER.equals(item.getType())) {
                    enterDirectory(item.getTitle());
                } else {
                    System.out.println("click! " + item.getTitle());
                    //downloadFile(item.getTitle());
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
}