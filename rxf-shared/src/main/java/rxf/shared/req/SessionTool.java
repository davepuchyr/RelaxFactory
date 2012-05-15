package rxf.shared.req;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServiceName;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:05 PM
 */
@ServiceName("rxf.server.VisitorPropertiesAccess")
public interface SessionTool extends RequestContext {
  Request<String> getSessionProperty(String key);

  /**
   * @param key
   * @param value
   * @return new version string
   */
  Request<String> setSessionProperty(String key, String value);
}