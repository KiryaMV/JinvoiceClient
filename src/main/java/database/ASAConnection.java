package database;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import service.ConfigApp;
import com.sybase.jdbcx.SybDriver;

public class ASAConnection {
    static final Logger logger = LogManager.getLogger(ASAConnection.class.getName());
    private Connection connection;

    public ASAConnection(){
        ConfigApp cfg = new ConfigApp();
        SybDriver sybDriver;
        try {
            sybDriver = (SybDriver) Class.forName("com.sybase.jdbc3.jdbc.SybDriver").newInstance();
            DriverManager.registerDriver(sybDriver);
            connection=DriverManager.getConnection("jdbc:sybase:Tds:"+cfg.getPropValue("host")+"/"+cfg.getPropValue("db")+"",
                    cfg.getPropValue("user"), cfg.getPropValue("pwd"));
            logger.info("DB="+cfg.getPropValue("db")+" connected");
        } catch (Exception e) {
            logger.error("DB="+cfg.getPropValue("db")+" NOT connected");
            e.printStackTrace();
        }
    }
    public Connection getConnection() {
       return connection;
    }
}
