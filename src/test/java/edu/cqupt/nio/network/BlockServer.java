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
 * @create 2021/7/14 - 16:58
 *
 * 单线程阻塞式服务器
 */
@Slf4j
public class BlockServer {
    public static void main(String[] args) throws IOException {
        // 创建服务服务端socketChannel
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 绑定监听端口
        ssc.bind(new InetSocketAddress(8080));
        // 保存连接的集合 连接的channel都是socketChannel
        List<SocketChannel> socketChannelList = new ArrayList<>();
        // 缓存数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        while(true) {
            // accept()方法是一个阻塞式的方法 接收客户端的连接，建立起SocketChannel
            log.debug("server wait for connect...");
            SocketChannel sc = ssc.accept();
            log.debug("server successful connected...{}",sc);
            // 将建立起来的SocketChannel保存起来
            socketChannelList.add(sc);

            for (SocketChannel socketChannel : socketChannelList) {
                log.debug("server before read...{}", socketChannel);
                // read()方法：接收客户端发送的数据 阻塞方法
                socketChannel.read(byteBuffer);
                // 切换到读模式
                byteBuffer.flip();
                debugRead(byteBuffer);
                byteBuffer.clear();
                log.debug("server after read...{}", socketChannel);
            }
        }
    }
}
