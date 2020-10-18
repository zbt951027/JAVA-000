import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 自定义一个 Classloader，加载一个 Hello.xlass 文件，执行 hello 方法，此文件内容是一个 Hello.class 文件所有字节（x=255-x）处理后的文件
 *
 * @author zhangbotao
 * @date 2020/10/18
 */
public class HelloClassLoader extends ClassLoader {

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = new byte[0];
        try {
            bytes = getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    private byte[] getBytes() throws IOException {
        File file = new File("./Hello.xlass");
        FileInputStream in = new FileInputStream(file);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (255 - bytes[i]);
        }
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        HelloClassLoader helloClassLoader = new HelloClassLoader();
        Class<?> helloClz = helloClassLoader.findClass("Hello");
        Object helloObj = helloClz.newInstance();
        Method helloMethod = helloClz.getMethod("hello");
        helloMethod.invoke(helloObj);
    }
}
