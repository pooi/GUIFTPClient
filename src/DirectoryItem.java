import javax.swing.*;

public class DirectoryItem{

    public final static String TYPE_FOLDER = "folder";
    public final static String TYPE_TEXT = "text";

    private String title, type, imgPath;

    private ImageIcon image;

    public DirectoryItem(String content){

        String[] directory = content.split("\\.");

        this.type = "";
        if(directory.length > 1){
            this.type = directory[directory.length-1];
        }
        title = content;

        switch (type){
            case "txt":
                this.type = TYPE_TEXT;
                break;
            default:
                this.type = TYPE_FOLDER;
                break;
        }

    }

    public ImageIcon getImage() {
        if (image == null) {
            image = new ImageIcon(getImgPath());
        }
        return image;
    }

    public String getTitle(){
        return title;
    }

    public String getImgPath(){
        switch (type){
            case TYPE_FOLDER:
                return "folder.png";
            case TYPE_TEXT:
                return "txt.png";
            default:
                return "txt.png";
        }
    }

}