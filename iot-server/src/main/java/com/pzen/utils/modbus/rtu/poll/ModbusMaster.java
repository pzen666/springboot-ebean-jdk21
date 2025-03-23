package com.pzen.utils.modbus.rtu.poll;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class ModbusMaster {

    private static final int DATA_BITS = 8;  // 数据位
    private static final int STOP_BITS = 1;  // 停止位
    private static final int PARITY = SerialPort.NO_PARITY;  // 校验位

    private static final int slaveId = 1; // 从站ID
    private static final int functionCode = 16; // 功能码 (Write Multiple Registers)

    // 创建Modbus报文（写多个寄存器）
    private static byte[] createModbusRequest(List<Float> floatList, int startAddress, int quantity) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 1 + 2 + 1 + 2 + 2 + floatList.size() * 4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        // 1. Slave ID (1 byte)
        byteBuffer.put((byte) slaveId);
        // 2. Function Code (1 byte)
        byteBuffer.put((byte) functionCode);
        // 3. Starting address (2 bytes)
        byteBuffer.putShort((short) startAddress);
        // 4. Quantity of registers (2 bytes)
        byteBuffer.putShort((short) (quantity));
        // 5. Byte count (1 byte)
        byteBuffer.put((byte) (quantity * 2));
        // 6. Register values (floatList.size() * 4 bytes)
        for (Float value : floatList) {
            byte[] floatBytes = floatToBytes(value);
            byteBuffer.put(floatBytes);
        }
        // 7. CRC (2 bytes)
        byte[] crc = calculateCRC(byteBuffer.array(), byteBuffer.position());
        byteBuffer.put(crc);
        byte[] modbusRequest = byteBuffer.array();
        return modbusRequest;
    }

    // 将float转换为Modbus寄存器值的字节数组
    private static byte[] floatToBytes(float value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putFloat(value);
        byte[] bytes = buffer.array();
        return new byte[]{
                bytes[0], bytes[1],
                bytes[2], bytes[3]
        }; // Big Endian format for Modbus
    }

    // 计算CRC校验值
    private static byte[] calculateCRC(byte[] data, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        byte[] crcBytes = new byte[2];
        crcBytes[0] = (byte) (crc & 0xFF);
        crcBytes[1] = (byte) ((crc >> 8) & 0xFF);
        return crcBytes;
    }

    // 发送Modbus请求并接收响应
    public static byte[] sendModbusRequest(SerialPort serialPort, List<Float> floatList, int startAddress, int quantity) {
        if (!serialPort.isOpen()) {
            System.out.println("Serial port is not open.");
            return null;
        }

        try {
            // 生成Modbus报文并发送
            byte[] modbusRequest = createModbusRequest(floatList, startAddress, quantity);
            serialPort.writeBytes(modbusRequest, modbusRequest.length);
            // 等待并读取响应
            byte[] response = new byte[256];
            int bytesRead = serialPort.readBytes(response, response.length);
            if (bytesRead > 0) {
                return Arrays.copyOf(response, bytesRead);
            } else {
                System.out.println("No response from slave.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // 创建一个浮动数列表，准备发送
        List<Float> floatList = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        // 打开串口连接
        SerialPort serialPort = SerialPort.getCommPort("COM10");
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(DATA_BITS);
        serialPort.setNumStopBits(STOP_BITS);
        serialPort.setParity(PARITY);

        if (!serialPort.openPort()) {
            System.out.println("Failed to open serial port.");
            return;
        }

        try {
            while (true) {
                // 发送Modbus请求并接收响应
                byte[] response = sendModbusRequest(serialPort, floatList, 0, 10);
                if (response != null) {
                    System.out.println("Response: " + Arrays.toString(response));
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serialPort.closePort();
        }
    }
}
