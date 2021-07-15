package edu.cqupt.nio.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * @author LWenH
 * @create 2021/7/14 - 17:13
 *
 * 客户端
 */
public class Client {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost",8080));
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("请发送信息：");
            // 0123456789abcdef3333\n
            String info = scanner.nextLine();
            System.out.println(info);
            sc.write(Charset.defaultCharset().encode(info));
        }
//        sc.write(Charset.defaultCharset().encode("0123456789abcdef3333\n"));
//        System.in.read();
    }
}
