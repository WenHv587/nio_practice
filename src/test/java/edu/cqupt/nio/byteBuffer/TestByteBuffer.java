package edu.cqupt.nio.byteBuffer;

import edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import static edu.cqupt.nio.byteBuffer.utils.ByteBufferUtil.debugAll;

/**
 * @author LWenH
 * @create 2021/7/13 - 19:39
 */
@Slf4j
public class TestByteBuffer {

    /**
     * 黏包、半包问题
     *
     * 网络上有多条数据发送给服务端，数据之间使用 \n 进行分隔
     * 但由于某种原因这些数据在接收时，被进行了重新组合，例如原始数据有3条为
     *     Hello,world\n
     *     I'm zhangsan\n
     *     How are you?\n
     * 变成了下面的两个 byteBuffer (黏包，半包)
     *     Hello,world\nI'm zhangsan\nHo
     *     w are you?\n
     * 现在要求你编写程序，将错乱的数据恢复成原始的按 \n 分隔的数据
     */
    @Test
    public void example() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put("Hello,world\nI'm zhangsan\nHo".getBytes());
        split(buffer);
        buffer.put("w are you?\n".getBytes());
        split(buffer);
    }

    /**
     * 分割数据
     */
    public void split(ByteBuffer buffer) {
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

    /**
     * 测试合并写
     */
    @Test
    public void testGatheringWrites() {
        ByteBuffer b1 = StandardCharsets.UTF_8.encode("hello");
        ByteBuffer b2 = StandardCharsets.UTF_8.encode("world");
        ByteBuffer b3 = StandardCharsets.UTF_8.encode("你好");
        try (FileChannel channel = new RandomAccessFile("src/test/resources/words.txt", "rw").getChannel()) {
            channel.write(new ByteBuffer[]{b1, b2, b3});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 测试分散读
     */
    @Test
    public void testScatteringReads() {
        String file = this.getClass().getClassLoader().getResource("data.txt").getFile();
        try (FileChannel channel = new RandomAccessFile(file, "r").getChannel()) {
            ByteBuffer buffer1 = ByteBuffer.allocate(8);
            ByteBuffer buffer2 = ByteBuffer.allocate(8);
            channel.read(new ByteBuffer[]{buffer1, buffer2});
            debugAll(buffer1);
            debugAll(buffer2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试ByteBuffer与String互转
     */
    @Test
    public void testBufferString() {
        // 1. 字符串转为ByteBuffer
        ByteBuffer buffer1 = ByteBuffer.allocate(16);
        buffer1.put("hello".getBytes());
        debugAll(buffer1);
        // 这种方式不会转为读模式。因此get不到东西
        System.out.println(buffer1.get());

        // 2. Charset 这种方式position = 0
        ByteBuffer buffer2 = StandardCharsets.UTF_8.encode("hello");
        debugAll(buffer2);
        System.out.println((char)buffer2.get());

        // 3. wrap 这种方式position = 0
        ByteBuffer buffer3 = ByteBuffer.wrap("hello".getBytes());
        debugAll(buffer3);

        // 4. 转字符串
        String str1 = StandardCharsets.UTF_8.decode(buffer3).toString();
        System.out.println(str1);

        // 因为buffer1 position在末尾（还是写模式），因此读不到。需要转为读模式，才能读到内容。
        buffer1.flip();
        String str2 = StandardCharsets.UTF_8.decode(buffer1).toString();
        System.out.println("=====" + str2);

    }

    /**
     * 测试ByteBuffer读写
     */
    @Test
    public void testReadWrite() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte) 0x61); // 'a'
        debugAll(buffer);
        buffer.put(new byte[]{0x62, 0x63, 0x64}); // b  c  d
        debugAll(buffer);
//        buffer.flip();
        System.out.println(buffer.get());
        debugAll(buffer);
        buffer.compact();
        debugAll(buffer);
        buffer.put(new byte[]{0x65, 0x6f});
        debugAll(buffer);
        buffer.clear();
        debugAll(buffer);
    }

    /**
     * 测试ByteBuffer读
     */
    @Test
    public void testRead() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(new byte[]{'a', 'b', 'c', 'd'});
        // 切换到读模式
        buffer.flip();
        buffer.get(new byte[4]);
        debugAll(buffer);

        // rewind 从头开始读  将position指针移到第一个位置
        buffer.rewind();
        System.out.println((char)buffer.get());
        debugAll(buffer);

        // mark & reset
        // mark 做一个标记，记录 position 位置， reset 是将 position 重置到 mark 的位置
        System.out.println((char) buffer.get());
        debugAll(buffer);
        // 做标记 保存当前position的位置
        buffer.mark();
        System.out.println((char) buffer.get());
        System.out.println((char) buffer.get());
        // 将position重置到mark位置
        buffer.reset();
        System.out.println((char) buffer.get());
        debugAll(buffer);

        // get(i) 不会改变读索引的位置
        System.out.println((char) buffer.get(3));
        debugAll(buffer);
    }


    /**
     * ByteBuffer分配空间的两种方式
     */
    @Test
    public void testAllocate() {
        /*
            使用堆内存
            读写效率较低，受到 GC 的影响
         */
        ByteBuffer buffer1 = ByteBuffer.allocate(16);
        /*
            使用直接内存
            写效率高（少一次拷贝），不会受 GC 影响，分配的效率低
         */
        ByteBuffer buffer2 = ByteBuffer.allocateDirect(16);
    }


    /**
     * ByteBuffer初测
     */
    @Test
    public void test1() {
        String source = TestByteBuffer.class.getClassLoader().getResource("data.txt").getFile();
        // 通过输入/输出流获取channel
        try (FileChannel channel = new FileInputStream(source).getChannel()) {
            // 准备缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(32);
            int len;
            while ((len = channel.read(buffer)) != -1) {
                log.info("读取到的字节数 = {}", len);
                // 切换为读模式
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    log.debug("实际字节 {}", (char)b);
                }
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
