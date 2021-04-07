package service;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipTool {

    private static String zipfolder;
    static final Logger logger = LogManager.getLogger(ZipTool.class.getName());

    public ZipTool(){
        zipfolder=new ConfigApp().getPropValue("mailfolder")+ "/out/";
    }

    public static List<File> doZip(List<File> aFiles)  {
        List<File> out=new ArrayList<>();
        String zipName=zipfolder+aFiles.get(0).getName().substring(2,aFiles.get(0).getName().indexOf(".dbf"));
        File res=new File(zipName + ".zip");
        FileOutputStream fop=null;
        try {
            fop=new FileOutputStream(res);
            ZipOutputStream zip = new ZipOutputStream(fop);
            for (File item:aFiles) {
                zip.putNextEntry(new ZipEntry(item.getName()));
                write(new FileInputStream(item),zip);
            }
            zip.close();
            out.add(res);
        }
        catch (Exception e){
            System.out.println(e.getMessage());

        }
        return out;
    }

    private static void write(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);
        in.close();
    }


}
