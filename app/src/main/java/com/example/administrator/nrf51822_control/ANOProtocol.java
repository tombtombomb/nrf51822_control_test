package com.example.administrator.nrf51822_control;

public class ANOProtocol {
    static final byte HARDWARE_INFO=0x00;
    static final byte POSE_INFO=0x01;
    static final byte SENSOR_DATA=0x02;
    static final byte CONTRAL_DATA=0x03;
    static final byte GPS_INFO=0x04;
    static final byte POWER_INFO=0x05;
    static final byte PWM_MOTOR_INFO=0x06;
    static final byte SONIC_ALTITUDE_INFO=0x07;
    static final byte FLY_MODE=0x0A;
    static final byte PID_INFO_1=0x10;
    static final byte PID_INFO_2=0x11;
    static final byte PID_INFO_3=0x12;
    static final byte PID_INFO_4=0x13;
    static final byte PID_INFO_5=0x14;
    static final byte PID_INFO_6=0x15;
    static final byte FP_NUMBER=0x20;
    static final byte DISTANCE=0x30;
    static final byte D_DISTANCE=0x31;
    static final byte LOCATION=0x32;
    static final byte D_LOCATION=0x33;
    static final byte LOCATION_SET=0x3A;
    static final byte LOCATION_SET2=0x3B;
    static final byte RADIO_LINK_SET=0x40;
    static final int MSG_INFO=0xEE;
    static final int CHECK=0xEF;
}
