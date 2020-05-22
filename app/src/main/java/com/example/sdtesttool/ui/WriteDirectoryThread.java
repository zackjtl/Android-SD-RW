package com.example.sdtesttool.ui;

import android.os.Handler;
import android.os.Message;

import com.example.sdtesttool.ui.test.TestFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
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

    public WriteDirectoryThread(Handler msgHandler, PatternVerifyType verifyType, String rootDir, int rootSizeKB, int fileSizeKB, int testSizeRatio) {
        this.rootDir = rootDir;
        this.rootSizeKB = rootSizeKB;
        this.fileSizeKB = fileSizeKB;
        this.testSizeRatio = testSizeRatio;
        this.msgHandler = msgHandler;
        terminated = false;
        this.verifyType = verifyType;
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

            totalWriteElapsedMilli = 0;
            totalKBWritten = 0;
            totalReadElapsedMilli = 0;
            totalKBRead = 0;

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
                if (!dir.canWrite()) {
                    throw new Exception("Directory not writable");
                }

                String fileName = String.format("%04x.bin", dirFileCount);

                File file = new File(directory, fileName);

                if (!file.createNewFile()) {
                    //throw new Exception("Create file failed! (" + file.getAbsolutePath() + ")");
                }
                showText("Try to write file " + file);

                FileOutputStream fsOut = new FileOutputStream(file);

                try {
                    byte[] bufOut = new byte[thisFileSizeKB << 10];
                    rand.nextBytes(bufOut);

                    bufOut[0] = (byte) 0x55;
                    bufOut[1] = (byte) 0xaa;
                    bufOut[bufOut.length - 2] = (byte) 0xaa;
                    bufOut[bufOut.length - 1] = (byte) 0x55;

                    long tWStart = System.currentTimeMillis();
                    fsOut.write(bufOut);
                    long tWEnd = System.currentTimeMillis();
                    totalWriteElapsedMilli += (tWEnd - tWStart);

                    if (verifyType == PatternVerifyType.immediately) {
                        FileInputStream fsIn = new FileInputStream(file);
                        try {
                            byte[] bufIn = new byte[thisFileSizeKB << 10];
                            long tRStart = System.currentTimeMillis();
                            fsIn.read(bufIn);
                            long tREnd = System.currentTimeMillis();
                            totalReadElapsedMilli += (tREnd - tRStart);
                            totalKBRead += thisFileSizeKB;
                            updateReadPerformance();
                            if (!Arrays.equals(bufOut, bufIn)) {
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
                totalKBWritten += thisFileSizeKB;
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

            totalReadElapsedMilli = 0;
            totalKBRead = 0;

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
                    totalReadElapsedMilli += (tREnd - tRStart);
                    totalKBRead += thisFileSizeKB;
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

                updateReadPerformance();
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
    public void updateWritePerformance()
    {
        writePerformanceKB = ((double)totalKBWritten / (double)totalWriteElapsedMilli) * 1000;
        setWritePerformance(writePerformanceKB);
    }
    public void updateReadPerformance()
    {
        readPerformanceKB = ((double)totalKBRead / (double)totalReadElapsedMilli) * 1000;
        setReadPerformance(readPerformanceKB);
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
