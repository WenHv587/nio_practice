package edu.cqupt.nio.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @author LWenH
 * @create 2021/7/14 - 23:07
 * <p>
 * 处理写事件的服务器
 * 服务器向客户端写数据
 */
public class WriteServer {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    SelectionKey scKey = sc.register(selector, SelectionKey.OP_READ);
                    // 向客户端发送数据
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < 3000000; i++) {
                        stringBuilder.append("greenhand");
                    }
                    ByteBuffer buffer = Charset.defaultCharset().encode(stringBuilder.toString());

                    // 返回值代表直接写入的字节数
                    int write = sc.write(buffer);
                    System.out.println("写入的字节数：" + write);

                    /*
                        使用一个while循环写完也是可以的，但是不符合“非阻塞”的要求
                        这里使用多个可写事件来写
                     */
                    // 判断是否有剩余内容
                    if (buffer.hasRemaining()) {
                        // 关注可写事件
                        scKey.interestOps(scKey.interestOps() + SelectionKey.OP_WRITE);
                        // 将未写完的数据以附件的形式关联到key上
                        scKey.attach(buffer);
                    }
                } else if (key.isWritable()) {
                    // 如果是写事件，说明服务端一次没有写完
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    SocketChannel sc = (SocketChannel) key.channel();
                    int write = sc.write(buffer);
                    System.out.println("写入的字节数：" + write);
                    // 清理操作
                    if (! buffer.hasRemaining()) {
                        // 便于ByteBuffer的垃圾回收
                        key.attach(null);
                        // 取消关注写事件
                        key.interestOps(key.interestOps() - SelectionKey.OP_WRITE);
                    }
                }
            }
        }
    }
}
