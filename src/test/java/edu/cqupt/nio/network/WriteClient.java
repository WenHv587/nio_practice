package edu.cqupt.nio.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author LWenH
 * @create 2021/7/14 - 23:07
 */
public class WriteClient {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 8080));

        // 接收数据
        int count = 0;
        while (true) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            count += sc.read(byteBuffer);
            System.out.println("读取数量：" + count);
            byteBuffer.clear();
        }
    }
}
