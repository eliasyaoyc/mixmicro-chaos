///*******************************************************************************
// * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
// * project name: smart-socket
// * file name: StringProtocol.java
// * Date: 2020-04-25
// * Author: sandao (zhengjunweimail@163.com)
// *
// ******************************************************************************/
//
//package xyz.vopen.framework.chaos.core.test.heartbeat;
//
//import xyz.vopen.framework.chaos.remoting.api.Protocol;
//import xyz.vopen.framework.chaos.remoting.api.Session;
//
//import java.nio.ByteBuffer;
//
//
//public class StringProtocol implements Protocol<String> {
//
//    @Override
//    public String encode(ByteBuffer writerBuffer, Session<String> session) {
//        return null;
//    }
//
//    @Override
//    public String decode(ByteBuffer readBuffer, Session<String> session) {
//        int remaining = readBuffer.remaining();
//        if (remaining < Integer.BYTES) {
//            return null;
//        }
//        readBuffer.mark();
//        int length = readBuffer.getInt();
//        if (length > readBuffer.remaining()) {
//            readBuffer.reset();
//            return null;
//        }
//        byte[] b = new byte[length];
//        readBuffer.get(b);
//        readBuffer.mark();
//        return new String(b);
//    }
//}
