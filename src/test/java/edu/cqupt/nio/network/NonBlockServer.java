package edu.cqupt.nio.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil.debugRead;

/**
 * @author LWenH
 * @create 2021/7/14 - 17:44
 *
 * 单线程非阻塞式服务器：即使没有连接建立，没有可读数据，线程仍然在不断运行，白白浪费了 cpu
 */
@Slf4j
public class NonBlockServer {
    public static void main(String[] args) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        // 使用非阻塞模式
        ssc.configureBlocking(false);
        // 连接集合
        List<SocketChannel> socketChannelList = new ArrayList<>();
        while (true) {
            // 由于开启了非阻塞模式，这里accept不会一直阻塞
            SocketChannel socketChannel = ssc.accept();
            if (socketChannel != null) {
                log.debug("server connected...{}", socketChannel);
                // 将socketChannel也设置为非阻塞模式
                socketChannel.configureBlocking(false);
                socketChannelList.add(socketChannel);
            }
            for (SocketChannel channel : socketChannelList) {
                // 非阻塞，线程仍然会继续运行，如果没有读到数据，read 返回 0
                int read = channel.read(byteBuffer);
                if (read > 0) {
                    // 如果读到有数据
                    byteBuffer.flip();
                    debugRead(byteBuffer);
                    byteBuffer.clear();
                    log.debug("after read{}",channel);
                }
            }
        }
    }
}
