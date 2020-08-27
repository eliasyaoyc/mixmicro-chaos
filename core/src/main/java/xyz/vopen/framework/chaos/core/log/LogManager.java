package xyz.vopen.framework.chaos.core.log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import xyz.vopen.framework.chaos.core.internals.event.ChaosEventManager;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * {@link LogManager} The operation of the disk is realized based on MapDB
 *
 * @author <a href="mailto:siran0611@gmail.com">siran.yao</a>
 * @version ${project.version}
 * @date 2020/7/9
 */
public class LogManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogManager.class);

  public static final String CHAOS_METADATA = "chaos-metadata";

  private static final String dbDir = "chaos-metadata";

  private static final String db_SUFFIX = ".db";

  private static final String dbName = "events";

  private HTreeMap logs;
  private DB db;

  private ReentrantLock lock = new ReentrantLock();

  public LogManager(String logDir) {
    db = DBMaker.fileDB(logDir).make();
    logs = db.hashMap(dbName).createOrOpen();
  }

  public void write(List<LogEntry> logEntries) {
    for (LogEntry entry : logEntries) write(entry);
  }

  public void write(LogEntry logEntry) {
    if (db.isClosed()) {
      return;
    }
    try {
      lock.tryLock(3000, MILLISECONDS);
      String s = JSON.toJSONString(logEntry.getMetadata());
      logs.put(logEntry.getServiceName(), s);
      //      LOGGER.info("DefaultLogModule write MapDB success, logEntry info : [{}]", logEntry);
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage());
    } finally {
      lock.unlock();
    }
  }

  public LogEntry read(String key) {
    if (db.isClosed()) {
      return null;
    }
    String data = (String) logs.get(key);
    if (!StringUtils.isEmpty(data)) {
      ChaosEventManager.ChaosMetadata logEntry = JSONObject.parseObject(data, ChaosEventManager.ChaosMetadata.class);
      return LogEntry.builder().metadata(logEntry).serviceName(key).build();
    }
    //        if (o != null && o.length > 0 ) {
    //          LogEntry result = JSONObject.parseObject(o, LogEntry.class);
    //          return result;
    //        }
    return null;
  }

  public void close() {
    if (db != null && !db.isClosed()) {
      LOGGER.info(
          "LogManger start close... db : {} db state : {}",
          db == null ? true : false,
          db.isClosed());
      db.close();
    }
  }

  public static String getDefaultLogDir(int port) {
    return dbDir + "-" + port + db_SUFFIX;
  }
}
