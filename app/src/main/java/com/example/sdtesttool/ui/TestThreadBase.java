package com.example.sdtesttool.ui;


public class TestThreadBase extends Thread {
    IThreadListener listener;

    public void setListener(IThreadListener listener) {
        this.listener = listener;
    }
}
