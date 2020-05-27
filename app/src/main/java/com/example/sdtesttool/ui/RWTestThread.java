package com.example.sdtesttool.ui;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;

import androidx.annotation.RequiresApi;

import com.example.sdtesttool.ui.test.TestFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;


public class RWTestThread extends TestThreadBase {
    Handler msgHandler;
    volatile boolean terminated;
    long totalKBWritten = 0;
    double writePerformanceKB = 0;
    double readPerformanceKB=0;

    TestArgs testArgs;

    static int MaxDirFiles = 32766;

    Queue<PerformanceUnit> writePerformanceQueue;
    Queue<PerformanceUnit> readPerformanceQueue;

    int writePerformanceSlit = 0;
    int readPerformanceSlit = 0;

    private class PerformanceUnit {
        public long time;
        public long size;

        public PerformanceUnit(int time, int size) {this.time = time; this.size = size;}
    }
    public enum VerifyType {
        none,
        immediately,
        after_all,
        verify_only,
        clear_files
    }

    public static class TestArgs {
        public VerifyType verifyType;
        public String rootDir;
        public int rootSizeKB;
        public int fileSizeKB;
        public int testSizeRatio;
        public boolean deleteFiles;
    }

    public RWTestThread(Handler msgHandler, TestArgs args) {
        this.testArgs = args;
        this.msgHandler = msgHandler;
        terminated = false;
        writePerformanceQueue = new ArrayDeque<>();
        readPerformanceQueue = new ArrayDeque<>();
    }

