import javax.swing.*;

public class DirectoryItem{

    public final static String TYPE_FOLDER = "folder";
    public final static String TYPE_NONE = "none";
    public final static String TYPE_TEXT = "text";
    public final static String TYPE_CSS = "css";
    public final static String TYPE_DOC = "doc";
    public final static String TYPE_HTML = "html";
    public final static String TYPE_JS = "js";
    public final static String TYPE_JPG = "jpg";
    public final static String TYPE_PNG = "png";
    public final static String TYPE_PPT = "ppt";
    public final static String TYPE_ZIP = "zip";
    public final static String TYPE_EXE = "exe";
    public final static String TYPE_MP3 = "mp3";
    public final static String TYPE_PDF = "pdf";
    public final static String TYPE_PHP = "php";

    private String title, type, imgPath;

    private ImageIcon image;

    public DirectoryItem(){

    }

    public DirectoryItem(String content){
        this();

//        String[] directory = content.split("\\.");
        int lastDotpos = content.lastIndexOf( "." );

        title = content;
        if(lastDotpos < 0){

            this.type = TYPE_FOLDER;

        }else{
            String ext = content.substring(lastDotpos + 1);

            this.type = ext;

            switch (type.toLowerCase()){
                case "txt":
                    this.type = TYPE_TEXT;
                    break;
                case "css":
                    this.type = TYPE_CSS;
                    break;
                case "docx":
                case "doc":
                    this.type = TYPE_DOC;
                    break;
                case "html":
                    this.type = TYPE_HTML;
                    break;
                case "js":
                    this.type = TYPE_JS;
                    break;
                case "jpeg":
                case "jpg":
                    this.type = TYPE_JPG;
                    break;
                case "png":
                    this.type = TYPE_PNG;
                    break;
                case "pptx":
                case "ppt":
                    this.type = TYPE_PPT;
                    break;
                case "7z":
                case "zip":
                    this.type = TYPE_ZIP;
                    break;
                case "exe":
                    this.type = TYPE_EXE;
                    break;
                case "mp3":
                    this.type = TYPE_MP3;
                    break;
                case "pdf":
                    this.type = TYPE_PDF;
                    break;
                case "php":
                    this.type = TYPE_PHP;
                    break;
                default:
                    this.type = TYPE_NONE;
                    break;
            }
        }

    }

    public DirectoryItem(String content, String dirType){
        this(content);
        if(dirType.startsWith("d") || dirType.contains("<DIR>")){
            this.type = TYPE_FOLDER;
        }
    }

    public static DirectoryItem getPreDirectory(){
        DirectoryItem item = new DirectoryItem();
        item.type = TYPE_FOLDER;
        item.title = "../";
        return item;
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

    public String getType() {
        return type;
    }

    public String getImgPath(){
        switch (type){
            case TYPE_FOLDER:
                return "folder.png";
            case TYPE_TEXT:
                return "txt.png";
            case TYPE_CSS:
                return "css.png";
            case TYPE_DOC:
                return "doc.png";
            case TYPE_HTML:
                return "html.png";
            case TYPE_JS:
                return "javascript.png";
            case TYPE_JPG:
                return "jpg.png";
            case TYPE_PNG:
                return "png.png";
            case TYPE_PPT:
                return "ppt.png";
            case TYPE_ZIP:
                return "zip.png";
            case TYPE_EXE:
                return "exe.png";
            case TYPE_MP3:
                return " mp3.png";
            case TYPE_PDF:
                return "pdf.png";
            case TYPE_PHP:
                return "php.png";
            default:
                return "file.png";
        }
    }

}