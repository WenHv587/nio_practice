package edu.cqupt.aio;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil.debugAll;

/**
 * @author LWenH
 * @create 2021/7/15 - 16:04
 *
 * 文件AIO
 */
@Slf4j
public class FileAIO {
    /**
     * 使用AIO的API来读取文件
     * @param args
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
        URL resource = FileAIO.class.getClassLoader().getResource("data.txt");
        AsynchronousFileChannel channel =
                AsynchronousFileChannel.open(Paths.get(resource.toURI()), StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        log.debug("begin...");
        channel.read(byteBuffer, 0, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                log.debug("read completed...bytes = {}", result);
                byteBuffer.flip();
                debugAll(byteBuffer);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                log.debug("read failed...");
            }
        });
        log.debug("some other operations");
        System.in.read();
    }

    @Test
    public void testResources() throws URISyntaxException {
        URL resource = this.getClass().getClassLoader().getResource("data.txt");
        System.out.println(resource);
        Path path = Paths.get(resource.toURI());
        System.out.println(path);
    }
}
