package rxf.server;

import java.lang.annotation.*;
import java.nio.ByteBuffer;

import one.xio.MimeType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, LOCAL_VARIABLE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, PACKAGE, PARAMETER})
public @interface DbKeys {
  enum etype {

    opaque, db, docId, rev {
      @Override
      <T> boolean validate(T... data) {
        return data[0].toString().length() > 0;
      }
    }, designDocId, view, validjson, mimetype {{
      clazz = MimeType.class;
    }}, blob {{
      clazz = ByteBuffer.class;
    }};

    <T> boolean validate(T... data) {
      return true;
    }

    Class clazz = String.class;
  }


  etype[] value();
//
//
//  public static abstract class ReturnAction<T> {
//
//    static ThreadLocal<ReturnAction> currentResults = new ThreadLocal<ReturnAction>();
//
//    public ReturnAction() {
//      currentResults.set(this);
//    }
//  }
//

}

