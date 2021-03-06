package rxf.core;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import one.xio.HttpStatus;

import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static one.xio.HttpHeaders.*;

public class Rfc6455WsInitiator {
  /**
   * <ul>
   * assumes:
   * <li>cursor is an unfinished ByteBuffer</li>
   * <li>exists with all the state needed from surrounding enclosures.
   * </ul>
   * <p/>
   * <p/>
   * <p>
   * 
   * <pre>
     *
     * 2.   The method of the request MUST be GET,
     * For example, if the WebSocket URI is "ws://example.com/chat",
     * the first line sent should be "GET /chat HTTP/1.1".
     *
     * @param httpRequest
     * @param ws_uri
     * @param host
     * @param wsProto
     * @return a HttpResponse suitable for .as(String.class) or .asByteBuffer()
   */
  public Rfc822HeaderState.HttpResponse parseInitiatorRequest(
      Rfc822HeaderState.HttpRequest httpRequest, URI ws_uri, String host, String wsProto)
      throws URISyntaxException {

    HttpMethod httpMethod = HttpMethod.valueOf(httpRequest.method());
    String err = "wrong method.";
    switch (httpMethod) {
      case GET:
        String protocol = httpRequest.protocol();
        /*
         * and the HTTP version MUST be at least 1.1.
         */
        if (!Rfc822HeaderState.HTTP_1_1.equals(protocol)) {
          err = "wrong protocol";
          break;
        }
        if (6 > httpRequest.headerStrings().size()) {
          err = "quantity of request headers is not enough to pass.  ending early.";
          break;
        }

        // 6 MUST be present.
        /*
         * . The request MUST contain a |Host| header field whose value contains /host/ plus optionally ":" followed by
         * /port/ (when not using the default port).
         */
        String rhost = httpRequest.headerString(HttpHeaders.Host);
        String path = httpRequest.path();
        if (null == rhost || rhost.isEmpty()
            || !(rhost.startsWith(host) && path.startsWith(ws_uri.getPath()))) {
          err =
              "The \"Request-URI\" part of the request MUST match the /resource "
                  + "name/ defined in Section 3 (a relative URI) or be an absolute "
                  + "http/https URI that, when parsed, has a /resource name/, /host/, "
                  + "and /port/ that match the corresponding ws/wss URI.";
          break;
        }

        /*
         * 5. The request MUST contain an |Upgrade| header field whose value MUST include the "websocket" keyword.
         */
        if (!httpRequest.headerString(Upgrade).contains("websocket")) {
          err =
              "The request MUST contain an |Upgrade| header field whose value MUST include the \"websocket\" keyword.";
          break;
        }
        /*
         * 6.
         */
        if (!httpRequest.headerString(Connection).contains("Upgrade")) {
          err =
              "The request MUST contain a |Connection| header field whose value MUST include the \"Upgrade\" token.";
          break;
        }

        /*
         * 7. The request MUST include a header field with the name |Sec-WebSocket-Key|. The value of this header field
         * MUST be a nonce consisting of a randomly selected 16-byte value that has been base64-encoded (see Section 4
         * of [RFC4648]). The nonce MUST be selected randomly for each connection. NOTE: As an example, if the randomly
         * selected value was the sequence of bytes 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0a 0x0b 0x0c 0x0d
         * 0x0e 0x0f 0x10, the value of the header field would be "AQIDBAUGBwgJCgsMDQ4PEC=="
         */
        String wsKeyBase64 = httpRequest.headerString(Sec$2dWebSocket$2dKey);
        byte[] wsKey = DatatypeConverter.parseBase64Binary(wsKeyBase64);
        if (wsKey.length != 16) {
          err =
              "The request MUST include a header field with the name |Sec-WebSocket-Key|.  The value of this header field MUST be a nonce consisting of a randomly selected 16-byte value that has been base64-encoded (see Section 4 of [RFC4648]).  The nonce MUST be selected randomly for each connection.";
          break;
        }
        /* *
         * 8. The request MUST include a header field with the name |Origin| [RFC6454] if the request is coming from a
         * browser client. If the connection is from a non-browser client, the request MAY include this header field if
         * the semantics of that client match the use-case described here for browser clients. The value of this header
         * field is the ASCII serialization of origin of the context in which the code establishing the connection is
         * running. See [RFC6454] for the details of how this header field value is constructed.
         * 
         * As an example, if code downloaded from www.example.com attempts to establish a connection to ww2.example.com,
         * the value of the header field would be "http://www.example.com".
         */

        /*
         * 9. The request MUST include a header field with the name |Sec-WebSocket-Version|. The value of this header
         * field MUST be 13.
         * 
         * NOTE: Although draft versions of this document (-09, -10, -11, and -12) were posted (they were mostly
         * comprised of editorial changes and clarifications and not changes to the wire protocol), values 9, 10, 11,
         * and 12 were not used as valid values for Sec-WebSocket-Version. These values were reserved in the IANA
         * registry but were not and will not be used.
         */
        if (13 != Integer.valueOf(httpRequest.headerString(HttpHeaders.Sec$2dWebSocket$2dVersion))) {
          err = "this server only supports version 13 of ws initiator";
          break;
        }
        Rfc822HeaderState.HttpResponse httpResponse = new Rfc822HeaderState().$res();
        Map<String, String> headerStrings = new TreeMap<String, String>();
        /*
         * 4. If the response lacks a |Sec-WebSocket-Accept| header field or the |Sec-WebSocket-Accept| contains a value
         * other than the base64-encoded SHA-1 of the concatenation of the |Sec-WebSocket- Key| (as a string, not
         * base64-decoded) with the string "258EAFA5-E914-47DA-95CA-C5AB0DC85B11" but ignoring any leading and trailing
         * whitespace, the client MUST _Fail the WebSocket Connection_.
         * 
         * /key/ The |Sec-WebSocket-Key| header field in the client's handshake includes a base64-encoded value that, if
         * decoded, is 16 bytes in length. This (encoded) value is used in the creation of the couch's handshake to
         * indicate an acceptance of the connection. It is not necessary for the couch to base64- decode the
         * |Sec-WebSocket-Key| value.
         */

        HashCode hashCode =
            Hashing.sha1().hashString(wsKeyBase64 + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11",
                StandardCharsets.UTF_8);
        headerStrings.put(Sec$2dWebSocket$2dAccept.getHeader(), BaseEncoding.base64().encode(
            hashCode.asBytes()));
        headerStrings.put(Sec$2dWebSocket$2dProtocol.getHeader(), wsProto);
        headerStrings.put(Upgrade.getHeader(), "websocket");
        headerStrings.put(Connection.getHeader(), "Upgrade");
        Rfc822HeaderState.HttpResponse response1 =
            httpResponse.resCode(HttpStatus.$101).status(HttpStatus.$101).headerStrings(
                headerStrings).$res()
        /* .asByteBuffer() */;
        System.err.println("sending back: " + httpResponse.as(String.class));
        return response1;
      case POST:
        break;
      case PUT:
        break;
      case HEAD:
        break;
      case DELETE:
        break;
      case TRACE:
        break;
      case CONNECT:
        break;
      case OPTIONS:
        break;
      case HELP:
        break;
      case VERSION:
        break;
    }

    return httpRequest.$res().status(HttpStatus.$401).status(err);
  }

