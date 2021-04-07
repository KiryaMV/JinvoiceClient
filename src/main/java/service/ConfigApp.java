package service;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ConfigApp {
    private Properties props=new Properties();

    public  ConfigApp() {
        try {
            File jarPath=new File(ConfigApp.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            String propertiesPath=jarPath.getParentFile().getAbsolutePath();
            System.out.println(" propertiesPath-"+propertiesPath);
            props.load(new FileInputStream(propertiesPath+"/app.properties"));
        //    InputStream is = getClass().getClassLoader().getResourceAsStream("app.properties");
        //    props.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPropValue(final String aProp){
        String value="";
        if (!aProp.equals("")){
            value=props.getProperty(aProp);
        }
        return value;
    }

}
