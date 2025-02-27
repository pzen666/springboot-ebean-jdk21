package com.pzen.server.utils.modbus.rtu.slave;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Modbus从站代码-长连接版，模拟WebSocket形式
 * Float 数据解析 AB CD
 * Double 数据解析 AB CD EF GH
 */
public class ModbusSlave {

    private static final String PORT_NAME = "COM11";// COM端口名
    private static final int BAUD_RATE = 9600;// 波特率
    private static final int DATA_BITS = 8;// 数据位
    private static final int STOP_BITS = 1;// 停止位
    private static final int PARITY = SerialPort.NO_PARITY;// 校验

    public static void main(String[] args) {
        SerialPort serialPort = SerialPort.getCommPort(PORT_NAME);

        serialPort.setBaudRate(BAUD_RATE);
        serialPort.setNumDataBits(DATA_BITS);
        serialPort.setNumStopBits(STOP_BITS);
        serialPort.setParity(PARITY);

        if (serialPort.openPort()) {
            serialPort.addDataListener(new SerialPortMessageListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    try {
                        byte[] receivedData = new byte[serialPort.bytesAvailable()];
                        serialPort.readBytes(receivedData, receivedData.length);
                        // 解析数据
                        processReceivedData(receivedData);
                    } catch (Exception e) {
                        System.err.println("Error processing data: " + e.getMessage());
                    }
                }

                @Override
                public byte[] getMessageDelimiter() {
                    return new byte[0];
                }

                @Override
                public boolean delimiterIndicatesEndOfMessage() {
                    return false;
                }
            });

            try {
                while (true) {
                    Thread.sleep(1000); // 每秒检查一次
                }
            } catch (InterruptedException e) {
                System.err.println("Main thread interrupted: " + e.getMessage());
            } finally {
                serialPort.closePort();
            }
        } else {
            System.out.println("Failed to open port.");
        }
    }

    private static void processReceivedData(byte[] data) {
        // 解析 float 和 double 数据
        try {
            List<Float> floatValues = parseFloatsFromFunctionCode16(data);
            for (Float value : floatValues) {
                System.out.println("Received float value: " + value);
            }

            List<Double> doubleValues = parseDoublesFromFunctionCode16(data);
            for (Double value : doubleValues) {
                System.out.println("Received double value: " + value);
            }

        } catch (Exception e) {
            System.err.println("Error processing Modbus data: " + e.getMessage());
        }
    }

    private static List<Float> parseFloatsFromFunctionCode16(byte[] data) {
        List<Float> floatValues = new ArrayList<>();
        int dataIndex = 7; // 数据起始索引

        while (dataIndex + 3 < data.length - 2) { // 忽略CRC
            // 解析两个16位寄存器，按大端顺序
            int highRegister = (data[dataIndex] & 0xFF) << 8 | (data[dataIndex + 1] & 0xFF);
            int lowRegister = (data[dataIndex + 2] & 0xFF) << 8 | (data[dataIndex + 3] & 0xFF);

            // 合并为一个32位浮动数
            byte[] floatBytes = new byte[4];
            floatBytes[0] = (byte) (highRegister >> 8);
            floatBytes[1] = (byte) highRegister;
            floatBytes[2] = (byte) (lowRegister >> 8);
            floatBytes[3] = (byte) lowRegister;

            // 使用ByteBuffer将字节转换为浮动数（使用大端模式）
            float floatValue = ByteBuffer.wrap(floatBytes).order(ByteOrder.BIG_ENDIAN).getFloat();
            floatValues.add(floatValue);

            dataIndex += 4; // 每个浮动数由4个字节组成
        }

        return floatValues;
    }

    private static List<Double> parseDoublesFromFunctionCode16(byte[] data) {
        List<Double> doubleValues = new ArrayList<>();
        int dataIndex = 7; // 数据起始索引

        while (dataIndex + 7 < data.length - 2) { // 8个字节构成一个double类型的数
            // 解析4个16位寄存器，按大端顺序
            long highRegister = (data[dataIndex] & 0xFFL) << 8 | (data[dataIndex + 1] & 0xFFL);
            long lowRegister = (data[dataIndex + 2] & 0xFFL) << 8 | (data[dataIndex + 3] & 0xFFL);
            long highRegister2 = (data[dataIndex + 4] & 0xFFL) << 8 | (data[dataIndex + 5] & 0xFFL);
            long lowRegister2 = (data[dataIndex + 6] & 0xFFL) << 8 | (data[dataIndex + 7] & 0xFFL);

            // 将4个16位寄存器合并为一个64位long值
            long combinedValue = (highRegister << 48) | (lowRegister << 32) | (highRegister2 << 16) | lowRegister2;

            // 转换为byte数组
            byte[] doubleBytes = new byte[8];
            for (int i = 0; i < 8; i++) {
                doubleBytes[i] = (byte) (combinedValue >> (56 - i * 8));  // 将long值转换为byte数组
            }

            // 使用ByteBuffer将字节转换为double（使用大端模式）
            double doubleValue = ByteBuffer.wrap(doubleBytes).order(ByteOrder.BIG_ENDIAN).getDouble();
            doubleValues.add(doubleValue);

            dataIndex += 8; // 每个double由8个字节组成
        }

        return doubleValues;
    }
}
