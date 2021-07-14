package edu.cqupt.nio.fileChannel;

import com.sun.deploy.util.SyncFileAccess;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author LWenH
 * @create 2021/7/14 - 15:07
 */
public class TestFileChannel {

    /*
        WalkFileTree使用的是观察者模式
     */

    /**
     * 测试使用 Files WalkFileTree 删除整个文件夹
     */
    @Test
    public void testFilesWalkFileTree2() throws IOException {
        Files.walkFileTree(Paths.get("D:\\DeskTop\\已经卸载的myql"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    /**
     * 测试 Files WalkFileTree 遍历文件夹
     */
    @Test
    public void testFilesWalkFileTree() throws IOException {
        AtomicInteger dirsCount = new AtomicInteger();
        AtomicInteger filesCount = new AtomicInteger();
        Files.walkFileTree(Paths.get("D:\\DeskTop\\已经卸载的mysql（副本）"), new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dirsCount.incrementAndGet();
                System.out.println("进入文件夹===》" + dir);
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                filesCount.incrementAndGet();
                System.out.println(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                System.out.println(dir + "<===退出文件夹");
                return super.postVisitDirectory(dir, exc);
            }
        });

        System.out.println("文件夹总数：" + dirsCount);
        System.out.println("文件总数：" + filesCount);
    }

    /**
     * 使用 Files API 进行整个文件夹的复制
     */
    @Test
    public void testFilesCopy() throws IOException {
        String source = "D:\\DeskTop\\已经卸载的myql";
        String target = "D:\\DeskTop\\已经卸载的mysql（副本）";

        Files.walk(Paths.get(source)).forEach(path -> {
            try {
                String targetName = path.toString().replace(source, target);
                System.out.println(targetName);
                if (Files.isDirectory(path)) {
                    Files.createDirectory(Paths.get(targetName));
                } else if (Files.isRegularFile(path)) {
                    Files.copy(path,Paths.get(targetName));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 测试fileChannel transferTo
     */
    @Test
    public void testTransferTo() {
        String formFile = this.getClass().getClassLoader().getResource("data.txt").getFile();
        try (FileChannel to = new FileOutputStream("to.txt").getChannel();
             FileChannel form = new FileInputStream(formFile).getChannel()) {
            // 文件的总大小
            long size = form.size();
            for (long left = size; left > 0; ) {
                System.out.println("position:" + (size - left) + " left:" + left);
                left -= form.transferTo(size - left, left, to);
                System.out.println("position:" + (size - left) + " left:" + left);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
