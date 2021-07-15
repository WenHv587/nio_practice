package edu.cqupt.nio.network;

import javafx.concurrent.Worker;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil.debugAll;

/**
 * @author LWenH
 * @create 2021/7/15 - 11:02
 * <p>
 * 多线程 每个线程配合Selector多路复用 非阻塞式服务器
 */
@Slf4j
public class MultiThreadSelectorServer {
    public static void main(String[] args) throws IOException {
        // 主线程（Boss线程 ），只负责处理Accept事件
        Thread.currentThread().setName("Boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false);
        Selector bossSelector = Selector.open();
        SelectionKey bossKey = ssc.register(bossSelector, SelectionKey.OP_ACCEPT);

        // 创建固定数量的worker线程
        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        // 用于选择worker的标记
        AtomicInteger index = new AtomicInteger();
        while (true) {
            bossSelector.select();
            Iterator<SelectionKey> iterator = bossSelector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    log.debug("server wait for connect...");
                    // 服务端和客户端建立连接
                    SocketChannel socketChannel = ssc.accept();
                    log.debug("server successful connected...{}", socketChannel);
                    socketChannel.configureBlocking(false);
                    log.debug("before worker init...");
                    /*
                        对worker进行初始化
                        因为有多个worker，要将socketChannel注册在不同的worker的selector上
                        每个worker的selector就会监听这些channel发生的事件，然后对事件进行处理。

                        要让channel平均的分 配给每个worker，做一个类似负载均衡的操作
                        这里采用一种round robin 轮询的方式，将channel平均的和每一个worker关联起来
                     */
                    workers[index.getAndIncrement() % workers.length].initialize(socketChannel);
                     log.debug("after worker init...");
                }
            }
        }
    }

    /**
     * Worker类，负责处理Read和Write事件
     *
     * 每一个worker都有自己的selector，可以负责监听事件的发生。
     * 当有多个客户端产生事件是时，多个worker就可以分别对这些事件进行处理
     *
     * 多线程非阻塞指的是：有多个worker（多线程），每个worker仍然使用selector 非阻塞 多路复用
     */
    static class Worker implements Runnable {
        private Thread thread;
        private Selector workSelector;
        private String name;
        /**
         * 初始化标记
         * 一个Worker仅被初始化一次。当被初始化过后，标记改为true。
         */
        private volatile boolean initFlag = false;

        public Worker(String name) {
            this.name = name;
        }

        /**
         * 初始化worker
         * @param socketChannel 客户端和服务器建立起来的socketChannel
         * @throws IOException
         */
        public void initialize(SocketChannel socketChannel) throws IOException {
            if (! initFlag) {
                workSelector = Selector.open();
                new Thread(this, name).start();
                initFlag = true;
            }
            // wakeup()无论是执行在select()方法的前还是后，都能够唤醒线程，结束掉等待任务的阻塞状态。
            workSelector.wakeup();
            socketChannel.register(workSelector, SelectionKey.OP_READ);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    workSelector.select();
                    Iterator<SelectionKey> iterator = workSelector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isReadable()) {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(32);
                            SocketChannel channel = (SocketChannel) key.channel();
                            log.debug("read...{}", channel);
                            channel.read(byteBuffer);
                            byteBuffer.flip();
                            debugAll(byteBuffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}



