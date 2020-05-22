package com.example.sdtesttool.ui;

import android.os.Handler;
import android.os.Message;
import android.os.StatFs;

import com.example.sdtesttool.ui.test.TestFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class WriteDirectoryThread extends Thread {
    String rootDir;
    int rootSizeKB;
    int fileSizeKB;
    int testSizeRatio;
    Handler msgHandler;
    volatile boolean terminated;
    PatternVerifyType verifyType;
    long totalKBWritten = 0;
    long totalKBRead = 0;
    long totalWriteElapsedMilli = 0;
    long totalReadElapsedMilli=0;
    double writePerformanceKB = 0;
    double readPerformanceKB=0;

    Queue<PerformanceUnit> writePerformanceQueue;
    Queue<PerformanceUnit> readPerformanceQueue;

    int writePerformanceSlit = 0;
    int readPerformanceSlit = 0;

    private class PerformanceUnit {
        public int time;
        public int size;

        public PerformanceUnit(int time, int size) {this.time = time; this.size = size;}
    }

    public WriteDirectoryThread(Handler msgHandler, PatternVerifyType verifyType, String rootDir, int rootSizeKB, int fileSizeKB, int testSizeRatio) {
        this.rootDir = rootDir;
        this.rootSizeKB = rootSizeKB;
        this.fileSizeKB = fileSizeKB;
        this.testSizeRatio = testSizeRatio;
        this.msgHandler = msgHandler;
        terminated = false;
        this.verifyType = verifyType;

        writePerformanceQueue = new ArrayDeque<>();
        readPerformanceQueue = new ArrayDeque<>();

    }
    public enum PatternVerifyType {
        none,
        immediately,
        after_all
    }

    private void addLog(String s)
    {
        sendMessage(TestFragment.messageId.add_log.value, 0, s);
    }
    private void showText(String s)
    {
        sendMessage(TestFragment.messageId.set_text_msg.value, 0, s);
    }
    private void almostDone()
    {
        sendMessage(TestFragment.messageId.almost_done.value, 0, null);
    }
    private void setProgressMax(int progressMax)
    {
        sendMessage(TestFragment.messageId.set_progress_max.value, progressMax, null);
    }
    private void setProgress(int progress)
    {
        sendMessage(TestFragment.messageId.set_progress.value, progress, null);
    }
    private void setWritePerformance(double value)
    {
        sendMessage(TestFragment.messageId.set_write_speed.value, (int)(value * 100), null);
    }
    private void setReadPerformance(double value)
    {
        sendMessage(TestFragment.messageId.set_read_speed.value, (int)(value * 100), null);
    }
    private void sendMessage(int arg1, int arg2, Object obj)
    {
        Message msg = msgHandler.obtainMessage();
        msg.what = 300;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        msgHandler.sendMessage(msg);
    }

    @Override
    public void run()
    {
        writeFlow();
        if (verifyType == PatternVerifyType.after_all)
            verifyFlow();
        almostDone();
    }
    private void resetWritePerformance()
    {
        if (fileSizeKB < 32768)
            writePerformanceSlit = (32768 + fileSizeKB - 1) / fileSizeKB;
        else
            writePerformanceSlit = 1;

        writePerformanceQueue.clear();
    }
    private void addWritePerformanceUnit(int time, int size) {
        if (writePerformanceQueue.size() >= writePerformanceSlit)
            writePerformanceQueue.remove();

        writePerformanceQueue.add(new PerformanceUnit(time, size));
    }
    private void updateWritePerformance()
    {
        long totalTime = 0;
        long totalSize = 0;
        for (PerformanceUnit pu: writePerformanceQueue) {
            totalTime += pu.time;
            totalSize += pu.size;
        }
        writePerformanceKB = ((double)totalSize / (double)totalTime) * 1000;
        setWritePerformance(writePerformanceKB);
    }
    private void resetReadPerformance()
    {
        if (fileSizeKB < 32768)
            readPerformanceSlit = (32768 + fileSizeKB - 1) / fileSizeKB;
        else {
            readPerformanceSlit = 1;
        }

        readPerformanceQueue.clear();
    }
    private void addReadPerformanceUnit(int time, int size) {
        if (readPerformanceQueue.size() >= readPerformanceSlit)
            readPerformanceQueue.remove();

        readPerformanceQueue.add(new PerformanceUnit(time, size));
    }
    private void updateReadPerformance()
    {
        long totalTime = 0;
        long totalSize = 0;
        for (PerformanceUnit pu: readPerformanceQueue) {
            totalTime += pu.time;
            totalSize += pu.size;
        }
        readPerformanceKB = ((double)totalSize / (double)totalTime) * 1000;
        setReadPerformance(readPerformanceKB);
    }

    private void writeFlow()
    {
        try {
            String fName;

            Random rand = new Random(0x12345678);

            setProgressMax(50);

            addLog("Star to write files on directory " + rootDir);

            int testSizeKB = (int)(rootSizeKB * ((double)testSizeRatio / 100));
            int remainKB = testSizeKB;
            int totalFilesToTest = (testSizeKB + fileSizeKB - 1) / fileSizeKB;

            addLog("Size to test: " + remainKB + " KB");

            int dirId = 0;
            int dirFileCount = 0;
            int totalFileCount = 0;
            int maxDirFileCount = 32768;

            resetWritePerformance();
            resetReadPerformance();

            String subDir = String.format("%04x", dirId);
            String directory = initialDir(rootDir, subDir);

            setProgressMax(totalFilesToTest);

            while (remainKB != 0) {
                if (terminated)
                    throw new Exception("User terminated");

                if (remainKB <= 1024) {
                    StatFs stat1 = new StatFs(rootDir);
                    long totalBytes = (long) stat1.getBlockSize() * (long) stat1.getFreeBlocks();
                    long totalKB = totalBytes / 1024;
                    if (totalKB < remainKB)
                        remainKB = (int)totalKB;
                }

                int thisFileSizeKB = Math.min(fileSizeKB, remainKB);

                if (dirFileCount >= maxDirFileCount) {
                    ++dirId;
                    subDir = String.format("%04x", dirId);
                    directory = initialDir(rootDir, subDir);
                    dirFileCount = 0;
                }
                if (dirId > maxDirFileCount) {
                    break;
                }
                File dir = new File(directory);
                if (!dir.canWrite()) {
                    throw new Exception("Directory not writable");
                }

                String fileName = String.format("%04x.bin", dirFileCount);

                File file = new File(directory, fileName);

                if (!file.createNewFile()) {
                    //throw new Exception("Create file failed! (" + file.getAbsolutePath() + ")");
                }
                showText("Test file " + file);

                FileOutputStream fsOut = new FileOutputStream(file);

                try {
                    int sizeByte = thisFileSizeKB << 10;
                    FileChannel fcOut = fsOut.getChannel();

                    ByteBuffer bbWrite = ByteBuffer.allocate(sizeByte);

                    rand.nextBytes(bbWrite.array());

                    bbWrite.array()[0] = (byte)0x55;
                    bbWrite.array()[1] = (byte) 0xaa;
                    bbWrite.array()[sizeByte- 2] = (byte) 0xaa;
                    bbWrite.array()[sizeByte - 1] = (byte) 0x55;

                    long tWStart = System.currentTimeMillis();
                    //fsOut.write(bufOut);
                    fcOut.write(bbWrite);
                    long tWEnd = System.currentTimeMillis();
                    addWritePerformanceUnit((int)(tWEnd - tWStart), thisFileSizeKB);

                    if (verifyType == PatternVerifyType.immediately && (totalFileCount <= 50)) {
                        FileInputStream fsIn = new FileInputStream(file);
                        try {
                            ByteBuffer bbRead = ByteBuffer.allocate(sizeByte);
                            FileChannel fcIn = fsIn.getChannel();
                            long tRStart = System.currentTimeMillis();
                            fcIn.read(bbRead);
                            long tREnd = System.currentTimeMillis();
                            totalReadElapsedMilli += (tREnd - tRStart);
                            totalKBRead += thisFileSizeKB;
                            addReadPerformanceUnit((int)(tREnd - tRStart), thisFileSizeKB);
                            updateReadPerformance();
                            if (bbRead.compareTo(bbWrite) != 0) {
                                throw new Exception("Pattern comparation failed");
                            }
                        }
                        finally {
                            fsIn.close();
                        }
                    }
                }
                finally {
                    fsOut.close();
                }

                ++totalFileCount;
                ++dirFileCount;
                ////totalKBWritten += thisFileSizeKB;
                remainKB -= thisFileSizeKB;

                updateWritePerformance();
                setProgress(totalFileCount);
            }

            showText("Done");
            addLog("Done");
        }
        catch (Exception e) {
            addLog("thread throw error: " + e.getMessage());
            ////throw new RuntimeException(e.getMessage());
        }
        finally {

        }
    }
    private void verifyFlow()
    {
        try {
            String fName;

            Random rand = new Random(0x12345678);

            setProgressMax(50);

            addLog("Star to verify files on directory " + rootDir);

            int testSizeKB = (int)(rootSizeKB * ((double)testSizeRatio / 100));
            int remainKB = testSizeKB;
            int totalFilesToTest = (testSizeKB + fileSizeKB - 1) / fileSizeKB;

            addLog("Size to verify: " + remainKB + " KB");

            resetReadPerformance();

            int dirId = 0;
            int dirFileCount = 0;
            int totalFileCount = 0;
            int maxDirFileCount = 32768;
            int totalKBWritten = 0;
            long totalElapsedMillis = 0;
            double kbPerSecond = 0;
            String subDir = String.format("%04x", dirId);
            String directory = initialDir(rootDir, subDir);

            setProgressMax(totalFilesToTest);

            while (remainKB != 0) {
                if (terminated)
                    throw new Exception("User terminated");

                int thisFileSizeKB = Math.min(fileSizeKB, remainKB);

                if (dirFileCount >= maxDirFileCount) {
                    ++dirId;
                    subDir = String.format("%04x", dirId);
                    directory = initialDir(rootDir, subDir);
                    dirFileCount = 0;
                }
                if (dirId > maxDirFileCount) {
                    break;
                }
                File dir = new File(directory);
                if (!dir.canRead()) {
                    throw new Exception("Directory not readable");
                }

                String fileName = String.format("%04x.bin", dirFileCount);

                File file = new File(directory, fileName);

                if (!file.exists()) {
                    throw new Exception("File not exists! (" + file.getAbsolutePath() + ")");
                }
                showText("Try to verify file " + file);

                byte[] bufOut = new byte[thisFileSizeKB << 10];
                rand.nextBytes(bufOut);

                bufOut[0] = (byte) 0x55;
                bufOut[1] = (byte) 0xaa;
                bufOut[bufOut.length - 2] = (byte) 0xaa;
                bufOut[bufOut.length - 1] = (byte) 0x55;

                FileInputStream fsIn = new FileInputStream(file);

                try {
                    byte[] bufIn = new byte[thisFileSizeKB << 10];

                    long tRStart = System.currentTimeMillis();
                    fsIn.read(bufIn);
                    long tREnd = System.currentTimeMillis();
                    if (!Arrays.equals(bufOut, bufIn)) {
                        throw new Exception("Pattern comparison failed");
                    }
                }
                finally {
                    fsIn.close();
                }
                ++totalFileCount;
                ++dirFileCount;
                totalKBWritten += thisFileSizeKB;
                remainKB -= thisFileSizeKB;

                setProgress(totalFileCount);
            }

            showText("Done");
            addLog("Done");
        }
        catch (Exception e) {
            addLog("thread throw error: " + e.getMessage());
            ////throw new RuntimeException(e.getMessage());
        }
    }


    public static String initialDir(String rootDir, String subDir) throws Exception {
        File rootDirectory = new File(rootDir);

        if (!rootDirectory.exists()) {
            throw new Exception("Selected root directory not exists");
        }
        if (!rootDirectory.setWritable(true)) {
            throw new Exception("Can't set selected root directory writable");
        }

        File directory = new File(rootDir, subDir);

        if (!directory.exists())
            if (!directory.mkdir()) {
                throw new Exception("Can't mkdir test directory");
            }

        if (!directory.setWritable(true))
            throw new Exception("Can't set test root directory writable");
        return rootDir + "/" + subDir;
    }
    public void terminate() {terminated = true;}
}
