package worseThanFailure;

import java.io.IOException;
import java.io.InputStream;

// http://worsethanfailure.com/Articles/Please_Supply_a_Test_Case.aspx
public class Post20061102 {

    void f(InputStream _inputStream) throws IOException {

        boolean _validConnection = false;
        while (!_validConnection) {
            StringBuffer _stringBuffer = new StringBuffer();
            try {
                while (true) {
                    char _char;
                    _stringBuffer.append(_char = (char) _inputStream.read());
                    if (_char == -1) {
                        break;
                    } else if (_char == '\r') {
                        _stringBuffer.append(_char = (char) _inputStream.read());
                        if (_char == -1) {
                            break;
                        } else if (_char == '\n') {
                            _stringBuffer.append(_char = (char) _inputStream.read());
                            if (_char == -1) {
                                break;
                            } else if (_char == '\r') {
                                _stringBuffer.append(_char = (char) _inputStream.read());
                                if (_char == -1) {
                                    break;
                                } else if (_char == '\n') {
                                    _inputStream.read(new byte[_inputStream.available()]);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (OutOfMemoryError error) {
                // received a bad response, try it again!
                continue;
            }

        }
    }
}
