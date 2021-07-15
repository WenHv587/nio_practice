package edu.cqupt.nio.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import static edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil.debugAll;

/**
 * @author LWenH
 * @create 2021/7/14 - 18:23
 * <p>
 * 单线程配合Selector多路复用 非阻塞式服务器
 */
@Slf4j
public class SelectorServer {
    public static void main(String[] args) throws IOException {
        // 创建selector, 管理多个channel
        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false);
        /*
            建立 selector 和 channel 的联系 （注册）
            SelectionKey的作用：将来事件发生后，通过它可以知道事件，且是哪个channel产生的事件
         */
        SelectionKey sscKey = ssc.register(selector, 0, null);
        // 只接受处理Accept类型的事件
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.debug("sscKey:{}", sscKey);
        while (true) {
            /*
                select 方法, 没有事件发生，线程阻塞，有事件，线程才会恢复运行
                select 在事件发生但是未处理时，不会阻塞, 事件发生后要么处理，要么取消，不能置之不理
             */
            selector.select();
            /*
                处理事件 selectedKeys 内部包含了所有发生的事件
                public abstract Set<SelectionKey> selectedKeys(); --> selectedKeys()返回一个Set集合
             */
//            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 处理key 时，要从 selectedKeys 集合中删除，否则下次处理就会有问题
                iterator.remove();
                log.debug("key:{}", key);
                // 区分事件类型进行处理
                if (key.isAcceptable()) {
                    // 如果是Accept事件
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
//                    serverSocketChannel.configureBlocking(false); 这里得到就是原本的ssc，上面设置过了非阻塞了
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);

                    /*
                        防止数据内容长度会超过ByteBuffer的容量，将ByteBuffer以附件attachment的形式注册在sckey上
                        因为sckey是唯一的，就算数据长度超出ByteBuffer限制，造成了两次读取，
                        也可以进行扩容，并从附件中得到同一个ByteBuffer
                     */
                    ByteBuffer byteBuffer = ByteBuffer.allocate(16);
                    SelectionKey scKey = sc.register(selector, 0, byteBuffer);
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("sc:{}", sc);
                    log.debug("scKey:{}", scKey);
                } else if (key.isReadable()) {
                    try {
                        // 如果是read事件
                        // 拿到触发事件的channel
                        SocketChannel channel = (SocketChannel) key.channel();
//                        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
                        // 从key上获取到关联的附件
                        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
                        int read = channel.read(byteBuffer);
                        // 如果是正常断开，read的返回值会是-1
                        if (read == - 1) {
                            // 正常断开依然会产生一个Read事件，因此需要把这个事件取消掉，不然
                            key.cancel();
                        } else {
//                            byteBuffer.flip();
//                            System.out.println(Charset.defaultCharset().decode(byteBuffer));
                            split(byteBuffer);
                            // 在进行了split()方法，其中进行了compact()以后，如果position == limit，那么说明ByteBuffer容量不够
                            if (byteBuffer.position() == byteBuffer.limit()) {
                                // 进行2倍扩容
                                ByteBuffer newBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2);
                                byteBuffer.flip();
                                newBuffer.put(byteBuffer);
                                // key重新关联新的newBuffer作为附件
                                key.attach(newBuffer);
                            }
                        }
                    } catch (IOException e) {
                        // 无论客户端是正常断开连接还是异常强制关闭连接，都会产生一个Read类型的事件
                        e.printStackTrace();
                        // 这里如果不处理关闭客户端产生的事件，循环就会继续而不是被阻塞，就会一直抛异常
                        key.cancel();
                    }
                }
            }
        }
    }

    /**
     * 分割数据
     */
    private static void split(ByteBuffer buffer) {
        // 转换为读模式
        buffer.flip();
        for (int i = 0; i < buffer.limit(); i++) {
            if (buffer.get(i) == '\n') {
                int length = i - buffer.position();
                ByteBuffer result = ByteBuffer.allocate(length);
                for (int j = 0; j < length; j++) {
                    result.put(buffer.get());
                }
                debugAll(result);
            }
        }
        // 压缩已经读过的结果，保留分包的内容
        buffer.compact();
    }
}