  public static final Random RANDOM = new Random();

  /**
   * initiator requries at least 6 headers. this will provide those.
   * 
   * @param path
   * @param host
   * @param origin
   * @param protocol
   * @return a request object with relevant interest. returns nonce in .headerString(Sec$2dWebSocket$2dKey )
   */
  public static Rfc822HeaderState.HttpRequest req(String path, String host, String origin,
      String protocol) {
    byte[] nonce = new byte[16];
    RANDOM.nextBytes(nonce);
    String encode = BaseEncoding.base64().encode(nonce);
    Rfc822HeaderState.HttpRequest req = headerInterest(new Rfc822HeaderState())//
        .headerString(Connection, "Upgrade")//
        .headerString(Host, host)//
        .headerString(Origin, origin)//
        .headerString(Sec$2dWebSocket$2dKey, encode)//
        .headerString(Sec$2dWebSocket$2dProtocol, protocol)//
        .headerString(Sec$2dWebSocket$2dVersion, "13")//
        .headerString(Upgrade, "websocket")//
        .$req().path(path);
    return req;
  }

  public static String accept(String encode) {
    return BaseEncoding.base64().encode(
        Hashing.sha1().hashString(encode + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11",
            StandardCharsets.UTF_8).asBytes());
  }

  /**
   * initiator requries at least 6 headers. this will provide those.
   * 
   * @param rfc822HeaderState
   * @return a request object with relevant interest
   */
  public static Rfc822HeaderState headerInterest(Rfc822HeaderState rfc822HeaderState) {
    return rfc822HeaderState.addHeaderInterest(Connection, Host, Origin, Sec$2dWebSocket$2dKey,
        Sec$2dWebSocket$2dProtocol, Sec$2dWebSocket$2dVersion, Upgrade);
  }
}
