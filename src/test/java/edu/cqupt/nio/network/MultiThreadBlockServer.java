package edu.cqupt.nio.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil.debugRead;

/**
 * @author LWenH
 * @create 2021/7/14 - 17:32
 *
 * 多线程阻塞式服务器
 */
@Slf4j
public class MultiThreadBlockServer {
    public static void main(String[] args) throws IOException {
        // 创建服务服务端socketChannel
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 绑定监听端口
        ssc.bind(new InetSocketAddress(8080));
        // 缓存数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        while(true) {
            // accept()方法是一个阻塞式的方法 接收客户端的连接，建立起SocketChannel
            log.debug("server wait for connect...");
            SocketChannel sc = ssc.accept();
            // 对于每一个客户端的连接，都新建一个线程对其进行处理
            new Thread(() -> {
                try {
                    log.debug("server successful connected...{}",sc);
                    log.debug("server before read...{}", sc);
                    sc.read(byteBuffer);
                    byteBuffer.flip();
                    debugRead(byteBuffer);
                    byteBuffer.clear();
                    log.debug("server after read...{}", sc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