    private void addLog(String s)
    {
        sendMessage(TestFragment.messageId.add_log.value, 0, s);
    }
    private void showTextAndLog(String s)
    {
        addLog(s);
        sendMessage(TestFragment.messageId.set_text_msg.value, 0, s);
    }
    private void showText(String s)
    {
        sendMessage(TestFragment.messageId.set_text_msg.value, 0, s);
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

    private void almostDone()
    {
        int tag = testArgs.verifyType == VerifyType.verify_only ? 1 : 0;

        sendMessage(TestFragment.messageId.almost_done.value, tag, null);
        if (listener != null)
            listener.onAlmostDone(tag);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void run()
    {
        try {
            if (testArgs.verifyType != VerifyType.verify_only &&
                testArgs.verifyType != VerifyType.clear_files) {
                writeFlow();
            }

            if (testArgs.verifyType == VerifyType.after_all ||
                    testArgs.verifyType == VerifyType.verify_only)
                verifyFlow();
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (testArgs.deleteFiles) {
                showTextAndLog("Delete test files");
                deleteTestFiles();
            }
            showTextAndLog("Done");
            almostDone();
        }
    }
    private void resetWritePerformance()
    {
        if (testArgs.fileSizeKB < 32768)
            writePerformanceSlit = (32768 + testArgs.fileSizeKB - 1) / testArgs.fileSizeKB;
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
        writePerformanceKB = ((double)totalSize / (double)totalTime) * (1000000000);
        setWritePerformance(writePerformanceKB);
    }
    private void resetReadPerformance()
    {
        if (testArgs.fileSizeKB < 32768)
            readPerformanceSlit = (32768 + testArgs.fileSizeKB - 1) / testArgs.fileSizeKB;
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
        readPerformanceKB = ((double)totalSize / (double)totalTime) * (1000000000);
        setReadPerformance(readPerformanceKB);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void writeFlow()
    {
        try {
            String fName;

            Random rand = new Random(0x12345678);

            addLog("Star to write files on directory " + testArgs.rootDir);

            int testSizeKB = (int)(testArgs.rootSizeKB * ((double)testArgs.testSizeRatio / 100));
            int remainKB = testSizeKB;
            int totalFilesToTest = (testSizeKB + testArgs.fileSizeKB - 1) / testArgs.fileSizeKB;

            addLog("Size to test: " + remainKB + " KB");

            int dirId = 0;
            int dirFileCount = 0;
            int totalFileCount = 0;

            resetWritePerformance();
            resetReadPerformance();

            String subDir = String.format("%05d", dirId);
            String directory = initialDir(testArgs.rootDir, subDir);

            if (testArgs.verifyType == VerifyType.after_all)
                setProgressMax(totalFilesToTest * 2);
            else
                setProgressMax(totalFilesToTest);

            while (remainKB != 0) {
                if (terminated)
                    throw new Exception("User terminated");

                if (remainKB <= 1024) {
                    StatFs stat1 = new StatFs(testArgs.rootDir);
                    long totalBytes = (long) stat1.getBlockSize() * (long) stat1.getFreeBlocks();
                    long totalKB = totalBytes / 1024;
                    if (totalKB < remainKB)
                        remainKB = (int)totalKB;
                }
                int thisFileSizeKB = Math.min(testArgs.fileSizeKB, remainKB);

                if (dirFileCount >= MaxDirFiles) {
                    ++dirId;
                    subDir = String.format("%05d", dirId);
                    directory = initialDir(testArgs.rootDir, subDir);
                    dirFileCount = 0;
                }
                if (dirId > MaxDirFiles) {
                    break;
                }
                File dir = new File(directory);
                if (!dir.canWrite()) {
                    throw new Exception("Directory not writable");
                }
                String fileName = String.format("%05d.BIN", dirFileCount);

                File file = new File(directory, fileName);

                if (!file.createNewFile()) {
                    //throw new Exception("Create file failed! (" + file.getAbsolutePath() + ")");
                }
                showText("Test file " + file);

                FileOutputStream fsOut = new FileOutputStream(file);
                FileChannel fcOut = fsOut.getChannel();
                try {
                    int sizeByte = thisFileSizeKB << 10;

                    ByteBuffer bbWrite = ByteBuffer.allocate(sizeByte);

                    rand.nextBytes(bbWrite.array());

                    bbWrite.array()[0] = (byte)0x55;
                    bbWrite.array()[1] = (byte) 0xaa;
                    bbWrite.array()[sizeByte- 2] = (byte) 0xaa;
                    bbWrite.array()[sizeByte - 1] = (byte) 0x55;

                    long tWStart = System.nanoTime();
                    //fsOut.write(bufOut);
                    fcOut.write(bbWrite);
                    fcOut.force(true);
                    long tWEnd = System.nanoTime();

                    fsOut.flush();
                    addWritePerformanceUnit((int)(tWEnd - tWStart), thisFileSizeKB);

                    if (testArgs.verifyType == VerifyType.immediately) {
                        readAndCompareFile(file, bbWrite);
                    }
                }
                finally {
                    fcOut.close();
                    fsOut.close();
                }

                ++totalFileCount;
                ++dirFileCount;
                ////totalKBWritten += thisFileSizeKB;
                remainKB -= thisFileSizeKB;

                updateWritePerformance();
                setProgress(totalFileCount);
            }
            if (testArgs.verifyType != VerifyType.after_all)
                setProgress(totalFilesToTest);

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
    private void readAndCompareFile(File file,  ByteBuffer Expect) throws Exception
    {
        int lengthByte = Expect.array().length;
        /*
        File cacheDir = main.getCacheDir();
        if (!deleteDir(cacheDir))
            throw new Exception("Delete cache files fail");*/

        FileInputStream fsIn = new FileInputStream(file);
        FileChannel fcIn = fsIn.getChannel();

        try {
            ByteBuffer bbRead = ByteBuffer.allocate(lengthByte);
            long tRStart = System.nanoTime();
            fcIn.force(true);
            fcIn.read(bbRead);
            long tREnd = System.nanoTime();

            fcIn.force(true);
            addReadPerformanceUnit((int) (tREnd - tRStart), lengthByte / 1024);
            updateReadPerformance();

            if (!Arrays.equals(Expect.array(), bbRead.array())) {
                throw new Exception("Pattern comparision failed");
            }
        }
        finally {
            fcIn.close();
            fsIn.close();
        }
    }

    private void verifyFlow()
    {
        try {
            String fName;

            Random rand = new Random(0x12345678);

            addLog("Star to verify files on directory " + testArgs.rootDir);

            int testSizeKB = (int)(testArgs.rootSizeKB * ((double)testArgs.testSizeRatio / 100));
            int remainKB = testSizeKB;
            int totalFilesToTest = (testSizeKB + testArgs.fileSizeKB - 1) / testArgs.fileSizeKB;

            addLog("Size to verify: " + remainKB + " KB");

            resetReadPerformance();

            int dirId = 0;
            int dirFileCount = 0;
            int totalFileCount = 0;
            String subDir = String.format("%05d", dirId);
            String directory = initialDir(testArgs.rootDir, subDir);

            int progressBase = totalFilesToTest;

            if (testArgs.verifyType == VerifyType.verify_only) {
                setProgressMax(totalFilesToTest);
                progressBase = 0;
            }

            while (remainKB != 0) {
                if (terminated)
                    throw new Exception("User terminated");

                int thisFileSizeKB = Math.min(testArgs.fileSizeKB, remainKB);

                if (dirFileCount >= MaxDirFiles) {
                    ++dirId;
                    subDir = String.format("%05d", dirId);
                    directory = initialDir(testArgs.rootDir, subDir);
                    dirFileCount = 0;
                }
                if (dirId > MaxDirFiles) {
                    break;
                }
                File dir = new File(directory);
                if (!dir.canRead()) {
                    throw new Exception("Directory not readable");
                }

                String fileName = String.format("%05d.BIN", dirFileCount);

                File file = new File(directory, fileName);

                if (!file.exists()) {
                    throw new Exception("File not exists! (" + file.getAbsolutePath() + ")");
                }
                showText("Try to verify file " + file);

                int sizeByte = thisFileSizeKB << 10;

                ByteBuffer bbExpected = ByteBuffer.allocate(sizeByte);

                rand.nextBytes(bbExpected.array());

                bbExpected.array()[0] = (byte)0x55;
                bbExpected.array()[1] = (byte) 0xaa;
                bbExpected.array()[sizeByte- 2] = (byte) 0xaa;
                bbExpected.array()[sizeByte - 1] = (byte) 0x55;

                readAndCompareFile(file, bbExpected);

                ++totalFileCount;
                ++dirFileCount;
                totalKBWritten += thisFileSizeKB;
                remainKB -= thisFileSizeKB;

                setProgress(progressBase + totalFileCount);
            }

            showText("Done");
            addLog("Done");
        }
        catch (Exception e) {
            addLog("thread throw error: " + e.getMessage());
            ////throw new RuntimeException(e.getMessage());
        }
    }
    private void deleteTestFiles()
    {
        File dir = new File(testArgs.rootDir);
        String[] children = dir.list();

        for (int i = 0; i < children.length; i++) {
            File child = new File(testArgs.rootDir + "/" + children[i]);
            if (child.isDirectory())
                deleteDir(child);
            else
                child.delete();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
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
